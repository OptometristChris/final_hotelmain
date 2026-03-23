package com.spring.app.jh.ops.admin.common.domain;

import lombok.Data;

@Data
public class BranchDashboardReservationDTO {

    private String reservationNo;   // A000123 형태 표시용
    private String memberName;      // 회원명
    private String roomTypeName;    // 객실명
    private String checkinDate;     // 2026.03.20
    private String checkoutDate;    // 2026.03.21
    private Integer guestCount;     // 인원
    private String totalPrice;      // 650,000
    private String statusCode;      // CHECKIN_TODAY / CHECKOUT_TODAY / STAYING / CONFIRMED / CANCELLED
    private String statusText;      // 화면 표시 문구
}