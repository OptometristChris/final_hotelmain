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
        String reserveType = request.getParameter("reserveType");
        String hotelId = request.getParameter("hotelId");

        if ("dining".equals(reserveType)) {
            String diningType = request.getParameter("diningType");
            return "redirect:/dining/all?hotel_id=" + hotelId + "&d_type=" + diningType;
        }

        String daterange = request.getParameter("daterange");
        String bedType = request.getParameter("bedType");

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

        List<Map<String, String>> hotelList = service.getHotelList();
        model.addAttribute("hotelList", hotelList);

        List<RoomTypeDTO> roomList = service.getAvailableRooms(paraMap);
        model.addAttribute("roomList", roomList);
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
