# HƯỚNG DẪN TEST POSTMAN - TỪNG BƯỚC CHI TIẾT

---

## PHẦN 1: CÀI ĐẶT VÀ CHUẨN BỊ

### Bước 1: Kiểm tra Server đang chạy

1. Mở terminal/command prompt
2. Chạy Spring Boot application
3. Đợi đến khi thấy dòng: `Started AuctionBackendApplication`
4. Server đang chạy trên: `http://localhost:8081`

### Bước 2: Mở Postman

1. Mở ứng dụng Postman trên máy tính
2. Nếu chưa có, download tại: https://www.postman.com/downloads/

### Bước 3: Import Collection

1. Trong Postman, click nút **Import** (góc trên bên trái)
2. Chọn tab **File**
3. Click **Upload Files**
4. Chọn file `postman_collection_Auction_Search_API.json`
5. Click **Import**
6. Collection "Auction Search API - Test Collection" sẽ xuất hiện ở sidebar bên trái

---

## PHẦN 2: TEST AUCTION ROOM SEARCH

### Test 1: Search by ID (Tìm theo ID)

**Mục đích**: Tìm một auction room cụ thể theo ID

**Các bước**:

1. Trong Postman, mở folder **"Auction Room Search"**
2. Click vào request **"Search by ID"**
3. Kiểm tra Method: Phải là **POST** (không phải GET)
4. Kiểm tra URL:
   ```
   http://localhost:8081/api/auctionroom/search
   ```
5. **QUAN TRỌNG**:
    - Vào tab **Body**
    - Chọn **raw** và **JSON** format
    - Nhập JSON body:
   ```json
   {
     "id": "ACR-abc123"
   }
   ```
    - Thay `ACR-abc123` bằng ID thật trong database của bạn
6. Click nút **Send** (màu xanh, góc trên bên phải)
7. Xem kết quả ở tab **Body** bên dưới

**Kết quả mong đợi**:

- **200 OK**: Trả về 1 object auction room
- **200 OK với []**: Không tìm thấy (ID không tồn tại)

**Ví dụ Response thành công**:

```json
[
  {
    "id": "ACR-abc123",
    "roomName": "Phòng đấu giá Modern Art",
    "type": "Modern",
    "status": 1,
    "createdAt": "2024-01-15T10:00:00"
  }
]
```

---

### Test 2: Search by Name (Tìm theo tên phòng)

**Mục đích**: Tìm các auction room có tên chứa từ khóa

**Các bước**:

1. Click vào request **"Search by Name"**
2. Đảm bảo Method là **POST**
3. URL:
   ```
   http://localhost:8081/api/auctionroom/search
   ```
4. Vào tab **Body**
5. Chọn **raw** và **JSON** format
6. Nhập JSON body:
   ```json
   {
     "name": "phòng"
   }
   ```
    - Sửa giá trị `name` thành từ khóa bạn muốn tìm (ví dụ: `Modern`, `Art`, `phòng`)
7. Click **Send**

**Kết quả mong đợi**:

- Trả về danh sách các room có tên chứa từ khóa
- Case-insensitive: `phòng` = `Phòng` = `PHÒNG`

**Ví dụ**: Tìm `name=Modern` sẽ trả về tất cả room có tên chứa "Modern"

---

### Test 3: Filter by Type (Lọc theo thể loại)

**Mục đích**: Lọc các auction room theo thể loại

**Các bước**:

1. Click vào request **"Filter by Type"**
2. Đảm bảo Method là **POST**
3. URL:
   ```
   http://localhost:8081/api/auctionroom/search
   ```
4. Vào tab **Body**
5. Chọn **raw** và **JSON** format
6. Nhập JSON body:
   ```json
   {
     "type": "Modern"
   }
   ```
    - Sửa giá trị `type` thành thể loại bạn muốn (ví dụ: `Modern`, `Classic`, `Contemporary`)
7. Click **Send**

**Kết quả mong đợi**:

- Trả về tất cả room có `type` = giá trị bạn nhập (exact match)

---

### Test 4: Filter by Date Range (Lọc theo khoảng ngày)

**Mục đích**: Lọc các auction room được tạo trong khoảng thời gian

**Các bước**:

1. Click vào request **"Filter by Date Range"**
2. Đảm bảo Method là **POST**
3. URL:
   ```
   http://localhost:8081/api/auctionroom/search
   ```
4. Vào tab **Body**
5. Chọn **raw** và **JSON** format
6. Nhập JSON body:
   ```json
   {
     "dateFrom": "2024-01-01",
     "dateTo": "2024-12-31"
   }
   ```
    - Sửa các giá trị:
        - `dateFrom`: Ngày bắt đầu (format: `yyyy-MM-dd`, ví dụ: `2024-01-01`)
        - `dateTo`: Ngày kết thúc (format: `yyyy-MM-dd`, ví dụ: `2024-12-31`)
7. Click **Send**

**Lưu ý**:

- Date format PHẢI là `yyyy-MM-dd` (không phải `dd/MM/yyyy`)
- Có thể chỉ dùng `dateFrom` hoặc chỉ `dateTo`
- Nếu không có date, sẽ lấy tất cả

---

### Test 5: Combined Search (Kết hợp nhiều điều kiện)

**Mục đích**: Tìm room thỏa mãn NHIỀU điều kiện cùng lúc

**Các bước**:

1. Click vào request **"Combined Search (Name + Type + Date)"**
2. Đảm bảo Method là **POST**
3. URL:
   ```
   http://localhost:8081/api/auctionroom/search
   ```
4. Vào tab **Body**
5. Chọn **raw** và **JSON** format
6. Nhập JSON body:
   ```json
   {
     "name": "phòng",
     "type": "Modern",
     "dateFrom": "2024-01-01"
   }
   ```
    - Sửa các giá trị:
        - `name`: Tên phòng
        - `type`: Thể loại
        - `dateFrom`: Từ ngày
7. Click **Send**

**Kết quả mong đợi**:

- Trả về các room thỏa mãn TẤT CẢ điều kiện (AND logic)
- Ví dụ: Tên chứa "phòng" VÀ type = "Modern" VÀ tạo từ 2024-01-01

---

### Test 6: Get All (Lấy tất cả)

**Mục đích**: Lấy tất cả auction room (không filter)

**Các bước**:

1. Click vào request **"Get All (No Filters)"**
2. Đảm bảo Method là **POST**
3. URL:
   ```
   http://localhost:8081/api/auctionroom/search
   ```
4. Vào tab **Body**
5. Chọn **raw** và **JSON** format
6. Nhập JSON body rỗng:
   ```json
   {}
   ```
7. Click **Send**

**Kết quả mong đợi**:

- Trả về tất cả auction room trong database

---

## PHẦN 3: TEST ARTWORK SEARCH

### Test 1: Search by ID

**Các bước**:

1. Mở folder **"Artwork Search"**
2. Click **"Search by ID"**
3. Đảm bảo Method là **POST**
4. URL:
   ```
   http://localhost:8081/api/artwork/search
   ```
5. Vào tab **Body**, chọn **raw** và **JSON**
6. Nhập JSON body:
   ```json
   {
     "id": "Aw-12345"
   }
   ```
    - Sửa `id` thành ID artwork thật trong database
7. Click **Send**

**Kết quả**: Trả về 1 artwork object

---

### Test 2: Search by Title

**Các bước**:

1. Click **"Search by Title"**
2. Đảm bảo Method là **POST**
3. URL:
   ```
   http://localhost:8081/api/artwork/search
   ```
4. Vào tab **Body**, chọn **raw** và **JSON**
5. Nhập JSON body:
   ```json
   {
     "name": "Monet"
   }
   ```
    - Sửa `name` thành title bạn muốn tìm (ví dụ: `Monet`, `Van Gogh`, `Picasso`)
6. Click **Send**

**Kết quả**: Trả về tất cả artwork có title chứa từ khóa

---

### Test 3: Filter by Genre

**Các bước**:

1. Click **"Filter by Genre"**
2. Đảm bảo Method là **POST**
3. URL:
   ```
   http://localhost:8081/api/artwork/search
   ```
4. Vào tab **Body**, chọn **raw** và **JSON**
5. Nhập JSON body:
   ```json
   {
     "type": "Impressionism"
   }
   ```
    - Sửa `type` thành genre bạn muốn (ví dụ: `Impressionism`, `Realism`, `Abstract`)
6. Click **Send**

**Kết quả**: Trả về tất cả artwork có `paintingGenre` = giá trị bạn nhập

---

### Test 4: Combined Search

**Các bước**:

1. Click **"Combined Search"**
2. Đảm bảo Method là **POST**
3. URL:
   ```
   http://localhost:8081/api/artwork/search
   ```
4. Vào tab **Body**, chọn **raw** và **JSON**
5. Nhập JSON body:
   ```json
   {
     "name": "Monet",
     "type": "Impressionism",
     "dateFrom": "2024-01-01"
   }
   ```
    - Sửa các giá trị:
        - `name`: Title artwork
        - `type`: Genre
        - `dateFrom`: Từ ngày
6. Click **Send**

**Kết quả**: Trả về artwork thỏa mãn TẤT CẢ điều kiện

---

## PHẦN 4: TEST INVOICE SEARCH

### Test 1: Search by ID

**Các bước**:

1. Mở folder **"Invoice Search"**
2. Click **"Search by ID"**
3. Đảm bảo Method là **POST**
4. URL:
   ```
   http://localhost:8081/api/invoice/search
   ```
5. Vào tab **Body**, chọn **raw** và **JSON**
6. Nhập JSON body:
   ```json
   {
     "id": "IV-12345"
   }
   ```
    - Sửa `id` thành ID invoice thật
7. Click **Send**

---

### Test 2: Search by Artwork Title or Room Name

**Các bước**:

1. Click **"Search by Artwork Title or Room Name"**
2. Đảm bảo Method là **POST**
3. URL:
   ```
   http://localhost:8081/api/invoice/search
   ```
4. Vào tab **Body**, chọn **raw** và **JSON**
5. Nhập JSON body:
   ```json
   {
     "name": "artwork"
   }
   ```
    - Sửa `name` thành từ khóa (tìm trong cả `artworkTitle` và `roomName`)
6. Click **Send**

**Lưu ý**: API sẽ tìm trong CẢ HAI field: `artworkTitle` và `roomName`

---

### Test 3: Filter by Date Range

**Các bước**:

1. Click **"Filter by Date Range"**
2. Đảm bảo Method là **POST**
3. URL:
   ```
   http://localhost:8081/api/invoice/search
   ```
4. Vào tab **Body**, chọn **raw** và **JSON**
5. Nhập JSON body:
   ```json
   {
     "dateFrom": "2024-01-01",
     "dateTo": "2024-12-31"
   }
   ```
    - Sửa `dateFrom` và `dateTo`
6. Click **Send**

**Kết quả**: Trả về invoice được tạo trong khoảng thời gian

---

## PHẦN 5: TEST WALLET SEARCH

### Test 1: Search by ID

**Các bước**:

1. Mở folder **"Wallet Search"**
2. Click **"Search by ID"**
3. Đảm bảo Method là **POST**
4. URL:
   ```
   http://localhost:8081/api/wallets/search
   ```
5. Vào tab **Body**, chọn **raw** và **JSON**
6. Nhập JSON body:
   ```json
   {
     "id": "WL-12345"
   }
   ```
    - Sửa `id` thành ID wallet thật
7. Click **Send**

**Lưu ý**: Wallet không có field "name" và "type", chỉ tìm được theo ID và ngày

---

### Test 2: Filter by Date Range

**Các bước**:

1. Click **"Filter by Date Range"**
2. Đảm bảo Method là **POST**
3. URL:
   ```
   http://localhost:8081/api/wallets/search
   ```
4. Vào tab **Body**, chọn **raw** và **JSON**
5. Nhập JSON body:
   ```json
   {
     "dateFrom": "2024-01-01",
     "dateTo": "2024-12-31"
   }
   ```
    - Sửa `dateFrom` và `dateTo`
6. Click **Send**

**Kết quả**: Trả về wallet được tạo trong khoảng thời gian

---

## PHẦN 6: TEST HISTORY (AUCTION SESSION) SEARCH

### Test 1: Search by ID

**Các bước**:

1. Mở folder **"History (AuctionSession) Search"**
2. Click **"Search by ID"**
3. Đảm bảo Method là **POST**
4. URL:
   ```
   http://localhost:8081/api/history/search
   ```
5. Vào tab **Body**, chọn **raw** và **JSON**
6. Nhập JSON body:
   ```json
   {
     "id": "ATSS-12345"
   }
   ```
    - Sửa `id` thành ID session thật
7. Click **Send**

---

### Test 2: Filter by Type

**Các bước**:

1. Click **"Filter by Type"**
2. Đảm bảo Method là **POST**
3. URL:
   ```
   http://localhost:8081/api/history/search
   ```
4. Vào tab **Body**, chọn **raw** và **JSON**
5. Nhập JSON body:
   ```json
   {
     "type": "Modern"
   }
   ```
    - Sửa `type` thành thể loại bạn muốn
6. Click **Send**

**Kết quả**: Trả về session có `type` = giá trị bạn nhập

---

### Test 3: Filter by Date Range

**Các bước**:

1. Click **"Filter by Date Range"**
2. Đảm bảo Method là **POST**
3. URL:
   ```
   http://localhost:8081/api/history/search
   ```
4. Vào tab **Body**, chọn **raw** và **JSON**
5. Nhập JSON body:
   ```json
   {
     "dateFrom": "2024-01-01",
     "dateTo": "2024-12-31"
   }
   ```
    - Sửa `dateFrom` và `dateTo`
6. Click **Send**

**Lưu ý**: Lọc theo `startTime` của session (không phải `createdAt`)

---

### Test 4: Combined Search

**Các bước**:

1. Click **"Combined Search"**
2. Đảm bảo Method là **POST**
3. URL:
   ```
   http://localhost:8081/api/history/search
   ```
4. Vào tab **Body**, chọn **raw** và **JSON**
5. Nhập JSON body:
   ```json
   {
     "type": "Modern",
     "dateFrom": "2024-01-01",
     "dateTo": "2024-12-31"
   }
   ```
    - Sửa các giá trị
6. Click **Send**

**Kết quả**: Trả về session thỏa mãn TẤT CẢ điều kiện

---

## PHẦN 7: CÁCH ĐỌC VÀ PHÂN TÍCH RESPONSE

### Response thành công (200 OK)

**Cấu trúc**:

```json
[
  {
    "id": "...",
    "field1": "...",
    "field2": "..."
  },
  {
    "id": "...",
    "field1": "...",
    "field2": "..."
  }
]
```

**Giải thích**:

- Luôn là một **array** (mảng)
- Mỗi phần tử là một object
- Có thể có 0, 1, hoặc nhiều phần tử

---

### Response rỗng (200 OK với [])

```json
[]
```

**Ý nghĩa**:

- Không có lỗi
- Không tìm thấy records thỏa mãn điều kiện
- Đây là kết quả hợp lệ

**Cách xử lý**:

- Thử với điều kiện khác
- Kiểm tra dữ liệu trong database
- Bỏ filter để xem tất cả

---

### Response lỗi (400/500)

```json
{
  "timestamp": "2024-01-15T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid date format"
}
```

**Cách xử lý**:

- Kiểm tra format ngày: phải là `yyyy-MM-dd`
- Kiểm tra ID có đúng format không
- Kiểm tra server có đang chạy không

---

## PHẦN 8: TIPS VÀ THỦ THUẬT

### Tip 1: Sử dụng Variables trong Postman

1. Click vào collection name
2. Vào tab **Variables**
3. Thêm variable:
    - Name: `base_url`
    - Value: `http://localhost:8081`
4. Trong request, dùng `{{base_url}}` thay vì gõ lại URL

---

### Tip 2: Save Response để so sánh

1. Sau khi nhận response, click **Save Response**
2. Chọn **Save as Example**
3. Có thể xem lại sau

---

### Tip 3: Test với dữ liệu thật

1. Trước khi test, kiểm tra database có dữ liệu không
2. Lấy ID thật từ database
3. Test với ID đó

---

### Tip 4: Sử dụng Collection Runner

1. Click vào collection name
2. Click **Run** (góc trên bên phải)
3. Chọn các request muốn chạy
4. Click **Run Auction Search API**
5. Xem kết quả tổng hợp

---

## PHẦN 9: TROUBLESHOOTING (XỬ LÝ LỖI)

### Lỗi: "Could not get response"

**Nguyên nhân**: Server không chạy hoặc sai port

**Cách fix**:

1. Kiểm tra server có đang chạy không
2. Kiểm tra port 8081 có đúng không
3. Thử truy cập `http://localhost:8081` trên browser

---

### Lỗi: "404 Not Found"

**Nguyên nhân**: URL sai hoặc endpoint không tồn tại

**Cách fix**:

1. Kiểm tra URL path có đúng không
2. Kiểm tra server logs xem có error không
3. Đảm bảo endpoint đã được deploy

---

### Lỗi: "Invalid date format"

**Nguyên nhân**: Format ngày sai

**Cách fix**:

1. Đảm bảo format là `yyyy-MM-dd`
2. Ví dụ đúng: `2024-01-15`
3. Ví dụ sai: `15/01/2024`, `2024/01/15`

---

### Response luôn trả về []

**Nguyên nhân**: Không có dữ liệu hoặc điều kiện quá strict

**Cách fix**:

1. Test với "Get All" (gửi body rỗng `{}`) để xem có dữ liệu không
2. Nới lỏng điều kiện (bỏ một số field trong JSON body)
3. Kiểm tra database có dữ liệu không

---

## CHECKLIST TEST HOÀN CHỈNH

### Auction Room

- [ ] Test Search by ID
- [ ] Test Search by Name
- [ ] Test Filter by Type
- [ ] Test Filter by Date Range
- [ ] Test Combined Search
- [ ] Test Get All

### Artwork

- [ ] Test Search by ID
- [ ] Test Search by Title
- [ ] Test Filter by Genre
- [ ] Test Combined Search

### Invoice

- [ ] Test Search by ID
- [ ] Test Search by Name
- [ ] Test Filter by Date Range

### Wallet

- [ ] Test Search by ID
- [ ] Test Filter by Date Range

### History

- [ ] Test Search by ID
- [ ] Test Filter by Type
- [ ] Test Filter by Date Range
- [ ] Test Combined Search

---

## HOÀN THÀNH!

Nếu bạn đã test hết tất cả các case trên, bạn đã hoàn thành việc test toàn bộ API Search!

**Chúc bạn test thành công!**
