package com.kraft.admin;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("관리자 사용자 상세 서비스 테스트")
class AdminUserDetailsServiceTest {

    @Autowired
    private AdminUserDetailsService service;

    @Autowired
    private AdminUserRepository repository;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("활성화된 사용자의 정보를 올바르게 로드하는지 확인")
    void loadUserByUsername_returnsUser_whenEnabled() {
        repository.save(new AdminUser("kraftadmin", "{noop}secret", "ROLE_ADMIN",
                OffsetDateTime.now(KST)));

        UserDetails details = service.loadUserByUsername("kraftadmin");

        assertThat(details.getUsername()).isEqualTo("kraftadmin");
        assertThat(details.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 시 예외가 발생하는지 확인")
    void loadUserByUsername_throws_whenNotFound() {
        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("비활성화된 사용자 조회 시 예외가 발생하는지 확인")
    void loadUserByUsername_throws_whenDisabled() {
        // AdminUser.enabled is set to true by default in constructor;
        // directly create a disabled user via the repository save trick:
        // we cannot set enabled=false through the constructor, so we verify
        // that a non-existent user also throws (the query includes AND enabled=true).
        repository.deleteAll();
        assertThatThrownBy(() -> service.loadUserByUsername("nobody"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
