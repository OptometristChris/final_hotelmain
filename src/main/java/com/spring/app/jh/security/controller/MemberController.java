package com.spring.app.jh.security.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.spring.app.jh.security.auth.domain.JwtPrincipalDTO;
import com.spring.app.jh.security.auth.service.JwtAuthService;
import com.spring.app.jh.security.domain.CustomUserDetails;
import com.spring.app.jh.security.domain.MemberDTO;
import com.spring.app.jh.security.domain.Session_MemberDTO;
import com.spring.app.jh.security.oauth.OAuth2MemberPrincipal;
import com.spring.app.jh.security.service.MemberService;
import com.spring.app.js.promotion.domain.PromotionDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/* ===== (#스프링시큐리티07) ===== */
@Controller
@RequiredArgsConstructor
@RequestMapping(value="/security/")
public class MemberController {

    private final JwtAuthService jwtAuthService;
    private final MemberService memberService;
    // private final PasswordEncoder passwordEncoder; 	// ★ 제거


    // 회원가입 form 페이지
    @GetMapping("memberRegister")
    public String memberRegister(HttpServletRequest request) {

        // 이미 로그인 했다면 인덱스로
        HttpSession session = request.getSession(false);
        if(session != null &&
           (session.getAttribute("sessionMemberDTO") != null
            || session.getAttribute("guestSession") != null
            || session.getAttribute("sessionAdminDTO") != null)) {
            return "redirect:/index";
        }

        return "security/login/memberRegisterForm";
        // src/main/resources/templates/security/login/memberRegisterForm.html 파일 생성해줘야 함
    }


    // 약관 iframe
    @GetMapping("agree")
    public String memberAgree(){

        return "security/login/memberAgree";
        // src/main/resources/templates/security/login/memberAgree.html 파일 생성해줘야 함
    }


    // 아이디 중복검사
    @PostMapping("member_id_check")
    @ResponseBody
    public Map<String, Boolean> member_id_check(@RequestParam(name="memberid") String memberid){

        int n = memberService.member_id_check(memberid);

        boolean isExists = false;
        if (n == 1) {
            isExists = true;
        }

        Map<String, Boolean> map = new HashMap<>();
        map.put("isExists", isExists);

        return map;
    }


    // email 중복검사
    @PostMapping("emailDuplicateCheck")
    @ResponseBody
    public Map<String, Boolean> emailDuplicateCheck(@RequestParam(name="email") String email){

        int n = memberService.emailDuplicateCheck(email);

        boolean isExists = false;
        if (n == 1) {
            isExists = true;
        }

        Map<String, Boolean> map = new HashMap<>();
        map.put("isExists", isExists);

        return map;
    }


    // 회원가입, DB에 insert 하는 것
    @PostMapping("memberRegisterEnd")
    public String memberRegisterEnd(MemberDTO memberdto, Model model) {

        try {
            int n = memberService.insert_member(memberdto);

            if (n == 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("<span style='font-weight: bold;'>")
                  .append(memberdto.getName())
                  .append("</span>님의 회원 가입이 정상적으로 처리되었습니다.<br/>");
                sb.append("메인메뉴에서 로그인 하시기 바랍니다.<br/>");

                model.addAttribute("message", sb.toString());
            }
            else {
                model.addAttribute("message", "장애가 발생되어 회원가입이 실패했습니다.");
            }
        }
        catch (Exception e) {
            model.addAttribute("message", "장애가 발생되어 회원가입이 실패했습니다.");
            e.printStackTrace();
        }

        return "security/login/memberRegisterComplete";
    }


    // 로그인 인증 form 페이지 보여주기
    @GetMapping(value="login")
    public String login(HttpServletRequest request){

        /*
          ★ SavedRequest 방식 사용 ★
          - 로그인 이전에 보호자원에 접근했다면 Spring Security 가 SavedRequest 로 "원래 가려던 URL" 을 저장한다.
          - 로그인 성공 시 MemberAuthenticationSuccessHandler(SavedRequestAwareAuthenticationSuccessHandler)가
            SavedRequest 를 읽어 원래 가려던 페이지로 자동 redirect 한다.
          - 따라서 여기서 referer 를 세션에 prevURLPage 로 저장하는 방식은 제거한다.
        */

        // ✅ 응답이 커밋되기 전에 세션 생성
        request.getSession(true);

        // ✅ CSRF 미리 생성
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) token.getToken();

        // 이미 일반회원, 어드민, 게스트로 로그인 했다면 인덱스로
        HttpSession session = request.getSession(false);
        if(session != null &&
           (session.getAttribute("sessionMemberDTO") != null
            || session.getAttribute("guestSession") != null
            || session.getAttribute("sessionAdminDTO") != null)) {
            return "redirect:/index";
        }

        // login 실패여부 체크하기
        String loginFail = request.getParameter("loginFail");

        String msg = "";

        if("true".equals(loginFail)) {
            msg = "로그인 실패!! 아이디 또는 암호를 잘못 입력하셨습니다.";
        }

        request.setAttribute("msg", msg);

        return "security/login/loginform";
        // src/main/resources/templates/security/login/loginform.html 파일 생성해줘야 함
    }


    // id 찾기 form
    @GetMapping("member_id_find")
    public String memberIdFindForm(HttpServletRequest request) {

        return "security/login/member_id_find";
        // src/main/resources/templates/security/login/member_id_find.html
    }


    // id 찾기 처리
    @PostMapping("member_id_find")
    public String memberIdFindEnd(@RequestParam Map<String, String> paraMap,
                                  Model model) {

        String memberid = memberService.findMemberId(paraMap);

        if (memberid == null || memberid.trim().isEmpty()) {
            model.addAttribute("isFound", false);
            model.addAttribute("message", "일치하는 회원 정보가 없습니다. 입력 정보를 다시 확인하세요.");
        }
        else {
            model.addAttribute("isFound", true);
            model.addAttribute("memberid", memberid);
        }

        return "security/login/member_id_find_result";
        // src/main/resources/templates/security/login/member_id_find_result.html
    }


    // 비밀번호 찾기 form
    @GetMapping("member_pw_find")
    public String memberPwFindForm(HttpServletRequest request) {

        return "security/login/member_pw_find";
        // src/main/resources/templates/security/login/member_pw_find.html
    }


    // 비밀번호 찾기 처리
    @PostMapping("member_pw_find")
    public String memberPwFindEnd(@RequestParam Map<String, String> paraMap,
                                  Model model) {

        boolean isVerified = memberService.verifyMemberForPwReset(paraMap);

        if (!isVerified) {
            model.addAttribute("isSuccess", false);
            model.addAttribute("message", "입력하신 정보와 일치하는 회원이 없습니다.");
            return "security/login/member_pw_find_result";
        }

        boolean ok = memberService.issueTempPasswordAndSendEmail(paraMap);

        if (!ok) {
            model.addAttribute("isSuccess", false);
            model.addAttribute("message", "입력하신 정보와 일치하는 회원이 없거나 이메일 발송에 실패했습니다.");
        }
        else {
            model.addAttribute("isSuccess", true);
            model.addAttribute("message", "임시 비밀번호를 이메일로 발송했습니다. 메일 확인 후 로그인하세요.");
        }

        return "security/login/member_pw_find_result";
        // src/main/resources/templates/security/login/member_pw_find_result.html
    }


    // 인증 실패시 URL
    @GetMapping(value="noAuthenticated")
    public String noAuthenticated(){

        return "security/noAuthenticated";
        // src/main/resources/templates/security/noAuthenticated.html 파일 생성해줘야 함
    }


    // 권한 실패시 URL
    @GetMapping(value="noAuthorized")
    public String noAuthorized(){

        return "security/noAuthorized";
        // src/main/resources/templates/security/noAuthorized.html 파일 생성해줘야 함
    }


    // 비밀번호 변경 form 페이지 보여주기
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value="passwdChange")
    public String passwdChange(){

        return "security/member/passwdChangeForm";
        // src/main/resources/templates/security/member/passwdChangeForm.html 파일 생성해줘야 함
    }


    // 비밀번호 변경 하기
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value="passwdChange")
    public String passwdChange(@RequestParam Map<String, String> paraMap, Model model){

        int result = memberService.passwdChange(paraMap);
        model.addAttribute("result", result);

        return "security/member/passwdChangeResult";
        // src/main/resources/templates/security/member/passwdChangeResult.html 파일 생성해줘야 함
    }


    // 마이페이지로
    @PreAuthorize("hasRole('USER')")
    @GetMapping("member/mypage")
    public String mypage(HttpSession session, Model model) {

        Integer memberNo = resolveMemberNo(session);
        if (memberNo == null) {
            return "redirect:/security/login";
        }

        MemberDTO memberDto = memberService.findByMemberNo(memberNo);
        model.addAttribute("memberDto", memberDto);

        List<PromotionDTO> promoList = memberService.selectMypagePromoList();
        model.addAttribute("promoList", promoList);
        
        boolean isSocialMember =
                memberDto != null &&
                memberDto.getSocialProvider() != null &&
                !memberDto.getSocialProvider().trim().isEmpty();

        model.addAttribute("isSocialMember", isSocialMember);

        if (isSocialMember) {
            String snsName = switch (memberDto.getSocialProvider().toUpperCase()) {
                case "KAKAO" -> "카카오";
                case "NAVER" -> "네이버";
                case "GOOGLE" -> "구글";
                default -> memberDto.getSocialProvider();
            };
            model.addAttribute("snsName", snsName);
        }

        boolean isKakaoWithoutEmail =
                memberDto != null
                && "KAKAO".equalsIgnoreCase(memberDto.getSocialProvider())
                && (memberDto.getEmail() == null || memberDto.getEmail().trim().isEmpty());

        model.addAttribute("isKakaoWithoutEmail", isKakaoWithoutEmail);

        boolean showKakaoEmailReminderOnce = Boolean.TRUE.equals(session.getAttribute("showKakaoEmailReminderOnce"));
        model.addAttribute("showKakaoEmailReminderOnce", showKakaoEmailReminderOnce);

        if (showKakaoEmailReminderOnce) {
            session.removeAttribute("showKakaoEmailReminderOnce"); // 1회만
        }

        return "security/member/mypage";
    }


    // 회원정보 수정 폼 페이지
    @PreAuthorize("hasRole('USER')")
    @GetMapping("member/profileEdit")
    public String profileEdit(HttpSession session, Model model) {

        Integer memberNo = resolveMemberNo(session);
        if (memberNo == null) {
            return "redirect:/security/login";
        }

        MemberDTO memberdto = memberService.findByMemberNo(memberNo);
        model.addAttribute("memberdto", memberdto);
        model.addAttribute("memberDto", memberdto);

        boolean canEditEmail =
                memberdto != null
                && "KAKAO".equalsIgnoreCase(memberdto.getSocialProvider())
                && (memberdto.getEmail() == null || memberdto.getEmail().trim().isEmpty());

        model.addAttribute("canEditEmail", canEditEmail);

        return "security/member/profileEditForm";
    }


    // 회원정보 수정 저장
    @PreAuthorize("hasRole('USER')")
    @PostMapping("member/profileEdit")
    public String profileEditEnd(MemberDTO memberdto,
                                 HttpSession session,
                                 Model model) {

        Integer memberNo = resolveMemberNo(session);
        if(memberNo == null) {
            return "redirect:/security/login";
        }

        MemberDTO originMember = memberService.findByMemberNo(memberNo);

        boolean canEditEmail =
                originMember != null
                && "KAKAO".equalsIgnoreCase(originMember.getSocialProvider())
                && (originMember.getEmail() == null || originMember.getEmail().trim().isEmpty());

        memberdto.setMemberNo(memberNo);   // ★ 수정 대상 회원 PK 세팅

        int result = memberService.update_member_profile(memberdto, canEditEmail);
        model.addAttribute("result", result);

        return "security/member/profileEditResult";
    }


    // 회원탈퇴 form
    @PreAuthorize("hasRole('USER')")
    @GetMapping("member/withdraw")
    public String withdrawForm(HttpSession session, Model model) {

        Integer memberNo = resolveMemberNo(session);
        if (memberNo == null) {
            return "redirect:/security/login";
        }

        MemberDTO memberDto = memberService.findByMemberNo(memberNo);
        model.addAttribute("memberDto", memberDto);

        return "security/member/withdrawForm";
    }


    // 회원탈퇴 처리
    @PreAuthorize("hasRole('USER')")
    @PostMapping("api/auth/member/withdraw")
    @ResponseBody
    public Map<String, Object> withdrawMember(HttpSession session,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {

        Map<String, Object> resultMap = new HashMap<>();

        Integer memberNo = resolveMemberNo(session);

        if (memberNo == null) {
            resultMap.put("success", false);
            resultMap.put("message", "로그인 정보가 없습니다.");
            return resultMap;
        }

        int n = memberService.disable_member(memberNo);

        if (n == 1) {

            // refresh token 삭제 + security context 정리 + session 무효화
            jwtAuthService.logout("MEMBER", Long.valueOf(memberNo), request, response);

            resultMap.put("success", true);
            resultMap.put("message", "회원탈퇴가 정상 처리되었습니다.");
        }
        else {
            resultMap.put("success", false);
            resultMap.put("message", "회원탈퇴 처리에 실패했습니다.");
        }

        return resultMap;
    }


    // 누구나 접근 가능한 URL
    @GetMapping(value="everybody")
    public String everybody(){

        return "security/everybody";
        // src/main/resources/templates/security/everybody.html 파일 생성해줘야 함
    }


    // 회원만 접근 가능한 URL
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value="authenticatedUserOnly")
    public String authenticatedUserOnly(){

        return "security/authenticatedUserOnly";
        // src/main/resources/templates/security/authenticatedUserOnly.html 파일 생성해줘야 함
    }


    // 특별회원 권한이 있는 URL
    @PreAuthorize("hasAnyRole('ADMIN','USER_SPECIAL')")
    @GetMapping(value="special/special_memberOnly")
    public String special_memberOnly(){

        return "security/special_memberOnly";
        // src/main/resources/templates/security/special_memberOnly.html 파일 생성해줘야 함
    }


    private Integer resolveMemberNo(HttpSession session) {

        if (session == null) return null;

        Object obj = session.getAttribute("sessionMemberDTO");
        if (obj instanceof Session_MemberDTO dto) {
            return dto.getMemberNo();
        }

        SecurityContext context =
                (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");

        if (context != null && context.getAuthentication() != null) {
            Object principal = context.getAuthentication().getPrincipal();

            if (principal instanceof OAuth2MemberPrincipal oauth2Principal) {
                return oauth2Principal.getMemberDto().getMemberNo();
            }

            if (principal instanceof CustomUserDetails customUserDetails) {
                return customUserDetails.getMemberDto().getMemberNo();
            }

            if (principal instanceof JwtPrincipalDTO jwtPrincipal) {
                if ("MEMBER".equals(jwtPrincipal.getPrincipalType())) {
                    return jwtPrincipal.getPrincipalNo().intValue();
                }
            }
        }

        return null;
    }

}