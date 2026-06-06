package com.dinhuan.shortify.domain.common;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "blocked_domains", indexes = {
        @Index(name = "idx_blocked_domains_created", columnList = "created_at")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedDomain {

    /**
     * Domain dạng "example.com" — không có www. hay schema.
     * Trigger DB extract và lowercase trước khi so sánh.
     */
    @Id
    @Column(name = "domain", length = 255)
    private String domain;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;
}
