package com.spring.app.jh.ops.admin.common.domain;

import lombok.Data;

@Data
public class ShuttleRouteDTO {

    private Long routeId;              // 노선 PK
    private Integer fkHotelId;         // 호텔 번호
    private String hotelName;          // 호텔명

    private String routeType;          // TO_HOTEL / FROM_HOTEL

    private String startPlaceCode;     // 출발지 코드
    private String startPlaceName;     // 출발지명

    private String endPlaceCode;       // 도착지 코드
    private String endPlaceName;       // 도착지명

    private String routeName;          // 노선명
    private String activeYn;           // 활성여부(Y/N)
}