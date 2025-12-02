package AdminBackend.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request để admin xử lý báo cáo
 * 
 * Nghiệp vụ:
 * - User Reports (entityType = 1): 
 *   - "WARNING": Gửi notification và email cảnh báo cho user
 *   - "BLOCK": Chặn user (đổi status = 2) và gửi email thông báo
 *   - "DISMISS": Từ chối báo cáo (status = 3), không có hành động gì
 * 
 * - Artwork Reports (entityType = 2):
 *   - "REJECT": Từ chối artwork (status = 3) và gửi email cho owner
 *   - "DISMISS": Từ chối báo cáo (status = 3), không có hành động gì
 * 
 * - Auction Room Reports (entityType = 3):
 *   - "CLOSE": Đóng room (status = 0) và gửi email cho admin/host
 *   - "DISMISS": Từ chối báo cáo (status = 3), không có hành động gì
 * 
 * - AI Artwork Reports (entityType = 4):
 *   - "REJECT": Từ chối artwork (status = 3) và gửi email cho owner
 *   - "DISMISS": Từ chối báo cáo (status = 3), không có hành động gì
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessReportRequest {
    /**
     * Hành động xử lý:
     * - "WARNING": Cảnh báo (chỉ dùng cho User reports)
     * - "BLOCK": Chặn user (chỉ dùng cho User reports)
     * - "REJECT": Từ chối artwork (dùng cho Artwork và AI Artwork reports)
     * - "CLOSE": Đóng room (chỉ dùng cho Auction Room reports)
     * - "DISMISS": Từ chối báo cáo, không có hành động (dùng cho tất cả)
     */
    private String action;
    
    /**
     * Ghi chú của admin (optional)
     */
    private String adminNote;
}

