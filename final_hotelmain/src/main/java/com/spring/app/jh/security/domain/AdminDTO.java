package com.spring.app.jh.security.domain;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class AdminDTO {

    private Integer admin_no;        // ✅ 관리자 PK(admin_no) 추가
    private String adminid;       // UNIQUE: 관리자 로그인 ID
    private String passwd;
    private String enabled;       // '1'/'0'

    private String name;
    private String email;
    private String mobile;

    private String admin_type;    // 'HQ'/'BRANCH'
    private Integer fk_hotel_id;     // BRANCH일 때만 값 존재(HQ는 NULL)

    private LocalDate registerday;
    private LocalDate passwd_modify_date;
    private LocalDate last_login_date;

    private List<String> authorities;
}