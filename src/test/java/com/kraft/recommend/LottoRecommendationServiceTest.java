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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
@DisplayName("로또 번호 추천 서비스 단위 테스트")
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
    @DisplayName("기본 추천")
    class BasicRecommend {

        @Test
        @DisplayName("요청한 개수만큼 조합을 반환한다")
        void recommend_returnsRequestedCount() {
            RecommendNumbersRequest request = new RecommendNumbersRequest(5, null, false);

            RecommendNumbersResponse response = service.recommend(request);

            assertThat(response.recommendations()).hasSize(5);
        }

        @Test
        @DisplayName("요청이 없으면 기본값 1개를 반환한다")
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
        @DisplayName("당첨금 최대화 모드가 아니면 점수 계산기를 호출하지 않는다")
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
        @DisplayName("후보가 6개 미만이 되도록 제외하면 과도한 제외 번호 예외가 발생한다")
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

        @Test
        @DisplayName("후보 6개에서 2세트를 요청하면 조합 부족 예외가 발생한다")
        void recommend_countExceedsPossibleCombinations_throwsApiException() {
            // 39개 제외 → 후보 6개 → C(6,6)=1가지뿐
            List<Integer> excluded = List.of(
                    1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                    11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                    21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
                    31, 32, 33, 34, 35, 36, 37, 38, 39
            );

            assertThatThrownBy(() ->
                    service.recommend(new RecommendNumbersRequest(2, excluded, false))
            )
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> {
                        ApiException apiEx = (ApiException) ex;
                        assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(apiEx.getCode()).isEqualTo("INSUFFICIENT_UNIQUE_COMBINATIONS");
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
        @DisplayName("역대 당첨 조합 로드 후 변경된 새 회차 이벤트를 받으면 갱신된다")
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
        @DisplayName("변경 없는 새 회차 이벤트를 받으면 저장소를 재조회하지 않는다")
        void onCollected_dataNotChanged_doesNotRefresh() {
            service.onCollected(new WinningNumbersCollectedEvent(1200, false));

            // loadHistoricalCombinations()에서 1회 호출되었으므로 총 1회
            verify(winningNumberRepository, org.mockito.Mockito.times(1)).findAllBalls();
        }

        @Test
        @DisplayName("역대 조합 판정은 45비트 비트마스크 기반이라 입력 순서와 무관하다")
        void isHistoricalFirstPrizeCombination_orderIndependent() {
            given(winningNumberRepository.findAllBalls())
                    .willReturn(List.of(ballsOnly(1, 3, 11, 19, 28, 34, 42)));
            service.loadHistoricalCombinations();

            assertThat(service.isHistoricalFirstPrizeCombination(List.of(3, 11, 19, 28, 34, 42))).isTrue();
            // 정렬되지 않은 순서로 넣어도(normalize가 정렬) 동일하게 판정되어야 한다
            assertThat(service.isHistoricalFirstPrizeCombination(List.of(42, 3, 34, 11, 28, 19))).isTrue();
        }

        @Test
        @DisplayName("비트마스크가 다른 조합끼리는 서로 다른 조합으로 오판되지 않는다")
        void isHistoricalFirstPrizeCombination_distinctCombosDoNotCollide() {
            given(winningNumberRepository.findAllBalls())
                    .willReturn(List.of(ballsOnly(1, 1, 2, 3, 4, 5, 6)));
            service.loadHistoricalCombinations();

            assertThat(service.isHistoricalFirstPrizeCombination(List.of(1, 2, 3, 4, 5, 6))).isTrue();
            assertThat(service.isHistoricalFirstPrizeCombination(List.of(7, 8, 9, 10, 11, 12))).isFalse();
            assertThat(service.isHistoricalFirstPrizeCombination(List.of(2, 3, 4, 5, 6, 7))).isFalse();
        }
    }

    // ── R1: 과거 1등 배제를 시도 기반이 아닌 불변 조건으로 만든다 ──────────────────
    // 무작위 시도 횟수에 기대지 않고, 산술적 경계 계산과 제어된 난수 주입으로
    // "충돌 상한 도달 시에도 과거 1등 조합이 새어나가지 않는다"를 결정론적으로 증명한다.

    @Nested
    @DisplayName("R1: 충돌 상한 도달을 결정론적으로 검증")
    class InvariantEnforcement {

        @Test
        @DisplayName("정확히 6개 남았고 그 조합이 과거 1등이면 즉시 조합 부족 오류가 발생한다")
        void recommend_onlyRemainingComboIsHistorical_rejectsImmediately() {
            // 39개 제외 → 후보 6개(1~6) → C(6,6)=1가지뿐이며 그 조합이 과거 1등
            List<Integer> excluded = IntStream.rangeClosed(7, 45).boxed().collect(Collectors.toList());
            given(winningNumberRepository.findAllBalls())
                    .willReturn(List.of(ballsOnly(1, 1, 2, 3, 4, 5, 6)));
            service.loadHistoricalCombinations();

            assertThatThrownBy(() ->
                    service.recommend(new RecommendNumbersRequest(1, excluded, false))
            )
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> {
                        ApiException apiEx = (ApiException) ex;
                        assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(apiEx.getCode()).isEqualTo("INSUFFICIENT_UNIQUE_COMBINATIONS");
                    });
        }

        @Test
        @DisplayName("난수 소스가 항상 같은 과거 1등 조합만 만들어내면 그 조합 대신 명시적 오류로 끝난다")
        void recommend_randomSourceAlwaysHitsHistorical_throwsInsteadOfLeakingIt() {
            // 38개 제외 → 후보 7개(1~7). randomSource가 항상 0을 반환하면(스왑 없음)
            // 부분 Fisher-Yates는 매 시도 candidates의 앞 6개(1,2,3,4,5,6)만 반복 생성한다.
            // 그 조합을 과거 1등으로 등록하면 — 후보 7개 중 다른 6가지 조합(allowedPossible=6)이
            // 존재해 요청 단계 검사는 통과하지만, 이 난수 소스로는 절대 도달할 수 없다.
            List<Integer> excluded = IntStream.rangeClosed(8, 45).boxed().collect(Collectors.toList());
            given(winningNumberRepository.findAllBalls())
                    .willReturn(List.of(ballsOnly(1, 1, 2, 3, 4, 5, 6)));
            service.loadHistoricalCombinations();
            service.setRandomSource(bound -> 0);

            assertThatThrownBy(() ->
                    service.recommend(new RecommendNumbersRequest(1, excluded, false))
            )
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> {
                        ApiException apiEx = (ApiException) ex;
                        assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(apiEx.getCode()).isEqualTo("INSUFFICIENT_UNIQUE_COMBINATIONS");
                    });
        }

        @Test
        @DisplayName("count=10에 제외 번호가 많고 과거 조합과 고충돌해도 항상 요청 개수만큼 유일하고 배제된 조합을 반환한다")
        void recommend_highCollisionManyExclusions_stillSatisfiesInvariants() {
            // 45 - 15(제외) = 30개 후보. 그중 5개를 과거 1등으로 등록해 충돌 밀도를 높인다.
            List<Integer> excluded = IntStream.rangeClosed(1, 15).boxed().collect(Collectors.toList());
            List<WinningBallsOnly> historical = List.of(
                    ballsOnly(1, 16, 17, 18, 19, 20, 21),
                    ballsOnly(2, 22, 23, 24, 25, 26, 27),
                    ballsOnly(3, 28, 29, 30, 31, 32, 33),
                    ballsOnly(4, 34, 35, 36, 37, 38, 39),
                    ballsOnly(5, 40, 41, 42, 43, 44, 45)
            );
            given(winningNumberRepository.findAllBalls()).willReturn(historical);
            service.loadHistoricalCombinations();

            List<List<Integer>> combos = service.recommend(
                    new RecommendNumbersRequest(10, excluded, false)).recommendations();

            assertThat(combos).hasSize(10);
            Set<Long> seenMasks = new HashSet<>();
            List<List<Integer>> historicalCombos = List.of(
                    List.of(16, 17, 18, 19, 20, 21),
                    List.of(22, 23, 24, 25, 26, 27),
                    List.of(28, 29, 30, 31, 32, 33),
                    List.of(34, 35, 36, 37, 38, 39),
                    List.of(40, 41, 42, 43, 44, 45)
            );
            for (List<Integer> combo : combos) {
                assertThat(combo).hasSize(6).doesNotHaveDuplicates().isSorted();
                assertThat(combo).doesNotContainAnyElementsOf(excluded);
                assertThat(historicalCombos).doesNotContain(combo);
                assertThat(seenMasks.add(combo.stream()
                        .mapToLong(n -> 1L << (n - 1)).reduce(0L, (a, b) -> a | b)))
                        .as("조합 간 중복 없음").isTrue();
            }
        }
    }

    // ── 속성 테스트: 모든 추천 결과가 지켜야 할 불변 조건 ─────────────────────────

    @Nested
    @DisplayName("속성 테스트 — 추천 결과 불변 조건")
    class PropertyBasedInvariants {

        @RepeatedTest(50)
        @DisplayName("무작위 요청에 대해 6개 유일·1~45 범위·제외 준수·과거 1등 배제·중복 없음이 항상 성립한다")
        void recommend_alwaysSatisfiesAllInvariants() {
            given(winningNumberRepository.findAllBalls())
                    .willReturn(List.of(ballsOnly(1, 1, 2, 3, 4, 5, 6), ballsOnly(2, 7, 14, 21, 28, 35, 42)));
            service.loadHistoricalCombinations();

            List<Integer> excluded = List.of(9, 18, 27, 36, 45);
            RecommendNumbersRequest request = new RecommendNumbersRequest(4, excluded, false);

            List<List<Integer>> combos = service.recommend(request).recommendations();

            assertThat(combos).hasSize(4);
            Set<Long> seenMasks = new HashSet<>();
            for (List<Integer> combo : combos) {
                assertThat(combo).hasSize(6);
                assertThat(combo).doesNotHaveDuplicates();
                assertThat(combo).isSorted();
                assertThat(combo).allMatch(n -> n >= 1 && n <= 45);
                assertThat(combo).doesNotContainAnyElementsOf(excluded);
                assertThat(combo).isNotEqualTo(List.of(1, 2, 3, 4, 5, 6));
                assertThat(combo).isNotEqualTo(List.of(7, 14, 21, 28, 35, 42));
                assertThat(seenMasks.add(combo.stream()
                        .mapToLong(n -> 1L << (n - 1)).reduce(0L, (a, b) -> a | b)))
                        .as("조합 간 중복 없음").isTrue();
            }
        }
    }

    // ── 당첨금 최대화 모드 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("당첨금 최대화 모드")
    class MaximizePrize {

        @BeforeEach
        void setUpScorer() {
            // 기본적으로 모든 조합에 0점 반환 (각 테스트에서 override 가능)
            given(combinationScorer.score(anyList())).willReturn(0);
        }

        @Test
        @DisplayName("당첨금 최대화 모드이면 후보 풀 크기만큼 점수 계산기를 호출한다")
        void recommend_maximizePrize_callsScorerForCandidatePool() {
            service.recommend(new RecommendNumbersRequest(1, null, true));

            // 조합 1개당 후보 50개 생성 → score 50회 호출
            verify(combinationScorer, atLeast(50)).score(anyList());
        }

        @Test
        @DisplayName("당첨금 최대화 모드에서 3세트를 요청하면 점수 계산기를 최소 150회 호출한다")
        void recommend_maximizePrize_multipleCount_callsScorerPerCombo() {
            service.recommend(new RecommendNumbersRequest(3, null, true));

            verify(combinationScorer, atLeast(150)).score(anyList());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("당첨금 최대화 모드이면 가장 높은 점수의 조합을 반환한다")
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
        @DisplayName("당첨금 최대화 모드에서도 반환 조합은 유효한 로또 번호 형식이다")
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
        @DisplayName("당첨금 최대화 모드에서도 제외 번호는 결과에 포함되지 않는다")
        void recommend_maximizePrize_respectsExcludedNumbers() {
            List<Integer> excluded = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

            service.recommend(new RecommendNumbersRequest(3, excluded, true))
                    .recommendations()
                    .forEach(combo ->
                            assertThat(combo).doesNotContainAnyElementsOf(excluded));
        }

        @Test
        @DisplayName("당첨금 최대화 모드에서도 역대 당첨 조합은 제외된다")
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
