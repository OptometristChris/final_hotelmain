package com.spring.app.ih.dining.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.app.ih.dining.model.DiningDTO;
import com.spring.app.ih.dining.model.DiningReservationDTO;
import com.spring.app.ih.dining.model.ShopReservationStatDTO;
import com.spring.app.ih.dining.service.DiningService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/admin/dining")
public class AdminDiningController {

    @Autowired
    private DiningService diningservice;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model, @RequestParam Map<String, Object> paraMap) {
   
        int currentPage = Integer.parseInt((String) paraMap.getOrDefault("page", "1"));
        int sizePerPage = 10; 
        
        int startRow = (currentPage - 1) * sizePerPage + 1;
        int endRow = currentPage * sizePerPage;
        
        paraMap.put("startRow", startRow);
        paraMap.put("endRow", endRow);

        Map<String, Object> counts = diningservice.getDashboardCounts();
        model.addAttribute("counts", counts);
        
        List<DiningReservationDTO> resList = diningservice.getAllReservationsAdmin(paraMap);
        model.addAttribute("resList", resList);
        
    	List<ShopReservationStatDTO> shopCounts = diningservice.getTodayShopStats();
        model.addAttribute("shopCounts", shopCounts);
     
        int totalCount = diningservice.getTotalReservationCount(paraMap); 
        int totalPage = (int) Math.ceil((double) totalCount / sizePerPage);
        
        int pageBarSize = 5;
        int startPage = ((currentPage - 1) / pageBarSize) * pageBarSize + 1;
        int endPage = Math.min(startPage + pageBarSize - 1, totalPage);

        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPage", totalPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("paraMap", paraMap); 

        return "dining/admin/admin_dashboard"; 
    }

    // AJAX를 이용한 상태 변경 처리
    @PreAuthorize("hasAnyRole('ADMIN_HQ','ADMIN_BRANCH')") 
    @PostMapping("/updateStatus")
    @ResponseBody
    public String updateStatus(@RequestParam("resId") Long resId, @RequestParam("status") String status) {
        String upperStatus = status.toUpperCase();
    	
    	int n = diningservice.updateReservationStatusAdmin(resId, upperStatus);
        return (n==1) ? "success" : "fail";
    }
    
    @PreAuthorize("hasAnyRole('ADMIN_HQ','ADMIN_BRANCH')") 
    @PostMapping("/registerManual")
    @ResponseBody
    public String registerManual(DiningReservationDTO dto) {

        dto.setStatus("CONFIRMED"); 
        
        int n = diningservice.registerManual(dto);
        return (n == 1) ? "success" : "fail";
    }
    
    // 예약 상세 조회
    @PreAuthorize("hasAnyRole('ADMIN_HQ','ADMIN_BRANCH')") 
    @GetMapping("/detail")
    @ResponseBody 
    public DiningReservationDTO getReservationDetail(@RequestParam("resId") Long resId) {
        
        DiningReservationDTO detail = diningservice.getReservationDetail(resId);
        System.out.println(">>> 가져온 데이터: " + detail);
        return detail; 
    }
    
    // 인원 합산
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN_HQ','ADMIN_BRANCH')") 
    public int check_backup(@RequestParam Map<String, Object> paraMap) {
        try {
            int total = Integer.parseInt(String.valueOf(paraMap.get("adult_count"))) 
                      + Integer.parseInt(String.valueOf(paraMap.get("child_count")))
                      + Integer.parseInt(String.valueOf(paraMap.get("infant_count")));
            paraMap.put("totalGuests", total);
            return diningservice.checkAvailability(paraMap);
        } catch (Exception e) {
            return 0; 
        }
    }
    
    // 다이닝 예약 관리
    @GetMapping("/setting")
    @PreAuthorize("hasAnyRole('ADMIN_HQ','ADMIN_BRANCH')") 
    public String settingPage(Model model) {

        List<Map<String, Object>> blockList = diningservice.getBlockList(); 
        List<Map<String, Object>> diningList = diningservice.getDiningList(); 
        
        model.addAttribute("blockList", blockList);
        model.addAttribute("diningList", diningList);
        
        return "dining/admin/control_setting";
    }
    
    // 차단 등록
    @PreAuthorize("hasAnyRole('ADMIN_HQ','ADMIN_BRANCH')") 
    @PostMapping("/block/register")
    @ResponseBody
    public String registerBlock(@RequestParam Map<String, Object> paraMap) {
        try {
            
            System.out.println(">>> 등록 요청 데이터: " + paraMap);

            if(paraMap.get("fkDiningId") == null || paraMap.get("blockDate") == null || paraMap.get("blockTime") == null) {
                return "empty_data";
            }

            int result = diningservice.insertBlock(paraMap);

            if(result > 0) {
                return "success";
            } else {
                return "fail";
            }

        } catch (Exception e) {
            e.printStackTrace(); 
            return "error: " + e.getMessage();
        }
    }

    // 차단 해제
    @PreAuthorize("hasAnyRole('ADMIN_HQ','ADMIN_BRANCH')") 
    @PostMapping("/block/delete")
    @ResponseBody
    public String deleteBlock(@RequestParam("blockId") Long blockId) {
    	diningservice.deleteBlock(blockId);
        return "success";
    }
    
    // 차단 시간대 불러오기
    @ResponseBody
    @GetMapping("/getUnavailableSlots")
    public List<String> getUnavailableSlots(@RequestParam Map<String, String> paraMap) {
        List<String> unavailableList = diningservice.getUnavailableTimeList(paraMap);
        return unavailableList;
    }
    
    @PreAuthorize("hasAnyRole('ADMIN_HQ','ADMIN_BRANCH')") 
    @PostMapping("/updateMaxCapacity")
    @ResponseBody
    public String updateMaxCapacity(@RequestParam("diningId") String diningId, 
                                    @RequestParam("maxCapacity") int maxCapacity) {
        Map<String, Object> paraMap = new HashMap<>();
        paraMap.put("diningId", diningId);
        paraMap.put("maxCapacity", maxCapacity);
        
        int n = diningservice.updateMaxCapacity(paraMap);
        return (n == 1) ? "success" : "fail";
    }
    
    @PreAuthorize("hasAnyRole('ADMIN_HQ','ADMIN_BRANCH')") 
    @PostMapping("/updateSlotCapacity")
    @ResponseBody
    public String updateSlotCapacity(@RequestParam("slotId") String slotId, 
                                     @RequestParam("maxSlotCapacity") int maxSlotCapacity) {
        Map<String, Object> paraMap = new HashMap<>();
        paraMap.put("slotId", slotId);
        paraMap.put("maxSlotCapacity", maxSlotCapacity);
        
        int n = diningservice.updateSlotCapacity(paraMap);
        return (n == 1) ? "success" : "fail";
    } 
    
    @PreAuthorize("hasAnyRole('ADMIN_HQ','ADMIN_BRANCH')") 
    @GetMapping("/getConfig")
    @ResponseBody
    public List<ShopReservationStatDTO> getDiningConfig(@RequestParam("diningId") String diningId) {
        return diningservice.getDiningConfig(diningId); 
    }
    
    @PreAuthorize("hasAnyRole('ADMIN_HQ','ADMIN_BRANCH')") 
    @GetMapping("/getTodayShopResList")
    @ResponseBody
    public List<Map<String, Object>> getTodayShopResList(@RequestParam("diningId") String diningId) {
        return diningservice.getTodayShopResList(diningId); 
    }
    
    @PostMapping("/check")
    @ResponseBody
    public int checkAvailability(@RequestParam Map<String, Object> params) {
        System.out.println(">>> [신규 검증] 파라미터: " + params);
        
        try {
            int availableSeat = diningservice.getAvailableSeatCount(params);
            
            int requestedPeople = Integer.parseInt(params.get("adult_count").toString())
                                + Integer.parseInt(params.get("child_count").toString())
                                + Integer.parseInt(params.get("infant_count").toString());

            System.out.println(">>> 잔여석: " + availableSeat + " / 요청인원: " + requestedPeople);

            return availableSeat; 
            
        } catch (Exception e) {
            e.printStackTrace();
            return -1; 
        }
    }
    
    @PreAuthorize("hasAnyRole('ADMIN_HQ','ADMIN_BRANCH')") 
    @GetMapping("/list")
    public String adminDiningList(Model model) {
    	Map<String, Object> paraMap = new HashMap<>();
    	List<DiningDTO> diningList = diningservice.getAdminDiningList(paraMap);
        model.addAttribute("diningList", diningList);
        return "dining/admin/list"; 
    }
    
    @PreAuthorize("hasAnyRole('ADMIN_HQ','ADMIN_BRANCH')") 
    @GetMapping("/editdetail")
    public String showEditForm(@RequestParam("dining_id") Long dining_id, Model model) {
        DiningDTO dining = diningservice.getDiningDetail(dining_id);
        if (dining == null) { return "redirect:/admin/dining/list";}
        model.addAttribute("dining", dining);
        return "dining/admin/dininginfoedit"; 
    }
    
    @PreAuthorize("hasAnyRole('ADMIN_HQ','ADMIN_BRANCH')") 
    @PostMapping("/editdetail")
    public String updateDiningInfo(
            @ModelAttribute DiningDTO diningDTO, 
            @RequestParam(value="attach_img", required=false) MultipartFile attach_img,   // 메인 대표이미지 (1장)
            @RequestParam(value="attach_pdf", required=false) MultipartFile attach_pdf,   // PDF (1장)
            @RequestParam(value="attach_store", required=false) MultipartFile[] attach_store, // 매장사진 (여러장)
            @RequestParam(value="attach_food", required=false) MultipartFile[] attach_food,   // 음식사진 (여러장)
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) throws Exception {

        String rootPath = request.getSession().getServletContext().getRealPath("/");
        
        String pdfPath = rootPath + "static/files/menu/";
        String imgPath = rootPath + "static/images/dining/";
        String diningPath = rootPath + "static/images/dining/";
        String foodPath = rootPath + "static/images/dining/";
        
        if (attach_pdf != null && !attach_pdf.isEmpty()) {
            String pdfFileName = System.currentTimeMillis() + "_" + attach_pdf.getOriginalFilename();
            File targetPdf = new File(pdfPath, pdfFileName);
            
            if(!targetPdf.getParentFile().exists()) targetPdf.getParentFile().mkdirs();
            
            byte[] bytes = attach_pdf.getBytes();
            java.nio.file.Files.write(targetPdf.toPath(), bytes);
            
            diningDTO.setMenu_pdf(pdfFileName);
        }

        if (attach_img != null && !attach_img.isEmpty()) {
            String imgFileName = System.currentTimeMillis() + "_" + attach_img.getOriginalFilename();
            File targetImg = new File(imgPath, imgFileName);
            
            if(!targetImg.getParentFile().exists()) targetImg.getParentFile().mkdirs();
            
            byte[] bytes = attach_img.getBytes();
            java.nio.file.Files.write(targetImg.toPath(), bytes);
            
            diningDTO.setMain_img(imgFileName); 
        } else {
            diningDTO.setMain_img(null); 
        }
        
        
        if (attach_store != null && attach_store.length > 0 && !attach_store[0].isEmpty()) {
            List<String> storeFileNames = new ArrayList<>();
            for (MultipartFile file : attach_store) {
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                java.nio.file.Files.write(Paths.get(diningPath + fileName), file.getBytes());
                storeFileNames.add(fileName);
            }
            diningDTO.setStore_imgs(String.join(",", storeFileNames)); 
        } else {
            diningDTO.setStore_imgs(null); 
        }

        if (attach_food != null && attach_food.length > 0 && !attach_food[0].isEmpty()) {
            List<String> foodFileNames = new ArrayList<>();
            for (MultipartFile file : attach_food) {
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                java.nio.file.Files.write(Paths.get(foodPath + fileName), file.getBytes());
                foodFileNames.add(fileName);
            }
            diningDTO.setFood_imgs(String.join(",", foodFileNames));
        } else {
            diningDTO.setFood_imgs(null);
        }

        diningservice.updateDiningDetails(diningDTO);
        redirectAttributes.addFlashAttribute("msg", "성공적으로 수정되었습니다.");
        
        return "redirect:/dining/detail/" + diningDTO.getDining_id();
    }
    
    @GetMapping("/download")
    public void downloadFile(@RequestParam("fileName") String fileName, 
                             HttpServletRequest request, 
                             HttpServletResponse response) {

        String root = request.getSession().getServletContext().getRealPath("resources");
        String path = root + File.separator + "uploadFiles" + File.separator + fileName;

        File file = new File(path);

        if (file.exists()) {
            try {
                response.setContentType("application/octet-stream");
                
                String encodedFileName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"");
                response.setContentLength((int) file.length());

                try (FileInputStream fis = new FileInputStream(file);
                     OutputStream os = response.getOutputStream()) {
                    org.springframework.util.FileCopyUtils.copy(fis, os);
                    os.flush();
                }
            } catch (Exception e) {
                System.out.println("파일 다운로드 중 오류 발생: " + e.getMessage());
            }
        } else {
            System.out.println("파일이 존재하지 않습니다: " + path);
        }
    }
    
    @PreAuthorize("hasAnyRole('ADMIN_HQ','ADMIN_BRANCH')") 
    @GetMapping("/main")
    public String adminMain() {
        return "dining/admin/main";
    }
    
    @PreAuthorize("hasAnyRole('ADMIN_HQ','ADMIN_BRANCH')") 
    @GetMapping("/statistics")
    public String viewStatistics(@RequestParam(value="diningId", required=false) String diningId, Model model) {
        
    	List<Map<String, Object>> diningList = diningservice.getDiningList(); 
    	
        model.addAttribute("diningList", diningList);
        
        if((diningId == null || diningId.isEmpty()) && !diningList.isEmpty()) {
            diningId = String.valueOf(diningList.get(0).get("DINING_ID"));
        }

        List<Map<String, Object>> statsList = diningservice.getDailyStatistics(diningId);
        
        List<Map<String, Object>> timeStats = diningservice.getTimeSlotStatistics(diningId);

        ObjectMapper mapper = new ObjectMapper();
        try {
            model.addAttribute("statsJson", mapper.writeValueAsString(statsList));
            model.addAttribute("timeStatsJson", mapper.writeValueAsString(timeStats));
        } catch (JsonProcessingException e) {
            model.addAttribute("statsJson", "[]");
            model.addAttribute("timeStatsJson", "[]");
        }

        model.addAttribute("diningList", diningList);
        model.addAttribute("selectedDiningId", diningId);
        
        return "dining/admin/statistics";
    }
    

}