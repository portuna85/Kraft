package com.kraft.recommend;

import com.kraft.common.error.ApiException;
import com.kraft.common.lotto.LottoNumberCodec;
import com.kraft.winningnumber.WinningBallsOnly;
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

    // 조합을 Set<Set<Integer>> 대신 45비트 이내로 표현되는 long 비트마스크(ball n → bit n-1)로
    // 다뤄 회차당 Set 객체 생성·해시 비용을 없앤다. historicalCombinations는 수천 개까지도
    // 늘 수 있어(1,200회 이상 누적) 효과가 누적된다.
    private volatile Set<Long> historicalCombinations = Set.of();

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
        Set<Long> combos = new HashSet<>();
        for (WinningBallsOnly wn : winningNumberRepository.findAllBalls()) {
            combos.add(bitmaskOf(wn.getN1(), wn.getN2(), wn.getN3(), wn.getN4(), wn.getN5(), wn.getN6()));
        }
        historicalCombinations = Set.copyOf(combos);
    }

    public boolean isHistoricalFirstPrizeCombination(List<Integer> numbers) {
        List<Integer> normalized = lottoNumberCodec.normalize(numbers);
        return historicalCombinations.contains(bitmaskOf(normalized));
    }

    /** 정렬 여부와 무관하게 번호 6개(1~45)를 45비트 이내의 long으로 인코딩한다(ball n → bit n-1). */
    private static long bitmaskOf(int n1, int n2, int n3, int n4, int n5, int n6) {
        return (1L << (n1 - 1)) | (1L << (n2 - 1)) | (1L << (n3 - 1))
                | (1L << (n4 - 1)) | (1L << (n5 - 1)) | (1L << (n6 - 1));
    }

    private static long bitmaskOf(List<Integer> numbers) {
        long mask = 0L;
        for (int n : numbers) {
            mask |= 1L << (n - 1);
        }
        return mask;
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

        long available = 45L - excluded.size();
        long possible = combinations(available, 6);
        if (count > possible) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INSUFFICIENT_UNIQUE_COMBINATIONS",
                    "요청한 조합 수(" + count + ")가 가능한 고유 조합 수(" + possible + ")를 초과합니다.");
        }

        List<Integer> candidates = buildCandidates(excluded);
        List<List<Integer>> recommendations = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        int attempts = 0;
        int maxAttempts = count * MAX_ATTEMPTS;
        while (recommendations.size() < count && attempts++ < maxAttempts) {
            List<Integer> candidate = maximizePrize ? generateBest(candidates) : generateOne(candidates);
            if (seen.add(bitmaskOf(candidate))) {
                recommendations.add(candidate);
            }
        }
        // 이론상 가능한 조합 수(possible) 안에서도, 역대 당첨 조합 회피와 중복 회피가 겹쳐
        // maxAttempts 안에 count만큼 못 채울 수 있다 — 부족분을 조용히 반환하지 않고 명시한다.
        if (recommendations.size() < count) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INSUFFICIENT_UNIQUE_COMBINATIONS",
                    "생성 가능한 고유 조합이 부족합니다(생성 %d / 요청 %d)."
                            .formatted(recommendations.size(), count));
        }
        return new RecommendNumbersResponse(recommendations);
    }

    /**
     * 후보 풀에서 비인기도 점수가 가장 높은 조합을 반환한다.
     * 공동 당첨자를 최소화해 개인 수령액을 최대화하는 목적.
     */
    private List<Integer> generateBest(List<Integer> candidates) {
        List<Integer> best = null;
        int bestScore = Integer.MIN_VALUE;

        for (int i = 0; i < PRIZE_CANDIDATE_POOL; i++) {
            List<Integer> candidate = generateOne(candidates);
            int score = combinationScorer.score(candidate);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static long combinations(long n, int k) {
        long result = 1;
        for (int i = 0; i < k; i++) {
            result = result * (n - i) / (i + 1);
        }
        return result;
    }

    private static List<Integer> buildCandidates(Set<Integer> excluded) {
        List<Integer> candidates = new ArrayList<>(45 - excluded.size());
        for (int i = 1; i <= 45; i++) {
            if (!excluded.contains(i)) {
                candidates.add(i);
            }
        }
        return candidates;
    }

    // 부분 Fisher-Yates(k=6): 전체 ~45개 대신 앞 6개 위치만 셔플해 O(n) → O(k) 로 단축.
    // candidates 배열을 호출 간 재사용하므로 요청당 한 번만 빌드한다.
    private List<Integer> generateOne(List<Integer> candidates) {
        int n = candidates.size();
        Set<Long> snapshot = historicalCombinations;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            for (int i = 0; i < 6; i++) {
                int j = i + ThreadLocalRandom.current().nextInt(n - i);
                int tmp = candidates.get(i);
                candidates.set(i, candidates.get(j));
                candidates.set(j, tmp);
            }
            List<Integer> result = lottoNumberCodec.normalize(candidates.subList(0, 6));
            if (!snapshot.contains(bitmaskOf(result))) {
                return result;
            }
        }
        return lottoNumberCodec.normalize(candidates.subList(0, 6));
    }
}
