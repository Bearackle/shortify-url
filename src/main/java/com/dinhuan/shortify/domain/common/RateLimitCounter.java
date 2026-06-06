package com.dinhuan.shortify.domain.common;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "rate_limit_counters", indexes = {
        @Index(name = "idx_rate_limit_expires", columnList = "expires_at")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimitCounter {

    /**
     * Key format:
     *   "ip:{ip}:{action}"     — ví dụ: "ip:1.2.3.4:create"
     *   "user:{id}:{action}"   — ví dụ: "user:123:shorten"
     *   "key:{keyId}:{action}" — ví dụ: "key:456:api"
     */
    @Id
    @Column(name = "key", length = 255)
    private String key;

    @Column(name = "count", nullable = false)
    @Builder.Default
    private Long count = 1L;

    @Column(name = "window_start", nullable = false, columnDefinition = "TIMESTAMPTZ")
    @Builder.Default
    private OffsetDateTime windowStart = OffsetDateTime.now();

    @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime expiresAt;

    // ----------------------------------------------------------------
    // Helper
    // ----------------------------------------------------------------

    public boolean isExpired() {
        return expiresAt.isBefore(OffsetDateTime.now());
    }

    /** Tăng count, trả về giá trị mới */
    public long increment() {
        return ++this.count;
    }
}
