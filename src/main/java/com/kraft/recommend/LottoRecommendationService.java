package com.kraft.recommend;

import com.kraft.common.error.ApiException;
import com.kraft.common.lotto.LottoNumberCodec;
import com.kraft.winningnumber.WinningNumber;
import com.kraft.winningnumber.WinningNumberRepository;
import com.kraft.winningnumber.WinningNumbersCollectedEvent;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class LottoRecommendationService {

    private static final int MAX_ATTEMPTS = 100;
    private static final int PRIZE_CANDIDATE_POOL = 50;

    private final LottoNumberCodec lottoNumberCodec;
    private final WinningNumberRepository winningNumberRepository;
    private final CombinationScorer combinationScorer;

    private volatile Set<Set<Integer>> historicalCombinations = Set.of();

    public LottoRecommendationService(LottoNumberCodec lottoNumberCodec,
                                      WinningNumberRepository winningNumberRepository,
                                      CombinationScorer combinationScorer) {
        this.lottoNumberCodec = lottoNumberCodec;
        this.winningNumberRepository = winningNumberRepository;
        this.combinationScorer = combinationScorer;
    }

    @PostConstruct
    void loadHistoricalCombinations() {
        refreshHistoricalCombinations();
    }

    /**
     * 커밋 전 동기 리스너(@EventListener)는 트랜잭션이 롤백돼도 이미 메모리 캐시를
     * 갱신해버려 유령 데이터를 남긴다. AFTER_COMMIT으로 전환해 실제 반영된 데이터만 반영한다.
     */
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onCollected(WinningNumbersCollectedEvent event) {
        if (event.dataChanged()) {
            refreshHistoricalCombinations();
        }
    }

    private void refreshHistoricalCombinations() {
        Set<Set<Integer>> combos = new HashSet<>();
        for (WinningNumber wn : winningNumberRepository.findAll()) {
            combos.add(Set.of(wn.getN1(), wn.getN2(), wn.getN3(), wn.getN4(), wn.getN5(), wn.getN6()));
        }
        historicalCombinations = Set.copyOf(combos);
    }

    public boolean isHistoricalFirstPrizeCombination(List<Integer> numbers) {
        List<Integer> normalized = lottoNumberCodec.normalize(numbers);
        return historicalCombinations.contains(new HashSet<>(normalized));
    }

    public RecommendNumbersResponse recommend(RecommendNumbersRequest request) {
        int count = request == null || request.count() == null ? 1 : request.count();
        boolean maximizePrize = request != null && Boolean.TRUE.equals(request.maximizePrize());
        Set<Integer> excluded = request == null || request.excludedNumbers() == null
                ? Set.of()
                : new HashSet<>(lottoNumberCodec.normalizeSubset(request.excludedNumbers()));

        if (45 - excluded.size() < 6) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TOO_MANY_EXCLUSIONS",
                    "제외 번호를 적용한 뒤에도 최소 6개 번호가 남아야 합니다.");
        }

        List<List<Integer>> recommendations = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int attempts = 0;
        int maxAttempts = count * MAX_ATTEMPTS;
        while (recommendations.size() < count && attempts++ < maxAttempts) {
            List<Integer> candidate = maximizePrize ? generateBest(excluded) : generateOne(excluded);
            if (seen.add(lottoNumberCodec.toStorageValue(candidate))) {
                recommendations.add(candidate);
            }
        }
        return new RecommendNumbersResponse(recommendations);
    }

    /**
     * 후보 풀에서 비인기도 점수가 가장 높은 조합을 반환한다.
     * 공동 당첨자를 최소화해 개인 수령액을 최대화하는 목적.
     */
    private List<Integer> generateBest(Set<Integer> excluded) {
        List<Integer> best = null;
        int bestScore = Integer.MIN_VALUE;

        for (int i = 0; i < PRIZE_CANDIDATE_POOL; i++) {
            List<Integer> candidate = generateOne(excluded);
            int score = combinationScorer.score(candidate);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private List<Integer> generateOne(Set<Integer> excluded) {
        List<Integer> candidates = new ArrayList<>(45 - excluded.size());
        for (int i = 1; i <= 45; i++) {
            if (!excluded.contains(i)) {
                candidates.add(i);
            }
        }

        Set<Set<Integer>> snapshot = historicalCombinations;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            for (int i = candidates.size() - 1; i > 0; i--) {
                int j = ThreadLocalRandom.current().nextInt(i + 1);
                int tmp = candidates.get(i);
                candidates.set(i, candidates.get(j));
                candidates.set(j, tmp);
            }
            List<Integer> result = lottoNumberCodec.normalize(candidates.subList(0, 6));
            if (!snapshot.contains(new HashSet<>(result))) {
                return result;
            }
        }
        return lottoNumberCodec.normalize(candidates.subList(0, 6));
    }
}
