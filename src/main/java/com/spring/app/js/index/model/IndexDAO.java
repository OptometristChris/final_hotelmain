package com.spring.app.js.index.model;

import com.spring.app.hk.room.domain.RoomTypeDTO;
import com.spring.app.ih.dining.model.DiningDTO;
import com.spring.app.js.banner.domain.BannerDTO;
import com.spring.app.js.promotion.domain.PromotionDTO;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface IndexDAO {
    List<BannerDTO> getMainBannerList();
    List<RoomTypeDTO> getMainRoomList();
    List<DiningDTO> getMainDiningList();
    List<PromotionDTO> getPromoCardList();
    List<RoomTypeDTO> getAvailableRooms(Map<String, Object> paraMap);
    List<Map<String, String>> getHotelList();
    List<Map<String, Object>> getHotelImages(String hotelId);
    Map<String, Object> getBannerByHotelId(String hotelId);
    int saveBanner(Map<String, String> paraMap);
}
