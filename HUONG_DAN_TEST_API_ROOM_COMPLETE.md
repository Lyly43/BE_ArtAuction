# HƯỚNG DẪN TEST API ROOM COMPLETE DETAIL - POSTMAN

## Tổng quan

API này lấy **TẤT CẢ** thông tin của một phòng đấu giá bao gồm:
- ✅ Thông tin phòng (AuctionRoom) - đầy đủ
- ✅ Tất cả sessions trong phòng (AuctionSession) - đầy đủ
- ✅ Thông tin tác phẩm (Artwork) của mỗi session - đầy đủ

---

## Thông tin API

**Endpoint**: `GET /api/auctionroom/complete/{id}`

**Method**: `GET`

**Base URL**: `http://localhost:8081` (hoặc port mà server đang chạy)

**Content-Type**: Không cần (GET request)

**Authentication**: Không cần (public endpoint)

---

## CÁCH TEST TRONG POSTMAN - TỪNG BƯỚC

### Bước 1: Chuẩn bị

1. ✅ Đảm bảo Spring Boot server đang chạy
2. ✅ Kiểm tra port server (thường là `8081` hoặc `8080`)
3. ✅ Mở Postman

### Bước 2: Tạo Request mới

1. Trong Postman, click nút **New** (góc trên bên trái)
2. Chọn **HTTP Request**
3. Hoặc click **+** để tạo tab mới

### Bước 3: Cấu hình Request

#### 3.1. Chọn Method

- Chọn method: **GET** (từ dropdown bên trái)

#### 3.2. Nhập URL

Nhập URL với format:
```
http://localhost:8081/api/auctionroom/complete/{roomId}
```

**Ví dụ**:
```
http://localhost:8081/api/auctionroom/complete/ACR-12345
```

**Lưu ý**:
- Thay `ACR-12345` bằng **ID thật** của phòng trong database
- Nếu server chạy port khác (ví dụ 8080), thay `8081` thành port đó

#### 3.3. Headers (Tùy chọn)

API này **KHÔNG CẦN** headers đặc biệt, nhưng bạn có thể thêm:

- **Content-Type**: `application/json` (tùy chọn)

**Cách thêm**:
1. Click tab **Headers**
2. Key: `Content-Type`
3. Value: `application/json`

#### 3.4. Body

**KHÔNG CẦN** body vì đây là GET request

---

## VÍ DỤ TEST CỤ THỂ

### Test Case 1: Lấy thông tin phòng có ID hợp lệ

**URL**:
```
GET http://localhost:8081/api/auctionroom/complete/ACR-abc123
```

**Các bước**:
1. Method: **GET**
2. URL: `http://localhost:8081/api/auctionroom/complete/ACR-abc123`
   - ⚠️ Thay `ACR-abc123` bằng ID thật trong database
3. Click **Send**

**Kết quả mong đợi**:

**Status**: `200 OK`

**Response Body** (JSON):
```json
{
  "room": {
    "id": "ACR-abc123",
    "adminId": "USR-xyz789",
    "memberIds": ["USR-001", "USR-002"],
    "viewCount": 150,
    "roomName": "Phòng đấu giá Modern Art",
    "description": "Mô tả phòng đấu giá",
    "imageAuctionRoom": "https://example.com/image.jpg",
    "type": "Modern",
    "status": 1,
    "depositAmount": 1000000,
    "paymentDeadlineDays": 7,
    "applicationFeePaidUserIds": [],
    "startedAt": "2024-01-15T10:00:00",
    "stoppedAt": null,
    "estimatedEndTime": "2024-01-15T18:00:00",
    "lowMemberWarningSent": false,
    "createdAt": "2024-01-10T08:00:00",
    "updatedAt": "2024-01-15T09:00:00"
  },
  "sessions": [
    {
      "session": {
        "id": "ATSS-001",
        "auctionRoomId": "ACR-abc123",
        "artworkId": "Aw-001",
        "imageUrl": "https://example.com/artwork1.jpg",
        "startTime": "2024-01-15T10:00:00",
        "endedAt": null,
        "startingPrice": 5000000,
        "currentPrice": 5500000,
        "status": 1,
        "winnerId": null,
        "type": "Modern",
        "viewCount": 50,
        "createdAt": "2024-01-10T08:00:00",
        "updatedAt": "2024-01-15T10:00:00",
        "bidStep": 100000,
        "durationSeconds": 3600,
        "durationMinutes": 60,
        "maxDurationSeconds": 7200,
        "extendStepSeconds": 300,
        "extendThresholdSeconds": 30,
        "finalPrice": null,
        "bidCount": 5,
        "sellerId": "USR-seller001",
        "orderIndex": 0
      },
      "artwork": {
        "id": "Aw-001",
        "ownerId": "USR-seller001",
        "title": "Water Lilies",
        "description": "Tác phẩm nổi tiếng của Monet",
        "avtArtwork": "https://example.com/artwork1.jpg",
        "imageUrls": [
          "https://example.com/artwork1.jpg",
          "https://example.com/artwork1-2.jpg"
        ],
        "status": 1,
        "aiVerified": true,
        "startedPrice": 5000000,
        "paintingGenre": "Impressionism",
        "yearOfCreation": 1919,
        "material": "Oil on canvas",
        "size": "100x80 cm",
        "certificateId": "CERT-001",
        "createdAt": "2024-01-05T10:00:00",
        "updatedAt": "2024-01-10T08:00:00"
      }
    },
    {
      "session": {
        "id": "ATSS-002",
        "auctionRoomId": "ACR-abc123",
        "artworkId": "Aw-002",
        "imageUrl": "https://example.com/artwork2.jpg",
        "startTime": null,
        "endedAt": null,
        "startingPrice": 3000000,
        "currentPrice": 3000000,
        "status": 0,
        "winnerId": null,
        "type": "Classic",
        "viewCount": 0,
        "createdAt": "2024-01-10T08:00:00",
        "updatedAt": "2024-01-10T08:00:00",
        "bidStep": 50000,
        "durationSeconds": 3600,
        "durationMinutes": 60,
        "maxDurationSeconds": 7200,
        "extendStepSeconds": 300,
        "extendThresholdSeconds": 30,
        "finalPrice": null,
        "bidCount": 0,
        "sellerId": "USR-seller002",
        "orderIndex": 1
      },
      "artwork": {
        "id": "Aw-002",
        "ownerId": "USR-seller002",
        "title": "The Starry Night",
        "description": "Tác phẩm nổi tiếng của Van Gogh",
        "avtArtwork": "https://example.com/artwork2.jpg",
        "imageUrls": [
          "https://example.com/artwork2.jpg"
        ],
        "status": 1,
        "aiVerified": true,
        "startedPrice": 3000000,
        "paintingGenre": "Post-Impressionism",
        "yearOfCreation": 1889,
        "material": "Oil on canvas",
        "size": "73.7x92.1 cm",
        "certificateId": "CERT-002",
        "createdAt": "2024-01-06T10:00:00",
        "updatedAt": "2024-01-10T08:00:00"
      }
    }
  ]
}
```

---

### Test Case 2: Phòng không tồn tại (ID sai)

**URL**:
```
GET http://localhost:8081/api/auctionroom/complete/ACR-INVALID
```

**Kết quả mong đợi**:

**Status**: `404 Not Found`

**Response Body**:
```json
{
  "timestamp": "2024-01-15T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Room not found",
  "path": "/api/auctionroom/complete/ACR-INVALID"
}
```

---

### Test Case 3: Phòng không có sessions

**URL**:
```
GET http://localhost:8081/api/auctionroom/complete/ACR-NO-SESSIONS
```

**Kết quả mong đợi**:

**Status**: `200 OK`

**Response Body**:
```json
{
  "room": {
    "id": "ACR-NO-SESSIONS",
    "roomName": "Phòng mới tạo",
    // ... các thông tin khác của phòng
  },
  "sessions": []
}
```

**Lưu ý**: `sessions` là mảng rỗng `[]` nếu phòng chưa có session nào

---

### Test Case 4: Session không có artwork (artworkId null hoặc artwork không tồn tại)

**Kết quả mong đợi**:

**Status**: `200 OK`

**Response Body**:
```json
{
  "room": {
    // ... thông tin phòng
  },
  "sessions": [
    {
      "session": {
        "id": "ATSS-003",
        "artworkId": "Aw-NOT-EXIST",
        // ... các thông tin khác của session
      },
      "artwork": null
    }
  ]
}
```

**Lưu ý**: `artwork` sẽ là `null` nếu:
- `artworkId` là `null` hoặc rỗng
- Artwork không tồn tại trong database

---

## CÁCH LẤY ID PHÒNG ĐỂ TEST

### Cách 1: Từ Database

1. Mở MongoDB Compass hoặc MongoDB shell
2. Kết nối database
3. Chọn collection `auction_rooms`
4. Xem danh sách phòng và copy một ID

### Cách 2: Từ API khác

Sử dụng API lấy danh sách phòng:

```
GET http://localhost:8081/api/auctionroom/all
```

Hoặc:

```
POST http://localhost:8081/api/auctionroom/allAuctionRoom
Body: {
  "page": 0,
  "size": 10
}
```

Sau đó copy một `id` từ response để test

---

## CẤU TRÚC RESPONSE CHI TIẾT

### 1. Room Object (AuctionRoom)

| Field | Type | Mô tả |
|-------|------|-------|
| `id` | String | ID phòng (format: ACR-xxx) |
| `adminId` | String | ID người quản lý phòng |
| `memberIds` | List<String> | Danh sách ID thành viên |
| `viewCount` | Integer | Số lượt xem |
| `roomName` | String | Tên phòng |
| `description` | String | Mô tả phòng |
| `imageAuctionRoom` | String | URL ảnh phòng |
| `type` | String | Thể loại phòng |
| `status` | Integer | Trạng thái (0: Sắp diễn ra, 1: Đang diễn ra, 2: Đã hoàn thành, 3: Hoãn) |
| `depositAmount` | BigDecimal | Số tiền đặt cọc |
| `paymentDeadlineDays` | Integer | Số ngày thanh toán sau khi thắng |
| `applicationFeePaidUserIds` | List<String> | Danh sách user đã trả phí hồ sơ |
| `startedAt` | LocalDateTime | Thời gian bắt đầu thực tế |
| `stoppedAt` | LocalDateTime | Thời gian kết thúc thực tế |
| `estimatedEndTime` | LocalDateTime | Thời gian kết thúc dự kiến |
| `lowMemberWarningSent` | Boolean | Cờ cảnh báo ít thành viên |
| `createdAt` | LocalDateTime | Ngày tạo |
| `updatedAt` | LocalDateTime | Ngày cập nhật |

### 2. Sessions Array

Mỗi phần tử trong `sessions` có cấu trúc:

```json
{
  "session": { /* AuctionSession object */ },
  "artwork": { /* Artwork object hoặc null */ }
}
```

#### 2.1. Session Object (AuctionSession)

| Field | Type | Mô tả |
|-------|------|-------|
| `id` | String | ID session (format: ATSS-xxx) |
| `auctionRoomId` | String | ID phòng |
| `artworkId` | String | ID tác phẩm |
| `imageUrl` | String | URL ảnh session |
| `startTime` | LocalDateTime | Thời gian bắt đầu |
| `endedAt` | LocalDateTime | Thời gian kết thúc |
| `startingPrice` | BigDecimal | Giá khởi điểm |
| `currentPrice` | BigDecimal | Giá hiện tại |
| `status` | Integer | Trạng thái (0: DRAFT, 1: SCHEDULED, 2: LIVE, 3: CLOSED, 4: CANCELLED) |
| `winnerId` | String | ID người thắng |
| `type` | String | Loại session |
| `viewCount` | Integer | Số lượt xem |
| `bidStep` | BigDecimal | Bước giá |
| `durationSeconds` | Integer | Thời lượng (giây) |
| `durationMinutes` | Integer | Thời lượng (phút) |
| `maxDurationSeconds` | Integer | Thời lượng tối đa (giây) |
| `extendStepSeconds` | Integer | Thời gian gia hạn mỗi lần (giây) |
| `extendThresholdSeconds` | Integer | Ngưỡng thời gian để gia hạn (giây) |
| `finalPrice` | BigDecimal | Giá cuối cùng |
| `bidCount` | Integer | Số lượt đấu giá |
| `sellerId` | String | ID người bán |
| `orderIndex` | Integer | Thứ tự trong phòng |
| `createdAt` | LocalDateTime | Ngày tạo |
| `updatedAt` | LocalDateTime | Ngày cập nhật |

#### 2.2. Artwork Object (Artwork)

| Field | Type | Mô tả |
|-------|------|-------|
| `id` | String | ID tác phẩm (format: Aw-xxx) |
| `ownerId` | String | ID chủ sở hữu |
| `title` | String | Tên tác phẩm |
| `description` | String | Mô tả |
| `avtArtwork` | String | URL ảnh đại diện |
| `imageUrls` | List<String> | Danh sách URL ảnh |
| `status` | Integer | Trạng thái (0: Chưa duyệt, 1: Đã duyệt, 2: Đang đấu giá, 3: Từ chối) |
| `aiVerified` | Boolean | Đã xác minh bằng AI |
| `startedPrice` | BigDecimal | Giá khởi điểm |
| `paintingGenre` | String | Thể loại tranh |
| `yearOfCreation` | Integer | Năm sáng tác |
| `material` | String | Chất liệu |
| `size` | String | Kích thước |
| `certificateId` | String | ID chứng chỉ |
| `createdAt` | LocalDateTime | Ngày tạo |
| `updatedAt` | LocalDateTime | Ngày cập nhật |

---

## TIPS VÀ THỦ THUẬT

### Tip 1: Sử dụng Variables trong Postman

1. Click vào collection name (hoặc tạo collection mới)
2. Vào tab **Variables**
3. Thêm variable:
   - **Name**: `base_url`
   - **Value**: `http://localhost:8081`
4. Trong request URL, dùng: `{{base_url}}/api/auctionroom/complete/ACR-12345`

### Tip 2: Lưu Request vào Collection

1. Click **Save** (góc trên bên phải)
2. Chọn collection hoặc tạo collection mới
3. Đặt tên request: "Get Room Complete Detail"
4. Click **Save**

### Tip 3: Test với nhiều ID khác nhau

1. Tạo nhiều request với các ID khác nhau
2. Hoặc dùng **Collection Runner** để test hàng loạt

### Tip 4: Format JSON Response

1. Sau khi nhận response, click vào tab **Pretty**
2. Chọn format **JSON**
3. Dễ đọc hơn nhiều!

---

## TROUBLESHOOTING (XỬ LÝ LỖI)

### Lỗi: "Could not get response"

**Nguyên nhân**: Server không chạy hoặc sai port

**Cách fix**:
1. Kiểm tra server có đang chạy không
2. Kiểm tra port (8081 hoặc 8080)
3. Thử truy cập `http://localhost:8081` trên browser

---

### Lỗi: "404 Not Found"

**Nguyên nhân**: 
- ID phòng không tồn tại
- URL sai

**Cách fix**:
1. Kiểm tra ID có đúng format không (ACR-xxx)
2. Kiểm tra ID có tồn tại trong database không
3. Kiểm tra URL path có đúng không

---

### Lỗi: "500 Internal Server Error"

**Nguyên nhân**: Lỗi server

**Cách fix**:
1. Kiểm tra server logs
2. Kiểm tra database connection
3. Kiểm tra dữ liệu có hợp lệ không

---

### Response trả về `sessions: []` (mảng rỗng)

**Nguyên nhân**: Phòng chưa có session nào

**Đây KHÔNG phải lỗi**, đây là kết quả hợp lệ nếu phòng chưa có session

---

### Response có `artwork: null`

**Nguyên nhân**: 
- Session không có `artworkId`
- Artwork không tồn tại trong database

**Đây KHÔNG phải lỗi**, API vẫn trả về session nhưng `artwork` sẽ là `null`

---

## SO SÁNH VỚI API CŨ

### API Cũ: `GET /api/auctionroom/room/{id}`

**Trả về**:
- Room information
- Sessions (chỉ có session, không có artwork)

### API Mới: `GET /api/auctionroom/complete/{id}`

**Trả về**:
- Room information (giống API cũ)
- Sessions (giống API cũ)
- **+ Artwork information cho mỗi session** (MỚI)

**Khi nào dùng API nào?**
- Dùng API cũ nếu chỉ cần room + sessions
- Dùng API mới nếu cần **TẤT CẢ** thông tin bao gồm artwork

---

## CHECKLIST TEST

- [ ] Test với ID phòng hợp lệ
- [ ] Test với ID phòng không tồn tại (404)
- [ ] Test với phòng không có sessions (sessions: [])
- [ ] Test với session không có artwork (artwork: null)
- [ ] Kiểm tra tất cả fields trong response
- [ ] Kiểm tra format JSON đúng
- [ ] Test với nhiều ID khác nhau

---

## HOÀN THÀNH!

Nếu bạn đã test hết các case trên, bạn đã hoàn thành việc test API!

**Chúc bạn test thành công!** 🎉

---

## LIÊN KẾT HỮU ÍCH

- API Documentation: Xem file `Hướng dẫn API.md`
- Search API: Xem file `PostMan_API_SEARCH.md`
- Test Search API: Xem file `Cach_Test_Postman_Search_hehe.md`

