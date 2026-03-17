package com.spring.app.jh.ops.admin.common.domain;

import lombok.Data;

@Data
public class ShuttleAdminTimetableDTO {

    private Long timetableId;          // 시간표 PK
    private Long fkRouteId;            // 노선 FK
    private Integer fkHotelId;         // 호텔 번호

    private String hotelName;          // 호텔명
    private String routeType;          // TO_HOTEL / FROM_HOTEL

    private String routeName;          // 노선명
    private String startPlaceCode;     // 출발지 코드
    private String startPlaceName;     // 출발지명
    private String endPlaceCode;       // 도착지 코드
    private String endPlaceName;       // 도착지명

    private String departTime;         // 출발시간
    private Integer capacity;          // 정원
    private String activeYn;           // 활성여부
}