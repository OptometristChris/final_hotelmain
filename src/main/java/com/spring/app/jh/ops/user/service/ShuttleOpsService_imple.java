package com.spring.app.jh.ops.user.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spring.app.jh.ops.user.domain.ReservationForShuttleDTO;
import com.spring.app.jh.ops.user.domain.ShuttleConfirmCardDTO;
import com.spring.app.jh.ops.user.domain.ShuttleConfirmLegItemDTO;
import com.spring.app.jh.ops.user.domain.ShuttleConfirmPageDTO;
import com.spring.app.jh.ops.user.domain.ShuttleLegDTO;
import com.spring.app.jh.ops.user.domain.ShuttleReservePageDTO;
import com.spring.app.jh.ops.user.domain.ShuttleTimetableDTO;
import com.spring.app.jh.ops.user.domain.SlotStockDTO;
import com.spring.app.jh.ops.user.model.ShuttleOpsDAO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShuttleOpsService_imple implements ShuttleOpsService {

    private final ShuttleOpsDAO shuttleDao;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Override
    public ShuttleReservePageDTO getReservePage(long reservationId, int sessionMemberNo) {

        ReservationForShuttleDTO res = shuttleDao.selectReservationForShuttle(reservationId);
        assertAccessible(res, sessionMemberNo);

        Date toRideDate = res.getCheckinDate();
        Date fromRideDate = res.getCheckoutDate();

        List<ShuttleTimetableDTO> toList =
                shuttleDao.selectTimetableList(res.getHotelId(), "TO_HOTEL", toRideDate);
        fillRemaining(toList, toRideDate);

        List<ShuttleTimetableDTO> fromList =
                shuttleDao.selectTimetableList(res.getHotelId(), "FROM_HOTEL", fromRideDate);
        fillRemaining(fromList, fromRideDate);

        Long bookingId = shuttleDao.selectBookingIdByReservation(reservationId);

        for (ShuttleTimetableDTO t : toList) {
            t.setMyQty(0);
        }
        for (ShuttleTimetableDTO t : fromList) {
            t.setMyQty(0);
        }

        if (bookingId != null) {
            List<ShuttleLegDTO> toLegs = shuttleDao.selectBookedLegListByBookingAndType(bookingId, "TO_HOTEL");
            List<ShuttleLegDTO> fromLegs = shuttleDao.selectBookedLegListByBookingAndType(bookingId, "FROM_HOTEL");

            java.util.Map<Long, Integer> toQtyMap = new java.util.HashMap<>();
            for (ShuttleLegDTO l : toLegs) {
                toQtyMap.put(l.getTimetableId(), l.getTicketQty());
            }

            java.util.Map<Long, Integer> fromQtyMap = new java.util.HashMap<>();
            for (ShuttleLegDTO l : fromLegs) {
                fromQtyMap.put(l.getTimetableId(), l.getTicketQty());
            }
            for (ShuttleTimetableDTO t : toList) {
                t.setMyQty(toQtyMap.getOrDefault(t.getTimetableId(), 0));
            }
            for (ShuttleTimetableDTO t : fromList) {
                t.setMyQty(fromQtyMap.getOrDefault(t.getTimetableId(), 0));
            }
        }

        ShuttleReservePageDTO page = new ShuttleReservePageDTO();
        page.setReservation(res);
        page.setToRideDate(toRideDate);
        page.setFromRideDate(fromRideDate);
        page.setToList(toList);
        page.setFromList(fromList);
        page.setShuttleBookingId(bookingId);

        return page;
    }

    @Override
    @Transactional
    public void confirm(long reservationId, int sessionMemberNo,
                        List<Long> toTimetableIds, List<Integer> toQtys,
                        List<Long> fromTimetableIds, List<Integer> fromQtys) {

        ReservationForShuttleDTO res = shuttleDao.selectReservationForShuttle(reservationId);
        assertAccessible(res, sessionMemberNo);

        Long bookingId = shuttleDao.selectBookingIdByReservation(reservationId);
        if (bookingId == null) {
            shuttleDao.insertBookingHeader(reservationId, res.getHotelId(), sessionMemberNo, res.getCheckinDate());
            bookingId = shuttleDao.selectBookingIdByReservation(reservationId);
        }

        if (toTimetableIds == null) toTimetableIds = List.of();
        if (toQtys == null) toQtys = List.of();
        if (fromTimetableIds == null) fromTimetableIds = List.of();
        if (fromQtys == null) fromQtys = List.of();

        Date toRideDate = res.getCheckinDate();
        Date fromRideDate = res.getCheckoutDate();

        int toSum = sumNonNeg(toQtys);
        int fromSum = sumNonNeg(fromQtys);

        if (toSum > res.getGuestCount()) {
            throw new IllegalArgumentException("호텔행(체크인) 총 인원은 최대 티켓 수를 초과할 수 없습니다.");
        }
        if (fromSum > res.getGuestCount()) {
            throw new IllegalArgumentException("행선지행(체크아웃) 총 인원은 최대 티켓 수를 초과할 수 없습니다.");
        }

        List<ShuttleLegDTO> existingTo = shuttleDao.selectBookedLegListByBookingAndType(bookingId, "TO_HOTEL");
        List<ShuttleLegDTO> existingFrom = shuttleDao.selectBookedLegListByBookingAndType(bookingId, "FROM_HOTEL");

        var exToQty = new java.util.HashMap<Long, Integer>();
        var exToLegId = new java.util.HashMap<Long, Long>();
        for (ShuttleLegDTO l : existingTo) {
            exToQty.put(l.getTimetableId(), l.getTicketQty());
            exToLegId.put(l.getTimetableId(), l.getShuttleLegId());
        }

        var exFromQty = new java.util.HashMap<Long, Integer>();
        var exFromLegId = new java.util.HashMap<Long, Long>();
        for (ShuttleLegDTO l : existingFrom) {
            exFromQty.put(l.getTimetableId(), l.getTicketQty());
            exFromLegId.put(l.getTimetableId(), l.getShuttleLegId());
        }

        var desiredTo = buildDesiredMap(toTimetableIds, toQtys);
        var desiredFrom = buildDesiredMap(fromTimetableIds, fromQtys);

        var toAllIds = unionKeys(exToQty, desiredTo);
        var fromAllIds = unionKeys(exFromQty, desiredFrom);

        applyDirectionDelta(
                bookingId, res.getHotelId(), "TO_HOTEL", toRideDate,
                toAllIds, desiredTo, exToQty, exToLegId
        );

        applyDirectionDelta(
                bookingId, res.getHotelId(), "FROM_HOTEL", fromRideDate,
                fromAllIds, desiredFrom, exFromQty, exFromLegId
        );

        List<ShuttleLegDTO> remainTo = shuttleDao.selectBookedLegListByBookingAndType(bookingId, "TO_HOTEL");
        List<ShuttleLegDTO> remainFrom = shuttleDao.selectBookedLegListByBookingAndType(bookingId, "FROM_HOTEL");

        boolean hasToBooked = (remainTo != null && !remainTo.isEmpty());
        boolean hasFromBooked = (remainFrom != null && !remainFrom.isEmpty());

        if (!hasToBooked && !hasFromBooked) {
            shuttleDao.updateHeaderCanceled(bookingId);
        }
        else {
            shuttleDao.updateHeaderBooked(bookingId);
        }
    }

    private int sumNonNeg(List<Integer> list) {
        int sum = 0;
        for (Integer v : list) {
            int x = (v == null ? 0 : v);
            if (x < 0) {
                throw new IllegalArgumentException("티켓 수량은 0 이상이어야 합니다.");
            }
            sum += x;
        }
        return sum;
    }

    private java.util.Map<Long, Integer> buildDesiredMap(List<Long> ids, List<Integer> qtys) {
        var map = new java.util.HashMap<Long, Integer>();
        int n = Math.min(ids.size(), qtys.size());

        for (int i = 0; i < n; i++) {
            Long id = ids.get(i);
            int q = (qtys.get(i) == null ? 0 : qtys.get(i));

            if (id == null) continue;
            if (q < 0) throw new IllegalArgumentException("티켓 수량은 0 이상이어야 합니다.");

            map.put(id, q);
        }

        return map;
    }

    private List<Long> unionKeys(java.util.Map<Long, Integer> a, java.util.Map<Long, Integer> b) {
        var set = new java.util.HashSet<Long>();
        set.addAll(a.keySet());
        set.addAll(b.keySet());

        var list = new java.util.ArrayList<>(set);
        list.sort(Long::compareTo);

        return list;
    }

    private void applyDirectionDelta(
            long bookingId, int hotelId, String legType, Date rideDate,
            List<Long> timetableIds,
            java.util.Map<Long, Integer> desired,
            java.util.Map<Long, Integer> existingQty,
            java.util.Map<Long, Long> existingLegId
    ) {
        for (Long timetableId : timetableIds) {

            int want = desired.getOrDefault(timetableId, 0);
            int have = existingQty.getOrDefault(timetableId, 0);
            int delta = want - have;

            if (delta == 0) continue;

            ShuttleTimetableDTO tt = shuttleDao.selectTimetableOne(timetableId);
            if (tt == null) throw new IllegalStateException("시간표가 존재하지 않습니다.");
            if (tt.getHotelId() == null || tt.getHotelId() != hotelId) throw new IllegalStateException("시간표-호텔 불일치");
            if (!legType.equals(tt.getLegType())) throw new IllegalStateException("시간표-방향 불일치");

            if (delta < 0) {
                assertCancelableDeadline(rideDate, tt.getDepartTime());
            }

            SlotStockDTO locked = shuttleDao.selectSlotForUpdate(timetableId, rideDate);
            if (locked == null) throw new IllegalStateException("slot_stock 누락(미리 생성 누락)");

            if (delta > 0) {
                int remaining = locked.getCapacity() - locked.getBookedQty();
                if (remaining < delta) throw new IllegalStateException("잔여 좌석이 부족합니다.");
                shuttleDao.updateBookedQtyPlus(timetableId, rideDate, delta);
            }
            else {
                int dec = Math.abs(delta);
                if (locked.getBookedQty() < dec) throw new IllegalStateException("재고 데이터 비정상(booked_qty 부족)");
                shuttleDao.updateBookedQtyMinus(timetableId, rideDate, dec);
            }

            Long legId = existingLegId.get(timetableId);

            if (want == 0) {
                if (legId != null) {
                    shuttleDao.cancelLeg(legId);
                }
                else {
                    ShuttleLegDTO anyLeg = shuttleDao.selectLegByBookingAndTimetable(bookingId, timetableId, rideDate);
                    if (anyLeg != null && "BOOKED".equals(anyLeg.getLegStatus())) {
                        shuttleDao.cancelLeg(anyLeg.getShuttleLegId());
                    }
                }
            }
            else {
                if (legId == null) {
                    ShuttleLegDTO existed = shuttleDao.selectLegByBookingAndTimetable(bookingId, timetableId, rideDate);

                    if (existed != null) {
                        shuttleDao.updateLegQtyBooked(existed.getShuttleLegId(), want);
                    }
                    else {
                        shuttleDao.insertLeg(
                                bookingId,
                                timetableId,
                                legType,
                                rideDate,
                                tt.getPlaceCode(),
                                tt.getDepartTime(),
                                want
                        );
                    }
                }
                else {
                    shuttleDao.updateLegQtyBooked(legId, want);
                }
            }
        }
    }

    private void assertCancelableDeadline(Date rideDate, String departTimeHHmm) {
        LocalDate ride = rideDate.toInstant().atZone(KST).toLocalDate();
        LocalTime depart = LocalTime.parse(departTimeHHmm);
        LocalDateTime departAt = LocalDateTime.of(ride, depart);

        LocalDateTime deadline = LocalDateTime.of(departAt.toLocalDate().minusDays(1), LocalTime.of(18, 0));
        LocalDateTime now = LocalDateTime.now(KST);

        if (now.isAfter(deadline)) {
            throw new IllegalStateException("취소 가능 시간(전날 18시)을 초과했습니다.");
        }
    }

    private void assertAccessible(ReservationForShuttleDTO res, int sessionMemberNo) {
        if (res == null) throw new IllegalArgumentException("예약 정보가 존재하지 않습니다.");
        if (res.getMemberNo() == null || res.getMemberNo() != sessionMemberNo) {
            throw new IllegalStateException("본인 예약만 셔틀 예약이 가능합니다.");
        }
        if (!"CONFIRMED".equals(res.getReservationStatus())) {
            throw new IllegalStateException("CONFIRMED 예약만 셔틀 예약이 가능합니다.");
        }
    }

    private void fillRemaining(List<ShuttleTimetableDTO> list, Date rideDate) {
        for (ShuttleTimetableDTO t : list) {
            SlotStockDTO slot = shuttleDao.selectSlotForDisplay(t.getTimetableId(), rideDate);
            if (slot == null) {
                t.setCapacity(0);
                t.setBookedQty(0);
                t.setRemaining(0);
            }
            else {
                t.setCapacity(slot.getCapacity());
                t.setBookedQty(slot.getBookedQty());
                t.setRemaining(slot.getCapacity() - slot.getBookedQty());
            }
        }
    }

    @Override
    public ShuttleConfirmPageDTO getConfirmPage(int memberNo) {

        List<ReservationForShuttleDTO> reservations =
                shuttleDao.selectActiveReservationsForShuttle(memberNo);

        if (reservations == null) reservations = List.of();

        LocalDate today = LocalDate.now(KST);
        List<ShuttleConfirmCardDTO> cards = new ArrayList<>();

        for (ReservationForShuttleDTO res : reservations) {

            if (res == null || res.getReservationId() == null) continue;

            LocalDate checkin = toLocalDate(res.getCheckinDate());
            LocalDate checkout = toLocalDate(res.getCheckoutDate());

            // checkout 지난 예약은 아예 제외
            if (checkout == null || checkout.isBefore(today)) {
                continue;
            }

            ShuttleConfirmCardDTO card = new ShuttleConfirmCardDTO();

            card.setReservationId(res.getReservationId());
            card.setHotelName(res.getHotelName());
            card.setRoomName(res.getRoomName());
            card.setRoomImageUrl(res.getRoomImageUrl());
            card.setReservationStatus(res.getReservationStatus());
            card.setGuestCount(res.getGuestCount() == null ? 0 : res.getGuestCount());

            card.setCheckinText(fmtDate(checkin));
            card.setCheckoutText(fmtDate(checkout));
            card.setToRideDateText(fmtDate(checkin));
            card.setFromRideDateText(fmtDate(checkout));

            Long bookingId = shuttleDao.selectBookingIdByReservation(res.getReservationId());
            card.setShuttleBookingId(bookingId);

            List<ShuttleConfirmLegItemDTO> toLegs = new ArrayList<>();
            List<ShuttleConfirmLegItemDTO> fromLegs = new ArrayList<>();

            if (bookingId != null) {
                List<ShuttleLegDTO> legs = shuttleDao.selectBookedLegsByBooking(bookingId);
                if (legs == null) legs = List.of();

                toLegs = legs.stream()
                        .filter(l -> l != null && "TO_HOTEL".equals(l.getLegType()))
                        .sorted(Comparator.comparing(ShuttleLegDTO::getDepartTime, Comparator.nullsLast(String::compareTo)))
                        .map(this::toLegItem)
                        .collect(Collectors.toList());

                fromLegs = legs.stream()
                        .filter(l -> l != null && "FROM_HOTEL".equals(l.getLegType()))
                        .sorted(Comparator.comparing(ShuttleLegDTO::getDepartTime, Comparator.nullsLast(String::compareTo)))
                        .map(this::toLegItem)
                        .collect(Collectors.toList());
            }

            // 호텔행: 체크아웃이 지나지 않았으면 표시 가능
            boolean showTo = !checkout.isBefore(today);

            // 행선지행: 체크아웃일이 오늘 이상이면 표시 가능
            boolean showFrom = !checkout.isBefore(today);

            card.setShowTo(showTo);
            card.setShowFrom(showFrom);
            card.setToLegs(toLegs);
            card.setFromLegs(fromLegs);

            cards.add(card);
        }

        ShuttleConfirmPageDTO page = new ShuttleConfirmPageDTO();
        page.setCards(cards);
        page.setValidCount(cards.size());
        page.setMemberName(shuttleDao.selectMemberName(memberNo));

        return page;
    }

    private LocalDate toLocalDate(Date d) {
        if (d == null) return null;
        return d.toInstant().atZone(KST).toLocalDate();
    }

    private String fmtDate(LocalDate d) {
        if (d == null) return "";
        return d.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private ShuttleConfirmLegItemDTO toLegItem(ShuttleLegDTO leg) {
        ShuttleConfirmLegItemDTO item = new ShuttleConfirmLegItemDTO();
        item.setDepartTime(leg.getDepartTime());
        item.setTicketQty(leg.getTicketQty() == null ? 0 : leg.getTicketQty());
        item.setPlaceName(placeName(leg.getPlaceCode()));
        return item;
    }

    private String placeName(String placeCode) {
        if (placeCode == null) return "";
        return switch (placeCode) {
            case "SEOUL_STATION" -> "서울역";
            case "GIMPO" -> "김포공항";
            case "INCHEON" -> "인천공항";
            default -> placeCode;
        };
    }
}