package com.kraft.saved;

import com.kraft.common.web.DeviceTokenSupport;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/saved")
public class SavedNumbersController {

    private final SavedNumbersService savedNumbersService;
    private final DeviceTokenSupport deviceTokenSupport;

    public SavedNumbersController(SavedNumbersService savedNumbersService, DeviceTokenSupport deviceTokenSupport) {
        this.savedNumbersService = savedNumbersService;
        this.deviceTokenSupport = deviceTokenSupport;
    }

    @GetMapping
    public ResponseEntity<List<SavedNumberResponse>> list(
            @RequestHeader(name = "X-Device-Token", required = true) String deviceToken) {
        List<SavedNumberResponse> result = savedNumbersService.list(deviceTokenSupport.requireHashedToken(deviceToken));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
    }

    @GetMapping("/matches")
    public ResponseEntity<List<SavedNumberMatchResult>> matches(
            @RequestHeader(name = "X-Device-Token", required = true) String deviceToken,
            @RequestParam(name = "round", defaultValue = "latest") String round) {
        String hash = deviceTokenSupport.requireHashedToken(deviceToken);
        List<SavedNumberMatchResult> result = savedNumbersService.compareWithRound(hash, round);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
    }

    @PostMapping
    public ResponseEntity<SaveNumberResult> save(@RequestHeader(name = "X-Device-Token", required = true) String deviceToken,
                                                 @Valid @RequestBody CreateSavedNumberRequest request) {
        SaveNumberResult result = savedNumbersService.save(deviceTokenSupport.requireHashedToken(deviceToken), request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader(name = "X-Device-Token", required = true) String deviceToken,
                                       @PathVariable long id) {
        savedNumbersService.delete(deviceTokenSupport.requireHashedToken(deviceToken), id);
        return ResponseEntity.noContent().build();
    }
}
