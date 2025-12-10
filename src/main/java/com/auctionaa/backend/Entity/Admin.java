package com.auctionaa.backend.Entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "admins")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Admin extends BaseEntity {

    private String fullName;

    private String email;

    private String password;

    private String phoneNumber;

    private String address;

    private String avatar;

    private Integer status; // 0 = Bị Khóa, 1 = Hoạt động

    private Integer role; // 3: Admin , 4: Super Admin

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Override
    public String getPrefix() {
        return "Ad-";
    }
}
