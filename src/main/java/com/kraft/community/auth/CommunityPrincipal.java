package com.kraft.community.auth;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * DB 사용자 ID 중심의 세션 principal. Blitz의 SessionUser에 대응하며, OAuth 원본
 * attributes(Naver의 중첩 "response" 맵 포함)는 그대로 보존해 name attribute key
 * 검증(getName())이 깨지지 않게 한다.
 */
public class CommunityPrincipal implements OAuth2User {

    private final Long userId;
    private final String nickname;
    private final String nameAttributeKey;
    private final Map<String, Object> attributes;

    public CommunityPrincipal(Long userId, String nickname, String nameAttributeKey, Map<String, Object> attributes) {
        this.userId = userId;
        this.nickname = nickname;
        this.nameAttributeKey = nameAttributeKey;
        this.attributes = attributes;
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

    public String getNameAttributeKey() {
        return nameAttributeKey;
    }
}
