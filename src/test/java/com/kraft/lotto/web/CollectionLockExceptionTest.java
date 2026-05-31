package com.kraft.lotto.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CollectionLockException")
class CollectionLockExceptionTest {

    @Test
    @DisplayName("메시지와 원인 예외를 보존한다")
    void preservesMessageAndCause() {
        Exception cause = new Exception("shedlock error");
        CollectionLockException ex = new CollectionLockException("collection lock failed", cause);

        assertThat(ex.getMessage()).isEqualTo("collection lock failed");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
