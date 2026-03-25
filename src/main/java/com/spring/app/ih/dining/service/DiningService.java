package com.spring.app.ih.dining.service;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.spring.app.ih.dining.model.DiningDTO;
import com.spring.app.ih.dining.model.DiningReservationDTO;
import com.spring.app.ih.dining.model.ShopReservationStatDTO;
import com.spring.app.jh.security.domain.MemberDTO;
import com.spring.app.jh.security.domain.Session_MemberDTO;

public interface DiningService {
	
	List<Map<String, Object>> getDiningList(Map<String, Object> paraMap);
    
    DiningDTO getDiningDetail(Long dining_id);

	int registerReservation(DiningReservationDTO reservationDTO, String impUid, Session_MemberDTO member);

	String getDiningName(long dining_id);

	List<DiningReservationDTO> findNonMemberReservations(String name, String email, String password);

	void updateStatus(Long id);

	List<DiningReservationDTO> findMemberReservations(String memberid);

	List<DiningReservationDTO> getAllReservationsAdmin(Map<String, Object> paraMap);
	
	int getTotalReservationCount(Map<String, Object> paraMap);
	
	Map<String, Object> getDashboardCounts();

	int registerManual(DiningReservationDTO dto);
	
	int updateReservationStatusAdmin(@Param("resId") Long resId, @Param("status") String status);

	// 관리자용 예약 상세 조회
	DiningReservationDTO getReservationDetail(Long resId);

	int insertBlock(Map<String, Object> paraMap);

	List<Map<String, Object>> getBlockList();

	void deleteBlock(Long blockId);
	
	List<Map<String, Object>> getDiningList();
	
	int checkAvailability(Map<String, Object> paraMap);
	
	List<String> getUnavailableTimeList(Map<String, String> paraMap);
	
	List<ShopReservationStatDTO> getTodayShopStats();

	int updateMaxCapacity(Map<String, Object> paraMap);

	int updateSlotCapacity(Map<String, Object> paraMap);

	List<ShopReservationStatDTO> getDiningConfig(String diningId);
	
	List<Map<String, Object>> getTodayShopResList(String diningId);
	
	int getAvailableSeatCount(Map<String, Object> params);

	int updateDiningDetails(DiningDTO diningDTO);
	
	List<DiningDTO> getAdminDiningList(Map<String, Object> paraMap);

	List<Map<String, Object>> getDailyStatistics(String diningId);
	
	List<Map<String, Object>> getTimeSlotStatistics(String diningId);
	
}