package com.spring.app.ih.dining.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiningReservationDTO {
    
    // 예약 PK
    private Long diningReservationId;
    
    // 외래키들
    private Long fkHotelId;
    private Long diningId;           // DiningDTO의 PK와 연결될 ID
    private Long tableId;
    private Long fkMemberNo;         // 회원번호 (비회원은 null)

    // 예약자 정보 (비회원 예약 필수)
    private String guestName;
    private String guestPhone;
    private String guestEmail;
    private String resPassword;      // 비회원 조회용 비밀번호

    // 인원 및 일시
    private Integer adultCount;
    private Integer childCount = 0;
    private Integer infantCount = 0;
    private LocalDate resDate;       // 예약 날짜
    private String resTime;          // 예약 시간 (예: "12:00", "18:30")

    // 기타 정보
    private String specialRequests;  // CLOB 대응
    private String allergyInfo;      // CLOB 대응
    private String status;           // 'PENDING', 'CONFIRMED' 등
    
    private Long paymentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String diningName;
    
    private String requestTerm;
    
}