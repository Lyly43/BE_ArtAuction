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
      "role": 4
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

### Lọc người dùng
- Method & URL: `POST /api/admin/loc-user`
- Request Body:
  ```json
  {
    "role": 1,
    "status": 1,
    "gender": 0,
    "province": "Hà Nội",
    "dateOfBirthFrom": "1990-01-01",
    "dateOfBirthTo": "2000-12-31",
    "createdAtFilter": "last7days"
  }
  ```
- Request Body Fields (tất cả đều optional - có thể để `null` hoặc không gửi):
  - `role`: `null` = bỏ qua filter (lấy tất cả), `0` = user, `1` = buyer, `2` = seller
  - `status`: `null` = bỏ qua filter (lấy tất cả), `1` = Active, `2` = Locked
  - `gender`: `null` = bỏ qua filter (lấy tất cả), `0` = Male, `1` = Female, `2` = Other
  - `province`: `null` hoặc chuỗi rỗng = bỏ qua filter, nếu có giá trị sẽ tìm trong trường `address` (case-insensitive)
  - `dateOfBirthFrom`: `null` = bỏ qua filter, nếu có giá trị (format: `yyyy-MM-dd`) sẽ lọc từ ngày này trở đi
  - `dateOfBirthTo`: `null` = bỏ qua filter, nếu có giá trị (format: `yyyy-MM-dd`) sẽ lọc đến ngày này
  - `createdAtFilter`: `null` hoặc chuỗi rỗng = bỏ qua filter, `"last7days"` = 7 ngày gần nhất, `"thismonth"` = tháng hiện tại
- Response: Danh sách `AdminUserResponse` với các trường:
  ```json
  [
    {
      "id": "U-8",
      "fullname": "anna_ho",
      "email": "anna@example.com",
      "phonenumber": "0908901234",
      "gender": 0,
      "dateOfBirth": "1994-08-14",
      "address": "Quang Ninh, Vietnam",
      "cccd": "789012345678",
      "role": 1,
      "status": 1,
      "balance": 0,
      "avt": "",
      "createdAt": "2025-09-26T07:00:00"
    }
  ]
  ```
- Lưu ý:
  - **Tất cả các trường filter đều optional**: Có thể để `null` hoặc không gửi trong request body, khi đó filter đó sẽ bỏ qua (lấy tất cả)
  - **Có thể kết hợp nhiều filter cùng lúc**: Ví dụ chỉ filter theo `status` và `gender`, các trường khác để `null`
  - **Request body có thể là `{}` (empty object)**: Khi đó sẽ trả về tất cả users
  - **`province`**: Tìm kiếm trong trường `address` (case-insensitive, partial match)
  - **`dateOfBirthFrom` và `dateOfBirthTo`**: Có thể dùng riêng lẻ hoặc kết hợp để tạo range
  - **`createdAtFilter`**: Chỉ hỗ trợ 2 giá trị: `"last7days"` (7 ngày gần nhất) và `"thismonth"` (tháng hiện tại). Các giá trị khác sẽ bị bỏ qua
  - **`gender`**: Nếu user không có `gender` (null) và request có filter `gender`, user đó sẽ bị loại bỏ

### Thống kê
- Method & URL: `GET /api/admin/thong-ke-user`
- Response:
  ```json
  {
    "totalUsers": 1222,
    "activeUsers": 1100,
    "totalSellers": 500,
    "totalBlockedUsers": 50,
    "monthlyComparison": {
      "currentMonth": 1222,
      "previousMonth": 1210,
      "changeAmount": 12,
      "changePercentage": 0.99,
      "isIncrease": true,
      "currentMonthLabel": "11/2025",
      "previousMonthLabel": "10/2025"
    }
  }
  ```
- Lưu ý:
  - `totalUsers`: Tổng số người dùng
  - `activeUsers`: Tổng số người dùng đang hoạt động (status = 1)
  - `totalSellers`: Tổng số người bán (role = 2)
  - `totalBlockedUsers`: Tổng số người dùng bị khóa (status = 2)
  - `monthlyComparison`: So sánh tháng này vs tháng trước cho tổng số user
    - `changeAmount`: Số thay đổi (có thể âm nếu giảm)
    - `changePercentage`: Phần trăm thay đổi (có thể âm nếu giảm)
    - `isIncrease`: `true` nếu tăng, `false` nếu giảm hoặc không đổi
    - `currentMonthLabel`, `previousMonthLabel`: Format "MM/yyyy"

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

## 4. Quản lý Admin

### Thêm admin

- **Bước 1: Upload avatar (nếu có)**
  - Dùng endpoint upload ảnh chung: `POST /api/admin/uploads/upload-image`
  - Xem chi tiết ở phần "Upload ảnh chung" bên dưới
  - Sau khi upload, lấy `imageUrl` từ response và gán vào field `avatar` khi tạo admin

- **Bước 2: Tạo admin bằng JSON**
  - Method & URL: `POST /api/admin/admins/them-admin`
  - Content-Type: `application/json`
  - Request Body:
   
    {
      "fullName": "Nguyễn Văn A",
      "email": "admin@example.com",
      "password": "123456",
      "phoneNumber": "0123456789",
      "address": "123 Đường ABC",
      "status": 1,
      "role": 3,
      "avatar": "https://cloudinary.com/.../avatar"
    }
      - Mô tả các field:
    - `fullName`: String (required) - Tên đầy đủ của admin
    - `email`: String (required) - Email của admin (phải unique)
    - `password`: String (required) - Mật khẩu của admin
    - `phoneNumber`: String (optional) - Số điện thoại
    - `address`: String (optional) - Địa chỉ
    - `status`: Integer (optional, default: 1) - `0` = Bị Khóa, `1` = Hoạt động
    - `role`: Integer (optional, default: 3) - Vai trò của admin
    - `avatar`: String (optional) - URL avatar (lấy từ bước upload ở trên)
  - Response:
   
    {
      "status": 1,
      "message": "Admin created successfully",
      "data": {
        "id": "Ad-xxx",
        "fullName": "Nguyễn Văn A",
        "email": "admin@example.com",
        "phoneNumber": "0123456789",
        "address": "123 Đường ABC",
        "avatar": "https://cloudinary.com/.../avatar",
        "role": 3,
        "status": 1,
        "createdAt": "2025-11-23T12:00:00",
        "updatedAt": "2025-11-23T12:00:00"
      }
    }
      - Lưu ý:
    - Có thể bỏ qua `avatar` nếu không muốn upload ảnh (trường `avatar` trong response sẽ là `null`)
    - Nếu không gửi `role` thì hệ thống tự set `role = 3`
    - Email phải unique, nếu trùng sẽ trả về lỗi `"Email already exists"` với `status = 0`

### Upload ảnh chung (DUY NHẤT - dùng cho tất cả các trường hợp)
- **Method & URL:** `POST /api/admin/uploads/upload-image`
- **Content-Type:** `multipart/form-data` (Postman sẽ tự động set khi chọn form-data)
- **Mô tả:**
  - **Đây là endpoint DUY NHẤT để upload ảnh** trong admin panel, dùng chung cho TẤT CẢ các trường hợp:
    - Upload avatar admin
    - Upload ảnh phòng đấu giá
    - Upload ảnh bất kỳ khác
  - Frontend chỉ cần gọi API này, lấy `imageUrl` từ response và gán vào field tương ứng trong các API khác.
- **Hướng dẫn sử dụng trong Postman:**
  1. Chọn method: `POST`
  2. URL: `http://localhost:8081/api/admin/uploads/upload-image`
  3. Tab **Body** → Chọn **form-data**
  4. Thêm field:
     - **Key:** `imageFile` | **Type:** `File` | **Value:** (click "Select Files" và chọn file ảnh từ máy tính)
- **Response (200):**
  ```json
  {
    "status": 1,
    "message": "Upload ảnh thành công",
    "data": {
      "imageUrl": "https://res.cloudinary.com/.../image/upload/auctionaa/misc/common-1733142222333.jpg",
      "publicId": "auctionaa/misc/common-1733142222333"
    }
  }
  ```
- **Lưu ý:**
  - `imageUrl`: URL ảnh dùng để lưu vào DB hoặc gửi kèm trong các API khác (ví dụ: field `avatar` khi tạo admin, field `imageAuctionRoom` khi tạo phòng đấu giá).
  - `publicId`: dùng nếu sau này cần xóa ảnh trên Cloudinary.
  - Nếu file rỗng hoặc không phải `image/*`, API sẽ trả `status = 0` và message lỗi tương ứng.
  - **Workflow:** Upload ảnh → Lấy `imageUrl` từ response → Gửi `imageUrl` vào field tương ứng của các API khác (tạo admin, tạo phòng đấu giá, ...)

### Lấy danh sách admin
- Method & URL: `GET /api/admin/admins/lay-du-lieu`
- Quyền: **Chỉ Super Admin (role = 4)**
- Response: Mảng `AdminAdminResponse` với các trường:
  ```json
  [
    {
      "id": "Ad-xxx",
      "fullName": "Nguyễn Văn A",
      "email": "admin@example.com",
      "phoneNumber": "0123456789",
      "address": "123 Đường ABC",
      "avatar": "https://cloudinary.com/.../avatar",
      "role": 3,
      "status": 1,
      "createdAt": "2025-11-23T12:00:00",
      "updatedAt": "2025-11-23T12:00:00"
    }
  ]
  ```

### Tìm kiếm admin
- Method & URL: `GET /api/admin/admins/tim-kiem?q={searchTerm}`
- Query Parameters:
  - `q`: String (optional) - Từ khóa tìm kiếm theo ID, fullName, email, phoneNumber
- Response: Danh sách `AdminAdminResponse` tương tự như lấy danh sách
- Lưu ý: Nếu `q` rỗng hoặc `null`, API sẽ trả về tất cả admin

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
- Lưu ý:
  - `totalAdmins`: Tổng số admin
  - `activeAdmins`: Số admin hoạt động (status = 1)
  - `lockedAdmins`: Số admin bị khóa (status = 0)

### Cập nhật admin
- Method & URL: `PUT /api/admin/admins/cap-nhat/{adminId}`
- Request Body (JSON):
  ```json
  {
    "fullName": "Nguyễn Văn B",
    "email": "admin2@example.com",
    "phoneNumber": "0987654321",
    "address": "456 Đường XYZ",
    "password": "newpassword123",
    "status": 1
  }
  ```
- Response: `UpdateResponse<AdminAdminResponse>`
- Lưu ý: Tất cả các trường trong request body đều optional, chỉ cập nhật các trường được gửi lên

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

### Lấy chi tiết 1 admin theo ID
- Method & URL: `GET /api/admin/admins/{adminId}`
- Quyền: **Chỉ Super Admin (role = 4)**
- Response:
  ```json
  {
    "status": 1,
    "message": "Success",
    "data": {
      "id": "Ad-xxx",
      "fullName": "Nguyễn Văn A",
      "email": "admin@example.com",
      "phoneNumber": "0123456789",
      "address": "123 Đường ABC",
      "avatar": "https://cloudinary.com/.../avatar",
      "role": 3,
      "status": 1,
      "createdAt": "2025-11-23T12:00:00",
      "updatedAt": "2025-11-23T12:00:00"
    }
  }
  ```

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
- **Lọc tác phẩm:** `POST /api/admin/artworks/loc-tac-pham`
  - **Lưu ý quan trọng**: Request phải có header `Content-Type: application/json`
  - Request Body:
    ```json
    {
      "paintingGenre": "Landscape",
      "priceRange": "5-20tr",
      "status": 1
    }
    ```
  - Request Body Fields (tất cả đều optional - có thể để `null` hoặc không gửi):
    - `paintingGenre`: `null` hoặc chuỗi rỗng = bỏ qua filter, nếu có giá trị sẽ tìm trong trường `paintingGenre` (case-insensitive, partial match). Ví dụ: "Abstract", "Portrait", "Landscape", "Modern", "Traditional"
    - `priceRange`: `null` hoặc chuỗi rỗng = bỏ qua filter, hỗ trợ các preset: `"<5tr"` (< 5 triệu), `"5-20tr"` (5-20 triệu), `"20-100tr"` (20-100 triệu), `">100tr"` (> 100 triệu). **Lưu ý: Lọc theo `startedPrice` của tác phẩm**
    - `priceMin`: `null` = bỏ qua filter, giá tối thiểu (BigDecimal) - chỉ dùng khi không có `priceRange`. **Lưu ý: Lọc theo `startedPrice` của tác phẩm**
    - `priceMax`: `null` = bỏ qua filter, giá tối đa (BigDecimal) - chỉ dùng khi không có `priceRange`. **Lưu ý: Lọc theo `startedPrice` của tác phẩm**
    - `status`: `null` = bỏ qua filter (lấy tất cả), `0` = Not Approved, `1` = Approved, `2` = Up for Auction, `3` = Refused
  - Response: Danh sách `AdminArtworkResponse` với các trường:
    ```json
    [
      {
        "id": "Aw-01",
        "title": "Sunset Over Da Nang",
        "description": "Mô tả tác phẩm...",
        "author": "Nguyễn Văn A",
        "yearOfCreation": 2023,
        "material": "Oil Paint",
        "paintingGenre": "Landscape",
        "size": "80x120 cm",
        "avtArtwork": "https://cdn.example.com/artwork.jpg",
        "startedPrice": 15000000,
        "status": 1,
        "createdAt": "2025-11-20T10:00:00"
      }
    ]
    ```
  - Lưu ý:
    - **Tất cả các trường filter đều optional**: Có thể để `null` hoặc không gửi trong request body, khi đó filter đó sẽ bỏ qua (lấy tất cả)
    - **Có thể kết hợp nhiều filter cùng lúc**: Ví dụ chỉ filter theo `status` và `paintingGenre`, các trường khác để `null`
    - **Request body có thể là `{}` (empty object)**: Khi đó sẽ trả về tất cả artworks
    - **`priceRange` vs `priceMin/priceMax`**: Nếu có `priceRange` thì sẽ dùng preset, bỏ qua `priceMin/priceMax`. Nếu không có `priceRange` thì dùng `priceMin/priceMax` (có thể dùng riêng lẻ hoặc kết hợp)
    - **Price Range lọc theo `startedPrice`**: Tất cả các filter về giá (`priceRange`, `priceMin`, `priceMax`) đều lọc dựa trên trường `startedPrice` của tác phẩm (Artwork)
    - **`paintingGenre`**: Tìm kiếm partial match (case-insensitive) trong trường `paintingGenre` của artwork
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
- **Chi tiết một tác phẩm:** `GET /api/admin/artworks/{artworkId}`
  - Response:
    ```json
    {
      "id": "Aw-533966731001600",
      "ownerId": "U-531569901185400",
      "owner": {
          "id": "U-531569901185400",
          "username": "nguyenvana",
          "email": "llttminh@gmail.com",
          "phoneNumber": "0987654321",
          "status": 1
      },
      "title": "Test ở bé biển ",
      "description": "Bức tranh vẽ con chó mang tên Võ ĐĂng Nam ngu như pò hjhj",
      "paintingGenre": "Tranh con cặt",
      "material": "Tranh dầu",
      "size": "30 * 40 cm",
      "yearOfCreation": 2004,
      "certificateId": null,
      "startedPrice": 32000000,
      "avtArtwork": "https://example.com/images/artwork-avatar.jpg",
      "imageUrls": [
          "https://example.com/images/artwork-1.jpg",
          "https://example.com/images/artwork-2.jpg",
          "https://example.com/images/artwork-3.jpg"
      ],
      "status": 2,
      "aiVerified": false,
      "createdAt": "2025-11-26T17:00:35.808",
      "updatedAt": "2025-11-26T20:23:46.31"
  }
    ```
  - Trả về đầy đủ thông tin từ document `artworks` kèm thông tin chủ sở hữu (`owner` object với `id`, `username`, `email`, `phonenumber`, `status`).
- **Duyệt tác phẩm:** `POST /api/admin/artworks/approve/{artworkId}`
  - Body:
    ```json
    {
      "startedPrice": 50000,
      "adminNote": "Điều chỉnh giá theo hội đồng thẩm định"
    }
    ```
  - Mô tả: Admin cập nhật lại `startedPrice` (nếu có) và chuyển `status` sang `1`. Sau khi duyệt hệ thống tự động gửi email "Artwork Approved" cho author dựa trên `ownerId`.
  - Ràng buộc: Chỉ những tác phẩm có `aiVerified = true` (đã được hệ thống AI kiểm tra) mới được phép approve. Nếu `aiVerified = false` API sẽ trả về lỗi 400 và không thay đổi trạng thái.
- **Từ chối tác phẩm:** `POST /api/admin/artworks/reject/{artworkId}`
  - Body:
    ```json
    {
      "reason": "Ảnh chưa đạt chất lượng in ấn",
      "adminNote": "Vui lòng bổ sung ảnh chụp rõ hơn."
    }
    ```
  - Mô tả: Admin chuyển `status` về `3` (rejected) và buộc phải nhập `reason`. Hệ thống gửi email "Artwork Rejected" cho author để thông báo lý do.
- **Thống kê:** `GET /api/admin/artworks/thong-ke-tac-pham`
  - Response:
    ```json
    {
      "totalArtworks": 500,
      "pendingArtworks": 50,
      "approvedArtworks": 400,
      "rejectedArtworks": 50,
      "monthlyComparison": {
        "currentMonth": 500,
        "previousMonth": 480,
        "changeAmount": 20,
        "changePercentage": 4.17,
        "isIncrease": true,
        "currentMonthLabel": "11/2025",
        "previousMonthLabel": "10/2025"
      }
    }
    ```
  - Lưu ý:
    - `totalArtworks`: Tổng số tác phẩm
    - `pendingArtworks`: Số tác phẩm chưa duyệt (status = 0)
    - `approvedArtworks`: Số tác phẩm đã duyệt (status = 1)
    - `rejectedArtworks`: Số tác phẩm bị từ chối (status = 3)
    - `monthlyComparison`: So sánh tháng này vs tháng trước cho tổng số artwork
      - `changeAmount`: Số thay đổi (có thể âm nếu giảm)
      - `changePercentage`: Phần trăm thay đổi (có thể âm nếu giảm)
      - `isIncrease`: `true` nếu tăng, `false` nếu giảm hoặc không đổi

---

## 6. Quản lý Phòng đấu giá

- **Tạo nhanh:** `POST /api/admin/auction-rooms/them-phong` (body `AddAuctionRoomRequest` – `roomName`, `description`, `material`, `startedAt`, `stoppedAt`, `estimatedEndTime`, `adminId`, `type`, `imageAuctionRoom`…).
- **Lấy danh sách:** `GET /api/admin/auction-rooms/lay-du-lieu` → `AdminAuctionRoomResponse` (kèm giá bắt đầu & hiện tại, thêm `estimatedEndTime`).
- **Tìm kiếm:** `GET /api/admin/auction-rooms/tim-kiem?q={keyword}`.
- **Lọc phòng đấu giá:** `POST /api/admin/auction-rooms/loc-phong-dau-gia`
  - **Lưu ý quan trọng**: Request phải có header `Content-Type: application/json`
  - Request Body:
    ```json
    {
      "statuses": [1, 2],
      "startTimeFrom": "2025-12-01T00:00:00",
      "startTimeTo": "2025-12-31T23:59:59",
      "endTimeFrom": "2025-12-01T00:00:00",
      "endTimeTo": "2025-12-31T23:59:59",
      "participantsRange": "10-50"
    }
    ```
  - Request Body Fields (tất cả đều optional - có thể để `null` hoặc không gửi):
    - `statuses`: `null` hoặc mảng rỗng `[]` = bỏ qua filter, mảng các số nguyên `[0, 1, 2, 3]` để chọn nhiều status cùng lúc. Giá trị: `0` = Sắp diễn ra (Coming Soon), `1` = Đang diễn ra (Live), `2` = Đã hoàn thành (Finished), `3` = Hoãn (Postponed)
    - `startTimeFrom`: `null` = bỏ qua filter, thời gian bắt đầu tối thiểu (LocalDateTime) - lọc theo `startedAt` của phòng đấu giá
    - `startTimeTo`: `null` = bỏ qua filter, thời gian bắt đầu tối đa (LocalDateTime) - lọc theo `startedAt` của phòng đấu giá
    - `endTimeFrom`: `null` = bỏ qua filter, thời gian kết thúc tối thiểu (LocalDateTime) - lọc theo `stoppedAt` của phòng đấu giá
    - `endTimeTo`: `null` = bỏ qua filter, thời gian kết thúc tối đa (LocalDateTime) - lọc theo `stoppedAt` của phòng đấu giá
    - `participantsRange`: `null`, chuỗi rỗng, hoặc `"all"` = bỏ qua filter, hỗ trợ các preset: `"<10"` (< 10 người), `"10-50"` (10-50 người), `">50"` (> 50 người). **Lưu ý: Lọc theo `totalMembers` (số lượng `memberIds`) của phòng đấu giá**
  - Response: Danh sách `AdminAuctionRoomResponse` với các trường:
    ```json
    [
      {
        "id": "ACR-123",
        "roomName": "Luxury Night",
        "description": "Phiên VIP cuối tuần",
        "type": "VIP",
        "imageAuctionRoom": "https://cdn.example.com/auction-room.jpg",
        "status": 1,
        "startedAt": "2025-12-01T10:00:00",
        "stoppedAt": "2025-12-01T12:00:00",
        "estimatedEndTime": "2025-12-01T12:30:00",
        "viewCount": 1200,
        "totalMembers": 25,
        "startingPrice": 1000000,
        "currentPrice": 1500000,
        "createdAt": "2025-11-20T10:00:00"
      }
    ]
    ```
  - Lưu ý:
    - **Tất cả các trường filter đều optional**: Có thể để `null` hoặc không gửi trong request body, khi đó filter đó sẽ bỏ qua (lấy tất cả)
    - **Có thể kết hợp nhiều filter cùng lúc**: Ví dụ chỉ filter theo `statuses` và `participantsRange`, các trường khác để `null`
    - **Request body có thể là `{}` (empty object)**: Khi đó sẽ trả về tất cả auction rooms
    - **`statuses`**: Có thể chọn nhiều status cùng lúc bằng cách gửi mảng `[0, 1, 2]` hoặc chỉ một status `[1]`
    - **Time ranges**: Có thể dùng riêng lẻ `startTimeFrom` hoặc kết hợp `startTimeFrom` và `startTimeTo` để tạo range
    - **`participantsRange`**: Lọc theo số lượng người tham gia (`totalMembers` = số phần tử trong `memberIds`)
- **Cập nhật:** `PUT /api/admin/auction-rooms/cap-nhat/{roomId}` → `UpdateResponse`.
- **Xóa:** `DELETE /api/admin/auction-rooms/xoa/{roomId}`.
- **Thống kê:** `GET /api/admin/auction-rooms/thong-ke`
  
- **Danh sách artworks có thể thêm vào phòng:** `GET /api/admin/auction-rooms/artworks`
  - Mô tả: Lấy tất cả artworks đã được duyệt (status = 1), chưa thuộc session nào và cũng chưa bị tạo hóa đơn để đảm bảo chưa có phòng đấu giá nào sử dụng.
  - Response: Mỗi phần tử theo cấu trúc `ArtworkForSelectionResponse` gồm `id`, `title`, `description`, `author` (chủ sở hữu), `paintingGenre`, `material`, `size`, `startedPrice`, `avtArtwork`, `status`.
  - Lưu ý: API dùng cho màn hình chọn tác phẩm khi cấu hình phòng đấu giá. Chỉ hiển thị những artwork hợp lệ để tránh trùng session.

  - Response:
    ```json
    {
      "totalRooms": 100,
      "runningRooms": 20,
      "upcomingRooms": 30,
      "completedRooms": 45,
      "cancelRooms": 5,
      "monthlyComparison": {
        "currentMonth": 100,
        "previousMonth": 95,
        "changeAmount": 5,
        "changePercentage": 5.26,
        "isIncrease": true,
        "currentMonthLabel": "11/2025",
        "previousMonthLabel": "10/2025"
      }
    }
    ```
  - Lưu ý:
    - `totalRooms`: Tổng số phòng đấu giá
    - `runningRooms`: Số phòng đang chạy (status = 1)
    - `upcomingRooms`: Số phòng sắp diễn ra (status = 0)
    - `completedRooms`: Số phòng đã hoàn thành (status = 2)
    - `cancelRooms`: Số phòng đã hoãn (status = 3)
    - `monthlyComparison`: So sánh tháng này vs tháng trước cho tổng số phòng
      - `changeAmount`: Số thay đổi (có thể âm nếu giảm)
      - `changePercentage`: Phần trăm thay đổi (có thể âm nếu giảm)
      - `isIncrease`: `true` nếu tăng, `false` nếu giảm hoặc không đổi
- **Tạo phòng hoàn chỉnh (4 bước trong 1 API):** `POST /api/admin/auction-rooms/tao-phong-hoan-chinh`
  - Content-Type: `application/json`
  - Request Body:
  ```json
  {
    "roomName": "...",
    "description": "...",
    "material": "Oil",
    "type": "VIP",
    "imageAuctionRoom": "https://example.com/image.jpg",
    "startedAt": "2025-12-01T10:00:00",
    "stoppedAt": "2025-12-01T12:00:00",
    "estimatedEndTime": "2025-12-01T12:30:00",
    "adminId": "Ad-1",
    "depositAmount": 5000,
    "paymentDeadlineDays": 3,
    "artworks": [
      { "artworkId": "A-1", "startingPrice": 1000, "bidStep": 50 }
    ]
  }
  ```
  - `imageAuctionRoom`: String (optional) - URL ảnh phòng đấu giá (lấy từ endpoint upload ảnh chung `/api/admin/uploads/upload-image`)
  - `estimatedEndTime`: String (optional, format `yyyy-MM-dd'T'HH:mm:ss`) - Thời gian kết thúc dự kiến của cả phòng (dùng để hiển thị & gửi email cảnh báo)
  - Response: `{ "status": 1, "message": "Auction room created successfully", "data": { "roomId": "...", "sessionsCreated": 3 } }`
  - **Lưu ý về upload ảnh:**
    - Để upload ảnh phòng đấu giá, dùng endpoint upload ảnh chung: `POST /api/admin/uploads/upload-image`
    - Xem chi tiết ở phần "Upload ảnh chung" trong mục "Quản lý Admin"
    - Sau khi upload, lấy `imageUrl` từ response và gán vào field `imageAuctionRoom` khi tạo phòng
- **Lấy chi tiết phòng:** `GET /api/admin/auction-rooms/{roomId}`
  - Response:
    ```json
    {
      "id": "ACR-123",
      "roomName": "Luxury Night",
      "type": "VIP",
      "admin": {
        "id": "Ad-1",
        "fullName": "Nguyễn Admin",
        "email": "admin@example.com",
        "phoneNumber": "0909xxx"
      },
      "description": "Phiên VIP cuối tuần",
      "imageAuctionRoom": "https://cdn.example.com/auction-room.jpg",
      "startedAt": "2025-12-01T10:00:00",
      "stoppedAt": "2025-12-01T12:00:00",
      "totalMembers": 150,
      "viewCount": 1200,
      "depositAmount": 5000,
      "status": 1,
      "createdAt": "2025-11-20T10:00:00",
      "updatedAt": "2025-11-25T15:30:00",
      "artworks": [
        {
          "sessionId": "ATSS-01",
          "artworkId": "Aw-01",
          "artworkName": "Sunset Symphony",
          "author": "Artist 01",
          "avtArtwork": "https://cdn.example.com/artwork1.jpg",
          "startingPrice": 1000,
          "currentPrice": 1800,
          "bidStep": 50,
          "status": 1,
          "imageUrls": ["https://cdn.example.com/img1.jpg", "https://cdn.example.com/img2.jpg"],
          "aiVerified": true,
          "size": "80x120 cm",
          "material": "Oil Paint",
          "certificateId": "CERT-001",
          "paintingGenre": "Landscape",
          "yearOfCreation": 2023,
          "description": "Mô tả chi tiết về tác phẩm..."
        },
        {
          "sessionId": "ATSS-02",
          "artworkId": "Aw-02",
          "artworkName": "Forest Dream",
          "author": "Artist 02",
          "avtArtwork": "https://cdn.example.com/artwork2.jpg",
          "startingPrice": 800,
          "currentPrice": 950,
          "bidStep": 25,
          "status": 0,
          "imageUrls": ["https://cdn.example.com/img3.jpg"],
          "aiVerified": true,
          "size": "60x90 cm",
          "material": "Acrylic",
          "certificateId": "CERT-002",
          "paintingGenre": "Abstract",
          "yearOfCreation": 2022,
          "description": "Mô tả chi tiết về tác phẩm..."
        }
      ]
    }
    ```
  - Trả về đầy đủ thông tin phòng, admin phụ trách, cọc, tổng thành viên, trạng thái và danh sách tất cả phiên/Artwork trong phòng. Mỗi artwork session bao gồm:
    - Thông tin session: `sessionId`, `status`, `startingPrice`, `currentPrice`, `bidStep`
    - Thông tin artwork: `artworkId`, `artworkName`, `author`, `avtArtwork`, `imageUrls`, `aiVerified`, `size`, `material`, `certificateId`, `paintingGenre`, `yearOfCreation`, `description`

---

## 7. Quản lý Thông báo

- **Lấy dữ liệu:** `GET /api/admin/notifications/lay-du-lieu`
- **Tìm kiếm:** `GET /api/admin/notifications/tim-kiem?q=...`
- **Lọc theo trạng thái:** `GET /api/admin/notifications/loc-theo-trang-thai?status=0|1`
  - `status=0`: thông báo thất bại, `status=1`: đã gửi thành công
  - Response: mảng `AdminNotificationResponse` ứng với trạng thái yêu cầu
- **Lọc thông báo:** `POST /api/admin/notifications/loc-thong-bao`
  - **Lưu ý quan trọng**: Request phải có header `Content-Type: application/json`
  - Request Body:
    ```json
    {
      "notificationStatus": 1,
      "dateFrom": "2025-12-01T00:00:00",
      "dateTo": "2025-12-31T23:59:59"
    }
    ```
  - Request Body Fields (tất cả đều optional - có thể để `null` hoặc không gửi):
    - `notificationStatus`: `null` = bỏ qua filter (lấy tất cả), `0` = failed, `1` = sent
    - `dateFrom`: `null` = bỏ qua filter, ngày bắt đầu tối thiểu (LocalDateTime) - **lọc theo `notificationTime` của thông báo**
    - `dateTo`: `null` = bỏ qua filter, ngày kết thúc tối đa (LocalDateTime) - **lọc theo `notificationTime` của thông báo**
  - Response: `AdminNotificationApiResponse<List<AdminNotificationResponse>>` với cấu trúc tương tự như `GET /api/admin/notifications/lay-du-lieu`
  - Lưu ý:
    - **Tất cả các trường filter đều optional**: Có thể để `null` hoặc không gửi trong request body, khi đó filter đó sẽ bỏ qua (lấy tất cả)
    - **Có thể kết hợp nhiều filter cùng lúc**: Ví dụ chỉ filter theo `notificationStatus` và `dateFrom`, các trường khác để `null`
    - **Request body có thể là `{}` (empty object)**: Khi đó sẽ trả về tất cả notifications
    - **Date range lọc theo `notificationTime`**: Tất cả các filter về ngày (`dateFrom`, `dateTo`) đều lọc dựa trên trường `notificationTime` của thông báo
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
- **Lọc hóa đơn:** `POST /api/admin/invoices/loc-hoa-don`
  - **Lưu ý quan trọng**: Request phải có header `Content-Type: application/json`
  - Request Body:
    ```json
    {
      "paymentStatus": 1,
      "invoiceStatus": 2,
      "totalAmountRange": "1M-10M",
      "dateFrom": "2025-12-01T00:00:00",
      "dateTo": "2025-12-31T23:59:59"
    }
    ```
  - Request Body Fields (tất cả đều optional - có thể để `null` hoặc không gửi):
    - `paymentStatus`: `null` = bỏ qua filter (lấy tất cả), `0` = Pending, `1` = Paid, `2` = Failed
    - `invoiceStatus`: `null` = bỏ qua filter (lấy tất cả), `0` = created, `1` = confirmed, `2` = completed, `3` = cancelled
    - `totalAmountRange`: `null` hoặc chuỗi rỗng = bỏ qua filter, hỗ trợ các preset: `"<1M"` (< 1 triệu), `"1M-10M"` (1-10 triệu), `">10M"` (> 10 triệu). **Lưu ý: Lọc theo `totalAmount` của hóa đơn**
    - `totalAmountMin`: `null` = bỏ qua filter, giá tối thiểu (BigDecimal) - chỉ dùng khi không có `totalAmountRange`
    - `totalAmountMax`: `null` = bỏ qua filter, giá tối đa (BigDecimal) - chỉ dùng khi không có `totalAmountRange`
    - `dateFrom`: `null` = bỏ qua filter, ngày bắt đầu tối thiểu (LocalDateTime) - **lọc theo `orderDate` của hóa đơn**
    - `dateTo`: `null` = bỏ qua filter, ngày kết thúc tối đa (LocalDateTime) - **lọc theo `orderDate` của hóa đơn**
  - Response: `AdminInvoiceApiResponse<List<AdminInvoiceResponse>>` với cấu trúc:
    ```json
    {
      "status": 1,
      "message": "Lọc hóa đơn thành công",
      "data": [
        {
          "id": "IV-123",
          "userId": "U-8",
          "buyerName": "Nguyễn Văn A",
          "buyerEmail": "user@example.com",
          "auctionRoomId": "ACR-123",
          "roomName": "Luxury Night",
          "sessionId": "ATSS-01",
          "artworkId": "Aw-01",
          "artworkTitle": "Sunset Over Da Nang",
          "amount": 1000000,
          "buyerPremium": 100000,
          "insuranceFee": 50000,
          "salesTax": 100000,
          "shippingFee": 50000,
          "totalAmount": 1300000,
          "paymentMethod": "BANK_TRANSFER",
          "paymentStatus": 1,
          "invoiceStatus": 2,
          "orderDate": "2025-12-01T10:00:00",
          "paymentDate": "2025-12-01T11:00:00",
          "createdAt": "2025-12-01T10:00:00"
        }
      ]
    }
    ```
  - Lưu ý:
    - **Tất cả các trường filter đều optional**: Có thể để `null` hoặc không gửi trong request body, khi đó filter đó sẽ bỏ qua (lấy tất cả)
    - **Có thể kết hợp nhiều filter cùng lúc**: Ví dụ chỉ filter theo `paymentStatus` và `totalAmountRange`, các trường khác để `null`
    - **Request body có thể là `{}` (empty object)**: Khi đó sẽ trả về tất cả invoices
    - **`totalAmountRange` vs `totalAmountMin/totalAmountMax`**: Nếu có `totalAmountRange` thì sẽ dùng preset, bỏ qua `totalAmountMin/totalAmountMax`. Nếu không có `totalAmountRange` thì dùng `totalAmountMin/totalAmountMax` (có thể dùng riêng lẻ hoặc kết hợp)
    - **Date range lọc theo `orderDate`**: Tất cả các filter về ngày (`dateFrom`, `dateTo`) đều lọc dựa trên trường `orderDate` của hóa đơn (không phải `paymentDate` hay `createdAt`)
    - **"Overdue" (Quá hạn)**: Có thể được xử lý ở frontend bằng cách filter `paymentStatus = 0` (Pending) và `paymentDate < now` (nếu có paymentDate)
- **Thống kê:** `GET /api/admin/invoices/thong-ke`
  - Response:
    ```json
    {
      "status": 1,
      "message": "Thống kê hóa đơn",
      "data": {
        "totalInvoices": 500,
        "paidInvoices": 400,
        "pendingInvoices": 80,
        "failedInvoices": 20,
        "monthlyComparison": {
          "currentMonth": 500,
          "previousMonth": 450,
          "changeAmount": 50,
          "changePercentage": 11.11,
          "isIncrease": true,
          "currentMonthLabel": "11/2025",
          "previousMonthLabel": "10/2025"
        }
      }
    }
    ```
  - Lưu ý:
    - `totalInvoices`: Tổng số hóa đơn
    - `paidInvoices`: Số hóa đơn đã thanh toán (status = 2)
    - `pendingInvoices`: Số hóa đơn đang chờ (status = 0 hoặc 1)
    - `failedInvoices`: Số hóa đơn thất bại (status = 3)
    - `monthlyComparison`: So sánh số lượng invoice tháng này vs tháng trước
      - `currentMonth`/`previousMonth`: Số lượng invoice (count, không phải doanh thu)
      - `changeAmount`: Số lượng thay đổi (có thể âm nếu giảm)
      - `changePercentage`: Phần trăm thay đổi (có thể âm nếu giảm)
      - `isIncrease`: `true` nếu tăng, `false` nếu giảm hoặc không đổi
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
- **Thống kê:** `GET /api/admin/reports/thong-ke`
  - Response:
    ```json
    {
      "status": 1,
      "message": "Thống kê báo cáo",
      "data": {
        "totalReports": 200,
        "pendingReports": 50,
        "investigatingReports": 30,
        "resolvedReports": 120,
        "monthlyComparison": {
          "currentMonth": 200,
          "previousMonth": 180,
          "changeAmount": 20,
          "changePercentage": 11.11,
          "isIncrease": true,
          "currentMonthLabel": "11/2025",
          "previousMonthLabel": "10/2025"
        }
      }
    }
    ```
  - Lưu ý:
    - `totalReports`: Tổng số báo cáo
    - `pendingReports`: Số báo cáo đang chờ xử lý (status = 0)
    - `investigatingReports`: Số báo cáo đang điều tra (status = 1)
    - `resolvedReports`: Số báo cáo đã giải quyết (status = 2)
    - `monthlyComparison`: So sánh tháng này vs tháng trước cho tổng số report
      - `changeAmount`: Số thay đổi (có thể âm nếu giảm)
      - `changePercentage`: Phần trăm thay đổi (có thể âm nếu giảm)
      - `isIncrease`: `true` nếu tăng, `false` nếu giảm hoặc không đổi
- `PUT /api/admin/reports/cap-nhat/{reportId}` – body `UpdateReportRequest` (reportReason, reportStatus, reportDoneTime).
- **Xử lý báo cáo:** `POST /api/admin/reports/xu-ly/{reportId}`
  - **Lưu ý quan trọng**: Request phải có header `Content-Type: application/json`
  - **Mô tả**: API này cho phép admin xử lý báo cáo với các hành động cụ thể tùy theo loại entity bị báo cáo
  - Request Body:
    ```json
    {
      "action": "WARNING",
      "adminNote": "Ghi chú của admin về việc xử lý báo cáo này"
    }
    ```
  - Request Body Fields:
    - `action` (String, bắt buộc): Hành động xử lý
      - **Cho User Reports (entityType = 1)**:
        - `"WARNING"`: Gửi notification và email cảnh báo cho user (không chặn). 
          - **Lưu ý quan trọng**: Nếu user đã bị báo cáo và xử lý 3 lần trước đó (đã có 3 reports với status = Resolved), thì không thể sử dụng action `WARNING` nữa. Hệ thống sẽ trả về lỗi và bắt buộc phải sử dụng action `BLOCK`.
        - `"BLOCK"`: Chặn user (đổi status = 2) và gửi email thông báo + notification. Email sẽ bao gồm thông tin liên hệ để user có thể khiếu nại nếu bị oan hoặc bị spam báo cáo.
        - `"DISMISS"`: Từ chối báo cáo (report status = 3), không có hành động gì với user
      - **Cho Artwork Reports (entityType = 2) và AI Artwork Reports (entityType = 4)**:
        - `"REJECT"`: Từ chối artwork (đổi status = 3) và gửi email cho owner + notification. Email sẽ bao gồm thông tin liên hệ để author có thể khiếu nại nếu bị oan hoặc bị spam báo cáo.
        - `"DISMISS"`: Từ chối báo cáo (report status = 3), không có hành động gì với artwork
      - **Cho Auction Room Reports (entityType = 3)**:
        - `"CLOSE"`: Đóng room (đổi status = 0) và gửi email cho admin/host + notification. Email sẽ bao gồm thông tin liên hệ để user có thể khiếu nại nếu bị oan hoặc bị spam báo cáo.
        - `"DISMISS"`: Từ chối báo cáo (report status = 3), không có hành động gì với room
    - `adminNote` (String, optional): Ghi chú của admin về việc xử lý
  - Response: `AdminReportApiResponse<AdminReportResponse>` với report đã được cập nhật
  - **Nghiệp vụ chi tiết**:
    - **User Reports**: 
      - Khi chọn `WARNING`: 
        - Hệ thống đếm số lần user đã bị báo cáo và xử lý trước đó (chỉ đếm các reports có status = Resolved, không tính DISMISS)
        - Nếu user đã bị báo cáo 3 lần trước đó, hệ thống sẽ trả về lỗi: `"User đã bị báo cáo X lần. Không thể cảnh báo thêm, bắt buộc phải chặn tài khoản (action = BLOCK)"` và bắt buộc phải sử dụng action `BLOCK`
        - Nếu chưa đủ 3 lần, hệ thống gửi notification và email cảnh báo cho user (hiển thị số lần cảnh báo), nhưng không chặn tài khoản
        - Email cảnh báo sẽ hiển thị số lần cảnh báo và cảnh báo rằng lần vi phạm tiếp theo sẽ dẫn đến việc tài khoản bị chặn
      - Khi chọn `BLOCK`: 
        - Hệ thống đổi status của user thành 2 (Bị chặn), gửi email thông báo và notification
        - Email sẽ bao gồm thông tin liên hệ để user có thể khiếu nại nếu bị oan hoặc bị spam báo cáo:
          - Link khiếu nại
          - Email hỗ trợ: support@artauction.com
          - Hotline: 1900-xxxx
      - Khi chọn `DISMISS`: Chỉ từ chối báo cáo, không có hành động gì với user
    - **Artwork Reports (bao gồm AI Artwork)**:
      - Khi chọn `REJECT`: 
        - Hệ thống đổi status của artwork thành 3 (Từ chối), gửi email cho owner (author) và notification
        - Email sẽ bao gồm thông tin liên hệ để author có thể khiếu nại nếu bị oan hoặc bị spam báo cáo:
          - Link khiếu nại
          - Email hỗ trợ: support@artauction.com
          - Hotline: 1900-xxxx
      - Khi chọn `DISMISS`: Chỉ từ chối báo cáo, không có hành động gì với artwork
    - **Auction Room Reports**:
      - Khi chọn `CLOSE`: 
        - Hệ thống đổi status của room thành 0 (Đã kết thúc), gửi email cho admin/host và notification
        - Email sẽ bao gồm thông tin liên hệ để user có thể khiếu nại nếu bị oan hoặc bị spam báo cáo:
          - Link khiếu nại
          - Email hỗ trợ: support@artauction.com
          - Hotline: 1900-xxxx
      - Khi chọn `DISMISS`: Chỉ từ chối báo cáo, không có hành động gì với room
  - **Lưu ý**:
    - Report status sẽ được tự động cập nhật: `DISMISS` → status = 3 (Rejected), các action khác → status = 2 (Resolved)
    - Tất cả các email và notification đều được gửi tự động khi thực hiện action
    - Nếu entity không tồn tại hoặc action không hợp lệ với entityType, API sẽ trả về lỗi
    - **Quy tắc 3 lần báo cáo cho User**: 
      - Hệ thống tự động đếm số lần user đã bị báo cáo và xử lý (chỉ đếm các reports có status = Resolved, tức là đã được xử lý với WARNING hoặc BLOCK, không tính DISMISS)
      - Nếu user đã bị báo cáo 3 lần và admin cố gắng sử dụng action `WARNING`, hệ thống sẽ trả về lỗi và bắt buộc phải sử dụng action `BLOCK`
    - **Thông tin liên hệ trong email**: 
      - Tất cả các email BLOCK và REJECT đều bao gồm thông tin liên hệ để user/author có thể khiếu nại nếu bị oan hoặc bị spam báo cáo
      - Thông tin bao gồm: Link khiếu nại, Email hỗ trợ (support@artauction.com), Hotline (1900-xxxx)
- `DELETE /api/admin/reports/xoa/{reportId}`
- Response chuẩn `AdminReportApiResponse`.

---

## 10. Dashboard

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
            "avtArtwork": "...",
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

## 11. Thống kê theo khoảng thời gian (Statistics)

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

## 12. Ghi chú cho Frontend

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

