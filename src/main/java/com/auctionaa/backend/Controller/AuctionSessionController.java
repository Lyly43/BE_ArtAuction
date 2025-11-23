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
     */
    @PostMapping("/search")
    public SearchResponse<AuctionSession> searchAndFilter(@RequestBody BaseSearchRequest request) {
        List<AuctionSession> results = auctionSessionService.searchAndFilter(request);
        return SearchResponse.success(results);
    }
}
