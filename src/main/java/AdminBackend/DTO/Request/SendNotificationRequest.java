package AdminBackend.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO Request cho việc gửi thông báo từ Admin
 * Tương ứng với form data từ frontend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {
    
    // Nội dung tin nhắn chính
    private String message;
    
    // Loại thông báo: "email" hoặc "system"
    private String type;
    
    // Loại email: "auction" hoặc "payment" (chỉ dùng khi type = "email")
    private String emailType;
    
    // Thông tin đấu giá (chỉ dùng khi emailType = "auction")
    private String auctionName;
    private LocalDate auctionDate;
    private LocalTime auctionTime;
    private List<String> relatedAuctionIds; // Danh sách ID các phiên đấu giá liên quan
    
    // Gửi đến tất cả hay một người cụ thể
    private Boolean sendToAll;
    
    // Email người nhận (khi type = "email" và sendToAll = false)
    private String recipientEmail;
    
    // User ID người nhận (khi type = "system" và sendToAll = false)
    private String recipientId;
    
    // Tin nhắn tùy chỉnh
    private String customNote;
}


