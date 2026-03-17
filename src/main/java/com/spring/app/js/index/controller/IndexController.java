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
        
        // 1. [수정] 메인 비주얼 슬라이더용 (BANNER_TYPE = 'MAIN')
        // 기존 bannerList 대신 통합된 PromotionDTO 리스트 사용
        List<BannerDTO> mainBannerList = service.getMainBannerList();
        
        // 2. [수정] 하단 프로모션 카드용 (BANNER_TYPE = 'CARD')
        // 기존 promoList 대신 통합된 PromotionDTO 리스트 사용
        List<PromotionDTO> promoCardList = service.getPromoCardList();

        // 3. 객실 목록 (상위 2개) 가져오기
        List<RoomTypeDTO> roomList = service.getMainRoomList();
        
        // 4. 다이닝 목록 (상위 3개) 가져오기
        List<DiningDTO> diningList = service.getMainDiningList();

        // 뷰(HTML)로 전달
        // HTML에서 사용하는 th:each 명칭과 일치시켜야 합니다.
        model.addAttribute("mainBannerList", mainBannerList); // 슬라이더용
        model.addAttribute("promoCardList", promoCardList);   // 하단 카드용
        model.addAttribute("roomList", roomList);
        model.addAttribute("diningList", diningList);

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
        String bedType = request.getParameter("bedType");

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
}