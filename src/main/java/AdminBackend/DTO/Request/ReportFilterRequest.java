package AdminBackend.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportFilterRequest {
    // Report Status filter: null hoặc empty = All, có thể chọn nhiều status
    // 0 = PENDING (Chờ xử lý)
    // 1 = INVESTIGATING (Đang điều tra)
    // 2 = RESOLVED (Đã giải quyết)
    private List<Integer> reportStatuses;
    
    // Object Type filter: null hoặc empty = All, có thể chọn nhiều entity type
    // Lấy từ ReportConstants: 1=User, 2=Artwork, 3=Auction Room, 4=AI Artwork
    private List<Integer> objectTypes;
    
    // Time of Report filter: khoảng thời gian tạo báo cáo
    private LocalDate createdAtFrom;
    private LocalDate createdAtTo;
}


