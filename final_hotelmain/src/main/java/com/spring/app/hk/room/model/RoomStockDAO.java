package com.spring.app.hk.room.model;

import java.time.LocalDate;

public interface RoomStockDAO {

	// ======= 예약 처리용 ========= //
    // 재고 잠금 (FOR UPDATE)
    int selectStockForUpdate(int room_type_id, LocalDate stay_date);

    // 재고 차감
    int decreaseStock(int room_type_id, LocalDate stay_date);

}