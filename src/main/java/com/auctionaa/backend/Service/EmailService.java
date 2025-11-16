package com.auctionaa.backend.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Gửi email thông báo đăng ký đấu giá thành công
     * @param toEmail Email người nhận
     * @param userName Tên người dùng
     * @param registrationId ID đăng ký để tạo link thanh toán
     * @param roomId ID phòng đấu giá
     */
    public void sendRegistrationSuccessEmail(String toEmail, String userName, String registrationId, String roomId) {
        log.info("Attempting to send email to: {}, userName: {}, registrationId: {}, roomId: {}",
                toEmail, userName, registrationId, roomId);
        log.info("Email config - fromEmail: {}, frontendUrl: {}", fromEmail, frontendUrl);

        // Validate email config
        if (fromEmail == null || fromEmail.isEmpty() || fromEmail.equals("your-email@gmail.com")) {
            log.error("Email configuration is missing or not set! Please update application.properties");
            throw new RuntimeException("Email configuration is missing. Please set spring.mail.username in application.properties");
        }

        if (toEmail == null || toEmail.isEmpty()) {
            log.error("Recipient email is null or empty!");
            throw new RuntimeException("Recipient email is required");
        }

        try {
            log.info("Creating MimeMessage...");
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Đăng ký tham gia đấu giá thành công");

            // Tạo link thanh toán hợp đồng và hồ sơ
            String paymentLink = frontendUrl + "/payment?registrationId=" + registrationId + "&roomId=" + roomId;
            log.info("Payment link: {}", paymentLink);

            // Tạo context cho Thymeleaf template
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("paymentLink", paymentLink);
            context.setVariable("roomId", roomId);

            // Render template Thymeleaf
            String htmlContent = templateEngine.process("emails/registration-success", context);
            log.info("Email template rendered successfully");

            helper.setText(htmlContent, true);

            log.info("Sending email via JavaMailSender...");
            mailSender.send(message);
            log.info("✅ Email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("❌ Failed to send email to: {}", toEmail, e);
            log.error("Exception details: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("Root cause: {}", e.getCause().getMessage());
            }
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Unexpected error while sending email to: {}", toEmail, e);
            throw new RuntimeException("Unexpected error while sending email: " + e.getMessage(), e);
        }
    }

}

