package com.auctionaa.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User extends BaseEntity {

    @Indexed(unique = true)
    private String username;
    private String password;
    @Indexed(unique = true)
    private String email;
    private String phonenumber;
    private int status; //0: ko hoạt động, 1: Đang hoạt động, 2: Bị chặn
    private String cccd;
    private String address;
    private String avt;          // secure_url
    private String avtPublicId;  // public_id (MỚI)

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private int role; //0:user, 1: buyer, 2:seller
    private LocalDate dateOfBirth;
    private Integer gender; // 0 = male, 1 = female, 2 = other

    @Override
    public String getPrefix() {
        return "U-";
    }
}
