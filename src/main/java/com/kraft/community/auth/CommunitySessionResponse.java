package com.kraft.community.auth;

public record CommunitySessionResponse(boolean loggedIn, Long userId, String nickname) {

    public static CommunitySessionResponse anonymous() {
        return new CommunitySessionResponse(false, null, null);
    }

    public static CommunitySessionResponse of(CommunityPrincipal principal) {
        return new CommunitySessionResponse(true, principal.getUserId(), principal.getNickname());
    }
}
