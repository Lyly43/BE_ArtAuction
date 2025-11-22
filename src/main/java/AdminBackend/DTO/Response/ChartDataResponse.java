package AdminBackend.DTO.Response;

import lombok.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ChartDataResponse {
    private int status;
    private String message;
    private List<ChartDataItem> data;
    private List<String> labels;
    private List<ChartDataset> datasets;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class ChartDataItem {
        private String date;
        private Long count;
        private Double totalAmount; // Cho doanh thu
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class ChartDataset {
        private String label;
        private List<Object> data;
        private List<String> backgroundColor;
    }
}


