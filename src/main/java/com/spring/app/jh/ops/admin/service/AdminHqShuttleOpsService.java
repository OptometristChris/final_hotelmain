package com.spring.app.jh.ops.admin.service;

import java.time.LocalDate;
import java.util.List;

import com.spring.app.jh.ops.admin.common.domain.HotelSimpleDTO;
import com.spring.app.jh.ops.admin.common.domain.ShuttleAdminTimetableDTO;
import com.spring.app.jh.ops.admin.common.domain.ShuttleBlockDTO;
import com.spring.app.jh.ops.admin.common.domain.ShuttlePlaceDTO;
import com.spring.app.jh.ops.admin.common.domain.ShuttleRouteDTO;

public interface AdminHqShuttleOpsService {

    List<HotelSimpleDTO> getHotelList();

    List<ShuttlePlaceDTO> getPlaceList();

    List<ShuttleRouteDTO> getRouteList(int hotelId);

    List<ShuttleAdminTimetableDTO> getTimetableList(int hotelId);

    List<ShuttleBlockDTO> getBlockList(int hotelId);

    int addRoute(int hotelId,
	            String routeType,
	            String startPlaceCode,
	            String endPlaceCode,
	            String routeName,
	            String departTime,
	            int capacity);
	
	int activateRoute(int hotelId, long routeId);

    int deactivateRoute(int hotelId, long routeId);

    int addBlock(int adminNo,
                 int hotelId,
                 long routeId,
                 Long timetableId,
                 LocalDate startDate,
                 LocalDate endDate,
                 String reason);

    int deactivateBlock(int hotelId, long blockId);

    int purgeOldShuttleData(int hotelId);
}