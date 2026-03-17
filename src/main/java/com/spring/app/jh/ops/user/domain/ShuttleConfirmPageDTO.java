package com.spring.app.jh.ops.user.domain;

import java.util.List;

import lombok.Data;

@Data
public class ShuttleConfirmPageDTO {

    private String memberName;
    private int validCount;
    private List<ShuttleConfirmCardDTO> cards;

}
