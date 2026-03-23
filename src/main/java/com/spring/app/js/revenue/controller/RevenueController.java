package com.spring.app.js.revenue.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.spring.app.jh.security.domain.AdminDTO;
import com.spring.app.jh.security.domain.CustomAdminDetails;
import com.spring.app.js.revenue.service.RevenueService;

@Controller
@RequestMapping("/admin")
public class RevenueController {

    @Autowired
    private RevenueService revenueService;

    // 1. 처음 페이지 접속
    @GetMapping("/revenue")
    public String revenueMain(Model model, 
                               @RequestParam(value = "month", defaultValue = "") String month) {
        
        // [보안] 권한 확인 (HQ 여부 판단하여 뷰에 전달)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomAdminDetails) {
            boolean isHq = auth.getAuthorities().stream()
                               .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_HQ"));
            model.addAttribute("isHq", isHq);
        }

        if(month.equals("")) {
            month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        
        List<Map<String, String>> hotelList = revenueService.getHotelList();
        model.addAttribute("hotelList", hotelList);
        model.addAttribute("selectedMonth", month);
        
        return "js/revenue/revenue"; 
    }

    // 2. AJAX 요청 시 데이터 반환
    @GetMapping("/revenue/data")
    @ResponseBody
    public Map<String, Object> getRevenueData(@RequestParam(value = "month") String month,
                                              @RequestParam(value = "hotelName", defaultValue = "") String hotelName) {
        
        Map<String, Object> data = new HashMap<>();
        
        // [중요] 숫자(ID)와 문자(Name/Month)를 동시에 수용하기 위해 <String, Object> 사용
        Map<String, Object> paraMap = new HashMap<>();
        paraMap.put("month", month);

        // Security 세션에서 관리자 정보 추출
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomAdminDetails) {
            CustomAdminDetails adminDetails = (CustomAdminDetails) auth.getPrincipal();
            AdminDTO adminDto = adminDetails.getAdminDto();
            
            boolean isHq = auth.getAuthorities().stream()
                               .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_HQ"));
            
            // 1. 지점 관리자라면: DTO의 이름이 null이어도 상관없는 확실한 숫자 ID를 주입
            if (!isHq && adminDto != null) {
                paraMap.put("hotelId", adminDto.getFk_hotel_id());
            } 
            // 2. 본사 관리자라면: 화면에서 선택한 호텔 이름을 주입
            else {
                paraMap.put("hotelName", hotelName);
            }
        }
        
        // 서비스 메서드 호출 (파라미터 타입을 Map<String, Object>로 일치시켜야 함)
        data.put("summary", revenueService.getRevenueSummary(paraMap));
        data.put("dailyTrends", revenueService.getDailyRevenue(paraMap));
        data.put("roomTypePortion", revenueService.getRoomTypePortion(paraMap));
        data.put("revenueList", revenueService.getMonthlyPaymentList(paraMap));
        
        return data; 
    }
}