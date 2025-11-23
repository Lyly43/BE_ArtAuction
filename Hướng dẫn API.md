# Hướng dẫn API Admin

Tài liệu này tổng hợp toàn bộ API phục vụ trang quản trị. Các nhóm frontend có thể dựa vào đây để tích hợp.

## 1. Quy ước chung

- **Base URL:** `http://localhost:8081`
- **Prefix:** Tất cả API quản trị dùng tiền tố `/api/admin`.
- **Xác thực:** Gửi header `Authorization: Bearer {token}` cho mọi API (trừ login).
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
      "avatar": "https://cdn.example.com/avatar.png",
      "adminStatus": "ONLINE"
    }
    ```

---

## 3. Quản lý Người dùng

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

### Thống kê so sánh tháng này vs tháng trước
- Method & URL: `GET /api/admin/thong-ke-user-monthly`
- Response:
  ```json
  {
    "status": 1,
    "message": "Success",
    "data": {
      "currentMonth": {
        "total": 1222,
        "month": "11/2025"
      },
      "previousMonth": {
        "total": 1210,
        "month": "10/2025"
      },
      "change": {
        "amount": 12,
        "percentage": 0.99,
        "isIncrease": true
      }
    }
  }
  ```
- Lưu ý: `amount` là số thay đổi (có thể âm nếu giảm), `percentage` là phần trăm thay đổi, `isIncrease` là `true` nếu tăng, `false` nếu giảm.

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

## 4. Quản lý Tác phẩm

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
- **Thống kê so sánh tháng:** `GET /api/admin/artworks/thong-ke-tac-pham-monthly`
  - Response tương tự format thống kê user (so sánh tháng này vs tháng trước)

---

## 5. Quản lý Phòng đấu giá

- **Tạo nhanh:** `POST /api/admin/auction-rooms/them-phong` (body `AddAuctionRoomRequest` – `roomName`, `description`, `material`, `startedAt`, `stoppedAt`, `adminId`, `type`, `imageAuctionRoom`…).
- **Lấy danh sách:** `GET /api/admin/auction-rooms/lay-du-lieu` → `AdminAuctionRoomResponse` (kèm giá bắt đầu & hiện tại).
- **Tìm kiếm:** `GET /api/admin/auction-rooms/tim-kiem?q={keyword}`.
- **Cập nhật:** `PUT /api/admin/auction-rooms/cap-nhat/{roomId}` → `UpdateResponse`.
- **Xóa:** `DELETE /api/admin/auction-rooms/xoa/{roomId}`.
- **Thống kê:** `GET /api/admin/auction-rooms/thong-ke` → `{ totalRooms, runningRooms, upcomingRooms, completedRooms }`.
- **Thống kê so sánh tháng:** `GET /api/admin/auction-rooms/thong-ke-monthly`
  - Response tương tự format thống kê user (so sánh tháng này vs tháng trước)
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

## 6. Quản lý Thông báo

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

## 7. Quản lý Hóa đơn

- `GET /api/admin/invoices/lay-du-lieu`
- `GET /api/admin/invoices/tim-kiem?q=...`
- `GET /api/admin/invoices/thong-ke`
- **Thống kê so sánh tháng (doanh thu):** `GET /api/admin/invoices/thong-ke-monthly`
  - Response tương tự format thống kê user, nhưng `total` là tổng doanh thu (số tiền) thay vì số lượng
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

## 8. Quản lý Report

- `GET /api/admin/reports/lay-du-lieu` – trả `AdminReportResponse` (bao gồm thông tin người báo cáo, đối tượng bị báo cáo, reportReason, status, thời gian).
- `GET /api/admin/reports/tim-kiem?q=...`
- `GET /api/admin/reports/thong-ke`
- **Thống kê so sánh tháng:** `GET /api/admin/reports/thong-ke-monthly`
  - Response tương tự format thống kê user (so sánh tháng này vs tháng trước)
- `PUT /api/admin/reports/cap-nhat/{reportId}` – body `UpdateReportRequest` (reportReason, reportStatus, reportDoneTime).
- `DELETE /api/admin/reports/xoa/{reportId}`
- Response chuẩn `AdminReportApiResponse`.

---

## 9. Dashboard

### 1. Thống kê chung
- Method & URL: `GET /api/admin/dashboard/thong-ke`
- Response:
  ```json
  {
    "status": 1,
    "message": "Success",
    "data": {
      "totalUsers": {
        "currentMonth": 120,
        "previousMonth": 107,
        "change": 13,
        "percentage": 12.15,
        "isIncrease": true
      },
      "totalArtworks": {
        "currentMonth": 100,
        "previousMonth": 89,
        "change": 11,
        "percentage": 12.36,
        "isIncrease": true
      },
      "totalBids": {
        "currentMonth": 120,
        "previousMonth": 107,
        "change": 13,
        "percentage": 12.15,
        "isIncrease": true
      },
      "revenue": {
        "currentMonth": 50000000.0,
        "previousMonth": 45000000.0,
        "change": 5000000.0,
        "percentage": 11.11,
        "isIncrease": true
      },
      "totalAuctionRooms": 23,
      "activeUsers": 10
    }
  }
  ```
- Lưu ý:
  - `totalUsers`, `totalArtworks`, `totalBids`, `revenue`: Có so sánh tháng này vs tháng trước
  - `totalAuctionRooms`, `activeUsers`: Chỉ là tổng số, không so sánh
  - `revenue`: Tính từ tổng `totalAmount` của tất cả Invoice trong tháng (dựa trên `createdAt`). API tự động xử lý cả trường hợp `totalAmount` là String hoặc Number trong database để đảm bảo tính toán chính xác.

### 2. Top 10 AuctionRoom mới nhất
- Method & URL: `GET /api/admin/dashboard/top-auction-rooms`
- Response:
  ```json
  {
    "status": 1,
    "message": "Success",
    "data": [
      {
        "id": "ACR-1",
        "roomName": "Tranh sơn dầu",
        "description": "...",
        "imageAuctionRoom": "...",
        "type": "VIP",
        "status": 1,
        "startedAt": "2025-11-23T10:00:00",
        "stoppedAt": "2025-11-23T12:00:00",
        "sessions": [
          {
            "id": "AS-1",
            "artworkId": "A-1",
            "artworkTitle": "Tranh phong cảnh mùa thu",
            "imageUrl": "...",
            "startingPrice": 10000.0,
            "currentPrice": 15000.0,
            "status": 1,
            "orderIndex": 1,
            "startTime": "2025-11-23T10:00:00",
            "endedAt": null
          }
        ]
      }
    ]
  }
  ```
- Lưu ý: Sắp xếp theo `startedAt` mới nhất, mỗi room có danh sách tất cả `sessions` trong room đó

### 3. Top 10 User mới đăng ký
- Method & URL: `GET /api/admin/dashboard/top-new-users`
- Response:
  ```json
  {
    "status": 1,
    "message": "Success",
    "data": [
      {
        "id": "U-1",
        "username": "N Nguyễn Thị A",
        "email": "a@gmail.com",
        "createdAt": "2025-10-22T10:00:00",
        "status": 1
      }
    ]
  }
  ```
- Lưu ý: Sắp xếp theo `createdAt` mới nhất

### 4. Top 10 Session có giá cao nhất
- Method & URL: `GET /api/admin/dashboard/top-sessions`
- Response:
  ```json
  {
    "status": 1,
    "message": "Success",
    "data": [
      {
        "sessionId": "AS-1",
        "auctionRoomId": "ACR-1",
        "roomName": "Tranh sơn dầu",
        "artworkId": "A-1",
        "artworkTitle": "Tranh phong cảnh mùa thu",
        "artworkImageUrl": "...",
        "artistName": "Nguyễn Văn A",
        "totalAmount": 100000.0,
        "winnerName": "Trần Văn B",
        "winnerEmail": "b@gmail.com",
        "orderDate": "2025-11-23T12:00:00",
        "viewCount": 150
      }
    ]
  }
  ```
- Lưu ý: 
  - **Sắp xếp theo `currentPrice` từ AuctionSession** (giá đấu giá cao nhất), giảm dần
  - API lấy dữ liệu chính từ **AuctionSession** để đảm bảo chính xác với session thực tế
  - `totalAmount`: Lấy từ Invoice nếu có (khi đã có người thắng và tạo invoice), nếu không có Invoice thì dùng `currentPrice` từ session
  - `winnerName`, `winnerEmail`, `orderDate`: Lấy từ Invoice nếu có, nếu không có Invoice thì lấy từ `winnerId` trong AuctionSession
  - `viewCount`: Lượt xem của session (lấy từ AuctionSession)
  - `artworkTitle`, `artworkImageUrl`, `artistName`: Lấy từ Artwork entity, đảm bảo dữ liệu đầy đủ và chính xác
  - `roomName`: Lấy từ AuctionRoom entity

---

## 10. Thống kê theo khoảng thời gian (Statistics)

Các API này cho phép thống kê dữ liệu theo khoảng thời gian với format biểu đồ.

### Thống kê doanh thu (Revenue)
- Method & URL: `POST /api/admin/statistics/revenue`
- Request:
  ```json
  {
    "begin": "2025-11-01",
    "end": "2025-11-30"
  }
  ```
- Response:
  ```json
  {
    "status": 1,
    "message": "Success",
    "data": [
      {
        "date": "01/11/2025",
        "totalAmount": 5000000.0
      },
      {
        "date": "02/11/2025",
        "totalAmount": 7500000.0
      }
    ],
    "labels": ["01/11/2025", "02/11/2025", ...],
    "datasets": [
      {
        "label": "Thống kê doanh thu",
        "data": [5000000.0, 7500000.0, ...],
        "backgroundColor": ["#...", "#...", ...]
      }
    ]
  }
  ```
- Lưu ý:
  - Tính tổng `totalAmount` từ Invoice theo ngày (dựa trên `createdAt`)
  - API tự động xử lý cả trường hợp `totalAmount` là **String hoặc Number** trong database để đảm bảo tính toán chính xác
  - Sử dụng MongoDB aggregation với `$convert` để chuyển String sang Double nếu cần
  - Format ngày: `dd/MM/yyyy`
  - Mỗi item trong `data` có `totalAmount` (số tiền) thay vì `count` (số lượng)

### Các API thống kê khác
- `POST /api/admin/statistics/users-registration` - Thống kê số người dùng đăng ký theo ngày
- `POST /api/admin/statistics/auction-rooms` - Thống kê số phòng đấu giá được lập theo ngày
- `POST /api/admin/statistics/reports` - Thống kê số report được báo cáo theo ngày
- `POST /api/admin/statistics/artworks` - Thống kê số tác phẩm được thêm vào theo ngày
- `POST /api/admin/statistics/bids` - Thống kê số đấu giá (bids) theo ngày

Tất cả các API này có format request và response tương tự, chỉ khác:
- Request: `{ "begin": "YYYY-MM-DD", "end": "YYYY-MM-DD" }`
- Response: `data` là mảng các object có `date` và `count` (số lượng) thay vì `totalAmount`

---

## 11. Ghi chú cho Frontend

1. **Header mặc định**
   ```
   Authorization: Bearer {token}
   Content-Type: application/json
   ```
2. **Status field:** luôn dùng `status` 1/0 để hiển thị toast thành công/thất bại.
3. **Datetime:** API dùng `ISO 8601` (ví dụ `2025-11-21T10:00:00`). Frontend cần chuyển timezone nếu hiển thị theo giờ địa phương.
4. **List pagination:** hiện tại tất cả API trả toàn bộ dữ liệu. Nếu cần phân trang bổ sung query `page`, `size` ở phiên bản sau.

---

Nếu cần thêm ví dụ cụ thể cho từng màn hình, hãy liên hệ backend để cập nhật file này.

