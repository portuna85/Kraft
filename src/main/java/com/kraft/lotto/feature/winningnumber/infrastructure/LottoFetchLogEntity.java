package com.kraft.lotto.feature.winningnumber.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "lotto_fetch_logs")
public class LottoFetchLogEntity {

    static final int MESSAGE_MAX_LENGTH = 500;
    static final int RAW_RESPONSE_MAX_LENGTH = 4000;
    static final int FAILURE_REASON_MAX_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "drw_no", nullable = false)
    private Integer drwNo;

    @Column(name = "winning_round")
    private Integer winningRound;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LottoFetchStatus status;

    @Column(name = "message", length = MESSAGE_MAX_LENGTH)
    private String message;

    @Column(name = "failure_reason", length = FAILURE_REASON_MAX_LENGTH)
    private String failureReason;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "raw_response", length = RAW_RESPONSE_MAX_LENGTH)
    private String rawResponse;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    protected LottoFetchLogEntity() {
    }

    public LottoFetchLogEntity(Integer drwNo,
                               Integer winningRound,
                               LottoFetchStatus status,
                               String message,
                               Integer responseCode,
                               String rawResponse,
                               LocalDateTime fetchedAt) {
        this.drwNo = drwNo;
        this.winningRound = winningRound;
        this.status = status;
        this.message = truncate(message, MESSAGE_MAX_LENGTH);
        this.failureReason = resolveFailureReason(status, this.message);
        this.responseCode = responseCode;
        this.rawResponse = truncate(rawResponse, RAW_RESPONSE_MAX_LENGTH);
        this.fetchedAt = fetchedAt;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public Long getId() { return id; }
    public Integer getDrwNo() { return drwNo; }
    public Integer getWinningRound() { return winningRound; }
    public LottoFetchStatus getStatus() { return status; }
    public String getMessage() { return message; }
    public String getFailureReason() { return failureReason; }
    public Integer getResponseCode() { return responseCode; }
    public String getRawResponse() { return rawResponse; }
    public LocalDateTime getFetchedAt() { return fetchedAt; }

    private static String resolveFailureReason(LottoFetchStatus status, String message) {
        if (status != LottoFetchStatus.FAILED || message == null || message.isBlank()) {
            return null;
        }
        if (!message.startsWith("reason=")) {
            return null;
        }
        int sep = message.indexOf(';');
        if (sep <= "reason=".length()) {
            return null;
        }
        return truncate(message.substring("reason=".length(), sep).trim().toLowerCase(), FAILURE_REASON_MAX_LENGTH);
    }
}
