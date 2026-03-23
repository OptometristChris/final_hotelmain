package com.spring.app.hk.reservation.service;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import net.nurigo.java_sdk.api.Message;

@Service
public class SmsService {

    @Value("${coolsms.api.key}")
    private String apiKey;

    @Value("${coolsms.api.secret}")
    private String apiSecret;

    @Value("${coolsms.from}")
    private String from;

    // 예약 완료 문자
    public void sendReservationSms(String phone, String msg) {

    	System.out.println("🔥 apiKey = " + apiKey);
    	System.out.println("🔥 apiSecret = " + apiSecret);
    	System.out.println("🔥 from = " + from);
    	
        Message coolsms = new Message(apiKey, apiSecret);

        HashMap<String, String> params = new HashMap<>();
        params.put("to", phone);
        params.put("from", from);
        params.put("type", "SMS");
        params.put("text", msg);

        try {
            coolsms.send(params);
        } catch (Exception e) {
            throw new RuntimeException("SMS 발송 실패", e);
        }
    }
}