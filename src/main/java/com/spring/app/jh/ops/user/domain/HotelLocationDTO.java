package com.spring.app.jh.ops.user.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class HotelLocationDTO {

    private Long hotelId;
    private String hotelName;
    private String address;
    private String contact;
    private String hotelDesc;
    private Double latitude;
    private Double longitude;
    private String mainImageUrl;
}
