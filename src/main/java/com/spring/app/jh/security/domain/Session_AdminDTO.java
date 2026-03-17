package com.spring.app.jh.security.domain;

import lombok.Data;

@Data
public class Session_AdminDTO {

	private Integer admin_no;
    private String adminid;
    private String name;
    private String admin_type; // HQ / BRANCH
    private Integer fk_hotel_id;  // BRANCH면 NOT NULL, HQ면 null
}
