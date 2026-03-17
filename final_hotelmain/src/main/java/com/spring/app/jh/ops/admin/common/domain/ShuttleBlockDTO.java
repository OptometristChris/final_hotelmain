package com.spring.app.jh.ops.admin.common.domain;

import java.time.LocalDate;

import lombok.Data;

@Data
public class ShuttleBlockDTO {

    private Long shuttleBlockId;       // 차단 PK
    private Integer fkHotelId;         // 호텔 번호
    private Long fkRouteId;            // 노선 FK
    private Long fkTimetableId;        // 시간표 FK (null 가능)

    private String hotelName;          // 호텔명
    private String routeType;          // TO_HOTEL / FROM_HOTEL
    private String routeName;          // 노선명

    private String startPlaceCode;     // 출발지 코드
    private String startPlaceName;     // 출발지명
    private String endPlaceCode;       // 도착지 코드
    private String endPlaceName;       // 도착지명

    private String departTime;         // 특정 시간표 차단일 때 출발시간
    private LocalDate blockStartDate;  // 차단 시작일
    private LocalDate blockEndDate;    // 차단 종료일
    private String reason;             // 사유
    private String activeYn;           // 활성여부
    private Integer createdByAdmin;    // 등록 관리자 번호
}