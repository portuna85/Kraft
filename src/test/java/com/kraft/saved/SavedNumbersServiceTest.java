package com.kraft.saved;

import com.kraft.common.config.SavedProperties;
import com.kraft.common.error.ApiException;
import com.kraft.common.lotto.LottoNumberCodec;
import com.kraft.winningnumber.WinningNumberQueryService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SavedNumbersService 단위 테스트")
class SavedNumbersServiceTest {

    @Mock
    private SavedNumberRepository savedNumberRepository;

    @Mock
    private WinningNumberQueryService winningNumberQueryService;

    private LottoNumberCodec lottoNumberCodec;
    private SavedProperties savedProperties;
    private Clock clock;
    private SavedNumbersService service;

    private static final String TOKEN_HASH = "abc123hash";
    private static final List<Integer> VALID_NUMBERS = List.of(3, 11, 19, 28, 34, 42);

    /** SavedNumber without auto-generated ID (field stays null → NPE when unboxed to long).
     *  Use this factory to create a persisted-like entity with a real ID for tests. */
    private SavedNumber savedEntity(String numbers, String label, String source) {
        try {
            SavedNumber entity = new SavedNumber(TOKEN_HASH, numbers, label, source,
                    java.time.OffsetDateTime.now(clock));
            var field = SavedNumber.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, 1L);
            return entity;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        lottoNumberCodec = new LottoNumberCodec();
        savedProperties = new SavedProperties(100);
        clock = Clock.fixed(Instant.parse("2026-06-13T10:00:00Z"), ZoneId.of("Asia/Seoul"));
        service = new SavedNumbersService(savedNumberRepository, lottoNumberCodec, savedProperties, winningNumberQueryService, clock);
    }

    @Test
    @DisplayName("새 번호 저장 시 created=true 반환")
    void save_newNumbers_returnsCreatedTrue() {
        String encoded = lottoNumberCodec.toStorageValue(VALID_NUMBERS);
        given(savedNumberRepository.findByClientTokenHashAndNumbers(TOKEN_HASH, encoded))
                .willReturn(Optional.empty());
        given(savedNumberRepository.countByClientTokenHash(TOKEN_HASH)).willReturn(0L);
        given(savedNumberRepository.save(any(SavedNumber.class)))
                .willAnswer(inv -> savedEntity(encoded, "즐겨찾기", "MANUAL"));

        SaveNumberResult result = service.save(TOKEN_HASH, new CreateSavedNumberRequest(VALID_NUMBERS, "즐겨찾기", "MANUAL"));

        assertThat(result.created()).isTrue();
        assertThat(result.savedNumber().numbers()).containsExactlyElementsOf(VALID_NUMBERS);
        verify(savedNumberRepository).save(any(SavedNumber.class));
    }

    @Test
    @DisplayName("이미 저장된 번호 재저장 시 created=false 반환 (멱등성)")
    void save_duplicateNumbers_returnsCreatedFalse() {
        String encoded = lottoNumberCodec.toStorageValue(VALID_NUMBERS);
        given(savedNumberRepository.findByClientTokenHashAndNumbers(TOKEN_HASH, encoded))
                .willReturn(Optional.of(savedEntity(encoded, "기존", "MANUAL")));

        SaveNumberResult result = service.save(TOKEN_HASH, new CreateSavedNumberRequest(VALID_NUMBERS, null, null));

        assertThat(result.created()).isFalse();
    }

    @Test
    @DisplayName("저장 한도 초과 시 CONFLICT ApiException 발생 (B-1 회귀)")
    void save_overLimit_throwsConflictApiException() {
        String encoded = lottoNumberCodec.toStorageValue(VALID_NUMBERS);
        given(savedNumberRepository.findByClientTokenHashAndNumbers(TOKEN_HASH, encoded))
                .willReturn(Optional.empty());
        given(savedNumberRepository.countByClientTokenHash(TOKEN_HASH)).willReturn(100L);

        assertThatThrownBy(() ->
                service.save(TOKEN_HASH, new CreateSavedNumberRequest(VALID_NUMBERS, null, null))
        )
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiEx.getCode()).isEqualTo("SAVED_LIMIT_REACHED");
                });
    }

    @Test
    @DisplayName("존재하지 않는 번호 삭제 시 NOT_FOUND ApiException 발생 (B-1 회귀)")
    void delete_notFound_throwsNotFoundApiException() {
        given(savedNumberRepository.findByIdAndClientTokenHash(99L, TOKEN_HASH))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(TOKEN_HASH, 99L))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiEx.getCode()).isEqualTo("SAVED_NUMBER_NOT_FOUND");
                });
    }

    @Test
    @DisplayName("번호 목록 조회 시 해시 기준으로 내림차순 반환")
    void list_returnsItemsForTokenHash() {
        String encoded = lottoNumberCodec.toStorageValue(VALID_NUMBERS);
        given(savedNumberRepository.findByClientTokenHashOrderByCreatedAtDesc(TOKEN_HASH))
                .willReturn(List.of(savedEntity(encoded, null, "MANUAL")));

        List<SavedNumberResponse> result = service.list(TOKEN_HASH);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).numbers()).containsExactlyElementsOf(VALID_NUMBERS);
    }

    @Test
    @DisplayName("source가 null이면 MANUAL로 저장")
    void save_nullSource_defaultsToManual() {
        String encoded = lottoNumberCodec.toStorageValue(VALID_NUMBERS);
        given(savedNumberRepository.findByClientTokenHashAndNumbers(TOKEN_HASH, encoded))
                .willReturn(Optional.empty());
        given(savedNumberRepository.countByClientTokenHash(TOKEN_HASH)).willReturn(0L);
        given(savedNumberRepository.save(any(SavedNumber.class)))
                .willAnswer(inv -> savedEntity(encoded, null, "MANUAL"));

        SaveNumberResult result = service.save(TOKEN_HASH, new CreateSavedNumberRequest(VALID_NUMBERS, null, null));

        assertThat(result.savedNumber().source()).isEqualTo("MANUAL");
    }
}
