package com.spring.app.jh.ops.admin.common.domain;

import lombok.Data;

@Data
public class BranchDashboardQnaDTO {

    private Long qnaId;         // 질문 PK
    private String title;       // 질문 제목
    private String dateText;    // 2026.02.11
    private String refText;     // 작성자 홍길동
}