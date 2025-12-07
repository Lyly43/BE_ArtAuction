package AdminBackend.Controller;

import com.auctionaa.backend.DTO.Response.SellerRequestWithUserResponse;
import com.auctionaa.backend.Service.SellerRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/all-seller-requests")
@RequiredArgsConstructor
public class AdminAllRequestSeller {
    private final SellerRequestService sellerRequestService;

    /**
     * Lấy danh sách tất cả yêu cầu của seller bao gồm:
     * - Tên user (userName)
     * - Trạng thái xử lý request (status)
     * - Ngày tháng năm gửi request (createdAt)
     * - Mô tả khi gửi request (description)
     */
    @GetMapping
    public ResponseEntity<List<SellerRequestWithUserResponse>> getAll() {
        return ResponseEntity.ok(sellerRequestService.getAllSellerRequests());
    }
}
