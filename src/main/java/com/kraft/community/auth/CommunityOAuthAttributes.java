package com.kraft.community.auth;

import com.kraft.common.error.ApiException;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * provider별 OAuth2 사용자 정보 응답을 정규화한 불변 표현. Google과 Naver는 응답 구조가
 * 달라(Naver는 실제 필드가 "response" 하위에 중첩됨) provider별 방어적 파싱이 필요하다.
 */
public final class CommunityOAuthAttributes {

    private final String provider;
    private final String providerId;
    private final String nickname;
    private final String profileImageUrl;
    private final String nameAttributeKey;

    private CommunityOAuthAttributes(String provider, String providerId, String nickname, String profileImageUrl,
                                      String nameAttributeKey) {
        this.provider = provider;
        this.providerId = providerId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.nameAttributeKey = nameAttributeKey;
    }

    public static CommunityOAuthAttributes of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "google" -> ofGoogle(attributes);
            case "naver" -> ofNaver(attributes);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "OAUTH_PROVIDER_UNSUPPORTED",
                    "지원하지 않는 로그인 제공자입니다: " + registrationId);
        };
    }

    private static CommunityOAuthAttributes ofGoogle(Map<String, Object> attributes) {
        String providerId = stripToNull(asString(attributes.get("sub")));
        String nickname = stripToNull(asString(attributes.get("name")));
        String profileImageUrl = stripToNull(asString(attributes.get("picture")));
        if (providerId == null || nickname == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "OAUTH_ATTRIBUTE_MISSING",
                    "Google 인증 응답에서 필수 정보를 확인할 수 없습니다.");
        }
        return new CommunityOAuthAttributes("google", providerId, nickname, profileImageUrl, "sub");
    }

    @SuppressWarnings("unchecked")
    private static CommunityOAuthAttributes ofNaver(Map<String, Object> attributes) {
        Object responseAttribute = attributes.get("response");
        if (!(responseAttribute instanceof Map)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "OAUTH_ATTRIBUTE_MISSING",
                    "Naver 인증 응답 형식이 올바르지 않습니다.");
        }
        Map<String, Object> response = (Map<String, Object>) responseAttribute;
        String providerId = stripToNull(asString(response.get("id")));
        String nickname = stripToNull(asString(response.get("nickname")));
        String profileImageUrl = stripToNull(asString(response.get("profile_image")));
        if (providerId == null || providerId.length() > 190) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "OAUTH_ATTRIBUTE_MISSING",
                    "Naver 인증 응답에서 사용자 식별자를 확인할 수 없습니다.");
        }
        if (nickname == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "OAUTH_ATTRIBUTE_MISSING",
                    "Naver 인증 응답에서 닉네임을 확인할 수 없습니다.");
        }
        return new CommunityOAuthAttributes("naver", providerId, nickname, profileImageUrl, "response");
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String stripToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String provider() {
        return provider;
    }

    public String providerId() {
        return providerId;
    }

    public String nickname() {
        return nickname;
    }

    public String profileImageUrl() {
        return profileImageUrl;
    }

    public String nameAttributeKey() {
        return nameAttributeKey;
    }
}
