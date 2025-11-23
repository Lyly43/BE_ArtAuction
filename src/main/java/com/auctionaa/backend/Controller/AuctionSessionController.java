package com.auctionaa.backend.Controller;

import com.auctionaa.backend.DTO.Request.BaseSearchRequest;
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
     * Query params: id, type, dateFrom, dateTo
     * Note: name param không áp dụng cho AuctionSession (không có field name)
     */
    @GetMapping("/search")
    public List<AuctionSession> searchAndFilter(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {

        BaseSearchRequest request = new BaseSearchRequest();
        request.setId(id);
        request.setType(type);

        if (dateFrom != null && !dateFrom.isEmpty()) {
            try {
                request.setDateFrom(java.time.LocalDate.parse(dateFrom));
            } catch (Exception e) {
                // Ignore invalid date format
            }
        }

        if (dateTo != null && !dateTo.isEmpty()) {
            try {
                request.setDateTo(java.time.LocalDate.parse(dateTo));
            } catch (Exception e) {
                // Ignore invalid date format
            }
        }

        return auctionSessionService.searchAndFilter(request);
    }
}
