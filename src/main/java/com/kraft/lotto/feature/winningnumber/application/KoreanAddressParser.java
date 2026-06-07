package com.kraft.lotto.feature.winningnumber.application;

public final class KoreanAddressParser {

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
        return switch (raw) {
            case "서울특별시", "서울" -> "서울";
            case "부산광역시", "부산" -> "부산";
            case "대구광역시", "대구" -> "대구";
            case "인천광역시", "인천" -> "인천";
            case "광주광역시", "광주" -> "광주";
            case "대전광역시", "대전" -> "대전";
            case "울산광역시", "울산" -> "울산";
            case "세종특별자치시", "세종" -> "세종";
            case "경기도", "경기" -> "경기";
            case "강원특별자치도", "강원도", "강원" -> "강원";
            case "충청북도", "충북" -> "충북";
            case "충청남도", "충남" -> "충남";
            case "전북특별자치도", "전라북도", "전북" -> "전북";
            case "전라남도", "전남" -> "전남";
            case "경상북도", "경북" -> "경북";
            case "경상남도", "경남" -> "경남";
            case "제주특별자치도", "제주도", "제주" -> "제주";
            default -> raw;
        };
    }
}
