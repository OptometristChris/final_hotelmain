package com.spring.app.hk.admin.hotel.controller;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.spring.app.common.FileManager;
import com.spring.app.hk.admin.hotel.service.HotelService;
import com.spring.app.jh.security.domain.CustomAdminDetails;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/hotel/")
public class HotelController {

    private final HotelService hotelService;
  
    // 이미지 업로드 위해 추가
    private final FileManager fileManager;

    @Value("${file.images-dir}")
    private String imagesDir;

    //  ======================== 1. 호텔 CRUD ============================
    // 호텔 리스트 가져오기
    @PreAuthorize("hasRole('ADMIN_HQ')")
    @GetMapping("list")
    public String hotelList(Model model){

    	List<Map<String,Object>> activeHotelList = hotelService.getApprovedHotelList();
    	List<Map<String,Object>> pendingHotelList = hotelService.getPendingHotelList();
    	List<Map<String,Object>> disabledHotelList = hotelService.getDisabledHotelList();

    	model.addAttribute("activeHotelList", activeHotelList);
    	model.addAttribute("disabledHotelList", disabledHotelList);
        
        return "hk/admin/hotel/list";
    }
    
    
    
    // 호텔 상세페이지 이동
    @PreAuthorize("hasAnyRole('ADMIN_HQ','ADMIN_BRANCH')")
    @GetMapping("detail")
    public String hotelDetail(@RequestParam("hotel_id") Long hotelId,
                              Model model){

        Map<String,Object> hotel = hotelService.getHotelDetail(hotelId);

        System.out.println("호텔 데이터 = " + hotel);
        
        model.addAttribute("hotel", hotel);

        return "hk/admin/hotel/detail";
    }
    
    
    // 호텔 상세페이지 내 수정하기
    @PreAuthorize("hasRole('ADMIN_HQ')")
    @PostMapping("update")
    @ResponseBody
    public Map<String,Object> updateHotel(@RequestBody Map<String,Object> param){ // 호텔 상세페이지 내 수정 완료 클릭 후 json 받는 용도

        int result = hotelService.updateHotel(param);

        Map<String,Object> map = new HashMap<>();
        map.put("result", result);

        return map;
    }
    

    // 호텔 상세페이지 내 비활성화하기
    @PreAuthorize("hasRole('ADMIN_HQ')")
    @PostMapping("delete")
    @ResponseBody
    public Map<String,Object> deleteHotel(@RequestBody Map<String,Object> param){

        int hotel_id = Integer.parseInt(param.get("hotel_id").toString());

        int result = hotelService.deleteHotel(hotel_id);

        Map<String,Object> map = new HashMap<>();
        map.put("result", result);

        return map;
    }
      
    
    // 호텔 활성화시키기
    @PreAuthorize("hasRole('ADMIN_HQ')")
    @PostMapping("restore")
    @ResponseBody
    public Map<String,Object> restoreHotel(@RequestBody Map<String,Object> param){

        int hotel_id = Integer.parseInt(param.get("hotel_id").toString());

        int result = hotelService.restoreHotel(hotel_id);

        Map<String,Object> map = new HashMap<>();
        map.put("result", result);

        return map;
    }
    
    
    // 등록 페이지 이동
	@PreAuthorize("hasRole('ADMIN_HQ')")
    @GetMapping("register")
    public String registerPage() {
        return "hk/admin/hotel/register";
    }

    
    // 호텔 등록
	@PreAuthorize("hasRole('ADMIN_HQ')")
    @PostMapping("register")
    @ResponseBody
    public Map<String, Object> register(
            @RequestParam Map<String, String> map,
            @RequestParam("mainImage") MultipartFile mainImage,
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            Authentication authentication
    ) {

        try {

        	// 로그인한 관리자 정보 가져오기
        	CustomAdminDetails loginAdmin = (CustomAdminDetails) authentication.getPrincipal();

			Integer adminNo = loginAdmin.getAdminDto().getAdmin_no();
			String adminType = loginAdmin.getAdminDto().getAdmin_type();

			Map<String,Object> paraMap = new HashMap<>(map);

			paraMap.put("latitude", latitude);
			paraMap.put("longitude", longitude);
			
			// 관리자 번호
			paraMap.put("admin_no", adminNo);

			// 활성 여부
			paraMap.put("active_yn","Y");

			// 승인 상태
			if("HQ".equals(adminType)){
			    paraMap.put("approve_status","APPROVED");
			}else{
			    paraMap.put("approve_status","PENDING");
			}
                      
            // 대표 이미지 업로드
            if(!mainImage.isEmpty()) {

            	// 업로드 위치 : file_images/hotel/파일명.jpg
            	String hotelPath = imagesDir + File.separator + "hotel";

            	String savedName =
            	        fileManager.doFileUpload(
            	                mainImage.getBytes(),
            	                mainImage.getOriginalFilename(),
            	                hotelPath);

                paraMap.put("main_image", savedName);
            }

            // 서비스 호출 (Reservation 구조 동일)
            hotelService.saveHotel(paraMap);         

        } catch(Exception e) {
            e.printStackTrace();
            return Map.of("result", 0);
        }

        return Map.of("result", 1);
    }
    
      
	// 호텔 위치
	@GetMapping("/map")
	public String hotelMap(Model model){

	    List<Map<String,Object>> hotelList = hotelService.getAllHotelLocation();

	    model.addAttribute("hotelList", hotelList);

	    return "hk/admin/hotel/map";
	}
	
}