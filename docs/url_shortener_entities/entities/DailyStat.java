package com.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;

@Entity
@Table(name = "daily_stats", indexes = {
        @Index(name = "idx_daily_stats_date", columnList = "stat_date")
})
@IdClass(DailyStat.DailyStatId.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyStat {

    /**
     * Composite PK: (short_url_id, stat_date).
     * ON CONFLICT (short_url_id, stat_date) DO UPDATE — idempotent batch job.
     */
    @Id
    @Column(name = "short_url_id", nullable = false)
    private Long shortUrlId;

    @Id
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "short_url_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_daily_stats_short_url"))
    private ShortUrl shortUrl;

    // ----------------------------------------------------------------
    // Metrics
    // ----------------------------------------------------------------

    @Column(name = "click_count", nullable = false)
    @Builder.Default
    private Long clickCount = 0L;

    @Column(name = "unique_ips", nullable = false)
    @Builder.Default
    private Long uniqueIps = 0L;

    @Column(name = "desktop_clicks", nullable = false)
    @Builder.Default
    private Long desktopClicks = 0L;

    @Column(name = "mobile_clicks",  nullable = false)
    @Builder.Default
    private Long mobileClicks = 0L;

    @Column(name = "tablet_clicks",  nullable = false)
    @Builder.Default
    private Long tabletClicks = 0L;

    /**
     * {"VN": 120, "US": 80, "SG": 30}
     * Lưu dạng JSONB trong PostgreSQL — deserialize thành Map khi đọc.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "country_breakdown", columnDefinition = "JSONB")
    private Map<String, Long> countryBreakdown;

    /**
     * {"google.com": 60, "direct": 40, "facebook.com": 20}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "referer_breakdown", columnDefinition = "JSONB")
    private Map<String, Long> refererBreakdown;

    // ----------------------------------------------------------------
    // Composite PK class
    // ----------------------------------------------------------------

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class DailyStatId implements Serializable {
        private Long shortUrlId;
        private LocalDate statDate;
    }
}
