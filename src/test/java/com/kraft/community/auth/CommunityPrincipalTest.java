package com.kraft.community.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CommunityPrincipal 세션 정보 최소화")
class CommunityPrincipalTest {

    @Test
    @DisplayName("OAuth 원본 속성 대신 내부 사용자 ID만 보존한다")
    void keepsOnlyInternalUserIdAsAttribute() {
        CommunityPrincipal principal = new CommunityPrincipal(42L, "사용자");

        assertThat(principal.getName()).isEqualTo("42");
        assertThat(principal.getNickname()).isEqualTo("사용자");
        assertThat(principal.getAttributes()).isEqualTo(Map.of("userId", 42L));
        assertThat(principal.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_COMMUNITY_USER");
    }
}
