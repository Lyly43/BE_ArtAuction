package AdminBackend.DTO.Response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdateResponse<T> {
    private boolean success;
    private String message;
    private T data;
}

