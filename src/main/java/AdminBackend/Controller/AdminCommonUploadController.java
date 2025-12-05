package AdminBackend.Controller;

import AdminBackend.DTO.Response.AdminBasicResponse;
import com.auctionaa.backend.Service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/uploads")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminCommonUploadController {

    @Autowired
    private CloudinaryService cloudinaryService;

    /**
     * POST /api/admin/uploads/upload-image
     * Upload ảnh chung từ thiết bị và trả về URL
     * 
     * Endpoint này dùng chung cho TẤT CẢ các trường hợp upload ảnh trong admin:
     * - Upload avatar admin
     * - Upload ảnh phòng đấu giá
     * - Upload ảnh bất kỳ khác
     * 
     * Request:
     * - Body: form-data
     * - Key: `imageFile` (bắt buộc)
     * - Value: File (chọn file ảnh từ máy tính)
     * 
     * Response:
     * {
     *   "status": 1,
     *   "message": "Upload ảnh thành công",
     *   "data": {
     *     "imageUrl": "https://cloudinary.com/...",
     *     "publicId": "auctionaa/misc/common-..."
     *   }
     * }
     * 
     * Frontend sẽ lấy `imageUrl` từ response và gửi vào field tương ứng của các API khác.
     */
    @PostMapping("/upload-image")
    public ResponseEntity<AdminBasicResponse<Map<String, String>>> uploadImage(
            @RequestParam("imageFile") MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new AdminBasicResponse<>(0, "File ảnh không được để trống", null));
        }

        try {
            // Dùng core uploadImage với folder "auctionaa/misc"
            String folder = "auctionaa/misc";
            String publicId = "common-" + System.currentTimeMillis();

            CloudinaryService.UploadResult uploadResult =
                    cloudinaryService.uploadImage(imageFile, folder, publicId, null);

            Map<String, String> data = new HashMap<>();
            data.put("imageUrl", uploadResult.getUrl());
            data.put("publicId", uploadResult.getPublicId());

            return ResponseEntity.ok(new AdminBasicResponse<>(1, "Upload ảnh thành công", data));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(new AdminBasicResponse<>(0, "Failed to upload image: " + e.getMessage(), null));
        }
    }
}


