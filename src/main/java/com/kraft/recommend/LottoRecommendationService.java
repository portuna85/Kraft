package com.kraft.recommend;

import com.kraft.common.error.ApiException;
import com.kraft.common.lotto.LottoNumberCodec;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LottoRecommendationService {

    private final LottoNumberCodec lottoNumberCodec;
    private final SecureRandom random = new SecureRandom();

    public LottoRecommendationService(LottoNumberCodec lottoNumberCodec) {
        this.lottoNumberCodec = lottoNumberCodec;
    }

    public RecommendNumbersResponse recommend(RecommendNumbersRequest request) {
        int count = request == null || request.count() == null ? 1 : request.count();
        Set<Integer> excluded = request == null || request.excludedNumbers() == null
                ? Set.of()
                : new HashSet<>(lottoNumberCodec.normalizeSubset(request.excludedNumbers()));
        if (45 - excluded.size() < 6) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TOO_MANY_EXCLUSIONS", "제외 번호를 적용한 뒤에도 최소 6개 번호가 남아야 합니다.");
        }

        List<List<Integer>> recommendations = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            recommendations.add(generateOne(excluded));
        }
        return new RecommendNumbersResponse(recommendations);
    }

    private List<Integer> generateOne(Set<Integer> excluded) {
        Set<Integer> numbers = new HashSet<>();
        while (numbers.size() < 6) {
            int candidate = random.nextInt(45) + 1;
            if (!excluded.contains(candidate)) {
                numbers.add(candidate);
            }
        }
        return lottoNumberCodec.normalize(new ArrayList<>(numbers));
    }
}
