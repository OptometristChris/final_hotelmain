package com.spring.app.jh.security.auth.service;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spring.app.jh.security.auth.domain.AdminLoginRequestDTO;
import com.spring.app.jh.security.auth.domain.MemberLoginRequestDTO;
import com.spring.app.jh.security.auth.domain.TokenRequestDTO;
import com.spring.app.jh.security.domain.AdminDTO;
import com.spring.app.jh.security.domain.MemberDTO;
import com.spring.app.jh.security.domain.RefreshTokenDTO;
import com.spring.app.jh.security.domain.Session_AdminDTO;
import com.spring.app.jh.security.domain.Session_MemberDTO;
import com.spring.app.jh.security.jwt.JwtToken;
import com.spring.app.jh.security.jwt.JwtTokenProvider;
import com.spring.app.jh.security.model.AdminDAO;
import com.spring.app.jh.security.model.MemberDAO;
import com.spring.app.jh.security.model.RefreshTokenDAO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/* ===== (#JWT-SERVICE-02) ===== */

@Service
@Transactional
public class JwtAuthService_imple implements JwtAuthService {

    // =====================================================================
    // 0) 의존 객체 주입
    // =====================================================================
    /*
        [회원/관리자 인증]
        - AuthenticationManager Bean 을 2개 만들면 Spring Security 전역 충돌이 날 수 있으므로
          현재 구조에서는 DaoAuthenticationProvider 를 직접 사용한다.

        [JWT]
        - JwtTokenProvider 를 통해 access token / refresh token 을 발급한다.

        [refresh token 저장]
        - RefreshTokenDAO 로 DB 저장/조회/갱신/삭제를 수행한다.
     */
    private final DaoAuthenticationProvider memberAuthProvider;
    private final DaoAuthenticationProvider adminAuthProvider;

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenDAO refreshTokenDAO;

    private final MemberDAO memberDAO;
    private final AdminDAO adminDAO;

    // 생성자를 직접 작성하여 어떤 Bean 을 넣을지 명확하게 지정한다.
    public JwtAuthService_imple(
            @Qualifier("memberAuthProvider") DaoAuthenticationProvider memberAuthProvider,
            @Qualifier("adminAuthProvider") DaoAuthenticationProvider adminAuthProvider,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenDAO refreshTokenDAO,
            MemberDAO memberDAO,
            AdminDAO adminDAO) {

        this.memberAuthProvider = memberAuthProvider;
        this.adminAuthProvider = adminAuthProvider;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenDAO = refreshTokenDAO;
        this.memberDAO = memberDAO;
        this.adminDAO = adminDAO;
    }

    // =====================================================================
    // 1) 회원 로그인
    // =====================================================================
    @Override
    public JwtToken loginMember(MemberLoginRequestDTO loginDto,
                                HttpServletRequest request,
                                HttpServletResponse response) {

        Authentication authentication = memberAuthProvider.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getMemberid(), loginDto.getPasswd())
        );

        MemberDTO member = memberDAO.findByMemberid(loginDto.getMemberid());
        if (member == null) {
            throw new RuntimeException("회원 정보를 찾을 수 없습니다.");
        }

        return issueMemberToken(authentication, member, request, response);
    }

    // =====================================================================
    // 2) 관리자 로그인
    // =====================================================================
    @Override
    public JwtToken loginAdmin(AdminLoginRequestDTO loginDto,
                               HttpServletRequest request,
                               HttpServletResponse response) {

        Authentication authentication = adminAuthProvider.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getAdminid(), loginDto.getPasswd())
        );

        AdminDTO admin = adminDAO.findByAdminid(loginDto.getAdminid());
        if (admin == null) {
            throw new RuntimeException("관리자 정보를 찾을 수 없습니다.");
        }

        Long hotelId = null;
        if (admin.getFk_hotel_id() != null) {
            hotelId = Long.valueOf(admin.getFk_hotel_id());
        }

        JwtToken jwtToken = jwtTokenProvider.generateToken(
                authentication,
                "ADMIN",
                Long.valueOf(admin.getAdmin_no()),
                admin.getName(),
                admin.getAdmin_type(),
                hotelId
        );

        upsertRefreshToken("ADMIN",
                           Long.valueOf(admin.getAdmin_no()),
                           admin.getAdminid(),
                           jwtToken.getRefreshToken());

        saveAuthenticationToSession(authentication, request);
        saveAdminSessionDto(admin, request);

        return jwtToken;
    }

    // =====================================================================
    // 3) 소셜로그인 성공 후 JWT 발급(이미 인증 완료된 회원용)
    // =====================================================================
    @Override
    public JwtToken issueMemberToken(Authentication authentication,
                                     MemberDTO member,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {

        if (authentication == null) {
            throw new RuntimeException("인증 객체가 없습니다.");
        }

        if (member == null || member.getMemberNo() == null) {
            throw new RuntimeException("회원 정보가 올바르지 않습니다.");
        }

        JwtToken jwtToken = jwtTokenProvider.generateToken(
                authentication,
                "MEMBER",
                Long.valueOf(member.getMemberNo()),
                member.getName(),
                null,
                null
        );

        upsertRefreshToken("MEMBER",
                           Long.valueOf(member.getMemberNo()),
                           member.getMemberid(),
                           jwtToken.getRefreshToken());

        saveAuthenticationToSession(authentication, request);
        saveMemberSessionDto(member, request);

        return jwtToken;
    }

    // =====================================================================
    // 4) 토큰 재발급
    // =====================================================================
    @Override
    public JwtToken refresh(TokenRequestDTO tokenRequestDto) {

        if (!jwtTokenProvider.validateToken(tokenRequestDto.getRefreshToken())) {
            throw new RuntimeException("유효하지 않은 refresh token 입니다.");
        }

        Map<String, Object> accessInfo = jwtTokenProvider.getTokenInfo(tokenRequestDto.getAccessToken());
        Map<String, Object> refreshInfo = jwtTokenProvider.getTokenInfo(tokenRequestDto.getRefreshToken());

        String principalType = (String) accessInfo.get("principalType");
        Long principalNo = (Long) accessInfo.get("principalNo");
        String loginId = (String) accessInfo.get("loginId");

        String refreshPrincipalType = (String) refreshInfo.get("principalType");
        Long refreshPrincipalNo = (Long) refreshInfo.get("principalNo");

        if (!principalType.equals(refreshPrincipalType) || !principalNo.equals(refreshPrincipalNo)) {
            throw new RuntimeException("access token 과 refresh token 의 사용자 정보가 일치하지 않습니다.");
        }

        RefreshTokenDTO savedToken = refreshTokenDAO.selectRefreshTokenByPrincipal(principalType, principalNo);

        if (savedToken == null) {
            throw new RuntimeException("저장된 refresh token 이 없습니다.");
        }

        if (!"N".equals(savedToken.getRevokedYn())) {
            throw new RuntimeException("이미 무효화된 refresh token 입니다.");
        }

        if (!savedToken.getTokenValue().equals(tokenRequestDto.getRefreshToken())) {
            throw new RuntimeException("refresh token 이 일치하지 않습니다.");
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(tokenRequestDto.getAccessToken());

        String name = null;
        String adminType = null;
        Long hotelId = null;

        if ("MEMBER".equals(principalType)) {
            MemberDTO member = memberDAO.findByMemberid(loginId);
            if (member != null) {
                name = member.getName();
            }
        }
        else if ("ADMIN".equals(principalType)) {
            AdminDTO admin = adminDAO.findByAdminid(loginId);
            if (admin != null) {
                name = admin.getName();
                adminType = admin.getAdmin_type();

                if (admin.getFk_hotel_id() != null) {
                    hotelId = Long.valueOf(admin.getFk_hotel_id());
                }
            }
        }

        JwtToken newJwtToken = jwtTokenProvider.generateToken(
                authentication,
                principalType,
                principalNo,
                name,
                adminType,
                hotelId
        );

        upsertRefreshToken(principalType,
                           principalNo,
                           loginId,
                           newJwtToken.getRefreshToken());

        return newJwtToken;
    }

    // =====================================================================
    // 5) 로그아웃
    // =====================================================================
    @Override
    public void logout(String principalType,
                       Long principalNo,
                       HttpServletRequest request,
                       HttpServletResponse response) {

        if (principalNo != null) {
            refreshTokenDAO.deleteRefreshToken(principalType, principalNo);
        }

        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    // =====================================================================
    // 6) refresh token insert / update 공통처리
    // =====================================================================
    private void upsertRefreshToken(String principalType,
                                    Long principalNo,
                                    String loginId,
                                    String refreshToken) {

        Date expiresAt = new Date(System.currentTimeMillis() + jwtTokenProvider.getRefreshTokenExpirationMs());

        RefreshTokenDTO exists = refreshTokenDAO.selectRefreshTokenByPrincipal(principalType, principalNo);

        RefreshTokenDTO dto = RefreshTokenDTO.builder()
                .principalType(principalType)
                .principalNo(principalNo)
                .loginId(loginId)
                .tokenValue(refreshToken)
                .expiresAt(expiresAt)
                .revokedYn("N")
                .build();

        if (exists == null) {
            refreshTokenDAO.insertRefreshToken(dto);
        }
        else {
            refreshTokenDAO.updateRefreshToken(dto);
        }
    }

    // =====================================================================
    // 7) 하이브리드 구조용 세션 저장
    // =====================================================================
    private void saveAuthenticationToSession(Authentication authentication,
                                             HttpServletRequest request) {

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", context);
    }

    private void saveMemberSessionDto(MemberDTO member, HttpServletRequest request) {

        HttpSession session = request.getSession(true);

        Session_MemberDTO smd = new Session_MemberDTO();
        smd.setMemberNo(member.getMemberNo());
        smd.setMemberid(member.getMemberid());
        smd.setName(member.getName());

        session.setAttribute("sessionMemberDTO", smd);
    }

    private void saveAdminSessionDto(AdminDTO admin, HttpServletRequest request) {

        HttpSession session = request.getSession(true);

        Session_AdminDTO sad = new Session_AdminDTO();
        sad.setAdmin_no(admin.getAdmin_no());
        sad.setAdminid(admin.getAdminid());
        sad.setName(admin.getName());
        sad.setAdmin_type(admin.getAdmin_type());
        sad.setFk_hotel_id(admin.getFk_hotel_id());

        session.setAttribute("sessionAdminDTO", sad);
    }
}
