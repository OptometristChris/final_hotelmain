package com.spring.app.jh.security.jwt;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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

        /*
            1. 요청 헤더에서 Authorization 값을 꺼낸다.
            2. "Bearer " 접두사를 제거하여 실제 JWT 문자열을 얻는다.
            3. 토큰이 유효하면 Authentication 을 복원한다.
            4. SecurityContextHolder 에 Authentication 을 저장한다.
            5. 다음 필터로 넘긴다.
         */
        String bearerToken = request.getHeader("Authorization");
        String accessToken = jwtTokenProvider.resolveToken(bearerToken);

        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {

            Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);

            /*
                인증 객체를 SecurityContext 에 저장해두면
                이후 Controller, Service, sec:authorize, 권한체크 등에서
                "현재 로그인 사용자"로 인식할 수 있다.
             */
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}