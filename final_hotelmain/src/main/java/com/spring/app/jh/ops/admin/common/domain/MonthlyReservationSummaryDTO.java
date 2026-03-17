package com.spring.app.jh.ops.admin.common.domain;

import lombok.Data;

@Data
public class MonthlyReservationSummaryDTO {

    // 이번달 결제완료 예약 건수
    private Integer paidCount;

    // 전월 결제완료 예약 건수
    private Integer prevPaidCount;

    // 이번달 - 전월
    private Integer paidDiffCount;

    // 전월 대비 증감률(%)
    private String paidDiffRate;

    // 이번달 취소 예약 건수
    private Integer cancelCount;

    // 전월 취소 예약 건수
    private Integer prevCancelCount;

    // 이번달 - 전월
    private Integer cancelDiffCount;

    // 전월 대비 증감률(%)
    private String cancelDiffRate;

    // 이번달 순예약(결제완료 - 취소)
    private Integer netCount;
}