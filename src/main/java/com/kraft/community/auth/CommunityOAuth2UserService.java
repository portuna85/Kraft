package com.kraft.community.auth;

import com.kraft.community.user.CommunityUser;
import com.kraft.community.user.CommunityUserRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Google/Naver 사용자 정보를 정규화(CommunityOAuthAttributes)한 뒤 (provider, providerId)
 * 기준으로 CommunityUser에 upsert한다. 동시 최초가입 경합은 unique 제약 위반을 잡아
 * 기존 행으로 재조회해 수렴시킨다(Blitz의 "동시 가입 경합 → 재조회 수렴" 방식과 동일).
 */
@Service
public class CommunityOAuth2UserService extends DefaultOAuth2UserService {

    private final CommunityUserRepository communityUserRepository;
    private final Clock clock;

    public CommunityOAuth2UserService(CommunityUserRepository communityUserRepository, Clock clock) {
        this.communityUserRepository = communityUserRepository;
        this.clock = clock;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        if (oAuth2User == null) {
            throw new OAuth2AuthenticationException("OAuth2 사용자 정보를 가져오지 못했습니다.");
        }
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        CommunityOAuthAttributes attributes = CommunityOAuthAttributes.of(registrationId, oAuth2User.getAttributes());

        CommunityUser user = upsert(attributes);

        return new CommunityPrincipal(user.getId(), user.getNickname(), attributes.nameAttributeKey(),
                oAuth2User.getAttributes());
    }

    @Transactional
    CommunityUser upsert(CommunityOAuthAttributes attributes) {
        return communityUserRepository.findByProviderAndProviderId(attributes.provider(), attributes.providerId())
                .orElseGet(() -> createNewUser(attributes));
    }

    private CommunityUser createNewUser(CommunityOAuthAttributes attributes) {
        try {
            return communityUserRepository.save(new CommunityUser(
                    attributes.provider(),
                    attributes.providerId(),
                    attributes.nickname(),
                    attributes.profileImageUrl(),
                    OffsetDateTime.now(clock)));
        } catch (DataIntegrityViolationException concurrentSignup) {
            return communityUserRepository
                    .findByProviderAndProviderId(attributes.provider(), attributes.providerId())
                    .orElseThrow(() -> concurrentSignup);
        }
    }
}
