package com.kraft.lotto.feature.recommend.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.feature.recommend.domain.ArithmeticSequenceRule;
import com.kraft.lotto.feature.recommend.domain.BirthdayBiasRule;
import com.kraft.lotto.feature.recommend.domain.ExclusionRule;
import com.kraft.lotto.feature.recommend.domain.LongRunRule;
import com.kraft.lotto.feature.recommend.domain.PastWinningCache;
import com.kraft.lotto.feature.recommend.domain.PastWinningRule;
import com.kraft.lotto.feature.recommend.domain.SingleDecadeRule;
import com.kraft.lotto.infra.config.KraftRecommendProperties;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("추천 규칙 설정")
class RecommendRuleConfigTest {

    private final RecommendRuleConfig config = new RecommendRuleConfig();

    private static final KraftRecommendProperties PROPERTIES = new KraftRecommendProperties(
            5000,
            10_000,
            1000,
            new KraftRecommendProperties.Rules(31, 5, 5)
    );

    @Test
    @DisplayName("모든 규칙 빈을 생성하고 의도된 규칙 순서를 유지한다")
    void createsRulesInExpectedOrder() {
        PastWinningCache cache = config.pastWinningCache();
        PastWinningRule pastWinningRule = config.pastWinningRule(cache);
        BirthdayBiasRule birthdayBiasRule = config.birthdayBiasRule(PROPERTIES);
        ArithmeticSequenceRule arithmeticSequenceRule = config.arithmeticSequenceRule();
        LongRunRule longRunRule = config.longRunRule(PROPERTIES);
        SingleDecadeRule singleDecadeRule = config.singleDecadeRule(PROPERTIES);

        List<ExclusionRule> rules = config.exclusionRules(
                birthdayBiasRule,
                arithmeticSequenceRule,
                longRunRule,
                singleDecadeRule,
                pastWinningRule
        );

        assertThat(rules)
                .containsExactly(
                        birthdayBiasRule,
                        arithmeticSequenceRule,
                        longRunRule,
                        singleDecadeRule,
                        pastWinningRule
                );
    }

    @Test
    @DisplayName("스프링 주입 시 LottoRecommender의 규칙 순서는 PastWinningRule이 마지막이다")
    void keepsPastWinningRuleAsLastInSpringInjection() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(KraftRecommendProperties.class, () -> PROPERTIES);
            context.register(RecommendRuleConfig.class, RecommendConfiguration.class);
            context.refresh();

            LottoRecommender recommender = context.getBean(LottoRecommender.class);
            @SuppressWarnings("unchecked")
            List<ExclusionRule> injectedRules =
                    (List<ExclusionRule>) ReflectionTestUtils.getField(recommender, "rules");

            assertThat(injectedRules)
                    .isNotNull()
                    .extracting(rule -> rule.getClass().getName())
                    .containsExactly(
                            BirthdayBiasRule.class.getName(),
                            ArithmeticSequenceRule.class.getName(),
                            LongRunRule.class.getName(),
                            SingleDecadeRule.class.getName(),
                            PastWinningRule.class.getName()
                    );
        }
    }
}
