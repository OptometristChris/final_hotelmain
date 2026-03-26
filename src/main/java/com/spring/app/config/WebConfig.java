package com.spring.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
   // URL 경로와 외부경로를 매핑시켜주는 설정 클래스 생성하기
   
   @Value("${file.images-dir}") 
   private String imagesDir;          // 이메일 작성시 첨부파일의 경로를 잡아주는 것이다.

   
   @Override
   public void addResourceHandlers(ResourceHandlerRegistry registry) {

       registry.addResourceHandler("/file_images/**")
               .addResourceLocations("file:" + imagesDir + "/");

       registry.addResourceHandler("/images/js/**")
               .addResourceLocations("file:" + imagesDir + "/js/");

       // 다이닝 매장 및 음식 이미지 경로
       registry.addResourceHandler("/images/dining/**")
               .addResourceLocations("classpath:/static/images/dining/");

       registry.addResourceHandler("/images/food/**")
               .addResourceLocations("classpath:/static/images/dining/");

       registry.addResourceHandler("/files/menu/**")
               .addResourceLocations("classpath:/static/images/menu/");
   }
}
