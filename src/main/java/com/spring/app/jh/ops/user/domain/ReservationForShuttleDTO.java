package com.spring.app.jh.ops.user.domain;

import java.util.Date;
import lombok.Data;

@Data
public class ReservationForShuttleDTO {
    private Long reservationId;
    private Integer memberNo;
    private Long roomTypeId;

    private Integer hotelId;
    private String hotelName;

    private Integer guestCount;
    private Date checkinDate;
    private Date checkoutDate;

    private String reservationStatus; // CONFIRMED

    // ===== confirm(view)에서 사용하는 확장 필드 =====
    private String roomName;
    private String roomImageUrl;
    private String memberName;
}