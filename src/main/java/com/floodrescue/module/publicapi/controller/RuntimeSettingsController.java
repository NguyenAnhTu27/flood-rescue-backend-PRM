package com.floodrescue.module.publicapi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class RuntimeSettingsController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/runtime-settings")
    public ResponseEntity<Map<String, String>> getRuntimeSettings() {
        Map<String, String> runtime = new HashMap<>();
        runtime.put("hotline", "1900-xxxx");
        runtime.put("footerBrandName", "QUẢN LÝ CỨU HỘ");
        runtime.put("footerDescription", "Hệ thống hỗ trợ cộng đồng trong tình huống thiên tai khẩn cấp. Thông tin được bảo mật và điều phối theo quy định của cơ quan chức năng.");
        runtime.put("footerTermsLabel", "Điều khoản sử dụng");
        runtime.put("footerTermsUrl", "/dieu-khoan-su-dung");
        runtime.put("footerPrivacyLabel", "Chính sách bảo mật");
        runtime.put("footerPrivacyUrl", "/chinh-sach-bao-mat");
        runtime.put("footerSupportLabel", "Liên hệ hỗ trợ");
        runtime.put("footerSupportUrl", "/lien-he-ho-tro");
        runtime.put("footerSupportEmail", "support@cuuho.gov.vn");
        runtime.put("footerFacebookUrl", "#");
        runtime.put("footerTwitterUrl", "#");
        runtime.put("footerYoutubeUrl", "#");
        runtime.put("footerCopyright", "© 2024 Hệ thống Quản lý Cứu hộ - Cứu trợ. Bản quyền thuộc về Cơ quan chủ quản.");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT COALESCE(setting_key, key_name) AS k, COALESCE(setting_value, value_text) AS v FROM system_settings"
        );
        for (Map<String, Object> row : rows) {
            String key = row.get("k") == null ? null : String.valueOf(row.get("k"));
            if (key == null || key.isBlank()) continue;
            Object rawValue = row.get("v");
            String value = rawValue == null ? "" : String.valueOf(rawValue);
            runtime.put(key, value);
        }

        return ResponseEntity.ok(runtime);
    }

    @GetMapping("/content-pages/{pageKey}")
    public ResponseEntity<Map<String, String>> getContentPage(@PathVariable String pageKey) {
        Map<String, String> values = new HashMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT COALESCE(setting_key, key_name) AS k, COALESCE(setting_value, value_text) AS v FROM system_settings"
        );
        for (Map<String, Object> row : rows) {
            String key = row.get("k") == null ? null : String.valueOf(row.get("k"));
            if (key == null || key.isBlank()) continue;
            values.put(key, row.get("v") == null ? "" : String.valueOf(row.get("v")));
        }

        return switch (pageKey.toLowerCase()) {
            case "terms" -> ResponseEntity.ok(Map.of(
                    "title", values.getOrDefault("legalTermsTitle", "Điều khoản sử dụng"),
                    "content", values.getOrDefault("legalTermsContent", "")
            ));
            case "privacy" -> ResponseEntity.ok(Map.of(
                    "title", values.getOrDefault("privacyPolicyTitle", "Chính sách bảo mật"),
                    "content", values.getOrDefault("privacyPolicyContent", "")
            ));
            case "support" -> ResponseEntity.ok(Map.of(
                    "title", values.getOrDefault("supportPageTitle", "Liên hệ hỗ trợ"),
                    "content", values.getOrDefault("supportPageContent", "")
            ));
            default -> ResponseEntity.notFound().build();
        };
    }
}
