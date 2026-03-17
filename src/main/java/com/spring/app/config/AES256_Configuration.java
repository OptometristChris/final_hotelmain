package com.spring.app.config;

import java.io.UnsupportedEncodingException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.spring.app.common.AES256;

@Configuration  // Spring 컨테이너가 처리해주는 클래스로서, 클래스내에 하나 이상의 @Bean 메소드를 선언만 해주면 런타임시 해당 빈에 대해 정의되어진 대로 요청을 처리해준다.   
public class AES256_Configuration {
    
	@Bean // Spring 컨테이너에서 관리할 빈을 메서드로 생성할 때 사용한다.
    AES256 aes() {  // 빈의 이름은 기본적으로 메소드명이 빈의 이름이 된다.
    	AES256 aes;
		try {
			aes = new AES256("abcd007#sistsix$");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
		return aes;
	}

}
