package com.kraft.lotto.feature.admin.application;

import com.kraft.lotto.feature.admin.infrastructure.NewsBlockedDomainEntity;
import com.kraft.lotto.feature.admin.infrastructure.NewsBlockedDomainRepository;
import com.kraft.lotto.feature.admin.infrastructure.NewsBlockedKeywordEntity;
import com.kraft.lotto.feature.admin.infrastructure.NewsBlockedKeywordRepository;
import com.kraft.lotto.feature.news.infrastructure.NewsArticleEntity;
import com.kraft.lotto.feature.news.infrastructure.NewsArticleRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminNewsService {

    private final NewsArticleRepository articleRepository;
    private final NewsBlockedDomainRepository blockedDomainRepository;
    private final NewsBlockedKeywordRepository blockedKeywordRepository;
    private final AdminAuditLogService auditLogService;
    private final Clock clock;

    public AdminNewsService(NewsArticleRepository articleRepository,
                            NewsBlockedDomainRepository blockedDomainRepository,
                            NewsBlockedKeywordRepository blockedKeywordRepository,
                            AdminAuditLogService auditLogService,
                            Clock clock) {
        this.articleRepository = articleRepository;
        this.blockedDomainRepository = blockedDomainRepository;
        this.blockedKeywordRepository = blockedKeywordRepository;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<NewsArticleEntity> listPending(Pageable pageable) {
        return articleRepository.findAllByApprovedFalseOrderByCollectedAtDesc(pageable);
    }

    @Transactional
    public void approve(long id, String actor, String ip, String ua) {
        NewsArticleEntity article = findOrThrow(id);
        article.setApproved(true);
        auditLogService.recordSuccess(actor, "NEWS_APPROVE", "articleId:" + id, ip, ua);
    }

    @Transactional
    public void reject(long id, String actor, String ip, String ua) {
        NewsArticleEntity article = findOrThrow(id);
        article.setApproved(false);
        auditLogService.recordSuccess(actor, "NEWS_REJECT", "articleId:" + id, ip, ua);
    }

    @Transactional
    public void blockDomain(long articleId, String reason, String actor, String ip, String ua) {
        NewsArticleEntity article = findOrThrow(articleId);
        String domain = article.getSourceDomain();
        if (domain != null && !blockedDomainRepository.existsByDomain(domain)) {
            blockedDomainRepository.save(NewsBlockedDomainEntity.builder()
                    .domain(domain)
                    .reason(reason)
                    .createdBy(actor)
                    .createdAt(LocalDateTime.now(clock))
                    .build());
            articleRepository.rejectAllBySourceDomain(domain);
        }
        auditLogService.recordSuccess(actor, "NEWS_BLOCK_DOMAIN", "domain:" + domain, ip, ua);
    }

    @Transactional
    public void blockKeyword(String keyword, String reason, String actor, String ip, String ua) {
        if (!blockedKeywordRepository.existsByKeyword(keyword)) {
            blockedKeywordRepository.save(NewsBlockedKeywordEntity.builder()
                    .keyword(keyword)
                    .reason(reason)
                    .createdBy(actor)
                    .createdAt(LocalDateTime.now(clock))
                    .build());
        }
        auditLogService.recordSuccess(actor, "NEWS_BLOCK_KEYWORD", "keyword:" + keyword, ip, ua);
    }

    private NewsArticleEntity findOrThrow(long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("article not found: " + id));
    }
}
