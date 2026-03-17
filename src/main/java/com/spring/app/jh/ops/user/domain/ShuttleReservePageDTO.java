package com.spring.app.jh.ops.user.domain;

import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class ShuttleReservePageDTO {
    private ReservationForShuttleDTO reservation;

    private Date toRideDate;       // checkin
    private Date fromRideDate;     // checkout

    private List<ShuttleTimetableDTO> toList;
    private List<ShuttleTimetableDTO> fromList;

    private Long shuttleBookingId;
    private ShuttleLegDTO toBookedLeg;
    private ShuttleLegDTO fromBookedLeg;
}