package com.bombadle.security.oauth2;

import com.bombadle.entity.Player;
import com.bombadle.service.player.PlayerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomOAuth2UserServiceTest {
    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    @Mock
    private PlayerService playerService;


    @Test
    void loadUser_PlayerDoesNotExist_CreateAndSaveNewPlayer() {
        Map<String, Object> claims = Map.of("sub", "sigmaId123", "email", "sigma@sigma.sigma", "name", "sigma");
        OidcUserRequest userRequest = createOidcUserRequest(claims);
        when(playerService.findByEmail("sigma@sigma.sigma")).thenReturn(Optional.empty());

        Player dummyPlayer = Player.builder().email("sigma@sigma.sigma").build();
        when(playerService.registerOAuth2Player("sigma@sigma.sigma", "sigma")).thenReturn(dummyPlayer);

        OidcUser resultUser = customOAuth2UserService.loadUser(userRequest);

        verify(playerService).registerOAuth2Player("sigma@sigma.sigma", "sigma");
        assertNotNull(resultUser);

    }

    @Test
    void loadUser_PlayerExists_ReturnsExistingPlayer() {
        Map<String, Object> claims = Map.of("sub", "sigmaId123", "email", "sigma@sigma.sigma", "name", "sigma");
        OidcUserRequest userRequest = createOidcUserRequest(claims);
        when(playerService.findByEmail("sigma@sigma.sigma")).thenReturn(Optional.of(Player.builder().build()));

        OidcUser resultUser = customOAuth2UserService.loadUser(userRequest);

        verify(playerService, never()).registerOAuth2Player(anyString(), anyString());
        assertNotNull(resultUser);
    }

    @Test
    void loadUser_NoEmailInToken_ThrowsOAuth2AuthenticationException() {
        Map<String, Object> claims = Map.of("sub", "sigmaId123", "name", "sigma");
        OidcUserRequest userRequest = createOidcUserRequest(claims);
        assertThrows(OAuth2AuthenticationException.class, () -> customOAuth2UserService.loadUser(userRequest));

    }

    @Test
    void loadUser_NoNameInToken_ThrowsOAuth2AuthenticationException() {
        Map<String, Object> claims = Map.of("sub", "sigmaId123", "email", "sigma@sigma.sigma");
        OidcUserRequest userRequest = createOidcUserRequest(claims);
        assertThrows(OAuth2AuthenticationException.class, () -> customOAuth2UserService.loadUser(userRequest));

    }

    private OidcUserRequest createOidcUserRequest(Map<String, Object> claims) {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("google")
                .clientId("test-client-id")
                .clientSecret("test-secret")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("sigma")
                .authorizationUri("sigma")
                .tokenUri("boy")
                .build();

        Instant issuedAt = Instant.now();
        Instant expiresAt = Instant.now().plusSeconds(60);

        OidcIdToken idToken = new OidcIdToken("mock-token", issuedAt, expiresAt, claims);
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "mock-access",
                issuedAt,
                expiresAt
        );

        return new OidcUserRequest(clientRegistration, accessToken, idToken);
    }

}
