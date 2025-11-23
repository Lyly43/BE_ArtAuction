package AdminBackend.DTO.Response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdateResponse<T> {
    private int status;
    private String message;
    private T data;
}

