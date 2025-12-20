package com.auctionaa.backend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho API lấy role của user hiện tại
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleResponse {
    private int role; // 0: user, 1: buyer, 2: seller
    private String roleName; // Tên role dễ đọc
}

