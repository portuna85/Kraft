package com.kraft.admin;

import java.time.Clock;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time admin account bootstrap.
 *
 * Set KRAFT_ADMIN_BOOTSTRAP_USERNAME and KRAFT_ADMIN_BOOTSTRAP_PASSWORD before the
 * first startup. The runner creates the account only when the admin_users table is
 * empty; subsequent restarts with the same env vars are silently ignored.
 *
 * Remove both env vars from the deployment config after the initial account is created.
 */
@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AdminBootstrap(AdminUserRepository adminUserRepository,
                          PasswordEncoder passwordEncoder,
                          Clock clock) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String username = System.getenv("KRAFT_ADMIN_BOOTSTRAP_USERNAME");
        String password = System.getenv("KRAFT_ADMIN_BOOTSTRAP_PASSWORD");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return;
        }

        if (adminUserRepository.count() > 0) {
            log.info("AdminBootstrap: admin_users is not empty — skipping bootstrap.");
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        adminUserRepository.save(new AdminUser(
                username.trim(),
                passwordEncoder.encode(password),
                "ROLE_ADMIN",
                now
        ));
        log.info("AdminBootstrap: initial admin account '{}' created.", username.trim());
    }
}
