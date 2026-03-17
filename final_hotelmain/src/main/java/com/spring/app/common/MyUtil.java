package com.spring.app.common;

import jakarta.servlet.http.HttpServletRequest;

public class MyUtil {
	
	// *** ? 다음의 데이터까지 포함한 현재 URL 주소를 알려주는 메소드를 생성 *** //
	public static String getCurrentURL(HttpServletRequest request) {
		
		String currentURL = request.getRequestURL().toString();
		
	//	System.out.println("currentURL => " + currentURL);
		// currentURL => http://localhost:9090/
		
		String queryString = request.getQueryString();
	//	System.out.println("queryString => " + queryString);
		// queryString => 
		// queryString => null 이 나온다 (POST 방식일 경우)
		
		if (queryString != null) { // get 방식일 경우
			currentURL += "?" + queryString;
		//	System.out.println("currentURL 2트 => " + currentURL);
			// currentURL 2트 => http://localhost:9090/
		}
		
		String ctxPath = request.getContextPath();
		//     /MyMVC
		
		int beginIndex = currentURL.indexOf(ctxPath) + ctxPath.length();
		// 		27	   =						 21	 +	6
		
		currentURL = currentURL.substring(beginIndex);
		System.out.println("currentURL => " + currentURL);
		// currentURL => 
		
		return currentURL;
	}
}
