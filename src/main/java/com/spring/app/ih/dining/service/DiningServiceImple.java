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
import com.spring.app.ih.dining.model.ShopReservationStatDTO;
import com.spring.app.jh.security.domain.MemberDTO;
import com.spring.app.jh.security.domain.Session_MemberDTO;

@Service
public class DiningServiceImple implements DiningService {

    @Autowired
    private DiningMapper diningMapper;

    // 목록 조회 (필터링용 Map 포함)
    @Override
    public List<Map<String, Object>> getDiningList(Map<String, Object> paraMap) {
        return diningMapper.getDiningList(paraMap);
    }

    // 다이닝 매장 상세 조회
    @Override
    public DiningDTO getDiningDetail(Long dining_id) {
        return diningMapper.getDiningDetail(dining_id);
    }
    
    @Transactional
    public int registerReservation(DiningReservationDTO reservationDTO, String impUid, Session_MemberDTO member) {
        
        if (member != null) {
        	reservationDTO.setFkMemberNo(member.getMemberNo().longValue());
            reservationDTO.setResPassword(null); 
            // System.out.println("확인용:DTO에 담긴 회원번호: " + reservationDTO.getFkMemberNo());
        } else {
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
	public String getDiningName(long dining_id) {
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
    public int getTotalReservationCount(Map<String, Object> paraMap) {
        return diningMapper.getTotalReservationCount(paraMap);
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
    
	// 다이닝 목록 불러오기
	@Override
	public List<Map<String, Object>> getDiningList() {

	    Map<String, Object> paraMap = new HashMap<>();
	    return diningMapper.getDiningList(paraMap);
	}
	
	// 예약 차단
	@Override
	public int insertBlock(Map<String, Object> paraMap) {
	    return diningMapper.insertBlock(paraMap);
	}

	// 차단 내역 불러오기
	@Override
	public List<Map<String, Object>> getBlockList() {
	    return diningMapper.getBlockList();
	}

	// 예약 차단 해제
	@Override
	public void deleteBlock(Long blockId) {
	    diningMapper.deleteBlock(blockId);
	}

	// 예약 가능 여부 확인
	@Override
	public int checkAvailability(Map<String, Object> paraMap) {
		return diningMapper.checkAvailability(paraMap);
	}

	@Override
	public List<String> getUnavailableTimeList(Map<String, String> paraMap) {
	    List<String> stopTimeList = diningMapper.getUnavailableTimeList(paraMap);
	    return stopTimeList;
	}
	
	@Override
	public List<ShopReservationStatDTO> getTodayShopStats() {
	    return diningMapper.getTodayShopStats();
	}
	
	@Override
    public List<ShopReservationStatDTO> getDiningConfig(String diningId) {
        return diningMapper.getDiningConfig(diningId);
    }

    @Override
    public int updateMaxCapacity(Map<String, Object> paraMap) {
        return diningMapper.updateMaxCapacity(paraMap);
    }

    @Override
    public int updateSlotCapacity(Map<String, Object> paraMap) {
        return diningMapper.updateSlotCapacity(paraMap);
    }
	
    @Override
    public List<Map<String, Object>> getTodayShopResList(String diningId) {
        return diningMapper.getTodayShopResList(diningId);
    }
    
    @Override
    public int getAvailableSeatCount(Map<String, Object> params) {
        int available = diningMapper.getAvailableSeatCount(params);
        return Math.max(available, 0); 
    }

    @Override
    @Transactional 
    public int updateDiningDetails(DiningDTO diningDTO) {
        return diningMapper.updateDiningDetails(diningDTO);
    }
    
    @Override
    public List<DiningDTO> getAdminDiningList(Map<String, Object> paraMap) {
        return diningMapper.getAdminDiningList(paraMap);
    }

    @Override
    public List<Map<String, Object>> getDailyStatistics(String diningId) {
        return diningMapper.getDailyStatistics(diningId);
    }

    @Override
    public List<Map<String, Object>> getTimeSlotStatistics(String diningId) {
        return diningMapper.getTimeSlotStatistics(diningId);
    }
    
}