package com.spring.app.jh.ops.user.domain;

import java.util.List;

import lombok.Data;

@Data
public class ShuttleConfirmCardDTO {

    private Long reservationId;
    private Long shuttleBookingId;

    private String hotelName;
    private String roomName;
    private String roomImageUrl;

    private String reservationStatus;

    private int guestCount;

    private String checkinText;
    private String checkoutText;

    private boolean showTo;
    private boolean showFrom;

    private String toRideDateText;
    private String fromRideDateText;

    private List<ShuttleConfirmLegItemDTO> toLegs;
    private List<ShuttleConfirmLegItemDTO> fromLegs;

}
