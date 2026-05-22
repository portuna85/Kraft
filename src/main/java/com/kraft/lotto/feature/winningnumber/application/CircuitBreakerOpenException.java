package com.kraft.lotto.feature.winningnumber.application;

class CircuitBreakerOpenException extends LottoApiClientException {

    CircuitBreakerOpenException(String message) {
        super(message);
    }
}
