package com.auctionaa.backend.Service;

import AdminBackend.Repository.AdminRepository;
import com.auctionaa.backend.Entity.AuctionRoom;
import com.auctionaa.backend.Entity.Notification;
import com.auctionaa.backend.Repository.AuctionRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service định kỳ kiểm tra các phòng đấu giá sắp diễn ra nhưng ít người tham gia
 * và gửi thông báo + email cho admin phụ trách phòng.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionRoomReminderService {

    private final AuctionRoomRepository auctionRoomRepository;
    private final AdminRepository adminRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    /**
     * Mỗi 5 phút kiểm tra các phòng:
     * - status = 0 (Sắp diễn ra)
     * - thời gian tới giờ bắt đầu còn trong khoảng [0, 3 giờ]
     * - số lượng memberIds <= 1
     * - chưa gửi cảnh báo trước đó (lowMemberWarningSent = null/false)
     */
    @Scheduled(fixedDelayString = "${auction.room.reminder-interval-ms:300000}")
    public void notifyAdminsForLowMemberRooms() {
        LocalDateTime now = LocalDateTime.now();

        List<AuctionRoom> upcomingRooms = auctionRoomRepository.findByStatus(0);
        if (upcomingRooms.isEmpty()) {
            return;
        }

        for (AuctionRoom room : upcomingRooms) {
            try {
                if (room.getStartedAt() == null) {
                    continue;
                }

                // Bỏ qua nếu đã gửi cảnh báo trước đó
                if (Boolean.TRUE.equals(room.getLowMemberWarningSent())) {
                    continue;
                }

                Duration duration = Duration.between(now, room.getStartedAt());
                long minutesUntilStart = duration.toMinutes();

                // Chỉ quan tâm các phòng còn từ 0 đến 180 phút (3h)
                if (minutesUntilStart < 0 || minutesUntilStart > 180) {
                    continue;
                }

                int memberCount = CollectionUtils.isEmpty(room.getMemberIds())
                        ? 0
                        : room.getMemberIds().size();

                // Điều kiện: memberIds không lớn hơn 1
                if (memberCount > 1) {
                    continue;
                }

                if (!StringUtils.hasText(room.getAdminId())) {
                    continue;
                }

                adminRepository.findById(room.getAdminId()).ifPresent(admin -> {
                    // Gửi email cho admin phụ trách phòng
                    try {
                        long hoursLeft = Math.max(1, minutesUntilStart / 60);
                        emailService.sendAuctionRoomLowMembersWarningToAdmin(
                                admin.getEmail(),
                                admin.getFullName(),
                                room,
                                memberCount,
                                hoursLeft
                        );
                    } catch (Exception e) {
                        log.error("Failed to send low-member email for room {} to admin {}", room.getId(), admin.getId(), e);
                    }

                    // Tạo notification cho admin (dựa trên adminId)
                    try {
                        Notification noti = new Notification();
                        noti.setUserId(admin.getId());
                        noti.setNotificationType(2);
                        noti.setTitle("Phòng đấu giá sắp diễn ra nhưng ít người tham gia");
                        noti.setNotificationContent("Phòng '" + room.getRoomName() +
                                "' sắp đến giờ bắt đầu nhưng chỉ có " + memberCount +
                                " người tham gia. Vui lòng kiểm tra và quyết định hoãn hoặc tiếp tục.");
                        noti.setNotificationStatus(1);
                        noti.setRefId(room.getId());
                        notificationService.addNotification(noti);
                    } catch (Exception e) {
                        log.error("Failed to create notification for low-member room {} to admin {}", room.getId(), admin.getId(), e);
                    }
                });

                // Đánh dấu đã gửi cảnh báo để không gửi lại nhiều lần
                room.setLowMemberWarningSent(true);
                room.setUpdatedAt(LocalDateTime.now());
                auctionRoomRepository.save(room);

            } catch (Exception ex) {
                log.error("Error while processing low-member reminder for room {}", room.getId(), ex);
            }
        }
    }
}



