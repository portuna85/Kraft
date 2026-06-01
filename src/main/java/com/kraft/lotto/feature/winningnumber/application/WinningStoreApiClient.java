package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.domain.WinningStore;
import java.util.List;

public interface WinningStoreApiClient {
    List<WinningStore> fetchStores(int round, int grade);
}
