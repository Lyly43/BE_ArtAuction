package com.auctionaa.backend.Service;

import com.auctionaa.backend.DTO.Request.SellerRequestDTO;
import com.auctionaa.backend.DTO.Response.SellerRequestResponse;
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

    private final SellerRequestRepository sellerRequestRepo;
    private final UserRepository userRepo;
    private final CloudinaryService cloudinaryService;

    /**
     * Buyer gửi request lên seller
     */
    @Transactional
    public SellerRequestResponse submitSellerRequest(String userId, SellerRequestDTO dto) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Kiểm tra user phải là buyer (role = 1)
        if (user.getRole() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ có Buyer mới có thể gửi request lên Seller. Role hiện tại: " + user.getRole());
        }

        // Kiểm tra đã có request đang pending chưa
        SellerRequest existingRequest = sellerRequestRepo.findByUserId(userId)
                .orElse(null);

        if (existingRequest != null && "PENDING".equals(existingRequest.getStatus())) {
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
        sellerRequest = sellerRequestRepo.save(sellerRequest);

        log.info("Seller request submitted by user {}: {}", userId, sellerRequest.getId());

        return mapToResponse(sellerRequest);
    }

    /**
     * Admin duyệt request → set role = 2 (seller)
     */
    @Transactional
    public SellerRequestResponse approveSellerRequest(String requestId, String adminNote) {
        SellerRequest request = sellerRequestRepo.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ có thể duyệt request đang ở trạng thái PENDING");
        }

        // Update user role = 2 (seller)
        User user = userRepo.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setRole(2); // Seller
        userRepo.save(user);

        // Update request status
        request.setStatus("APPROVED");
        request.setAdminNote(adminNote != null ? adminNote : "Đã được duyệt bởi admin");
        request = sellerRequestRepo.save(request);

        log.info("Seller request {} approved, user {} role updated to 2 (seller)", requestId, user.getId());

        return mapToResponse(request);
    }

    /**
     * Admin từ chối request
     */
    @Transactional
    public SellerRequestResponse rejectSellerRequest(String requestId, String adminNote) {
        SellerRequest request = sellerRequestRepo.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ có thể từ chối request đang ở trạng thái PENDING");
        }

        request.setStatus("REJECTED");
        request.setAdminNote(adminNote != null ? adminNote : "Đã bị từ chối bởi admin");
        request = sellerRequestRepo.save(request);

        log.info("Seller request {} rejected", requestId);

        return mapToResponse(request);
    }

    /**
     * Lấy danh sách request theo status (cho admin)
     */
    public List<SellerRequestResponse> getRequestsByStatus(String status) {
        List<SellerRequest> requests = sellerRequestRepo.findByStatus(status);
        return requests.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy request của user
     */
    public List<SellerRequestResponse> getUserRequests(String userId) {
        List<SellerRequest> requests = sellerRequestRepo.findByUserIdOrderByCreatedAtDesc(userId);
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
