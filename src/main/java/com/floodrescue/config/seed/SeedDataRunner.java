package com.floodrescue.config.seed;

import com.floodrescue.module.team.entity.TeamEntity;
import com.floodrescue.module.team.repository.TeamRepository;
import com.floodrescue.module.user.entity.RoleEntity;
import com.floodrescue.module.user.entity.UserEntity;
import com.floodrescue.module.user.repository.RoleRepository;
import com.floodrescue.module.user.repository.UserRepository;
import com.floodrescue.shared.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Seed roles + default coordinator account for local/dev usage.
 * Controlled by app.seed.enabled in application.properties.
 */
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class SeedDataRunner implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TeamRepository teamRepository;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${app.seed.coordinator.email:coordinator@example.com}")
    private String coordinatorEmail;

    @Value("${app.seed.coordinator.phone:0900000001}")
    private String coordinatorPhone;

    @Value("${app.seed.coordinator.password:Password123}")
    private String coordinatorPassword;

    @Value("${app.seed.coordinator.full-name:Điều phối viên mặc định}")
    private String coordinatorFullName;

    // Admin seed (for demo / local)
    @Value("${app.seed.admin.email:admin@example.com}")
    private String adminEmail;

    @Value("${app.seed.admin.phone:0900000000}")
    private String adminPhone;

    @Value("${app.seed.admin.password:Admin123}")
    private String adminPassword;

    @Value("${app.seed.admin.full-name:Quản trị viên hệ thống}")
    private String adminFullName;

    // Manager seed (for demo / local)
    @Value("${app.seed.manager.email:manager@example.com}")
    private String managerEmail;

    @Value("${app.seed.manager.phone:0900000003}")
    private String managerPhone;

    @Value("${app.seed.manager.password:Manager123}")
    private String managerPassword;

    @Value("${app.seed.manager.full-name:Quản lý hệ thống}")
    private String managerFullName;

    // Team 1 seed
    @Value("${app.seed.team1.phone:0910000001}")
    private String team1Phone;
    @Value("${app.seed.team1.email:team1@gmail.com}")
    private String team1Email;
    @Value("${app.seed.team1.password:Team123}")
    private String team1Password;
    @Value("${app.seed.team1.full-name:Đội trưởng Đội Cứu hộ số 1}")
    private String team1FullName;

    // Team 2 seed
    @Value("${app.seed.team2.phone:0910000002}")
    private String team2Phone;
    @Value("${app.seed.team2.email:team2@gmail.com}")
    private String team2Email;
    @Value("${app.seed.team2.password:Team123}")
    private String team2Password;
    @Value("${app.seed.team2.full-name:Đội trưởng Đội Cứu hộ số 2}")
    private String team2FullName;

    @Override
    public void run(ApplicationArguments args) {
        if (!seedEnabled) return;

        // Ensure roles exist
        ensureRole("CITIZEN", "Công dân");
        RoleEntity coordinatorRole = ensureRole("COORDINATOR", "Điều phối");
        RoleEntity rescuerRole = ensureRole("RESCUER", "Đội cứu hộ");
        RoleEntity managerRole = ensureRole("MANAGER", "Quản lý");
        RoleEntity adminRole = ensureRole("ADMIN", "Admin");

        // Seed default admin + coordinator + manager accounts (if not exists)
        seedAdminUser(adminRole);
        seedCoordinatorUser(coordinatorRole);
        seedManagerUser(managerRole);

        // Seed team accounts (RESCUER role) - cần có team trước
        seedTeamUsers(rescuerRole);
    }

    private RoleEntity ensureRole(String code, String name) {
        return roleRepository.findByCode(code)
                .orElseGet(() -> {
                    RoleEntity role = RoleEntity.builder()
                            .code(code)
                            .name(name)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return roleRepository.save(role);
                });
    }

    private void seedAdminUser(RoleEntity adminRole) {
        String email = normalizeEmail(adminEmail);
        String phone = normalizePhone(adminPhone);
        if (email == null && phone == null) {
            return;
        }

        boolean exists = userExistsByEmailOrPhone(email, phone);
        if (exists) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        UserEntity admin = UserEntity.builder()
                .role(adminRole)
                .teamId(null)
                .fullName(adminFullName)
                .phone(phone)
                .email(email)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .status((byte) 1)
                .createdAt(now)
                .updatedAt(now)
                .build();

        userRepository.save(admin);
    }

    private void seedCoordinatorUser(RoleEntity coordinatorRole) {
        String email = normalizeEmail(coordinatorEmail);
        String phone = normalizePhone(coordinatorPhone);
        if (email == null && phone == null) {
            return;
        }

        boolean exists = userExistsByEmailOrPhone(email, phone);
        if (exists) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        UserEntity user = UserEntity.builder()
                .role(coordinatorRole)
                .teamId(null)
                .fullName(coordinatorFullName)
                .phone(phone)
                .email(email)
                .passwordHash(passwordEncoder.encode(coordinatorPassword))
                .status((byte) 1)
                .createdAt(now)
                .updatedAt(now)
                .build();

        userRepository.save(user);
    }

    private void seedManagerUser(RoleEntity managerRole) {
        String email = normalizeEmail(managerEmail);
        String phone = normalizePhone(managerPhone);
        if (email == null && phone == null) {
            return;
        }

        boolean exists = userExistsByEmailOrPhone(email, phone);
        if (exists) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        UserEntity manager = UserEntity.builder()
                .role(managerRole)
                .teamId(null)
                .fullName(managerFullName)
                .phone(phone)
                .email(email)
                .passwordHash(passwordEncoder.encode(managerPassword))
                .status((byte) 1)
                .createdAt(now)
                .updatedAt(now)
                .build();

        userRepository.save(manager);
    }

    /**
     * Seed tài khoản đội cứu hộ (RESCUER role).
     * Tự động tạo team nếu chưa có, rồi gán user vào team đó.
     */
    private void seedTeamUsers(RoleEntity rescuerRole) {
        // Đảm bảo có ít nhất 2 team để gán user
        TeamEntity team1 = ensureTeam("Đội Cứu hộ số 1", "Đội phản ứng nhanh khu vực trung tâm");
        TeamEntity team2 = ensureTeam("Đội Cứu hộ số 2", "Đội hỗ trợ khu vực ngoại thành");

        // Seed user cho Team 1
        seedTeamUser(rescuerRole, team1.getId(), team1Phone, team1Email, team1Password, team1FullName);

        // Seed user cho Team 2
        seedTeamUser(rescuerRole, team2.getId(), team2Phone, team2Email, team2Password, team2FullName);
    }

    /**
     * Đảm bảo team tồn tại, nếu chưa có thì tạo mới.
     */
    private TeamEntity ensureTeam(String name, String description) {
        return teamRepository.findByName(name)
                .orElseGet(() -> {
                    String code = CodeGenerator.generateTeamCode();
                    // Đảm bảo code unique
                    int attempts = 0;
                    while (teamRepository.existsByCode(code) && attempts < 20) {
                        code = CodeGenerator.generateTeamCode();
                        attempts++;
                    }
                    if (teamRepository.existsByCode(code)) {
                        throw new IllegalStateException("Không thể tạo mã team unique cho " + name);
                    }

                    TeamEntity team = TeamEntity.builder()
                            .code(code)
                            .name(name)
                            .description(description)
                            .build();
                    return teamRepository.save(team);
                });
    }

    /**
     * Seed 1 user RESCUER gán vào team.
     */
    private void seedTeamUser(RoleEntity rescuerRole, Long teamId, String phone, String email, String password, String fullName) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedPhone = normalizePhone(phone);
        if (normalizedEmail == null && normalizedPhone == null) {
            return;
        }

        // Check xem đã tồn tại user với phone hoặc email này chưa
        boolean exists = userExistsByEmailOrPhone(normalizedEmail, normalizedPhone);
        if (exists) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        UserEntity user = UserEntity.builder()
                .role(rescuerRole)
                .teamId(teamId) // Gán vào team
                .fullName(fullName)
                .phone(normalizedPhone)
                .email(normalizedEmail) // Set email
                .passwordHash(passwordEncoder.encode(password))
                .status((byte) 1)
                .isLeader(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        userRepository.save(user);
    }

    private boolean userExistsByEmailOrPhone(String email, String phone) {
        return (email != null && userRepository.existsByEmail(email))
                || (phone != null && userRepository.existsByPhone(phone));
    }

    private String normalizeEmail(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase();
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizePhone(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
