# Hướng dẫn Test API Admin bằng Postman

## Cấu hình cơ bản

**Base URL:** `http://localhost:8080` (hoặc port mà bạn đang chạy Spring Boot)

**Headers mặc định:**
- `Content-Type: application/json`
- `Accept: application/json`

---

## 1. POST /api/admin/them-user - Thêm người dùng mới

### Request
- **Method:** `POST`
- **URL:** `http://localhost:8080/api/admin/them-user`
- **Headers:**
  ```
  Content-Type: application/json
  ```
- **Body (JSON):**
  ```json
  {
    "username": "nguyenvana",
    "password": "password123",
    "email": "nguyenvana@example.com",
    "phonenumber": "0123456789",
    "cccd": "001234567890",
    "address": "123 Đường ABC, Quận 1, TP.HCM",
    "dateOfBirth": "1990-01-15",
    "gender": 0,
    "role": 1,
    "status": 1
  }
  ```

### Giải thích các trường:
- `username`: Tên đăng nhập (bắt buộc, unique)
- `password`: Mật khẩu (bắt buộc)
- `email`: Email (bắt buộc, unique)
- `phonenumber`: Số điện thoại
- `cccd`: Số CCCD/CMND
- `address`: Địa chỉ
- `dateOfBirth`: Ngày sinh (format: `YYYY-MM-DD`)
- `gender`: Giới tính
  - `0` = Nam
  - `1` = Nữ
  - `2` = Khác
- `role`: Vai trò
  - `1` = Người dùng thường
  - `2` = Buyer
  - `3` = Seller
- `status`: Trạng thái
  - `0` = Offline
  - `1` = Đang hoạt động
  - `2` = Đang bị chặn

### Response thành công (201 Created):
```json
"User created successfully with ID: U-1234567890"
```

### Response lỗi (400 Bad Request):
```json
"Email already exists"
```
hoặc
```json
"Username already exists"
```

---

## 2. GET /api/admin/lay-du-lieu-user - Lấy tất cả người dùng

### Request
- **Method:** `GET`
- **URL:** `http://localhost:8080/api/admin/lay-du-lieu-user`
- **Headers:** Không cần (hoặc `Accept: application/json`)

### Response thành công (200 OK):
```json
[
  {
    "id": "U-1234567890",
    "fullname": "nguyenvana",
    "email": "nguyenvana@example.com",
    "phonenumber": "0123456789",
    "gender": 0,
    "dateOfBirth": "1990-01-15",
    "address": "123 Đường ABC, Quận 1, TP.HCM",
    "cccd": "001234567890",
    "role": 1,
    "status": 1,
    "balance": 0.00,
    "createdAt": "2024-01-15T10:30:00"
  },
  {
    "id": "U-0987654321",
    "fullname": "tranthib",
    "email": "tranthib@example.com",
    "phonenumber": "0987654321",
    "gender": 1,
    "dateOfBirth": "1995-05-20",
    "address": "456 Đường XYZ, Quận 2, TP.HCM",
    "cccd": "009876543210",
    "role": 3,
    "status": 1,
    "balance": 500000.00,
    "createdAt": "2024-01-10T08:15:00"
  }
]
```

---

## 3. GET /api/admin/tim-kiem-user - Tìm kiếm người dùng

### Request
- **Method:** `GET`
- **URL:** `http://localhost:8080/api/admin/tim-kiem-user?q={searchTerm}`
- **Query Parameters:**
  - `q`: Từ khóa tìm kiếm (tùy chọn)
    - Tìm theo: ID, username, phonenumber, cccd
    - Không phân biệt hoa thường
    - Hỗ trợ regex

### Ví dụ:

#### Tìm kiếm theo username:
```
GET http://localhost:8080/api/admin/tim-kiem-user?q=nguyenvana
```

#### Tìm kiếm theo số điện thoại:
```
GET http://localhost:8080/api/admin/tim-kiem-user?q=0123456789
```

#### Tìm kiếm theo CCCD:
```
GET http://localhost:8080/api/admin/tim-kiem-user?q=001234567890
```

#### Tìm kiếm theo ID:
```
GET http://localhost:8080/api/admin/tim-kiem-user?q=U-1234567890
```

#### Không có query (trả về tất cả):
```
GET http://localhost:8080/api/admin/tim-kiem-user
```

### Response thành công (200 OK):
```json
[
  {
    "id": "U-1234567890",
    "fullname": "nguyenvana",
    "email": "nguyenvana@example.com",
    "phonenumber": "0123456789",
    "gender": 0,
    "dateOfBirth": "1990-01-15",
    "address": "123 Đường ABC, Quận 1, TP.HCM",
    "cccd": "001234567890",
    "role": 1,
    "status": 1,
    "balance": 0.00,
    "createdAt": "2024-01-15T10:30:00"
  }
]
```

---

## 4. GET /api/admin/thong-ke-user - Lấy thống kê người dùng

### Request
- **Method:** `GET`
- **URL:** `http://localhost:8080/api/admin/thong-ke-user`
- **Headers:** Không cần (hoặc `Accept: application/json`)

### Response thành công (200 OK):
```json
{
  "totalUsers": 150,
  "totalSellers": 25,
  "totalBlockedUsers": 5
}
```

### Giải thích:
- `totalUsers`: Tổng số người dùng trong hệ thống
- `totalSellers`: Tổng số người bán (role = 3)
- `totalBlockedUsers`: Tổng số người dùng bị khóa (status = 2)

---

## 5. PUT /api/admin/cap-nhat-user/{userId} - Cập nhật thông tin người dùng

### Request
- **Method:** `PUT`
- **URL:** `http://localhost:8080/api/admin/cap-nhat-user/{userId}`
  - Thay `{userId}` bằng ID thực tế của user (ví dụ: `U-1234567890`)
- **Headers:**
  ```
  Content-Type: application/json
  ```
- **Body (JSON):**
  ```json
  {
    "username": "nguyenvana_updated",
    "email": "nguyenvana_new@example.com",
    "phonenumber": "0987654321",
    "cccd": "001234567890",
    "address": "456 Đường XYZ, Quận 2, TP.HCM",
    "dateOfBirth": "1990-01-15",
    "gender": 0,
    "role": 2,
    "status": 1,
    "password": "newpassword123"
  }
  ```

### Giải thích các trường:
- Tất cả các trường đều **tùy chọn** (optional)
- Chỉ cập nhật các trường bạn muốn thay đổi
- `password`: Chỉ cập nhật nếu có giá trị, nếu không có thì giữ nguyên password cũ
- `email` và `username`: Nếu thay đổi, sẽ kiểm tra unique

### Response thành công (200 OK):
```json
{
  "id": "U-1234567890",
  "fullname": "nguyenvana_updated",
  "email": "nguyenvana_new@example.com",
  "phonenumber": "0987654321",
  "gender": 0,
  "dateOfBirth": "1990-01-15",
  "address": "456 Đường XYZ, Quận 2, TP.HCM",
  "cccd": "001234567890",
  "role": 2,
  "status": 1,
  "balance": 0.00,
  "createdAt": "2024-01-15T10:30:00"
}
```

### Response lỗi (404 Not Found):
```json
"User not found with ID: U-1234567890"
```

### Response lỗi (400 Bad Request):
```json
"Email already exists"
```
hoặc
```json
"Username already exists"
```

### Ví dụ update chỉ một số trường:
```json
{
  "phonenumber": "0987654321",
  "status": 2
}
```
→ Chỉ cập nhật số điện thoại và status, các trường khác giữ nguyên

---

## 6. DELETE /api/admin/xoa-user/{userId} - Xóa người dùng

### Request
- **Method:** `DELETE`
- **URL:** `http://localhost:8080/api/admin/xoa-user/{userId}`
  - Thay `{userId}` bằng ID thực tế của user (ví dụ: `U-1234567890`)
- **Headers:** Không cần

### Response thành công (200 OK):
```json
"User deleted successfully with ID: U-1234567890"
```

### Response lỗi (404 Not Found):
```json
"User not found with ID: U-1234567890"
```

### Lưu ý:
- Khi xóa user, hệ thống sẽ tự động xóa wallet liên quan
- Hành động này **không thể hoàn tác**, hãy cẩn thận khi xóa

---

## Các bước test trong Postman

### Bước 1: Tạo Collection mới
1. Mở Postman
2. Click **New** → **Collection**
3. Đặt tên: `Admin User Management APIs`

### Bước 2: Tạo Request cho từng API

#### Request 1: Thêm User
1. Click **Add Request** trong Collection
2. Đặt tên: `Add User`
3. Chọn method: **POST**
4. URL: `http://localhost:8080/api/admin/them-user`
5. Tab **Headers**: Thêm `Content-Type: application/json`
6. Tab **Body**: Chọn **raw** → **JSON**
7. Paste JSON body mẫu ở trên
8. Click **Send**

#### Request 2: Lấy tất cả Users
1. Click **Add Request**
2. Đặt tên: `Get All Users`
3. Method: **GET**
4. URL: `http://localhost:8080/api/admin/lay-du-lieu-user`
5. Click **Send**

#### Request 3: Tìm kiếm User
1. Click **Add Request**
2. Đặt tên: `Search Users`
3. Method: **GET**
4. URL: `http://localhost:8080/api/admin/tim-kiem-user`
5. Tab **Params**: Thêm key `q`, value `nguyenvana`
6. Click **Send**

#### Request 4: Thống kê
1. Click **Add Request**
2. Đặt tên: `Get Statistics`
3. Method: **GET**
4. URL: `http://localhost:8080/api/admin/thong-ke-user`
5. Click **Send**

#### Request 5: Cập nhật User
1. Click **Add Request**
2. Đặt tên: `Update User`
3. Method: **PUT**
4. URL: `http://localhost:8080/api/admin/cap-nhat-user/{userId}` (thay {userId} bằng ID thực tế)
5. Tab **Headers**: Thêm `Content-Type: application/json`
6. Tab **Body**: Chọn **raw** → **JSON**
7. Paste JSON body mẫu ở trên
8. Click **Send**

#### Request 6: Xóa User
1. Click **Add Request**
2. Đặt tên: `Delete User`
3. Method: **DELETE**
4. URL: `http://localhost:8080/api/admin/xoa-user/{userId}` (thay {userId} bằng ID thực tế)
5. Click **Send**

---

## Ví dụ test flow hoàn chỉnh

### Test Case 1: Thêm user mới và kiểm tra
1. **POST /api/admin/them-user** với body:
   ```json
   {
     "username": "testuser001",
     "password": "test123",
     "email": "testuser001@test.com",
     "phonenumber": "0901234567",
     "cccd": "123456789",
     "address": "Test Address",
     "dateOfBirth": "2000-01-01",
     "gender": 0,
     "role": 1,
     "status": 1
   }
   ```
   → Kỳ vọng: 201 Created với message thành công

2. **GET /api/admin/lay-du-lieu-user**
   → Kiểm tra user vừa tạo có trong danh sách

3. **GET /api/admin/tim-kiem-user?q=testuser001**
   → Kiểm tra tìm thấy user vừa tạo

### Test Case 2: Test validation
1. **POST /api/admin/them-user** với email đã tồn tại
   → Kỳ vọng: 400 Bad Request với message "Email already exists"

2. **POST /api/admin/them-user** với username đã tồn tại
   → Kỳ vọng: 400 Bad Request với message "Username already exists"

### Test Case 3: Test thống kê
1. **GET /api/admin/thong-ke-user**
   → Kiểm tra số liệu thống kê chính xác

### Test Case 4: Test update user
1. **GET /api/admin/lay-du-lieu-user**
   → Lấy ID của một user để test

2. **PUT /api/admin/cap-nhat-user/{userId}** với body:
   ```json
   {
     "phonenumber": "0999999999",
     "status": 1
   }
   ```
   → Kỳ vọng: 200 OK với thông tin user đã cập nhật

3. **GET /api/admin/lay-du-lieu-user**
   → Kiểm tra thông tin đã được cập nhật

### Test Case 5: Test delete user
1. **POST /api/admin/them-user** 
   → Tạo user mới để test xóa

2. **GET /api/admin/lay-du-lieu-user**
   → Lấy ID của user vừa tạo

3. **DELETE /api/admin/xoa-user/{userId}**
   → Kỳ vọng: 200 OK với message thành công

4. **GET /api/admin/lay-du-lieu-user**
   → Kiểm tra user đã bị xóa khỏi danh sách

5. **DELETE /api/admin/xoa-user/{userId}** (xóa lại)
   → Kỳ vọng: 404 Not Found vì user không còn tồn tại

---

## Lưu ý khi test

1. **Đảm bảo server đang chạy**: Spring Boot application phải đang chạy trên port 8080 (hoặc port bạn đã cấu hình)

2. **CORS**: Nếu gặp lỗi CORS, kiểm tra SecurityConfig đã cho phép origin của bạn

3. **Database**: Đảm bảo MongoDB đang chạy và kết nối được

4. **Format Date**: Ngày sinh phải theo format `YYYY-MM-DD`

5. **Unique fields**: Email và username phải unique, nếu test lại cần đổi giá trị

6. **Balance**: Khi tạo user mới, balance sẽ tự động là 0.00

---

## Troubleshooting

### Lỗi 404 Not Found
- Kiểm tra URL có đúng không
- Kiểm tra server có đang chạy không
- Kiểm tra SecurityConfig đã permitAll cho các endpoint chưa

### Lỗi 500 Internal Server Error
- Kiểm tra MongoDB connection
- Kiểm tra logs trong console của Spring Boot
- Kiểm tra các field required có đầy đủ không

### Lỗi CORS
- Kiểm tra SecurityConfig có cấu hình CORS đúng không
- Thêm header `Origin` trong Postman nếu cần

---

## Export Collection (Tùy chọn)

Sau khi tạo xong các request, bạn có thể:
1. Click vào Collection → **...** (3 chấm)
2. Chọn **Export**
3. Lưu file JSON để chia sẻ với team

