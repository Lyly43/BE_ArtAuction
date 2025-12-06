package com.auctionaa.backend.DTO.Response;

import com.auctionaa.backend.Entity.AuctionRoom;
import com.auctionaa.backend.Entity.AuctionSession;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomDetailDTO {
    private AuctionRoom room;
    private List<AuctionSession> sessions;
}

