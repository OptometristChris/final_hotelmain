package com.spring.app.jh.ops.admin.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spring.app.jh.ops.admin.common.domain.HotelSimpleDTO;
import com.spring.app.jh.ops.admin.common.domain.ShuttleAdminTimetableDTO;
import com.spring.app.jh.ops.admin.common.domain.ShuttleBlockDTO;
import com.spring.app.jh.ops.admin.common.domain.ShuttlePlaceDTO;
import com.spring.app.jh.ops.admin.common.domain.ShuttleRouteDTO;
import com.spring.app.jh.ops.admin.model.AdminHqShuttleOpsDAO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminHqShuttleOpsService_imple implements AdminHqShuttleOpsService {

    private final AdminHqShuttleOpsDAO shuttleDao;

    @Override
    public List<HotelSimpleDTO> getHotelList() {
        return shuttleDao.selectHotelList();
    }

    @Override
    public List<ShuttlePlaceDTO> getPlaceList() {
        return shuttleDao.selectPlaceList();
    }

    @Override
    public List<ShuttleRouteDTO> getRouteList(int hotelId) {
        return shuttleDao.selectRouteList(hotelId);
    }

    @Override
    public List<ShuttleAdminTimetableDTO> getTimetableList(int hotelId) {
        return shuttleDao.selectTimetableList(hotelId);
    }

    @Override
    public List<ShuttleBlockDTO> getBlockList(int hotelId) {
        return shuttleDao.selectBlockList(hotelId);
    }

    @Override
    @Transactional
    public int addRoute(int hotelId,
                        String routeType,
                        String startPlaceCode,
                        String endPlaceCode,
                        String routeName,
                        String departTime,
                        int capacity) {

        if (hotelId <= 0) {
            throw new IllegalArgumentException("호텔 정보가 올바르지 않습니다.");
        }

        if (routeType == null || routeType.trim().isEmpty()) {
            throw new IllegalArgumentException("노선 유형은 필수입니다.");
        }

        if (startPlaceCode == null || startPlaceCode.trim().isEmpty()) {
            throw new IllegalArgumentException("출발지는 필수입니다.");
        }

        if (endPlaceCode == null || endPlaceCode.trim().isEmpty()) {
            throw new IllegalArgumentException("도착지는 필수입니다.");
        }

        if (routeName == null || routeName.trim().isEmpty()) {
            throw new IllegalArgumentException("노선명은 필수입니다.");
        }

        if (departTime == null || departTime.trim().isEmpty()) {
            throw new IllegalArgumentException("출발시간은 필수입니다.");
        }

        if (capacity <= 0) {
            throw new IllegalArgumentException("정원은 1 이상이어야 합니다.");
        }

        shuttleDao.insertRoute(hotelId, routeType, startPlaceCode, endPlaceCode, routeName);

        Long routeId = shuttleDao.selectLastRouteId(hotelId, routeType, startPlaceCode, endPlaceCode, routeName);

        if (routeId == null) {
            throw new IllegalStateException("생성된 노선 ID를 찾을 수 없습니다.");
        }

        shuttleDao.insertTimetable(hotelId, routeId, departTime, capacity, routeType);

        shuttleDao.extendSlotStock(hotelId, LocalDate.now(), LocalDate.now().plusDays(90));

        return 1;
    }
    
    
    @Override
    @Transactional
    public int activateRoute(int hotelId, long routeId) {
        shuttleDao.activateRoute(hotelId, routeId);
        return shuttleDao.activateTimetableByRoute(hotelId, routeId);
    }

    @Override
    @Transactional
    public int deactivateRoute(int hotelId, long routeId) {
        shuttleDao.deactivateTimetableByRoute(hotelId, routeId);
        return shuttleDao.deactivateRoute(hotelId, routeId);
    }

    @Override
    @Transactional
    public int addBlock(int adminNo,
                        int hotelId,
                        long routeId,
                        Long timetableId,
                        LocalDate startDate,
                        LocalDate endDate,
                        String reason) {

        if (hotelId <= 0) {
            throw new IllegalArgumentException("호텔 번호가 올바르지 않습니다.");
        }

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("차단 시작일과 종료일은 필수입니다.");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("차단 종료일은 시작일보다 빠를 수 없습니다.");
        }

        return shuttleDao.insertBlock(adminNo, hotelId, routeId, timetableId, startDate, endDate, reason);
    }

    @Override
    @Transactional
    public int deactivateBlock(int hotelId, long blockId) {
        return shuttleDao.deactivateBlock(hotelId, blockId);
    }

    @Override
    @Transactional
    public int purgeOldShuttleData(int hotelId) {

        if (hotelId <= 0) {
            throw new IllegalArgumentException("호텔 번호가 올바르지 않습니다.");
        }

        LocalDate cutoffDate = LocalDate.now().minusMonths(1);

        int cnt1 = shuttleDao.deleteOldBooking(hotelId, cutoffDate);
        int cnt2 = shuttleDao.deleteOldSlotStock(hotelId, cutoffDate);

        return cnt1 + cnt2;
    }
}