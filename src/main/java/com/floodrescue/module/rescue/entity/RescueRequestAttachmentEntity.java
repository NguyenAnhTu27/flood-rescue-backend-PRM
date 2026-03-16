package com.floodrescue.module.rescue.entity;

import com.floodrescue.shared.enums.AttachmentFileType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "rescue_request_attachments",
        indexes = {
                @Index(name = "idx_rr_att_req", columnList = "rescue_request_id")
        }
)
public class RescueRequestAttachmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rescue_request_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_rr_att_req"))
    private RescueRequestEntity rescueRequest;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", length = 30)
    private AttachmentFileType fileType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
