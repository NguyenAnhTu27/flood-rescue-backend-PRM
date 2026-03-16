package com.floodrescue.module.map.controller;

import com.floodrescue.module.map.dto.*;
import com.floodrescue.module.map.service.MapboxService;
import com.floodrescue.module.rescue.entity.RescueRequestEntity;
import com.floodrescue.module.rescue.repository.RescueRequestRepository;
import com.floodrescue.module.team.service.TeamService;
import com.floodrescue.module.user.entity.UserEntity;
import com.floodrescue.module.user.repository.UserRepository;
import com.floodrescue.shared.enums.RescueRequestStatus;
import com.floodrescue.shared.exception.BusinessException;
import com.floodrescue.shared.exception.NotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MapController {

    private final TeamService teamService;
    private final MapboxService mapboxService;
    private final RescueRequestRepository rescueRequestRepository;
    private final UserRepository userRepository;

    @PutMapping("/teams/{teamId}/location")
    @PreAuthorize("hasAnyRole('RESCUER', 'COORDINATOR')")
    public ResponseEntity<TeamLocationResponse> updateTeamLocation(
            @PathVariable Long teamId,
            @Valid @RequestBody LocationUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));

        // RESCUER must belong to the team they are updating
        boolean isRescuer = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESCUER"));
        if (isRescuer && !teamId.equals(user.getTeamId())) {
            throw new BusinessException("Bạn chỉ có thể cập nhật vị trí đội của mình");
        }

        var team = teamService.updateTeamLocation(teamId, request.getLatitude(), request.getLongitude());
        var response = TeamLocationResponse.builder()
                .teamId(team.getId())
                .name(team.getName())
                .status(team.getStatus())
                .teamType(team.getTeamType())
                .latitude(team.getCurrentLatitude())
                .longitude(team.getCurrentLongitude())
                .lastLocationUpdate(team.getCurrentLocationUpdatedAt())
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/map/teams/locations")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<List<TeamLocationResponse>> getAllTeamLocations() {
        return ResponseEntity.ok(teamService.getAllTeamLocations());
    }

    @GetMapping("/map/teams/nearest")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<List<TeamLocationResponse>> findNearestTeams(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "50") Double radius) {
        return ResponseEntity.ok(teamService.findNearestTeams(lat, lng, radius));
    }

    @GetMapping("/map/rescue-requests/locations")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<List<RescueLocationResponse>> getRescueRequestLocations() {
        List<RescueRequestEntity> requests = rescueRequestRepository
                .findByStatusAndLatitudeIsNotNull(RescueRequestStatus.PENDING);

        List<RescueLocationResponse> responses = requests.stream()
                .map(r -> RescueLocationResponse.builder()
                        .id(r.getId())
                        .code(r.getCode())
                        .status(r.getStatus())
                        .priority(r.getPriority())
                        .latitude(r.getLatitude())
                        .longitude(r.getLongitude())
                        .addressText(r.getAddressText())
                        .affectedPeopleCount(r.getAffectedPeopleCount())
                        .citizenName(r.getCitizen().getFullName())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/map/geocode")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GeocodingResponse> geocode(@RequestParam String address) {
        return ResponseEntity.ok(mapboxService.geocode(address));
    }
}
