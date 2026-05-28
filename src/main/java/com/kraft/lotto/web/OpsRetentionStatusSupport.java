package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.application.LottoFetchLogQueryService;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchLogRetentionStatusDto;
import com.kraft.lotto.infra.config.KraftCollectProperties;

final class OpsRetentionStatusSupport {

    private OpsRetentionStatusSupport() {
    }

    static FetchLogRetentionStatusDto resolve(LottoFetchLogQueryService fetchLogQueryService,
                                              KraftCollectProperties collectProperties) {
        return fetchLogQueryService.retentionStatus(
                collectProperties.logRetention().enabled(),
                collectProperties.logRetention().days(),
                collectProperties.logRetention().deleteBatchSize(),
                collectProperties.logRetention().cron(),
                collectProperties.auto().zone()
        );
    }
}
