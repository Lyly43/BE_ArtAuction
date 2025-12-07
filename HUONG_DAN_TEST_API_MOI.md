# 📚 HƯỚNG DẪN TEST API MỚI BẰNG POSTMAN

Tài liệu này hướng dẫn cách test các API vừa được phát triển bằng Postman.

**Base URL**: `http://localhost:8081`

---

## 📋 MỤC LỤC

1. [API Lấy Danh Sách Tất Cả Yêu Cầu Seller](#1-api-lấy-danh-sách-tất-cả-yêu-cầu-seller)
2. [API Lấy Phòng Đấu Giá Đang Diễn Ra](#2-api-lấy-phòng-đấu-giá-đang-diễn-ra)
3. [API Lấy Phòng Đấu Giá Sắp Diễn Ra](#3-api-lấy-phòng-đấu-giá-sắp-diễn-ra)
4. [Troubleshooting](#troubleshooting)

---

## 1. API Lấy Danh Sách Tất Cả Yêu Cầu Seller

### 📌 Thông tin API

- **Endpoint**: `GET /api/all-seller-requests`
- **Method**: `GET`
- **Authentication**: Không cần (hoặc tùy theo cấu hình security)
- **Mô tả**: Lấy danh sách tất cả yêu cầu của seller bao gồm tên user, avatar, trạng thái, ngày gửi request, mô tả

### 🔧 Cách test trong Postman

#### Bước 1: Tạo Request mới

1. Mở Postman
2. Click **New** → **HTTP Request**
3. Đặt tên request: `Get All Seller Requests`

#### Bước 2: Cấu hình Request

1. **Method**: Chọn `GET`
2. **URL**: Nhập `http://localhost:8081/api/all-seller-requests`
3. **Headers**: 
   - `Content-Type`: `application/json` (nếu cần)
   - Nếu API yêu cầu authentication, thêm:
     - `Authorization`: `Bearer <your-token>`

#### Bước 3: Gửi Request

1. Click nút **Send**
2. Xem kết quả trong phần **Response**

### 📤 Response mẫu

```json
[
  {
    "requestId": "SR-12345",
    "userId": "USR-67890",
    "userName": "Nguyễn Văn A",
    "userAvatar": "https://cloudinary.com/avatar.jpg",
    "verificationImageUrl": "https://cloudinary.com/verification.jpg",
    "description": "Tôi muốn trở thành seller để bán các tác phẩm nghệ thuật",
    "status": "PENDING",
    "adminNote": null,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  },
  {
    "requestId": "SR-12346",
    "userId": "USR-67891",
    "userName": "Trần Thị B",
    "userAvatar": "https://cloudinary.com/avatar2.jpg",
    "verificationImageUrl": "https://cloudinary.com/verification2.jpg",
    "description": "Tôi có nhiều kinh nghiệm trong lĩnh vực nghệ thuật",
    "status": "APPROVED",
    "adminNote": "Đã được duyệt bởi admin",
    "createdAt": "2024-01-14T09:20:00",
    "updatedAt": "2024-01-14T15:45:00"
  }
]
```

### ✅ Kiểm tra Response

- **Status Code**: `200 OK`
- **Response Body**: Mảng các object `SellerRequestWithUserResponse`
- Mỗi object chứa:
  - `requestId`: ID của request
  - `userId`: ID của user
  - `userName`: Tên user
  - `userAvatar`: URL avatar của user
  - `status`: Trạng thái (PENDING, APPROVED, REJECTED)
  - `createdAt`: Ngày tạo request
  - `description`: Mô tả của user khi gửi request

---

## 2. API Lấy Phòng Đấu Giá Đang Diễn Ra

### 📌 Thông tin API

- **Endpoint**: `POST /api/auctionroom/ongoing`
- **Method**: `POST`
- **Authentication**: Không cần
- **Mô tả**: Lấy danh sách phòng đấu giá đang diễn ra (status = 1) với phân trang

### 🔧 Cách test trong Postman

#### Bước 1: Tạo Request mới

1. Click **New** → **HTTP Request**
2. Đặt tên request: `Get Ongoing Auction Rooms`

#### Bước 2: Cấu hình Request

1. **Method**: Chọn `POST`
2. **URL**: Nhập `http://localhost:8081/api/auctionroom/ongoing`
3. **Headers**: 
   - `Content-Type`: `application/json`
4. **Body**: 
   - Chọn tab **Body**
   - Chọn **raw**
   - Chọn **JSON** từ dropdown
   - Nhập JSON:

```json
{
  "page": 0,
  "size": 10
}
```

**Giải thích tham số:**
- `page`: Số trang (bắt đầu từ 0)
- `size`: Số lượng item mỗi trang

#### Bước 3: Gửi Request

1. Click nút **Send**
2. Xem kết quả trong phần **Response**

### 📤 Response mẫu

```json
[
  {
    "id": "ACR-12345",
    "adminId": "USR-67890",
    "memberIds": ["USR-67890", "USR-67891"],
    "viewCount": 150,
    "roomName": "Phòng Đấu Giá Nghệ Thuật Hiện Đại",
    "description": "Phòng đấu giá các tác phẩm nghệ thuật hiện đại",
    "imageAuctionRoom": "https://cloudinary.com/room.jpg",
    "type": "Modern",
    "status": 1,
    "depositAmount": 1000000.00,
    "paymentDeadlineDays": 7,
    "applicationFeePaidUserIds": [],
    "startedAt": "2024-01-15T10:00:00",
    "stoppedAt": null,
    "createdAt": "2024-01-10T08:00:00",
    "updatedAt": "2024-01-15T10:00:00"
  },
  {
    "id": "ACR-12346",
    "adminId": "USR-67892",
    "memberIds": ["USR-67892", "USR-67893", "USR-67894"],
    "viewCount": 200,
    "roomName": "Phòng Đấu Giá Tranh Cổ Điển",
    "description": "Phòng đấu giá các tác phẩm nghệ thuật cổ điển",
    "imageAuctionRoom": "https://cloudinary.com/room2.jpg",
    "type": "Classic",
    "status": 1,
    "depositAmount": 2000000.00,
    "paymentDeadlineDays": 7,
    "applicationFeePaidUserIds": [],
    "startedAt": "2024-01-14T09:00:00",
    "stoppedAt": null,
    "createdAt": "2024-01-08T07:00:00",
    "updatedAt": "2024-01-14T09:00:00"
  }
]
```

### ✅ Kiểm tra Response

- **Status Code**: `200 OK`
- **Response Body**: Mảng các object `AuctionRoom`
- Tất cả rooms có `status = 1` (đang diễn ra)
- Response được phân trang theo `page` và `size`

### 🧪 Test với các giá trị khác

**Test trang 2:**
```json
{
  "page": 1,
  "size": 10
}
```

**Test với size lớn hơn:**
```json
{
  "page": 0,
  "size": 20
}
```

---

## 3. API Lấy Phòng Đấu Giá Sắp Diễn Ra

### 📌 Thông tin API

- **Endpoint**: `POST /api/auctionroom/upcoming`
- **Method**: `POST`
- **Authentication**: Không cần
- **Mô tả**: Lấy danh sách phòng đấu giá sắp diễn ra (status = 2) với phân trang

### 🔧 Cách test trong Postman

#### Bước 1: Tạo Request mới

1. Click **New** → **HTTP Request**
2. Đặt tên request: `Get Upcoming Auction Rooms`

#### Bước 2: Cấu hình Request

1. **Method**: Chọn `POST`
2. **URL**: Nhập `http://localhost:8081/api/auctionroom/upcoming`
3. **Headers**: 
   - `Content-Type`: `application/json`
4. **Body**: 
   - Chọn tab **Body**
   - Chọn **raw**
   - Chọn **JSON** từ dropdown
   - Nhập JSON:

```json
{
  "page": 0,
  "size": 10
}
```

#### Bước 3: Gửi Request

1. Click nút **Send**
2. Xem kết quả trong phần **Response**

### 📤 Response mẫu

```json
[
  {
    "id": "ACR-12347",
    "adminId": "USR-67895",
    "memberIds": ["USR-67895"],
    "viewCount": 50,
    "roomName": "Phòng Đấu Giá Nghệ Thuật Đương Đại",
    "description": "Phòng đấu giá các tác phẩm nghệ thuật đương đại",
    "imageAuctionRoom": "https://cloudinary.com/room3.jpg",
    "type": "Contemporary",
    "status": 2,
    "depositAmount": 500000.00,
    "paymentDeadlineDays": 7,
    "applicationFeePaidUserIds": [],
    "startedAt": null,
    "stoppedAt": null,
    "createdAt": "2024-01-12T10:00:00",
    "updatedAt": "2024-01-12T10:00:00"
  },
  {
    "id": "ACR-12348",
    "adminId": "USR-67896",
    "memberIds": ["USR-67896", "USR-67897"],
    "viewCount": 30,
    "roomName": "Phòng Đấu Giá Điêu Khắc",
    "description": "Phòng đấu giá các tác phẩm điêu khắc",
    "imageAuctionRoom": "https://cloudinary.com/room4.jpg",
    "type": "Sculpture",
    "status": 2,
    "depositAmount": 800000.00,
    "paymentDeadlineDays": 7,
    "applicationFeePaidUserIds": [],
    "startedAt": null,
    "stoppedAt": null,
    "createdAt": "2024-01-11T09:00:00",
    "updatedAt": "2024-01-11T09:00:00"
  }
]
```

### ✅ Kiểm tra Response

- **Status Code**: `200 OK`
- **Response Body**: Mảng các object `AuctionRoom`
- Tất cả rooms có `status = 2` (sắp diễn ra)
- Response được phân trang theo `page` và `size`

### 🧪 Test với các giá trị khác

**Test trang đầu tiên với 5 items:**
```json
{
  "page": 0,
  "size": 5
}
```

**Test trang cuối (nếu có nhiều dữ liệu):**
```json
{
  "page": 2,
  "size": 10
}
```

---

## 📊 So sánh 3 API

| API | Method | Endpoint | Phân trang | Filter |
|-----|--------|----------|------------|--------|
| Get All Seller Requests | GET | `/api/all-seller-requests` | ❌ | ❌ |
| Get Ongoing Rooms | POST | `/api/auctionroom/ongoing` | ✅ | Status = 1 |
| Get Upcoming Rooms | POST | `/api/auctionroom/upcoming` | ✅ | Status = 2 |

---

## 🔍 Tips và Best Practices

### 1. Sử dụng Variables trong Postman

1. Click vào collection name
2. Vào tab **Variables**
3. Thêm variable:
   - Name: `base_url`
   - Value: `http://localhost:8081`
4. Trong request, dùng `{{base_url}}` thay vì gõ lại URL

**Ví dụ**: `{{base_url}}/api/all-seller-requests`

### 2. Tạo Collection cho các API mới

1. Click **New** → **Collection**
2. Đặt tên: `New APIs - Test`
3. Kéo thả các request vào collection
4. Có thể chạy tất cả requests cùng lúc bằng **Collection Runner**

### 3. Lưu Response để so sánh

1. Sau khi nhận response, click **Save Response**
2. Chọn **Save as Example**
3. Có thể xem lại sau để so sánh

### 4. Test với dữ liệu thật

1. Trước khi test, kiểm tra database có dữ liệu không
2. Đảm bảo có ít nhất một số records với status phù hợp
3. Test với các giá trị page và size khác nhau

---

## 🐛 Troubleshooting

### Lỗi: "Could not get response"

**Nguyên nhân**: Server không chạy hoặc sai port

**Cách fix**:
1. Kiểm tra server có đang chạy không
2. Kiểm tra port 8081 có đúng không
3. Thử truy cập `http://localhost:8081` trên browser
4. Kiểm tra console logs của Spring Boot

---

### Lỗi: "404 Not Found"

**Nguyên nhân**: URL sai hoặc endpoint không tồn tại

**Cách fix**:
1. Kiểm tra URL path có đúng không:
   - `/api/all-seller-requests` (không có dấu `/` ở cuối)
   - `/api/auctionroom/ongoing` (không có dấu `/` ở cuối)
   - `/api/auctionroom/upcoming` (không có dấu `/` ở cuối)
2. Kiểm tra server logs xem có error không
3. Đảm bảo endpoint đã được deploy và compile thành công

---

### Lỗi: "400 Bad Request" (cho API phân trang)

**Nguyên nhân**: Request body sai format hoặc thiếu thông tin

**Cách fix**:
1. Đảm bảo Content-Type là `application/json`
2. Kiểm tra JSON format có đúng không:
   ```json
   {
     "page": 0,
     "size": 10
   }
   ```
3. Đảm bảo `page` và `size` là số nguyên (integer)
4. `page` phải >= 0
5. `size` phải > 0

---

### Response trả về mảng rỗng `[]`

**Nguyên nhân**: Không có dữ liệu phù hợp trong database

**Cách fix**:
1. Kiểm tra database có dữ liệu không
2. Đối với API Ongoing: Kiểm tra có room nào có `status = 1` không
3. Đối với API Upcoming: Kiểm tra có room nào có `status = 2` không
4. Thử tăng `size` hoặc kiểm tra `page` có quá lớn không

---

### Lỗi: "500 Internal Server Error"

**Nguyên nhân**: Lỗi server-side

**Cách fix**:
1. Kiểm tra server logs để xem chi tiết lỗi
2. Kiểm tra database connection
3. Kiểm tra các dependencies có đầy đủ không
4. Đảm bảo các repository methods đã được implement đúng

---

### Lỗi: "401 Unauthorized" hoặc "403 Forbidden"

**Nguyên nhân**: API yêu cầu authentication nhưng chưa có token

**Cách fix**:
1. Kiểm tra API có yêu cầu authentication không
2. Nếu có, thêm header:
   ```
   Authorization: Bearer <your-jwt-token>
   ```
3. Lấy token từ API login trước

---

## 📝 Checklist Test

Trước khi hoàn thành test, đảm bảo đã test:

### API Get All Seller Requests
- [ ] Request thành công với status 200
- [ ] Response trả về mảng các seller requests
- [ ] Mỗi item có đầy đủ: requestId, userId, userName, userAvatar, status, createdAt, description
- [ ] Test với database có dữ liệu
- [ ] Test với database rỗng (trả về `[]`)

### API Get Ongoing Rooms
- [ ] Request thành công với status 200
- [ ] Response trả về mảng các rooms
- [ ] Tất cả rooms có `status = 1`
- [ ] Phân trang hoạt động đúng (test với page = 0, 1, 2)
- [ ] Test với size khác nhau (5, 10, 20)
- [ ] Test với page quá lớn (trả về `[]`)

### API Get Upcoming Rooms
- [ ] Request thành công với status 200
- [ ] Response trả về mảng các rooms
- [ ] Tất cả rooms có `status = 2`
- [ ] Phân trang hoạt động đúng (test với page = 0, 1, 2)
- [ ] Test với size khác nhau (5, 10, 20)
- [ ] Test với page quá lớn (trả về `[]`)

---

## 🎯 Kết luận

Sau khi test xong, bạn sẽ có thể:
- ✅ Lấy danh sách tất cả yêu cầu seller với đầy đủ thông tin user
- ✅ Lấy danh sách phòng đấu giá đang diễn ra với phân trang
- ✅ Lấy danh sách phòng đấu giá sắp diễn ra với phân trang

Nếu gặp vấn đề, hãy kiểm tra phần Troubleshooting hoặc xem server logs để tìm nguyên nhân.

---

**Chúc bạn test thành công! 🚀**

