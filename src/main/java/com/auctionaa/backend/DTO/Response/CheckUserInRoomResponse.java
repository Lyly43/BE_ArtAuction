package com.auctionaa.backend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho API kiểm tra user có trong member list của auction room
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckUserInRoomResponse {
    private int status; // 1 = có, 0 = không có
    private String message;
}

