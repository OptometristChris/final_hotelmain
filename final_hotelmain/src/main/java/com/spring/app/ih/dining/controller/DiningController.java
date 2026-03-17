package com.spring.app.ih.dining.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.spring.app.ih.dining.model.DiningDTO;
import com.spring.app.ih.dining.model.DiningReservationDTO;
import com.spring.app.ih.dining.service.DiningService;
import com.spring.app.jh.security.domain.MemberDTO;
import com.spring.app.jh.security.domain.Session_MemberDTO;
import com.spring.app.jh.security.service.MemberService;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

@Controller // 뷰 페이지 없이 데이터를 직접 출력해줍니다.
@RequestMapping("/dining")
public class DiningController {

    @Autowired
    private DiningService diningService;
    
    @Autowired
    private MemberService memberService;

    @GetMapping("/all") // http://localhost:9081/final_hotel/dining/all
    public String getAll(
        @RequestParam(value = "hotel_id", required = false) Integer hotel_id,
        @RequestParam(value = "d_type", required = false) String d_type,
        Model model) {

        Map<String, Object> paraMap = new HashMap<>();
        paraMap.put("hotel_id", hotel_id);
        paraMap.put("d_type", d_type);

    	// System.out.println(">>> 넘어온 파라미터 : " + paraMap);
    	
        List<DiningDTO> diningList = diningService.getDiningList(paraMap);
        
        model.addAttribute("diningList", diningList);
        model.addAttribute("selectedHotel", paraMap.get("hotel_id"));
        model.addAttribute("selectedType", paraMap.get("d_type"));
        
        return "dining/all";
    }
    
    
    @GetMapping("/detail/{dining_id}") // http://localhost:9081/final_hotel/dining/detail/1
    public String diningDetail(@PathVariable("dining_id") int dining_id, Model model) {
        
        // 한 개의 식당 정보 가져오기
        DiningDTO dining = diningService.getDiningDetail(dining_id);
        model.addAttribute("dining", dining);
        return "dining/detail"; // templates/dining/detail.html
    }
    
    @GetMapping("/reserve/{dining_id}")
    public String showReservationPage(@PathVariable("dining_id") int dining_id, 
                                      HttpSession session, 
                                      Model model) {
        
        DiningDTO dining = diningService.getDiningDetail(dining_id);
        if (dining == null) {
            return "redirect:/dining/all"; 
        }
        
        if (dining.getAvailable_times() != null && !dining.getAvailable_times().isEmpty()) {
            String[] allTimes = dining.getAvailable_times().split(",");
            List<String> lunchSlots = new ArrayList<>();
            List<String> dinnerSlots = new ArrayList<>();

            for (String time : allTimes) {
                String trimmedTime = time.trim(); // 공백 제거
                int hour = Integer.parseInt(trimmedTime.split(":")[0]);
                if (hour < 15) {
                    lunchSlots.add(trimmedTime);
                } else {
                    dinnerSlots.add(trimmedTime);
                }
            }
            model.addAttribute("lunchSlots", lunchSlots);
            model.addAttribute("dinnerSlots", dinnerSlots);
        }
        
        model.addAttribute("dining", dining);

        // 로그인 여부 체크 및 회원 정보 전달
        Session_MemberDTO sessionUser = (Session_MemberDTO) session.getAttribute("sessionMemberDTO");

        if (sessionUser != null) {
            MemberDTO fullMemberInfo = memberService.findByMemberid(sessionUser.getMemberid());
            model.addAttribute("isMember", true);
            model.addAttribute("memberInfo", fullMemberInfo); 
        } else {
            model.addAttribute("isMember", false);
        }
        
        return "dining/reservation";
    }
    

    
    @PostMapping("/reserve/confirm")
    public String reserveConfirm(DiningReservationDTO reservationDTO, HttpSession session, Model model) {

        session.setAttribute("tempReservation", reservationDTO);
        
        int adultPrice = 20000;
        int childPrice = 10000;
        int totalAmount = (reservationDTO.getAdultCount() * adultPrice) + 
                           (reservationDTO.getChildCount() * childPrice);

        model.addAttribute("res", reservationDTO);
        model.addAttribute("totalAmount", totalAmount); 
        
        return "dining/reserve_confirm";  
    }
    
    
    
    @GetMapping("/reserve/complete")
    public String reserveComplete(
            @RequestParam("imp_uid") String impUid,
            @RequestParam("diningId") Long diningId,
            HttpSession session, 
            Model model) {

        DiningReservationDTO reservationDTO = (DiningReservationDTO) session.getAttribute("tempReservation");

        if (reservationDTO == null) {
            return "common/error"; // "세션이 만료되었습니다. 다시 시도해주세요."
        }
        reservationDTO.setDiningId(diningId);
        
        Session_MemberDTO member = (Session_MemberDTO) session.getAttribute("sessionMemberDTO");
        
        int result = diningService.registerReservation(reservationDTO, impUid, member);
        
        if(result > 0) {
            session.removeAttribute("tempReservation");
            
            model.addAttribute("res", reservationDTO);
            return "dining/reserve_success"; 
        } else {
            return "common/error";
        }
    }

    // 예약 완료 페이지 매핑
    @GetMapping("/reserve_success")
    public String reserveSuccess(@RequestParam("resNo") String resNo, Model model) {
        model.addAttribute("resNo", resNo);
        return "dining/reserve_success";
    }
    
    // 예약 조회 (로그인 여부에 따라 분기)
    @GetMapping("/reservation_search")
    public String searchPage(HttpSession session) {
    	
    	// 세션에서 로그인 정보 확인
        Session_MemberDTO sessionUser = (Session_MemberDTO) session.getAttribute("sessionMemberDTO");

        if (sessionUser != null) {
            // 로그인 상태, 회원 전용 조회 경로로 리다이렉트
            return "redirect:/dining/my_member_reservations";
        }

        // 로그아웃 상태, 원래대로 비회원 조회 폼 페이지 보여주기
        return "dining/reservation_search";
    }

    // 비회원 예약 내역 조회 
    @PostMapping("/my_reservations")
    public String getMyReservations(
    		@RequestParam("guestName") String guestName,
            @RequestParam("guestEmail") String guestEmail,
            @RequestParam("resPassword") String resPassword, 
            Model model) {
        
        List<DiningReservationDTO> reservations = diningService.findNonMemberReservations(guestName, guestEmail, resPassword);
        
        model.addAttribute("reservations", reservations);
        return "dining/my_reservations";
    }

    // 회원 전용 예약 내역 조회
    @GetMapping("/my_member_reservations")
    public String getMemberReservations(HttpSession session, Model model) {
        
        Session_MemberDTO sessionUser = (Session_MemberDTO) session.getAttribute("sessionMemberDTO");

        if (sessionUser == null) {
            return "redirect:/dining/reservation_search"; 
        }

        List<DiningReservationDTO> reservations = diningService.findMemberReservations(sessionUser.getMemberid());
        
        model.addAttribute("reservations", reservations);
        model.addAttribute("isMember", true); 
        
        return "dining/my_reservations"; 
    }
    
    // 예약 취소 처리 
    @GetMapping("/cancel")
    public String cancelReservation(@RequestParam("id") Long id) {
    	diningService.updateStatus(id);
        return "redirect:/dining/reservation_search"; // 취소 후 조회 페이지로 리다이렉트
    }
    
}