package com.spring.app.js.index.service;

import com.spring.app.hk.room.domain.RoomTypeDTO;
import com.spring.app.ih.dining.model.DiningDTO;
import com.spring.app.js.banner.domain.BannerDTO;
import com.spring.app.js.index.model.IndexDAO;
import com.spring.app.js.promotion.domain.PromotionDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class IndexServiceimple implements IndexService {

    @Autowired
    private IndexDAO inDao;

    @Override
    public List<BannerDTO> getMainBannerList() {
        return inDao.getMainBannerList();
    }

    @Override
    public List<RoomTypeDTO> getMainRoomList() {
        return inDao.getMainRoomList();
    }

    @Override
    public List<DiningDTO> getMainDiningList() {
        return inDao.getMainDiningList();
    }

    @Override
    public List<PromotionDTO> getPromoCardList() {
        return inDao.getPromoCardList();
    }

    @Override
    public List<RoomTypeDTO> getAvailableRooms(Map<String, Object> paraMap) {
        return inDao.getAvailableRooms(paraMap);
    }

    @Override
    public List<Map<String, String>> getHotelList() {
        return inDao.getHotelList();
    }

    @Override
    public List<Map<String, Object>> getHotelImages(String hotelId) {
        return inDao.getHotelImages(hotelId);
    }

    @Override
    public Map<String, Object> getBannerByHotelId(String hotelId) {
        return inDao.getBannerByHotelId(hotelId);
    }

    @Override
    public int saveBanner(Map<String, String> paraMap) {
        return inDao.saveBanner(paraMap);
    }
}
