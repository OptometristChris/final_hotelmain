package com.spring.app.jh.security.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


import jakarta.servlet.http.HttpServletRequest;

/* ===== (#스프링시큐리티07-ADMIN-LOGIN) ===== */
@Controller
@RequestMapping(value="/admin/")
public class AdminLoginController {
	
	/*
	   관리자 로그인/접근실패 안내

	   - 관리자 로그인 폼(/admin/login)
	   - 인증 실패 안내(/admin/noAuthenticated)
	   - 권한 실패 안내(/admin/noAuthorized)

	   ※ 로그인 성공 후 세션 저장/redirect는
	      AdminAuthenticationSuccessHandler 에서 처리한다.
	*/


	// ============================================================
	// A. 관리자 로그인(폼/실패메시지)  ※ 인증처리는 Security FilterChain에서 수행
	// ============================================================

	// 관리자 로그인 form 페이지
	@GetMapping(value="login")
	public String login(HttpServletRequest request){
		

		/*
		   ★ SavedRequest 방식 사용 ★
		   - 로그인 전 /admin/** 보호자원 접근 시 SavedRequest 로 원래 URL이 저장된다.
		   - 로그인 성공 시 AdminAuthenticationSuccessHandler 가 SavedRequest 를 읽어 redirect 한다.
		   - SavedRequest 가 없으면 defaultTargetUrl 로 이동한다.
		*/

		// login 실패여부 체크하기
		String loginFail = request.getParameter("loginFail");

		String msg = "";

		if("true".equals(loginFail)) {
			msg = "로그인 실패!! 아이디 또는 암호를 잘못 입력하셨습니다.";
		}

		request.setAttribute("msg", msg);

		return "admin/login/loginform";
		// src/main/resources/templates/admin/login/loginform.html
	}


	// 인증 실패시 URL  /* ===== (#스프링시큐리티16-ADMIN) ===== */
	@GetMapping(value="noAuthenticated")
	public String noAuthenticated(){

		return "admin/noAuthenticated";
		// src/main/resources/templates/admin/noAuthenticated.html
	}


	// 권한 실패시 URL  /* ===== (#스프링시큐리티17-ADMIN) ===== */
	@GetMapping(value="noAuthorized")
	public String noAuthorized(){

		return "admin/noAuthorized";
		// src/main/resources/templates/admin/noAuthorized.html
	}

}