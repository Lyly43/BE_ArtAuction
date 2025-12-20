package com.auctionaa.backend.Service;

import com.auctionaa.backend.DTO.Request.SellerRequestDTO;
import com.auctionaa.backend.DTO.Response.SellerRequestResponse;
import com.auctionaa.backend.DTO.Response.SellerRequestWithUserResponse;
import com.auctionaa.backend.Entity.Notification;
import com.auctionaa.backend.Entity.SellerRequest;
import com.auctionaa.backend.Entity.User;
import com.auctionaa.backend.Repository.SellerRequestRepository;
import com.auctionaa.backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerRequestService {

    private final SellerRequestRepository sellerRequestRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final NotificationService notificationService;

    public List<SellerRequestWithUserResponse> getAllSellerRequests() {
        List<SellerRequest> requests = sellerRequestRepository.findAll();

        return requests.stream().map(req -> {
            var user = userRepository.findById(req.getUserId()).orElse(null);

            return SellerRequestWithUserResponse.builder()
                    .requestId(req.getId())
                    .userId(req.getUserId())
                    .userName(user != null ? user.getUsername() : null)
                    .verificationImageUrl(req.getVerificationImageUrl())
                    .description(req.getDescription())
                    .status(req.getStatus())
                    .adminNote(req.getAdminNote())
                    .createdAt(req.getCreatedAt())
                    .updatedAt(req.getUpdatedAt())
                    .build();
        }).toList();
    }
    /**
     * Buyer gửi request lên seller
     */
    @Transactional
    public SellerRequestResponse submitSellerRequest(String userId, SellerRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Kiểm tra user phải là buyer (role = 1)
        if (user.getRole() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ có Buyer mới có thể gửi request lên Seller. Role hiện tại: " + user.getRole());
        }

        // Kiểm tra đã có request đang pending chưa
        // Sử dụng findByUserIdOrderByCreatedAtDesc để tránh lỗi non-unique result
        List<SellerRequest> existingRequests = sellerRequestRepository.findByUserIdOrderByCreatedAtDesc(userId);
        boolean hasPendingRequest = existingRequests.stream()
                .anyMatch(req -> "PENDING".equals(req.getStatus()));

        if (hasPendingRequest) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bạn đã có request đang chờ duyệt. Vui lòng đợi admin xử lý.");
        }

        // Upload ảnh chứng thực lên Cloudinary
        String imageUrl;
        try {
            if (dto.getVerificationImage() == null || dto.getVerificationImage().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ảnh chứng thực không được để trống");
            }

            String folder = "auctionaa/seller-verification/" + userId;
            CloudinaryService.UploadResult uploadResult = cloudinaryService.uploadImage(
                    dto.getVerificationImage(),
                    folder,
                    "seller-verification",
                    null);
            imageUrl = uploadResult.getUrl();
        } catch (IOException e) {
            log.error("Failed to upload verification image for user {}", userId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể upload ảnh chứng thực: " + e.getMessage());
        }

        // Tạo SellerRequest
        SellerRequest sellerRequest = SellerRequest.builder()
                .userId(userId)
                .verificationImageUrl(imageUrl)
                .description(dto.getDescription())
                .status("PENDING")
                .adminNote(null)
                .build();

        sellerRequest.generateId();
        sellerRequest = sellerRequestRepository.save(sellerRequest);

        log.info("Seller request submitted by user {}: {}", userId, sellerRequest.getId());

        return mapToResponse(sellerRequest);
    }

    /**
     * Admin duyệt request → set role = 2 (seller)
     */
    @Transactional
    public SellerRequestResponse approveSellerRequest(String requestId, String adminNote) {
        SellerRequest request = sellerRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ có thể duyệt request đang ở trạng thái PENDING");
        }

        // Update user role = 2 (seller)
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setRole(2); // Seller
        userRepository.save(user);

        // Update request status
        request.setStatus("APPROVED");
        request.setAdminNote(adminNote != null ? adminNote : "Đã được duyệt bởi admin");
        request = sellerRequestRepository.save(request);

        // Tạo thông báo cho seller
        Notification notification = new Notification();
        notification.setUserId(user.getId());
        notification.setTitle("Seller Request Approved");
        notification.setNotificationContent("Your request to become a Seller has been approved. Please proceed with the confirmation.");
        notification.setNotificationType(0); // Có thể điều chỉnh theo loại notification
        notification.setNotificationStatus(1); // 1 = đã gửi
        notification.setRefId(requestId); // Tham chiếu đến request ID
        notificationService.addNotification(notification);

        log.info("Seller request {} approved, user {} role updated to 2 (seller). Notification sent.", requestId, user.getId());

        return mapToResponse(request);
    }

    /**
     * Admin từ chối request
     */
    @Transactional
    public SellerRequestResponse rejectSellerRequest(String requestId, String adminNote) {
        SellerRequest request = sellerRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ có thể từ chối request đang ở trạng thái PENDING");
        }

        request.setStatus("REJECTED");
        request.setAdminNote(adminNote != null ? adminNote : "Đã bị từ chối bởi admin");
        request = sellerRequestRepository.save(request);

        log.info("Seller request {} rejected", requestId);

        return mapToResponse(request);
    }

    /**
     * Lấy danh sách request theo status (cho admin)
     */
    public List<SellerRequestResponse> getRequestsByStatus(String status) {
        List<SellerRequest> requests = sellerRequestRepository.findByStatus(status);
        return requests.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy request của user
     */
    public List<SellerRequestResponse> getUserRequests(String userId) {
        List<SellerRequest> requests = sellerRequestRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return requests.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private SellerRequestResponse mapToResponse(SellerRequest request) {
        return SellerRequestResponse.builder()
                .id(request.getId())
                .userId(request.getUserId())
                .verificationImageUrl(request.getVerificationImageUrl())
                .description(request.getDescription())
                .status(request.getStatus())
                .adminNote(request.getAdminNote())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
