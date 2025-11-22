# Hướng dẫn API Admin

Tài liệu này tổng hợp toàn bộ API phục vụ trang quản trị. Các nhóm frontend có thể dựa vào đây để tích hợp.

## 1. Quy ước chung

- **Base URL:** `http://{host}:8085`
- **Prefix:** Tất cả API quản trị dùng tiền tố `/api/admin`.
- **Xác thực:** 
  - Tất cả API admin (trừ `/api/admin/auth/login`) **BẮT BUỘC** phải có header `Authorization: Bearer {adminToken}`.
  - Token phải là admin token (được tạo từ API login admin).
  - Nếu không có token hoặc token không hợp lệ, hệ thống sẽ trả về lỗi 401 Unauthorized.
  - Chỉ admin đã đăng nhập mới có thể truy cập các API quản trị.
- **Trạng thái:** Các response JSON dùng trường `status` với giá trị `1` (thành công) hoặc `0` (thất bại). `message` mô tả ngắn gọn kết quả.

---

## 2. Xác thực Admin

- **Đăng nhập**
  - Method & URL: `POST /api/admin/auth/login`
  - Request:
    ```json
    {
      "email": "admin@example.com",
      "password": "123456"
    }
    ```
  - Response:
    ```json
    {
      "status": 1,
      "message": "Admin login successfully",
      "token": "eyJhbGciOiJI..."
    }
    ```

- **Kiểm tra token / lấy thông tin**
  - Method & URL: `GET /api/admin/auth/me`
  - Header: `Authorization: Bearer {token}`
  - Response:
    ```json
    {
      "status": 1,
      "message": "Token is valid",
      "name": "Admin Root",
      "email": "admin@example.com",
      "avatar": "https://cdn.example.com/avatar.png"
    }
    ```

---

## 3. Quản lý Admin

### Thêm admin
- Method & URL: `POST /api/admin/admins/them-admin`
- Request:
  ```json
  {
    "fullName": "Nguyễn Văn A",
    "email": "admin2@example.com",
    "password": "Abc@1234",
    "phoneNumber": "0912345678",
    "address": "Hà Nội, Việt Nam",
    "status": 1
  }
  ```
- Response:
  ```json
  {
    "status": 1,
    "message": "Admin created successfully",
    "data": {
      "id": "Ad-2",
      "fullName": "Nguyễn Văn A",
      "email": "admin2@example.com",
      "phoneNumber": "0912345678",
      "address": "Hà Nội, Việt Nam",
      "avatar": null,
      "role": "4",
      "status": 1,
      "createdAt": "2025-11-20T10:00:00",
      "updatedAt": "2025-11-20T10:00:00"
    }
  }
  ```
- Lưu ý: Password sẽ được tự động hash bằng BCrypt. Status: 0 = Bị Khóa, 1 = Hoạt động.

### Lấy danh sách admin
- Method & URL: `GET /api/admin/admins/lay-du-lieu`
- Response: Mảng `AdminAdminResponse` gồm `id, fullName, email, phoneNumber, address, avatar, role, status, createdAt, updatedAt`.

### Tìm kiếm admin
- Method & URL: `GET /api/admin/admins/tim-kiem?q={searchTerm}`
- Tìm kiếm theo: ID, fullName, email, phoneNumber
- Nếu không có `q`, trả về tất cả admin
- Response: Mảng `AdminAdminResponse`

### Thống kê admin
- Method & URL: `GET /api/admin/admins/thong-ke`
- Response:
  ```json
  {
    "totalAdmins": 10,
    "activeAdmins": 8,
    "lockedAdmins": 2
  }
  ```

### Cập nhật admin
- Method & URL: `PUT /api/admin/admins/cap-nhat/{adminId}`
- Request (chỉ gửi các trường cần đổi):
  ```json
  {
    "fullName": "Nguyễn Văn A Updated",
    "email": "admin2_new@example.com",
    "phoneNumber": "0987654321",
    "address": "TP. Hồ Chí Minh, Việt Nam",
    "status": 1,
    "password": "NewPassword@123"
  }
  ```
- Lưu ý: `password` là optional, chỉ cập nhật nếu có giá trị
- Response:
  ```json
  {
    "status": 1,
    "message": "Admin updated successfully",
    "data": { ...AdminAdminResponse }
  }
  ```

### Xóa admin
- Method & URL: `DELETE /api/admin/admins/xoa/{adminId}`
- Response:
  ```json
  {
    "status": 1,
    "message": "Admin deleted successfully",
    "data": null
  }
  ```

---

## 4. Quản lý Người dùng

### Thêm người dùng
- Method & URL: `POST /api/admin/them-user`
- Request:
  ```json
  {
    "username": "user01",
    "email": "user01@example.com",
    "password": "Abc@1234",
    "phonenumber": "0912345678",
    "cccd": "012345678901",
    "address": "Hà Nội",
    "dateOfBirth": "1998-10-01",
    "gender": "MALE",
    "role": 3,
    "status": 1
  }
  ```
- Response: chuỗi `"User created successfully with ID: U-..."`.

### Lấy danh sách
- `GET /api/admin/lay-du-lieu-user`
- Response: mảng `AdminUserResponse` gồm `id, fullname, email, phonenumber, gender, dateOfBirth, address, cccd, role, status, balance, createdAt`.

### Tìm kiếm
- `GET /api/admin/tim-kiem-user?q={term}`

### Thống kê
- `GET /api/admin/thong-ke-user`
- Response: `{ "totalUsers", "totalSellers", "totalBlockedUsers" }`.

### Cập nhật
- Method & URL: `PUT /api/admin/cap-nhat-user/{userId}`
- Request (chỉ gửi các trường cần đổi):
  ```json
  {
    "email": "user01_new@example.com",
    "phonenumber": "0987654321",
    "role": 2,
    "status": 2
  }
  ```
- Response:
  ```json
  {
    "status": 1,
    "message": "User updated successfully",
    "data": { ...AdminUserResponse }
  }
  ```

### Xóa
- `DELETE /api/admin/xoa-user/{userId}`
- Response: chuỗi xác nhận.

---

## 5. Quản lý Tác phẩm

- **Thêm tác phẩm**
  - `POST /api/admin/artworks/them-tac-pham`
  - Request:
    ```json
    {
      "ownerId": "U-100",
      "title": "Sunset Over Da Nang",
      "description": "Oil on canvas",
      "size": "80x120",
      "material": "Oil",
      "paintingGenre": ["Landscape"],
      "startedPrice": 1500,
      "avtArtwork": "https://cdn/artwork.jpg",
      "imageUrls": ["https://cdn/a1.jpg", "https://cdn/a2.jpg"],
      "yearOfCreation": 2022
    }
    ```
  - Response: `"Artwork created successfully with ID: A-..."`.

- **Lấy danh sách:** `GET /api/admin/artworks/lay-du-lieu-tac-pham`
- **Tìm kiếm:** `GET /api/admin/artworks/tim-kiem-tac-pham?q={term}`
- **Lọc để chọn cho phòng:** `GET /api/admin/artworks/chon-tac-pham?paintingGenre=...&material=...&q=...`
- **Cập nhật**
  - `PUT /api/admin/artworks/cap-nhat-tac-pham/{artworkId}`
  - Request mẫu:
    ```json
    {
      "title": "Sunset Over Hoi An",
      "status": 1,
      "paintingGenre": ["Landscape", "Modern"],
      "startedPrice": 2000
    }
    ```
  - Response `UpdateResponse`.
- **Xóa:** `DELETE /api/admin/artworks/xoa-tac-pham/{artworkId}`
- **Thống kê:** `GET /api/admin/artworks/thong-ke-tac-pham`

---

## 6. Quản lý Phòng đấu giá

- **Tạo nhanh:** `POST /api/admin/auction-rooms/them-phong` (body `AddAuctionRoomRequest` – `roomName`, `description`, `material`, `startedAt`, `stoppedAt`, `adminId`, `type`, `imageAuctionRoom`…).
- **Lấy danh sách:** `GET /api/admin/auction-rooms/lay-du-lieu` → `AdminAuctionRoomResponse` (kèm giá bắt đầu & hiện tại).
- **Tìm kiếm:** `GET /api/admin/auction-rooms/tim-kiem?q={keyword}`.
- **Cập nhật:** `PUT /api/admin/auction-rooms/cap-nhat/{roomId}` → `UpdateResponse`.
- **Xóa:** `DELETE /api/admin/auction-rooms/xoa/{roomId}`.
- **Thống kê:** `GET /api/admin/auction-rooms/thong-ke` → `{ totalRooms, runningRooms, upcomingRooms, completedRooms }`.
- **Tạo phòng hoàn chỉnh (4 bước trong 1 API):** `POST /api/admin/auction-rooms/tao-phong-hoan-chinh`
  ```json
  {
    "roomName": "...",
    "description": "...",
    "material": "Oil",
    "type": "VIP",
    "startedAt": "2025-12-01T10:00:00",
    "stoppedAt": "2025-12-01T12:00:00",
    "adminId": "Ad-1",
    "depositAmount": 5000,
    "paymentDeadlineDays": 3,
    "artworks": [
      { "artworkId": "A-1", "startingPrice": 1000, "bidStep": 50 }
    ]
  }
  ```
  - Response: `{ "status": 1, "message": "Auction room created successfully", "roomId": "...", "sessionsCreated": 3 }`.

---

## 7. Quản lý Thông báo

- **Lấy dữ liệu:** `GET /api/admin/notifications/lay-du-lieu`
- **Tìm kiếm:** `GET /api/admin/notifications/tim-kiem?q=...`
- **Tạo thông báo**
  - `POST /api/admin/notifications/tao-thong-bao`
  - Request:
    ```json
    {
      "userId": "U-10",
      "notificationType": 1,
      "title": "Phiên đấu giá sắp bắt đầu",
      "link": "/auction-room/AR-01",
      "notificationContent": "Phòng VIP 01 sẽ mở lúc 10:00",
      "notificationStatus": 1,
      "notificationTime": "2025-03-25T09:00:00",
      "refId": "AR-01"
    }
    ```
  - Response: `{ "status": 1, "message": "...", "data": { ...AdminNotificationResponse } }`
- **Cập nhật**
  - `PUT /api/admin/notifications/cap-nhat/{notificationId}`
  - Request tương tự POST (chỉ gửi trường cần đổi)
  - Response: `{ "status": 1, "message": "...", "data": {...} }`
- **Xóa:** `DELETE /api/admin/notifications/xoa/{notificationId}`
- **Thống kê:** `GET /api/admin/notifications/thong-ke`

---

## 8. Quản lý Hóa đơn

- `GET /api/admin/invoices/lay-du-lieu`
- `GET /api/admin/invoices/tim-kiem?q=...`
- `GET /api/admin/invoices/thong-ke`
- **Cập nhật hóa đơn**
  - `PUT /api/admin/invoices/cap-nhat/{invoiceId}`
  - Request mẫu:
    ```json
    {
      "buyerPremium": 150,
      "insuranceFee": 50,
      "salesTax": 100,
      "shippingFee": 120,
      "totalAmount": 2370,
      "paymentMethod": "BANK_TRANSFER",
      "paymentStatus": 1,
      "invoiceStatus": 2,
      "note": "Đã thanh toán đủ"
    }
    ```
  - Response: `{ "status": 1, "message": "Cập nhật hóa đơn thành công", "data": {...} }`
- **Xóa hóa đơn:** `DELETE /api/admin/invoices/xoa/{invoiceId}`

---

## 9. Quản lý Report

- `GET /api/admin/reports/lay-du-lieu` – trả `AdminReportResponse` (bao gồm thông tin người báo cáo, đối tượng bị báo cáo, reportReason, status, thời gian).
- `GET /api/admin/reports/tim-kiem?q=...`
- `GET /api/admin/reports/thong-ke`
- `PUT /api/admin/reports/cap-nhat/{reportId}` – body `UpdateReportRequest` (reportReason, reportStatus, reportDoneTime).
- `DELETE /api/admin/reports/xoa/{reportId}`
- Response chuẩn `AdminReportApiResponse`.

---

## 10. Thống kê Hệ thống (Biểu đồ)

Tất cả API thống kê sử dụng phương thức POST và nhận request body với `begin` và `end` (format: `dd/MM/yyyy`).

**Lưu ý quan trọng:** API sẽ trả về tất cả các ngày trong khoảng từ `begin` đến `end`, kể cả những ngày không có dữ liệu (sẽ có giá trị 0).

### Thống kê người dùng đăng ký
- Method & URL: `POST /api/admin/statistics/users-registration`
- Request Body:
  ```json
  {
    "begin": "02/11/2025",
    "end": "17/11/2025"
  }
  ```
- Response:
  ```json
  {
    "status": 1,
    "message": "Success",
    "data": [
      { "date": "02/11/2025", "count": 10 },
      { "date": "03/11/2025", "count": 0 },
      { "date": "04/11/2025", "count": 15 },
      ...
      { "date": "17/11/2025", "count": 5 }
    ],
    "labels": ["02/11/2025", "03/11/2025", "04/11/2025", ..., "17/11/2025"],
    "datasets": [{
      "label": "Thống kê người dùng đăng ký",
      "data": [10, 0, 15, ..., 5],
      "backgroundColor": ["#A1B2C3", "#D4E5F6", ...]
    }]
  }
  ```

### Thống kê phòng đấu giá
- Method & URL: `POST /api/admin/statistics/auction-rooms`
- Request Body:
  ```json
  {
    "begin": "02/11/2025",
    "end": "17/11/2025"
  }
  ```
- Response: Tương tự format trên, với `label: "Thống kê phòng đấu giá"`. Trả về tất cả các ngày trong khoảng.

### Thống kê doanh thu
- Method & URL: `POST /api/admin/statistics/revenue`
- Request Body:
  ```json
  {
    "begin": "02/11/2025",
    "end": "17/11/2025"
  }
  ```
- Response:
  ```json
  {
    "status": 1,
    "message": "Success",
    "data": [
      { "date": "02/11/2025", "totalAmount": 5000000.0 },
      { "date": "03/11/2025", "totalAmount": 0.0 },
      { "date": "04/11/2025", "totalAmount": 7500000.0 },
      ...
      { "date": "17/11/2025", "totalAmount": 3000000.0 }
    ],
    "labels": ["02/11/2025", "03/11/2025", "04/11/2025", ..., "17/11/2025"],
    "datasets": [{
      "label": "Thống kê doanh thu",
      "data": [5000000.0, 0.0, 7500000.0, ..., 3000000.0],
      "backgroundColor": ["#A1B2C3", "#D4E5F6", ...]
    }]
  }
  ```
- Lưu ý: `data` trong response chứa `totalAmount` (Double) thay vì `count` (Long). Trả về tất cả các ngày trong khoảng, ngày không có doanh thu sẽ có `totalAmount: 0.0`.

### Thống kê báo cáo
- Method & URL: `POST /api/admin/statistics/reports`
- Request Body:
  ```json
  {
    "begin": "02/11/2025",
    "end": "17/11/2025"
  }
  ```
- Response:
  ```json
  {
    "status": 1,
    "message": "Success",
    "data": [
      { "date": "02/11/2025", "count": 5 },
      { "date": "03/11/2025", "count": 0 },
      { "date": "04/11/2025", "count": 8 },
      ...
      { "date": "17/11/2025", "count": 3 }
    ],
    "labels": ["02/11/2025", "03/11/2025", "04/11/2025", ..., "17/11/2025"],
    "datasets": [{
      "label": "Thống kê báo cáo",
      "data": [5, 0, 8, ..., 3],
      "backgroundColor": ["#A1B2C3", "#D4E5F6", ...]
    }]
  }
  ```
- Lưu ý: Trả về tất cả các ngày trong khoảng, ngày không có báo cáo sẽ có `count: 0`.

### Thống kê tác phẩm
- Method & URL: `POST /api/admin/statistics/artworks`
- Request Body:
  ```json
  {
    "begin": "02/11/2025",
    "end": "17/11/2025"
  }
  ```
- Response: Tương tự format trên, với `label: "Thống kê tác phẩm"`. Trả về tất cả các ngày trong khoảng.

### Thống kê đấu giá
- Method & URL: `POST /api/admin/statistics/bids`
- Request Body:
  ```json
  {
    "begin": "02/11/2025",
    "end": "17/11/2025"
  }
  ```
- Response:
  ```json
  {
    "status": 1,
    "message": "Success",
    "data": [
      { "date": "02/11/2025", "count": 25 },
      { "date": "03/11/2025", "count": 0 },
      { "date": "04/11/2025", "count": 42 },
      ...
      { "date": "17/11/2025", "count": 18 }
    ],
    "labels": ["02/11/2025", "03/11/2025", "04/11/2025", ..., "17/11/2025"],
    "datasets": [{
      "label": "Thống kê đấu giá",
      "data": [25, 0, 42, ..., 18],
      "backgroundColor": ["#A1B2C3", "#D4E5F6", ...]
    }]
  }
  ```
- Lưu ý: Trả về tất cả các ngày trong khoảng, ngày không có đấu giá sẽ có `count: 0`.

---

## 11. Ghi chú cho Frontend

1. **Header mặc định**
   ```
   Authorization: Bearer {adminToken}
   Content-Type: application/json
   ```
   - **Lưu ý quan trọng:** Token phải là admin token (lấy từ `/api/admin/auth/login`).
   - Tất cả API admin (trừ login) đều yêu cầu header này.
   - Nếu thiếu hoặc token không hợp lệ, server sẽ trả về 401 Unauthorized.

2. **Xử lý lỗi authentication:**
   - Khi nhận 401 Unauthorized, frontend nên redirect về trang login.
   - Token có thể hết hạn, cần kiểm tra và refresh token nếu cần.

3. **Status field:** luôn dùng `status` 1/0 để hiển thị toast thành công/thất bại.

4. **Datetime:** API dùng `ISO 8601` (ví dụ `2025-11-21T10:00:00`). Frontend cần chuyển timezone nếu hiển thị theo giờ địa phương.

5. **List pagination:** hiện tại tất cả API trả toàn bộ dữ liệu. Nếu cần phân trang bổ sung query `page`, `size` ở phiên bản sau.

---

Nếu cần thêm ví dụ cụ thể cho từng màn hình, hãy liên hệ backend để cập nhật file này.

