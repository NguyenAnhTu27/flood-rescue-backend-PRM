package com.floodrescue.module.admin.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiController {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return Long.parseLong(userDetails.getUsername());
    }

    private String getActorName(Long userId) {
        if (userId == null) return "SYSTEM";
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT full_name FROM users WHERE id = ?",
                    String.class,
                    userId
            );
        } catch (EmptyResultDataAccessException ex) {
            return "SYSTEM";
        }
    }

    private void writeAudit(
            Long actorId,
            String action,
            String entityType,
            Long entityId,
            String level,
            String detail,
            String target,
            HttpServletRequest request,
            Object oldData,
            Object newData
    ) {
        if (actorId == null) return;

        String actor = getActorName(actorId);
        String ip = request != null ? request.getRemoteAddr() : null;
        String ua = request != null ? request.getHeader("User-Agent") : null;
        String oldJson = toJson(oldData);
        String newJson = toJson(newData);

        jdbcTemplate.update(
                "INSERT INTO audit_logs(actor_id, action, entity_type, entity_id, old_data, new_data, ip_address, user_agent, created_at, actor, detail, level, target) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?, ?, ?)",
                actorId, action, entityType, entityId, oldJson, newJson, ip, ua, actor, detail, level, target
        );
    }

    private String toJson(Object data) {
        if (data == null) return null;
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Map<String, String> getAllSystemSettings() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT COALESCE(setting_key, key_name) AS k, COALESCE(setting_value, value_text) AS v FROM system_settings"
        );
        Map<String, String> values = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String key = row.get("k") == null ? null : String.valueOf(row.get("k"));
            if (key == null || key.isBlank()) continue;
            values.put(key, row.get("v") == null ? "" : String.valueOf(row.get("v")));
        }
        return values;
    }

    private void upsertSystemSetting(String key, String value, Long actorId) {
        jdbcTemplate.update(
                "INSERT INTO system_settings(setting_key, setting_value, key_name, value_text, value_type, updated_by, updated_at) " +
                        "VALUES (?, ?, ?, ?, 'STRING', ?, NOW()) " +
                        "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), value_text = VALUES(value_text), updated_by = VALUES(updated_by), updated_at = NOW()",
                key, value, key, value, actorId
        );
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        Long active = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE status = 1", Long.class);
        Long locked = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE status <> 1", Long.class);

        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", total == null ? 0 : total);
        data.put("activeUsers", active == null ? 0 : active);
        data.put("lockedUsers", locked == null ? 0 : locked);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer roleId
    ) {
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(0, page);
        int offset = safePage * safeSize;

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (roleId != null) {
            where.append(" AND u.role_id = ? ");
            params.add(roleId);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append(" AND (u.full_name LIKE ? OR u.email LIKE ? OR u.phone LIKE ? OR CAST(u.id AS CHAR) LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        Long totalUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users u " + where,
                Long.class,
                params.toArray()
        );

        List<Object> queryParams = new ArrayList<>(params);
        queryParams.add(safeSize);
        queryParams.add(offset);

        List<Map<String, Object>> users = jdbcTemplate.query(
                "SELECT u.id, u.full_name, u.email, u.phone, u.status, u.role_id, r.code AS role_code, u.created_at " +
                        "FROM users u JOIN roles r ON r.id = u.role_id " +
                        where +
                        " ORDER BY u.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getLong("id"));
                    item.put("fullName", rs.getString("full_name"));
                    item.put("email", rs.getString("email"));
                    item.put("phone", rs.getString("phone"));
                    item.put("status", rs.getInt("status") == 1 ? "ACTIVE" : "LOCKED");
                    item.put("roleId", rs.getInt("role_id"));
                    item.put("role", rs.getString("role_code"));
                    item.put("createdAt", rs.getTimestamp("created_at"));
                    return item;
                },
                queryParams.toArray()
        );

        int totalPages = (int) Math.ceil((totalUsers == null ? 0 : totalUsers) / (double) safeSize);
        if (totalPages <= 0) totalPages = 1;

        Map<String, Object> response = new HashMap<>();
        response.put("users", users);
        response.put("totalUsers", totalUsers == null ? 0 : totalUsers);
        response.put("totalPages", totalPages);
        response.put("page", safePage);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create-user")
    public ResponseEntity<Map<String, Object>> createUser(
            @RequestBody Map<String, Object> payload,
            Authentication authentication,
            HttpServletRequest request
    ) {
        String fullName = String.valueOf(payload.getOrDefault("fullName", "")).trim();
        String email = String.valueOf(payload.getOrDefault("email", "")).trim().toLowerCase();
        String phone = String.valueOf(payload.getOrDefault("phone", "")).trim();
        String password = String.valueOf(payload.getOrDefault("password", ""));
        Integer roleId = payload.get("roleId") == null ? null : Integer.parseInt(String.valueOf(payload.get("roleId")));
        Long teamId = payload.get("teamId") == null ? null : Long.parseLong(String.valueOf(payload.get("teamId")));

        if (fullName.isEmpty() || password.isEmpty() || roleId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu dữ liệu bắt buộc"));
        }

        if (!email.isEmpty()) {
            Integer existed = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
            if (existed != null && existed > 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email đã tồn tại"));
            }
        }
        if (!phone.isEmpty()) {
            Integer existed = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE phone = ?", Integer.class, phone);
            if (existed != null && existed > 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Số điện thoại đã tồn tại"));
            }
        }

        jdbcTemplate.update(
                "INSERT INTO users(role_id, team_id, full_name, phone, email, password_hash, status, last_login_at, created_at, updated_at, failed_login_attempts, locked_at, temp_locked_until, is_leader) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 1, NULL, NOW(), NOW(), 0, NULL, NULL, b'0')",
                roleId, teamId, fullName, phone.isEmpty() ? null : phone, email.isEmpty() ? null : email,
                passwordEncoder.encode(password)
        );

        Long actorId = getCurrentUserId(authentication);
        writeAudit(actorId, "CREATE_USER", "USER", null, "SUCCESS", "Tạo tài khoản mới", email, request, null, payload);

        return ResponseEntity.ok(Map.of("message", "Tạo tài khoản thành công"));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            Authentication authentication,
            HttpServletRequest request
    ) {
        Map<String, Object> before = jdbcTemplate.queryForMap("SELECT * FROM users WHERE id = ?", id);

        String fullName = String.valueOf(payload.getOrDefault("fullName", before.get("full_name"))).trim();
        String email = payload.get("email") == null ? null : String.valueOf(payload.get("email")).trim().toLowerCase();
        String phone = payload.get("phone") == null ? null : String.valueOf(payload.get("phone")).trim();
        Integer roleId = payload.get("roleId") == null ? ((Number) before.get("role_id")).intValue() : Integer.parseInt(String.valueOf(payload.get("roleId")));
        String status = String.valueOf(payload.getOrDefault("status", ((Number) before.get("status")).intValue() == 1 ? "ACTIVE" : "LOCKED"));
        int statusVal = "ACTIVE".equalsIgnoreCase(status) ? 1 : 0;

        jdbcTemplate.update(
                "UPDATE users SET full_name = ?, email = ?, phone = ?, role_id = ?, status = ?, updated_at = NOW() WHERE id = ?",
                fullName, email, phone, roleId, statusVal, id
        );

        Long actorId = getCurrentUserId(authentication);
        writeAudit(actorId, "UPDATE_USER", "USER", id, "SUCCESS", "Cập nhật thông tin người dùng", email, request, before, payload);
        return ResponseEntity.ok(Map.of("message", "Cập nhật người dùng thành công"));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request
    ) {
        Map<String, Object> before = jdbcTemplate.queryForMap("SELECT * FROM users WHERE id = ?", id);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
        Long actorId = getCurrentUserId(authentication);
        writeAudit(actorId, "DELETE_USER", "USER", id, "WARN", "Xóa tài khoản", String.valueOf(before.get("email")), request, before, null);
        return ResponseEntity.ok(Map.of("message", "Đã xoá user"));
    }

    @PutMapping("/users/{id}/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            Authentication authentication,
            HttpServletRequest request
    ) {
        String password = String.valueOf(payload.getOrDefault("password", "")).trim();
        if (password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu mới không được để trống"));
        }
        jdbcTemplate.update("UPDATE users SET password_hash = ?, updated_at = NOW() WHERE id = ?", passwordEncoder.encode(password), id);
        Long actorId = getCurrentUserId(authentication);
        writeAudit(actorId, "RESET_PASSWORD", "USER", id, "SUCCESS", "Reset mật khẩu người dùng", null, request, null, Map.of("id", id));
        return ResponseEntity.ok(Map.of("message", "Reset password thành công"));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            Authentication authentication,
            HttpServletRequest request
    ) {
        String status = String.valueOf(payload.getOrDefault("status", "ACTIVE"));
        int statusVal = "ACTIVE".equalsIgnoreCase(status) ? 1 : 0;
        jdbcTemplate.update("UPDATE users SET status = ?, updated_at = NOW() WHERE id = ?", statusVal, id);
        Long actorId = getCurrentUserId(authentication);
        writeAudit(actorId, "UPDATE_STATUS", "USER", id, "SUCCESS", "Cập nhật trạng thái người dùng", status, request, null, payload);
        return ResponseEntity.ok(Map.of("message", "Cập nhật trạng thái thành công"));
    }

    @GetMapping("/permissions")
    public ResponseEntity<Map<String, Object>> getPermissions() {
        List<Map<String, Object>> roles = jdbcTemplate.query(
                "SELECT r.id, r.code, r.name, (SELECT COUNT(*) FROM users u WHERE u.role_id = r.id) AS user_count FROM roles r ORDER BY r.id",
                (rs, rowNum) -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("id"));
                    item.put("code", rs.getString("code"));
                    item.put("name", rs.getString("name"));
                    item.put("userCount", rs.getLong("user_count"));
                    return item;
                }
        );

        List<Map<String, Object>> permissions = jdbcTemplate.query(
                "SELECT id, code, name, module FROM permissions ORDER BY module, code",
                (rs, rowNum) -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("id"));
                    item.put("code", rs.getString("code"));
                    item.put("name", rs.getString("name"));
                    item.put("module", rs.getString("module"));
                    return item;
                }
        );

        Map<String, List<String>> rolePermissions = new HashMap<>();
        for (Map<String, Object> role : roles) {
            String roleCode = String.valueOf(role.get("code"));
            Integer roleId = (Integer) role.get("id");
            List<String> codes = jdbcTemplate.query(
                    "SELECT p.code FROM role_permissions rp JOIN permissions p ON p.id = rp.permission_id WHERE rp.role_id = ? ORDER BY p.code",
                    (rs, rowNum) -> rs.getString("code"),
                    roleId
            );
            rolePermissions.put(roleCode, codes);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("roles", roles);
        result.put("permissions", permissions);
        result.put("rolePermissions", rolePermissions);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/roles/{roleCode}/permissions")
    public ResponseEntity<Map<String, Object>> updateRolePermissions(
            @PathVariable String roleCode,
            @RequestBody Map<String, Object> payload,
            Authentication authentication,
            HttpServletRequest request
    ) {
        Integer roleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE code = ?", Integer.class, roleCode);
        if (roleId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vai trò không tồn tại"));
        }

        @SuppressWarnings("unchecked")
        List<String> codes = payload.get("permissions") instanceof List
                ? ((List<Object>) payload.get("permissions")).stream().map(String::valueOf).collect(Collectors.toList())
                : new ArrayList<>();

        jdbcTemplate.update("DELETE FROM role_permissions WHERE role_id = ?", roleId);

        if (!codes.isEmpty()) {
            String placeholders = codes.stream().map(c -> "?").collect(Collectors.joining(","));
            List<Map<String, Object>> perms = jdbcTemplate.queryForList(
                    "SELECT id, code FROM permissions WHERE code IN (" + placeholders + ")",
                    codes.toArray()
            );
            for (Map<String, Object> p : perms) {
                Integer permissionId = ((Number) p.get("id")).intValue();
                jdbcTemplate.update(
                        "INSERT INTO role_permissions(role_id, permission_id, created_at) VALUES (?, ?, ?)",
                        roleId, permissionId, Timestamp.valueOf(LocalDateTime.now())
                );
            }
        }

        Long actorId = getCurrentUserId(authentication);
        writeAudit(actorId, "UPDATE_ROLE_PERMISSIONS", "ROLE", roleId.longValue(), "SUCCESS", "Cập nhật phân quyền", roleCode, request, null, payload);
        return ResponseEntity.ok(Map.of("message", "Cập nhật phân quyền thành công"));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword
    ) {
        int safeSize = Math.max(1, Math.min(size, 200));
        int safePage = Math.max(0, page);
        int offset = safePage * safeSize;

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (action != null && !action.isBlank()) {
            where.append(" AND action = ? ");
            params.add(action);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (actor LIKE ? OR target LIKE ? OR action LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }

        List<Object> queryParams = new ArrayList<>(params);
        queryParams.add(safeSize);
        queryParams.add(offset);
        List<Map<String, Object>> items = jdbcTemplate.query(
                "SELECT id, created_at, action, actor, target, level, detail FROM audit_logs " +
                        where + " ORDER BY id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getLong("id"));
                    item.put("createdAt", rs.getTimestamp("created_at"));
                    item.put("action", rs.getString("action"));
                    item.put("actor", rs.getString("actor"));
                    item.put("target", rs.getString("target"));
                    item.put("level", rs.getString("level"));
                    item.put("detail", rs.getString("detail"));
                    return item;
                },
                queryParams.toArray()
        );
        return ResponseEntity.ok(Map.of("items", items, "page", safePage, "size", safeSize));
    }

    @GetMapping("/system-settings")
    public ResponseEntity<Map<String, Object>> getSystemSettings() {
        return ResponseEntity.ok(Map.of("values", getAllSystemSettings()));
    }

    @PutMapping("/system-settings")
    public ResponseEntity<Map<String, Object>> updateSystemSettings(
            @RequestBody Map<String, Object> payload,
            Authentication authentication,
            HttpServletRequest request
    ) {
        Long actorId = getCurrentUserId(authentication);
        for (Map.Entry<String, Object> e : payload.entrySet()) {
            String key = e.getKey();
            String value = String.valueOf(e.getValue());
            upsertSystemSetting(key, value, actorId);
        }
        writeAudit(actorId, "UPDATE_SYSTEM_SETTINGS", "SYSTEM", null, "SUCCESS", "Cập nhật cấu hình hệ thống", null, request, null, payload);
        return ResponseEntity.ok(Map.of("message", "Đã lưu cấu hình"));
    }

    @GetMapping("/content-pages")
    public ResponseEntity<Map<String, Object>> getContentPages() {
        Map<String, String> values = getAllSystemSettings();

        Map<String, Object> pages = new HashMap<>();
        pages.put("termsTitle", values.getOrDefault("legalTermsTitle", "Điều khoản sử dụng"));
        pages.put("termsContent", values.getOrDefault("legalTermsContent", ""));
        pages.put("termsLabel", values.getOrDefault("footerTermsLabel", "Điều khoản sử dụng"));
        pages.put("privacyTitle", values.getOrDefault("privacyPolicyTitle", "Chính sách bảo mật"));
        pages.put("privacyContent", values.getOrDefault("privacyPolicyContent", ""));
        pages.put("privacyLabel", values.getOrDefault("footerPrivacyLabel", "Chính sách bảo mật"));
        pages.put("supportTitle", values.getOrDefault("supportPageTitle", "Liên hệ hỗ trợ"));
        pages.put("supportContent", values.getOrDefault("supportPageContent", ""));
        pages.put("supportLabel", values.getOrDefault("footerSupportLabel", "Liên hệ hỗ trợ"));

        return ResponseEntity.ok(pages);
    }

    @PutMapping("/content-pages")
    public ResponseEntity<Map<String, Object>> updateContentPages(
            @RequestBody Map<String, Object> payload,
            Authentication authentication,
            HttpServletRequest request
    ) {
        Long actorId = getCurrentUserId(authentication);

        upsertSystemSetting("legalTermsTitle", String.valueOf(payload.getOrDefault("termsTitle", "Điều khoản sử dụng")), actorId);
        upsertSystemSetting("legalTermsContent", String.valueOf(payload.getOrDefault("termsContent", "")), actorId);
        upsertSystemSetting("privacyPolicyTitle", String.valueOf(payload.getOrDefault("privacyTitle", "Chính sách bảo mật")), actorId);
        upsertSystemSetting("privacyPolicyContent", String.valueOf(payload.getOrDefault("privacyContent", "")), actorId);
        upsertSystemSetting("supportPageTitle", String.valueOf(payload.getOrDefault("supportTitle", "Liên hệ hỗ trợ")), actorId);
        upsertSystemSetting("supportPageContent", String.valueOf(payload.getOrDefault("supportContent", "")), actorId);
        upsertSystemSetting("footerTermsLabel", String.valueOf(payload.getOrDefault("termsLabel", "Điều khoản sử dụng")), actorId);
        upsertSystemSetting("footerPrivacyLabel", String.valueOf(payload.getOrDefault("privacyLabel", "Chính sách bảo mật")), actorId);
        upsertSystemSetting("footerSupportLabel", String.valueOf(payload.getOrDefault("supportLabel", "Liên hệ hỗ trợ")), actorId);

        // Keep footer links pointing to internal pages by default.
        upsertSystemSetting("footerTermsUrl", String.valueOf(payload.getOrDefault("footerTermsUrl", "/dieu-khoan-su-dung")), actorId);
        upsertSystemSetting("footerPrivacyUrl", String.valueOf(payload.getOrDefault("footerPrivacyUrl", "/chinh-sach-bao-mat")), actorId);
        upsertSystemSetting("footerSupportUrl", String.valueOf(payload.getOrDefault("footerSupportUrl", "/lien-he-ho-tro")), actorId);

        writeAudit(actorId, "UPDATE_CONTENT_PAGES", "SYSTEM", null, "SUCCESS", "Cập nhật nội dung trang công khai", null, request, null, payload);
        return ResponseEntity.ok(Map.of("message", "Đã lưu nội dung trang"));
    }

    @GetMapping("/notification-templates")
    public ResponseEntity<Map<String, Object>> getNotificationTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(0, page);
        int offset = safePage * safeSize;

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (code LIKE ? OR template_key LIKE ? OR title LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_templates " + where,
                Long.class,
                params.toArray()
        );
        List<Object> q = new ArrayList<>(params);
        q.add(safeSize);
        q.add(offset);
        List<Map<String, Object>> items = jdbcTemplate.query(
                "SELECT id, code, template_key, title, content, channel, IF(COALESCE(is_active,0)=1 OR COALESCE(active,0)=1,1,0) AS active_flag, created_at, updated_at " +
                        "FROM notification_templates " + where +
                        " ORDER BY id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("id"));
                    item.put("code", rs.getString("code"));
                    item.put("templateKey", rs.getString("template_key"));
                    item.put("title", rs.getString("title"));
                    item.put("content", rs.getString("content"));
                    item.put("channel", rs.getString("channel"));
                    item.put("active", rs.getInt("active_flag") == 1);
                    item.put("createdAt", rs.getTimestamp("created_at"));
                    item.put("updatedAt", rs.getTimestamp("updated_at"));
                    return item;
                },
                q.toArray()
        );

        Long activeCnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_templates WHERE IF(COALESCE(is_active,0)=1 OR COALESCE(active,0)=1,1,0)=1",
                Long.class
        );
        String topChannel = jdbcTemplate.query(
                "SELECT channel, COUNT(*) c FROM notification_templates GROUP BY channel ORDER BY c DESC LIMIT 1",
                rs -> rs.next() ? rs.getString("channel") : "N/A"
        );

        int totalPages = (int) Math.ceil((total == null ? 0 : total) / (double) safeSize);
        if (totalPages <= 0) totalPages = 1;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTemplates", total == null ? 0 : total);
        stats.put("activeTemplates", activeCnt == null ? 0 : activeCnt);
        stats.put("topChannel", topChannel == null ? "N/A" : topChannel);

        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("page", safePage);
        response.put("totalPages", totalPages);
        response.put("totalItems", total == null ? 0 : total);
        response.put("stats", stats);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/notification-templates")
    public ResponseEntity<Map<String, Object>> createNotificationTemplate(
            @RequestBody Map<String, Object> payload,
            Authentication authentication,
            HttpServletRequest request
    ) {
        String code = String.valueOf(payload.getOrDefault("code", "")).trim().toUpperCase();
        String templateKey = String.valueOf(payload.getOrDefault("templateKey", code)).trim().toUpperCase();
        String title = String.valueOf(payload.getOrDefault("title", "")).trim();
        String channel = String.valueOf(payload.getOrDefault("channel", "WEB")).trim().toUpperCase();
        String content = String.valueOf(payload.getOrDefault("content", "")).trim();
        boolean active = Boolean.parseBoolean(String.valueOf(payload.getOrDefault("active", "true")));

        jdbcTemplate.update(
                "INSERT INTO notification_templates(code, template_key, title, content, channel, is_active, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                code, templateKey, title, content, channel, active ? 1 : 0, active ? 1 : 0
        );
        Long actorId = getCurrentUserId(authentication);
        writeAudit(actorId, "CREATE_NOTIFICATION_TEMPLATE", "NOTIFICATION_TEMPLATE", null, "SUCCESS", "Tạo mẫu thông báo", code, request, null, payload);
        return ResponseEntity.ok(Map.of("message", "Tạo mẫu thông báo thành công"));
    }

    @PutMapping("/notification-templates/{id}")
    public ResponseEntity<Map<String, Object>> updateNotificationTemplate(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> payload,
            Authentication authentication,
            HttpServletRequest request
    ) {
        jdbcTemplate.update(
                "UPDATE notification_templates SET title = ?, content = ?, channel = ?, is_active = ?, active = ?, updated_at = NOW() WHERE id = ?",
                String.valueOf(payload.getOrDefault("title", "")).trim(),
                String.valueOf(payload.getOrDefault("content", "")).trim(),
                String.valueOf(payload.getOrDefault("channel", "WEB")).trim().toUpperCase(),
                Boolean.parseBoolean(String.valueOf(payload.getOrDefault("active", "true"))) ? 1 : 0,
                Boolean.parseBoolean(String.valueOf(payload.getOrDefault("active", "true"))) ? 1 : 0,
                id
        );
        Long actorId = getCurrentUserId(authentication);
        writeAudit(actorId, "UPDATE_NOTIFICATION_TEMPLATE", "NOTIFICATION_TEMPLATE", id.longValue(), "SUCCESS", "Cập nhật mẫu thông báo", String.valueOf(id), request, null, payload);
        return ResponseEntity.ok(Map.of("message", "Cập nhật mẫu thông báo thành công"));
    }

    @PatchMapping("/notification-templates/{id}/active")
    public ResponseEntity<Map<String, Object>> toggleNotificationTemplateActive(@PathVariable Integer id) {
        jdbcTemplate.update(
                "UPDATE notification_templates SET is_active = IF(COALESCE(is_active,0)=1 OR COALESCE(active,0)=1, 0, 1), active = IF(COALESCE(is_active,0)=1 OR COALESCE(active,0)=1, 0, 1), updated_at = NOW() WHERE id = ?",
                id
        );
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật trạng thái"));
    }

    @DeleteMapping("/notification-templates/{id}")
    public ResponseEntity<Map<String, Object>> deleteNotificationTemplate(@PathVariable Integer id) {
        jdbcTemplate.update("DELETE FROM notification_templates WHERE id = ?", id);
        return ResponseEntity.ok(Map.of("message", "Xóa mẫu thông báo thành công"));
    }

    @GetMapping("/catalogs")
    public ResponseEntity<List<Map<String, Object>>> getCatalogs() {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "SELECT id, group_code, code, name, active, created_at, updated_at FROM admin_catalogs ORDER BY group_code, code",
                (rs, rowNum) -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("groupCode", rs.getString("group_code"));
                    row.put("code", rs.getString("code"));
                    row.put("name", rs.getString("name"));
                    row.put("nameVn", rs.getString("name"));
                    row.put("active", rs.getBoolean("active"));
                    row.put("createdAt", rs.getTimestamp("created_at"));
                    row.put("updatedAt", rs.getTimestamp("updated_at"));
                    return row;
                }
        );
        return ResponseEntity.ok(rows);
    }

    @PostMapping("/catalogs")
    public ResponseEntity<Map<String, Object>> createCatalog(@RequestBody Map<String, Object> payload) {
        String groupCode = String.valueOf(payload.getOrDefault("groupCode", "")).trim().toUpperCase();
        String code = String.valueOf(payload.getOrDefault("code", "")).trim().toUpperCase();
        String name = String.valueOf(payload.getOrDefault("nameVn", payload.getOrDefault("name", ""))).trim();
        boolean active = Boolean.parseBoolean(String.valueOf(payload.getOrDefault("active", "true")));

        jdbcTemplate.update(
                "INSERT INTO admin_catalogs(group_code, code, name, active, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())",
                groupCode, code, name, active ? 1 : 0
        );
        return ResponseEntity.ok(Map.of("message", "Tạo catalog thành công"));
    }

    @PutMapping("/catalogs/{id}")
    public ResponseEntity<Map<String, Object>> updateCatalog(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        String groupCode = String.valueOf(payload.getOrDefault("groupCode", "")).trim().toUpperCase();
        String code = String.valueOf(payload.getOrDefault("code", "")).trim().toUpperCase();
        String name = String.valueOf(payload.getOrDefault("nameVn", payload.getOrDefault("name", ""))).trim();
        boolean active = Boolean.parseBoolean(String.valueOf(payload.getOrDefault("active", "true")));

        jdbcTemplate.update(
                "UPDATE admin_catalogs SET group_code = ?, code = ?, name = ?, active = ?, updated_at = NOW() WHERE id = ?",
                groupCode, code, name, active ? 1 : 0, id
        );
        return ResponseEntity.ok(Map.of("message", "Cập nhật catalog thành công"));
    }

    @DeleteMapping("/catalogs/{id}")
    public ResponseEntity<Map<String, Object>> deleteCatalog(@PathVariable Long id) {
        jdbcTemplate.update("DELETE FROM admin_catalogs WHERE id = ?", id);
        return ResponseEntity.ok(Map.of("message", "Xóa catalog thành công"));
    }

    @PatchMapping("/catalogs/{id}/active")
    public ResponseEntity<Map<String, Object>> toggleCatalogActive(@PathVariable Long id) {
        jdbcTemplate.update("UPDATE admin_catalogs SET active = IF(active = 1, 0, 1), updated_at = NOW() WHERE id = ?", id);
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật trạng thái"));
    }

    @GetMapping("/catalog-groups")
    public ResponseEntity<List<Map<String, Object>>> getCatalogGroups() {
        List<Map<String, Object>> groups = jdbcTemplate.query(
                "SELECT group_code, MAX(CASE WHEN code='__GROUP__' THEN name ELSE group_code END) AS display_name, " +
                        "SUM(CASE WHEN code<>'__GROUP__' THEN 1 ELSE 0 END) AS total_statuses " +
                        "FROM admin_catalogs GROUP BY group_code ORDER BY group_code",
                (rs, rowNum) -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("groupCode", rs.getString("group_code"));
                    item.put("name", rs.getString("display_name"));
                    item.put("totalStatuses", rs.getLong("total_statuses"));
                    return item;
                }
        );
        return ResponseEntity.ok(groups);
    }

    @PutMapping("/catalog-groups/{groupCode}")
    public ResponseEntity<Map<String, Object>> updateCatalogGroupName(
            @PathVariable String groupCode,
            @RequestBody Map<String, Object> payload
    ) {
        String normalized = groupCode.trim().toUpperCase();
        String name = String.valueOf(payload.getOrDefault("name", normalized)).trim();

        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_catalogs WHERE group_code = ? AND code = '__GROUP__'",
                Integer.class,
                normalized
        );
        if (cnt != null && cnt > 0) {
            jdbcTemplate.update(
                    "UPDATE admin_catalogs SET name = ?, updated_at = NOW() WHERE group_code = ? AND code = '__GROUP__'",
                    name, normalized
            );
        } else {
            jdbcTemplate.update(
                    "INSERT INTO admin_catalogs(group_code, code, name, active, created_at, updated_at) VALUES (?, '__GROUP__', ?, 1, NOW(), NOW())",
                    normalized, name
            );
        }
        return ResponseEntity.ok(Map.of("message", "Cập nhật nhóm danh mục thành công"));
    }

    @DeleteMapping("/catalog-groups/{groupCode}")
    public ResponseEntity<Map<String, Object>> deleteCatalogGroup(@PathVariable String groupCode) {
        jdbcTemplate.update("DELETE FROM admin_catalogs WHERE group_code = ?", groupCode.trim().toUpperCase());
        return ResponseEntity.ok(Map.of("message", "Xóa nhóm danh mục thành công"));
    }
}
