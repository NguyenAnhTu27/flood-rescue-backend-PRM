# Hướng dẫn kết nối API Authentication từ Frontend

## 🔗 API Endpoints

### Base URL
```
http://localhost:8080/api/auth
```

---

## 1. 📝 Đăng ký (Register)

**Endpoint:** `POST /api/auth/register`

**Request Body:**
```json
{
  "fullName": "Nguyen Van A",
  "phone": "0912345678",
  "email": "user@example.com",  // Optional
  "password": "Abc12345"
}
```

**Response Success (200):**
```json
{
  "message": "Đăng ký Citizen thành công"
}
```

**Response Error (400):**
```json
{
  "message": "Dữ liệu không hợp lệ",
  "errors": {
    "phone": "Số điện thoại không hợp lệ. Vui lòng nhập số điện thoại Việt Nam (10-11 chữ số)",
    "password": "Mật khẩu phải chứa ít nhất một chữ hoa, một chữ thường và một chữ số"
  }
}
```

---

## 2. 🔐 Đăng nhập (Login)

**Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
  "identifier": "user@example.com",  // hoặc "0912345678"
  "password": "Abc12345"
}
```

**Response Success (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "fullName": "Nguyen Van A",
  "role": "CITIZEN"
}
```

**Response Error (400):**
```json
{
  "message": "Dữ liệu không hợp lệ",
  "errors": {
    "identifier": "Định danh không được để trống"
  }
}
```

**Response Error (401):**
```json
{
  "message": "Tài khoản không tồn tại"
}
```

---

## 3. 💻 Code Examples

### React/JavaScript với Fetch API

```javascript
// API Base URL
const API_BASE_URL = 'http://localhost:8080/api/auth';

// 1. Đăng ký
async function register(fullName, phone, email, password) {
  try {
    const response = await fetch(`${API_BASE_URL}/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        fullName,
        phone,
        email, // optional
        password
      })
    });

    const data = await response.json();
    
    if (!response.ok) {
      throw new Error(data.message || 'Đăng ký thất bại');
    }

    return data;
  } catch (error) {
    console.error('Register error:', error);
    throw error;
  }
}

// 2. Đăng nhập
async function login(identifier, password) {
  try {
    const response = await fetch(`${API_BASE_URL}/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        identifier, // email hoặc phone
        password
      })
    });

    const data = await response.json();
    
    if (!response.ok) {
      throw new Error(data.message || 'Đăng nhập thất bại');
    }

    // Lưu token vào localStorage
    if (data.token) {
      localStorage.setItem('accessToken', data.token);
      localStorage.setItem('user', JSON.stringify({
        userId: data.userId,
        fullName: data.fullName,
        role: data.role
      }));
    }

    return data;
  } catch (error) {
    console.error('Login error:', error);
    throw error;
  }
}

// 3. Sử dụng token cho các API khác
function getAuthHeaders() {
  const token = localStorage.getItem('accessToken');
  return {
    'Content-Type': 'application/json',
    ...(token && { 'Authorization': `Bearer ${token}` })
  };
}

// Ví dụ: Gọi API có authentication
async function getProfile() {
  try {
    const response = await fetch('http://localhost:8080/api/user/profile', {
      method: 'GET',
      headers: getAuthHeaders()
    });

    if (!response.ok) {
      if (response.status === 401) {
        // Token hết hạn, đăng xuất
        localStorage.removeItem('accessToken');
        localStorage.removeItem('user');
        window.location.href = '/login';
        return;
      }
      throw new Error('Lấy thông tin thất bại');
    }

    return await response.json();
  } catch (error) {
    console.error('Get profile error:', error);
    throw error;
  }
}
```

### React với Axios

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json'
  }
});

// Thêm token vào mọi request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Xử lý lỗi 401 (Unauthorized)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Đăng ký
export const register = async (data) => {
  const response = await api.post('/auth/register', data);
  return response.data;
};

// Đăng nhập
export const login = async (identifier, password) => {
  const response = await api.post('/auth/login', { identifier, password });
  
  if (response.data.token) {
    localStorage.setItem('accessToken', response.data.token);
    localStorage.setItem('user', JSON.stringify({
      userId: response.data.userId,
      fullName: response.data.fullName,
      role: response.data.role
    }));
  }
  
  return response.data;
};
```

### React Hook Example

```javascript
import { useState } from 'react';

function LoginForm() {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await fetch('http://localhost:8080/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          identifier,
          password
        })
      });

      const data = await response.json();

      if (!response.ok) {
        // Xử lý lỗi validation
        if (data.errors) {
          const errorMessages = Object.values(data.errors).join(', ');
          setError(errorMessages);
        } else {
          setError(data.message || 'Đăng nhập thất bại');
        }
        return;
      }

      // Lưu token và chuyển hướng
      localStorage.setItem('accessToken', data.token);
      window.location.href = '/dashboard';
    } catch (error) {
      setError('Có lỗi xảy ra. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input
        type="text"
        value={identifier}
        onChange={(e) => setIdentifier(e.target.value)}
        placeholder="Email hoặc số điện thoại"
        required
      />
      <input
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="Mật khẩu"
        required
      />
      {error && <div className="error">{error}</div>}
      <button type="submit" disabled={loading}>
        {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
      </button>
    </form>
  );
}
```

---

## 4. ⚠️ Lưu ý quan trọng

### ✅ Phải có:
1. **Content-Type header:** `Content-Type: application/json`
2. **Request body:** Phải là JSON string (`JSON.stringify()`)
3. **Field names:** Phải đúng tên field (`identifier`, `password`, `phone`, etc.)

### ❌ Lỗi thường gặp:

**Lỗi 1: Thiếu Content-Type**
```javascript
// ❌ SAI
fetch('/api/auth/login', {
  method: 'POST',
  body: JSON.stringify({ identifier, password })
});

// ✅ ĐÚNG
fetch('/api/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ identifier, password })
});
```

**Lỗi 2: Gửi FormData thay vì JSON**
```javascript
// ❌ SAI
const formData = new FormData();
formData.append('identifier', identifier);
fetch('/api/auth/login', {
  method: 'POST',
  body: formData
});

// ✅ ĐÚNG
fetch('/api/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ identifier, password })
});
```

**Lỗi 3: Field name sai**
```javascript
// ❌ SAI
body: JSON.stringify({ email: identifier, password })

// ✅ ĐÚNG
body: JSON.stringify({ identifier, password })
```

---

## 5. 🔒 CORS Configuration

Backend đã cấu hình CORS cho `http://localhost:5173` (Vite default port).

Nếu frontend chạy ở port khác, cần cập nhật trong `CorsConfig.java`:
```java
config.setAllowedOrigins(List.of("http://localhost:3000")); // React default
// hoặc
config.setAllowedOrigins(List.of("http://localhost:5173")); // Vite default
```

---

## 6. 🧪 Test với Postman/cURL

### cURL - Register
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Nguyen Van A",
    "phone": "0912345678",
    "email": "user@example.com",
    "password": "Abc12345"
  }'
```

### cURL - Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "user@example.com",
    "password": "Abc12345"
  }'
```

### cURL - Login với Token
```bash
curl -X GET http://localhost:8080/api/user/profile \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```
