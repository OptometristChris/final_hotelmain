package com.spring.app.jh.ops.admin.common.domain;

import lombok.Data;

@Data
public class AdminDashboardKpiDTO {

    private String occupancyRate;   // 예: 72.35
    private String monthlySales;    // 예: 12000000
    private String cancelRate;      // 예: 4.20
    private String revpar;          // 예: 168000
}