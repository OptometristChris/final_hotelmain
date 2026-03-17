package com.spring.app.hk.room.service;

import java.time.LocalDate;

public interface RoomStockService {
	
	// 재고 차감
	int decreaseStockByDateRange(int roomId, LocalDate checkIn, LocalDate checkOut);
}
