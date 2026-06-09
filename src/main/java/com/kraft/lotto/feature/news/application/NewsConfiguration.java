package com.kraft.lotto.feature.news.application;

import com.kraft.lotto.feature.admin.infrastructure.NewsBlockedDomainRepository;
import com.kraft.lotto.feature.admin.infrastructure.NewsBlockedKeywordRepository;
import com.kraft.lotto.feature.news.infrastructure.NewsArticleRepository;
import com.kraft.lotto.infra.config.KraftNewsProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
class NewsConfiguration {

    @Bean
    NewsRssClient newsRssClient(KraftNewsProperties properties) {
        RestClient restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (compatible; KraftLottoCrawler/1.0)")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().set(HttpHeaders.ACCEPT_LANGUAGE, "ko-KR,ko;q=0.9");
                    return execution.execute(request, body);
                })
                .build();
        return new NewsRssClient(restClient, properties.rssUrl(), properties.maxArticlesPerRun(), properties.excludeKeywords());
    }

    @Bean
    NewsSourceClassifier newsSourceClassifier(KraftNewsProperties properties) {
        return new NewsSourceClassifier(properties.officialSources(), properties.pressSources());
    }

    @Bean
    NewsArticlePersister newsArticlePersister(NewsArticleRepository repository) {
        return new NewsArticlePersister(repository);
    }

    @Bean
    NewsCollectionService newsCollectionService(NewsArticleRepository repository,
                                                NewsRssClient rssClient,
                                                NewsArticlePersister persister,
                                                NewsSourceClassifier classifier,
                                                NewsBlockedDomainRepository blockedDomainRepository,
                                                NewsBlockedKeywordRepository blockedKeywordRepository,
                                                NewsRelevancePolicy relevancePolicy,
                                                Clock clock,
                                                KraftNewsProperties properties,
                                                ObjectProvider<MeterRegistry> meterRegistryProvider) {
        return new NewsCollectionService(
                repository, rssClient, persister, classifier,
                clock, properties.retentionDays(), properties.blockedDomains(), properties.excludeKeywords(),
                blockedDomainRepository, blockedKeywordRepository, relevancePolicy,
                meterRegistryProvider.getIfAvailable(SimpleMeterRegistry::new));
    }
}
