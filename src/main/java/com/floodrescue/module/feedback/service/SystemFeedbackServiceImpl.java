package com.floodrescue.module.feedback.service;

import com.floodrescue.module.feedback.dto.request.SystemFeedbackCreateRequest;
import com.floodrescue.module.feedback.dto.response.SystemFeedbackResponse;
import com.floodrescue.module.feedback.dto.response.SystemFeedbackSummaryResponse;
import com.floodrescue.module.feedback.entity.SystemFeedbackEntity;
import com.floodrescue.module.feedback.repository.SystemFeedbackRepository;
import com.floodrescue.module.user.entity.UserEntity;
import com.floodrescue.module.user.repository.UserRepository;
import com.floodrescue.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemFeedbackServiceImpl implements SystemFeedbackService {

    private final SystemFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SystemFeedbackResponse createFeedback(Long citizenId, SystemFeedbackCreateRequest request) {
        UserEntity citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy công dân."));

        String content = normalizeText(request.getFeedbackContent());

        SystemFeedbackEntity saved = feedbackRepository.save(SystemFeedbackEntity.builder()
                .citizen(citizen)
                .rating(request.getRating())
                .feedbackContent(content)
                .rescuedConfirmed(Boolean.TRUE.equals(request.getRescuedConfirmed()))
                .reliefConfirmed(Boolean.TRUE.equals(request.getReliefConfirmed()))
                .build());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SystemFeedbackResponse> getFeedbacks(Pageable pageable) {
        return feedbackRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SystemFeedbackSummaryResponse getSummary() {
        long total = feedbackRepository.count();
        Double avg = feedbackRepository.findAverageRating();

        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            distribution.put(star, feedbackRepository.countByRating(star));
        }

        return SystemFeedbackSummaryResponse.builder()
                .totalFeedbacks(total)
                .averageRating(avg == null ? 0.0 : Math.round(avg * 100.0) / 100.0)
                .ratingDistribution(distribution)
                .build();
    }

    private SystemFeedbackResponse toResponse(SystemFeedbackEntity entity) {
        UserEntity citizen = entity.getCitizen();
        return SystemFeedbackResponse.builder()
                .id(entity.getId())
                .citizenId(citizen == null ? null : citizen.getId())
                .citizenName(citizen == null ? null : citizen.getFullName())
                .citizenEmail(citizen == null ? null : citizen.getEmail())
                .rating(entity.getRating())
                .feedbackContent(entity.getFeedbackContent())
                .rescuedConfirmed(Boolean.TRUE.equals(entity.getRescuedConfirmed()))
                .reliefConfirmed(Boolean.TRUE.equals(entity.getReliefConfirmed()))
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private String normalizeText(String input) {
        if (input == null) return null;
        String normalized = input.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
