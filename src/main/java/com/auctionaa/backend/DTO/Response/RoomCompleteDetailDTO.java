package com.auctionaa.backend.DTO.Response;

import com.auctionaa.backend.Entity.AuctionRoom;
import com.auctionaa.backend.Entity.AuctionSession;
import com.auctionaa.backend.Entity.Artwork;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomCompleteDetailDTO {
    private AuctionRoom room;
    private List<SessionWithArtworkDTO> sessions;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SessionWithArtworkDTO {
        private AuctionSession session;
        private Artwork artwork;
    }
}

