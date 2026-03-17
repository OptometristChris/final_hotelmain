package com.spring.app.common;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public void sendTempPassword(String toEmail, String memberid, String tempPw) throws MessagingException {

        String subject = "[CIEL] 임시 비밀번호 안내";

        String html = ""
            + "<div style='font-family:Arial, sans-serif; line-height:1.6;'>"
            + "<h2 style='color:#8b6a52;'>CIEL 임시 비밀번호</h2>"
            + "<p>요청하신 계정(<b>" + escape(memberid) + "</b>)의 임시 비밀번호입니다.</p>"
            + "<div style='padding:12px; border:1px solid #d8c6b3; border-radius:10px; background:#fffaf5;'>"
            + "  <b style='font-size:18px; color:#8b6a52; letter-spacing:0.04em;'>" + escape(tempPw) + "</b>"
            + "</div>"
            + "<p style='margin-top:12px;'>임시 비밀번호로 로그인한 뒤, 반드시 비밀번호를 변경해주세요.</p>"
            + "<p style='color:#7a6a61; font-size:12px;'>본 메일은 발신전용입니다.</p>"
            + "</div>";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(from);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(html, true);

        mailSender.send(message);
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }
}