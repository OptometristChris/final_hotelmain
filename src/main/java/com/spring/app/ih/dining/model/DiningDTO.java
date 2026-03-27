package com.spring.app.ih.dining.model;

import lombok.Data;

@Data
public class DiningDTO {
    private int dining_id;      
    private int fk_hotel_id;    
    private String name;        
    private String d_type;      
    private String tel;         
    private String floor;       
    
    // 새로 추가한 컬럼들
    private String main_img;       // MAIN_IMG
    private String description;    // DESCRIPTION
    private String business_hours; // BUSINESS_HOURS
    
    private String introduction;
    private String store_imgs;
    private String food_imgs;
    private String menu_pdf;
    private String extra_info;
	private String available_times;
	private int max_total_capacity;
}