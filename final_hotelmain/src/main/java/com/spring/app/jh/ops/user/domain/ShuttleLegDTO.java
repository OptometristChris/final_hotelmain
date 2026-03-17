package com.spring.app.jh.ops.user.domain;

import java.util.Date;
import lombok.Data;

@Data
public class ShuttleLegDTO {
    private Long shuttleLegId;
    private Long shuttleBookingId;

    private Long timetableId;
    private String legType;

    private Date rideDate;      // ★ 레그별 탑승일
    private String placeCode;
    private String departTime;

    private Integer ticketQty;
    private String legStatus;   // BOOKED / CANCELED
}