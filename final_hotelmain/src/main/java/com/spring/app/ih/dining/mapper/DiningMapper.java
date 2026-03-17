package com.spring.app.ih.dining.mapper;

import com.spring.app.ih.dining.model.DiningDTO;
import com.spring.app.ih.dining.model.DiningReservationDTO;
import com.spring.app.jh.security.domain.MemberDTO;
import com.spring.app.jh.security.domain.Session_MemberDTO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper 
public interface DiningMapper {
    
	List<DiningDTO> getDiningList(Map<String, Object> paraMap);

    DiningDTO getDiningDetail(@Param("dining_id") int dining_id);
    
    int registerReservation(DiningReservationDTO reservationDTO, String impUid, Session_MemberDTO member);

	int insertReservation(DiningReservationDTO reservationDTO);

	int insertPayment(Map<String, Object> payMap);

	String getDiningName(int dining_id);

    List<DiningReservationDTO> findNonMemberReservations(
        @Param("name") String name, @Param("email") String email,  @Param("password") String password);

    void updateStatus(@Param("id") Long id, @Param("status") String status);

	List<DiningReservationDTO> findMemberReservations(String memberid);

	List<DiningReservationDTO> findAllReservationsAdmin(Map<String, Object> paraMap);

	Map<String, Object> getAdminDashboardCounts();

	int updateReservationStatusAdmin(@Param("resId") Long resId, @Param("status") String status);

	int insertManualReservation(DiningReservationDTO dto);
	
	// 관리자 - 예약 상세 정보 조회
	DiningReservationDTO getReservationDetail(Long resId);
}