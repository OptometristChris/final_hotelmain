package com.spring.app.jh.ops.user.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.spring.app.jh.ops.user.domain.HotelLocationDTO;
import com.spring.app.jh.ops.user.service.HotelLocationOpsService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/hotel")
public class HotelLocationController {

    private final HotelLocationOpsService hotelLocationOpsService;

    @GetMapping("/location")
    public String hotelLocationPage(Model model) {

        List<HotelLocationDTO> hotelList = hotelLocationOpsService.getActiveHotelLocationList();
        model.addAttribute("hotelList", hotelList);

        return "hotel/location";
    }
}
