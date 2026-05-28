package com.bombadle.security.oauth2;

import com.bombadle.entity.Player;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.enums.Role;
import com.bombadle.repository.PlayerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends OidcUserService {
    private final PlayerRepository playerRepository;
    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getAttribute("email");
        Player player = playerRepository.findByEmail(email)
                .orElseGet(() -> {
                    Player p = Player.builder()
                            .login(Objects.requireNonNull(oidcUser.getAttribute("name")).toString().replace('\u00A0', ' ').strip())
                            .email(email)
                            .passwordHash("")
                            .role(Role.ROLE_USER)
                            .createdAt(Instant.now())
                            .lastActiveAt(Instant.now())
                            .hasGuessedToday(false)
                            .avatarImage(AvatarImage.AVATAR_DEFAULT)
                            .authProvider(PlayerAuthProvider.OAUTH2_GOOGLE)
                            .build();
                    log.info(Objects.requireNonNull(oidcUser.getAttribute("name")).toString().trim());
                    return playerRepository.save(p);
                });
        player.setLastActiveAt(Instant.now());

        return new CustomOAuth2PlayerUser(oidcUser, player);
    }
}
