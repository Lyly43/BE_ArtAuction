# ALL SEARCH API - TÀI LIỆU TỔNG HỢP

## 📋 Tổng quan

Tài liệu này tổng hợp **TẤT CẢ** các API endpoint tìm kiếm và lọc dữ liệu trong hệ thống Auction.

**Base URL**: `http://localhost:8081`

**Method**: `POST`

**Content-Type**: `application/json`

**Response Format**: Tất cả API search trả về `SearchResponse` với cấu trúc:
```json
{
  "success": boolean,
  "message": string,
  "data": array | null,
  "count": number
}
```

---

## 🎯 Danh sách tất cả Search Endpoints

| # | Endpoint | Entity | Fields hỗ trợ |
|---|----------|--------|---------------|
| 1 | `/api/auctionroom/search` | AuctionRoom | `id`, `name`, `type`, `dateFrom`, `dateTo` |
| 2 | `/api/artwork/search` | Artwork | `id`, `name`, `type`, `dateFrom`, `dateTo` |
| 3 | `/api/invoice/search` | Invoice | `id`, `name`, `dateFrom`, `dateTo` |
| 4 | `/api/wallets/search` | Wallet | `id`, `dateFrom`, `dateTo` |
| 5 | `/api/history/search` | AuctionSession | `id`, `type`, `dateFrom`, `dateTo` |

---

## 1️⃣ AUCTION ROOM SEARCH

### Endpoint
```
POST /api/auctionroom/search
```

### Request Body (JSON)

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `id` | String | No | Tìm kiếm theo ID chính xác | `ACR-12345` |
| `name` | String | No | Tìm kiếm theo tên phòng (partial match, case-insensitive) | `phòng`, `Modern` |
| `type` | String | No | Lọc theo thể loại (exact match) | `Modern`, `Classic` |
| `dateFrom` | String | No | Lọc từ ngày (format: `yyyy-MM-dd`) | `2024-01-01` |
| `dateTo` | String | No | Lọc đến ngày (format: `yyyy-MM-dd`) | `2024-12-31` |

### Ví dụ Request

**Chỉ tìm theo tên:**
```json
{
  "name": "phòng"
}
```

**Kết hợp nhiều điều kiện:**
```json
{
  "name": "phòng",
  "type": "Modern",
  "dateFrom": "2024-01-01",
  "dateTo": "2024-12-31"
}
```

**Lấy tất cả (body rỗng):**
```json
{}
```

### Response Format

**Khi tìm thấy:**
```json
{
  "success": true,
  "message": "Tìm thấy 5 kết quả",
  "data": [
    {
      "id": "ACR-abc123",
      "roomName": "Phòng đấu giá Modern Art",
      "type": "Modern",
      "status": 1,
      "createdAt": "2024-01-15T10:00:00",
      "description": "...",
      "imageAuctionRoom": "...",
      "viewCount": 100,
      "depositAmount": 1000000
    }
  ],
  "count": 5
}
```

**Khi không tìm thấy:**
```json
{
  "success": false,
  "message": "Không tìm thấy kết quả nào",
  "data": null,
  "count": 0
}
```

### Ví dụ Code (JavaScript/Axios)

```javascript
const response = await axios.post('http://localhost:8081/api/auctionroom/search', {
  name: 'phòng',
  type: 'Modern'
});

if (response.data.success) {
  console.log(response.data.message); // "Tìm thấy 3 kết quả"
  console.log(response.data.data);    // Array các phòng
  console.log(response.data.count);  // 3
} else {
  console.log(response.data.message); // "Không tìm thấy kết quả nào"
}
```

---

## 2️⃣ ARTWORK SEARCH

### Endpoint
```
POST /api/artwork/search
```

### Request Body (JSON)

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `id` | String | No | Tìm kiếm theo ID chính xác | `Aw-12345` |
| `name` | String | No | Tìm kiếm theo title (partial match, case-insensitive) | `Monet`, `Van Gogh` |
| `type` | String | No | Lọc theo paintingGenre (exact match) | `Impressionism`, `Realism` |
| `dateFrom` | String | No | Lọc từ ngày tạo (format: `yyyy-MM-dd`) | `2024-01-01` |
| `dateTo` | String | No | Lọc đến ngày tạo (format: `yyyy-MM-dd`) | `2024-12-31` |

### Ví dụ Request

```json
{
  "name": "Monet",
  "type": "Impressionism",
  "dateFrom": "2024-01-01"
}
```

### Response Format

**Khi tìm thấy:**
```json
{
  "success": true,
  "message": "Tìm thấy 3 kết quả",
  "data": [
    {
      "id": "Aw-12345",
      "title": "Water Lilies",
      "paintingGenre": "Impressionism",
      "createdAt": "2024-01-15T10:00:00",
      "startedPrice": 1000000,
      "status": 1
    }
  ],
  "count": 3
}
```

**Khi không tìm thấy:**
```json
{
  "success": false,
  "message": "Không tìm thấy kết quả nào",
  "data": null,
  "count": 0
}
```

---

## 3️⃣ INVOICE SEARCH

### Endpoint
```
POST /api/invoice/search
```

### Request Body (JSON)

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `id` | String | No | Tìm kiếm theo ID chính xác | `IV-12345` |
| `name` | String | No | Tìm trong `artworkTitle` HOẶC `roomName` (partial match) | `artwork`, `phòng` |
| `dateFrom` | String | No | Lọc từ ngày tạo (format: `yyyy-MM-dd`) | `2024-01-01` |
| `dateTo` | String | No | Lọc đến ngày tạo (format: `yyyy-MM-dd`) | `2024-12-31` |

**Lưu ý**: Field `name` sẽ tìm trong CẢ HAI field: `artworkTitle` và `roomName`

### Ví dụ Request

```json
{
  "name": "artwork",
  "dateFrom": "2024-01-01",
  "dateTo": "2024-12-31"
}
```

### Response Format

**Khi tìm thấy:**
```json
{
  "success": true,
  "message": "Tìm thấy 2 kết quả",
  "data": [
    {
      "id": "IV-12345",
      "artworkTitle": "Water Lilies",
      "roomName": "Phòng đấu giá Modern Art",
      "totalAmount": 15000000,
      "paymentStatus": 1,
      "createdAt": "2024-01-15T10:00:00"
    }
  ],
  "count": 2
}
```

**Khi không tìm thấy:**
```json
{
  "success": false,
  "message": "Không tìm thấy kết quả nào",
  "data": null,
  "count": 0
}
```

---

## 4️⃣ WALLET SEARCH

### Endpoint
```
POST /api/wallets/search
```

### Request Body (JSON)

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `id` | String | No | Tìm kiếm theo ID chính xác | `WL-12345` |
| `dateFrom` | String | No | Lọc từ ngày tạo (format: `yyyy-MM-dd`) | `2024-01-01` |
| `dateTo` | String | No | Lọc đến ngày tạo (format: `yyyy-MM-dd`) | `2024-12-31` |

**Lưu ý**: Wallet không có field "name" và "type", chỉ tìm được theo ID và ngày

### Ví dụ Request

```json
{
  "dateFrom": "2024-01-01",
  "dateTo": "2024-12-31"
}
```

### Response Format

**Khi tìm thấy:**
```json
{
  "success": true,
  "message": "Tìm thấy 1 kết quả",
  "data": [
    {
      "id": "WL-12345",
      "userId": "USR-abc123",
      "balance": 1000000,
      "frozenBalance": 500000,
      "createdAt": "2024-01-15T10:00:00"
    }
  ],
  "count": 1
}
```

**Khi không tìm thấy:**
```json
{
  "success": false,
  "message": "Không tìm thấy kết quả nào",
  "data": null,
  "count": 0
}
```

---

## 5️⃣ HISTORY (AUCTION SESSION) SEARCH

### Endpoint
```
POST /api/history/search
```

### Request Body (JSON)

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `id` | String | No | Tìm kiếm theo ID chính xác | `ATSS-12345` |
| `type` | String | No | Lọc theo type (exact match) | `Modern`, `Classic` |
| `dateFrom` | String | No | Lọc từ ngày startTime (format: `yyyy-MM-dd`) | `2024-01-01` |
| `dateTo` | String | No | Lọc đến ngày startTime (format: `yyyy-MM-dd`) | `2024-12-31` |

**Lưu ý**:
- Không có field `name` (AuctionSession không có field name)
- Lọc theo `startTime` của session (không phải `createdAt`)

### Ví dụ Request

```json
{
  "type": "Modern",
  "dateFrom": "2024-01-01",
  "dateTo": "2024-12-31"
}
```

### Response Format

**Khi tìm thấy:**
```json
{
  "success": true,
  "message": "Tìm thấy 4 kết quả",
  "data": [
    {
      "id": "ATSS-12345",
      "auctionRoomId": "ACR-abc123",
      "artworkId": "Aw-xyz789",
      "type": "Modern",
      "startTime": "2024-01-15T10:00:00",
      "currentPrice": 1500000,
      "status": 1
    }
  ],
  "count": 4
}
```

**Khi không tìm thấy:**
```json
{
  "success": false,
  "message": "Không tìm thấy kết quả nào",
  "data": null,
  "count": 0
}
```

---

## 📊 SO SÁNH CÁC FIELD GIỮA CÁC API

### Field dùng chung

| Field | AuctionRoom | Artwork | Invoice | Wallet | History |
|-------|-------------|---------|---------|--------|---------|
| `id` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `dateFrom` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `dateTo` | ✅ | ✅ | ✅ | ✅ | ✅ |

### Field đặc biệt

| Field | AuctionRoom | Artwork | Invoice | Wallet | History |
|-------|-------------|---------|---------|--------|---------|
| `name` | ✅ (roomName) | ✅ (title) | ✅ (artworkTitle/roomName) | ❌ | ❌ |
| `type` | ✅ | ✅ (paintingGenre) | ❌ | ❌ | ✅ |

---

## 🔍 LOGIC TÌM KIẾM

### 1. Exact Match
- **Field**: `id`, `type`
- **Cách hoạt động**: Phải khớp chính xác
- **Ví dụ**: `id: "ACR-123"` chỉ tìm ID = "ACR-123"

### 2. Partial Match (Case-Insensitive)
- **Field**: `name`
- **Cách hoạt động**: Tìm chuỗi chứa trong field, không phân biệt hoa thường
- **Ví dụ**: `name: "phòng"` sẽ match "Phòng đấu giá", "PHÒNG", "phòng"

### 3. Date Range
- **Field**: `dateFrom`, `dateTo`
- **Cách hoạt động**: Lọc theo khoảng thời gian
- **Format**: `yyyy-MM-dd` (ví dụ: `2024-01-15`)
- **Lưu ý**:
    - Chỉ có `dateFrom`: từ ngày đó trở đi
    - Chỉ có `dateTo`: đến ngày đó
    - Có cả 2: trong khoảng thời gian

### 4. Combined Filters (AND Logic)
- Khi gửi nhiều field, tất cả điều kiện phải thỏa mãn (AND)
- **Ví dụ**:
  ```json
  {
    "name": "phòng",
    "type": "Modern"
  }
  ```
  → Tìm phòng có tên chứa "phòng" **VÀ** type = "Modern"

---

## ✅ VÍ DỤ SỬ DỤNG TRONG FRONTEND

### React với Axios

```javascript
import axios from 'axios';

const SearchComponent = () => {
  const [results, setResults] = useState([]);
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSearch = async (searchParams) => {
    setLoading(true);
    try {
      const response = await axios.post(
        'http://localhost:8081/api/auctionroom/search',
        searchParams
      );

      if (response.data.success) {
        setResults(response.data.data);
        setMessage(response.data.message);
      } else {
        setResults([]);
        setMessage(response.data.message);
      }
    } catch (error) {
      console.error('Error:', error);
      setMessage('Có lỗi xảy ra khi tìm kiếm');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <button onClick={() => handleSearch({ name: 'phòng' })}>
        Tìm kiếm
      </button>
      {loading && <p>Đang tìm kiếm...</p>}
      {message && <p>{message}</p>}
      {results.map(item => (
        <div key={item.id}>{item.roomName}</div>
      ))}
    </div>
  );
};
```

### Vue với Axios

```javascript
<template>
  <div>
    <button @click="handleSearch">Tìm kiếm</button>
    <p v-if="loading">Đang tìm kiếm...</p>
    <p v-if="message">{{ message }}</p>
    <div v-for="item in results" :key="item.id">
      {{ item.roomName }}
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      results: [],
      message: '',
      loading: false
    };
  },
  methods: {
    async handleSearch() {
      this.loading = true;
      try {
        const response = await axios.post(
          'http://localhost:8081/api/auctionroom/search',
          { name: 'phòng' }
        );

        if (response.data.success) {
          this.results = response.data.data;
          this.message = response.data.message;
        } else {
          this.results = [];
          this.message = response.data.message;
        }
      } catch (error) {
        console.error('Error:', error);
        this.message = 'Có lỗi xảy ra khi tìm kiếm';
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>
```

---

## 🧪 TEST VỚI POSTMAN

### Cấu hình Request

1. **Method**: `POST`
2. **URL**: `http://localhost:8081/api/auctionroom/search`
3. **Headers**:
    - `Content-Type: application/json`
4. **Body** (raw JSON):
   ```json
   {
     "name": "phòng",
     "type": "Modern"
   }
   ```

### Expected Response

**Success (200 OK):**
```json
{
  "success": true,
  "message": "Tìm thấy 3 kết quả",
  "data": [...],
  "count": 3
}
```

**Not Found (200 OK):**
```json
{
  "success": false,
  "message": "Không tìm thấy kết quả nào",
  "data": null,
  "count": 0
}
```

---

## ⚠️ LƯU Ý QUAN TRỌNG

### 1. Date Format
- **PHẢI** sử dụng format: `yyyy-MM-dd`
- ✅ Đúng: `2024-01-15`
- ❌ Sai: `15/01/2024`, `2024/01/15`, `01-15-2024`

### 2. Tất cả Field đều Optional
- Không cần gửi hết tất cả field
- Chỉ gửi những field bạn muốn filter
- Gửi body rỗng `{}` để lấy tất cả records

### 3. Response Format
- Tất cả API đều trả về `SearchResponse`
- Luôn kiểm tra `success` trước khi dùng `data`
- `data` có thể là `null` khi không tìm thấy

### 4. Security
- Tất cả search endpoints đều **public** (không cần authentication)
- Có thể gọi trực tiếp từ frontend hoặc Postman

---

## 📝 TÓM TẮT NHANH

| Endpoint | Entity | Fields chính |
|----------|--------|--------------|
| `/api/auctionroom/search` | AuctionRoom | `id`, `name`, `type`, `dateFrom`, `dateTo` |
| `/api/artwork/search` | Artwork | `id`, `name`, `type`, `dateFrom`, `dateTo` |
| `/api/invoice/search` | Invoice | `id`, `name`, `dateFrom`, `dateTo` |
| `/api/wallets/search` | Wallet | `id`, `dateFrom`, `dateTo` |
| `/api/history/search` | AuctionSession | `id`, `type`, `dateFrom`, `dateTo` |

**Response Format:**
```json
{
  "success": boolean,
  "message": string,
  "data": array | null,
  "count": number
}
```

---

**Tài liệu này được cập nhật lần cuối: 2024**

**Happy Coding! 🚀**

