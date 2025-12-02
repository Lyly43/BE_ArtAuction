package com.auctionaa.backend.Service;

import com.auctionaa.backend.Entity.Artwork;
import com.auctionaa.backend.Entity.AuctionRoom;
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

import java.math.BigDecimal;

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
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("paymentLink", frontendUrl + "/payment?registrationId=" + registrationId + "&roomId=" + roomId);
        context.setVariable("roomId", roomId);
        sendEmailWithTemplate(toEmail, "Đăng ký tham gia đấu giá thành công", "emails/registration-success", context);
    }

    public void sendArtworkApprovalEmail(String toEmail, String userName, Artwork artwork, String adminNote) {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("artworkTitle", artwork.getTitle());
        context.setVariable("startedPrice", formatCurrency(artwork.getStartedPrice()));
        context.setVariable("material", artwork.getMaterial());
        context.setVariable("size", artwork.getSize());
        context.setVariable("adminNote", adminNote);
        context.setVariable("detailLink", frontendUrl + "/artworks/" + artwork.getId());
        sendEmailWithTemplate(toEmail, "Tác phẩm của bạn đã được duyệt", "emails/artwork-approved", context);
    }

    public void sendArtworkRejectionEmail(String toEmail, String userName, Artwork artwork, String reason, String adminNote) {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("artworkTitle", artwork.getTitle());
        context.setVariable("reason", reason);
        context.setVariable("adminNote", adminNote);
        context.setVariable("detailLink", frontendUrl + "/artworks/" + artwork.getId());
        sendEmailWithTemplate(toEmail, "Tác phẩm của bạn chưa được duyệt", "emails/artwork-rejected", context);
    }

    /**
     * Gửi email cảnh báo cho user do báo cáo
     */
    public void sendUserWarningEmail(String toEmail, String userName, String reportType, String reason, String adminNote, long reportCount) {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("reportType", reportType);
        context.setVariable("reason", reason);
        context.setVariable("adminNote", adminNote);
        context.setVariable("reportCount", reportCount);
        context.setVariable("profileLink", frontendUrl + "/profile");
        context.setVariable("supportLink", frontendUrl + "/support");
        context.setVariable("contactEmail", "support@artauction.com");
        context.setVariable("contactPhone", "1900-xxxx");
        sendEmailWithTemplate(toEmail, "Cảnh báo từ hệ thống", "emails/user-warning", context);
    }

    /**
     * Gửi email thông báo user bị chặn
     */
    public void sendUserBlockedEmail(String toEmail, String userName, String reportType, String reason, String adminNote) {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("reportType", reportType);
        context.setVariable("reason", reason);
        context.setVariable("adminNote", adminNote);
        context.setVariable("supportLink", frontendUrl + "/support");
        context.setVariable("appealLink", frontendUrl + "/support/appeal");
        context.setVariable("contactEmail", "support@artauction.com");
        context.setVariable("contactPhone", "1900-xxxx");
        sendEmailWithTemplate(toEmail, "Tài khoản của bạn đã bị chặn", "emails/user-blocked", context);
    }

    /**
     * Gửi email thông báo artwork bị từ chối do báo cáo
     */
    public void sendArtworkRejectedByReportEmail(String toEmail, String userName, Artwork artwork, String reportType, String reason, String adminNote) {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("artworkTitle", artwork.getTitle());
        context.setVariable("reportType", reportType);
        context.setVariable("reason", reason);
        context.setVariable("adminNote", adminNote);
        context.setVariable("detailLink", frontendUrl + "/artworks/" + artwork.getId());
        context.setVariable("supportLink", frontendUrl + "/support");
        context.setVariable("appealLink", frontendUrl + "/support/appeal");
        context.setVariable("contactEmail", "support@artauction.com");
        context.setVariable("contactPhone", "1900-xxxx");
        sendEmailWithTemplate(toEmail, "Tác phẩm của bạn đã bị từ chối", "emails/artwork-rejected-by-report", context);
        }

    /**
     * Gửi email thông báo auction room bị đóng
     */
    public void sendRoomClosedEmail(String toEmail, String userName, AuctionRoom room, String reportType, String reason, String adminNote) {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("roomName", room.getRoomName());
        context.setVariable("reportType", reportType);
        context.setVariable("reason", reason);
        context.setVariable("adminNote", adminNote);
        context.setVariable("roomLink", frontendUrl + "/auction-rooms/" + room.getId());
        context.setVariable("supportLink", frontendUrl + "/support");
        context.setVariable("appealLink", frontendUrl + "/support/appeal");
        context.setVariable("contactEmail", "support@artauction.com");
        context.setVariable("contactPhone", "1900-xxxx");
        sendEmailWithTemplate(toEmail, "Phòng đấu giá đã bị đóng", "emails/room-closed", context);
        }

    private void sendEmailWithTemplate(String toEmail, String subject, String template, Context context) {
        validateEmailConfig(toEmail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            String htmlContent = templateEngine.process(template, context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("✅ Email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("❌ Failed to send email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    private void validateEmailConfig(String toEmail) {
        if (fromEmail == null || fromEmail.isEmpty() || fromEmail.equals("your-email@gmail.com")) {
            throw new RuntimeException("Email configuration is missing. Please set spring.mail.username in application.properties");
        }
        if (toEmail == null || toEmail.isEmpty()) {
            throw new RuntimeException("Recipient email is required");
        }
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return amount.stripTrailingZeros().toPlainString();
        }
}

