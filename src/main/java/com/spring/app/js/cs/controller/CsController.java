package com.spring.app.js.cs.controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.spring.app.js.index.service.IndexService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/cs")
public class CsController {

    @Autowired
    private CsService service;

    @Autowired
    private IndexService indexService;

    @GetMapping("/list")
    public ModelAndView csList(ModelAndView mav,
                               @RequestParam(value = "hotelId", defaultValue = "1") String hotelId,
                               @RequestParam(value = "searchKeyword", defaultValue = "") String searchKeyword,
                               HttpServletRequest request) {

        String str_currentShowPageNo = request.getParameter("curPage");
        int currentShowPageNo = (str_currentShowPageNo == null) ? 1 : Integer.parseInt(str_currentShowPageNo);

        Map<String, String> paraMap = new HashMap<>();
        paraMap.put("hotelId", hotelId);
        paraMap.put("searchKeyword", searchKeyword);

        mav.addObject("hotelList", indexService.getHotelList());
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

        mav.setViewName("js/cs/csList");
        return mav;
    }

    @GetMapping("/qnaWrite")
    public ModelAndView qnaWrite(ModelAndView mav,
                                 @RequestParam(value = "hotelId", defaultValue = "1") String hotelId,
                                 HttpSession session,
                                 java.security.Principal principal) {

        boolean isMember = (principal != null);
        boolean isGuest = (session.getAttribute("guestSession") != null);

        if (!isMember && !isGuest) {
            mav.addObject("message", "로그인이 필요한 서비스입니다.");
            mav.addObject("loc", "/security/login");
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
        }
        else {
            com.spring.app.jh.security.domain.Session_GuestDTO guest =
                (com.spring.app.jh.security.domain.Session_GuestDTO) session.getAttribute("guestSession");

            if (guest != null) {
                paraMap.put("writer_name", guest.getGuestName());
                paraMap.put("lookup_key", guest.getLookupKey());
            }
        }

        int n = service.insertQna(paraMap);

        if (n == 1) {
            mav.addObject("message", "문의가 성공적으로 등록되었습니다.");
            mav.addObject("loc", request.getContextPath() + "/cs/list?hotelId=" + hotelId);
        }
        else {
            mav.addObject("message", "등록에 실패했습니다.");
            mav.addObject("loc", "javascript:history.back()");
        }

        mav.setViewName("msg");
        return mav;
    }

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
        boolean isAdmin = request.isUserInRole("ADMIN_BRANCH") || request.isUserInRole("ROLE_ADMIN_BRANCH")
                       || request.isUserInRole("ADMIN_HQ") || request.isUserInRole("ROLE_ADMIN_HQ");
        boolean isMemberOwner = (principal != null && principal.getName().equals(writerName));

        com.spring.app.jh.security.domain.Session_GuestDTO guest =
            (com.spring.app.jh.security.domain.Session_GuestDTO) session.getAttribute("guestSession");
        boolean isGuestOwner = (guest != null && guest.getGuestName().equals(writerName));

        if ("Y".equals(qna.get("IS_SECRET")) && !isAdmin && !isMemberOwner && !isGuestOwner) {
            mav.addObject("message", "비밀글은 작성자와 관리자만 볼 수 있습니다.");
            mav.addObject("loc", "javascript:history.back()");
            mav.setViewName("msg");
            return mav;
        }

        mav.addObject("qna", qna);
        mav.addObject("isAdmin", isAdmin);
        mav.addObject("isMemberOwner", isMemberOwner);
        mav.addObject("isGuestOwner", isGuestOwner);
        mav.addObject("hotelList", indexService.getHotelList());
        mav.setViewName("js/cs/qnaDetail");
        return mav;
    }

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

        String writerName = String.valueOf(qna.get("WRITER_NAME"));
        boolean isMemberOwner = (principal != null && principal.getName().equals(writerName));
        com.spring.app.jh.security.domain.Session_GuestDTO guest =
            (com.spring.app.jh.security.domain.Session_GuestDTO) session.getAttribute("guestSession");
        boolean isGuestOwner = (guest != null && guest.getGuestName().equals(writerName));

        if (!isMemberOwner && !isGuestOwner) {
            mav.addObject("message", "본인이 작성한 글만 수정할 수 있습니다.");
            mav.addObject("loc", "javascript:history.back()");
            mav.setViewName("msg");
            return mav;
        }

        if (qna.get("ANS_CONTENT") != null) {
            mav.addObject("message", "관리자 답변이 완료된 문의는 수정할 수 없습니다.");
            mav.addObject("loc", "javascript:history.back()");
            mav.setViewName("msg");
            return mav;
        }

        mav.addObject("qna", qna);
        mav.addObject("hotelList", indexService.getHotelList());
        mav.setViewName("js/cs/qnaUpdate");
        return mav;
    }

    @PostMapping("/qnaUpdateEnd")
    public String qnaUpdateEnd(@RequestParam Map<String, String> paraMap, RedirectAttributes rttr) {

        Map<String, String> qna = service.getQnaDetail(paraMap.get("qnaId"));

        if (qna != null && qna.get("ANS_CONTENT") != null) {
            rttr.addFlashAttribute("message", "이미 답변이 등록되어 수정이 불가능합니다.");
            return "redirect:/cs/qnaDetail?qnaId=" + paraMap.get("qnaId");
        }

        int n = service.updateQna(paraMap);

        if (n == 1) {
            rttr.addFlashAttribute("message", "문의 내용이 수정되었습니다.");
        }
        else {
            rttr.addFlashAttribute("message", "수정에 실패했습니다.");
        }

        return "redirect:/cs/qnaDetail?qnaId=" + paraMap.get("qnaId");
    }

    @GetMapping("/qnaDelete")
    public String qnaDelete(@RequestParam("qna_id") String qnaId,
                            @RequestParam("hotelId") String hotelId,
                            HttpServletRequest request,
                            java.security.Principal principal,
                            HttpSession session,
                            RedirectAttributes rttr) {

        Map<String, String> qna = service.getQnaDetail(qnaId);
        if (qna == null) {
            return "redirect:/cs/list";
        }

        String writerName = String.valueOf(qna.get("WRITER_NAME"));
        boolean isAdmin = request.isUserInRole("ADMIN_BRANCH") || request.isUserInRole("ROLE_ADMIN_BRANCH")
                       || request.isUserInRole("ADMIN_HQ") || request.isUserInRole("ROLE_ADMIN_HQ");

        boolean isOwner = (principal != null && principal.getName().equals(writerName));
        if (!isOwner) {
            com.spring.app.jh.security.domain.Session_GuestDTO guest =
                (com.spring.app.jh.security.domain.Session_GuestDTO) session.getAttribute("guestSession");
            if (guest != null && guest.getGuestName().equals(writerName)) {
                isOwner = true;
            }
        }

        if (isOwner || isAdmin) {
            service.deleteQna(qnaId);
            rttr.addFlashAttribute("message", "성공적으로 삭제되었습니다.");
        }
        else {
            rttr.addFlashAttribute("message", "삭제 권한이 없습니다.");
        }

        return "redirect:/cs/list?hotelId=" + hotelId;
    }

    @PostMapping("/qnaAnswerEnd")
    public String qnaAnswerEnd(@RequestParam Map<String, String> paraMap,
                               HttpServletRequest request,
                               RedirectAttributes rttr) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomAdminDetails)) {
            rttr.addFlashAttribute("message", "관리자 로그인 정보가 없습니다.");
            return "redirect:/security/login";
        }

        CustomAdminDetails adminDetails = (CustomAdminDetails) auth.getPrincipal();
        AdminDTO adminDto = adminDetails.getAdminDto();

        boolean isHq = request.isUserInRole("ROLE_ADMIN_HQ");
        String myHotelId = String.valueOf(adminDto.getFk_hotel_id());
        String qnaHotelId = paraMap.get("hotelId");

        if (!isHq && !myHotelId.equals(qnaHotelId)) {
            rttr.addFlashAttribute("message", "해당 지점 관리자만 답변을 등록/수정할 수 있습니다.");
            return "redirect:/cs/list?hotelId=" + qnaHotelId;
        }

        paraMap.put("adminNo", String.valueOf(adminDto.getAdmin_no()));

        int n = service.updateQnaAnswer(paraMap);

        if (n == 1) {
            rttr.addFlashAttribute("message", "답변이 성공적으로 처리되었습니다.");
        }
        else {
            rttr.addFlashAttribute("message", "답변 처리에 실패했습니다.");
        }

        return "redirect:/cs/qnaDetail?qnaId=" + paraMap.get("qnaId");
    }

    @GetMapping("/faqWrite")
    public String faqWrite(@RequestParam("hotelId") String hotelId, Model model) {

        List<Map<String, String>> hotelList = indexService.getHotelList();
        String hotelName = "";
        if (hotelList != null) {
            for (Map<String, String> hotel : hotelList) {
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

    @PostMapping("/faqWriteEnd")
    public String faqWriteEnd(@RequestParam Map<String, String> paraMap, RedirectAttributes rttr) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof CustomAdminDetails) {
            CustomAdminDetails adminDetails = (CustomAdminDetails) auth.getPrincipal();
            AdminDTO adminDto = adminDetails.getAdminDto();
            paraMap.put("admin_no", String.valueOf(adminDto.getAdmin_no()));
        }

        int result = service.insertFaq(paraMap);

        if (result == 1) {
            rttr.addFlashAttribute("message", "FAQ가 성공적으로 등록되었습니다.");
        }
        else {
            rttr.addFlashAttribute("message", "FAQ 등록에 실패했습니다. 다시 시도해주세요.");
        }

        return "redirect:/cs/list?hotelId=" + paraMap.get("fk_hotel_id");
    }

    @PostMapping("/faqDelete")
    public String faqDelete(@RequestParam("faqId") String faqId,
                            @RequestParam("hotelId") String hotelId,
                            RedirectAttributes rttr) {

        int result = service.deleteFaq(faqId);

        if (result > 0) {
            rttr.addFlashAttribute("message", "FAQ가 성공적으로 삭제되었습니다.");
        }
        else {
            rttr.addFlashAttribute("message", "삭제에 실패했거나 이미 존재하지 않는 FAQ입니다.");
        }

        return "redirect:/cs/list?hotelId=" + hotelId;
    }
}
