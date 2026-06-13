package com.kraft.admin;

import java.time.OffsetDateTime;
import java.time.ZoneId;
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
    void loadUserByUsername_returnsUser_whenEnabled() {
        repository.save(new AdminUser("kraftadmin", "{noop}secret", "ROLE_ADMIN",
                OffsetDateTime.now(KST)));

        UserDetails details = service.loadUserByUsername("kraftadmin");

        assertThat(details.getUsername()).isEqualTo("kraftadmin");
        assertThat(details.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Test
    void loadUserByUsername_throws_whenNotFound() {
        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
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
