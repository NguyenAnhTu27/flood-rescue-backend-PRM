# Trieu_Test

## Mục tiêu

Test toàn bộ luồng chính theo dữ liệu thật, xác nhận mỗi bước có ghi DB thật và nêu ảnh hưởng thay đổi sau mỗi bước.

## A. Chuẩn bị môi trường

1. Start MySQL local từ data của project
```bash
/usr/local/mysql/bin/mysqld \
  --datadir=/Users/ke/IdeaProjects/flood_rescue/.mysql-data \
  --socket=/Users/ke/IdeaProjects/flood_rescue/.mysql-run/mysql.sock \
  --port=3307 --bind-address=127.0.0.1 \
  --pid-file=/Users/ke/IdeaProjects/flood_rescue/.mysql-run/mysql.pid \
  --innodb-undo-directory=/Users/ke/IdeaProjects/flood_rescue/.mysql-data
```

2. Nạp schema + patch
```bash
for f in /Users/ke/IdeaProjects/flood_rescue/MySQL/flood_rescue_*.sql /Users/ke/IdeaProjects/flood_rescue/MySQL/patch_*.sql; do
  sed "/GTID_PURGED/d" "$f" | /usr/local/mysql/bin/mysql -h 127.0.0.1 -P 3307 -u root flood_rescue --force
done
```

3. Start backend
```bash
cd /Users/ke/IdeaProjects/flood_rescue/flood-rescue-backend3
SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3307/flood_rescue?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true' \
SPRING_DATASOURCE_USERNAME='root' \
SPRING_DATASOURCE_PASSWORD='' \
SERVER_PORT='18080' \
sh mvnw -q spring-boot:run
```

## B. Luồng test chính và ảnh hưởng DB mỗi bước

### B1) Citizen đăng ký + tạo yêu cầu cứu hộ

API:
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/rescue/citizen/requests`

SQL kiểm tra:
```sql
SELECT id,email,phone,role_id,status FROM users ORDER BY id DESC LIMIT 1;
SELECT id,code,status,priority,citizen_id,affected_people_count FROM rescue_requests ORDER BY id DESC LIMIT 1;
```

Ảnh hưởng kỳ vọng:
- `users`: thêm 1 citizen mới.
- `rescue_requests`: thêm 1 bản ghi `PENDING`.
- `rescue_request_timeline`: thêm event tạo yêu cầu.

### B2) Coordinator verify + prioritize

API:
- `POST /api/rescue/coordinator/requests/{id}/verify`
- `PUT /api/rescue/coordinator/requests/{id}/priority`

SQL kiểm tra:
```sql
SELECT id,status,location_verified,priority FROM rescue_requests WHERE id = ?;
```

Ảnh hưởng kỳ vọng:
- `rescue_requests.status`: `PENDING -> VERIFIED`
- `rescue_requests.priority`: cập nhật theo lựa chọn.
- timeline thêm event verify/prioritize.

### B3) Coordinator tạo TaskGroup + phân công team

API:
- `POST /api/rescue/coordinator/task-groups`
- `POST /api/rescue/coordinator/task-groups/assign`

SQL kiểm tra:
```sql
SELECT id,status,assigned_team_id FROM task_groups WHERE id = ?;
SELECT id,task_group_id,team_id,is_active FROM rescue_assignments WHERE task_group_id = ? ORDER BY id DESC LIMIT 1;
SELECT id,status FROM rescue_requests WHERE id = ?;
```

Ảnh hưởng kỳ vọng:
- `task_groups`: thêm group mới.
- `rescue_assignments`: thêm bản ghi phân công.
- `task_groups.status`: `NEW -> ASSIGNED`.
- `rescue_requests.status`: `VERIFIED -> ASSIGNED`.

### B4) Rescuer cập nhật tiến độ

API:
- `PUT /api/rescue/rescuer/task-groups/{id}/status?status=IN_PROGRESS`
- `PUT /api/rescue/rescuer/tasks/{id}/status?status=IN_PROGRESS`
- `PUT /api/rescue/rescuer/tasks/{id}/status?status=COMPLETED`

SQL kiểm tra:
```sql
SELECT id,status FROM task_groups WHERE id = ?;
SELECT id,status,rescue_result_confirmation_status FROM rescue_requests WHERE id = ?;
SELECT COUNT(*) FROM rescue_request_timeline WHERE rescue_request_id = ?;
```

Ảnh hưởng kỳ vọng:
- `task_groups.status`: `ASSIGNED -> IN_PROGRESS`.
- `rescue_requests.status`: `ASSIGNED -> IN_PROGRESS -> COMPLETED`.
- timeline tăng thêm event theo từng lần đổi trạng thái.

### B5) Citizen xác nhận kết quả + gửi feedback

API:
- `POST /api/rescue/citizen/requests/{id}/confirm-result`
- `POST /api/feedback/citizen`

SQL kiểm tra:
```sql
SELECT id,citizen_id,rating,rescued_confirmed,relief_confirmed FROM system_feedbacks ORDER BY id DESC LIMIT 1;
```

Ảnh hưởng kỳ vọng:
- `system_feedbacks`: thêm bản ghi phản hồi.
- Với `confirm-result`: theo nghiệp vụ sẽ cập nhật xác nhận kết quả cứu hộ; nếu fail thì cần kiểm tra điều kiện service.

## C. Kết quả chạy thật đã ghi nhận (2026-03-08)

- Đã chạy full flow với:
  - `REQ_ID=2`, `REQ_CODE=RR202603084162`
  - `TG_ID=1`
  - Citizen test: `trieu_test_1772982960@mail.com`
- DB ghi nhận đúng ở các bảng: `users`, `rescue_requests`, `task_groups`, `rescue_assignments`, `rescue_request_timeline`, `system_feedbacks`.
- Lỗi cần xử lý: `confirm-result` trả thông báo chưa đúng trạng thái thực tế (request đã `COMPLETED`).

## D. Checklist regression toàn hệ thống

1. Auth: register/login/me cho từng role.
2. Citizen rescue: create/update/cancel/list/detail/upload attachment.
3. Coordinator: queue/verify/prioritize/duplicate/create-group/assign/block-unblock.
4. Rescuer: dashboard/tasks/task-groups/update status/escalate/team-location.
5. Manager inventory-relief: receipt/issue/approve-dispatch/reject.
6. Admin: user CRUD, permissions, catalogs, templates, settings, audit logs.
7. Feedback + Notifications: tạo, đọc, đánh dấu đã đọc, queue emergency.
8. DB consistency: timeline, assignment active flag, FK, index scan key tables.
