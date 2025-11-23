# HƯỚNG DẪN TEST REPORT API

## 📋 Tổng quan

Hệ thống Report API cho phép người dùng tố cáo:
- **User** (Người dùng)
- **Artwork** (Tác phẩm)
- **Auction Room** (Phòng đấu giá)
- **AI Artwork** (Kết quả AI không chính xác)

### Cấu trúc dữ liệu:
- **Entity Type**: `int` (1=User, 2=Artwork, 3=Auction Room, 4=AI Artwork)
- **Report Type**: `String` (ví dụ: "Spam", "Fake Identity", "Copyright Violation")
- **Các field khác**: `String` (reason, imageUrl, adminNote, etc.)

---

## 🔐 Authentication

Tất cả các endpoint đều yêu cầu **JWT Token** trong header:
```
Authorization: Bearer {your_jwt_token}
```

---

## 📝 API Endpoints

### 1. Tố cáo User
**POST** `/api/reports/user`

#### Request:
- **Content-Type**: `multipart/form-data`
- **Headers**:
    - `Authorization: Bearer {token}`

#### Form Data:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `reportType` | String | ✅ Yes | Loại tố cáo (xem danh sách bên dưới) |
| `reportedEntityId` | String | ✅ Yes | ID của user bị tố cáo |
| `reason` | String | ⚠️ Optional* | Mô tả chi tiết |
| `image` | File | ⚠️ Optional* | Ảnh chứng minh |

\* Phải có ít nhất `reason` HOẶC `image`

#### Report Types cho User:
- `"Fake Identity"` - Giả mạo danh tính
- `"Suspicious Activity"` - Hoạt động bất thường
- `"Scam / Fraud"` - Lừa đảo / gian lận
- `"Harassment / Abusive Behavior"` - Quấy rối, xúc phạm
- `"Policy Violation"` - Vi phạm quy định nền tảng
- `"Spam / Unwanted Ads"` - Spam hoặc quảng cáo không mong muốn
- `"Unauthorized Access"` - Tài khoản bị hack / dùng trái phép
- `"Other"` - Khác

#### Ví dụ trong Postman:

**Tab Body → form-data:**
```
Key                    | Type    | Value
-----------------------|---------|----------------------------------
reportType             | Text    | Spam / Unwanted Ads
reportedEntityId       | Text    | USR-1234567890
reason                 | Text    | User này spam quá nhiều trong chat
image                  | File    | [Chọn file ảnh]
```

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### Response thành công (200 OK):
```json
{
  "id": "RP-1234567890",
  "entityType": 1,
  "entityTypeName": "User",
  "reportType": "Spam / Unwanted Ads",
  "reportedEntityId": "USR-1234567890",
  "reporterId": "USR-9876543210",
  "reason": "User này spam quá nhiều trong chat",
  "imageUrl": "https://res.cloudinary.com/.../reports/RP-1234567890/evidence",
  "status": 0,
  "statusName": "Chờ xử lý",
  "adminNote": null,
  "createdAt": "2025-11-23T15:30:00",
  "updatedAt": "2025-11-23T15:30:00",
  "resolvedAt": null,
  "message": "Tố cáo đã được gửi thành công. Chúng tôi sẽ xem xét trong thời gian sớm nhất."
}
```

---

### 2. Tố cáo Artwork
**POST** `/api/reports/artwork`

#### Request:
- **Content-Type**: `multipart/form-data`
- **Headers**:
    - `Authorization: Bearer {token}`

#### Form Data:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `reportType` | String | ✅ Yes | Loại tố cáo (xem danh sách bên dưới) |
| `reportedEntityId` | String | ✅ Yes | ID của artwork bị tố cáo |
| `reason` | String | ⚠️ Optional* | Mô tả chi tiết |
| `image` | File | ⚠️ Optional* | Ảnh chứng minh |

\* Phải có ít nhất `reason` HOẶC `image`

#### Report Types cho Artwork:
- `"Fake Artwork"` - Tác phẩm giả mạo
- `"Copyright Violation"` - Vi phạm bản quyền
- `"Wrong / Misleading Information"` - Thông tin mô tả sai sự thật
- `"Inappropriate Content"` - Nội dung không phù hợp
- `"Restricted / Sensitive Artwork"` - Tác phẩm bị cấm / nhạy cảm
- `"Manipulated / Misleading Images"` - Ảnh che giấu/ chỉnh sửa gian dối

#### Ví dụ trong Postman:

**Tab Body → form-data:**
```
Key                    | Type    | Value
-----------------------|---------|----------------------------------
reportType             | Text    | Copyright Violation
reportedEntityId       | Text    | ART-1234567890
reason                 | Text    | Tác phẩm này vi phạm bản quyền của tôi
image                  | File    | [Chọn file ảnh chứng minh]
```

#### Response thành công (200 OK):
```json
{
  "id": "RP-1234567891",
  "entityType": 2,
  "entityTypeName": "Artwork",
  "reportType": "Copyright Violation",
  "reportedEntityId": "ART-1234567890",
  "reporterId": "USR-9876543210",
  "reason": "Tác phẩm này vi phạm bản quyền của tôi",
  "imageUrl": "https://res.cloudinary.com/.../reports/RP-1234567891/evidence",
  "status": 0,
  "statusName": "Chờ xử lý",
  "adminNote": null,
  "createdAt": "2025-11-23T15:35:00",
  "updatedAt": "2025-11-23T15:35:00",
  "resolvedAt": null,
  "message": "Tố cáo đã được gửi thành công. Chúng tôi sẽ xem xét trong thời gian sớm nhất."
}
```

---

### 3. Tố cáo Auction Room
**POST** `/api/reports/auction-room`

#### Request:
- **Content-Type**: `multipart/form-data`
- **Headers**:
    - `Authorization: Bearer {token}`

#### Form Data:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `reportType` | String | ✅ Yes | Loại tố cáo (xem danh sách bên dưới) |
| `reportedEntityId` | String | ✅ Yes | ID của auction room bị tố cáo |
| `reason` | String | ⚠️ Optional* | Mô tả chi tiết |
| `image` | File | ⚠️ Optional* | Ảnh chứng minh |

\* Phải có ít nhất `reason` HOẶC `image`

#### Report Types cho Auction Room:
- `"Fraudulent Bidding / Fake Bids"` - Đưa giá ảo / phá giá
- `"Host Rule Violation"` - Chủ phòng vi phạm quy định
- `"Unfair Behavior"` - Hành vi không công bằng
- `"System / Technical Error"` - Phòng đấu giá có lỗi kỹ thuật
- `"Misleading Room Information"` - Thông tin phòng không chính xác
- `"Disruptive Participant"` - Có người phá hoại phiên đấu giá
- `"Unusual Rule Changes"` - Thời gian, quy tắc đấu giá bị thay đổi bất thường

#### Ví dụ trong Postman:

**Tab Body → form-data:**
```
Key                    | Type    | Value
-----------------------|---------|----------------------------------
reportType             | Text    | Fraudulent Bidding / Fake Bids
reportedEntityId       | Text    | AR-1234567890
reason                 | Text    | Có người đưa giá ảo để đẩy giá lên cao
image                  | File    | [Chọn file ảnh screenshot]
```

#### Response thành công (200 OK):
```json
{
  "id": "RP-1234567892",
  "entityType": 3,
  "entityTypeName": "Auction Room",
  "reportType": "Fraudulent Bidding / Fake Bids",
  "reportedEntityId": "AR-1234567890",
  "reporterId": "USR-9876543210",
  "reason": "Có người đưa giá ảo để đẩy giá lên cao",
  "imageUrl": "https://res.cloudinary.com/.../reports/RP-1234567892/evidence",
  "status": 0,
  "statusName": "Chờ xử lý",
  "adminNote": null,
  "createdAt": "2025-11-23T15:40:00",
  "updatedAt": "2025-11-23T15:40:00",
  "resolvedAt": null,
  "message": "Tố cáo đã được gửi thành công. Chúng tôi sẽ xem xét trong thời gian sớm nhất."
}
```

---

### 4. Tố cáo AI Artwork (Kết quả AI không chính xác)
**POST** `/api/reports/ai-artwork`

#### Request:
- **Content-Type**: `multipart/form-data`
- **Headers**:
    - `Authorization: Bearer {token}`

#### Form Data:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `reason` | String | ✅ Yes | Mô tả chi tiết (bắt buộc) |
| `image` | File | ✅ Yes | Ảnh chứng minh (bắt buộc) |

**Lưu ý**:
- Không cần truyền `reportedEntityId` (ID tranh)
- `reportType` tự động = `"Inaccurate AI Result"` (không cần gửi)
- `entityType` tự động = `4` (AI Artwork)

#### Ví dụ trong Postman:

**Tab Body → form-data:**
```
Key                    | Type    | Value
-----------------------|---------|----------------------------------
reason                 | Text    | Kết quả AI phân tích sai, tác phẩm này không phải giả
image                  | File    | [Chọn file ảnh chứng minh]
```

#### Response thành công (200 OK):
```json
{
  "id": "RP-1234567893",
  "entityType": 4,
  "entityTypeName": "AI Artwork",
  "reportType": "Inaccurate AI Result",
  "reportedEntityId": null,
  "reporterId": "USR-9876543210",
  "reason": "Kết quả AI phân tích sai, tác phẩm này không phải giả",
  "imageUrl": "https://res.cloudinary.com/.../reports/RP-1234567893/evidence",
  "status": 0,
  "statusName": "Chờ xử lý",
  "adminNote": null,
  "createdAt": "2025-11-23T15:45:00",
  "updatedAt": "2025-11-23T15:45:00",
  "resolvedAt": null,
  "message": "Tố cáo đã được gửi thành công. Chúng tôi sẽ xem xét trong thời gian sớm nhất."
}
```

**Lưu ý**: `reportedEntityId` sẽ là `null` vì không cần truyền ID tranh.

---

### 5. Lấy danh sách tố cáo của user hiện tại
**GET** `/api/reports/my-reports`

#### Request:
- **Headers**:
    - `Authorization: Bearer {token}`

#### Response thành công (200 OK):
```json
[
  {
    "id": "RP-1234567890",
    "entityType": 1,
    "entityTypeName": "User",
    "reportType": "Spam / Unwanted Ads",
    "reportedEntityId": "USR-1234567890",
    "reporterId": "USR-9876543210",
    "reason": "User này spam quá nhiều",
    "imageUrl": "https://res.cloudinary.com/.../evidence",
    "status": 0,
    "statusName": "Chờ xử lý",
    "adminNote": null,
    "createdAt": "2025-11-23T15:30:00",
    "updatedAt": "2025-11-23T15:30:00",
    "resolvedAt": null,
    "message": null
  },
  {
    "id": "RP-1234567891",
    "entityType": 2,
    "entityTypeName": "Artwork",
    "reportType": "Copyright Violation",
    "reportedEntityId": "ART-1234567890",
    "reporterId": "USR-9876543210",
    "reason": "Vi phạm bản quyền",
    "imageUrl": null,
    "status": 1,
    "statusName": "Đang xử lý",
    "adminNote": "Đang kiểm tra",
    "createdAt": "2025-11-23T15:35:00",
    "updatedAt": "2025-11-23T16:00:00",
    "resolvedAt": null,
    "message": null
  }
]
```

---

## ⚠️ Validation Rules

### 1. Report Type phải hợp lệ
- User: Chỉ chấp nhận 8 loại report types đã định nghĩa
- Artwork: Chỉ chấp nhận 6 loại report types đã định nghĩa
- Auction Room: Chỉ chấp nhận 7 loại report types đã định nghĩa
- AI Artwork: Tự động = "Inaccurate AI Result"

**Lỗi nếu không hợp lệ:**
```json
{
  "timestamp": "2025-11-23T15:50:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Loại tố cáo không hợp lệ cho User",
  "path": "/api/reports/user"
}
```

### 2. Validation reason và image
**Đối với AI Artwork**: Cả `reason` và `image` đều bắt buộc

**Lỗi nếu thiếu reason:**
```json
{
  "timestamp": "2025-11-23T15:50:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Mô tả là bắt buộc cho tố cáo AI",
  "path": "/api/reports/ai-artwork"
}
```

**Lỗi nếu thiếu image:**
```json
{
  "timestamp": "2025-11-23T15:50:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Ảnh chứng minh là bắt buộc cho tố cáo AI",
  "path": "/api/reports/ai-artwork"
}
```

**Đối với các loại khác**: Phải có ít nhất `reason` HOẶC `image`

**Lỗi nếu thiếu cả hai:**
```json
{
  "timestamp": "2025-11-23T15:50:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Phải có ít nhất mô tả hoặc ảnh chứng minh",
  "path": "/api/reports/user"
}
```

### 3. Entity được tố cáo phải tồn tại (trừ AI Artwork)
**Lưu ý**: AI Artwork không cần `reportedEntityId`, nên không cần validate này.

**Lỗi nếu entity không tồn tại:**
```json
{
  "timestamp": "2025-11-23T15:50:00",
  "status": 404,
  "error": "Not Found",
  "message": "User không tồn tại",
  "path": "/api/reports/user"
}
```

### 4. Không được tố cáo chính mình (chỉ áp dụng cho User)
**Lỗi nếu tố cáo chính mình:**
```json
{
  "timestamp": "2025-11-23T15:50:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Không thể tố cáo chính mình",
  "path": "/api/reports/user"
}
```

### 5. Ảnh phải hợp lệ
- Chỉ chấp nhận file ảnh (`image/*`)
- Kích thước tối đa: 10MB

**Lỗi nếu ảnh không hợp lệ:**
```json
{
  "timestamp": "2025-11-23T15:50:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Chỉ cho phép upload image/*",
  "path": "/api/reports/user"
}
```

---

## 📊 Status Codes

| Status | Mô tả |
|--------|-------|
| `0` | Chờ xử lý (Pending) |
| `1` | Đang xử lý (Processing) |
| `2` | Đã xử lý (Resolved) |
| `3` | Đã từ chối (Rejected) |

---

## 🧪 Test Cases

### Test Case 1: Tố cáo User với reason và image
```
POST /api/reports/user
reportType: "Spam / Unwanted Ads"
reportedEntityId: "USR-1234567890"
reason: "User này spam quá nhiều"
image: [file.jpg]
```
**Expected**: 200 OK với report mới được tạo

### Test Case 2: Tố cáo Artwork chỉ với reason (không có image)
```
POST /api/reports/artwork
reportType: "Copyright Violation"
reportedEntityId: "ART-1234567890"
reason: "Vi phạm bản quyền"
```
**Expected**: 200 OK với report mới được tạo (imageUrl = null)

### Test Case 3: Tố cáo chỉ với image (không có reason) - Chỉ áp dụng cho User/Artwork/Room
```
POST /api/reports/artwork
reportType: "Fake Artwork"
reportedEntityId: "ART-1234567890"
image: [file.jpg]
```
**Expected**: 200 OK với report mới được tạo (reason = null)

### Test Case 3b: Tố cáo AI Artwork (cả reason và image đều bắt buộc)
```
POST /api/reports/ai-artwork
reason: "Kết quả AI phân tích sai"
image: [file.jpg]
```
**Expected**: 200 OK với report mới được tạo (reportedEntityId = null)

### Test Case 4: Report type không hợp lệ
```
POST /api/reports/user
reportType: "Invalid Type"
reportedEntityId: "USR-1234567890"
reason: "Test"
```
**Expected**: 400 Bad Request với message "Loại tố cáo không hợp lệ cho User"

### Test Case 5: Thiếu cả reason và image (cho User/Artwork/Room)
```
POST /api/reports/user
reportType: "Spam / Unwanted Ads"
reportedEntityId: "USR-1234567890"
```
**Expected**: 400 Bad Request với message "Phải có ít nhất mô tả hoặc ảnh chứng minh"

### Test Case 5b: Thiếu reason cho AI Artwork
```
POST /api/reports/ai-artwork
image: [file.jpg]
```
**Expected**: 400 Bad Request với message "Mô tả là bắt buộc cho tố cáo AI"

### Test Case 5c: Thiếu image cho AI Artwork
```
POST /api/reports/ai-artwork
reason: "Kết quả AI sai"
```
**Expected**: 400 Bad Request với message "Ảnh chứng minh là bắt buộc cho tố cáo AI"

### Test Case 6: Entity không tồn tại (không áp dụng cho AI Artwork)
```
POST /api/reports/user
reportType: "Spam / Unwanted Ads"
reportedEntityId: "USR-NOTEXIST"
reason: "Test"
```
**Expected**: 404 Not Found với message "User không tồn tại"

**Lưu ý**: AI Artwork không cần `reportedEntityId`, nên không có test case này.

### Test Case 7: Tố cáo chính mình
```
POST /api/reports/user
reportType: "Spam / Unwanted Ads"
reportedEntityId: {current_user_id}
reason: "Test"
```
**Expected**: 400 Bad Request với message "Không thể tố cáo chính mình"

### Test Case 8: Lấy danh sách reports của user
```
GET /api/reports/my-reports
```
**Expected**: 200 OK với danh sách reports của user hiện tại

---

## 💡 Tips

1. **Lưu request**: Click **Save** trong Postman để lưu các request và dùng lại sau
2. **Tạo Collection**: Tạo collection "Report API" để quản lý tất cả endpoints
3. **Sử dụng Variables**: Tạo variable `base_url = http://localhost:8081` và `token = {your_token}` để dễ thay đổi
4. **Test với các loại ảnh khác nhau**: JPG, PNG, GIF để đảm bảo validation hoạt động đúng
5. **Kiểm tra ảnh trên Cloudinary**: Sau khi upload thành công, kiểm tra ảnh có được lưu đúng folder `auctionaa/reports/{reportId}/evidence` không

---

## 📁 Ảnh được lưu ở đâu?

Ảnh chứng minh được upload lên **Cloudinary** với cấu trúc:
```
auctionaa/reports/{reportId}/evidence
```

Ví dụ:
- Report ID: `RP-1234567890`
- Folder: `auctionaa/reports/RP-1234567890/evidence`
- URL: `https://res.cloudinary.com/{cloud_name}/image/upload/v1234567890/auctionaa/reports/RP-1234567890/evidence.jpg`

---

## 🔍 Troubleshooting

### Lỗi: "Required request parameter 'reportType' is not present"
- **Nguyên nhân**: Thiếu field `reportType` trong form-data
- **Giải pháp**: Thêm field `reportType` với giá trị hợp lệ

### Lỗi: "Content-Type not supported"
- **Nguyên nhân**: Chọn sai Content-Type
- **Giải pháp**: Chọn `multipart/form-data` trong Postman

### Lỗi: "401 Unauthorized"
- **Nguyên nhân**: Token không hợp lệ hoặc hết hạn
- **Giải pháp**: Đăng nhập lại để lấy token mới

### Lỗi: "403 Forbidden"
- **Nguyên nhân**: Endpoint bị chặn bởi SecurityConfig
- **Giải pháp**: Kiểm tra SecurityConfig đã cho phép `/api/reports/**` chưa

### Response luôn trả về "Không tìm thấy" khi GET my-reports
- **Nguyên nhân**: User chưa có report nào
- **Giải pháp**: Tạo report mới trước, sau đó test GET

---

**Happy Testing! 🚀**

