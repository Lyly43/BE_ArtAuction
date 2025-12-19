package com.auctionaa.backend.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.mail.fromName:No-Reply}")
    private String fromName;

    public void sendOtpHtml(String toEmail, String otp, long expireMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Your OTP Code");
            helper.setText(buildOtpHtml(otp, expireMinutes), true);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage(), e);
        }
    }

    private String buildOtpHtml(String otp, long expireMinutes) {
        return """
            <div style="font-family:Arial,sans-serif;background:#f6f8fa;padding:24px;">
              <div style="max-width:520px;margin:0 auto;background:#ffffff;border-radius:12px;padding:24px;border:1px solid #e5e7eb;">
                <h2 style="margin:0 0 12px;color:#0f172a;">Verify your email</h2>
                <p style="margin:0 0 16px;color:#334155;line-height:1.6;">
                  Use the OTP code below to verify your account. This code expires in
                  <b>""" + expireMinutes + """
                </p>

                <div style="text-align:center;margin:20px 0;">
                  <div style="display:inline-block;font-size:28px;letter-spacing:6px;
                              padding:14px 18px;border-radius:10px;border:1px dashed #94a3b8;
                              background:#f8fafc;color:#0f172a;font-weight:700;">
                   \s""" + otp + """
                  </div>
                </div>

                <p style="margin:16px 0 0;color:#64748b;font-size:13px;line-height:1.6;">
                  If you didn’t request this, you can ignore this email.
                </p>
              </div>

              <p style="max-width:520px;margin:12px auto 0;color:#94a3b8;font-size:12px;text-align:center;">
               \s""" + fromName + """ 
              • No-reply
              </p>
            </div>
            """;
    }
}
