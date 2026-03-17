package com.spring.app.ih.dining.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spring.app.ih.dining.mapper.DiningMapper;
import com.spring.app.ih.dining.model.DiningDTO;
import com.spring.app.ih.dining.model.DiningReservationDTO;
import com.spring.app.jh.security.domain.MemberDTO;
import com.spring.app.jh.security.domain.Session_MemberDTO;

@Service
public class DiningServiceImple implements DiningService {

    @Autowired
    private DiningMapper diningMapper;

    // 목록 조회 (필터링용 Map 포함)
    @Override
    public List<DiningDTO> getDiningList(Map<String, Object> paraMap) {
        return diningMapper.getDiningList(paraMap);
    }

    // 다이닝 매장 상세 조회
    @Override
    public DiningDTO getDiningDetail(int dining_id) {
        return diningMapper.getDiningDetail(dining_id);
    }
    
    @Transactional
    public int registerReservation(DiningReservationDTO reservationDTO, String impUid, Session_MemberDTO member) {
        
        if (member != null) {
        	System.out.println(">>> 로그인 유저 번호 확인: " + member.getMemberNo()); // 이게 콘솔에 찍히는지 확인!
            
        	reservationDTO.setFkMemberNo(member.getMemberNo().longValue());
            reservationDTO.setResPassword(null); 
            
            
            System.out.println("디버깅 - DTO에 담긴 회원번호: " + reservationDTO.getFkMemberNo());
        } else {
            // 비회원인 경우: 회원 번호는 비우고, 입력받은 4자리 비밀번호를 유지
            reservationDTO.setFkMemberNo(null);
            
        }

        int resResult = diningMapper.insertReservation(reservationDTO);
        
        if (resResult > 0) {
            Map<String, Object> paraMap = new HashMap<>();
           
            paraMap.put("resNo", reservationDTO.getDiningReservationId()); 
            paraMap.put("amount", 100); 
            paraMap.put("originalAmount", 100);
            paraMap.put("paymentMethod", "card");
            paraMap.put("status", "PAID");
            paraMap.put("pgTid", impUid);
            
            return diningMapper.insertPayment(paraMap);
        }
        
        return 0;
    }

    // 다이닝 매장 이름
	@Override
	public String getDiningName(int dining_id) {
		return diningMapper.getDiningName(dining_id);
	}

	// 비회원 예약 조회
	@Override
	public List<DiningReservationDTO> findNonMemberReservations(String name, String email, String password) {
        return diningMapper.findNonMemberReservations(name, email, password);
    }

	// 회원 예약 조회
	@Override
	public List<DiningReservationDTO> findMemberReservations(String memberid) {
		return diningMapper.findMemberReservations(memberid);
	}
	
	// 예약 취소
    @Transactional
    public void updateStatus(Long id) {
    	diningMapper.updateStatus(id, "CANCELLED");
    }

    @Override
    public List<DiningReservationDTO> getAllReservationsAdmin(Map<String, Object> paraMap) {
        return diningMapper.findAllReservationsAdmin(paraMap);
    }

    @Override
    public Map<String, Object> getDashboardCounts() {
        return diningMapper.getAdminDashboardCounts();
    }

    @Override
    public int updateReservationStatusAdmin(Long resId, String status) {
        return diningMapper.updateReservationStatusAdmin(resId, status);
    }

	@Override
	public int registerManual(DiningReservationDTO dto) {
		return diningMapper.insertManualReservation(dto);
	}
	
	@Override
	public DiningReservationDTO getReservationDetail(Long resId) {
	    return diningMapper.getReservationDetail(resId);
	}
    
}