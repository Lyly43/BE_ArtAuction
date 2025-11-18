package com.auctionaa.backend.DTO.Request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class SellerRequestDTO {
    private MultipartFile verificationImage; // Ảnh chứng thực
    private String description; // Mô tả
}
