package com.kraft.lotto.feature.winningnumber.application;

import java.util.Map;

public final class KoreanAddressParser {

    private static final Map<String, String> SIDO_ALIASES = Map.ofEntries(
            Map.entry("서울특별시", "서울"),
            Map.entry("서울", "서울"),
            Map.entry("부산광역시", "부산"),
            Map.entry("부산", "부산"),
            Map.entry("대구광역시", "대구"),
            Map.entry("대구", "대구"),
            Map.entry("인천광역시", "인천"),
            Map.entry("인천", "인천"),
            Map.entry("광주광역시", "광주"),
            Map.entry("광주", "광주"),
            Map.entry("대전광역시", "대전"),
            Map.entry("대전", "대전"),
            Map.entry("울산광역시", "울산"),
            Map.entry("울산", "울산"),
            Map.entry("세종특별자치시", "세종"),
            Map.entry("세종", "세종"),
            Map.entry("경기도", "경기"),
            Map.entry("경기", "경기"),
            Map.entry("강원특별자치도", "강원"),
            Map.entry("강원도", "강원"),
            Map.entry("강원", "강원"),
            Map.entry("충청북도", "충북"),
            Map.entry("충북", "충북"),
            Map.entry("충청남도", "충남"),
            Map.entry("충남", "충남"),
            Map.entry("전북특별자치도", "전북"),
            Map.entry("전라북도", "전북"),
            Map.entry("전북", "전북"),
            Map.entry("전라남도", "전남"),
            Map.entry("전남", "전남"),
            Map.entry("경상북도", "경북"),
            Map.entry("경북", "경북"),
            Map.entry("경상남도", "경남"),
            Map.entry("경남", "경남"),
            Map.entry("제주특별자치도", "제주"),
            Map.entry("제주도", "제주"),
            Map.entry("제주", "제주")
    );

    private KoreanAddressParser() {}

    public record ParsedAddress(String sido, String sigungu) {
        public static ParsedAddress empty() {
            return new ParsedAddress(null, null);
        }
    }

    public static ParsedAddress parse(String address) {
        if (address == null || address.isBlank()) {
            return ParsedAddress.empty();
        }
        String[] parts = address.strip().split("\\s+");
        if (parts.length == 0) {
            return ParsedAddress.empty();
        }
        String sido = normalizeSido(parts[0]);
        if (parts.length == 1) {
            return new ParsedAddress(sido, null);
        }
        String sigungu = resolveSimgungu(parts);
        return new ParsedAddress(sido, sigungu);
    }

    private static String resolveSimgungu(String[] parts) {
        // 예: 수원시 팔달구, 청주시 흥덕구 → "수원시 팔달구"
        if (parts.length >= 3 && parts[1].endsWith("시") && parts[2].endsWith("구")) {
            return parts[1] + " " + parts[2];
        }
        return parts[1];
    }

    private static String normalizeSido(String raw) {
        return SIDO_ALIASES.getOrDefault(raw, raw);
    }
}
