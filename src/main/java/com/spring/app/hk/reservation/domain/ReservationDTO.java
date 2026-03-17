package com.spring.app.hk.reservation.domain;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ReservationDTO {

    private int reservation_id;
    private int member_no;
    private int room_type_id;

    private LocalDate checkin_date;
    private LocalDate checkout_date;

    private int guest_count;
    private int total_price;

    private String reservation_status;
}