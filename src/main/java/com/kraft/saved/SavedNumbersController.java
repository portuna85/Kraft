package com.kraft.saved;

import com.kraft.common.web.DeviceTokenSupport;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public List<SavedNumberResponse> list(@RequestHeader(name = "X-Device-Token", required = false) String deviceToken) {
        return savedNumbersService.list(deviceTokenSupport.requireHashedToken(deviceToken));
    }

    @PostMapping
    public ResponseEntity<SavedNumberResponse> save(@RequestHeader(name = "X-Device-Token", required = false) String deviceToken,
                                                    @Valid @RequestBody CreateSavedNumberRequest request) {
        SaveNumberResult result = savedNumbersService.save(deviceTokenSupport.requireHashedToken(deviceToken), request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.savedNumber());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader(name = "X-Device-Token", required = false) String deviceToken,
                                       @PathVariable long id) {
        savedNumbersService.delete(deviceTokenSupport.requireHashedToken(deviceToken), id);
        return ResponseEntity.noContent().build();
    }
}
