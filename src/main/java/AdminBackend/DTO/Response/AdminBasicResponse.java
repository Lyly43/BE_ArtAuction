package AdminBackend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminBasicResponse<T> {
    private int status;
    private String message;
    private T data;
}

