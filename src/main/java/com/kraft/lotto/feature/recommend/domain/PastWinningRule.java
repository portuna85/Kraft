package com.kraft.lotto.feature.recommend.domain;

import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * 과거 1등 당첨 조합과 완전히 동일한 조합을 제외한다.
 * DB / Repository 직접 접근은 금지되며 PastWinningCache만 참조한다.
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Cache reference is an immutable dependency contract for rule evaluation")
public class PastWinningRule implements ExclusionRule {

    private final PastWinningCache cache;

    public PastWinningRule(PastWinningCache cache) {
        this.cache = cache;
    }

    @Override
    public boolean shouldExclude(LottoCombination combination) {
        return cache.contains(combination);
    }

    @Override
    public String reason() {
        return "과거 1등 당첨 조합과 완전히 동일한 조합은 제외합니다.";
    }
}
