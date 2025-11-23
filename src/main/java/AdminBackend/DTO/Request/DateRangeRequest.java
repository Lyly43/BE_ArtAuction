package AdminBackend.DTO.Request;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DateRangeRequest {
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate begin;
    
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate end;
}

