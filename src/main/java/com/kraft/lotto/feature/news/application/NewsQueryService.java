package com.kraft.lotto.feature.news.application;

import com.kraft.lotto.feature.news.infrastructure.NewsArticleEntity;
import com.kraft.lotto.feature.news.infrastructure.NewsArticleRepository;
import com.kraft.lotto.feature.news.web.dto.NewsArticleDto;
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
        if (page < 0) {
            throw new com.kraft.lotto.support.BusinessException(
                    com.kraft.lotto.support.ErrorCode.INVALID_PARAMETER,
                    "page must be >= 0"
            );
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new com.kraft.lotto.support.BusinessException(
                    com.kraft.lotto.support.ErrorCode.INVALID_PARAMETER,
                    "size must be between 1 and " + MAX_PAGE_SIZE
            );
        }
        Page<NewsArticleEntity> p = repository.findAllByOrderByPubDateDescCollectedAtDesc(
                PageRequest.of(page, size));
        List<NewsArticleDto> articles = p.getContent().stream()
                .map(e -> NewsArticleDto.of(e.getId(), e.getTitle(), e.getLink(),
                        e.getDescription(), e.getSource(), e.getPubDate()))
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
