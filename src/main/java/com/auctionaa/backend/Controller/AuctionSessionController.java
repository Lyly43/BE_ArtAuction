package com.auctionaa.backend.Controller;

import com.auctionaa.backend.DTO.Request.BaseSearchRequest;
import com.auctionaa.backend.DTO.Response.SearchResponse;
import com.auctionaa.backend.Entity.AuctionSession;
import com.auctionaa.backend.Service.AuctionSessionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/history")
public class AuctionSessionController {

    private final AuctionSessionService auctionSessionService;

    public AuctionSessionController(AuctionSessionService auctionSessionService) {
        this.auctionSessionService = auctionSessionService;
    }

    /**
     * Tìm kiếm và lọc auction session (history)
     * Request body (JSON): id, type, dateFrom, dateTo
     * Note: name param không áp dụng cho AuctionSession (không có field name)
     * Có thể gửi body rỗng {} để lấy tất cả
     */
    @PostMapping("/search")
    public SearchResponse<AuctionSession> searchAndFilter(@RequestBody(required = false) BaseSearchRequest request) {
        // Nếu request null hoặc không có body, tạo object mới (lấy tất cả)
        if (request == null) {
            request = new BaseSearchRequest();
        }
        List<AuctionSession> results = auctionSessionService.searchAndFilter(request);
        return SearchResponse.success(results);
    }
}
