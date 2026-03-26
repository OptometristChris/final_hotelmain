package com.spring.app.js.promotion.controller;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.spring.app.jh.security.auth.domain.JwtPrincipalDTO;
import com.spring.app.jh.security.domain.AdminDTO;
import com.spring.app.jh.security.domain.CustomAdminDetails;
import com.spring.app.js.promotion.domain.PromotionDTO;
import com.spring.app.js.promotion.service.PromotionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/promotion")
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    // 프로모션 목록 페이지
    @GetMapping("/list")
    public ModelAndView promotionList(@RequestParam(value = "hotelId", defaultValue = "1") int hotelId,
                                      ModelAndView mav) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean canWrite = false;

        if (auth != null && auth.getPrincipal() != null) {

            Object principal = auth.getPrincipal();

            // 1. 기존 Spring Security principal
            if (principal instanceof CustomAdminDetails adminDetails) {

                AdminDTO adminDto = adminDetails.getAdminDto();

                if (adminDto != null) {
                    Collection<? extends GrantedAuthority> authorities = adminDetails.getAuthorities();

                    boolean isHq = authorities.stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_HQ"));

                    boolean isBranchAdmin = authorities.stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_BRANCH")
                                        || a.getAuthority().equals("ADMIN_BRANCH"));

                    if (isHq) {
                        canWrite = true;
                    }
                    else if (isBranchAdmin) {
                        if (adminDto.getFk_hotel_id() == hotelId) {
                            canWrite = true;
                        }
                    }
                }
            }

            // 2. JWT 필터 복원 principal
            else if (principal instanceof JwtPrincipalDTO jwtPrincipal) {

                if ("ADMIN".equals(jwtPrincipal.getPrincipalType())) {

                    List<String> roles = jwtPrincipal.getRoles();

                    boolean isHq = roles != null && roles.stream()
                            .anyMatch(role -> "ROLE_ADMIN_HQ".equals(role));

                    boolean isBranchAdmin = roles != null && roles.stream()
                            .anyMatch(role -> "ROLE_ADMIN_BRANCH".equals(role)
                                            || "ADMIN_BRANCH".equals(role));

                    if (isHq) {
                        canWrite = true;
                    }
                    else if (isBranchAdmin) {
                        if (jwtPrincipal.getHotelId() != null
                                && jwtPrincipal.getHotelId().intValue() == hotelId) {
                            canWrite = true;
                        }
                    }
                }
            }
        }

        List<Map<String, String>> hotelList = promotionService.getHotelList();

        Map<String, Object> paraMap = new HashMap<>();
        paraMap.put("hotelId", hotelId);
        paraMap.put("isAdmin", canWrite);

        List<PromotionDTO> promoList = promotionService.getPromotionList(paraMap);

        mav.addObject("hotelList", hotelList);
        mav.addObject("promoList", promoList);
        mav.addObject("hotelId", hotelId);
        mav.addObject("canWrite", canWrite);

        mav.setViewName("js/promotion/list");
        return mav;
    }

    // 프로모션 상세 페이지
    @GetMapping("/detail/{id}")
    public ModelAndView promotionDetail(@PathVariable("id") int id, ModelAndView mav) {

        PromotionDTO promo = promotionService.getPromotionDetail(id);

        if (promo == null) {
            mav.setViewName("redirect:/promotion/list");
            return mav;
        }

        // --- [추가] 로그인한 관리자의 정보 및 지점 ID 추출 ---
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer loginHotelId = null;

        if (auth != null && auth.getPrincipal() instanceof CustomAdminDetails) {
            CustomAdminDetails adminDetails = (CustomAdminDetails) auth.getPrincipal();
            AdminDTO adminDto = adminDetails.getAdminDto();
            if (adminDto != null) {
                loginHotelId = adminDto.getFk_hotel_id();
            }
        }
        mav.addObject("loginHotelId", loginHotelId);

        // ★ 비활성 프로모션 접근 제어 로직 추가 ★
        if (promo.getIs_active() == 0) {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ADMIN_BRANCH")
                                || a.getAuthority().equals("ROLE_ADMIN_BRANCH"));

            if (!isAdmin) {
                mav.addObject("message", "종료되었거나 존재하지 않는 프로모션입니다.");
                mav.addObject("loc", "javascript:history.back()");
                mav.setViewName("msg");
                return mav;
            }
        }

        mav.addObject("promo", promo);
        mav.setViewName("js/promotion/detail");
        return mav;
    }

    /**
     * [관리자] 프로모션 등록 페이지 이동
     */
    @GetMapping("/write")
    public ModelAndView promotionWrite(@RequestParam("hotelId") String hotelId, ModelAndView mav) {

        List<Map<String, String>> hotelList = promotionService.getHotelList();

        mav.addObject("hotelList", hotelList);
        mav.addObject("hotelId", hotelId);
        mav.setViewName("js/promotion/write");
        return mav;
    }

    /**
     * [관리자] 프로모션 등록 처리
     */
    @PostMapping("/writeEnd")
    public ModelAndView promotionWriteEnd(ModelAndView mav,
                                          HttpServletRequest request,
                                          @RequestParam(value = "price", defaultValue = "0") String price,
                                          @RequestParam(value = "discount_rate", defaultValue = "0") String discountRate,
                                          @RequestParam(value = "discount_amount", defaultValue = "0") String discountAmount,
                                          @RequestParam("attach") MultipartFile attach) {

        Map<String, String> paraMap = new HashMap<>();

        // 1. 일반 파라미터 수집
        paraMap.put("fk_hotel_id", request.getParameter("fk_hotel_id"));
        paraMap.put("title", request.getParameter("title"));
        paraMap.put("start_date", request.getParameter("start_date"));
        paraMap.put("end_date", request.getParameter("end_date"));
        paraMap.put("subtitle", request.getParameter("subtitle"));
        paraMap.put("benefits", request.getParameter("benefits"));
        paraMap.put("sort_order", request.getParameter("sort_order"));
        paraMap.put("is_active", request.getParameter("is_active"));
        paraMap.put("banner_type", request.getParameter("banner_type"));

        // 2. 숫자형 데이터 정제
        paraMap.put("price", price.replaceAll("[^0-9]", ""));
        paraMap.put("discount_rate", discountRate.isEmpty() ? "0" : discountRate);
        paraMap.put("discount_amount", discountAmount.replaceAll("[^0-9]", ""));

        paraMap.put("target_room_type", request.getParameter("target_room_type"));
        paraMap.put("target_bed_type", request.getParameter("target_bed_type"));
        paraMap.put("target_view_type", request.getParameter("target_view_type"));

        // 3. 파일 업로드 처리
        if (attach != null && !attach.isEmpty()) {
            String originalFilename = attach.getOriginalFilename();

            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileNameOnly = originalFilename.substring(0, originalFilename.lastIndexOf("."));

            String sanitizedFileName = fileNameOnly.replaceAll("[^a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ_\\-]", "");

            String newFilename = sanitizedFileName + "_" + UUID.randomUUID().toString().substring(0, 8) + fileExtension;

            paraMap.put("image_url", newFilename);

            String projectPath = System.getProperty("user.dir");
            String deployPath = projectPath + File.separator + "file_images" + File.separator + "js";
            String staticPath = projectPath + File.separator + "src" + File.separator + "main"
                    + File.separator + "resources" + File.separator + "static"
                    + File.separator + "images" + File.separator + "js";

            try {
                byte[] fileData = attach.getBytes();

                File deployDir = new File(deployPath);
                if (!deployDir.exists()) deployDir.mkdirs();
                FileCopyUtils.copy(fileData, new File(deployPath, newFilename));

                File staticDir = new File(staticPath);
                if (!staticDir.exists()) staticDir.mkdirs();
                FileCopyUtils.copy(fileData, new File(staticPath, newFilename));

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            paraMap.put("image_url", "");
        }

        // 4. DB Insert 및 응답 처리
        int n = promotionService.insertPromotion(paraMap);

        if (n == 1) {
            mav.addObject("message", "프로모션이 등록되었습니다.");
            mav.addObject("loc", request.getContextPath() + "/promotion/list?hotelId=" + paraMap.get("fk_hotel_id"));
        } else {
            mav.addObject("message", "등록 실패!");
            mav.addObject("loc", "javascript:history.back()");
        }

        mav.setViewName("msg");
        return mav;
    }

    // [관리자] 프로모션 수정 페이지 이동
    @GetMapping("/edit")
    public String promotionEdit(@RequestParam("promoId") int promoId, Model model) {

        PromotionDTO promotion = promotionService.getPromotionDetail(promoId);

        if (promotion == null) {
            return "redirect:/promotion/list";
        }
        List<Map<String, String>> hotelList = promotionService.getHotelList();

        model.addAttribute("hotelList", hotelList);
        model.addAttribute("promo", promotion);
        return "js/promotion/edit";
    }

    /**
     * [관리자] 프로모션 수정 처리
     */
    @PostMapping("/update")
    public ModelAndView promotionUpdate(ModelAndView mav,
                                        HttpServletRequest request,
                                        @RequestParam(value = "price", defaultValue = "0") String price,
                                        @RequestParam(value = "discount_rate", defaultValue = "0") String discountRate,
                                        @RequestParam("attach") MultipartFile attach) {

        Map<String, String> paraMap = new HashMap<>();

        // 1. 파라미터 수집
        String promotion_id = request.getParameter("promotion_id");
        String fk_hotel_id = request.getParameter("fk_hotel_id");

        paraMap.put("promotion_id", promotion_id);
        paraMap.put("fk_hotel_id", fk_hotel_id);
        paraMap.put("title", request.getParameter("title"));
        paraMap.put("start_date", request.getParameter("start_date"));
        paraMap.put("end_date", request.getParameter("end_date"));
        paraMap.put("subtitle", request.getParameter("subtitle"));
        paraMap.put("benefits", request.getParameter("benefits"));

        paraMap.put("is_active", request.getParameter("is_active"));
        paraMap.put("sort_order", request.getParameter("sort_order"));

        // 숫자 데이터 정제
        paraMap.put("price", price.replaceAll("[^0-9]", ""));
        paraMap.put("discount_rate", discountRate.isEmpty() ? "0" : discountRate);

        paraMap.put("target_room_type", request.getParameter("target_room_type"));
        paraMap.put("target_bed_type", request.getParameter("target_bed_type"));
        paraMap.put("target_view_type", request.getParameter("target_view_type"));

        // 2. 파일 업로드 처리
        if (attach != null && !attach.isEmpty()) {
            String originalFilename = attach.getOriginalFilename();

            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileNameOnly = originalFilename.substring(0, originalFilename.lastIndexOf("."));

            String sanitizedFileName = fileNameOnly.replaceAll("[^a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ_\\-]", "");

            String newFilename = sanitizedFileName + "_" + UUID.randomUUID().toString().substring(0, 8) + fileExtension;

            paraMap.put("image_url", newFilename);

            String projectPath = System.getProperty("user.dir");
            String deployPath = projectPath + File.separator + "file_images" + File.separator + "js";
            String staticPath = projectPath + File.separator + "src" + File.separator + "main"
                    + File.separator + "resources" + File.separator + "static"
                    + File.separator + "images" + File.separator + "js";

            try {
                byte[] fileData = attach.getBytes();
                new File(deployPath).mkdirs();
                FileCopyUtils.copy(fileData, new File(deployPath, newFilename));

                new File(staticPath).mkdirs();
                FileCopyUtils.copy(fileData, new File(staticPath, newFilename));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            paraMap.put("image_url", null);
        }

        // 3. DB Update 서비스 호출
        int n = promotionService.updatePromotion(paraMap);

        if (n == 1) {
            mav.addObject("message", "프로모션 정보가 수정되었습니다.");
            mav.addObject("loc", request.getContextPath() + "/promotion/detail/" + promotion_id);
        } else {
            mav.addObject("message", "수정 실패!");
            mav.addObject("loc", "javascript:history.back()");
        }

        mav.setViewName("msg");
        return mav;
    }

    // 프로모션 삭제
    @PostMapping("/delete")
    public ModelAndView promotionDelete(ModelAndView mav,
                                        HttpServletRequest request,
                                        @RequestParam("promotion_id") int promotionId,
                                        @RequestParam("hotelId") String hotelId) {

        int n = promotionService.deletePromotion(promotionId);

        if (n > 0) {
            mav.addObject("message", "프로모션이 삭제되었습니다.");
            mav.addObject("loc", request.getContextPath() + "/promotion/list?hotelId=" + hotelId);
        } else {
            mav.addObject("message", "삭제 처리에 실패했습니다.");
            mav.addObject("loc", "javascript:history.back()");
        }

        mav.setViewName("msg");
        return mav;
    }

    @GetMapping("/reserve")
    public String reservePackage(@RequestParam("promoId") int promoId, Model model) {

        PromotionDTO promo = promotionService.getPromotionDetail(promoId);
        if (promo == null) return "redirect:/promotion/list";

        List<Map<String, Object>> roomList = promotionService.getAvailableRoomsForPromotion(promo);

        model.addAttribute("promo", promo);
        model.addAttribute("roomList", roomList);

        return "js/promotion/package_reserve";
    }

    /**
     * [사용자] 패키지 예약 확정 처리 (결제 + 예약 + 프로모션 매칭)
     */
    @PostMapping("/reservation_final")
    public ModelAndView reservationFinal(ModelAndView mav,
                                         @RequestParam Map<String, String> paraMap,
                                         HttpSession session) {

        // MemberDTO loginUser = (MemberDTO) session.getAttribute("loginUser");
        // if(loginUser == null) { ... 로그인 리다이렉트 ... }
        // paraMap.put("member_no", String.valueOf(loginUser.getMemberNo()));

        try {
            int n = promotionService.registerPackageReservation(paraMap);

            if (n == 1) {
                mav.addObject("message", "패키지 예약 및 결제가 완료되었습니다.");
                mav.addObject("loc", "/reservation/confirmation");
            } else {
                mav.addObject("message", "예약 처리 실패. 다시 시도해주세요.");
                mav.addObject("loc", "javascript:history.back()");
            }
        } catch (Exception e) {
            e.printStackTrace();
            mav.addObject("message", "시스템 오류: " + e.getMessage());
            mav.addObject("loc", "javascript:history.back()");
        }

        mav.setViewName("msg");
        return mav;
    }
}