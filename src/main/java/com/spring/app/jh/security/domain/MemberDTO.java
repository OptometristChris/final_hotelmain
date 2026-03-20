package com.spring.app.jh.security.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/* ===== (#스프링시큐리티03) ===== */
//== Lombok 라이브러리에서 제공하는 어노테이션 == 
//@Getter                   // private 으로 설정된 필드 변수를 외부에서 접근하여 사용하도록 getter()메소드를 만들어 주는 것.
//@Setter                   // private 으로 설정된 필드 변수를 외부에서 접근하여 수정하도록 setter()메소드를 만들어 주는 것.
//@ToString                 // 객체를 문자열로 표현할 때 사용
//@RequiredArgsConstructor  // final 필드 또는 @NonNull이 붙은 필드에 대해 이 필드만 포함하는 생성자를 자동으로 생성해준다.
//@AllArgsConstructor       // 모든 필드 값을 파라미터로 받는 생성자를 만들어주는 것
//@NoArgsConstructor        // 파라미터가 없는 기본생성자를 만들어주는 것
@Data
public class MemberDTO {

    // PK
    private Integer memberNo;   // ✅ 회원 PK(member_no) 추가
    
    // ID : 유니크
    private String memberid; 

    // 인증
    private String passwd;
    private String enabled; // '1' or '0'

    // 기본
    private String name;
    private String birthday;

    // 연락처(암호문 저장 전제)
    private String email;
    private String mobile;
    
    // 회원/비회원 구분 컬럼, 비회원일 시 이름+전화번호 키 조합으로 특정하는 컬럼 추가
    private String memberType;
    private String lookupKey;

    // 소셜 로그인
    private String socialProvider;
    private String providerUserId;
    // 화면/로그인 후처리용 임시 플래그
    private boolean socialJoinJustCreated;

    // 주소
    private String postcode;
    private String address;
    private String detail_address;
    private String extra_address;

    // 포인트/등급 (✅ 최종 DB 기준)
    private int point;
    private int point_earned_total;
    private String grade_code;

    // 일자
    private LocalDate registerday;
    private LocalDate passwd_modify_date;
    private LocalDateTime last_login_date;

    // 권한(ROLE 목록)
    private List<String> authorities;

    // 화면용 분해 필드(선택)
    private String hp1;
    private String hp2;
    private String hp3;
}