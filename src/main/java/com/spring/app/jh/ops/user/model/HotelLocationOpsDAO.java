package com.spring.app.jh.ops.user.model;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.spring.app.jh.ops.user.domain.HotelLocationDTO;

@Mapper
public interface HotelLocationOpsDAO {

    List<HotelLocationDTO> selectActiveHotelLocationList();
}
