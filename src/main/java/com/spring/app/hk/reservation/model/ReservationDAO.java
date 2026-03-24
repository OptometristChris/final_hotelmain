package com.spring.app.hk.reservation.model;

import java.util.List;
import java.util.Map;

public interface ReservationDAO {

	// payment 테이블에 insert
	int insertPayment(Map<String, Object> paraMap);
	
	// reservation 테이블에 insert
    int insertReservation(Map<String, Object> paraMap);

    // 예약 완료 페이지
    Map<String, Object> findByReservationCode(String reservationCode);

    // 예약 페이지 내 객실 기본 정보 조회
	Map<String, Object> getRoomInfo(int room_type_id);

	// 마이페이지 예약 목록 조회
	List<Map<String, Object>> selectMyReservationList(int memberNo);
	
	// 예약 취소
	int cancelReservation(long reservationId);

	// 비회원 예약 조회
	List<Map<String, Object>> findGuestReservation(Map<String, Object> paraMap);

	// 비회원 예약 취소
	int cancelGuestReservation(String reservationCode);
	
	// 스프링 스케줄러로 예약 메일 보내기
	List<Map<String, Object>> selectTomorrowCheckinForMail();

	// 소셜로그인용
	Map<String, Object> findMemberByEmail(String emailFromOauth);

	// 암호화 조회 후 없으면 회원 생성
	void insertSocialMember(String emailFromOauth);
	
	// 예약코드 통일
	String selectReservationCodeById(Long reservationId);


}