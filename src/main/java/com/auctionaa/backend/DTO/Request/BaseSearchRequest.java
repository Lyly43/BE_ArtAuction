package com.auctionaa.backend.DTO.Request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Base DTO cho tìm kiếm và lọc các entity
 * Các entity cụ thể có thể extend class này để thêm các field riêng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseSearchRequest {
    private String id; // Tìm kiếm theo ID (exact match)
    private String name; // Tìm kiếm theo tên (partial match) - có thể là title, roomName, artworkTitle,
    // etc.
    private String type; // Lọc theo thể loại/type/genre

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFrom; // Lọc từ ngày

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateTo; // Lọc đến ngày
}
