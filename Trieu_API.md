# Trieu_API

## 1) Kết quả quét + xác minh runtime (2026-03-08)

Đã chạy test API thật trên backend `http://127.0.0.1:18080` và DB MySQL `127.0.0.1:3307`.

- Citizen đăng ký + đăng nhập + tạo yêu cầu cứu hộ: OK, ghi thật vào `users`, `rescue_requests`.
- Coordinator verify + prioritize + tạo task group + assign team: OK, ghi thật vào `rescue_requests`, `task_groups`, `rescue_assignments`.
- Rescuer cập nhật tiến độ task group + request: OK, ghi thật vào `task_groups`, `rescue_requests`, `rescue_request_timeline`.
- Citizen gửi feedback: OK, ghi thật vào `system_feedbacks`.
- Điểm lỗi runtime ghi nhận: `POST /api/rescue/citizen/requests/{id}/confirm-result` trả về
  `"Chỉ có thể xác nhận khi yêu cầu đã ở trạng thái COMPLETED"` dù request đã `COMPLETED` sau bước rescuer (cần debug điều kiện nghiệp vụ trong service).

Artifact test thực tế:
- `REQ_ID=2`, `REQ_CODE=RR202603084162`
- `TG_ID=1`
- test citizen: `trieu_test_1772982960@mail.com`

## 2) Các nút/API có nguy cơ không lưu DB thật (frontend lệch backend)

### 2.1 Lệch endpoint rõ ràng

1. `AdminTeamAssetPage` gọi endpoint cũ không tồn tại ở backend hiện tại
- FE: `/api/admin/delete-user/{id}`, `/api/admin/reset-password/{id}`, `/api/admin/update-status/{id}`
- Tham chiếu: `flood-rescue-frontend/src/pages/admin/AdminTeamAssetPage.jsx` (dòng 102, 129, 154)
- BE thật: `/api/admin/users/{id}`, `/api/admin/users/{id}/reset-password`, `/api/admin/users/{id}/status`
- Tham chiếu: `flood-rescue-backend3/src/main/java/com/floodrescue/module/admin/controller/AdminApiController.java` (dòng 260, 273, 290)
- Kết luận: các nút này bấm lên sẽ không đi vào nhánh ghi DB đúng.

2. API Asset update/delete trong frontend không có endpoint backend tương ứng
- FE: `PUT /api/assets/{id}`, `DELETE /api/assets/{id}`
- Tham chiếu: `flood-rescue-frontend/src/features/assets/api.js` (dòng 70, 75)
- BE hiện chỉ có `POST /api/assets`, `GET /api/assets`, `GET /api/assets/{id}`, `PUT /api/assets/{id}/status`
- Tham chiếu: `flood-rescue-backend3/src/main/java/com/floodrescue/module/asset/controller/AssetController.java` (dòng 26, 36, 44, 50)
- Kết luận: nếu UI dùng `updateAsset`/`deleteAsset` thì sẽ không lưu DB.

3. `approveReliefRequest` ở frontend gọi endpoint không tồn tại
- FE: `PUT /api/relief/requests/{id}/approve`
- Tham chiếu: `flood-rescue-frontend/src/features/relief/api.js` (dòng 260)
- BE: dùng `PUT /api/relief/requests/{id}/approve-dispatch`
- Tham chiếu: `flood-rescue-backend3/src/main/java/com/floodrescue/module/relief/controller/ReliefRequestController.java` (dòng 136)
- Kết luận: nút nào gọi hàm này sẽ không cập nhật DB.

### 2.2 Rủi ro cấu hình môi trường

Nhiều trang admin hardcode `http://localhost:8080/api/admin` thay vì dùng config chung (`httpClient`/env):
- `UserManagementPage.jsx` dòng 4
- `RolesPermissionsPage.jsx` dòng 4
- `SystemCatalogPage.jsx` dòng 27
- `AuditLogsPage.jsx` dòng 4
- `NotificationTemplatesPage.jsx` (tương tự)

Kết luận: khi backend chạy cổng khác (vd `18080`) thì nút vẫn gọi API nhưng fail kết nối, nên không có ghi DB thật.

## 3) API write-path chính đã xác nhận ghi DB thật

1. Auth
- `POST /api/auth/register` -> `users`
- `POST /api/auth/login` -> cập nhật `users.last_login_at`

2. Rescue Citizen
- `POST /api/rescue/citizen/requests` -> `rescue_requests`, `rescue_request_timeline`, (attachments nếu có)
- `PUT/DELETE /api/rescue/citizen/requests/{id}` -> cập nhật trạng thái request

3. Rescue Coordinator
- `POST /api/rescue/coordinator/requests/{id}/verify` -> cập nhật `rescue_requests` + timeline
- `PUT /api/rescue/coordinator/requests/{id}/priority` -> cập nhật `rescue_requests.priority`
- `POST /api/rescue/coordinator/task-groups` -> `task_groups`, `task_group_requests`, timeline
- `POST /api/rescue/coordinator/task-groups/assign` -> `rescue_assignments`, `task_groups.assigned_team_id`, cập nhật `rescue_requests.status`

4. Rescue Rescuer
- `PUT /api/rescue/rescuer/task-groups/{id}/status` -> cập nhật `task_groups.status`, timeline
- `PUT /api/rescue/rescuer/tasks/{id}/status` -> cập nhật `rescue_requests.status`, timeline
- `POST /api/rescue/rescuer/team-location` -> cập nhật `teams.current_*`

5. Feedback
- `POST /api/feedback/citizen` -> `system_feedbacks`

## 4) Khuyến nghị sửa ngay để tránh "nút gọi API nhưng không lưu"

1. Đổi endpoint trong `AdminTeamAssetPage.jsx` sang chuẩn `/users/{id}`.
2. Bỏ/điều chỉnh `updateAsset`, `deleteAsset` trong `features/assets/api.js` theo backend hiện có.
3. Sửa `approveReliefRequest` thành `/approve-dispatch` hoặc bỏ hàm nếu không dùng.
4. Loại hardcode `http://localhost:8080`; chuyển toàn bộ về `httpClient` + biến môi trường.
 sửa cho commit