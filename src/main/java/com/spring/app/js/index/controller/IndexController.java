package com.spring.app.js.index.controller;

import com.spring.app.hk.room.domain.RoomTypeDTO;
import com.spring.app.ih.dining.model.DiningDTO;
import com.spring.app.js.banner.domain.BannerDTO;
import com.spring.app.js.index.service.IndexService;
import com.spring.app.js.promotion.domain.PromotionDTO;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class IndexController {

    @Autowired
    private IndexService service;

    @GetMapping("/")
    public String redirectToIndex() {
        return "redirect:/index";
    }

    @GetMapping("/index")
    public String indexPage(Model model) {
        List<BannerDTO> mainBannerList = service.getMainBannerList();
        List<PromotionDTO> promoCardList = service.getPromoCardList();
        List<RoomTypeDTO> roomList = service.getMainRoomList();
        List<DiningDTO> diningList = service.getMainDiningList();
        List<Map<String, String>> hotelList = service.getHotelList();

        model.addAttribute("mainBannerList", mainBannerList);
        model.addAttribute("promoCardList", promoCardList);
        model.addAttribute("roomList", roomList);
        model.addAttribute("diningList", diningList);
        model.addAttribute("hotelList", hotelList);

        return "js/index/index";
    }

    @GetMapping("/search")
    public String searchRooms(HttpServletRequest request, Model model) {
        // 1. 공통 파라미터 수집
        String reserveType = request.getParameter("reserveType"); // 객실/다이닝 구분
        String hotelId = request.getParameter("hotelId");         // 선택한 호텔 ID

        // 2. 다이닝 예약일 경우 처리
        if ("dining".equals(reserveType)) {
            String diningType = request.getParameter("diningType"); // 다이닝 타입 (d_type)
            
            // 요청하신 경로 형식으로 리다이렉트
            return "redirect:/dining/all?hotel_id=" + hotelId + "&d_type=" + diningType;
        }

        // 3. 객실 예약일 경우 처리 (기존 로직)
        String daterange = request.getParameter("daterange"); 
        String capacity = request.getParameter("capacity");

        // 날짜 파싱 (안전하게 처리)
        String checkIn = "";
        String checkOut = "";
        if (daterange != null && daterange.contains(" ~ ")) {
            String[] dateParts = daterange.split(" ~ ");
            checkIn = dateParts[0].trim();
            checkOut = dateParts[1].trim();
        }

        Map<String, Object> paraMap = new HashMap<>();
        paraMap.put("hotelId", hotelId);
        paraMap.put("checkIn", checkIn);
        paraMap.put("checkOut", checkOut);
        paraMap.put("capacity", capacity);

        // 필터용 호텔 목록
        List<Map<String, String>> hotelList = service.getHotelList(); 
        model.addAttribute("hotelList", hotelList);

        // 검색 결과 객실 목록
        List<RoomTypeDTO> roomList = service.getAvailableRooms(paraMap);
        model.addAttribute("roomList", roomList);
        
        // UI 유지용 파라미터 전달
        model.addAttribute("searchParams", paraMap);

        return "hk/room/list"; 
    }

    @GetMapping("/admin/banner/write")
    public String bannerWrite(Model model) {
        List<Map<String, String>> hotelList = service.getHotelList();
        model.addAttribute("hotelList", hotelList);
        return "js/index/banner_write";
    }

    @GetMapping("/api/banner/detail")
    @ResponseBody
    public Map<String, Object> getBannerDetail(@RequestParam("hotelId") String hotelId) {
        return service.getBannerByHotelId(hotelId);
    }

    @PostMapping("/admin/banner/write")
    public String bannerWriteEnd(@RequestParam Map<String, String> paraMap) {
        if (!paraMap.containsKey("banner_type") || paraMap.get("banner_type").isEmpty()) {
            paraMap.put("banner_type", "MAIN");
        }

        int n = service.saveBanner(paraMap);
        if (n >= 1) {
            return "redirect:/index";
        }
        return "js/index/banner_write";
    }

    @GetMapping("/api/hotel/images")
    @ResponseBody
    public List<Map<String, Object>> getHotelImages(@RequestParam(value = "hotelId") String hotelId) {
        return service.getHotelImages(hotelId);
    }
}
