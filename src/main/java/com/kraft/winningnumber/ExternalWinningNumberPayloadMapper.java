package com.kraft.winningnumber;

import com.kraft.common.error.ApiException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ExternalWinningNumberPayloadMapper {

    // Parses both the old common.do format and the new lt645/selectPstLt645InfoNew.do item format.
    public WinningNumberUpsertRequest toRequest(Map<String, Object> payload) {
        String returnValue = asString(payload.get("returnValue"));
        if (returnValue != null && !returnValue.isBlank() && !"success".equalsIgnoreCase(returnValue)) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LOTTO_SOURCE_INVALID", "외부 응답이 성공 상태가 아닙니다.");
        }

        Integer round = asInteger(firstOf(payload, "ltEpsd", "round", "drwNo"));
        String drawDate = normalizeDate(asString(firstOf(payload, "ltRflYmd", "drawDate", "drwNoDate")));
        Integer bonusNumber = asInteger(firstOf(payload, "bnsWnNo", "bonusNumber", "bnusNo"));
        Long firstPrizeAmount = asLong(firstOf(payload, "rnk1WnAmt", "firstPrizeAmount", "firstWinamnt", "firstWinAmount"));

        List<Integer> numbers = extractNumbers(payload);

        if (round == null || drawDate == null || bonusNumber == null || firstPrizeAmount == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LOTTO_SOURCE_INVALID", "외부 응답 필드가 누락되었습니다.");
        }

        Long secondPrize = asLong(firstOf(payload, "rnk2WnAmt", "secondPrize", "secondWinamnt"));
        Integer secondWinners = asInteger(firstOf(payload, "rnk2WnNope", "secondWinners", "secondPrzwnerCo"));
        Long totalSales = asLong(firstOf(payload, "rlvtEpsdSumNtslAmt", "totalSales", "totSellamnt"));
        Long firstAccumAmount = asLong(firstOf(payload, "rnk1SumWnAmt", "firstAccumAmount", "firstAccumamnt"));

        return new WinningNumberUpsertRequest(
                round,
                LocalDate.parse(drawDate),
                numbers,
                bonusNumber,
                firstPrizeAmount,
                secondPrize,
                secondWinners,
                totalSales,
                firstAccumAmount
        );
    }

    private List<Integer> extractNumbers(Map<String, Object> payload) {
        Object directNumbers = payload.get("numbers");
        if (directNumbers instanceof List<?> values) {
            List<Integer> result = values.stream().map(this::asInteger).toList();
            validateNumbers(result);
            return result;
        }
        // New format: tm1WnNo - tm6WnNo
        if (payload.containsKey("tm1WnNo")) {
            return requireNumbers(
                    asInteger(payload.get("tm1WnNo")),
                    asInteger(payload.get("tm2WnNo")),
                    asInteger(payload.get("tm3WnNo")),
                    asInteger(payload.get("tm4WnNo")),
                    asInteger(payload.get("tm5WnNo")),
                    asInteger(payload.get("tm6WnNo"))
            );
        }
        // Old format: drwtNo1 - drwtNo6
        return requireNumbers(
                asInteger(payload.get("drwtNo1")),
                asInteger(payload.get("drwtNo2")),
                asInteger(payload.get("drwtNo3")),
                asInteger(payload.get("drwtNo4")),
                asInteger(payload.get("drwtNo5")),
                asInteger(payload.get("drwtNo6"))
        );
    }

    private List<Integer> requireNumbers(Integer... nums) {
        List<Integer> result = new java.util.ArrayList<>();
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "LOTTO_SOURCE_INVALID",
                        "당첨 번호 " + (i + 1) + "번 필드가 누락되었습니다.");
            }
            result.add(nums[i]);
        }
        return java.util.Collections.unmodifiableList(result);
    }

    private void validateNumbers(List<Integer> numbers) {
        for (int i = 0; i < numbers.size(); i++) {
            if (numbers.get(i) == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "LOTTO_SOURCE_INVALID",
                        "당첨 번호 목록에 null 값이 포함되어 있습니다 (index " + i + ").");
            }
        }
    }

    // Converts YYYYMMDD (new API) to YYYY-MM-DD; passes through YYYY-MM-DD as-is.
    private String normalizeDate(String raw) {
        if (raw == null) {
            return null;
        }
        if (raw.length() == 8 && !raw.contains("-")) {
            return raw.substring(0, 4) + "-" + raw.substring(4, 6) + "-" + raw.substring(6, 8);
        }
        return raw;
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
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "LOTTO_SOURCE_PARSE_ERROR", "숫자 변환 실패: " + value);
        }
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "LOTTO_SOURCE_PARSE_ERROR", "숫자 변환 실패: " + value);
        }
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
