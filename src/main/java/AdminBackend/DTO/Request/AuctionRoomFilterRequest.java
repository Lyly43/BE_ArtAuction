package AdminBackend.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuctionRoomFilterRequest {
    // Room Status: List<Integer> - có thể chọn nhiều status cùng lúc
    // null hoặc empty = bỏ qua filter (lấy tất cả)
    // 0 = Sắp diễn ra (Coming Soon), 1 = Đang diễn ra (Live), 2 = Đã hoàn thành (Finished)
    private List<Integer> statuses;
    
    // Start Time range: Lọc theo startedAt của phòng đấu giá
    private LocalDateTime startTimeFrom; // null = bỏ qua filter
    private LocalDateTime startTimeTo;   // null = bỏ qua filter
    
    // End Time range: Lọc theo stoppedAt của phòng đấu giá
    private LocalDateTime endTimeFrom;   // null = bỏ qua filter
    private LocalDateTime endTimeTo;     // null = bỏ qua filter
    
    // Number of participants: Lọc theo totalMembers (số lượng memberIds)
    // null hoặc "all" = bỏ qua filter, "<10", "10-50", ">50"
    private String participantsRange;
}


