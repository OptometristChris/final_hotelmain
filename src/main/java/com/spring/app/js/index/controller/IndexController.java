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

        // 1. 공통 파라미터
        String reserveType = request.getParameter("reserveType");
        String hotel = request.getParameter("hotel");
        String hotelId = request.getParameter("hotelId");

        // hotel 우선, 없으면 hotelId fallback
        if (hotel == null || hotel.isBlank()) {
            hotel = hotelId;
        }

        // 2. 다이닝 예약
        if ("dining".equals(reserveType)) {
            String diningType = request.getParameter("diningType");
            return "redirect:/dining/all?hotel_id=" + hotel + "&d_type=" + diningType;
        }

        // 3. 객실 검색 파라미터
        String roomGrade = request.getParameter("room_grade");
        String bedType   = request.getParameter("bed_type");
        String viewType  = request.getParameter("view_type");
        String capacity  = request.getParameter("capacity");
        String sort      = request.getParameter("sort");

        String checkIn  = request.getParameter("check_in");
        String checkOut = request.getParameter("check_out");

        // 기존 Final_hotel 방식 fallback
        if ((checkIn == null || checkIn.isBlank()) || (checkOut == null || checkOut.isBlank())) {
            String daterange = request.getParameter("daterange");

            if (daterange != null && !daterange.isBlank()) {
                String[] dateParts = null;

                if (daterange.contains(" ~ ")) {
                    dateParts = daterange.split(" ~ ");
                }
                else if (daterange.contains(" - ")) {
                    dateParts = daterange.split(" - ");
                }

                if (dateParts != null && dateParts.length == 2) {
                    checkIn = dateParts[0].trim();
                    checkOut = dateParts[1].trim();
                }
            }
        }

        Map<String, Object> paraMap = new HashMap<>();
        paraMap.put("hotel", hotel);
        paraMap.put("room_grade", roomGrade);
        paraMap.put("bed_type", bedType);
        paraMap.put("view_type", viewType);
        paraMap.put("capacity", capacity);
        paraMap.put("sort", sort);
        paraMap.put("check_in", checkIn);
        paraMap.put("check_out", checkOut);

        // 뷰 호환용 fallback 키도 같이 유지
        paraMap.put("hotelId", hotel);
        paraMap.put("checkIn", checkIn);
        paraMap.put("checkOut", checkOut);
        paraMap.put("bedType", bedType);

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
