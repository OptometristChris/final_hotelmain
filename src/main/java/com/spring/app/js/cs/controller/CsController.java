package com.spring.app.js.cs.controller;

import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.spring.app.jh.security.domain.AdminDTO;
import com.spring.app.jh.security.domain.CustomAdminDetails;
import com.spring.app.js.cs.service.CsService;
import com.spring.app.js.notice.service.NoticeService;

@Controller
@RequestMapping("/cs")
public class CsController {

    @Autowired
    private CsService service;

    @Autowired
    private NoticeService noticeService;

    /**
     * [사용자/관리자] FAQ + QnA 통합 고객지원 메인 페이지
     */
    @GetMapping("/list")
    public ModelAndView csList(ModelAndView mav, 
                               @RequestParam(value = "hotelId", defaultValue = "1") String hotelId,
                               @RequestParam(value = "searchKeyword", defaultValue = "") String searchKeyword,
                               HttpServletRequest request) {

        // 페이징 처리
        String str_currentShowPageNo = request.getParameter("curPage"); 
        int currentShowPageNo = (str_currentShowPageNo == null) ? 1 : Integer.parseInt(str_currentShowPageNo);

        Map<String, String> paraMap = new HashMap<>();
        paraMap.put("hotelId", hotelId);
        paraMap.put("searchKeyword", searchKeyword);

        // --- [권한 로직 통합] 프로모션 컨트롤러 스타일 ---
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isHq = false;
        boolean isAdmin = false;
        String myHotelId = ""; 

        if (auth != null && auth.getPrincipal() instanceof CustomAdminDetails) {
            CustomAdminDetails adminDetails = (CustomAdminDetails) auth.getPrincipal();
            AdminDTO adminDto = adminDetails.getAdminDto();

            if (adminDto != null) {
                Collection<? extends GrantedAuthority> authorities = adminDetails.getAuthorities();
                isHq = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_HQ"));
                boolean isBranchAdmin = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_BRANCH"));
                
                isAdmin = isHq || isBranchAdmin;

                if (isHq) {
                    myHotelId = "HQ";
                } else if (isBranchAdmin && adminDto.getFk_hotel_id() != null) {
                    myHotelId = String.valueOf(adminDto.getFk_hotel_id());
                }
            }
        }
        // ------------------------------------------

        mav.addObject("hotelList", noticeService.getHotelList());
        mav.addObject("faqList", service.getFaqListByHotel(hotelId));

        int totalCount = service.getQnaTotalCount(paraMap); 
        int sizePerPage = 10; 
        int totalPage = (int) Math.ceil((double) totalCount / sizePerPage);
        int startRno = ((currentShowPageNo - 1) * sizePerPage) + 1;
        int endRno = startRno + sizePerPage - 1;
        
        paraMap.put("startRno", String.valueOf(startRno));
        paraMap.put("endRno", String.valueOf(endRno));
        
        List<Map<String, String>> qnaList = service.getQnaListWithPaging(paraMap);

        mav.addObject("qnaList", qnaList);
        mav.addObject("hotelId", hotelId); 
        mav.addObject("searchKeyword", searchKeyword);
        mav.addObject("totalCount", totalCount);
        mav.addObject("curPage", currentShowPageNo);  
        mav.addObject("totalPage", totalPage);
        
        mav.addObject("isHq", isHq);
        mav.addObject("isAdmin", isAdmin); // 통합 관리자 여부
        mav.addObject("myHotelId", myHotelId); 
        
        mav.setViewName("js/cs/csList");
        return mav;
    }
    
    /**
     * [사용자] 1:1 문의 작성 페이지 이동
     */
    @GetMapping("/qnaWrite")
    public ModelAndView qnaWrite(ModelAndView mav, 
                                 @RequestParam(value = "hotelId", defaultValue = "1") String hotelId,
                                 HttpSession session,
                                 java.security.Principal principal) {
        
        // [중요] DB에서 호텔 리스트를 가져와야 select 박스에 뿌려줄 수 있습니다.
        List<Map<String, String>> hotelList = noticeService.getHotelList();
        mav.addObject("hotelList", hotelList);

        boolean isMember = (principal != null);
        boolean isGuest = (session.getAttribute("Session_GuestDTO") != null);

        // 로그인이나 비회원 예약 정보가 둘 다 없으면 로그인 페이지로
        if (!isMember && !isGuest) {
            mav.addObject("message", "로그인이 필요한 서비스입니다.");
            mav.addObject("loc", "javascript:history.back()"); 
            mav.setViewName("msg"); 
            return mav;
        }
        
        mav.addObject("hotelId", hotelId);
        mav.setViewName("js/cs/qnaWrite");
        return mav;
    }
    
    @PostMapping("/qnaWriteEnd")
    public ModelAndView qnaWriteEnd(ModelAndView mav, HttpServletRequest request, HttpSession session, java.security.Principal principal) {
        
        String hotelId = request.getParameter("fk_hotel_id");
        String title = request.getParameter("title");
        String content = request.getParameter("content");
        String is_secret = "1".equals(request.getParameter("is_secret")) ? "Y" : "N";

        Map<String, String> paraMap = new HashMap<>();
        paraMap.put("fk_hotel_id", hotelId);
        paraMap.put("title", title);
        paraMap.put("content", content);
        paraMap.put("is_secret", is_secret);

        if (principal != null) {
            paraMap.put("writer_name", principal.getName()); 
        } else {
            com.spring.app.jh.security.domain.Session_GuestDTO guest = 
                (com.spring.app.jh.security.domain.Session_GuestDTO) session.getAttribute("Session_GuestDTO");
            
            if (guest != null) {
                paraMap.put("writer_name", guest.getGuestName());
                paraMap.put("lookup_key", guest.getLookupKey()); 
            }
        }

        int n = service.insertQna(paraMap);

        if(n == 1) {
            mav.addObject("message", "문의가 성공적으로 등록되었습니다.");
            mav.addObject("loc", request.getContextPath() + "/cs/list?hotelId=" + hotelId);
        } else {
            mav.addObject("message", "등록에 실패했습니다.");
            mav.addObject("loc", "javascript:history.back()");
        }

        mav.setViewName("msg"); 
        return mav;
    }

    /**
     * [사용자/관리자] 1:1 문의 상세 보기
     */
    @GetMapping("/qnaDetail")
    public ModelAndView qnaDetail(ModelAndView mav, 
                                 @RequestParam(value = "qnaId") String qnaId,
                                 HttpServletRequest request, 
                                 HttpSession session,
                                 java.security.Principal principal) {
        
        Map<String, String> qna = service.getQnaDetail(qnaId);
        if (qna == null) {
            mav.addObject("message", "존재하지 않는 게시물입니다.");
            mav.addObject("loc", "javascript:history.back()");
            mav.setViewName("msg");
            return mav;
        }

        String writerName = String.valueOf(qna.get("WRITER_NAME"));
        String qnaHotelId = String.valueOf(qna.get("FK_HOTEL_ID"));

        // [수정된 권한 체크 로직]
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isHq = request.isUserInRole("ROLE_ADMIN_HQ");
        boolean isAdminBranch = request.isUserInRole("ROLE_ADMIN_BRANCH");
        boolean isMyBranch = false;

        // 지점 관리자인 경우: CustomAdminDetails를 통해 AdminDTO를 가져와야 합니다.
        if (isAdminBranch && auth != null && auth.getPrincipal() instanceof CustomAdminDetails) {
            CustomAdminDetails adminDetails = (CustomAdminDetails) auth.getPrincipal();
            AdminDTO adminDto = adminDetails.getAdminDto();
            
            if (adminDto != null && adminDto.getFk_hotel_id() != null) {
                String myHotelId = String.valueOf(adminDto.getFk_hotel_id());
                // 글의 지점ID와 관리자의 지점ID 비교
                isMyBranch = qnaHotelId.equals(myHotelId);
            }
        }
        
        // 작성자 본인 확인
        boolean isMemberOwner = (principal != null && principal.getName().equals(writerName));
        com.spring.app.jh.security.domain.Session_GuestDTO guest = 
            (com.spring.app.jh.security.domain.Session_GuestDTO) session.getAttribute("Session_GuestDTO");
        boolean isGuestOwner = (guest != null && guest.getGuestName().equals(writerName));

        // [비밀글 접근 제어]
        if ("Y".equals(qna.get("IS_SECRET"))) {
            // 본사 관리자(HQ) 이거나, (지점 관리자이면서 내 지점 글) 이거나, 작성자 본인인 경우만 허용
            boolean canAccess = isHq || (isAdminBranch && isMyBranch) || isMemberOwner || isGuestOwner;
            
            if (!canAccess) {
                mav.addObject("message", "비밀글은 작성자와 해당 지점 관리자만 볼 수 있습니다.");
                mav.addObject("loc", "javascript:history.back()");
                mav.setViewName("msg");
                return mav;
            }
        }
        
        mav.addObject("qna", qna);
        mav.addObject("isHq", isHq);
        mav.addObject("isAdmin", isHq || isAdminBranch);
        mav.addObject("isMyBranch", isMyBranch);
        mav.addObject("isMemberOwner", isMemberOwner);
        mav.addObject("isGuestOwner", isGuestOwner);
        mav.addObject("hotelList", noticeService.getHotelList());
        
        mav.setViewName("js/cs/qnaDetail"); 
        return mav;
    }

    /**
     * [사용자] 1:1 문의 수정 페이지 이동
     */
    @GetMapping("/qnaUpdate")
    public ModelAndView qnaUpdate(ModelAndView mav, 
                                  @RequestParam("qnaId") String qnaId,
                                  HttpSession session,
                                  java.security.Principal principal) {
        
        Map<String, String> qna = service.getQnaDetail(qnaId);
        
        if (qna == null) {
            mav.addObject("message", "존재하지 않는 게시물입니다.");
            mav.addObject("loc", "javascript:history.back()");
            mav.setViewName("msg");
            return mav;
        }

        // 작성자 본인 확인 로직
        String writerName = String.valueOf(qna.get("WRITER_NAME"));
        boolean isMemberOwner = (principal != null && principal.getName().equals(writerName));
        com.spring.app.jh.security.domain.Session_GuestDTO guest = 
            (com.spring.app.jh.security.domain.Session_GuestDTO) session.getAttribute("Session_GuestDTO");
        boolean isGuestOwner = (guest != null && guest.getGuestName().equals(writerName));

        if (!isMemberOwner && !isGuestOwner) {
            mav.addObject("message", "본인이 작성한 글만 수정할 수 있습니다.");
            mav.addObject("loc", "javascript:history.back()");
            mav.setViewName("msg");
            return mav;
        }

        // 답변 완료 시 수정 불가
        if (qna.get("ANS_CONTENT") != null) {
            mav.addObject("message", "관리자 답변이 완료된 문의는 수정할 수 없습니다.");
            mav.addObject("loc", "javascript:history.back()");
            mav.setViewName("msg");
            return mav;
        }

        mav.addObject("qna", qna);
        mav.addObject("hotelList", noticeService.getHotelList()); // 호텔 리스트 반드시 포함
        mav.setViewName("js/cs/qnaUpdate"); 
        return mav;
    }
    
    @PostMapping("/qnaUpdateEnd")
    public String qnaUpdateEnd(@RequestParam Map<String, String> paraMap, RedirectAttributes rttr) {
        
        // DB에서 최신 정보를 다시 읽어와 답변 여부 최종 확인 (보안 강화)
        Map<String, String> qna = service.getQnaDetail(paraMap.get("qnaId"));
        
        if(qna.get("ANS_CONTENT") != null) {
            rttr.addFlashAttribute("message", "이미 답변이 등록되어 수정이 불가능합니다.");
            return "redirect:/cs/qnaDetail?qnaId=" + paraMap.get("qnaId");
        }

        int n = service.updateQna(paraMap);
        
        if(n == 1) {
            rttr.addFlashAttribute("message", "문의 내용이 수정되었습니다.");
        } else {
            rttr.addFlashAttribute("message", "수정에 실패했습니다.");
        }

        return "redirect:/cs/qnaDetail?qnaId=" + paraMap.get("qnaId");
    }
    
    /**
     * [사용자/관리자] 1:1 문의 삭제
     */
    @GetMapping("/qnaDelete")
    public String qnaDelete(@RequestParam("qna_id") String qnaId, 
                            @RequestParam("hotelId") String hotelId,
                            HttpServletRequest request,
                            java.security.Principal principal, 
                            HttpSession session,
                            RedirectAttributes rttr) {

        Map<String, String> qna = service.getQnaDetail(qnaId);
        if(qna == null) return "redirect:/cs/list";
        
        String writerName = String.valueOf(qna.get("WRITER_NAME"));
        String qnaHotelId = String.valueOf(qna.get("FK_HOTEL_ID"));
        
        boolean isHq = request.isUserInRole("ROLE_ADMIN_HQ");
        boolean isAdminBranch = request.isUserInRole("ROLE_ADMIN_BRANCH");
        
        // 지점장 권한 체크 (본인 지점 글만 삭제 가능하도록 설정할 경우)
        boolean isMyBranch = false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isAdminBranch && auth != null) {
            Object authPrincipal = auth.getPrincipal();
            if (authPrincipal instanceof com.spring.app.jh.security.domain.AdminDTO) {
                String myHotelId = String.valueOf(((com.spring.app.jh.security.domain.AdminDTO) authPrincipal).getFk_hotel_id());
                isMyBranch = qnaHotelId.equals(myHotelId);
            }
        }

        boolean isOwner = (principal != null && principal.getName().equals(writerName));
        if (!isOwner) {
            com.spring.app.jh.security.domain.Session_GuestDTO guest = 
                (com.spring.app.jh.security.domain.Session_GuestDTO) session.getAttribute("Session_GuestDTO");
            if (guest != null && guest.getGuestName().equals(writerName)) {
                isOwner = true; 
            }
        }

        // 삭제 권한: 본사 관리자 OR 본인 지점 관리자 OR 작성자
        if (isHq || (isAdminBranch && isMyBranch) || isOwner) {
            service.deleteQna(qnaId);
            rttr.addFlashAttribute("message", "성공적으로 삭제되었습니다.");
        } else {
            rttr.addFlashAttribute("message", "삭제 권한이 없습니다.");
        }

        return "redirect:/cs/list?hotelId=" + hotelId;
    }
    
    // 어드민 답변 등록 및 수정 (통합 처리)
    @PostMapping("/qnaAnswerEnd")
    public String qnaAnswerEnd(@RequestParam Map<String, String> paraMap, 
                               HttpServletRequest request, 
                               RedirectAttributes rttr) {
        
        // 1. 로그인한 관리자 정보 가져오기
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomAdminDetails)) {
            rttr.addFlashAttribute("message", "관리자 로그인 정보가 없습니다.");
            return "redirect:/security/login";
        }

        CustomAdminDetails adminDetails = (CustomAdminDetails) auth.getPrincipal();
        AdminDTO adminDto = adminDetails.getAdminDto();
        
        // 2. 권한 검증: 본사 관리자(HQ) 또는 해당 지점 관리자인지 확인
        boolean isHq = request.isUserInRole("ROLE_ADMIN_HQ");
        String myHotelId = String.valueOf(adminDto.getFk_hotel_id());
        String qnaHotelId = paraMap.get("hotelId"); // 폼에서 넘겨준 해당 글의 hotelId

        // 지점 관리자인데 본인 지점 글이 아닌 경우 차단 (총괄 제외 요청 반영)
        if (!isHq && !myHotelId.equals(qnaHotelId)) {
            rttr.addFlashAttribute("message", "해당 지점 관리자만 답변을 등록/수정할 수 있습니다.");
            return "redirect:/cs/list?hotelId=" + qnaHotelId;
        }

        // 3. 파라미터 세팅
        paraMap.put("adminNo", String.valueOf(adminDto.getAdmin_no())); 

        // 4. 서비스 호출 (등록/수정 실행)
        int n = service.updateQnaAnswer(paraMap); 

        if(n == 1) {
            rttr.addFlashAttribute("message", "답변이 성공적으로 처리되었습니다.");
        } else {
            rttr.addFlashAttribute("message", "답변 처리에 실패했습니다.");
        }

        return "redirect:/cs/qnaDetail?qnaId=" + paraMap.get("qnaId");
    }
    
    // FAQ 작성 페이지 이동
    @GetMapping("/faqWrite")
    public String faqWrite(@RequestParam("hotelId") String hotelId, Model model) {
    	
    	List<Map<String, String>> hotelList = noticeService.getHotelList();
    	
    	String hotelName = "";
        if (hotelList != null) {
            for (Map<String, String> hotel : hotelList) {
                // DB 컬럼명 대소문자 확인 필요 (보통 MyBatis 조회시 대문자 "HOTEL_ID" 혹은 소문자 "hotel_id")
                String id = String.valueOf(hotel.get("HOTEL_ID") != null ? hotel.get("HOTEL_ID") : hotel.get("hotel_id"));
                
                if (id.equals(hotelId)) {
                    hotelName = String.valueOf(hotel.get("HOTEL_NAME") != null ? hotel.get("HOTEL_NAME") : hotel.get("hotel_name"));
                    break;
                }
            }
        }
    	
        model.addAttribute("hotelId", hotelId);
        model.addAttribute("hotelName", hotelName);
        return "js/cs/faqWrite";
    }

    // FAQ 작성 완료 처리
    @PostMapping("/faqWriteEnd")
    public String faqWriteEnd(@RequestParam Map<String, String> paraMap, RedirectAttributes rttr) {
        
        // 1. 현재 로그인한 관리자 정보 가져오기 (Security 사용)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.getPrincipal() instanceof CustomAdminDetails) {
            CustomAdminDetails adminDetails = (CustomAdminDetails) auth.getPrincipal();
            AdminDTO adminDto = adminDetails.getAdminDto();
            
            // 2. admin_no를 꺼내서 paraMap에 추가
            // 주의: MyBatis Mapper에서 #{admin_no}로 쓴다면 키값도 "admin_no"여야 합니다.
            paraMap.put("admin_no", String.valueOf(adminDto.getAdmin_no())); 
        }
        
        // 3. 서비스 호출 (이제 paraMap 안에 admin_no가 포함되어 있습니다.)
        int result = service.insertFaq(paraMap);
        
        if(result == 1) {
            // 성공했을 때
            rttr.addFlashAttribute("message", "FAQ가 성공적으로 등록되었습니다.");
        } else {
            // 실패했을 때 (DB 오류 등)
            rttr.addFlashAttribute("message", "FAQ 등록에 실패했습니다. 다시 시도해주세요.");
        }
        
        return "redirect:/cs/list?hotelId=" + paraMap.get("fk_hotel_id");
    }

    // FAQ 삭제 처리 (이전 답변에서 말씀드린 폼 전송용)
    @PostMapping("/faqDelete")
    public String faqDelete(@RequestParam("faqId") String faqId, 
                            @RequestParam("hotelId") String hotelId, 
                            RedirectAttributes rttr) {
        
        // 서비스 호출 (DB 삭제 로직)
        int result = service.deleteFaq(faqId);
        
        if(result > 0) {
            rttr.addFlashAttribute("message", "FAQ가 성공적으로 삭제되었습니다.");
        } else {
            // 이미 삭제되었거나 파라미터가 잘못 넘어온 경우
            rttr.addFlashAttribute("message", "삭제에 실패했거나 이미 존재하지 않는 FAQ입니다.");
        }
        
        return "redirect:/cs/list?hotelId=" + hotelId;
    }
}