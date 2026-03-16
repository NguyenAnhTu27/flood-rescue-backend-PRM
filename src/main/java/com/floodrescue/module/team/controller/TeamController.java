package com.floodrescue.module.team.controller;

import com.floodrescue.module.team.dto.request.CreateTeamRequest;
import com.floodrescue.module.team.dto.response.TeamMemberResponse;
import com.floodrescue.module.team.dto.response.TeamResponse;
import com.floodrescue.module.team.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    /**
     * Tạo đội cứu hộ mới (dành cho Admin/Manager).
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        TeamResponse team = teamService.createTeam(request);
        return ResponseEntity.ok(team);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<TeamResponse> updateTeam(@PathVariable Long id, @Valid @RequestBody CreateTeamRequest request) {
        return ResponseEntity.ok(teamService.updateTeam(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> deleteTeam(@PathVariable Long id) {
        teamService.deleteTeam(id);
        return ResponseEntity.ok().body(java.util.Map.of("message", "Xóa đội cứu hộ thành công"));
    }

    /**
     * Lấy danh sách tất cả đội cứu hộ.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','COORDINATOR')")
    public ResponseEntity<List<TeamResponse>> getTeams() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }

    /**
     * Lấy chi tiết 1 đội cứu hộ theo ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','COORDINATOR')")
    public ResponseEntity<TeamResponse> getTeam(@PathVariable Long id) {
        return ResponseEntity.ok(teamService.getTeamById(id));
    }

    @GetMapping("/member-candidates")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<TeamMemberResponse>> getMemberCandidates() {
        return ResponseEntity.ok(teamService.getRescuerCandidates());
    }
}
