package com.auctionaa.backend.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO để lấy danh sách member của phòng đấu giá
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetRoomMembersRequest {
    private String roomId; // ID của phòng đấu giá
}

