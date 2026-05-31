package com.bombadle.security.filter;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.security.oauth2.CustomOAuth2PlayerUser;
import com.bombadle.service.stats.ActivityTrackingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ActivityTrackingFilterTest {

    @InjectMocks
    private ActivityTrackingFilter filter;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private PlayerPrincipal playerPrincipal;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CustomOAuth2PlayerUser customOAuth2PlayerUser;

    @Mock
    private ActivityTrackingService activityTrackingService;

    private HttpServletRequest request;
    private HttpServletResponse response;

    private final UUID anonymousSessionId = UUID.randomUUID();

    private final Cookie anonymousCookie = new Cookie("ANON_SESSION_ID", anonymousSessionId.toString());

    private final Cookie[] cookies = new Cookie[]{anonymousCookie};

    private final Cookie invalidAnonymousCookie = new Cookie("ANON_SESSION_ID", "costam");

    @BeforeEach
    void setUp() {
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_AuthenticatedUser_MarksPlayerActiveAndContinuesChain() throws ServletException, IOException {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(playerPrincipal);
        when(playerPrincipal.getPlayerId()).thenReturn(1L);

        filter.doFilterInternal(request, response, filterChain);

        verify(activityTrackingService).markPlayerActive(1L);
        verify(filterChain).doFilter(request, response);
    }


    @Test
    void doFilterInternal_OAuth2AuthenticatedUser_MarksPlayerAsActiveAndContinuesChain() throws ServletException, IOException {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(customOAuth2PlayerUser);
        when(customOAuth2PlayerUser.getPlayer().getId()).thenReturn(1L);

        filter.doFilterInternal(request, response, filterChain);

        verify(activityTrackingService).markPlayerActive(customOAuth2PlayerUser.getPlayer().getId());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_AuthenticatedUserAsSpringAnonymousUserWithAnonymousCookie_MarksAnonymousAsActiveAndContinuesChain() throws ServletException, IOException {
        AnonymousAuthenticationToken anonymousAuth = new AnonymousAuthenticationToken(
                "some-key",
                "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
        );
        when(securityContext.getAuthentication()).thenReturn(anonymousAuth);
        when(request.getCookies()).thenReturn(cookies);

        filter.doFilterInternal(request, response, filterChain);

        verify(activityTrackingService).markAnonymousActive(anonymousSessionId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_UnauthenticatedUserWithValidAnonymousCookie_MarksAnonymousActiveAndContinuesChain() throws ServletException, IOException {
        when(securityContext.getAuthentication()).thenReturn(null);
        when(request.getCookies()).thenReturn(cookies);

        filter.doFilterInternal(request, response, filterChain);

        verify(activityTrackingService).markAnonymousActive(anonymousSessionId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_UnauthenticatedUserWithOutAnonymousCookie_ContinuesChain() throws ServletException, IOException {
        when(securityContext.getAuthentication()).thenReturn(null);
        when(request.getCookies()).thenReturn(new Cookie[]{});

        filter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(activityTrackingService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_UnauthenticatedUserWithInvalidAnonymousCookie_ContinuesChain() throws ServletException, IOException {
        when(securityContext.getAuthentication()).thenReturn(null);
        when(request.getCookies()).thenReturn(new Cookie[]{invalidAnonymousCookie});
        filter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(activityTrackingService);
        verify(filterChain).doFilter(request, response);
    }



}
