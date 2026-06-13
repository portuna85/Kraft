package com.kraft.winningnumber;

import com.kraft.common.error.ApiException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ExternalWinningNumberPayloadMapper {

    public WinningNumberUpsertRequest toRequest(Map<String, Object> payload) {
        String returnValue = asString(payload.get("returnValue"));
        if (returnValue != null && !returnValue.isBlank() && !"success".equalsIgnoreCase(returnValue)) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LOTTO_SOURCE_INVALID", "외부 응답이 성공 상태가 아닙니다.");
        }

        Integer round = asInteger(firstOf(payload, "round", "drwNo"));
        String drawDate = asString(firstOf(payload, "drawDate", "drwNoDate"));
        Integer bonusNumber = asInteger(firstOf(payload, "bonusNumber", "bnusNo"));
        Long firstPrizeAmount = asLong(firstOf(payload, "firstPrizeAmount", "firstWinamnt", "firstWinAmount"));

        List<Integer> numbers = extractNumbers(payload);

        if (round == null || drawDate == null || bonusNumber == null || firstPrizeAmount == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LOTTO_SOURCE_INVALID", "외부 응답 필드가 누락되었습니다.");
        }

        Long secondPrize = asLong(firstOf(payload, "secondPrize", "secondWinamnt"));
        Integer secondWinners = asInteger(firstOf(payload, "secondWinners", "secondPrzwnerCo"));
        Long totalSales = asLong(firstOf(payload, "totalSales", "totSellamnt"));
        Long firstAccumAmount = asLong(firstOf(payload, "firstAccumAmount", "firstAccumamnt"));

        return new WinningNumberUpsertRequest(
                round,
                LocalDate.parse(drawDate),
                numbers,
                bonusNumber,
                firstPrizeAmount,
                secondPrize,
                secondWinners,
                totalSales,
                firstAccumAmount,
                null
        );
    }

    private List<Integer> extractNumbers(Map<String, Object> payload) {
        Object directNumbers = payload.get("numbers");
        if (directNumbers instanceof List<?> values) {
            return values.stream().map(this::asInteger).toList();
        }

        return List.of(
                asInteger(payload.get("drwtNo1")),
                asInteger(payload.get("drwtNo2")),
                asInteger(payload.get("drwtNo3")),
                asInteger(payload.get("drwtNo4")),
                asInteger(payload.get("drwtNo5")),
                asInteger(payload.get("drwtNo6"))
        );
    }

    private Object firstOf(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            if (payload.containsKey(key)) {
                return payload.get(key);
            }
        }
        return null;
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
