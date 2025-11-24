package AdminBackend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyComparisonResponse {
    private int status;
    private String message;
    private MonthlyComparisonData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyComparisonData {
        private MonthData currentMonth;
        private MonthData previousMonth;
        private ChangeData change;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthData {
        private long total;
        private String month; // Format: "MM/yyyy"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangeData {
        private long amount; // Số thay đổi (có thể âm)
        private double percentage; // Phần trăm thay đổi
        private boolean isIncrease; // true nếu tăng, false nếu giảm
    }
}



