package com.spring.app.hk.reservation.model;

import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ReservationDAO_imple implements ReservationDAO {

    @Autowired
    private SqlSessionTemplate sqlsession;
 
    // 예약 페이지 내 객실 기본 정보 조회
   	@Override
   	public Map<String, Object> getRoomInfo(int room_type_id) {
   		return sqlsession.selectOne("reservation.getRoomInfo", room_type_id);
   	}
  	
   	// 소셜 로그인용
 	@Override
 	public Map<String, Object> findMemberByEmail(String emailFromOauth) {
 		return sqlsession.selectOne("reservation.findMemberByEmail", emailFromOauth);
 	}
   	
   	// payment 테이블에 insert
    @Override
    public int insertPayment(Map<String, Object> paraMap) {
        return sqlsession.insert("reservation.insertPayment", paraMap);
    }
    
    // reservation 테이블에 insert
    @Override
    public int insertReservation(Map<String, Object> paraMap) {
        return sqlsession.insert("reservation.insertReservation", paraMap);
    }

    // 예약 완료 페이지
    @Override
    public Map<String, Object> findByReservationCode(String reservationCode) {
        return sqlsession.selectOne("reservation.findByReservationCode", reservationCode);
    }  

	// 마이페이지 예약 목록 조회
	@Override
	public List<Map<String, Object>> selectMyReservationList(int memberNo) {
		 return sqlsession.selectList("reservation.selectMyReservationList", memberNo);
	}

	// 예약 취소
	@Override
	public int cancelReservation(long reservationId) {
		 return sqlsession.update("reservation.cancelReservation", reservationId);
	}

	// 비회원 예약 조회
	@Override
	public List<Map<String, Object>> findGuestReservation(Map<String, Object> paraMap) {
		return sqlsession.selectList("reservation.findGuestReservation", paraMap);
	}

	// 비회원 예약 취소
	@Override
	public int cancelGuestReservation(String reservationCode) {
		return sqlsession.update("reservation.cancelGuestReservation", reservationCode);
	}
	
	// 스프링 스케줄러로 예약 메일 보내기
	@Override
	public List<Map<String, Object>> selectTomorrowCheckinForMail() {
	    return sqlsession.selectList("reservation.selectTomorrowCheckinForMail");
	}

	
	// 암호화 조회 후 없으면 생성
	@Override
	public void insertSocialMember(String emailFromOauth) {
		sqlsession.insert("reservation.insertSocialMember", emailFromOauth);
		
	}	

}
