package com.auctionaa.backend.DTO.Request;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor
public class UserRequest {
    private String username;

    // server sẽ chặn đổi email ở endpoint này (chỉ dùng để FE gửi lại đúng email cũ hoặc bỏ trống)
    private String email;

    private String phonenumber;
    private String cccd;
    private String address;


    private LocalDate dateOfBirth;

    private Integer gender; // 0,1,2
}
