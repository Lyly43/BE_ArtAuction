package com.auctionaa.backend.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.exceptions.BadRequest;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryFolderService {

    private final Cloudinary cloudinary;

    /** Tạo folder nếu chưa có. Nếu đã tồn tại -> bỏ qua lỗi. */
    public void ensureFolder(String folderPath) throws Exception {
        try {
            cloudinary.api().createFolder(folderPath, ObjectUtils.emptyMap());
        } catch (BadRequest e) {
            // Cloudinary trả lỗi nếu folder đã tồn tại → bỏ qua
            if (!e.getMessage().contains("already exists")) {
                throw e;
            }
        }
    }

    public Map uploadToFolder(byte[] bytes, String folder, String publicId) throws Exception {
        return cloudinary.uploader().upload(
                bytes,
                ObjectUtils.asMap(
                        "folder", folder,
                        "public_id", publicId,
                        "resource_type", "image",
                        "overwrite", true
                )
        );
    }
}