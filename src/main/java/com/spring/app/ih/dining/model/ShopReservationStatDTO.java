package com.spring.app.ih.dining.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ShopReservationStatDTO {
	private int diningId;
    private String shopName;      // 매장명
    private int totalCount;       // 오늘 총 예약 건수
    private int visitedCount;     // 오늘 방문 완료 건수
    private int currentPeople;    // 오늘 총 예약 인원 (성인+소아+유아 합계)
    private int maxCapacity;      // 매장 최대 수용 인원 

    private Integer slotId;        
    private String resTime;        
    private int slotCapacity;      // MAX_SLOT_CAPACITY 해당 시간대 제한 인원
    private int maxTotal;          // 일일 총 인원
}