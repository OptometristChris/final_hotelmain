package com.spring.app.hk.room.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.spring.app.hk.room.domain.RoomTypeDTO;

@Repository
public class RoomTypeDAO_imple implements RoomTypeDAO {

    @Autowired
    private SqlSessionTemplate sqlsession;

    // 객실 목록 페이지 (최초 진입)
    @Override
    public List<RoomTypeDTO> selectRoomTypeList() {
        return sqlsession.selectList("room.selectRoomTypeList");
    }

    // 객실 필터 조회
    @Override
    public List<RoomTypeDTO> selectRoomTypeByFilter(Map<String, String> paraMap) {
        return sqlsession.selectList("room.selectRoomTypeByFilter", paraMap);
    }

    // 객실 상세 조회
    @Override
    public RoomTypeDTO selectRoomDetail(Long roomId) {
        return sqlsession.selectOne("room.selectRoomDetail", roomId);
    }

    // 날짜별 가격 조회
    @Override
    public List<Map<String, Object>> selectCalendarPrice(int room_id) {
        return sqlsession.selectList("room.selectCalendarPrice", room_id);
    }

    // 비교 모달용 (비교함에 담긴 객실 id리스트를 기준으로 객실 정보 조회하기) -- 푸터
	@Override
	public List<RoomTypeDTO> selectRoomsByIds(List<Long> roomIds) {
		return sqlsession.selectList("room.selectRoomsByIds", roomIds);
	}

	// 상세 페이지 이미지 캐러셀용
	@Override
	public List<String> getRoomImages(Long roomId) {
	    return sqlsession.selectList("room.selectRoomImages", roomId);
	}

	// 상세 페이지 로그인 기반 조회기록 저장
	@Override
	public void insertViewHistory(Integer memberNo, Long roomId) {
		Map<String, Object> paramMap = new HashMap<>();
	    paramMap.put("memberNo", memberNo);
	    paramMap.put("roomId", roomId);

	    sqlsession.insert("room.insertViewHistory", paramMap);
		
	}

	// 상세 페이지 최근 객실 조회
	@Override
    public List<RoomTypeDTO> selectRecentRooms(Integer memberNo) {
        return sqlsession.selectList("room.selectRecentRooms", memberNo);
    }

	// 상세페이지 추천 객실 조회
	@Override
    public List<RoomTypeDTO> selectRecommendedRooms(Object object, Long roomId) {

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("currentRoomId", roomId);

        return sqlsession.selectList("room.selectRecommendedRooms", paramMap);
    }

	
}
