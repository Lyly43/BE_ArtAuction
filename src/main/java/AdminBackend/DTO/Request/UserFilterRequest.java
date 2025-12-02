package AdminBackend.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFilterRequest {
    // Status filter: null = All, 1 = Active, 2 = Locked
    private Integer status;
    
    // Gender filter: null = All, 0 = Male, 1 = Female, 2 = Other
    private Integer gender;
    
    // Province/City filter
    private String province;
    
    // Date of birth range
    private LocalDate dateOfBirthFrom;
    private LocalDate dateOfBirthTo;
    
    // Account creation date filter: "last7days", "thismonth", or null
    private String createdAtFilter;
}


