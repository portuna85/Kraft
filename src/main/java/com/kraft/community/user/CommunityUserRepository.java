package com.kraft.community.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityUserRepository extends JpaRepository<CommunityUser, Long> {

    Optional<CommunityUser> findByProviderAndProviderId(String provider, String providerId);
}
