package com.spring.app.jh.ops.user.service;

import java.util.List;

import com.spring.app.jh.ops.user.domain.HotelLocationDTO;

public interface HotelLocationOpsService {

    List<HotelLocationDTO> getActiveHotelLocationList();
}
