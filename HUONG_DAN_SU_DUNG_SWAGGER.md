# 📚 HƯỚNG DẪN SỬ DỤNG SWAGGER

## 🚀 Bước 1: Khởi động ứng dụng

1. Chạy ứng dụng Spring Boot như bình thường
2. Đợi ứng dụng khởi động hoàn tất (thường thấy dòng "Started AuctionBackendApplication")
3. Ứng dụng sẽ chạy tại: `http://localhost:8081`

---

## 🌐 Bước 2: Truy cập Swagger UI

Mở trình duyệt và truy cập một trong các địa chỉ sau:

- **Swagger UI chính**: http://localhost:8081/swagger-ui.html
- **Hoặc**: http://localhost:8081/swagger-ui/index.html
- **API Docs (JSON)**: http://localhost:8081/v3/api-docs
- **API Docs (YAML)**: http://localhost:8081/v3/api-docs.yaml

---

## 📖 Bước 3: Xem danh sách API

Sau khi truy cập Swagger UI, bạn sẽ thấy:

1. **Thông tin API** ở đầu trang:
   - Title: Art Auction Backend API
   - Version: 1.0.0
   - Description về các tính năng

2. **Nút "Authorize"** (khóa màu xanh) - dùng để xác thực JWT token

3. **Danh sách các API endpoints** được nhóm theo Controller:
   - User APIs
   - Artwork APIs
   - Auction Room APIs
   - Admin APIs
   - ... và nhiều hơn nữa

---

## 🔍 Bước 4: Xem chi tiết một API

1. **Tìm API bạn muốn xem** trong danh sách
2. **Click vào API endpoint** để mở rộng
3. Bạn sẽ thấy:
   - **Mô tả** của API
   - **HTTP Method** (GET, POST, PUT, DELETE...)
   - **Parameters** (nếu có)
   - **Request Body** (nếu có)
   - **Response** (các mã trạng thái và cấu trúc dữ liệu trả về)

---

## 🧪 Bước 5: Test API trực tiếp trên Swagger

### 5.1. Test API không cần authentication

1. **Click vào API endpoint** bạn muốn test
2. **Click nút "Try it out"** (màu xanh)
3. **Điền thông tin** vào các trường:
   - Parameters (nếu có)
   - Request body (nếu có)
4. **Click "Execute"** (màu xanh)
5. **Xem kết quả**:
   - Response code (200, 400, 500...)
   - Response body (dữ liệu trả về)
   - Response headers

### 5.2. Test API cần authentication (JWT Token)

#### Bước 1: Lấy JWT Token

**Cách 1: Lấy token cho User thường**
1. Trong Swagger UI, tìm API: `POST /api/auth/login`
2. Click vào API đó
3. Click "Try it out"
4. Điền thông tin:
   ```json
   {
     "email": "your-email@example.com",
     "password": "your-password"
   }
   ```
5. Click "Execute"
6. Copy token từ response (trong field "token")

**Cách 2: Lấy token cho Admin**
1. Tìm API: `POST /api/admin/auth/login`
2. Làm tương tự như trên với email/password của admin

#### Bước 2: Xác thực token trong Swagger

1. **Click nút "Authorize"** ở đầu trang Swagger UI (khóa màu xanh)
2. Một popup sẽ hiện ra với title "Available authorizations"
3. Tìm phần **"Bearer Authentication"**
4. **Nhập token** vào ô "Value":
   - Chỉ cần nhập token, KHÔNG cần gõ "Bearer"
   - Ví dụ: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
5. **Click "Authorize"**
6. **Click "Close"** để đóng popup
7. Bây giờ bạn có thể test các API cần authentication!

#### Bước 3: Test API đã xác thực

1. Tìm API cần authentication (thường có biểu tượng khóa 🔒)
2. Click vào API
3. Click "Try it out"
4. Điền thông tin cần thiết
5. Click "Execute"
6. Token sẽ tự động được thêm vào header `Authorization: Bearer {your-token}`

---

## 📝 Ví dụ cụ thể

### Ví dụ 1: Test API Login

1. Tìm: `POST /api/auth/login`
2. Click vào API
3. Click "Try it out"
4. Điền request body:
   ```json
   {
     "email": "user@example.com",
     "password": "password123"
   }
   ```
5. Click "Execute"
6. Xem response:
   ```json
   {
     "status": 1,
     "message": "Login successfully",
     "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
   }
   ```
7. Copy token để dùng cho các API khác

### Ví dụ 2: Test API lấy thông tin User (cần token)

1. **Trước tiên**: Xác thực token như hướng dẫn ở Bước 5.2
2. Tìm: `GET /api/user/info`
3. Click vào API
4. Click "Try it out"
5. Click "Execute" (không cần điền gì vì API này lấy token từ header)
6. Xem response với thông tin user

### Ví dụ 3: Test API tạo Artwork (cần token)

1. **Trước tiên**: Xác thực token
2. Tìm: `POST /api/artwork` (hoặc tương tự)
3. Click vào API
4. Click "Try it out"
5. Điền request body với thông tin artwork
6. Click "Execute"
7. Xem kết quả

---

## 🔑 Các tính năng hữu ích

### 1. Xem Schema của Request/Response

- Click vào **"Schema"** tab để xem cấu trúc dữ liệu
- Click vào **"Example Value"** để xem ví dụ dữ liệu
- Click vào **"Model"** để xem chi tiết các field

### 2. Copy cURL command

- Sau khi test API, Swagger sẽ hiển thị **cURL command**
- Bạn có thể copy và chạy trong terminal
- Rất hữu ích để test từ command line

### 3. Download API Documentation

- Truy cập: http://localhost:8081/v3/api-docs
- Bạn sẽ thấy JSON chứa toàn bộ API documentation
- Có thể import vào Postman hoặc các tool khác

---

## ⚠️ Lưu ý quan trọng

1. **Token hết hạn**: Nếu token hết hạn, bạn sẽ nhận lỗi 401 Unauthorized. Cần đăng nhập lại để lấy token mới.

2. **CORS**: Nếu gặp lỗi CORS khi test từ Swagger UI, kiểm tra lại cấu hình CORS trong `SecurityConfig.java`.

3. **Validation**: Swagger sẽ hiển thị các validation rules (required, min, max...) cho các field.

4. **Response Codes**: Mỗi API sẽ hiển thị các mã trạng thái có thể:
   - 200: Success
   - 400: Bad Request
   - 401: Unauthorized
   - 403: Forbidden
   - 404: Not Found
   - 500: Internal Server Error

---

## 🎯 Tips & Tricks

1. **Tìm kiếm API**: Sử dụng ô tìm kiếm ở đầu trang để tìm nhanh API theo tên

2. **Lọc theo tag**: Các API được nhóm theo tag (User, Artwork, Admin...), click vào tag để xem tất cả API trong nhóm đó

3. **Lưu token**: Sau khi authorize, token sẽ được lưu trong session của trình duyệt. Bạn không cần nhập lại cho đến khi refresh trang.

4. **Test nhiều API**: Bạn có thể test nhiều API liên tiếp mà không cần đăng nhập lại (nếu token còn hiệu lực)

---

## 🐛 Xử lý lỗi thường gặp

### Lỗi: "Failed to fetch"
- **Nguyên nhân**: Ứng dụng chưa chạy hoặc địa chỉ sai
- **Giải pháp**: Kiểm tra ứng dụng đã chạy tại `http://localhost:8081` chưa

### Lỗi: 401 Unauthorized
- **Nguyên nhân**: Token hết hạn hoặc chưa authorize
- **Giải pháp**: Đăng nhập lại và authorize token mới

### Lỗi: 403 Forbidden
- **Nguyên nhân**: Token không có quyền truy cập API này
- **Giải pháp**: Kiểm tra role/permission của user

### Lỗi: 404 Not Found
- **Nguyên nhân**: API endpoint không tồn tại hoặc đường dẫn sai
- **Giải pháp**: Kiểm tra lại đường dẫn API

---

## 📞 Hỗ trợ

Nếu gặp vấn đề, kiểm tra:
1. Ứng dụng đã chạy chưa?
2. Port 8081 có bị chiếm dụng không?
3. Token có còn hiệu lực không?
4. Có lỗi trong console của ứng dụng không?

---

**Chúc bạn sử dụng Swagger hiệu quả! 🎉**

