package com.kraft.recommend;

import com.kraft.common.error.ApiException;
import com.kraft.common.lotto.LottoNumberCodec;
import com.kraft.winningnumber.WinningBallsOnly;
import com.kraft.winningnumber.WinningNumberRepository;
import com.kraft.winningnumber.WinningNumbersCollectedEvent;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LottoRecommendationService 단위 테스트")
class LottoRecommendationServiceTest {

    @Mock
    private WinningNumberRepository winningNumberRepository;

    @Mock
    private CombinationScorer combinationScorer;

    private LottoNumberCodec lottoNumberCodec;
    private LottoRecommendationService service;

    private static final OffsetDateTime NOW =
            OffsetDateTime.now(ZoneId.of("Asia/Seoul"));

    private static WinningBallsOnly ballsOnly(int round, int n1, int n2, int n3, int n4, int n5, int n6) {
        return new WinningBallsOnly() {
            public int getRound() { return round; }
            public int getN1() { return n1; }
            public int getN2() { return n2; }
            public int getN3() { return n3; }
            public int getN4() { return n4; }
            public int getN5() { return n5; }
            public int getN6() { return n6; }
        };
    }

    @BeforeEach
    void setUp() {
        lottoNumberCodec = new LottoNumberCodec();
        given(winningNumberRepository.findAllBalls()).willReturn(List.of());
        service = new LottoRecommendationService(lottoNumberCodec, winningNumberRepository, combinationScorer);
        service.loadHistoricalCombinations();
    }

    // ── 기본 추천 동작 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("기본 추천 (maximizePrize=false)")
    class BasicRecommend {

        @Test
        @DisplayName("요청한 count만큼 조합을 반환한다")
        void recommend_returnsRequestedCount() {
            RecommendNumbersRequest request = new RecommendNumbersRequest(5, null, false);

            RecommendNumbersResponse response = service.recommend(request);

            assertThat(response.recommendations()).hasSize(5);
        }

        @Test
        @DisplayName("요청이 null이면 기본값 1개를 반환한다")
        void recommend_nullRequest_returnsOneCombo() {
            RecommendNumbersResponse response = service.recommend(null);

            assertThat(response.recommendations()).hasSize(1);
        }

        @Test
        @DisplayName("각 조합은 정확히 6개의 번호를 포함한다")
        void recommend_eachComboHasSixNumbers() {
            RecommendNumbersRequest request = new RecommendNumbersRequest(3, null, false);

            service.recommend(request).recommendations()
                    .forEach(combo -> assertThat(combo).hasSize(6));
        }

        @Test
        @DisplayName("모든 번호는 1~45 범위 내에 있다")
        void recommend_allNumbersInValidRange() {
            RecommendNumbersRequest request = new RecommendNumbersRequest(3, null, false);

            service.recommend(request).recommendations()
                    .forEach(combo ->
                            assertThat(combo).allMatch(n -> n >= 1 && n <= 45));
        }

        @Test
        @DisplayName("각 조합 내 번호는 중복 없이 오름차순으로 정렬된다")
        void recommend_numbersAreSortedAndUnique() {
            RecommendNumbersRequest request = new RecommendNumbersRequest(3, null, false);

            service.recommend(request).recommendations().forEach(combo -> {
                assertThat(combo).doesNotHaveDuplicates();
                assertThat(combo).isSorted();
            });
        }

        @Test
        @DisplayName("maximizePrize=false이면 scorer.score()를 호출하지 않는다")
        void recommend_notMaximizePrize_scorerNotCalled() {
            service.recommend(new RecommendNumbersRequest(3, null, false));

            verify(combinationScorer, never()).score(anyList());
        }
    }

    // ── 제외 번호 ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("제외 번호 처리")
    class ExcludedNumbers {

        @RepeatedTest(10)
        @DisplayName("제외 번호는 추천 조합에 포함되지 않는다")
        void recommend_excludedNumbersAbsentFromResult() {
            List<Integer> excluded = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            RecommendNumbersRequest request = new RecommendNumbersRequest(3, excluded, false);

            service.recommend(request).recommendations()
                    .forEach(combo ->
                            assertThat(combo).doesNotContainAnyElementsOf(excluded));
        }

        @Test
        @DisplayName("후보가 6개 미만이 되도록 제외하면 TOO_MANY_EXCLUSIONS 예외 발생")
        void recommend_tooManyExclusions_throwsApiException() {
            List<Integer> excluded = List.of(
                    1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                    11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                    21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
                    31, 32, 33, 34, 35, 36, 37, 38, 39, 40
            ); // 40개 제외 → 후보 5개

            assertThatThrownBy(() ->
                    service.recommend(new RecommendNumbersRequest(1, excluded, false))
            )
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> {
                        ApiException apiEx = (ApiException) ex;
                        assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(apiEx.getCode()).isEqualTo("TOO_MANY_EXCLUSIONS");
                    });
        }
    }

    // ── 역대 당첨 조합 제외 ───────────────────────────────────────────────────

    @Nested
    @DisplayName("역대 1등 당첨 조합 제외")
    class HistoricalExclusion {

        @RepeatedTest(30)
        @DisplayName("역대 당첨 조합은 추천 결과에 절대 포함되지 않는다")
        void recommend_historicalCombinationsNeverReturned() {
            // 1~43번 조합은 모두 역대 당첨으로 등록 (극단 시나리오)
            // 실제로는 1,100개 수준이므로 특정 조합만 등록
            given(winningNumberRepository.findAllBalls()).willReturn(List.of(ballsOnly(1, 1, 2, 3, 4, 5, 6)));
            service.loadHistoricalCombinations();

            service.recommend(new RecommendNumbersRequest(5, null, false))
                    .recommendations()
                    .forEach(combo ->
                            assertThat(combo).isNotEqualTo(List.of(1, 2, 3, 4, 5, 6)));
        }

        @Test
        @DisplayName("역대 당첨 조합 로드 후 새 회차 이벤트(dataChanged=true) 수신 시 갱신된다")
        void onCollected_dataChanged_refreshesHistoricalCombinations() {
            // 초기: 빈 목록
            assertThat(service.recommend(new RecommendNumbersRequest(1, null, false))
                    .recommendations()).hasSize(1);

            // 새 회차 수집
            given(winningNumberRepository.findAllBalls()).willReturn(List.of(ballsOnly(1200, 3, 11, 19, 28, 34, 42)));

            service.onCollected(new WinningNumbersCollectedEvent(1200, true));

            // 갱신 후 해당 조합 제외 확인 (반복 시도로 확률적 검증)
            for (int i = 0; i < 20; i++) {
                service.recommend(new RecommendNumbersRequest(1, null, false))
                        .recommendations()
                        .forEach(combo ->
                                assertThat(combo).isNotEqualTo(List.of(3, 11, 19, 28, 34, 42)));
            }
        }

        @Test
        @DisplayName("새 회차 이벤트(dataChanged=false) 수신 시 repository를 재조회하지 않는다")
        void onCollected_dataNotChanged_doesNotRefresh() {
            service.onCollected(new WinningNumbersCollectedEvent(1200, false));

            // loadHistoricalCombinations()에서 1회 호출되었으므로 총 1회
            verify(winningNumberRepository, org.mockito.Mockito.times(1)).findAllBalls();
        }
    }

    // ── 당첨금 최대화 모드 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("당첨금 최대화 모드 (maximizePrize=true)")
    class MaximizePrize {

        @BeforeEach
        void setUpScorer() {
            // 기본적으로 모든 조합에 0점 반환 (각 테스트에서 override 가능)
            given(combinationScorer.score(anyList())).willReturn(0);
        }

        @Test
        @DisplayName("maximizePrize=true이면 후보 풀 크기(50)만큼 scorer.score()를 호출한다")
        void recommend_maximizePrize_callsScorerForCandidatePool() {
            service.recommend(new RecommendNumbersRequest(1, null, true));

            // 조합 1개당 후보 50개 생성 → score 50회 호출
            verify(combinationScorer, atLeast(50)).score(anyList());
        }

        @Test
        @DisplayName("maximizePrize=true, count=3이면 score()를 최소 150회 호출한다")
        void recommend_maximizePrize_multipleCount_callsScorerPerCombo() {
            service.recommend(new RecommendNumbersRequest(3, null, true));

            verify(combinationScorer, atLeast(150)).score(anyList());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("maximizePrize=true이면 scorer가 가장 높은 점수를 부여한 조합을 반환한다")
        void recommend_maximizePrize_returnsHighestScoredCandidate() {
            // 점수 = 조합 번호의 합계 → 결정론적이고 검증 가능한 스코어
            given(combinationScorer.score(anyList())).willAnswer(inv -> {
                List<Integer> combo = inv.getArgument(0);
                return combo.stream().mapToInt(Integer::intValue).sum();
            });

            List<Integer> result = service.recommend(new RecommendNumbersRequest(1, null, true))
                    .recommendations().get(0);

            ArgumentCaptor<List<Integer>> captor = ArgumentCaptor.forClass(List.class);
            verify(combinationScorer, atLeast(50)).score(captor.capture());

            int maxScore = captor.getAllValues().stream()
                    .mapToInt(combo -> combo.stream().mapToInt(Integer::intValue).sum())
                    .max().orElse(0);
            int resultScore = result.stream().mapToInt(Integer::intValue).sum();

            assertThat(resultScore).isEqualTo(maxScore);
        }

        @Test
        @DisplayName("maximizePrize=true이어도 반환 조합은 유효한 로또 번호 형식이다")
        void recommend_maximizePrize_returnsValidCombos() {
            service.recommend(new RecommendNumbersRequest(3, null, true))
                    .recommendations()
                    .forEach(combo -> {
                        assertThat(combo).hasSize(6);
                        assertThat(combo).doesNotHaveDuplicates();
                        assertThat(combo).isSorted();
                        assertThat(combo).allMatch(n -> n >= 1 && n <= 45);
                    });
        }

        @Test
        @DisplayName("maximizePrize=true이어도 제외 번호는 결과에 포함되지 않는다")
        void recommend_maximizePrize_respectsExcludedNumbers() {
            List<Integer> excluded = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

            service.recommend(new RecommendNumbersRequest(3, excluded, true))
                    .recommendations()
                    .forEach(combo ->
                            assertThat(combo).doesNotContainAnyElementsOf(excluded));
        }

        @Test
        @DisplayName("maximizePrize=true이어도 역대 당첨 조합은 제외된다")
        void recommend_maximizePrize_historicalExclusionStillApplies() {
            // 역대 당첨 조합은 generateOne 단계에서 재추첨 처리되므로
            // scorer 호출 자체가 이루어지지 않음 → @BeforeEach의 기본 mock(0점)으로 충분
            given(winningNumberRepository.findAllBalls())
                    .willReturn(List.of(ballsOnly(1, 1, 2, 3, 4, 5, 6)));
            service.loadHistoricalCombinations();

            Set<List<Integer>> results = new HashSet<>();
            for (int i = 0; i < 20; i++) {
                results.addAll(service.recommend(
                        new RecommendNumbersRequest(1, null, true)).recommendations());
            }

            assertThat(results).doesNotContain(List.of(1, 2, 3, 4, 5, 6));
        }
    }
}
