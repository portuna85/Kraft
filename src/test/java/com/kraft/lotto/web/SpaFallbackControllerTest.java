package com.kraft.lotto.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

@DisplayName("SPA fallback controller")
class SpaFallbackControllerTest {

    private final SpaFallbackController controller = new SpaFallbackController(new DefaultResourceLoader());

    @Test
    @DisplayName("reserved admin path returns not found")
    void rejectsAdminPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin");

        assertThatThrownBy(() -> controller.forward(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);
    }

    @Test
    @DisplayName("reserved ops path returns not found")
    void rejectsOpsPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ops");

        assertThatThrownBy(() -> controller.forward(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);
    }
}
