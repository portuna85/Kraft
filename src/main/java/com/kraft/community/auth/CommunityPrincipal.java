package com.kraft.community.auth;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * DB 사용자 ID 중심의 세션 principal. OAuth provider의 원본 응답은 토큰·프로필 정보 등
 * 세션에 불필요한 값을 포함할 수 있으므로 보존하지 않고 내부 사용자 ID만 노출한다.
 */
public class CommunityPrincipal implements OAuth2User {

    private final Long userId;
    private final String nickname;
    private final Map<String, Object> attributes;

    public CommunityPrincipal(Long userId, String nickname) {
        this.userId = userId;
        this.nickname = nickname;
        this.attributes = Map.of("userId", userId);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_COMMUNITY_USER"));
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }

    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }
}
