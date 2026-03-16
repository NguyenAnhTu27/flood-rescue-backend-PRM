package com.floodrescue.module.team.service;

import com.floodrescue.module.map.dto.TeamLocationResponse;
import com.floodrescue.module.team.dto.request.CreateTeamRequest;
import com.floodrescue.module.team.dto.response.TeamMemberResponse;
import com.floodrescue.module.team.dto.response.TeamResponse;
import com.floodrescue.module.rescue.repository.TaskGroupRepository;
import com.floodrescue.module.team.entity.TeamEntity;
import com.floodrescue.module.team.repository.TeamRepository;
import com.floodrescue.module.user.entity.UserEntity;
import com.floodrescue.module.user.repository.UserRepository;
import com.floodrescue.shared.enums.TaskGroupStatus;
import com.floodrescue.shared.exception.BusinessException;
import com.floodrescue.shared.exception.NotFoundException;
import com.floodrescue.shared.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TaskGroupRepository taskGroupRepository;

    @Override
    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request) {
        String name = normalizeRequired(request.getName(), "Tên đội cứu hộ không được để trống");
        if (teamRepository.existsByName(name)) {
            throw new BusinessException("Tên đội cứu hộ đã tồn tại");
        }

        String code = normalizeOptional(request.getCode());
        if (code == null) {
            code = generateUniqueCode();
        } else if (teamRepository.existsByCode(code)) {
            throw new BusinessException("Mã đội cứu hộ đã tồn tại");
        }

        TeamEntity team = TeamEntity.builder()
                .code(code)
                .name(name)
                .description(normalizeOptional(request.getDescription()))
                .build();
        team = teamRepository.save(team);

        assignMembers(team, request);
        return toResponse(team);
    }

    @Override
    @Transactional
    public TeamResponse updateTeam(Long id, CreateTeamRequest request) {
        TeamEntity team = teamRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Đội cứu hộ không tồn tại"));

        String name = normalizeRequired(request.getName(), "Tên đội cứu hộ không được để trống");
        if (!name.equals(team.getName()) && teamRepository.existsByName(name)) {
            throw new BusinessException("Tên đội cứu hộ đã tồn tại");
        }

        String code = normalizeOptional(request.getCode());
        if (code != null && !code.equals(team.getCode()) && teamRepository.existsByCode(code)) {
            throw new BusinessException("Mã đội cứu hộ đã tồn tại");
        }

        team.setName(name);
        team.setDescription(normalizeOptional(request.getDescription()));
        if (code != null) {
            team.setCode(code);
        }
        team = teamRepository.save(team);

        assignMembers(team, request);
        return toResponse(team);
    }

    @Override
    @Transactional
    public void deleteTeam(Long id) {
        TeamEntity team = teamRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Đội cứu hộ không tồn tại"));

        // Clear member links before delete to avoid orphan team references.
        List<UserEntity> members = userRepository.findByTeamId(team.getId());
        for (UserEntity user : members) {
            user.setTeamId(null);
            user.setIsLeader(false);
        }
        userRepository.saveAll(members);

        teamRepository.delete(team);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TeamResponse getTeamById(Long id) {
        TeamEntity team = teamRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Đội cứu hộ không tồn tại"));
        return toResponse(team);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getRescuerCandidates() {
        return userRepository.findAllByRoleCode("RESCUER").stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
    }

    private void assignMembers(TeamEntity team, CreateTeamRequest request) {
        Set<Long> selectedIds = new LinkedHashSet<>();
        if (request.getMemberIds() != null) {
            selectedIds.addAll(request.getMemberIds().stream()
                    .filter(id -> id != null && id > 0)
                    .collect(Collectors.toSet()));
        }
        if (request.getLeaderId() != null) {
            selectedIds.add(request.getLeaderId());
        }

        List<UserEntity> currentMembers = userRepository.findByTeamId(team.getId());

        if (selectedIds.isEmpty()) {
            for (UserEntity user : currentMembers) {
                user.setTeamId(null);
                user.setIsLeader(false);
            }
            userRepository.saveAll(currentMembers);
            return;
        }

        Map<Long, UserEntity> selectedUsers = userRepository.findAllById(selectedIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        if (selectedUsers.size() != selectedIds.size()) {
            throw new BusinessException("Có thành viên không tồn tại trong hệ thống");
        }

        List<String> invalidRoles = selectedUsers.values().stream()
                .filter(u -> u.getRole() == null || !"RESCUER".equalsIgnoreCase(u.getRole().getCode()))
                .map(UserEntity::getFullName)
                .collect(Collectors.toList());
        if (!invalidRoles.isEmpty()) {
            throw new BusinessException("Chỉ có thể chọn thành viên role RESCUER. Không hợp lệ: " + String.join(", ", invalidRoles));
        }

        boolean forceReassign = Boolean.TRUE.equals(request.getForceReassignMembers());
        List<String> alreadyInOtherTeam = selectedUsers.values().stream()
                .filter(u -> u.getTeamId() != null && !u.getTeamId().equals(team.getId()))
                .map(UserEntity::getFullName)
                .collect(Collectors.toList());
        if (!alreadyInOtherTeam.isEmpty() && !forceReassign) {
            throw new BusinessException("Một số thành viên đã thuộc đội khác: " + String.join(", ", alreadyInOtherTeam)
                    + ". Vui lòng xác nhận điều chuyển.");
        }

        for (UserEntity user : currentMembers) {
            user.setTeamId(null);
            user.setIsLeader(false);
        }

        Long leaderId = request.getLeaderId();
        for (UserEntity user : selectedUsers.values()) {
            user.setTeamId(team.getId());
            user.setIsLeader(leaderId != null && leaderId.equals(user.getId()));
        }

        List<UserEntity> toSave = new ArrayList<>();
        toSave.addAll(currentMembers);
        toSave.addAll(selectedUsers.values());
        userRepository.saveAll(toSave);
    }

    private TeamResponse toResponse(TeamEntity team) {
        List<TeamMemberResponse> members = userRepository.findByTeamId(team.getId()).stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
        boolean inRescue = taskGroupRepository.findByAssignedTeamIdAndStatus(
                team.getId(),
                TaskGroupStatus.IN_PROGRESS,
                org.springframework.data.domain.Pageable.ofSize(1)
        ).hasContent();

        TeamMemberResponse leader = members.stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsLeader()))
                .findFirst()
                .orElse(null);

        return TeamResponse.builder()
                .id(team.getId())
                .code(team.getCode())
                .name(team.getName())
                .description(team.getDescription())
                .status(team.getStatus() != null && team.getStatus() == 1 ? "active" : "inactive")
                .workloadStatus(inRescue ? "IN_RESCUE" : "AVAILABLE")
                .currentLatitude(team.getCurrentLatitude())
                .currentLongitude(team.getCurrentLongitude())
                .currentLocationText(team.getCurrentLocationText())
                .currentLocationUpdatedAt(team.getCurrentLocationUpdatedAt())
                .leaderId(leader != null ? leader.getId() : null)
                .leaderName(leader != null ? leader.getFullName() : null)
                .memberCount(members.size())
                .members(members)
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .build();
    }

    private TeamMemberResponse toMemberResponse(UserEntity user) {
        TeamEntity team = null;
        if (user.getTeamId() != null) {
            team = teamRepository.findById(user.getTeamId()).orElse(null);
        }

        return TeamMemberResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .teamId(user.getTeamId())
                .teamCode(team != null ? team.getCode() : null)
                .teamName(team != null ? team.getName() : null)
                .isLeader(Boolean.TRUE.equals(user.getIsLeader()))
                .build();
    }

    private String generateUniqueCode() {
        String code;
        int attempts = 0;
        do {
            code = CodeGenerator.generateTeamCode();
            attempts++;
            if (attempts > 10) {
                throw new BusinessException("Không thể tạo mã đội cứu hộ duy nhất");
            }
        } while (teamRepository.existsByCode(code));
        return code;
    }

    @Override
    @Transactional
    public TeamEntity updateTeamLocation(Long teamId, Double latitude, Double longitude) {
        TeamEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Đội cứu hộ không tồn tại"));
        team.setCurrentLatitude(latitude);
        team.setCurrentLongitude(longitude);
        team.setCurrentLocationUpdatedAt(java.time.LocalDateTime.now());
        return teamRepository.save(team);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamLocationResponse> getAllTeamLocations() {
        return teamRepository.findByStatusAndCurrentLatitudeIsNotNull((byte) 1).stream()
                .map(this::toLocationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamLocationResponse> findNearestTeams(Double lat, Double lng, Double radiusKm) {
        return teamRepository.findByStatusAndCurrentLatitudeIsNotNull((byte) 1).stream()
                .map(team -> {
                    double dist = haversine(lat, lng, team.getCurrentLatitude(), team.getCurrentLongitude());
                    TeamLocationResponse r = toLocationResponse(team);
                    r.setDistanceKm(dist);
                    return r;
                })
                .filter(r -> r.getDistanceKm() <= radiusKm)
                .sorted(java.util.Comparator.comparingDouble(TeamLocationResponse::getDistanceKm))
                .collect(Collectors.toList());
    }

    private TeamLocationResponse toLocationResponse(TeamEntity team) {
        return TeamLocationResponse.builder()
                .teamId(team.getId())
                .name(team.getName())
                .status(team.getStatus())
                .teamType(team.getTeamType())
                .latitude(team.getCurrentLatitude())
                .longitude(team.getCurrentLongitude())
                .lastLocationUpdate(team.getCurrentLocationUpdatedAt())
                .build();
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String normalizeRequired(String value, String errorMessage) {
        String normalized = normalizeOptional(value);
        if (normalized == null || normalized.isEmpty()) {
            throw new BusinessException(errorMessage);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
