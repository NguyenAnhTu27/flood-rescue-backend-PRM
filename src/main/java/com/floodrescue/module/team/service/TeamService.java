package com.floodrescue.module.team.service;

import com.floodrescue.module.map.dto.TeamLocationResponse;
import com.floodrescue.module.team.dto.request.CreateTeamRequest;
import com.floodrescue.module.team.dto.response.TeamMemberResponse;
import com.floodrescue.module.team.dto.response.TeamResponse;
import com.floodrescue.module.team.entity.TeamEntity;

import java.util.List;

public interface TeamService {

    TeamResponse createTeam(CreateTeamRequest request);

    TeamResponse updateTeam(Long id, CreateTeamRequest request);

    void deleteTeam(Long id);

    List<TeamResponse> getAllTeams();

    TeamResponse getTeamById(Long id);

    List<TeamMemberResponse> getRescuerCandidates();

    TeamEntity updateTeamLocation(Long teamId, Double latitude, Double longitude);

    List<TeamLocationResponse> getAllTeamLocations();

    List<TeamLocationResponse> findNearestTeams(Double lat, Double lng, Double radiusKm);
}
