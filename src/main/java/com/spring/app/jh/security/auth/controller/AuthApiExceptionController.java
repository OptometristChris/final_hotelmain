package com.spring.app.jh.security.auth.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/* ===== (#JWT-CONTROLLER-04) ===== */
/*
    Auth API 전용 예외를 JSON 형태로 내려주기 위한 예외 처리기이다.

    [왜 필요한가?]
    - 기존 formLogin() 시절에는 failureUrl 로 redirect 하면 되었지만,
      지금은 로그인/재발급을 AJAX 로 호출하므로
      실패 응답도 JSON 으로 내려주는 것이 프론트 처리에 유리하다.
 */
@RestControllerAdvice(basePackages = "com.spring.app.jh.security.auth.controller")
public class AuthApiExceptionController {

    // =====================================================================
    // 1) 아이디/비밀번호 불일치
    // =====================================================================
    /*
        AuthenticationManager.authenticate(...) 과정에서
        비밀번호가 틀리거나 인증 실패가 발생하면 BadCredentialsException 이 발생할 수 있다.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException e) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", "아이디 또는 비밀번호가 올바르지 않습니다.");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }


    // =====================================================================
    // 2) RuntimeException 처리
    // =====================================================================
    /*
        현재 1차 단계에서는 로그인 실패/토큰 오류 등을 RuntimeException 으로 먼저 처리한다.
        추후에는 커스텀 예외 클래스로 세분화할 수 있다.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }


    // =====================================================================
    // 3) 기타 예외 처리
    // =====================================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", "인증 처리 중 오류가 발생했습니다.");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}