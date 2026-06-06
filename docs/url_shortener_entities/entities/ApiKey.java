package com.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "api_keys", indexes = {
        @Index(name = "idx_api_keys_user_id",  columnList = "user_id"),
        @Index(name = "idx_api_keys_key_hash", columnList = "key_hash")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    @Id
    @Column(name = "id", nullable = false)
    private Long id; // Snowflake ID

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_api_keys_user"))
    private User user;

    /**
     * SHA-256 của raw key. Raw key chỉ trả về 1 lần khi tạo,
     * không bao giờ lưu vào DB.
     */
    @Column(name = "key_hash", nullable = false, unique = true, columnDefinition = "TEXT")
    private String keyHash;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "rate_limit_per_hour", nullable = false)
    @Builder.Default
    private Long rateLimitPerHour = 1000L;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "expires_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime expiresAt;

    @Column(name = "last_used_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime lastUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    // ----------------------------------------------------------------
    // Helper
    // ----------------------------------------------------------------

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(OffsetDateTime.now());
    }

    public boolean isValid() {
        return isActive && !isExpired();
    }
}
