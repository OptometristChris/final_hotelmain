package com.spring.app.hk.room.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.spring.app.hk.room.model.RoomStockDAO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomStockService_imple implements RoomStockService {

    private final RoomStockDAO roomStockDAO;

    // 재고차감 + 재고 0시 롤백 처리 (예약 막기)
    @Override
    public int decreaseStockByDateRange(int roomId, LocalDate checkIn, LocalDate checkOut) {

        int totalUpdated = 0;
        LocalDate date = checkIn;

        while(date.isBefore(checkOut)) {

        	// 재고 잠금 (FOR UPDATE)
        	int stock = roomStockDAO.selectStockForUpdate(roomId, date);
        	
        	// 재고 부족 체크
        	 if(stock <= 0) {
                 throw new RuntimeException("재고 부족 날짜: " + date);
             }
        	
        	// 재고 차감
            totalUpdated += roomStockDAO.decreaseStock(roomId, date);

            date = date.plusDays(1);
        }

        return totalUpdated;
    }
}