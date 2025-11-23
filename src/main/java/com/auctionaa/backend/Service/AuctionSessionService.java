package com.auctionaa.backend.Service;

import com.auctionaa.backend.DTO.Request.BaseSearchRequest;
import com.auctionaa.backend.Entity.AuctionSession;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuctionSessionService {

    private final GenericSearchService genericSearchService;

    public AuctionSessionService(GenericSearchService genericSearchService) {
        this.genericSearchService = genericSearchService;
    }

    /**
     * Tìm kiếm và lọc auction session (history)
     * - Tìm theo ID
     * - Tìm theo auctionRoomId (tên phòng - thông qua ID)
     * - Lọc theo type (thể loại)
     * - Lọc theo startTime (ngày)
     */
    public List<AuctionSession> searchAndFilter(BaseSearchRequest request) {
        return genericSearchService.searchAndFilter(
                request,
                AuctionSession.class,
                "_id", // idField
                null, // nameField (không có field name, chỉ có auctionRoomId)
                "type", // typeField
                "startTime" // dateField (lọc theo startTime)
        );
    }
}
