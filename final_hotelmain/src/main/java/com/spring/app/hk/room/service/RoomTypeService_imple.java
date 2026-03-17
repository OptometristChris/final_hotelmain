package com.spring.app.hk.room.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.spring.app.hk.room.domain.RoomTypeDTO;
import com.spring.app.hk.room.model.RoomTypeDAO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomTypeService_imple implements RoomTypeService {

    private final RoomTypeDAO roomdao;

    // 객실 목록 페이지 (최초 진입) 조회
    @Override
    public List<RoomTypeDTO> getRoomList() {
        return roomdao.selectRoomTypeList();
    }

    // 객실 필터 조회 (AJAX 필터용 JSON 반환)
    @Override
    public List<RoomTypeDTO> getRoomListByFilter(Map<String,String> paraMap) {
        return roomdao.selectRoomTypeByFilter(paraMap);
    }

    // 객실 상세 페이지 조회
    @Override
    public RoomTypeDTO getRoomDetail(Long roomId) {
        return roomdao.selectRoomDetail(roomId);
    }

    // 날짜별 가격 조회
    @Override
    public List<Map<String, Object>> getCalendarPrice(int roomId) {
        return roomdao.selectCalendarPrice(roomId);
    }

    // 비교 모달용 (비교함에 담긴 객실 id리스트를 기준으로 객실 정보 조회하기) -- 푸터
	@Override
	public List<RoomTypeDTO> getRoomsByIds(List<Long> roomIds) {
		return roomdao.selectRoomsByIds(roomIds);
    }
	
	// 상세 페이지 이미지 캐러셀용
	@Override
	public List<String> getRoomImages(Long roomId) {
	    return roomdao.getRoomImages(roomId);
	}

	// 상세 페이지 로그인 기반 조회기록 저장
	@Override
	public void insertViewHistory(Integer memberNo, Long roomId) {
		 roomdao.insertViewHistory(memberNo, roomId);
		
	}

	// 상세 페이지 로그인 기반 추천 객실 조회
	@Override
	public List<RoomTypeDTO> getRecommendedRooms(Integer memberNo, Long roomId) {
		return roomdao.selectRecommendedRooms(memberNo, roomId);
	}
	
}