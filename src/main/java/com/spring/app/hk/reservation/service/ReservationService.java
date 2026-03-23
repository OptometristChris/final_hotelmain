package com.spring.app.hk.reservation.service;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

public interface ReservationService {
	
	// 예약 페이지 내 객실 기본 정보 조회
	Map<String, Object> getRoomInfo(int room_type_id);
	
	// 소셜로그인용
	Map<String, Object> findMemberByEmail(String emailFromOauth);
	
	// 예약 db 저장 후 reservation_code 반환 + 비회원 붙이기
	String saveReservation(Map<String, String> map, HttpSession session);

    // 예약 완료 페이지 조회
	Map<String, Object> getReservationByCode(String code);

	
	// 마이페이지 예약 목록 조회
	List<Map<String, Object>> selectMyReservationList(int memberNo);
	
	// 예약 취소
	int cancelReservation(long reservationId);

	// 비회원 에약 조회
	List<Map<String, Object>> findGuestReservation(String name, String phone);

	// 비회원 예약 취소
	int cancelGuestReservation(String reservationCode);

	


}