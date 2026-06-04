package com.kraft.lotto.feature.winningnumber.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import static com.kraft.lotto.feature.winningnumber.application.LottoApiClientException.FailureReason;

class DhLotteryResponseParser {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    DhLotteryResponseParser(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    Optional<WinningNumber> parse(int round, String body) {
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.startsWith("<")) {
            throw new LottoApiClientException("response is not JSON (round=" + round + ")", FailureReason.NON_JSON);
        }
        JsonNode node;
        try {
            node = objectMapper.readTree(body);
        } catch (Exception ex) {
            throw new LottoApiClientException("response parse failed (round=" + round + ")", ex, FailureReason.JSON_PARSE);
        }
        String returnValue = requiredText(node, "returnValue", round);
        if (!"success".equalsIgnoreCase(returnValue)) {
            if ("fail".equalsIgnoreCase(returnValue)) {
                return Optional.empty();
            }
            throw new LottoApiClientException("unexpected_return_value (round=" + round + ", returnValue=" + returnValue + ")",
                    FailureReason.UNEXPECTED_RETURN_VALUE);
        }
        try {
            requireFields(node, round, "drwNo", "drwNoDate", "drwtNo1", "drwtNo2", "drwtNo3",
                    "drwtNo4", "drwtNo5", "drwtNo6", "bnusNo", "firstWinamnt", "firstPrzwnerCo", "totSellamnt");
            int drwNo = requiredInt(node, "drwNo", round);
            if (drwNo != round) {
                throw new LottoApiClientException("validation: round mismatch request=" + round + ", response=" + drwNo,
                        FailureReason.VALIDATION);
            }
            LocalDate drawDate = LocalDate.parse(node.path("drwNoDate").asText());
            List<Integer> mains = List.of(
                    requiredInt(node, "drwtNo1", round), requiredInt(node, "drwtNo2", round),
                    requiredInt(node, "drwtNo3", round), requiredInt(node, "drwtNo4", round),
                    requiredInt(node, "drwtNo5", round), requiredInt(node, "drwtNo6", round)
            );
            int bonus = requiredInt(node, "bnusNo", round);
            long firstPrize = requiredLong(node, "firstWinamnt", round);
            int firstWinners = requiredInt(node, "firstPrzwnerCo", round);
            long totalSales = requiredLong(node, "totSellamnt", round);
            long firstAccumAmount = optionalLong(node, "firstAccumamnt", round, 0L);
            long secondPrize = optionalLong(node, "secondWinamnt", round, 0L);
            int secondWinners = (int) optionalLong(node, "secondPrzwnerCo", round, 0L);
            return Optional.of(new WinningNumber(
                    drwNo, drawDate, new LottoCombination(mains), bonus, firstPrize, firstWinners,
                    totalSales, firstAccumAmount, secondPrize, secondWinners, body, LocalDateTime.now(clock)
            ));
        } catch (DateTimeParseException | IllegalArgumentException ex) {
            throw new LottoApiClientException("transform: response transform failed (round=" + round + "): " + ex.getMessage(),
                    ex, FailureReason.TRANSFORM);
        }
    }

    private static int requiredInt(JsonNode node, String fieldName, int round) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            throw new LottoApiClientException("missing_field: field missing (round=" + round + ", field=" + fieldName + ")",
                    FailureReason.MISSING_FIELD);
        }
        if (!value.isIntegralNumber()) {
            throw new LottoApiClientException("validation: field is not integral (round=" + round + ", field=" + fieldName + ")",
                    FailureReason.VALIDATION);
        }
        if (!value.canConvertToInt()) {
            throw new LottoApiClientException("validation: field out of int range (round=" + round + ", field=" + fieldName + ")",
                    FailureReason.VALIDATION);
        }
        return value.asInt();
    }

    private static long optionalLong(JsonNode node, String fieldName, int round, long defaultValue) {
        if (node.path(fieldName).isMissingNode() || node.path(fieldName).isNull()) {
            return defaultValue;
        }
        return requiredLong(node, fieldName, round);
    }

    private static long requiredLong(JsonNode node, String fieldName, int round) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            throw new LottoApiClientException("missing_field: field missing (round=" + round + ", field=" + fieldName + ")",
                    FailureReason.MISSING_FIELD);
        }
        if (!value.isIntegralNumber()) {
            throw new LottoApiClientException("validation: field is not integral (round=" + round + ", field=" + fieldName + ")",
                    FailureReason.VALIDATION);
        }
        if (!value.canConvertToLong()) {
            throw new LottoApiClientException("validation: field out of long range (round=" + round + ", field=" + fieldName + ")",
                    FailureReason.VALIDATION);
        }
        return value.asLong();
    }

    private static void requireFields(JsonNode node, int round, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.path(fieldName).isMissingNode() || node.path(fieldName).isNull()) {
                throw new LottoApiClientException("missing_field: field missing (round=" + round + ", field=" + fieldName + ")",
                        FailureReason.MISSING_FIELD);
            }
        }
    }

    private static String requiredText(JsonNode node, String fieldName, int round) {
        if (node.path(fieldName).isMissingNode() || node.path(fieldName).isNull()) {
            throw new LottoApiClientException("missing_field: field missing (round=" + round + ", field=" + fieldName + ")",
                    FailureReason.MISSING_FIELD);
        }
        return node.path(fieldName).asText();
    }
}
