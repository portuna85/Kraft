package com.kraft.lotto.feature.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.admin.infrastructure.NewsBlockedDomainRepository;
import com.kraft.lotto.feature.admin.infrastructure.NewsBlockedKeywordRepository;
import com.kraft.lotto.feature.news.infrastructure.NewsArticleEntity;
import com.kraft.lotto.feature.news.infrastructure.NewsArticleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("관리자 뉴스 서비스 테스트")
class AdminNewsServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-05T12:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock NewsArticleRepository articleRepository;
    @Mock NewsBlockedDomainRepository blockedDomainRepository;
    @Mock NewsBlockedKeywordRepository blockedKeywordRepository;
    @Mock AdminAuditLogService auditLogService;

    AdminNewsService service;

    @BeforeEach
    void setUp() {
        service = new AdminNewsService(articleRepository, blockedDomainRepository,
                blockedKeywordRepository, auditLogService, CLOCK);
    }

    @Test
    @DisplayName("대기 중인 뉴스 목록 조회 — 리포지토리에 위임 호출한다")
    void listPendingDelegates() {
        var pageable = PageRequest.of(0, 10);
        service.listPending(pageable);
        verify(articleRepository).findAllByApprovedFalseAndRejectedFalseOrderByCollectedAtDesc(pageable);
    }

    @Test
    @DisplayName("뉴스 승인 — 승인 상태로 변경하고 감사 로그를 기록한다")
    void approveUpdatesApprovedAndAudits() {
        NewsArticleEntity article = makeArticle(1L, "news.com");
        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));

        service.approve(1L, "admin@example.com", "127.0.0.1", "Mozilla/5.0");

        assertThat(article.isApproved()).isTrue();
        verify(auditLogService).recordSuccess(eq("admin@example.com"), eq("NEWS_APPROVE"),
                contains("1"), eq("127.0.0.1"), eq("Mozilla/5.0"));
    }

    @Test
    @DisplayName("뉴스 거절 — 미승인 및 거절 상태로 변경하고 감사 로그를 기록한다")
    void rejectUpdatesApprovedAndAudits() {
        NewsArticleEntity article = makeArticle(2L, "news.com");
        article.setApproved(true);
        when(articleRepository.findById(2L)).thenReturn(Optional.of(article));

        service.reject(2L, "admin@example.com", "127.0.0.1", "curl/7.x");

        assertThat(article.isApproved()).isFalse();
        assertThat(article.isRejected()).isTrue();
        verify(auditLogService).recordSuccess(eq("admin@example.com"), eq("NEWS_REJECT"),
                contains("2"), any(), any());
    }

    @Test
    @DisplayName("뉴스 승인 실패 — 기사가 없으면 예외를 던진다")
    void approveThrowsWhenArticleNotFound() {
        when(articleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(99L, "admin", "127.0.0.1", "ua"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("도메인 차단 — 신규 도메인이면 저장하고 해당 도메인 기사들을 거절한다")
    void blockDomainSavesAndRejectsArticles() {
        NewsArticleEntity article = makeArticle(3L, "spam.com");
        when(articleRepository.findById(3L)).thenReturn(Optional.of(article));
        when(blockedDomainRepository.existsByDomain("spam.com")).thenReturn(false);

        service.blockDomain(3L, "스팸", "admin@example.com", "127.0.0.1", "ua");

        verify(blockedDomainRepository).save(any());
        verify(articleRepository).rejectAllBySourceDomain("spam.com");
        verify(auditLogService).recordSuccess(eq("admin@example.com"), eq("NEWS_BLOCK_DOMAIN"),
                contains("spam.com"), any(), any());
    }

    @Test
    @DisplayName("도메인 차단 건너뛰기 — 이미 차단된 도메인이면 저장을 수행하지 않는다")
    void blockDomainSkipsIfAlreadyBlocked() {
        NewsArticleEntity article = makeArticle(4L, "spam.com");
        when(articleRepository.findById(4L)).thenReturn(Optional.of(article));
        when(blockedDomainRepository.existsByDomain("spam.com")).thenReturn(true);

        service.blockDomain(4L, "스팸", "admin@example.com", "127.0.0.1", "ua");

        verify(blockedDomainRepository, never()).save(any());
        verify(articleRepository, never()).rejectAllBySourceDomain(any());
    }

    @Test
    @DisplayName("키워드 차단 — 신규 키워드이면 저장한다")
    void blockKeywordSavesNewKeyword() {
        when(blockedKeywordRepository.existsByKeyword("아파트 로또")).thenReturn(false);

        service.blockKeyword("아파트 로또", "부동산 키워드", "admin@example.com", "127.0.0.1", "ua");

        verify(blockedKeywordRepository).save(any());
        verify(auditLogService).recordSuccess(eq("admin@example.com"), eq("NEWS_BLOCK_KEYWORD"),
                contains("아파트 로또"), any(), any());
    }

    @Test
    @DisplayName("키워드 차단 건너뛰기 — 이미 등록된 키워드이면 저장을 수행하지 않는다")
    void blockKeywordSkipsIfExists() {
        when(blockedKeywordRepository.existsByKeyword("로또")).thenReturn(true);

        service.blockKeyword("로또", "이유", "admin@example.com", "127.0.0.1", "ua");

        verify(blockedKeywordRepository, never()).save(any());
    }

    private static NewsArticleEntity makeArticle(Long id, String domain) {
        return new NewsArticleEntity("제목", "https://" + domain + "/news",
                "hash" + id, "설명", "출처", domain,
                com.kraft.lotto.feature.news.domain.NewsSourceTier.GENERAL,
                LocalDateTime.of(2026, 6, 5, 10, 0), LocalDateTime.of(2026, 6, 5, 12, 0));
    }
}
