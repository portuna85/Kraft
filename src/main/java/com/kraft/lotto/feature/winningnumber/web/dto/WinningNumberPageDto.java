package com.kraft.lotto.feature.winningnumber.web.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Common explicit page DTO shape decoupled from Spring Data Page serialization.
 */
public record WinningNumberPageDto(
        List<WinningNumberDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public WinningNumberPageDto {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public static WinningNumberPageDto from(Page<WinningNumberDto> page) {
        return new WinningNumberPageDto(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
