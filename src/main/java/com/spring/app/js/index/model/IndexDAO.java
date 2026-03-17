package com.spring.app.js.index.model;

import com.spring.app.hk.room.domain.RoomTypeDTO;
import com.spring.app.ih.dining.model.DiningDTO;
import com.spring.app.js.banner.domain.BannerDTO;
import com.spring.app.js.promotion.domain.PromotionDTO;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper // MyBatis 매퍼 인터페이스임을 명시
public interface IndexDAO {

    // 메인 배너 리스트 (우선순위 순)
    List<BannerDTO> getMainBannerList();

    // 메인용 객실 리스트 (최신 등록된 2개)
    List<RoomTypeDTO> getMainRoomList();

    // 메인용 다이닝 리스트 (임의의 3개 또는 등록순)
    List<DiningDTO> getMainDiningList();

    // 진행 중인 프로모션 리스트
    List<PromotionDTO> getPromoCardList();

    // 객실 검색
	List<RoomTypeDTO> getAvailableRooms(Map<String, Object> paraMap);

	// 호텔 리스트
	List<Map<String, String>> getHotelList();
}