package com.auctionaa.backend.Entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "artworks")
@CompoundIndexes({
        @CompoundIndex(name = "startedPrice_desc", def = "{ 'startedPrice': -1 }"),
        @CompoundIndex(name = "ownerId_asc",      def = "{ 'ownerId': 1 }")
})
public class Artwork extends BaseEntity {

    // Lưu thẳng ownerId để query nhanh
    private String ownerId;

    private String title;
    private String description;
    private String avtArtwork;
    private List<String> imageUrls;
    private int status; //    0: Chưa duyệt, 1: Đã duyệt, 2: Đang đấu giá, 3: Từ chối
    private boolean aiVerified;
    private BigDecimal startedPrice;

    private String paintingGenre;// The loai

    private int yearOfCreation; // chỉ lưu năm
    private String material;
    private String size;

    private String certificateId; // thay vì @DBRef

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Override
    public String getPrefix() {
        return "Aw-";
    }

}
