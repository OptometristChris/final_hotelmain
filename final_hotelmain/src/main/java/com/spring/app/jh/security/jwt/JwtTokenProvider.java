package com.spring.app.jh.security.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.spring.app.jh.security.auth.domain.JwtPrincipalDTO;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/* ===== (#JWT-02) ===== */

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    private static final long ACCESS_TOKEN_EXPIRATION_MS  = 1000L * 60 * 30;           // 30분
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 1000L * 60 * 60 * 24 * 14; // 14일

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public JwtToken generateToken(Authentication authentication,
                                  String principalType,
                                  Long principalNo,
                                  String name,
                                  String adminType,
                                  Long hotelId) {

        String authorities = authentication.getAuthorities().stream()
                                           .map(GrantedAuthority::getAuthority)
                                           .collect(Collectors.joining(","));

        String loginId = authentication.getName();

        long now = (new Date()).getTime();

        Date accessTokenExpiresIn  = new Date(now + ACCESS_TOKEN_EXPIRATION_MS);
        Date refreshTokenExpiresIn = new Date(now + REFRESH_TOKEN_EXPIRATION_MS);

        String accessToken = Jwts.builder()
                .subject(loginId)
                .claim("principalType", principalType)
                .claim("principalNo", principalNo)
                .claim("name", name)
                .claim("roles", authorities)
                .claim("adminType", adminType)
                .claim("hotelId", hotelId)
                .issuedAt(new Date(now))
                .expiration(accessTokenExpiresIn)
                .signWith(getSigningKey())
                .compact();

        String refreshToken = Jwts.builder()
                .subject(loginId)
                .claim("principalType", principalType)
                .claim("principalNo", principalNo)
                .issuedAt(new Date(now))
                .expiration(refreshTokenExpiresIn)
                .signWith(getSigningKey())
                .compact();

        return JwtToken.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpireTime(accessTokenExpiresIn.getTime())
                .principalType(principalType)
                .adminType(adminType)
                .hotelId(hotelId)
                .build();
    }

    public Authentication getAuthentication(String accessToken) {

        Claims claims = parseClaims(accessToken);

        String loginId = claims.getSubject();
        String principalType = claims.get("principalType", String.class);

        Number principalNoNumber = claims.get("principalNo", Number.class);
        Long principalNo = (principalNoNumber != null) ? principalNoNumber.longValue() : null;

        String name = claims.get("name", String.class);
        String roles = claims.get("roles", String.class);
        String adminType = claims.get("adminType", String.class);

        Number hotelIdNumber = claims.get("hotelId", Number.class);
        Long hotelId = (hotelIdNumber != null) ? hotelIdNumber.longValue() : null;

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream((roles != null ? roles : "").split(","))
                      .filter(role -> role != null && !role.isBlank())
                      .map(SimpleGrantedAuthority::new)
                      .collect(Collectors.toList());

        JwtPrincipalDTO principal = JwtPrincipalDTO.builder()
                .principalType(principalType)
                .principalNo(principalNo)
                .loginId(loginId)
                .name(name)
                .adminType(adminType)
                .hotelId(hotelId)
                .roles(authorities.stream()
                                  .map(GrantedAuthority::getAuthority)
                                  .collect(Collectors.toList()))
                .build();

        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);

            return true;
        }
        catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /*
        [중요]
        refresh 처리 시에는 "만료된 access token" 에서도
        principalType, principalNo, loginId 같은 식별정보를 읽어야 한다.

        따라서 access token 이 만료되었더라도
        ExpiredJwtException 에서 claims 를 꺼내 반환하도록 한다.
     */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                       .verifyWith(getSigningKey())
                       .build()
                       .parseSignedClaims(token)
                       .getPayload();
        }
        catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public String resolveToken(String bearerToken) {

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    public Map<String, Object> getTokenInfo(String token) {

        Claims claims = parseClaims(token);

        Number principalNoNumber = claims.get("principalNo", Number.class);

        return Map.of(
                "loginId", claims.getSubject(),
                "principalType", claims.get("principalType", String.class),
                "principalNo", principalNoNumber != null ? principalNoNumber.longValue() : null
        );
    }

    public long getAccessTokenExpirationMs() {
        return ACCESS_TOKEN_EXPIRATION_MS;
    }

    public long getRefreshTokenExpirationMs() {
        return REFRESH_TOKEN_EXPIRATION_MS;
    }
}
