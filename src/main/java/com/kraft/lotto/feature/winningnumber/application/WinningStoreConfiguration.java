package com.kraft.lotto.feature.winningnumber.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class WinningStoreConfiguration {

    private static final String STORE_BASE_URL = "https://www.dhlottery.co.kr/store.do";

    @Bean
    WinningStoreApiClient winningStoreApiClient(RestClient lottoRestClient, ObjectMapper objectMapper) {
        return new DhLotteryStoreApiClient(lottoRestClient, objectMapper, STORE_BASE_URL);
    }
}
