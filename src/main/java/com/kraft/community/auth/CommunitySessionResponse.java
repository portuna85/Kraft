package com.kraft.community.auth;

import java.util.List;

public record CommunitySessionResponse(boolean loggedIn, Long userId, String nickname, List<String> activeProviders) {

    public static CommunitySessionResponse anonymous(List<String> activeProviders) {
        return new CommunitySessionResponse(false, null, null, activeProviders);
    }

    public static CommunitySessionResponse of(CommunityPrincipal principal, List<String> activeProviders) {
        return new CommunitySessionResponse(true, principal.getUserId(), principal.getNickname(), activeProviders);
    }
}
