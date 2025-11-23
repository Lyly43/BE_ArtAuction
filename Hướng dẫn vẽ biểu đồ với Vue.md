# Hướng dẫn vẽ biểu đồ với Vue.js

Tài liệu này hướng dẫn cách sử dụng các API thống kê để vẽ biểu đồ trong ứng dụng Vue.js.

## 1. Cài đặt thư viện

### Chart.js (khuyến nghị)
```bash
npm install chart.js vue-chartjs
```

Hoặc nếu dùng Vue 3:
```bash
npm install chart.js vue-chartjs@next
```

### ApexCharts (tùy chọn)
```bash
npm install apexcharts vue3-apexcharts
```

## 2. Cấu trúc Response từ API

Tất cả API thống kê trả về format chuẩn:
```json
{
  "status": 1,
  "message": "Success",
  "data": [
    { "date": "02/11/2025", "count": 10 },
    { "date": "03/11/2025", "count": 0 },
    { "date": "04/11/2025", "count": 15 }
  ],
  "labels": ["02/11/2025", "03/11/2025", "04/11/2025"],
  "datasets": [{
    "label": "Thống kê người dùng đăng ký",
    "data": [10, 0, 15],
    "backgroundColor": ["#A1B2C3", "#D4E5F6", "#E7F8AB"]
  }]
}
```

**Lưu ý quan trọng:**
- Format date: `dd/MM/yyyy` (ví dụ: "02/11/2025")
- API sẽ trả về **tất cả các ngày** trong khoảng từ `begin` đến `end`, kể cả những ngày không có dữ liệu (sẽ có giá trị 0)
- API doanh thu (`/revenue`) trả về `totalAmount` trong `data` thay vì `count`

## 3. Ví dụ với Chart.js

### Component Vue 3 (Composition API)

```vue
<template>
  <div class="chart-container">
    <div class="date-range-picker">
      <input 
        type="date" 
        v-model="beginDate" 
        @change="loadChart"
      />
      <input 
        type="date" 
        v-model="endDate" 
        @change="loadChart"
      />
    </div>
    <Line 
      v-if="chartData"
      :data="chartData" 
      :options="chartOptions"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
} from 'chart.js'
import axios from 'axios'

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
)

const beginDate = ref('')
const endDate = ref('')
const chartData = ref(null)
const chartOptions = ref({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'top',
    },
    title: {
      display: true,
      text: 'Thống kê người dùng đăng ký'
    }
  }
})

const loadChart = async () => {
  if (!beginDate.value || !endDate.value) return

  try {
    const response = await axios.post(
      `http://localhost:8085/api/admin/statistics/users-registration`,
      {
        begin: beginDate.value,
        end: endDate.value
      },
      {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('adminToken')}`,
          'Content-Type': 'application/json'
        }
      }
    )

    if (response.data.status === 1) {
      // Map response từ API sang format Chart.js
      chartData.value = {
        labels: response.data.labels,
        datasets: response.data.datasets.map(dataset => ({
          label: dataset.label,
          data: dataset.data,
          backgroundColor: dataset.backgroundColor,
          borderColor: dataset.backgroundColor.map(color => color + '80'), // Thêm opacity
          borderWidth: 2
        }))
      }
    }
  } catch (error) {
    console.error('Error loading chart:', error)
  }
}

// Helper function: Format date to dd/MM/yyyy
const formatDate = (date) => {
  const d = new Date(date)
  const day = String(d.getDate()).padStart(2, '0')
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const year = d.getFullYear()
  return `${day}/${month}/${year}`
}

onMounted(() => {
  // Set default date range (last 30 days)
  const end = new Date()
  const begin = new Date()
  begin.setDate(begin.getDate() - 30)
  
  beginDate.value = formatDate(begin)
  endDate.value = formatDate(end)
  
  loadChart()
})
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 400px;
  padding: 20px;
}

.date-range-picker {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.date-range-picker input {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}
</style>
```

## 4. Ví dụ với Bar Chart

```vue
<template>
  <Bar 
    v-if="chartData"
    :data="chartData" 
    :options="chartOptions"
  />
</template>

<script setup>
import { Bar } from 'vue-chartjs'
// ... import và setup tương tự như trên
</script>
```

## 5. Ví dụ với Pie Chart

```vue
<template>
  <Pie 
    v-if="chartData"
    :data="chartData" 
    :options="chartOptions"
  />
</template>

<script setup>
import { Pie } from 'vue-chartjs'
// ... import và setup tương tự như trên
</script>
```

## 6. Component tái sử dụng cho tất cả API thống kê

```vue
<template>
  <div class="statistics-chart">
    <div class="controls">
      <select v-model="selectedStatType" @change="loadChart">
        <option value="users-registration">Người dùng đăng ký</option>
        <option value="auction-rooms">Phòng đấu giá</option>
        <option value="revenue">Doanh thu</option>
        <option value="reports">Báo cáo</option>
        <option value="artworks">Tác phẩm</option>
        <option value="bids">Đấu giá</option>
      </select>
      <input type="date" v-model="beginDate" @change="loadChart" />
      <input type="date" v-model="endDate" @change="loadChart" />
    </div>
    <Line v-if="chartData" :data="chartData" :options="chartOptions" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Line } from 'vue-chartjs'
// ... imports Chart.js như trên
import axios from 'axios'

const selectedStatType = ref('users-registration')
const beginDate = ref('')
const endDate = ref('')
const chartData = ref(null)

const API_ENDPOINTS = {
  'users-registration': '/api/admin/statistics/users-registration',
  'auction-rooms': '/api/admin/statistics/auction-rooms',
  'revenue': '/api/admin/statistics/revenue',
  'reports': '/api/admin/statistics/reports',
  'artworks': '/api/admin/statistics/artworks',
  'bids': '/api/admin/statistics/bids'
}

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'top',
    },
    title: {
      display: true,
      text: getChartTitle(selectedStatType.value)
    }
  }
}))

const getChartTitle = (type) => {
  const titles = {
    'users-registration': 'Thống kê người dùng đăng ký',
    'auction-rooms': 'Thống kê phòng đấu giá',
    'revenue': 'Thống kê doanh thu',
    'reports': 'Thống kê báo cáo',
    'artworks': 'Thống kê tác phẩm',
    'bids': 'Thống kê đấu giá'
  }
  return titles[type] || 'Thống kê'
}

const loadChart = async () => {
  if (!beginDate.value || !endDate.value) return

  try {
    const endpoint = API_ENDPOINTS[selectedStatType.value]
    const response = await axios.post(
      `http://localhost:8085${endpoint}`,
      {
        begin: beginDate.value,
        end: endDate.value
      },
      {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('adminToken')}`,
          'Content-Type': 'application/json'
        }
      }
    )

    if (response.data.status === 1) {
      chartData.value = {
        labels: response.data.labels,
        datasets: response.data.datasets.map(dataset => ({
          label: dataset.label,
          data: dataset.data,
          backgroundColor: dataset.backgroundColor,
          borderColor: dataset.backgroundColor.map(color => color + '80'),
          borderWidth: 2
        }))
      }
    }
  } catch (error) {
    console.error('Error loading chart:', error)
  }
}

// Initialize với date range mặc định
// Helper function: Format date to dd/MM/yyyy
const formatDate = (date) => {
  const d = new Date(date)
  const day = String(d.getDate()).padStart(2, '0')
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const year = d.getFullYear()
  return `${day}/${month}/${year}`
}

const init = () => {
  const end = new Date()
  const begin = new Date()
  begin.setDate(begin.getDate() - 30)
  
  beginDate.value = formatDate(begin)
  endDate.value = formatDate(end)
  
  loadChart()
}

init()
</script>
```

## 7. Xử lý API Doanh thu (Revenue)

API doanh thu trả về `totalAmount` thay vì `count`. Cần xử lý đặc biệt:

```javascript
const loadRevenueChart = async () => {
  const response = await axios.post(
    `http://localhost:8085/api/admin/statistics/revenue`,
    {
      begin: beginDate.value,
      end: endDate.value
    },
    {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    }
  )

  if (response.data.status === 1) {
    // Extract totalAmount từ data items
    const amounts = response.data.data.map(item => item.totalAmount || 0)
    
    chartData.value = {
      labels: response.data.labels,
      datasets: [{
        label: 'Doanh thu (VNĐ)',
        data: amounts,
        backgroundColor: response.data.datasets[0].backgroundColor,
        borderColor: response.data.datasets[0].backgroundColor.map(c => c + '80'),
        borderWidth: 2
      }]
    }
  }
}
```

## 8. Format số tiền cho Doanh thu

```javascript
import { formatCurrency } from '@/utils/format' // Tùy chỉnh utility

const chartOptions = {
  // ... other options
  scales: {
    y: {
      ticks: {
        callback: function(value) {
          return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
          }).format(value)
        }
      }
    }
  }
}
```

## 9. Xử lý lỗi và Loading state

```vue
<template>
  <div>
    <div v-if="loading">Đang tải...</div>
    <div v-else-if="error">{{ error }}</div>
    <Line v-else-if="chartData" :data="chartData" :options="chartOptions" />
  </div>
</template>

<script setup>
const loading = ref(false)
const error = ref(null)

const loadChart = async () => {
  loading.value = true
  error.value = null
  
  try {
    const response = await axios.post(
      `http://localhost:8085/api/admin/statistics/users-registration`,
      {
        begin: beginDate.value,
        end: endDate.value
      },
      {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('adminToken')}`,
          'Content-Type': 'application/json'
        }
      }
    )
  } catch (err) {
    error.value = 'Không thể tải dữ liệu thống kê'
    console.error(err)
  } finally {
    loading.value = false
  }
}
</script>
```

## 10. Tips và Best Practices

1. **Cache dữ liệu:** Lưu response vào localStorage hoặc Vuex/Pinia để tránh gọi API nhiều lần
2. **Debounce:** Thêm debounce cho date picker để tránh gọi API quá nhiều khi user thay đổi ngày
3. **Validation:** Kiểm tra `begin <= end` trước khi gọi API
4. **Error handling:** Luôn xử lý trường hợp API trả về `status: 0`
5. **Responsive:** Đảm bảo biểu đồ responsive trên mobile

## 11. Ví dụ với ApexCharts (Tùy chọn)

```vue
<template>
  <apexchart
    type="line"
    height="350"
    :options="chartOptions"
    :series="series"
  />
</template>

<script setup>
import { ref } from 'vue'
import VueApexCharts from 'vue3-apexcharts'

const series = ref([{
  name: 'Người dùng đăng ký',
  data: []
}])

const chartOptions = ref({
  chart: {
    type: 'line'
  },
  xaxis: {
    categories: []
  }
})

const loadChart = async () => {
  const response = await axios.post(
    `http://localhost:8085/api/admin/statistics/users-registration`,
    {
      begin: beginDate.value,
      end: endDate.value
    },
    {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('adminToken')}`,
        'Content-Type': 'application/json'
      }
    }
  )
  
  if (response.data.status === 1) {
    series.value[0].data = response.data.datasets[0].data
    chartOptions.value.xaxis.categories = response.data.labels
  }
}
</script>
```

---

**Lưu ý:** Thay đổi `http://localhost:8085` thành base URL thực tế của backend trong production.

