package com.spring.app.hk.reservation.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationMailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public void sendReservationMail(
            String toEmail,
            String memberName,
            String reservationCode,
            String hotelName,
            String roomName,
            String checkIn,
            String checkOut,
            String totalPrice
    ) {

        try {

            String subject = "[CIEL] 호텔 예약 완료 안내";

            String html = ""
                    + "<div style='font-family:Arial;'>"
                    + "<h2 style='color:#8b6a52;'>예약이 완료되었습니다</h2>"
                    + "<p><b>" + memberName + "</b>님의 예약 정보입니다.</p>"

                    + "<div style='border:1px solid #ddd;padding:15px;border-radius:10px;'>"
                    + "<p><b>예약번호</b> : " + reservationCode + "</p>"
                    + "<p><b>호텔</b> : " + hotelName + "</p>"
                    + "<p><b>객실</b> : " + roomName + "</p>"
                    + "<p><b>체크인</b> : " + checkIn + "</p>"
                    + "<p><b>체크아웃</b> : " + checkOut + "</p>"
                    + "<p><b>결제금액</b> : ₩" + totalPrice + "</p>"
                    + "</div>"

                    + "<p style='margin-top:15px;'>CIEL 호텔을 이용해주셔서 감사합니다.</p>"
                    + "</div>";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}