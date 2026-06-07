package com.kraft.lotto.feature.winningnumber.web.dto;

import com.kraft.lotto.feature.winningnumber.domain.WinningStore;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public record WinningStoreDto(
        int grade,
        String name,
        String address,
        int winCount,
        String naverMapUrl,
        String sido,
        String sigungu
) {
    private static final String NAVER_MAP_SEARCH = "https://map.naver.com/v5/search/";

    public static WinningStoreDto from(WinningStore store) {
        return new WinningStoreDto(
                store.grade(),
                store.name(),
                store.address(),
                store.winCount(),
                naverMapUrl(store.name(), store.address()),
                store.sido(),
                store.sigungu()
        );
    }

    private static String naverMapUrl(String name, String address) {
        String query = URLEncoder.encode(name + " " + address, StandardCharsets.UTF_8);
        return NAVER_MAP_SEARCH + query;
    }
}
