package com.bombadle.security.oauth2;

import com.bombadle.entity.Player;
import com.bombadle.service.player.PlayerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends OidcUserService {
    private final PlayerService playerService;
    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getAttribute("email");
        Object nameAttribute = oidcUser.getAttribute("name");
        if (email == null) {
            OAuth2Error error = new OAuth2Error("missing_email", "No email address found in OAuth2 profile", null);
            throw new OAuth2AuthenticationException(error);
        }

        if (nameAttribute == null) {
            OAuth2Error error = new OAuth2Error("missing_name", "No username found in OAuth2 profile", null);
            throw new OAuth2AuthenticationException(error);
        }

        Player player = playerService.findByEmail(email.toLowerCase())
                .orElseGet( () -> playerService.registerOAuth2Player(
                        email.toLowerCase(),
                        nameAttribute.toString()
                        )
                );

        return new CustomOAuth2PlayerUser(oidcUser, player);
    }
}
