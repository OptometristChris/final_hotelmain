package com.spring.app.jh.ops.admin.common.domain;

import lombok.Data;

@Data
public class ShuttlePlaceDTO {

    private String placeCode;          // 장소 코드
    private String placeName;          // 장소명
    private String activeYn;           // 활성여부
}