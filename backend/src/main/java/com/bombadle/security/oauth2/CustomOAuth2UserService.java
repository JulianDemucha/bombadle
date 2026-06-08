package com.bombadle.security.oauth2;

import com.bombadle.entity.Player;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.enums.Role;
import com.bombadle.service.auth.AuthenticationService;
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

import java.time.Instant;

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
                .orElseGet(() -> registerOAuth2Player(
                        email.toLowerCase(),
                        nameAttribute.toString()
                ));

        if (!player.getEmailVerified()) {
            log.info("Player {} logged in via OAuth2. Auto-verifying unverified LOCAL account email.", player.getLogin());
            playerService.activateAccount(player.getId());
        }

        return new CustomOAuth2PlayerUser(oidcUser, player);
    }


    private Player registerOAuth2Player(String email, String rawName) {
        String cleanName = rawName.replace('\u00A0', ' ').strip();
        String uniqueLogin = generateUniqueLogin(cleanName);

        Player newPlayer = Player.builder()
                .displayName(cleanName)
                .login(uniqueLogin)
                .email(email.toLowerCase())
                .passwordHash("")
                .role(Role.ROLE_USER)
                .createdAt(Instant.now())
                .lastActiveAt(Instant.now())
                .hasGuessedToday(false)
                .accountLocked(false)
                .emailVerified(true)
                .avatarImage(AvatarImage.AVATAR_DEFAULT)
                .authProvider(PlayerAuthProvider.OAUTH2_GOOGLE)
                .build();

        return playerService.save(newPlayer);
    }

    private String generateUniqueLogin(String baseName) {
        String login = baseName.toLowerCase();
        int counter = 1;
        while (playerService.existsByLogin(login)) {
            login = baseName.toLowerCase() + counter;
            counter++;
        }
        return login;
    }
}