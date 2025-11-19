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

    @Aggregation(pipeline = {
            "{ $project: { " +
                    "   id: '$_id', " + // thêm dòng này
                    "   roomName: 1, description: 1, " +
                    "   viewCount: '$viewCount', " +
                    "   memberIds: 1, imageAuctionRoom: 1, type: 1, status: 1, " +
                    "   createdAt: 1, updatedAt: 1, " +
                    "   membersCount: { $size: { $ifNull: ['$memberIds', []] } } " + // xem mục 2
                    "} }",

    })
    List<AuctionRoomLiveDTO> findTop6ByMembersCount();

    // (nên sửa tên) dùng contains: List<AuctionRoom> findByMemberIdsContains(String
    // memberId);
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
                    // alias _id -> id để map an toàn sang DTO
                    "   id: '$_id', " +
                    "   roomName: 1, " +
                    "   imageAuctionRoom: 1, " + // <-- sửa đúng tên field của room
                    "   type: 1, " +
                    "   status: 1, " +
                    "   memberIds: 1, " +
                    // lấy viewCount của PHÒNG
                    "   viewCount: '$viewCount', " +
                    // thông tin phiên
                    "   sessionId: '$live._id', " +
                    "   startTime: '$live.startTime', " +
                    "   endTime:   '$live.endedAt', " +
                    "   startingPrice: '$live.startingPrice', " +
                    "   currentPrice:  '$live.currentPrice', " +
                    // mô tả: ưu tiên mô tả phiên, rỗng thì fallback về mô tả phòng
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

}
