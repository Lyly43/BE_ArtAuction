package com.auctionaa.backend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho member của Auction Room
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {
    private String id; // User ID
    private String username; // Tên user
    private String avt; // Avatar URL
}
