package com.kraft.lotto.feature.winningnumber.web.dto;

import com.kraft.lotto.feature.winningnumber.domain.WinningStore;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public record WinningStoreDto(
        int grade,
        String name,
        String address,
        int winCount,
        String naverMapUrl
) {
    public static WinningStoreDto from(WinningStore store) {
        String query = URLEncoder.encode(store.name() + " " + store.address(), StandardCharsets.UTF_8);
        return new WinningStoreDto(
                store.grade(),
                store.name(),
                store.address(),
                store.winCount(),
                "https://map.naver.com/v5/search/" + query
        );
    }
}
