package com.auctionaa.backend.Controller;

import com.auctionaa.backend.DTO.Request.PlaceBidRequest;
import com.auctionaa.backend.DTO.Response.PlaceBidResponse;
import com.auctionaa.backend.Jwt.JwtUtil;
import com.auctionaa.backend.Service.BidService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;
    private final JwtUtil jwtUtil;

    @PostMapping("/{auctionRoomId}/place")
    public ResponseEntity<PlaceBidResponse> placeByRoom(
            @PathVariable String auctionRoomId,
            @RequestBody PlaceBidRequest body,
            @RequestHeader("Authorization") String authHeader) {

        String userId = jwtUtil.extractUserId(authHeader);

        return ResponseEntity.ok(
                bidService.placeBidByRoom(userId, auctionRoomId, body.getAmount(),
                        body.getIdempotencyKey()));
    }
}
