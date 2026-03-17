package com.spring.app.jh.ops.user.service;

import com.spring.app.jh.ops.user.domain.ShuttleConfirmPageDTO;
import com.spring.app.jh.ops.user.domain.ShuttleReservePageDTO;

public interface ShuttleOpsService {
    ShuttleReservePageDTO getReservePage(long reservationId, int sessionMemberNo);


    void confirm(long reservationId, int sessionMemberNo,
            java.util.List<Long> toTimetableIds, java.util.List<Integer> toQtys,
            java.util.List<Long> fromTimetableIds, java.util.List<Integer> fromQtys);
    
    ShuttleConfirmPageDTO getConfirmPage(int memberNo);
}