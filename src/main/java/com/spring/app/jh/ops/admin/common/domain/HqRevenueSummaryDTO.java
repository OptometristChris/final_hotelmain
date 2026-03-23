package com.spring.app.jh.ops.admin.common.domain;

import lombok.Data;

@Data
public class HqRevenueSummaryDTO {

    private Long totalRevenue;        // 총 매출액
    private Integer totalCount;       // 예약 건수
    private Double occupancyRate;     // 평균 객실 점유율
}