# Postman Testing Guide - Rescue Flow APIs

## Base Configuration

- **Base URL**: `http://localhost:8080`
- **Authentication**: JWT Bearer Token (required for all rescue endpoints)
- **Content-Type**: `application/json`

---

## Step 1: Authentication (Get JWT Token)

### 1.1 Register a Citizen (Optional - if you don't have an account)

**POST** `http://localhost:8080/api/auth/register`

**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "fullName": "Nguyễn Văn A",
  "phone": "0912345678",
  "email": "nguyenvana@example.com",
  "password": "Password123"
}
```

**Response:**
```json
{
  "message": "Đăng ký Citizen thành công"
}
```

### 1.2 Login to Get JWT Token

**POST** `http://localhost:8080/api/auth/login`

**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "identifier": "0912345678",
  "password": "Password123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "fullName": "Nguyễn Văn A",
  "role": "CITIZEN"
}
```

**⚠️ IMPORTANT**: Copy the `token` value from the response. You'll need it for all rescue endpoints.

---

## Step 2: Set Up Postman Environment (Recommended)

1. In Postman, click **Environments** → **+** to create a new environment
2. Add variables:
   - `base_url`: `http://localhost:8080`
   - `token`: (leave empty, will be set after login)
3. Use `{{base_url}}` and `{{token}}` in your requests

---

## Step 3: Citizen Rescue Endpoints

### 3.1 Create Rescue Request

**POST** `{{base_url}}/api/rescue/citizen/requests`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {{token}}
```

**Body (JSON):**
```json
{
  "affectedPeopleCount": 3,
  "description": "Nhà bị ngập nước, cần cứu hộ khẩn cấp. Nước đã dâng cao đến tầng 2.",
  "addressText": "123 Đường ABC, Phường XYZ, Quận 1, TP.HCM",
  "priority": "HIGH",
  "attachments": [
    {
      "fileUrl": "https://example.com/image1.jpg",
      "fileType": "IMAGE"
    },
    {
      "fileUrl": "https://example.com/video1.mp4",
      "fileType": "VIDEO"
    }
  ]
}
```

**Priority values**: `HIGH`, `MEDIUM`, `LOW`
**FileType values**: `IMAGE`, `VIDEO`, `OTHER`

**Response:**
```json
{
  "id": 1,
  "code": "RR202412151234",
  "citizenId": 1,
  "citizenName": "Nguyễn Văn A",
  "citizenPhone": "0912345678",
  "status": "PENDING",
  "priority": "HIGH",
  "affectedPeopleCount": 3,
  "description": "Nhà bị ngập nước...",
  "addressText": "123 Đường ABC...",
  "locationVerified": false,
  "createdAt": "2024-12-15T10:30:00",
  "updatedAt": "2024-12-15T10:30:00"
}
```

### 3.2 Get My Rescue Requests (List)

**GET** `{{base_url}}/api/rescue/citizen/requests?page=0&size=20`

**Headers:**
```
Authorization: Bearer {{token}}
```

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `sort`: Sort field (optional)

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "code": "RR202412151234",
      "status": "PENDING",
      ...
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

### 3.3 Get Rescue Request by ID

**GET** `{{base_url}}/api/rescue/citizen/requests/1`

**Headers:**
```
Authorization: Bearer {{token}}
```

**Response:** (Includes attachments and timeline)
```json
{
  "id": 1,
  "code": "RR202412151234",
  "status": "PENDING",
  "attachments": [
    {
      "id": 1,
      "fileUrl": "https://example.com/image1.jpg",
      "fileType": "IMAGE",
      "createdAt": "2024-12-15T10:30:00"
    }
  ],
  "timeline": [
    {
      "id": 1,
      "actorId": 1,
      "actorName": "Nguyễn Văn A",
      "eventType": "STATUS_CHANGE",
      "fromStatus": null,
      "toStatus": "PENDING",
      "note": "Yêu cầu cứu hộ được tạo",
      "createdAt": "2024-12-15T10:30:00"
    }
  ],
  ...
}
```

### 3.4 Update Rescue Request

**PUT** `{{base_url}}/api/rescue/citizen/requests/1`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {{token}}
```

**Body (JSON):** (All fields optional)
```json
{
  "affectedPeopleCount": 5,
  "description": "Cập nhật: Nước đã dâng cao hơn, cần cứu hộ ngay lập tức",
  "addressText": "123 Đường ABC, Phường XYZ, Quận 1, TP.HCM",
  "priority": "HIGH"
}
```

### 3.5 Cancel Rescue Request

**DELETE** `{{base_url}}/api/rescue/citizen/requests/1`

**Headers:**
```
Authorization: Bearer {{token}}
```

**Response:**
```json
{
  "message": "Yêu cầu cứu hộ đã được hủy"
}
```

### 3.6 Add Note to Rescue Request

**POST** `{{base_url}}/api/rescue/citizen/requests/1/notes`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {{token}}
```

**Body (JSON):**
```json
{
  "note": "Cập nhật tình hình: Nước đã rút một phần, nhưng vẫn cần hỗ trợ"
}
```

---

## Step 4: Coordinator Rescue Endpoints

**Note**: You need to login with a COORDINATOR account. If you don't have one, you may need to create it in the database or through an admin endpoint.

### 4.1 Get All Rescue Requests (with filters)

**GET** `{{base_url}}/api/rescue/coordinator/requests?status=PENDING&page=0&size=20`

**Headers:**
```
Authorization: Bearer {{token}}
```

**Query Parameters:**
- `status`: Filter by status (optional) - `PENDING`, `VERIFIED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `DUPLICATE`
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)

**If no status provided**, returns pending requests ordered by priority.

### 4.2 Get Rescue Request by ID

**GET** `{{base_url}}/api/rescue/coordinator/requests/1`

**Headers:**
```
Authorization: Bearer {{token}}
```

### 4.3 Get Rescue Request by Code

**GET** `{{base_url}}/api/rescue/coordinator/requests/code/RR202412151234`

**Headers:**
```
Authorization: Bearer {{token}}
```

### 4.4 Verify Rescue Request

**POST** `{{base_url}}/api/rescue/coordinator/requests/1/verify`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {{token}}
```

**Body (JSON):**
```json
{
  "locationVerified": true,
  "note": "Đã xác minh địa điểm, tình hình khẩn cấp"
}
```

**Note**: If `locationVerified` is `true`, status automatically changes to `VERIFIED`.

### 4.5 Change Priority

**PUT** `{{base_url}}/api/rescue/coordinator/requests/1/priority`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {{token}}
```

**Body (JSON):**
```json
{
  "priority": "HIGH"
}
```

### 4.6 Mark as Duplicate

**POST** `{{base_url}}/api/rescue/coordinator/requests/2/duplicate`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {{token}}
```

**Body (JSON):**
```json
{
  "masterRequestId": 1,
  "note": "Yêu cầu trùng lặp với yêu cầu RR202412151234"
}
```

### 4.7 Change Status

**PUT** `{{base_url}}/api/rescue/coordinator/requests/1/status?status=IN_PROGRESS&note=Đã phân công đội cứu hộ`

**Headers:**
```
Authorization: Bearer {{token}}
```

**Query Parameters:**
- `status`: New status (required)
- `note`: Optional note

### 4.8 Add Note

**POST** `{{base_url}}/api/rescue/coordinator/requests/1/notes`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {{token}}
```

**Body (JSON):**
```json
{
  "note": "Đã liên hệ với đội cứu hộ, sẽ đến trong 30 phút"
}
```

---

## Step 5: Rescuer Task Endpoints

**Note**: You need to login with a RESCUER account.

### 5.1 Get My Tasks

**GET** `{{base_url}}/api/rescue/rescuer/tasks?status=IN_PROGRESS&page=0&size=20`

**Headers:**
```
Authorization: Bearer {{token}}
```

**Query Parameters:**
- `status`: Filter by status (optional)
- If no status provided, returns `IN_PROGRESS` tasks by default

### 5.2 Get Task by ID

**GET** `{{base_url}}/api/rescue/rescuer/tasks/1`

**Headers:**
```
Authorization: Bearer {{token}}
```

### 5.3 Update Task Status

**PUT** `{{base_url}}/api/rescue/rescuer/tasks/1/status?status=COMPLETED&note=Đã hoàn thành cứu hộ, tất cả người dân đã an toàn`

**Headers:**
```
Authorization: Bearer {{token}}
```

**Query Parameters:**
- `status`: New status (required)
- `note`: Optional note

### 5.4 Add Note to Task

**POST** `{{base_url}}/api/rescue/rescuer/tasks/1/notes`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {{token}}
```

**Body (JSON):**
```json
{
  "note": "Đã đến hiện trường, đang tiến hành cứu hộ"
}
```

---

## Common Status Values

- `PENDING`: Yêu cầu mới tạo, chưa được xác minh
- `VERIFIED`: Đã được xác minh
- `IN_PROGRESS`: Đang được xử lý
- `COMPLETED`: Đã hoàn thành
- `CANCELLED`: Đã bị hủy
- `DUPLICATE`: Bị đánh dấu là trùng lặp

## Common Priority Values

- `HIGH`: Ưu tiên cao
- `MEDIUM`: Ưu tiên trung bình
- `LOW`: Ưu tiên thấp

---

## Testing Workflow Example

1. **Register/Login** → Get JWT token
2. **Create Rescue Request** (as Citizen) → Get request ID
3. **View Request** → Check status is PENDING
4. **Login as Coordinator** → Get coordinator token
5. **Verify Request** → Status changes to VERIFIED
6. **Change Status to IN_PROGRESS** → Assign to rescuer
7. **Login as Rescuer** → Get rescuer token
8. **View Tasks** → See assigned tasks
9. **Update Status to COMPLETED** → Mark task as done

---

## Troubleshooting

### 401 Unauthorized
- Check if JWT token is included in `Authorization` header
- Token format: `Bearer <your-token>`
- Token might be expired, try logging in again

### 403 Forbidden
- User role doesn't have permission for this endpoint
- Check user role in login response

### 404 Not Found
- Check if the request ID exists
- Verify the endpoint URL is correct

### 400 Bad Request
- Check request body format (JSON)
- Validate required fields are present
- Check enum values (status, priority, etc.)

### 500 Internal Server Error
- Check server logs
- Verify database connection
- Check if all required data exists (e.g., user, roles)

---

## Postman Collection Setup Tips

1. **Create a Collection** named "Flood Rescue API"
2. **Add Environment Variables**:
   - `base_url`: `http://localhost:8080`
   - `token`: (auto-set after login)
3. **Add Pre-request Script** to auto-include token:
   ```javascript
   if (pm.environment.get("token")) {
       pm.request.headers.add({
           key: "Authorization",
           value: "Bearer " + pm.environment.get("token")
       });
   }
   ```
4. **Add Tests** to save token after login:
   ```javascript
   if (pm.response.code === 200) {
       var jsonData = pm.response.json();
       if (jsonData.token) {
           pm.environment.set("token", jsonData.token);
       }
   }
   ```
