package com.spring.app.js.cs.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CsDTO {
    // QUESTIONS 테이블 관련
    private String qna_id;        // 문의번호
    private String fk_hotel_id;   // 호텔ID (1:시엘, 2:르시엘)
    private String fk_userid;     // 작성자아이디
    private String writer_name;   // 작성자명
    private String title;         // 제목
    private String content;       // 내용
    private String is_secret;     // 비밀글 여부 (Y/N)
    private String status;        // 답변상태 (PENDING/ANSWERED)
    private String created_at;    // 작성일
    
    // 조인 시 답변(ANSWERS) 내용을 담기 위한 필드
    private String ans_content;   // 관리자 답변 내용
    private String ans_date;      // 답변 일자
}