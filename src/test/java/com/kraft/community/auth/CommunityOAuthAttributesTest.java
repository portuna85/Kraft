package com.kraft.community.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kraft.common.error.ApiException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CommunityOAuthAttributes provider별 정규화")
class CommunityOAuthAttributesTest {

    @Test
    @DisplayName("Google 응답을 정상적으로 정규화한다")
    void mapsGoogleAttributes() {
        Map<String, Object> attributes = Map.of(
                "sub", "google-sub-1",
                "name", "구글사용자",
                "picture", "https://example.com/pic.jpg");

        CommunityOAuthAttributes result = CommunityOAuthAttributes.of("google", attributes);

        assertThat(result.provider()).isEqualTo("google");
        assertThat(result.providerId()).isEqualTo("google-sub-1");
        assertThat(result.nickname()).isEqualTo("구글사용자");
        assertThat(result.profileImageUrl()).isEqualTo("https://example.com/pic.jpg");
    }

    @Test
    @DisplayName("Naver 중첩 response 응답을 정상적으로 정규화한다")
    void mapsNaverAttributes() {
        Map<String, Object> response = Map.of(
                "id", "naver-id-1",
                "nickname", "네이버사용자",
                "profile_image", "https://example.com/naver.jpg");
        Map<String, Object> attributes = Map.of("response", response);

        CommunityOAuthAttributes result = CommunityOAuthAttributes.of("naver", attributes);

        assertThat(result.provider()).isEqualTo("naver");
        assertThat(result.providerId()).isEqualTo("naver-id-1");
        assertThat(result.nickname()).isEqualTo("네이버사용자");
        assertThat(result.profileImageUrl()).isEqualTo("https://example.com/naver.jpg");
    }

    @Test
    @DisplayName("지원하지 않는 provider는 거부한다")
    void rejectsUnsupportedProvider() {
        assertThatThrownBy(() -> CommunityOAuthAttributes.of("kakao", Map.of()))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "OAUTH_PROVIDER_UNSUPPORTED");
    }

    @Test
    @DisplayName("Google 응답에 sub이 없으면 거부한다")
    void rejectsMissingGoogleProviderId() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", "이름만있음");

        assertThatThrownBy(() -> CommunityOAuthAttributes.of("google", attributes))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "OAUTH_ATTRIBUTE_MISSING");
    }

    @Test
    @DisplayName("Google 응답에 name이 없으면 거부한다")
    void rejectsMissingGoogleNickname() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google-sub-2");

        assertThatThrownBy(() -> CommunityOAuthAttributes.of("google", attributes))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "OAUTH_ATTRIBUTE_MISSING");
    }

    @Test
    @DisplayName("Naver 응답에 response 맵이 없으면 거부한다")
    void rejectsMissingNaverResponseMap() {
        Map<String, Object> attributes = Map.of("id", "top-level-id");

        assertThatThrownBy(() -> CommunityOAuthAttributes.of("naver", attributes))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "OAUTH_ATTRIBUTE_MISSING");
    }

    @Test
    @DisplayName("Naver 응답에 식별자가 없으면 거부한다")
    void rejectsMissingNaverProviderId() {
        Map<String, Object> response = Map.of("nickname", "닉네임만있음");
        Map<String, Object> attributes = Map.of("response", response);

        assertThatThrownBy(() -> CommunityOAuthAttributes.of("naver", attributes))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "OAUTH_ATTRIBUTE_MISSING");
    }

    @Test
    @DisplayName("Naver 응답의 식별자가 190자를 초과하면 거부한다")
    void rejectsOverlongNaverProviderId() {
        Map<String, Object> response = Map.of(
                "id", "x".repeat(191),
                "nickname", "네이버사용자");
        Map<String, Object> attributes = Map.of("response", response);

        assertThatThrownBy(() -> CommunityOAuthAttributes.of("naver", attributes))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "OAUTH_ATTRIBUTE_MISSING");
    }

    @Test
    @DisplayName("Google 응답의 식별자·닉네임·프로필 URL이 DB 길이를 초과하면 거부한다")
    void rejectsOverlongGoogleAttributes() {
        assertThatThrownBy(() -> CommunityOAuthAttributes.of("google", Map.of(
                "sub", "x".repeat(191),
                "name", "구글사용자")))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "OAUTH_ATTRIBUTE_MISSING");

        assertThatThrownBy(() -> CommunityOAuthAttributes.of("google", Map.of(
                "sub", "google-sub-3",
                "name", "가".repeat(101))))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "OAUTH_ATTRIBUTE_MISSING");

        assertThatThrownBy(() -> CommunityOAuthAttributes.of("google", Map.of(
                "sub", "google-sub-4",
                "name", "구글사용자",
                "picture", "x".repeat(501))))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "OAUTH_ATTRIBUTE_MISSING");
    }

    @Test
    @DisplayName("Naver 응답의 닉네임·프로필 URL이 DB 길이를 초과하면 거부한다")
    void rejectsOverlongNaverProfileAttributes() {
        assertThatThrownBy(() -> CommunityOAuthAttributes.of("naver", Map.of("response", Map.of(
                "id", "naver-id-4",
                "nickname", "가".repeat(101)))))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "OAUTH_ATTRIBUTE_MISSING");

        assertThatThrownBy(() -> CommunityOAuthAttributes.of("naver", Map.of("response", Map.of(
                "id", "naver-id-5",
                "nickname", "네이버사용자",
                "profile_image", "x".repeat(501)))))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "OAUTH_ATTRIBUTE_MISSING");
    }

    @Test
    @DisplayName("Naver 응답에 닉네임이 없으면 거부한다")
    void rejectsMissingNaverNickname() {
        Map<String, Object> response = Map.of("id", "naver-id-3");
        Map<String, Object> attributes = Map.of("response", response);

        assertThatThrownBy(() -> CommunityOAuthAttributes.of("naver", attributes))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "OAUTH_ATTRIBUTE_MISSING");
    }
}
