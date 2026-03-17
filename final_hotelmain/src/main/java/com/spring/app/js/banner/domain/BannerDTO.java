package com.spring.app.js.banner.domain;

import lombok.Data;

@Data
public class BannerDTO {
    private int banner_id;      // PK
    private int fk_hotel_id;    // 지점 ID
    private String title;       // 제목
    private String promo_type;  // 프로모션 타입 (이것을 subtitle로 활용 가능)
    private int price;          // 가격
    private String start_date;  // 시작일
    private String end_date;    // 종료일
    private String image_url;   // 이미지 경로
    private String room_type;   // 객실 타입
    private String active_yn;   // 활성화 여부
    
    // 기존 코드에서 사용하던 필드 (Mapper에서 Alias로 맞춰줄 예정)
    private String subtitle;    
    private String link_url;    
    private int priority;       
}