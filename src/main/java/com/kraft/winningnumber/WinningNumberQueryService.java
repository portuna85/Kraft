package com.kraft.winningnumber;

import com.kraft.common.error.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WinningNumberQueryService {

    private final WinningNumberRepository winningNumberRepository;

    public WinningNumberQueryService(WinningNumberRepository winningNumberRepository) {
        this.winningNumberRepository = winningNumberRepository;
    }

    public WinningNumberResponse getLatest() {
        return winningNumberRepository.findTopByOrderByRoundDesc()
                .map(WinningNumberResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROUND_NOT_FOUND", "당첨 번호 데이터가 없습니다."));
    }

    public WinningNumberResponse getByRound(int round) {
        return winningNumberRepository.findByRound(round)
                .map(WinningNumberResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROUND_NOT_FOUND", round + "회차 정보를 찾을 수 없습니다."));
    }

    public WinningNumberListResponse list(int page, int size) {
        Page<WinningNumber> result = winningNumberRepository.findAllByOrderByRoundDesc(PageRequest.of(page, size));
        return new WinningNumberListResponse(
                result.getContent().stream().map(WinningNumberResponse::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }
}
