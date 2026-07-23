package com.kraft.community.auth;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프런트가 로그인 상태를 조회하는 개인화 엔드포인트. §4.4에 따라 항상
 * private, no-store — 공용 캐시(Caddy/PublicApiCacheControlFilter)에 절대 들어가지 않는다.
 * 익명 요청은 200 + loggedIn=false로 응답한다(permitAll, 세션 미생성 — §4.2 익명 무세션 규율).
 */
@RestController
@RequestMapping("/api/v1/community/session")
public class CommunitySessionController {

    @GetMapping
    public ResponseEntity<CommunitySessionResponse> session(@AuthenticationPrincipal CommunityPrincipal principal) {
        CommunitySessionResponse body = principal == null
                ? CommunitySessionResponse.anonymous()
                : CommunitySessionResponse.of(principal);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
    }
}
