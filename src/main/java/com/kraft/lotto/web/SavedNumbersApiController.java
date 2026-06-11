package com.kraft.lotto.web;

import com.kraft.lotto.feature.saved.application.SavedNumbersService;
import com.kraft.lotto.feature.saved.application.SavedNumbersService.SavedNumbersDto;
import com.kraft.lotto.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@PublicApi
@RestController
@RequestMapping("/api/v1/saved")
@Validated
@RequiredArgsConstructor
@Tag(name = "Saved", description = "번호 저장함 관리")
public class SavedNumbersApiController {

    private final SavedNumbersService savedNumbersService;

    @GetMapping
    @Operation(summary = "저장된 번호 목록 조회")
    public ResponseEntity<ApiResponse<List<SavedNumbersDto>>> list(
            @RequestParam @NotBlank @Size(max = 255) String deviceToken) {
        return ResponseEntity.ok(ApiResponse.success(savedNumbersService.list(deviceToken)));
    }

    @PostMapping
    @Operation(summary = "번호 저장")
    public ResponseEntity<ApiResponse<SavedNumbersDto>> save(@RequestBody @Valid SaveRequest req) {
        SavedNumbersDto saved = savedNumbersService.save(req.deviceToken(), req.numbers(), req.label());
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "저장된 번호 삭제")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @RequestParam @NotBlank @Size(max = 255) String deviceToken) {
        savedNumbersService.delete(id, deviceToken);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    public record SaveRequest(
            @NotBlank @Size(max = 255) String deviceToken,
            @NotEmpty List<Integer> numbers,
            @Size(max = 100) String label
    ) {
        public SaveRequest {
            numbers = List.copyOf(numbers);
        }
    }
}
