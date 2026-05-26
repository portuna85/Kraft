package com.kraft.lotto.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

@DisplayName("필수 설정 검증 테스트")
class RequiredConfigValidatorTest {

    @Test
    @DisplayName("필수 배포 환경 변수 목록을 제공한다")
    void exposesRequiredDeployEnvVars() {
        assertThat(RequiredConfigValidator.requiredDeployEnvVars())
                .containsExactly(
                        "KRAFT_DB_NAME",
                        "KRAFT_DB_USER",
                        "KRAFT_DB_PASSWORD",
                        "KRAFT_DB_ROOT_PASSWORD",
                        "KRAFT_SECURITY_OPS_REQUIRED_TOKEN");
    }

    @Test
    @DisplayName("prod 프로필에서 운영 설정이 누락되면 문제를 감지한다")
    void addsProblemsWhenProdOperationalConfigsMissing() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        List<String> problems = new ArrayList<>();

        RequiredConfigValidator.addProdOperationalConfigProblems(env, problems);

        assertThat(problems).hasSize(9);
        assertThat(problems.get(0)).contains("kraft.api.url");
        assertThat(problems.get(1)).contains("kraft.security.ops.required-token");
        assertThat(problems.get(2)).contains("kraft.recommend.max-attempts");
        assertThat(problems).anyMatch(it -> it.contains("kraft.recommend.initial-pick-max-attempts"));
        assertThat(problems).anyMatch(it -> it.contains("kraft.recommend.fixup-max-attempts"));
        assertThat(problems).anyMatch(it -> it.contains("kraft.recommend.rules.birthday-threshold"));
        assertThat(problems).anyMatch(it -> it.contains("kraft.recommend.rules.long-run-threshold"));
        assertThat(problems).anyMatch(it -> it.contains("kraft.recommend.rules.decade-threshold"));
        assertThat(problems).anyMatch(it -> it.contains("kraft.api.client"));
        assertThat(problems).anyMatch(it -> it.contains("action: set KRAFT_API_URL"));
        assertThat(problems).anyMatch(it -> it.contains("action: set KRAFT_SECURITY_OPS_REQUIRED_TOKEN"));
        assertThat(problems).anyMatch(it -> it.contains("action: set KRAFT_RECOMMEND_MAX_ATTEMPTS"));
    }

    @Test
    @DisplayName("prod가 아닌 프로필에서는 운영 설정을 검증하지 않는다")
    void doesNotAddOperationalConfigProblemsOutsideProd() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");
        List<String> problems = new ArrayList<>();

        RequiredConfigValidator.addProdOperationalConfigProblems(env, problems);

        assertThat(problems).isEmpty();
    }

    @Test
    @DisplayName("prod 프로필에서 mock API 클라이언트는 허용되지 않는다")
    void addsProblemWhenProdClientIsMock() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        setValidProdOperationalConfig(env);
        env.setProperty("kraft.api.client", "mock");
        List<String> problems = new ArrayList<>();

        RequiredConfigValidator.addProdOperationalConfigProblems(env, problems);

        assertThat(problems).anyMatch(it -> it.contains("kraft.api.client"));
    }

    @Test
    @DisplayName("prod 프로필에서 dh(smok) API 클라이언트는 허용된다")
    void noProblemWhenProdClientIsSmok() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        setValidProdOperationalConfig(env);
        env.setProperty("kraft.api.client", "smok");
        List<String> problems = new ArrayList<>();

        RequiredConfigValidator.addProdOperationalConfigProblems(env, problems);

        assertThat(problems).noneMatch(it -> it.contains("kraft.api.client"));
    }

    @Test
    @DisplayName("운영 환경의 추천 범위 오류를 보고한다")
    void addsProblemsWhenProdRecommendValuesAreOutOfRange() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        setValidProdOperationalConfig(env);
        env.setProperty("kraft.api.client", "smok");
        env.setProperty("kraft.recommend.rules.birthday-threshold", "45");
        env.setProperty("kraft.recommend.rules.long-run-threshold", "1");
        env.setProperty("kraft.recommend.rules.decade-threshold", "2");
        env.setProperty("kraft.recommend.max-attempts", "0");
        List<String> problems = new ArrayList<>();

        RequiredConfigValidator.addProdOperationalConfigProblems(env, problems);

        assertThat(problems).anyMatch(p -> p.contains("kraft.recommend.rules.birthday-threshold"));
        assertThat(problems).anyMatch(p -> p.contains("kraft.recommend.rules.long-run-threshold"));
        assertThat(problems).anyMatch(p -> p.contains("kraft.recommend.rules.decade-threshold"));
        assertThat(problems).anyMatch(p -> p.contains("kraft.recommend.max-attempts"));
    }

    @Test
    @DisplayName("컨테이너 환경에서 prod 프로필이 아니면 실패한다")
    void failsWhenInContainerButNotProdProfile() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");
        env.setProperty("KRAFT_IN_CONTAINER", "true");
        List<String> problems = new ArrayList<>();

        RequiredConfigValidator.addProfilePolicyProblems(env, problems);

        assertThat(problems).anyMatch(p -> p.contains("requires prod profile"));
    }

    @Test
    @DisplayName("컨테이너 외부 환경에서 local 프로필이 아니면 실패한다")
    void failsWhenOutsideContainerButNotLocalProfile() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("KRAFT_IN_CONTAINER", "false");
        List<String> problems = new ArrayList<>();

        RequiredConfigValidator.addProfilePolicyProblems(env, problems);

        assertThat(problems).anyMatch(p -> p.contains("requires local profile"));
    }

    @Test
    @DisplayName("프로필 정책이 유효하면 KRAFT_ENV 요구사항 문제를 보고하지 않는다")
    void doesNotRequireKraftEnvWhenProfilePolicyValid() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");
        env.setProperty("KRAFT_IN_CONTAINER", "false");
        List<String> problems = new ArrayList<>();

        RequiredConfigValidator.addProfilePolicyProblems(env, problems);

        assertThat(problems).noneMatch(p -> p.contains("KRAFT_ENV"));
    }

    private static void setValidProdOperationalConfig(MockEnvironment env) {
        env.setProperty("kraft.api.url", "https://example.com");
        env.setProperty("kraft.security.ops.required-token", "ops-token");
        env.setProperty("kraft.recommend.max-attempts", "5000");
        env.setProperty("kraft.recommend.initial-pick-max-attempts", "10000");
        env.setProperty("kraft.recommend.fixup-max-attempts", "1000");
        env.setProperty("kraft.recommend.rules.birthday-threshold", "31");
        env.setProperty("kraft.recommend.rules.long-run-threshold", "5");
        env.setProperty("kraft.recommend.rules.decade-threshold", "5");
    }
}
