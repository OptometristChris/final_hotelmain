package com.spring.app.hk.room.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.spring.app.jh.security.domain.Session_MemberDTO;
import com.spring.app.hk.admin.hotel.service.HotelService;
import com.spring.app.hk.room.domain.RoomTypeDTO;
import com.spring.app.hk.room.service.RoomTypeService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RoomController {

    private final RoomTypeService roomService;
    private final HotelService hotelService;

    /* ==============================
       1. 객실 목록 페이지 조회
       ============================== */
    @GetMapping("/room/list")
    public ModelAndView roomList(ModelAndView mav) {

    	// 객실 목록
        List<RoomTypeDTO> roomList = roomService.getRoomList();

        // 호텔 목록 (운영중인 호텔만)
        List<Map<String,Object>> hotelList = hotelService.getApprovedHotelList();
        
        // 디버깅용
        System.out.println("roomList size = " + roomList.size());
        System.out.println(roomList);

        mav.addObject("roomList", roomList);
        mav.addObject("hotelList", hotelList);
        mav.setViewName("hk/room/list");

        return mav;
    }

    /* ==============================
    2. 객실 필터 조회 (AJAX)
    - 날짜 범위(check_in ~ check_out)를 받아
      해당 기간의 최소 재고를 계산하여 반환
    ============================== */
	 @GetMapping("/room/filter")
	 @ResponseBody
	 public List<RoomTypeDTO> filterRoom(
	
	     // room type 
	     @RequestParam(name="hotel", defaultValue="") String hotel,
	     @RequestParam(name="room_grade", defaultValue="") String roomGrade,
	     @RequestParam(name="bed_type", defaultValue="") String bedType,
	     @RequestParam(name="view_type", defaultValue="") String viewType,
	     @RequestParam(name="capacity", required=false) String capacity,
	     @RequestParam(name="sort", defaultValue="") String sort,
	
	     // 체크인 날짜/ 체크아웃 날짜
	     @RequestParam(name="check_in", defaultValue="") String checkIn,
	     @RequestParam(name="check_out", defaultValue="") String checkOut
	 ) {	
	     Map<String, String> paraMap = new HashMap<>();
	
	     paraMap.put("hotel", hotel);
	     paraMap.put("room_grade", roomGrade);
	     paraMap.put("bed_type", bedType);
	     paraMap.put("view_type", viewType);
	     paraMap.put("capacity", capacity);
	     paraMap.put("sort", sort);
	
	     // 날짜 추가 _ 재고와 연결
	     paraMap.put("check_in", checkIn);
	     paraMap.put("check_out", checkOut);
	
	     return roomService.getRoomListByFilter(paraMap);
	 }

    /* ==============================
       3. 객실 상세 조회
       ============================== */
	 @GetMapping("/room/detail")
	 public ModelAndView roomDetail(ModelAndView mav,
	                                @RequestParam("room_id") Long roomId,
	                                HttpSession session) {

	     // 1️. 상세 정보 조회
	     RoomTypeDTO room = roomService.getRoomDetail(roomId);
	     List<String> imageList = roomService.getRoomImages(roomId);

	     mav.addObject("room", room);
	     mav.addObject("imageList", imageList);

	     // 2️. 로그인 사용자 확인
	     Session_MemberDTO loginUser = (Session_MemberDTO) session.getAttribute("sessionMemberDTO");

	     if(loginUser != null) {

	    	 Integer memberNo = loginUser.getMemberNo();

	         // 3️. 조회 기록 저장
	         roomService.insertViewHistory(memberNo, roomId);

	         // 4️. 추천 객실 조회
	         List<RoomTypeDTO> recommendList =
	                 roomService.getRecommendedRooms(memberNo, roomId);

	         mav.addObject("recommendList", recommendList);
	     }

	     mav.setViewName("hk/room/detail");
	     return mav;
	 }
	 
    
    /* ==============================
       4. 달력 모달 띄우기
       ============================== */
    @GetMapping("/room/calendar")
    @ResponseBody
    public List<Map<String,Object>> getCalendar(
            @RequestParam(name="room_id")  int room_id) {

        return roomService.getCalendarPrice(room_id);
    }
    
    
    /* ==============================
	    5. 푸터에서 비교하기
	    ============================== */
    @PostMapping("/room/compare")
    @ResponseBody
    public List<RoomTypeDTO> compareRooms(@RequestBody List<Long> roomIds){

        return roomService.getRoomsByIds(roomIds);
    }
    
}