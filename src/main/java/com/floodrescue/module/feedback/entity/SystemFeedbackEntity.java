package com.floodrescue.module.feedback.entity;

import com.floodrescue.module.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "system_feedbacks", indexes = {
        @Index(name = "idx_system_feedbacks_citizen", columnList = "citizen_id"),
        @Index(name = "idx_system_feedbacks_rating", columnList = "rating"),
        @Index(name = "idx_system_feedbacks_created", columnList = "created_at")
})
public class SystemFeedbackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "citizen_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_system_feedbacks_citizen"))
    private UserEntity citizen;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "feedback_content", columnDefinition = "TEXT")
    private String feedbackContent;

    @Column(name = "rescued_confirmed", nullable = false)
    @Builder.Default
    private Boolean rescuedConfirmed = false;

    @Column(name = "relief_confirmed", nullable = false)
    @Builder.Default
    private Boolean reliefConfirmed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
