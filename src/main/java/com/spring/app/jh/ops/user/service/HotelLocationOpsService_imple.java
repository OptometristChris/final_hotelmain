package com.spring.app.jh.ops.user.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.spring.app.jh.ops.user.domain.HotelLocationDTO;
import com.spring.app.jh.ops.user.model.HotelLocationOpsDAO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HotelLocationOpsService_imple implements HotelLocationOpsService {

    private final HotelLocationOpsDAO hotelLocationOpsDAO;

    @Override
    public List<HotelLocationDTO> getActiveHotelLocationList() {
        return hotelLocationOpsDAO.selectActiveHotelLocationList();
    }
}
