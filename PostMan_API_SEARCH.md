# API SEARCH ENDPOINTS - TÀI LIỆU CHO FRONTEND

## Tổng quan

Tài liệu này mô tả tất cả các API endpoint để tìm kiếm và lọc dữ liệu trong hệ thống Auction.

**Base URL**: `http://localhost:8081`

**Method**: `POST`

**Content-Type**: `application/json`

---

## 1. AUCTION ROOM SEARCH

### Endpoint

```
POST /api/auctionroom/search
```

### Request Body (JSON)

| Tên field | Kiểu | Bắt buộc | Mô tả | Ví dụ |
|-----------|------|----------|-------|-------|
| `id` | String | Không | Tìm kiếm theo ID chính xác | `ACR-12345` |
| `name` | String | Không | Tìm kiếm theo tên phòng (partial match, case-insensitive) | `phòng`, `Modern` |
| `type` | String | Không | Lọc theo thể loại (exact match) | `Modern`, `Classic` |
| `dateFrom` | String | Không | Lọc từ ngày (format: `yyyy-MM-dd`) | `2024-01-01` |
| `dateTo` | String | Không | Lọc đến ngày (format: `yyyy-MM-dd`) | `2024-12-31` |

**⚠️ QUAN TRỌNG**:
- **TẤT CẢ các field đều KHÔNG BẮT BUỘC** (optional)
- Bạn có thể gửi **1 field, nhiều field, hoặc không gửi field nào** (body rỗng `{}`)
- Khi gửi nhiều field, logic là **AND** (phải thỏa mãn TẤT CẢ điều kiện)
- Nếu không gửi field nào hoặc gửi `{}`, API sẽ trả về **TẤT CẢ** records

### Ví dụ Request từ Frontend

**JavaScript/Axios**:
```javascript
// ✅ Chỉ tìm theo tên (1 field)
const response = await axios.post('http://localhost:8081/api/auctionroom/search', {
  name: 'phòng'
});

// ✅ Chỉ tìm theo ID (1 field)
const response = await axios.post('http://localhost:8081/api/auctionroom/search', {
  id: 'ACR-12345'
});

// ✅ Kết hợp 2 field (name + type)
const response = await axios.post('http://localhost:8081/api/auctionroom/search', {
  name: 'phòng',
  type: 'Modern'
});

// ✅ Kết hợp nhiều điều kiện (tất cả field)
const response = await axios.post('http://localhost:8081/api/auctionroom/search', {
  name: 'phòng',
  type: 'Modern',
  dateFrom: '2024-01-01',
  dateTo: '2024-12-31'
});

// ✅ Lấy tất cả (không gửi field nào hoặc body rỗng)
const response = await axios.post('http://localhost:8081/api/auctionroom/search', {});
// hoặc
const response = await axios.post('http://localhost:8081/api/auctionroom/search');
```

**Request Body (JSON)**:
```json
{
  "name": "phòng",
  "type": "Modern",
  "dateFrom": "2024-01-01",
  "dateTo": "2024-12-31"
}
```

### Response Format

```json
[
  {
    "id": "ACR-abc123",
    "roomName": "Phòng đấu giá Modern Art",
    "type": "Modern",
    "status": 1,
    "createdAt": "2024-01-15T10:00:00",
    "description": "...",
    "imageAuctionRoom": "...",
    "viewCount": 100
  }
]
```

---

## 2. ARTWORK SEARCH

### Endpoint

```
POST /api/artwork/search
```

### Request Body (JSON)

| Tên field | Kiểu | Bắt buộc | Mô tả | Ví dụ |
|-----------|------|----------|-------|-------|
| `id` | String | Không | Tìm kiếm theo ID chính xác | `Aw-12345` |
| `name` | String | Không | Tìm kiếm theo title (partial match, case-insensitive) | `Monet`, `Van Gogh` |
| `type` | String | Không | Lọc theo paintingGenre (exact match) | `Impressionism`, `Realism` |
| `dateFrom` | String | Không | Lọc từ ngày tạo (format: `yyyy-MM-dd`) | `2024-01-01` |
| `dateTo` | String | Không | Lọc đến ngày tạo (format: `yyyy-MM-dd`) | `2024-12-31` |

### Ví dụ Request từ Frontend

**JavaScript/Axios**:
```javascript
// Tìm theo title
const response = await axios.post('http://localhost:8081/api/artwork/search', {
  name: 'Monet'
});

// Lọc theo genre
const response = await axios.post('http://localhost:8081/api/artwork/search', {
  type: 'Impressionism'
});

// Kết hợp điều kiện
const response = await axios.post('http://localhost:8081/api/artwork/search', {
  name: 'Monet',
  type: 'Impressionism',
  dateFrom: '2024-01-01'
});
```

**Request Body (JSON)**:
```json
{
  "name": "Monet",
  "type": "Impressionism",
  "dateFrom": "2024-01-01"
}
```

### Response Format

```json
[
  {
    "id": "Aw-12345",
    "title": "Water Lilies",
    "paintingGenre": "Impressionism",
    "createdAt": "2024-01-15T10:00:00",
    "startedPrice": 1000000,
    "status": 1
  }
]
```

---

## 3. INVOICE SEARCH

### Endpoint

```
POST /api/invoice/search
```

### Request Body (JSON)

| Tên field | Kiểu | Bắt buộc | Mô tả | Ví dụ |
|-----------|------|----------|-------|-------|
| `id` | String | Không | Tìm kiếm theo ID chính xác | `IV-12345` |
| `name` | String | Không | Tìm trong `artworkTitle` HOẶC `roomName` (partial match) | `artwork`, `phòng` |
| `dateFrom` | String | Không | Lọc từ ngày tạo (format: `yyyy-MM-dd`) | `2024-01-01` |
| `dateTo` | String | Không | Lọc đến ngày tạo (format: `yyyy-MM-dd`) | `2024-12-31` |

**Lưu ý**: Field `name` sẽ tìm trong CẢ HAI field: `artworkTitle` và `roomName`

### Ví dụ Request từ Frontend

**JavaScript/Axios**:
```javascript
// Tìm theo ID
const response = await axios.post('http://localhost:8081/api/invoice/search', {
  id: 'IV-12345'
});

// Tìm trong artworkTitle hoặc roomName
const response = await axios.post('http://localhost:8081/api/invoice/search', {
  name: 'artwork'
});

// Lọc theo khoảng ngày
const response = await axios.post('http://localhost:8081/api/invoice/search', {
  dateFrom: '2024-01-01',
  dateTo: '2024-12-31'
});
```

**Request Body (JSON)**:
```json
{
  "name": "artwork",
  "dateFrom": "2024-01-01",
  "dateTo": "2024-12-31"
}
```

### Response Format

```json
[
  {
    "id": "IV-12345",
    "artworkTitle": "Water Lilies",
    "roomName": "Phòng đấu giá Modern Art",
    "totalAmount": 15000000,
    "paymentStatus": 1,
    "createdAt": "2024-01-15T10:00:00"
  }
]
```

---

## 4. WALLET SEARCH

### Endpoint

```
POST /api/wallets/search
```

### Request Body (JSON)

| Tên field | Kiểu | Bắt buộc | Mô tả | Ví dụ |
|-----------|------|----------|-------|-------|
| `id` | String | Không | Tìm kiếm theo ID chính xác | `WL-12345` |
| `dateFrom` | String | Không | Lọc từ ngày tạo (format: `yyyy-MM-dd`) | `2024-01-01` |
| `dateTo` | String | Không | Lọc đến ngày tạo (format: `yyyy-MM-dd`) | `2024-12-31` |

**Lưu ý**: Wallet không có field "name" và "type", chỉ tìm được theo ID và ngày

### Ví dụ Request từ Frontend

**JavaScript/Axios**:
```javascript
// Tìm theo ID
const response = await axios.post('http://localhost:8081/api/wallets/search', {
  id: 'WL-12345'
});

// Lọc theo khoảng ngày
const response = await axios.post('http://localhost:8081/api/wallets/search', {
  dateFrom: '2024-01-01',
  dateTo: '2024-12-31'
});
```

**Request Body (JSON)**:
```json
{
  "dateFrom": "2024-01-01",
  "dateTo": "2024-12-31"
}
```

### Response Format

```json
[
  {
    "id": "WL-12345",
    "userId": "USR-abc123",
    "balance": 1000000,
    "frozenBalance": 500000,
    "createdAt": "2024-01-15T10:00:00"
  }
]
```

---

## 5. HISTORY (AUCTION SESSION) SEARCH

### Endpoint

```
POST /api/history/search
```

### Request Body (JSON)

| Tên field | Kiểu | Bắt buộc | Mô tả | Ví dụ |
|-----------|------|----------|-------|-------|
| `id` | String | Không | Tìm kiếm theo ID chính xác | `ATSS-12345` |
| `type` | String | Không | Lọc theo type (exact match) | `Modern`, `Classic` |
| `dateFrom` | String | Không | Lọc từ ngày startTime (format: `yyyy-MM-dd`) | `2024-01-01` |
| `dateTo` | String | Không | Lọc đến ngày startTime (format: `yyyy-MM-dd`) | `2024-12-31` |

**Lưu ý**:
- Không có field `name` (AuctionSession không có field name)
- Lọc theo `startTime` của session (không phải `createdAt`)

### Ví dụ Request từ Frontend

**JavaScript/Axios**:
```javascript
// Tìm theo ID
const response = await axios.post('http://localhost:8081/api/history/search', {
  id: 'ATSS-12345'
});

// Lọc theo type
const response = await axios.post('http://localhost:8081/api/history/search', {
  type: 'Modern'
});

// Kết hợp điều kiện
const response = await axios.post('http://localhost:8081/api/history/search', {
  type: 'Modern',
  dateFrom: '2024-01-01',
  dateTo: '2024-12-31'
});
```

**Request Body (JSON)**:
```json
{
  "type": "Modern",
  "dateFrom": "2024-01-01",
  "dateTo": "2024-12-31"
}
```

### Response Format

```json
[
  {
    "id": "ATSS-12345",
    "auctionRoomId": "ACR-abc123",
    "artworkId": "Aw-xyz789",
    "type": "Modern",
    "startTime": "2024-01-15T10:00:00",
    "currentPrice": 1500000,
    "status": 1
  }
]
```

---

## TỔNG HỢP CÁC THAM SỐ CHUNG

### Field dùng chung cho tất cả API

| Field | Kiểu | Mô tả | Format | Ví dụ |
|-------|------|-------|--------|-------|
| `id` | String | Tìm theo ID chính xác | Bất kỳ | `ACR-12345` |
| `dateFrom` | String | Lọc từ ngày | `yyyy-MM-dd` | `2024-01-01` |
| `dateTo` | String | Lọc đến ngày | `yyyy-MM-dd` | `2024-12-31` |

### Field đặc biệt

| API | Field đặc biệt | Mô tả |
|-----|---------------|-------|
| Auction Room | `name` | Tìm trong `roomName` |
| Auction Room | `type` | Lọc theo `type` |
| Artwork | `name` | Tìm trong `title` |
| Artwork | `type` | Lọc theo `paintingGenre` |
| Invoice | `name` | Tìm trong `artworkTitle` HOẶC `roomName` |
| Wallet | Không có | Chỉ có `id`, `dateFrom`, `dateTo` |
| History | `type` | Lọc theo `type` |

---

## LƯU Ý QUAN TRỌNG

### 1. Date Format

**PHẢI** sử dụng format: `yyyy-MM-dd`

- Đúng: `2024-01-15`
- Sai: `15/01/2024`, `2024/01/15`, `01-15-2024`

### 2. Case-Insensitive Search

Tham số `name` không phân biệt hoa thường:
- `phòng` = `Phòng` = `PHÒNG`
- `monet` = `Monet` = `MONET`

### 3. Partial Match

Tham số `name` sẽ tìm kiếm partial match (chứa chuỗi):
- Tìm `name=Modern` sẽ match: "Modern Art", "Post-Modern", "Modernism"

### 4. Exact Match

Tham số `type` và `id` là exact match (chính xác):
- `type=Modern` chỉ match giá trị chính xác là "Modern"

### 5. Combined Filters

Khi kết hợp nhiều field, logic là **AND** (tất cả điều kiện phải thỏa mãn):
- `{"name": "phòng", "type": "Modern"}` = Tên chứa "phòng" **VÀ** type = "Modern"

### 6. Empty Response

Nếu không tìm thấy, API trả về mảng rỗng `[]` với status `200 OK` (không phải lỗi)

---

## VÍ DỤ SỬ DỤNG TRONG REACT/VUE

### React với Axios

```javascript
import axios from 'axios';

// Component Search Auction Room
const SearchAuctionRoom = () => {
  const [searchParams, setSearchParams] = useState({
    name: '',
    type: '',
    dateFrom: '',
    dateTo: ''
  });

  const handleSearch = async () => {
    try {
      const requestBody = {};
      if (searchParams.name) requestBody.name = searchParams.name;
      if (searchParams.type) requestBody.type = searchParams.type;
      if (searchParams.dateFrom) requestBody.dateFrom = searchParams.dateFrom;
      if (searchParams.dateTo) requestBody.dateTo = searchParams.dateTo;

      const response = await axios.post('http://localhost:8081/api/auctionroom/search', requestBody);
      
      console.log(response.data);
    } catch (error) {
      console.error('Error:', error);
    }
  };

  return (
    <div>
      <input 
        placeholder="Tên phòng" 
        value={searchParams.name}
        onChange={(e) => setSearchParams({...searchParams, name: e.target.value})}
      />
      <input 
        placeholder="Thể loại" 
        value={searchParams.type}
        onChange={(e) => setSearchParams({...searchParams, type: e.target.value})}
      />
      <input 
        type="date" 
        value={searchParams.dateFrom}
        onChange={(e) => setSearchParams({...searchParams, dateFrom: e.target.value})}
      />
      <button onClick={handleSearch}>Tìm kiếm</button>
    </div>
  );
};
```

### Vue với Axios

```javascript
<template>
  <div>
    <input v-model="searchParams.name" placeholder="Tên phòng" />
    <input v-model="searchParams.type" placeholder="Thể loại" />
    <input type="date" v-model="searchParams.dateFrom" />
    <button @click="handleSearch">Tìm kiếm</button>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      searchParams: {
        name: '',
        type: '',
        dateFrom: '',
        dateTo: ''
      }
    };
  },
  methods: {
    async handleSearch() {
      try {
        const requestBody = {};
        if (this.searchParams.name) requestBody.name = this.searchParams.name;
        if (this.searchParams.type) requestBody.type = this.searchParams.type;
        if (this.searchParams.dateFrom) requestBody.dateFrom = this.searchParams.dateFrom;
        if (this.searchParams.dateTo) requestBody.dateTo = this.searchParams.dateTo;

        const response = await axios.post('http://localhost:8081/api/auctionroom/search', requestBody);
        
        console.log(response.data);
      } catch (error) {
        console.error('Error:', error);
      }
    }
  }
};
</script>
```

---

## ERROR HANDLING

### Status Codes

| Status Code | Ý nghĩa |
|-------------|---------|
| `200 OK` | Thành công (có thể trả về `[]` nếu không tìm thấy) |
| `400 Bad Request` | Tham số không hợp lệ (ví dụ: date format sai) |
| `404 Not Found` | Endpoint không tồn tại |
| `500 Internal Server Error` | Lỗi server |

### Error Response Format

```json
{
  "timestamp": "2024-01-15T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid date format"
}
```

### Xử lý lỗi trong Frontend

```javascript
try {
  const response = await axios.post('http://localhost:8081/api/auctionroom/search', searchParams);
  
  if (response.data.length === 0) {
    // Không tìm thấy kết quả (không phải lỗi)
    console.log('Không tìm thấy kết quả');
  } else {
    // Có kết quả
    setResults(response.data);
  }
} catch (error) {
  if (error.response) {
    // Server trả về lỗi
    console.error('Error:', error.response.data.message);
  } else {
    // Lỗi network hoặc server không phản hồi
    console.error('Network error:', error.message);
  }
}
```

---

## TÓM TẮT ENDPOINTS

| Endpoint | Method | Request Body Fields |
|----------|--------|---------------------|
| `/api/auctionroom/search` | POST | `id`, `name`, `type`, `dateFrom`, `dateTo` |
| `/api/artwork/search` | POST | `id`, `name`, `type`, `dateFrom`, `dateTo` |
| `/api/invoice/search` | POST | `id`, `name`, `dateFrom`, `dateTo` |
| `/api/wallets/search` | POST | `id`, `dateFrom`, `dateTo` |
| `/api/history/search` | POST | `id`, `type`, `dateFrom`, `dateTo` |

---

**Lưu ý cuối**:

### ✅ Cách sử dụng các field:

1. **Chỉ cần 1 field**:
   ```json
   {"name": "phòng"}
   ```
   → Tìm tất cả phòng có tên chứa "phòng"

2. **Kết hợp nhiều field** (logic AND):
   ```json
   {
     "name": "phòng",
     "type": "Modern"
   }
   ```
   → Tìm phòng có tên chứa "phòng" **VÀ** type = "Modern"

3. **Body rỗng hoặc không gửi field nào**:
   ```json
   {}
   ```
   → Trả về **TẤT CẢ** phòng trong database

### ⚠️ Lưu ý:
- **KHÔNG cần gửi hết tất cả field** - chỉ gửi những field bạn muốn filter
- Nếu gửi nhiều field, phải thỏa mãn **TẤT CẢ** điều kiện (AND logic)
- Field nào không gửi sẽ không được áp dụng filter


