package com.auctionaa.backend.Service;

import com.auctionaa.backend.Config.MbProps;
import com.auctionaa.backend.Entity.AuctionRoom;
import com.auctionaa.backend.Entity.MbTxn;
import com.auctionaa.backend.DTO.Request.AuctionRegistrationRequest;
import com.auctionaa.backend.DTO.Response.AuctionRegistrationResponse;
import com.auctionaa.backend.Service.MbClient;
import com.auctionaa.backend.Repository.AuctionRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AuctionRoomDepositService {

    private static final BigDecimal APPLICATION_FEE = new BigDecimal("100000"); // phí hồ sơ

    private final AuctionRoomRepository auctionRoomRepository;
    private final MbClient mbClient;
    private final MbProps mbProps;

    // 🔹 THANH TOÁN CỌC
    public AuctionRegistrationResponse createQrAndCheck(
            String roomId,
            String userId
    ) {
        AuctionRoom room = auctionRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Auction room không tồn tại"));

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId không được để trống");
        }

        // LẤY TIỀN CỌC TỪ auction_rooms.depositAmount
        BigDecimal amount = room.getDepositAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("depositAmount của phòng không hợp lệ");
        }

        // Ghi chú: AR-roomId-userId-last4
        String note = generateArNote(roomId, userId);

        return processPayment(
                amount,
                note,
                () -> {
                    addMemberIfNotExists(room, userId);
                    auctionRoomRepository.save(room);
                },
                "Thanh toán cọc thành công, bạn đã được thêm vào phòng đấu giá."
        );
    }

    // 🔹 THANH TOÁN PHÍ HỒ SƠ (100.000 VND)
    public AuctionRegistrationResponse payApplicationFee(String roomId, String userId) {

        // Chỉ để check room tồn tại
        AuctionRoom room = auctionRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Auction room không tồn tại"));

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId không được để trống");
        }

        BigDecimal amount = APPLICATION_FEE;
        String note = generateAppFeeNote(roomId, userId);

        // Phí hồ sơ KHÔNG add member (tùy nghiệp vụ)
        return processPayment(
                amount,
                note,
                null,
                "Thanh toán phí hồ sơ thành công."
        );
    }

    // 🔹 THANH TOÁN COMBO: PHÍ HỒ SƠ + TIỀN CỌC
    public AuctionRegistrationResponse payApplicationFeeAndDeposit(
            String roomId,
            String userId
    ) {
        AuctionRoom room = auctionRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Auction room không tồn tại"));

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId không được để trống");
        }

        BigDecimal deposit = room.getDepositAmount();
        if (deposit == null || deposit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("depositAmount của phòng không hợp lệ");
        }

        BigDecimal total = deposit.add(APPLICATION_FEE);   // combo

        String note = generateComboNote(roomId, userId);

        return processPayment(
                total,
                note,
                () -> {
                    // combo này coi như đủ điều kiện vào phòng
                    addMemberIfNotExists(room, userId);
                    auctionRoomRepository.save(room);
                },
                "Thanh toán phí hồ sơ và tiền cọc thành công, bạn đã được thêm vào phòng đấu giá."
        );
    }

    // ================== HELPER METHODS ==================

    private String generateArNote(String roomId, String userId) {
        String roomSuffix = (roomId != null && roomId.length() > 4)
                ? roomId.substring(roomId.length() - 4)
                : roomId;

        String userSuffix = (userId != null && userId.length() > 4)
                ? userId.substring(userId.length() - 4)
                : userId;

        String millis = String.valueOf(System.currentTimeMillis());
        String last4 = millis.substring(millis.length() - 4);

        return "AR-" + roomSuffix + "-" + userSuffix + "-" + last4;
    }

    private String generateAppFeeNote(String roomId, String userId) {
        String roomSuffix = (roomId != null && roomId.length() > 4)
                ? roomId.substring(roomId.length() - 4)
                : roomId;

        String userSuffix = (userId != null && userId.length() > 4)
                ? userId.substring(userId.length() - 4)
                : userId;

        String millis = String.valueOf(System.currentTimeMillis());
        String last4 = millis.substring(millis.length() - 4);

        return "AF-" + roomSuffix + "-" + userSuffix + "-" + last4; // AF = Application Fee
    }

    private String generateComboNote(String roomId, String userId) {
        String roomSuffix = (roomId != null && roomId.length() > 4)
                ? roomId.substring(roomId.length() - 4)
                : roomId;

        String userSuffix = (userId != null && userId.length() > 4)
                ? userId.substring(userId.length() - 4)
                : userId;

        String millis = String.valueOf(System.currentTimeMillis());
        String last4 = millis.substring(millis.length() - 4);

        return "ARF-" + roomSuffix + "-" + userSuffix + "-" + last4; // ARF = Auction + Registration Fee
    }

    private String url(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private boolean hasMatchingTransaction(BigDecimal amount, String note) {
        LocalDate today = LocalDate.now();
        List<MbTxn> txns = mbClient.fetchRecentTransactions(
                today.minusDays(1),
                today
        );

        if (txns == null || txns.isEmpty()) {
            return false;
        }

        return txns.stream().anyMatch(tx -> {
            String credit = tx.getCreditAmount(); // chỉnh theo field thật trong MbTxn
            if (credit == null) return false;

            // TODO: convert credit -> BigDecimal cho chuẩn, VD:
            // BigDecimal creditAmount = new BigDecimal(credit);
            // if (creditAmount.compareTo(amount) != 0) return false;

            if (!credit.equals(amount.toPlainString())) {
                return false;
            }

            String desc = tx.getDescription();
            return desc != null && desc.contains(note);
        });
    }

    private void addMemberIfNotExists(AuctionRoom room, String userId) {
        if (room.getMemberIds() == null) {
            room.setMemberIds(new ArrayList<>());
        }
        if (!room.getMemberIds().contains(userId)) {
            room.getMemberIds().add(userId);
        }
    }

    private AuctionRegistrationResponse processPayment(
            BigDecimal amount,
            String note,
            Runnable onPaidAction,
            String successMessage
    ) {
        // Tạo URL ảnh QR (ẩn số tài khoản, chỉ show QR)
        String qrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%s&addInfo=%s",
                url(mbProps.getBankCode()),
                url(mbProps.getAccountNo()),
                url(amount.toPlainString()),
                url(note)
        );

        // Check giao dịch từ MB
        boolean paid = hasMatchingTransaction(amount, note);

        String message;
        if (paid) {
            if (onPaidAction != null) {
                onPaidAction.run();
            }
            message = successMessage;
        } else {
            message = "Chưa tìm thấy giao dịch tương ứng. Vui lòng chuyển khoản theo QR và chờ hệ thống xác nhận.";
        }

        return new AuctionRegistrationResponse(qrUrl, note, paid, message);
    }
}