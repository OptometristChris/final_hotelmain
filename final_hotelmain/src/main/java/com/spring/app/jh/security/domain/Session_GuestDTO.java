package com.spring.app.jh.security.domain;

import lombok.Data;

@Data
public class Session_GuestDTO {

    private Integer memberNo;   // 게스트(또는 매칭된 MEMBER)의 member_no
    private String guestName;   // 화면 표시용
    private String guestPhone;  // 화면 표시용(원문/사용자 입력값)
    private String lookupKey;   // SHA-256 key (정규화 기반)
}