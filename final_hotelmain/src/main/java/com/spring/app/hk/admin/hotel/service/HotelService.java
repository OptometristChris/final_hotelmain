package com.spring.app.hk.admin.hotel.service;

import java.util.List;
import java.util.Map;

public interface HotelService {

	// 호텔 리스트 가져오기
	// List<Map<String, Object>> getHotelList(); ---> 전체/ 승인 나눔.
	List<Map<String, Object>> getApprovedHotelList();
	List<Map<String, Object>> getPendingHotelList();
	List<Map<String, Object>> getDisabledHotelList();
	
    // 호텔 + 이미지 저장
    void saveHotel(Map<String, Object> paraMap);

    // 호텔 상세페이지 이동
	Map<String, Object> getHotelDetail(Long hotelId);

	 // 호텔 상세페이지 내 수정하기
	int updateHotel(Map<String, Object> param);

	// 호텔 상세페이지 내 비활성화하기
	int deleteHotel(int hotel_id);

	// 호텔 활성화시키기
	int restoreHotel(int hotel_id);
	
	// 호텔 위치
	List<Map<String, Object>> getAllHotelLocation();

	
}