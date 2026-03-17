package com.spring.app.jh.security.loginsuccess;

import java.io.IOException;
import java.io.PrintWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import com.spring.app.jh.security.domain.CustomUserDetails;
import com.spring.app.jh.security.domain.MemberDTO;
import com.spring.app.jh.security.domain.Session_MemberDTO;
import com.spring.app.jh.security.oauth.OAuth2MemberPrincipal;
import com.spring.app.jh.security.service.MemberService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.Setter;

/* ===== (#스프링시큐리티15) ===== */
// 로그인 성공후 세션 및 쿠키등의 부가적인 처리를 할 수 있도록 해주는 클래스를 작성한다.

/*
   [로그인 전 정보를 Cache 하도록 해야한다.]
   ==> 로그인 되지 않은 상태에서 로그인 상태에서만 사용할 수 있는 페이지로 이동할 경우에는
       로그인 인증 페이지로 이동하고, 로그인이 성공 되어진 후에는 로그인 하기 전의 페이지로 이동하도록 한다.

   ※ 여기서 말하는 "Cache"는 우리가 직접 세션에 prevURLPage 같은 것을 저장하는 방식이 아니라,
      Spring Security가 제공하는 SavedRequest 기능을 의미한다.

   - 사용자가 로그인 없이 보호자원에 접근하면, Spring Security가 "원래 가려던 URL"을 SavedRequest로 저장한다.
   - 로그인 성공 시, SavedRequestAwareAuthenticationSuccessHandler가 SavedRequest를 읽어
     "원래 가려던 페이지"로 자동 redirect 한다.
   - SavedRequest가 없으면, 우리가 설정한 defaultTargetUrl로 이동한다.
*/

// @RequiredArgsConstructor
@Getter
@Setter
public class MemberAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

	@Autowired  // 필드주입
	private MemberService memberService;

	public MemberAuthenticationSuccessHandler() { }

	public MemberAuthenticationSuccessHandler(String defaultTargetUrl) {
		super.setDefaultTargetUrl(defaultTargetUrl);
	}

	//===================================================================================================//

	/*
	   Spring Security 는 로그인이 성공한 뒤에 우리가 해야할 부가적인 작업들은
	   public void onAuthenticationSuccess() 메서드를 재정의 하여 기술하면 되도록 되어있다.

	   onAuthenticationSuccess() 메서드는 3가지 파라미터를 가진다.
	       첫번째  HttpServletRequest  request  : request.getParameter() 등으로 요청값을 읽을 수 있다.
	       두번째  HttpServletResponse response : response.getWriter() 등으로 응답/출력/redirect를 제어할 수 있다.
	       세번째  Authentication      authentication : 인증 성공 후 principal(로그인 사용자 정보)을 얻을 수 있다.

	   예를 들어 회원별 방문수를 증가시키거나, 마지막 로그인 날짜를 기록하거나,
	   로그인 히스토리를 쌓는 작업 등이 모두 여기에서 가능하다.

	   여기서 우리는 아래와 같이 4가지를 하도록 하겠다.
	   1. 비밀번호 변경이 6개월 이상 지났으면 "비밀번호 변경"을 권장(확인창)한다.
	   2. DB에 마지막 로그인 날짜를 현재일자로 업데이트한다.
	   3. tbl_loginhistory 테이블에 로그인한 사용자 정보를 insert 한다.
	      ※ 우리 프로젝트 DDL 기준으로 tbl_loginhistory 는 memberid 가 아니라 member_no(FK) 로 저장한다.
	         (즉, 로그인 히스토리 insert 시 member_no 를 함께 넘겨야 한다.)
	   4. 로그인 성공 후 이동 URL 처리를 한다.

	      >>> 로그인 성공 후 이동 URL 처리 방식(추천) <<<
	      - Spring Security의 SavedRequest 기능을 그대로 사용한다.
	      - SavedRequest가 있으면: 로그인 전 접근하려던 페이지로 자동 이동한다.
	      - SavedRequest가 없으면: defaultTargetUrl로 이동한다.
	      (따로 prevURLPage 같은 값을 우리가 세션에 저장해서 redirect 하는 방식은 제거한다.)
	 */

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request
			                          , HttpServletResponse response
			                          , Authentication authentication) throws IOException, ServletException {

		Object principal = authentication.getPrincipal();
		MemberDTO memberDto = null;
		String memberid = null;
		int n = 0;

		if (principal instanceof CustomUserDetails customUserDetails) {
			memberid = customUserDetails.getUsername();
			memberDto = memberService.findByMemberid(memberid);
			n = memberService.lastPasswdChangeMonth(memberid);
			memberService.update_last_login(memberid);
		}
		else if (principal instanceof OAuth2MemberPrincipal oauth2MemberPrincipal) {
			memberDto = oauth2MemberPrincipal.getMemberDto();
			memberid = memberDto.getMemberid();
			memberService.updateLastLoginByMemberNo(memberDto.getMemberNo());
		}
		else {
			super.onAuthenticationSuccess(request, response, authentication);
			return;
		}

		Session_MemberDTO sessionMemberDTO = new Session_MemberDTO();
		sessionMemberDTO.setMemberid(memberDto.getMemberid());
		sessionMemberDTO.setName(memberDto.getName());
		sessionMemberDTO.setMemberNo(memberDto.getMemberNo());

		HttpSession session = request.getSession();
		session.removeAttribute("Session_GuestDTO");
		session.setAttribute("sessionMemberDTO", sessionMemberDTO);

		String clientip = request.getRemoteAddr();
		memberService.insertLoginhistory(memberDto.getMemberNo(), clientip);

		boolean shouldAskPasswordChange = memberid != null
				&& memberDto.getSocialProvider() == null
				&& n >= 6;

		if (shouldAskPasswordChange) {

			/*
			   ★ URL 하드코딩 제거 ★
			   - localhost, 포트, context-path를 코드에 박아두면 배포/환경 변경시 깨진다.
			   - request.getContextPath()를 사용하면 현재 프로젝트의 컨텍스트패스를 자동 반영한다.
			 */
			String ctxPath = request.getContextPath();

			response.setContentType("text/html; charset=UTF-8");
			PrintWriter out = response.getWriter();
			// out 은 웹브라우저에 기술하는 대상체라고 생각하자.

			out.println("<html>");
			out.println("<head>"
					  + "<script type='text/javascript'>"
					  + "if (confirm('" + n + "개월 동안 비밀번호를 변경하지 않았습니다.\\n비밀번호 변경을 권장합니다!!.\\n비밀번호 변경 페이지로 이동할까요?'))"
					  + "{location.href='" + ctxPath + "/security/passwdChange';}"
					  + "else"
					  + "{location.href='" + ctxPath + "/index';}"
					  + "</script>"
					  + "</head>");
			out.println("<body>");
			out.println("</body>");
			out.println("</html>");
			out.flush();
			return; // confirm 응답으로 이동하므로 여기서 종료

		}
		// 로그인한 사용자 정보에서 비밀번호를 변경한 날짜가 현재일로 부터 6개월 이내 이라면
		else {
			/*
			   >>> 화면에 보여질 페이지를 지정하도록 한다. <<<
			   - SavedRequestAwareAuthenticationSuccessHandler의 기본 로직을 그대로 사용한다.

			   1) SavedRequest(원래 가려던 페이지)가 있으면 그쪽으로 redirect
			   2) SavedRequest가 없으면 defaultTargetUrl로 redirect
			 */
			super.onAuthenticationSuccess(request, response, authentication);
		}

	}// end of onAuthenticationSuccess()-------------------------------------

}