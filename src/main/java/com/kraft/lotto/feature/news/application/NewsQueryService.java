package com.kraft.lotto.feature.news.application;

import com.kraft.lotto.feature.news.domain.NewsSourceTier;
import com.kraft.lotto.feature.news.infrastructure.NewsArticleEntity;
import com.kraft.lotto.feature.news.infrastructure.NewsArticleRepository;
import com.kraft.lotto.feature.news.web.dto.NewsArticleDto;
import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NewsQueryService {

    static final int MAX_PAGE_SIZE = 50;
    private final NewsArticleRepository repository;

    @Transactional(readOnly = true)
    public NewsPage list(int page, int size) {
        return list(page, size, null);
    }

    @Transactional(readOnly = true)
    public NewsPage list(int page, int size, NewsSourceTier tier) {
        if (page < 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "page must be >= 0"
            );
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "size must be between 1 and " + MAX_PAGE_SIZE
            );
        }
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<NewsArticleEntity> p = tier == null
                ? repository.findAllByOrderByPubDateDescCollectedAtDesc(pageRequest)
                : repository.findAllBySourceTierOrderByPubDateDescCollectedAtDesc(tier, pageRequest);
        List<NewsArticleDto> articles = p.getContent().stream()
                .map(e -> NewsArticleDto.of(e.getId(), e.getTitle(), e.getLink(),
                        e.getDescription(), e.getSource(), e.getPubDate(),
                        e.getSourceTier()))
                .toList();
        return new NewsPage(articles, p.getNumber(), p.getSize(),
                (int) p.getTotalElements(), p.getTotalPages());
    }

    public record NewsPage(List<NewsArticleDto> articles, int page, int size,
                           int totalElements, int totalPages) {
        public NewsPage {
            articles = List.copyOf(articles);
        }
    }
}
