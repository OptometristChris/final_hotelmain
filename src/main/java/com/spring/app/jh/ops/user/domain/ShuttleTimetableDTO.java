package com.spring.app.jh.ops.user.domain;

import lombok.Data;

@Data
public class ShuttleTimetableDTO {

    private Long timetableId;      // tbl_shuttle_timetable.timetable_id
    private Long fkRouteId;        // tbl_shuttle_timetable.fk_route_id

    private Integer hotelId;       // tbl_shuttle_route.fk_hotel_id
    private String legType;        // tbl_shuttle_route.route_type
    private String routeName;      // tbl_shuttle_route.route_name

    private String placeCode;      // 출발지 코드
    private String placeName;      // 출발지명
    private String departTime;     // 출발시간

    private Integer capacity;      // 정원
    private Integer bookedQty;     // 예약 수량
    private Integer remaining;     // 잔여 수량
    private Integer myQty;         // 내 예약 수량
}