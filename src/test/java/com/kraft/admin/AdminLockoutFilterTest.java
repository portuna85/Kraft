package com.kraft.admin;

import com.kraft.common.web.ClientIpResolver;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("관리자 lockout 필터 테스트")
class AdminLockoutFilterTest {

    @Mock
    private AdminLoginAttemptService lockout;
    @Mock
    private ClientIpResolver ipResolver;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    private SimpleMeterRegistry meterRegistry;
    private AdminLockoutFilter filter;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        filter = new AdminLockoutFilter(lockout, ipResolver, meterRegistry);
    }

    @Test
    @DisplayName("lockout 상태면 로그인 요청을 거부하고 카운터를 증가시킨다")
    void doFilterInternal_lockedOut_rejectsAndIncrementsCounter() throws Exception {
        given(request.getMethod()).willReturn("POST");
        given(request.getServletPath()).willReturn("/admin/login");
        given(request.getParameter("username")).willReturn("admin");
        given(ipResolver.resolve(request)).willReturn("1.2.3.4");
        given(lockout.isLockedOut("admin", "1.2.3.4")).willReturn(true);

        filter.doFilterInternal(request, response, chain);

        verify(response).sendRedirect("/admin/login?locked");
        verify(chain, never()).doFilter(request, response);
        assertThat(meterRegistry.get("kraft_admin_lockout_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("lockout 상태가 아니면 다음 필터로 통과시키고 카운터를 증가시키지 않는다")
    void doFilterInternal_notLockedOut_passesThrough() throws Exception {
        given(request.getMethod()).willReturn("POST");
        given(request.getServletPath()).willReturn("/admin/login");
        given(request.getParameter("username")).willReturn("admin");
        given(ipResolver.resolve(request)).willReturn("1.2.3.4");
        given(lockout.isLockedOut("admin", "1.2.3.4")).willReturn(false);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(meterRegistry.get("kraft_admin_lockout_total").counter().count()).isZero();
    }
}
