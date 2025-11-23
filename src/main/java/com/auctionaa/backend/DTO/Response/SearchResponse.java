package com.auctionaa.backend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO cho các API search
 *
 * @param <T> Type của entity trong kết quả
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse<T> {
    private boolean success;
    private String message;
    private List<T> data;
    private int count;

    /**
     * Tạo response thành công với dữ liệu
     */
    public static <T> SearchResponse<T> success(List<T> data) {
        if (data == null || data.isEmpty()) {
            return new SearchResponse<>(false, "Không tìm thấy kết quả nào", data, 0);
        }
        return new SearchResponse<>(true, "Tìm thấy " + data.size() + " kết quả", data, data.size());
    }

    /**
     * Tạo response không tìm thấy
     */
    public static <T> SearchResponse<T> notFound() {
        return new SearchResponse<>(false, "Không tìm thấy kết quả nào", null, 0);
    }
}
