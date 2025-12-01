package com.auctionaa.backend.Repository;

import com.auctionaa.backend.DTO.Response.AuctionRoomLiveDTO;
import com.auctionaa.backend.Entity.AuctionRoom;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRoomRepository extends MongoRepository<AuctionRoom, String> {

        // Tìm phòng theo memberId
        List<AuctionRoom> findByMemberIdsContaining(String memberId);

        /**
         * Lấy 6 phòng featured theo tổng giá của 2 tranh có giá cao nhất trong room
         * (dựa trên trường starting_price trong collection auction_sessions).
         */
        @Aggregation(pipeline = {
                        // Lấy ra tối đa 2 session có starting_price cao nhất cho mỗi room
                        "{ $lookup: { " +
                                        "   from: 'auction_sessions', " +
                                        "   let: { roomId: '$_id' }, " +
                                        "   pipeline: [" +
                                        "     { $match: { $expr: { $eq: ['$auctionRoomId', '$$roomId'] } } }," +
                                        "     { $sort: { starting_price: -1 } }," +
                                        "     { $limit: 2 }" +
                                        "   ], " +
                                        "   as: 'topSessions' } }",
                        // Tính tổng giá của 2 tranh top (nếu <2 thì cộng những cái có)
                        "{ $addFields: { totalPrice: { $sum: '$topSessions.starting_price' } } }",
                        // Sort theo tổng giá giảm dần và giới hạn 6 phòng
                        "{ $sort: { totalPrice: -1 } }",
                        "{ $limit: 6 }",
                        // Project ra DTO cho FE dùng
                        "{ $project: { " +
                                        "   id: '$_id', " +
                                        "   roomName: 1, description: 1, " +
                                        "   viewCount: \"$viewCount\", " +
                                        "   depositAmount: { $convert: { input: \"$depositAmount\", to: \"decimal\" } }, "
                                        +
                                        "   memberIds: 1, imageAuctionRoom: 1, type: 1, status: 1 " +
                                        "} }"
        })
        List<AuctionRoomLiveDTO> findTop6ByMembersCount();

        @Aggregation(pipeline = {
                        "{ $lookup: { " +
                                        "   from: 'auction_sessions', " +
                                        "   let: { roomId: '$_id' }, " +
                                        "   pipeline: [" +
                                        "     { $match: { $expr: { $and: [ " +
                                        "         { $eq: ['$auctionRoomId', '$$roomId'] }, " +
                                        "         { $eq: ['$status', ?0] } " +
                                        "     ] } } }," +
                                        "     { $sort: { startTime: -1 } }," +
                                        "     { $limit: 1 }" +
                                        "   ], " +
                                        "   as: 'live' } }",
                        "{ $addFields: { live: { $first: '$live' } } }",
                        "{ $project: { " +
                                        "   id: '$_id', " +
                                        "   roomName: 1, " +
                                        "   imageAuctionRoom: 1, " +
                                        "   type: 1, " +
                                        "   status: 1, " +
                                        "   memberIds: 1, " +
                                        // Chuyển về dùng $convert với cú pháp JSON chuẩn để tránh Spring cố convert
                                        // chuỗi "$depositAmount" thành Decimal128
                                        "   depositAmount: { $convert: { input: \"$depositAmount\", to: \"decimal\" } }, "
                                        +
                                        "   viewCount: \"$viewCount\", " +
                                        "   sessionId: '$live._id', " +
                                        "   startTime: '$live.startTime', " +
                                        "   endTime:   '$live.endedAt', " +
                                        "   startingPrice: '$live.startingPrice', " +
                                        "   currentPrice:  '$live.currentPrice', " +
                                        "   description: { $ifNull: ['$live.description', '$description'] }" +
                                        "} }"
        })
        List<AuctionRoomLiveDTO> findRoomsWithLivePrices(int runningStatus);

        // Tìm kiếm theo ID (exact match)
        Optional<AuctionRoom> findById(String id);

        // Tìm kiếm theo tên phòng (case-insensitive, partial match)
        List<AuctionRoom> findByRoomNameContainingIgnoreCase(String roomName);

        // Lọc theo thể loại (type)
        List<AuctionRoom> findByType(String type);

        // Lọc theo thể loại và tên phòng
        List<AuctionRoom> findByTypeAndRoomNameContainingIgnoreCase(String type, String roomName);

        // Lọc theo ngày tạo (từ ngày)
        @Query("{ 'createdAt': { $gte: ?0 } }")
        List<AuctionRoom> findByCreatedAtGreaterThanEqual(LocalDateTime dateFrom);

        // Lọc theo ngày tạo (đến ngày)
        @Query("{ 'createdAt': { $lte: ?0 } }")
        List<AuctionRoom> findByCreatedAtLessThanEqual(LocalDateTime dateTo);

        // Lọc theo ngày tạo (khoảng thời gian)
        @Query("{ 'createdAt': { $gte: ?0, $lte: ?1 } }")
        List<AuctionRoom> findByCreatedAtBetween(LocalDateTime dateFrom, LocalDateTime dateTo);

        long countByStatus(int status);

        // List<AuctionRoom> findByRoomNameContainingIgnoreCase(String roomName);

}
