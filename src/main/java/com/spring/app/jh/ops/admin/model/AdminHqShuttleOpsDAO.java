package com.spring.app.jh.ops.admin.model;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.spring.app.jh.ops.admin.common.domain.HotelSimpleDTO;
import com.spring.app.jh.ops.admin.common.domain.ShuttleAdminTimetableDTO;
import com.spring.app.jh.ops.admin.common.domain.ShuttleBlockDTO;
import com.spring.app.jh.ops.admin.common.domain.ShuttlePlaceDTO;
import com.spring.app.jh.ops.admin.common.domain.ShuttleRouteDTO;

@Mapper
public interface AdminHqShuttleOpsDAO {

    List<HotelSimpleDTO> selectHotelList();

    List<ShuttlePlaceDTO> selectPlaceList();

    List<ShuttleRouteDTO> selectRouteList(@Param("hotelId") int hotelId);

    List<ShuttleAdminTimetableDTO> selectTimetableList(@Param("hotelId") int hotelId);

    List<ShuttleBlockDTO> selectBlockList(@Param("hotelId") int hotelId);

    int insertRoute(@Param("hotelId") int hotelId,
                    @Param("routeType") String routeType,
                    @Param("startPlaceCode") String startPlaceCode,
                    @Param("endPlaceCode") String endPlaceCode,
                    @Param("routeName") String routeName);

    int deactivateRoute(@Param("hotelId") int hotelId,
                        @Param("routeId") long routeId);

    int deactivateTimetableByRoute(@Param("hotelId") int hotelId,
                                   @Param("routeId") long routeId);

    int insertBlock(@Param("adminNo") int adminNo,
                    @Param("hotelId") int hotelId,
                    @Param("routeId") long routeId,
                    @Param("timetableId") Long timetableId,
                    @Param("startDate") LocalDate startDate,
                    @Param("endDate") LocalDate endDate,
                    @Param("reason") String reason);

    int deactivateBlock(@Param("hotelId") int hotelId,
                        @Param("blockId") long blockId);

    int deleteOldBooking(@Param("hotelId") int hotelId,
                         @Param("cutoffDate") LocalDate cutoffDate);

    int deleteOldSlotStock(@Param("hotelId") int hotelId,
                           @Param("cutoffDate") LocalDate cutoffDate);
}