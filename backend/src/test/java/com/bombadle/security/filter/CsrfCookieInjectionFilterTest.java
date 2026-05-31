package com.bombadle.security.filter;

import com.bombadle.service.auth.CsrfCookieService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CsrfCookieInjectionFilterTest {

    @Mock
    private CsrfCookieService csrfCookieService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private CsrfCookieInjectionFilter filter;

    @Test
    void doFilterInternal_invokesCsrfServiceAndContinuesChain() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);

        verify(csrfCookieService).ensureCsrfCookie(request, response);

        verify(filterChain).doFilter(request, response);
    }
}
