package com.spring.app.jh.ops.admin.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.spring.app.jh.security.domain.AdminDTO;
import com.spring.app.jh.security.domain.CustomAdminDetails;
import com.spring.app.jh.security.domain.Session_AdminDTO;
import com.spring.app.jh.security.auth.domain.JwtPrincipalDTO;
import com.spring.app.jh.security.service.AdminService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping(value="/admin/branch/")
@PreAuthorize("hasRole('ADMIN_BRANCH')")
public class AdminBranchOpsController {

    private final AdminService adminService;

    @GetMapping("account/myInfo")
    public String myInfo(HttpSession session, Model model){

        Integer adminNo = getSessionAdminNo(session);
        if(adminNo == null) {
            return "redirect:/admin/login";
        }

        AdminDTO adminDto = adminService.getAdminDetail(adminNo);
        model.addAttribute("adminDto", adminDto);

        return "admin/branch/account/myInfo";
    }

    @GetMapping("account/profileEdit")
    public String profileEditForm(HttpSession session, Model model){

        Integer adminNo = getSessionAdminNo(session);
        if(adminNo == null) {
            return "redirect:/admin/login";
        }

        AdminDTO adminDto = adminService.getAdminDetail(adminNo);
        model.addAttribute("adminDto", adminDto);

        return "admin/branch/account/profileEditForm";
    }

    @PostMapping("account/profileEdit")
    public String profileEditEnd(AdminDTO adminDto, HttpSession session, Model model){

        Integer adminNo = getSessionAdminNo(session);
        if(adminNo == null) {
            return "redirect:/admin/login";
        }

        adminDto.setAdmin_no(adminNo);

        int n = adminService.updateAdminProfile(adminDto);
        model.addAttribute("result", n);

        return "admin/branch/account/profileEditResult";
    }

    private Integer getSessionAdminNo(HttpSession session) {

        if (session == null) return null;

        Object obj = session.getAttribute("sessionAdminDTO");
        if (obj instanceof Session_AdminDTO dto) {
            return dto.getAdmin_no();
        }

        SecurityContext context =
                (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");

        if (context != null && context.getAuthentication() != null) {
            Object principal = context.getAuthentication().getPrincipal();

            if (principal instanceof JwtPrincipalDTO jwtPrincipal) {
                if ("ADMIN".equals(jwtPrincipal.getPrincipalType())) {
                    return jwtPrincipal.getPrincipalNo().intValue();
                }
            }

            if (principal instanceof CustomAdminDetails cad) {
                return cad.getAdminDto().getAdmin_no();
            }
        }

        return null;
    }
}