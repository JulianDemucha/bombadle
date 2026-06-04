package com.bombadle.security.oauth2;

import com.bombadle.config.ApplicationConfigProperties;
import com.bombadle.config.PlayerPrincipal;
import com.bombadle.entity.Player;
import com.bombadle.service.auth.PostLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OAuth2SuccessHandlerTest {

    @InjectMocks
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @Mock
    private PlayerPrincipal playerPrincipal;

    @Mock
    private CustomOAuth2PlayerUser customOAuth2PlayerUser;

    @Mock
    private PostLoginService postLoginService;

    @Mock
    private Authentication authentication;

    @Mock
    private ApplicationConfigProperties.FrontendConfig frontendConfig;

    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
    }

    @Nested
    class OnAuthenticationSuccessTests {

        @Test
        void onAuthenticationSuccess_oAuth2PrincipalType_success() throws IOException {
            // Arrange
            when(authentication.getPrincipal()).thenReturn(customOAuth2PlayerUser);
            when(customOAuth2PlayerUser.getPlayer()).thenReturn(Player.builder().build());
            when(frontendConfig.baseUrl()).thenReturn("https://sigma.com");

            // Act
            oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

            // Assert
            verify(response).sendRedirect("https://sigma.com/login-success");
            verify(postLoginService).processSuccessfulLogin(request, response, customOAuth2PlayerUser.getPlayer());
        }

        @Test
        void onAuthenticationSuccess_otherPrincipalType_throwsIllegalStateException() {
            // Arrange
            when(authentication.getPrincipal()).thenReturn(playerPrincipal);

            // Act & Assert
            assertThrows(IllegalStateException.class,
                    () -> oAuth2SuccessHandler.onAuthenticationSuccess(
                            request,
                            response,
                            authentication
                    )
            );
        }
    }
}