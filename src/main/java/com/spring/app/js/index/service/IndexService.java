package com.spring.app.js.index.service;

import com.spring.app.hk.room.domain.RoomTypeDTO;
import com.spring.app.ih.dining.model.DiningDTO;
import com.spring.app.js.banner.domain.BannerDTO;
import com.spring.app.js.promotion.domain.PromotionDTO;

import java.util.List;
import java.util.Map;

public interface IndexService {
    // 메인 배너 리스트 조회
    List<BannerDTO> getMainBannerList();

    // 메인용 객실 리스트
    List<RoomTypeDTO> getMainRoomList();

    // 메인용 다이닝 리스트
    List<DiningDTO> getMainDiningList();

    // 메인용 프로모션 리스트
    List<PromotionDTO> getPromoCardList();

    // 객실 검색
	List<RoomTypeDTO> getAvailableRooms(Map<String, Object> paraMap);

	// 호텔 리스트 
	List<Map<String, String>> getHotelList();
}