package com.spring.app.jh.security.jwt;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/* ===== (#JWT-03) ===== */

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // =====================================================================
    // 0) JwtTokenProvider 주입
    // =====================================================================
    /*
        이 필터는 매 요청마다 실행되면서
        Authorization 헤더의 Bearer 토큰을 확인하고,
        JWT 가 유효하면 Authentication 을 복원하여 SecurityContext 에 넣는다.
     */
    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }


    // =====================================================================
    // 1) 필터 핵심 로직
    // =====================================================================
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String accessToken = null;

        // 1. 먼저 Authorization 헤더에서 토큰 확인
        String bearerToken = request.getHeader("Authorization");
        accessToken = jwtTokenProvider.resolveToken(bearerToken);

        // 2. 헤더에 없으면 쿠키에서 accessToken 확인
        if (accessToken == null) {
            accessToken = resolveTokenFromCookie(request, "accessToken");
        }

        // 3. 토큰이 유효하면 SecurityContext 에 인증 저장
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveTokenFromCookie(HttpServletRequest request, String cookieName) {

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}