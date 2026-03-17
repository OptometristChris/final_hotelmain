package com.spring.app.jh.security.domain;

import lombok.Data;
// import lombok.Getter;
// import lombok.Setter;

/* ===== (#스프링시큐리티04) ===== */

/*
   Session_SecurityMemberDTO 클래스는 HttpSession(세션)에 저장할 클래스로서,
   SecurityMemberDTO 의 일부 필드 정보만 가진 클래스이다.
   
   SecurityMemberDTO 의 필드중 
   private String memberid;  // 회원아이디
   private String name;      // 회원명
   만 사용하겠다.
*/

// @Getter // private 으로 설정된 필드 변수를 외부에서 접근하여 사용하도록 getter()메소드를 만들어 주는 것.
// @Setter // private 으로 설정된 필드 변수를 외부에서 접근하여 수정하도록 setter()메소드를 만들어 주는 것.
// @AllArgsConstructor // 모든 필드 값을 파라미터로 받는 생성자를 만들어주는 것
// @NoArgsConstructor  // 파라미터가 없는 기본생성자를 만들어주는 것
@Data  // lombok 에서 사용하는 @Data 어노테이션은 @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequiredArgsConstructor 를 모두 합쳐놓은 종합선물세트인 것이다. 
public class Session_MemberDTO {
 
	private String memberid;
	private String name;
	private Integer memberNo;
		
}
