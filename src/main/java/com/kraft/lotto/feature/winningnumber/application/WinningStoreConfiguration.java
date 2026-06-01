package com.kraft.lotto.feature.winningnumber.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.infra.config.KraftApiProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class WinningStoreConfiguration {

    @Bean
    WinningStoreApiClient winningStoreApiClient(ObjectProvider<RestClient> lottoRestClientProvider,
                                                ObjectProvider<ObjectMapper> objectMapperProvider,
                                                KraftApiProperties properties) {
        String baseStoreUrl = resolveStoreBaseUrl(properties.url());
        RestClient restClient = lottoRestClientProvider.getIfAvailable(RestClient::create);
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new DhLotteryStoreApiClient(restClient, objectMapper, baseStoreUrl);
    }

    private static String resolveStoreBaseUrl(String winningNumberUrl) {
        if (winningNumberUrl != null && winningNumberUrl.contains("dhlottery.co.kr")) {
            return "https://www.dhlottery.co.kr/store.do";
        }
        return "https://www.dhlottery.co.kr/store.do";
    }
}
