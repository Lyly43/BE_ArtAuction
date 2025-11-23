# Hướng dẫn Test Postman: Phiên Đấu Giá với Countdown và Auto-Extend

## Tổng quan tính năng

Tính năng này bao gồm:
1. **Tạo phòng đấu giá**: Mỗi phiên (session) có thể có thời lượng riêng (set cho từng phiên)
2. **Khởi động phiên mới**: Tạo phiên với thời gian bắt đầu và kết thúc theo `durationSeconds` của từng phiên
3. **Auto-extend khi đặt giá**: Nếu còn ≤ 60 giây, tự động cộng thêm 120 giây (không vượt quá thời lượng tối đa)
4. **Auto-close**: Tự động đóng phiên khi hết thời gian và khởi động phiên tiếp theo nếu có
5. **WebSocket realtime**: Gửi thông tin countdown và giá hiện tại cho frontend

### Lưu ý quan trọng về thời gian
- **Thời gian set theo TỪNG PHIÊN, không phải theo phòng**: Mỗi phiên (session) có thể có thời lượng khác nhau (5 phút, 10 phút, 15 phút, v.v.)
- **Thiết lập cho từng phiên**: Thời gian được set trong `durationMinutes` của từng session trong mảng `sessions` khi tạo phòng
- **Mỗi phiên độc lập**: Trong cùng một phòng, phiên 1 có thể 15 phút, phiên 2 có thể 10 phút, phiên 3 có thể 20 phút
- **Nếu không set**: Chỉ khi một phiên không có `durationMinutes` thì mới dùng mặc định 10 phút cho phiên đó

---

## Chuẩn bị

### 1. Đảm bảo Server đang chạy
```bash
# Server phải chạy trên port 8081
http://localhost:8081
```

### 2. Cần có sẵn:
- **Auction Room ID**: ID của phòng đấu giá đã được tạo
- **Bearer Token**: Token JWT của user (admin hoặc buyer)
- **Session IDs**: Danh sách session trong phòng (có thể lấy từ API)

---

## Bước 0: Tạo phòng đấu giá - Set thời gian cho từng phiên (session)

### API Endpoint
```
POST /api/stream/create
```

### Headers
```
Authorization: Bearer {your-jwt-token}
Content-Type: multipart/form-data
```

### Request Body (Form Data)
- `roomName`: Tên phòng đấu giá
- `description`: Mô tả (optional)
- `type`: Loại phòng (optional)
- `file`: Ảnh cover (optional, MultipartFile)
- `sessionsJson`: JSON string chứa danh sách session

### Ví dụ `sessionsJson`:
```json
[
  {
    "artworkId": "Aw-12345",
    "startingPrice": 50000000,
    "bidStep": 1000000,
    "imageUrl": "https://...",
    "durationMinutes": 15
  },
  {
    "artworkId": "Aw-67890",
    "startingPrice": 30000000,
    "bidStep": 500000,
    "imageUrl": "https://...",
    "durationMinutes": 10
  },
  {
    "artworkId": "Aw-11111",
    "startingPrice": 70000000,
    "bidStep": 2000000,
    "imageUrl": "https://...",
    "durationMinutes": 20
  }
]
```

### Giải thích
- **Thời gian set theo TỪNG PHIÊN, không phải theo phòng**: Mỗi phiên trong mảng `sessions` có thể có `durationMinutes` riêng
    - Phiên 1: `durationMinutes: 15` → 15 phút
    - Phiên 2: `durationMinutes: 10` → 10 phút
    - Phiên 3: `durationMinutes: 20` → 20 phút
- **Mỗi phiên độc lập**: Trong cùng một phòng, các phiên có thể có thời lượng khác nhau
- Nếu một phiên không set `durationMinutes` hoặc `durationMinutes = 0`, hệ thống sẽ dùng mặc định **10 phút** cho phiên đó
- Thời gian này được lưu vào `durationSeconds` (phút × 60) của từng phiên trong database

### Response (200 OK)
```json
{
  "roomId": "ACR-12345",
  "wsUrl": "ws://localhost:8081/ws/stream/ACR-12345",
  "status": 2
}
```

### Test trong Postman
1. Tạo request mới: `POST /api/stream/create`
2. Chọn tab **Body** → **form-data**
3. Thêm các field:
    - `roomName`: "Phòng đấu giá Modern Art"
    - `description`: "Mô tả phòng"
    - `type`: "public"
    - `sessionsJson`: Paste JSON array ở trên
4. Thêm header `Authorization: Bearer {token}`
5. Click **Send**
6. Lưu `roomId` từ response

---

## Bước 1: Khởi động phiên đấu giá mới

### API Endpoint
```
POST /api/stream/room/{roomId}/start-next
```

### Headers
```
Authorization: Bearer {your-jwt-token}
Content-Type: application/json
```

### Request
- **Method**: POST
- **URL**: `http://localhost:8081/api/stream/room/{roomId}/start-next`
- **Path Variable**: `roomId` - ID của phòng đấu giá

### Response (200 OK)
```json
{
  "sessionId": "ATSS-12345",
  "orderIndex": 0,
  "status": 1,
  "startedAt": "2025-01-20T10:00:00"
}
```

### Giải thích
- `status: 1` = Phiên đã LIVE
- `startedAt` = Thời điểm bắt đầu phiên
- Backend tự động tính `endTime = startedAt + durationSeconds` (thời lượng của phiên đó, đã được set cho từng phiên khi tạo phòng)
- Frontend cần lưu `startedAt` và tính countdown từ đó

### Test trong Postman
1. Tạo request mới: `POST /api/stream/room/{roomId}/start-next`
2. Thay `{roomId}` bằng ID thực tế (ví dụ: `ACR-12345`)
3. Thêm header `Authorization: Bearer {token}`
4. Click **Send**
5. Lưu `sessionId` và `startedAt` từ response

---

## Bước 2: Lấy thông tin phiên hiện tại (để xem countdown)

### API Endpoint
```
GET /api/stream/room/{roomId}/sessions/current-or-next
```

### Headers
```
Authorization: Bearer {your-jwt-token}
```

### Response (200 OK)
```json
{
  "id": "ATSS-12345",
  "auctionRoomId": "ACR-12345",
  "artworkId": "Aw-67890",
  "imageUrl": "https://...",
  "startTime": "2025-01-20T10:00:00",
  "endedAt": "2025-01-20T10:10:00",
  "startingPrice": 50000000,
  "currentPrice": 50000000,
  "status": 1,
  "durationSeconds": 600,
  "maxDurationSeconds": 1200,
  "extendStepSeconds": 120,
  "extendThresholdSeconds": 60
}
```

### Giải thích
- `startTime`: Thời điểm bắt đầu
- `endedAt`: Thời điểm kết thúc dự kiến (để FE countdown)
- `status: 1` = Đang LIVE
- `durationSeconds`: Thời lượng cơ bản của phiên này (đã được set cho phiên này khi tạo phòng)
    - Ví dụ: 900 = 15 phút, 600 = 10 phút, 1200 = 20 phút
    - **Lưu ý**: Mỗi phiên có thể có `durationSeconds` khác nhau, không phụ thuộc vào phòng
- `maxDurationSeconds`: Thời lượng tối đa có thể kéo dài (thường = `durationSeconds + 600` giây)
- `extendStepSeconds`: Số giây cộng thêm khi auto-extend (120 = 2 phút)
- `extendThresholdSeconds`: Ngưỡng để kích hoạt auto-extend (60 = 1 phút)

### Cách tính countdown trong Frontend
```javascript
const now = new Date();
const endTime = new Date(session.endedAt);
const remainingSeconds = Math.max(0, Math.floor((endTime - now) / 1000));
```

---

## Bước 3: Đặt giá và kiểm tra auto-extend

### API Endpoint
```
POST /api/bids/{roomId}/place
```

### Headers
```
Authorization: Bearer {your-jwt-token}
Content-Type: application/json
```

### Request Body
```json
{
  "amount": 55000000,
  "idempotencyKey": "BID-001-20250120"
}
```

### Response khi đặt giá thành công (200 OK)
```json
{
  "result": 1,
  "currentPrice": 55000000,
  "leader": "username123",
  "message": "Accepted",
  "bidTime": "2025-01-20T10:05:30",
  "sessionEndTime": "2025-01-20T10:12:00",
  "extended": true
}
```

### Response khi đặt giá nhưng bị outbid (-1)
```json
{
  "result": -1,
  "currentPrice": 56000000,
  "leader": "another_user",
  "message": "Someone bid faster",
  "bidTime": "2025-01-20T10:05:31",
  "sessionEndTime": "2025-01-20T10:12:00",
  "extended": false
}
```

### Giải thích
- `result: 1` = Đặt giá thành công
- `result: -1` = Bị outbid (có người đặt giá nhanh hơn)
- `result: 0` = Từ chối (giá quá thấp, bạn đã là leader hiện tại, v.v.)
- `sessionEndTime`: Thời điểm kết thúc mới (có thể đã được gia hạn)
- `extended: true` = Phiên vừa được auto-extend thêm 120 giây
- **Lưu ý**: Hệ thống không kiểm tra số dư ví, người dùng có thể đặt giá tùy thích

### Kịch bản test auto-extend

#### Test Case 1: Đặt giá khi còn > 60 giây
1. Tạo phòng với phiên đầu tiên có `durationMinutes: 15` (15 phút cho phiên này)
2. Khởi động phiên mới
3. Đợi 10 phút (còn 5 phút)
4. Đặt giá
5. **Kỳ vọng**: `extended: false`, `sessionEndTime` không thay đổi

#### Test Case 2: Đặt giá khi còn ≤ 60 giây
1. Tạo phòng với phiên đầu tiên có `durationMinutes: 10` (10 phút cho phiên này)
2. Khởi động phiên mới
3. Đợi đến khi còn 50 giây (hoặc set `endedAt` trong DB về quá khứ gần)
4. Đặt giá
5. **Kỳ vọng**:
    - `extended: true`
    - `sessionEndTime` = `endedAt cũ + 120 giây`
    - Nhưng không vượt quá `startTime + maxDurationSeconds`

#### Test Case 3: Đặt giá khi đã đạt max duration
1. Tạo phòng với phiên đầu tiên có `durationMinutes: 5` (5 phút cho phiên này)
2. Khởi động phiên mới
3. Gia hạn nhiều lần đến khi đạt `maxDurationSeconds`
4. Đặt giá khi còn ≤ 60 giây
5. **Kỳ vọng**: `extended: false` (vì đã đạt max, không thể gia hạn thêm)

#### Test Case 4: Các phiên trong cùng phòng có thời gian khác nhau
1. Tạo phòng với 3 phiên:
    - Phiên 1: `durationMinutes: 15` (15 phút)
    - Phiên 2: `durationMinutes: 10` (10 phút)
    - Phiên 3: `durationMinutes: 20` (20 phút)
2. Khởi động phiên 1 → Kiểm tra `durationSeconds = 900` (15 phút)
3. Đợi phiên 1 kết thúc, phiên 2 tự động start
4. Kiểm tra phiên 2 có `durationSeconds = 600` (10 phút)
5. Đợi phiên 2 kết thúc, phiên 3 tự động start
6. Kiểm tra phiên 3 có `durationSeconds = 1200` (20 phút)

### Test trong Postman
1. Tạo request: `POST /api/bids/{roomId}/place`
2. Body (raw JSON):
   ```json
   {
     "amount": 55000000,
     "idempotencyKey": "BID-" + Date.now()
   }
   ```
3. **Lưu ý**: `idempotencyKey` phải unique mỗi lần đặt giá
4. Click **Send**
5. Kiểm tra `extended` và `sessionEndTime` trong response

---

## Bước 4: Theo dõi WebSocket realtime

### WebSocket Endpoints

#### 1. Topic cho từng session (bids)
```
ws://localhost:8081/ws
Subscribe: /topic/auction.{sessionId}.bids
```

#### 2. Topic cho toàn bộ phòng (session events)
```
ws://localhost:8081/ws
Subscribe: /topic/auction-room/{roomId}
```

### Message Format khi có bid mới
```json
{
  "eventType": "BID_ACCEPTED",
  "sessionId": "ATSS-12345",
  "roomId": "ACR-12345",
  "price": 55000000,
  "leader": "username123",
  "at": "2025-01-20T10:05:30",
  "extended": true,
  "endTime": "2025-01-20T10:12:00",
  "remainingSeconds": 390
}
```

### Message Format khi session bắt đầu
```json
{
  "eventType": "SESSION_STARTED",
  "sessionId": "ATSS-12345",
  "roomId": "ACR-12345",
  "orderIndex": 0,
  "startTime": "2025-01-20T10:00:00",
  "endTime": "2025-01-20T10:10:00",
  "currentPrice": 50000000,
  "status": 1,
  "autoStart": false
}
```

### Message Format khi session kết thúc
```json
{
  "eventType": "SESSION_ENDED",
  "sessionId": "ATSS-12345",
  "roomId": "ACR-12345",
  "orderIndex": 0,
  "startTime": "2025-01-20T10:00:00",
  "endTime": "2025-01-20T10:12:00",
  "currentPrice": 55000000,
  "status": 0,
  "reason": "TIMEOUT"
}
```

### Cách test WebSocket trong Postman
1. Mở tab **New > WebSocket Request**
2. URL: `ws://localhost:8081/ws`
3. Click **Connect**
4. Sau khi kết nối, gửi message để subscribe:
   ```json
   {
     "destination": "/topic/auction-room/ACR-12345",
     "body": ""
   }
   ```
5. Hoặc subscribe session cụ thể:
   ```json
   {
     "destination": "/topic/auction.ATSS-12345.bids",
     "body": ""
   }
   ```
6. Khi có bid mới hoặc session event, message sẽ xuất hiện trong tab **Messages**

---

## Bước 5: Kiểm tra auto-close và chuyển phiên tiếp theo

### Cách test auto-close

#### Phương pháp 1: Chờ thời gian thực
1. Khởi động phiên mới
2. Đợi đến khi `endedAt` đã qua
3. Backend scheduler (chạy mỗi 1 giây) sẽ tự động:
    - Đóng phiên hiện tại (`status = 0`)
    - Gửi WebSocket event `SESSION_ENDED`
    - Tự động khởi động phiên tiếp theo nếu có

#### Phương pháp 2: Kiểm tra bằng API
Sau khi `endedAt` đã qua, gọi:
```
GET /api/stream/room/{roomId}/sessions/current-or-next
```

**Kỳ vọng**:
- Nếu còn session chưa chạy: Trả về session mới với `status = 1` (đã tự động start)
- Nếu hết session: Trả về 404 hoặc error message

### Response khi có phiên tiếp theo
```json
{
  "id": "ATSS-12346",
  "auctionRoomId": "ACR-12345",
  "artworkId": "Aw-67891",
  "startTime": "2025-01-20T10:12:00",
  "endedAt": "2025-01-20T10:22:00",
  "status": 1,
  "orderIndex": 1
}
```

### Response khi hết session
```json
{
  "timestamp": "2025-01-20T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "No session in this room"
}
```

---

## Checklist Test Đầy Đủ

### ✅ Test tạo phòng - Set thời gian cho từng phiên
- [ ] POST `/api/stream/create` với các session có `durationMinutes` khác nhau
- [ ] Kiểm tra mỗi session có `durationSeconds` đúng với `durationMinutes` đã set cho phiên đó
- [ ] Kiểm tra trong cùng một phòng, các phiên có thể có thời lượng khác nhau
- [ ] Kiểm tra session không set `durationMinutes` → dùng mặc định 10 phút cho phiên đó

### ✅ Test khởi động phiên
- [ ] POST `/api/stream/room/{roomId}/start-next` thành công
- [ ] Response có `startedAt` và `status = 1`
- [ ] GET `/api/stream/room/{roomId}/sessions/current-or-next` trả về phiên vừa start
- [ ] Kiểm tra `endedAt` = `startTime + durationSeconds` (đúng với thời lượng đã set)

### ✅ Test đặt giá
- [ ] POST `/api/bids/{roomId}/place` với giá hợp lệ → `result = 1`
- [ ] POST với giá quá thấp → `result = 0`, message "Bid too low"
- [ ] POST khi đã là leader hiện tại → `result = 0`, message "You are already the current leader"
- [ ] POST với `idempotencyKey` trùng → Trả về trạng thái hiện tại
- [ ] **Lưu ý**: Hệ thống không kiểm tra số dư ví, người dùng có thể đặt giá tùy thích

### ✅ Test auto-extend
- [ ] Đặt giá khi còn > 60 giây → `extended = false`
- [ ] Đặt giá khi còn ≤ 60 giây → `extended = true`, `sessionEndTime` tăng 120 giây
- [ ] Đặt giá khi đã đạt max duration → `extended = false` (không thể gia hạn thêm)

### ✅ Test WebSocket
- [ ] Subscribe `/topic/auction-room/{roomId}` → Nhận event `SESSION_STARTED`
- [ ] Subscribe `/topic/auction.{sessionId}.bids` → Nhận event `BID_ACCEPTED` khi có bid
- [ ] Message có đầy đủ `endTime`, `remainingSeconds`, `extended`

### ✅ Test auto-close
- [ ] Chờ đến `endedAt` → Backend tự đóng phiên
- [ ] WebSocket nhận event `SESSION_ENDED` với `reason: "TIMEOUT"`
- [ ] Nếu còn session → Tự động start phiên tiếp theo
- [ ] GET `/api/stream/room/{roomId}/sessions/current-or-next` trả về phiên mới

---

## Ví dụ Test Scenario Hoàn Chỉnh

### Scenario: Đấu giá một phiên từ đầu đến cuối

1. **Tạo phòng với phiên có thời gian 15 phút (set cho phiên này)**
   ```
   POST /api/stream/create
   Body: sessionsJson = [{
     "artworkId": "Aw-12345",
     "startingPrice": 50000000,
     "bidStep": 1000000,
     "durationMinutes": 15  // Thời gian này chỉ áp dụng cho phiên này
   }]
   → Lưu roomId = "ACR-12345"
   ```
   **Lưu ý**: `durationMinutes: 15` chỉ áp dụng cho phiên đầu tiên. Nếu có nhiều phiên, mỗi phiên có thể có `durationMinutes` khác nhau.

2. **Khởi động phiên**
   ```
   POST /api/stream/room/ACR-12345/start-next
   → Lưu sessionId = "ATSS-12345", startedAt = "10:00:00"
   ```

3. **Lấy thông tin countdown**
   ```
   GET /api/stream/room/ACR-12345/sessions/current-or-next
   → endedAt = "10:15:00", remainingSeconds = 900 (15 phút = 900 giây)
   ```

4. **Đặt giá lần 1 (còn 10 phút)**
   ```
   POST /api/bids/ACR-12345/place
   Body: { "amount": 55000000, "idempotencyKey": "BID-001" }
   → extended = false, sessionEndTime = "10:15:00" (không đổi)
   ```

5. **Đặt giá lần 2 (còn 30 giây)**
   ```
   POST /api/bids/ACR-12345/place
   Body: { "amount": 60000000, "idempotencyKey": "BID-002" }
   → extended = true, sessionEndTime = "10:17:00" (tăng 120 giây từ 10:15:00)
   ```

6. **Theo dõi WebSocket**
    - Subscribe `/topic/auction.ATSS-12345.bids`
    - Nhận message mỗi khi có bid mới với `remainingSeconds` cập nhật

7. **Chờ hết thời gian**
    - Đợi đến "10:17:00" (hoặc thời gian kết thúc sau khi gia hạn)
    - Backend tự đóng phiên
    - WebSocket nhận `SESSION_ENDED`
    - Nếu còn session → Tự động start phiên tiếp theo (với thời lượng riêng của phiên đó)

---

## Troubleshooting

### Lỗi: "No active session in this room at this time"
- ✅ Kiểm tra phiên đã được start chưa (`POST /api/stream/room/{roomId}/start-next`)
- ✅ Kiểm tra `status = 1` (LIVE)
- ✅ Kiểm tra `startTime <= now`

### Lỗi: "Session is not LIVE"
- ✅ Phiên có thể đã bị đóng (`status = 0`)
- ✅ Gọi `GET /api/stream/room/{roomId}/sessions/current-or-next` để xem phiên hiện tại

### Auto-extend không hoạt động
- ✅ Kiểm tra `endedAt` có giá trị không null
- ✅ Kiểm tra thời gian còn lại ≤ `extendThresholdSeconds` (mặc định 60)
- ✅ Kiểm tra chưa đạt `maxDurationSeconds`

### WebSocket không nhận message
- ✅ Kiểm tra kết nối WebSocket đã thành công
- ✅ Kiểm tra đã subscribe đúng topic chưa
- ✅ Kiểm tra server có đang chạy không

### Auto-close không hoạt động
- ✅ Kiểm tra `@EnableScheduling` đã được bật trong `AuctionBackendApplication`
- ✅ Kiểm tra scheduler có đang chạy (mỗi 1 giây)
- ✅ Kiểm tra `endedAt` đã qua thời điểm hiện tại chưa

---

## Tóm tắt các API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/stream/create` | Tạo phòng đấu giá với các session (set `durationMinutes` cho TỪNG phiên, không phải cho phòng) |
| POST | `/api/stream/room/{roomId}/start-next` | Khởi động phiên tiếp theo |
| GET | `/api/stream/room/{roomId}/sessions/current-or-next` | Lấy phiên hiện tại hoặc tiếp theo |
| POST | `/api/bids/{roomId}/place` | Đặt giá (có auto-extend) |
| POST | `/api/stream/stop-session/{sessionId}` | Dừng phiên thủ công |
| WS | `/ws` → `/topic/auction-room/{roomId}` | WebSocket cho session events |
| WS | `/ws` → `/topic/auction.{sessionId}.bids` | WebSocket cho bid events |

---

**Happy Testing! 🚀**

