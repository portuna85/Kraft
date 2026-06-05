package com.kraft.lotto.feature.admin.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NewsBlockedKeywordRepository extends JpaRepository<NewsBlockedKeywordEntity, Long> {

    boolean existsByKeyword(String keyword);

    @Query("select k.keyword from NewsBlockedKeywordEntity k")
    List<String> findAllKeywords();
}
