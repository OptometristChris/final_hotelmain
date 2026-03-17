package com.spring.app.hk.admin.hotel.domain;

import lombok.Data;
import java.util.Date;

@Data
public class HotelDTO {

    private int hotel_id;
    private String hotel_name;

    private String address;
    private Double latitude;
    private Double longitude;
    private String contact;
    private String hotel_desc;

    private String approve_status;
    private String reject_reason;

    private String active_yn;
    private String created_by;
    private Date created_at;
}