package com.kraft.lotto.feature.admin.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NewsBlockedDomainRepository extends JpaRepository<NewsBlockedDomainEntity, Long> {

    boolean existsByDomain(String domain);

    List<NewsBlockedDomainEntity> findAllByOrderByCreatedAtDesc();

    @Query("select d.domain from NewsBlockedDomainEntity d")
    List<String> findAllDomains();
}
