package com.dinhuan.shortify.domain.analytics;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "click_events", indexes = {
        @Index(name = "idx_click_events_url_id",  columnList = "short_url_id, clicked_at"),
        @Index(name = "idx_click_events_country", columnList = "ip_country, clicked_at"),
        @Index(name = "idx_click_events_device",  columnList = "device_type, clicked_at")
})
@IdClass(ClickEvent.ClickEventId.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickEvent {

    /**
     * Composite PK: (id, clicked_at) — bắt buộc vì bảng được PARTITION BY RANGE (clicked_at).
     * PostgreSQL yêu cầu partition key phải nằm trong PRIMARY KEY.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
                    generator  = "click_events_seq")
    @SequenceGenerator(name           = "click_events_seq",
                       sequenceName   = "click_events_id_seq",
                       allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Id
    @Column(name = "clicked_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    @Builder.Default
    private OffsetDateTime clickedAt = OffsetDateTime.now();

    // ----------------------------------------------------------------
    // Không dùng @ManyToOne để tránh JOIN overhead trên hot write path.
    // FK enforce ở application layer.
    // ----------------------------------------------------------------

    @Column(name = "short_url_id", nullable = false)
    private Long shortUrlId;

    @Column(name = "session_id", columnDefinition = "UUID")
    private UUID sessionId;

    // ----------------------------------------------------------------
    // Network
    // ----------------------------------------------------------------

    /** PostgreSQL INET type — lưu dạng String, cast bởi DB */
    @Column(name = "ip_address", columnDefinition = "INET")
    private String ipAddress;

    @Column(name = "ip_country", length = 8)
    private String ipCountry;

    @Column(name = "ip_city", length = 100)
    private String ipCity;

    @Column(name = "ip_latitude",  precision = 9, scale = 6,
            columnDefinition = "DECIMAL(9,6)")
    private BigDecimal ipLatitude;

    @Column(name = "ip_longitude", precision = 9, scale = 6,
            columnDefinition = "DECIMAL(9,6)")
    private BigDecimal ipLongitude;

    // ----------------------------------------------------------------
    // HTTP headers
    // ----------------------------------------------------------------

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "referer", columnDefinition = "TEXT")
    private String referer;

    @Column(name = "referer_domain", length = 255)
    private String refererDomain;

    // ----------------------------------------------------------------
    // Parsed device info (parse tại app trước khi insert, tránh parse lúc query)
    // ----------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 16)
    @Builder.Default
    private DeviceType deviceType = DeviceType.UNKNOWN;

    @Column(name = "browser",         length = 64)
    private String browser;

    @Column(name = "browser_version", length = 32)
    private String browserVersion;

    @Column(name = "os",              length = 64)
    private String os;

    @Column(name = "os_version",      length = 32)
    private String osVersion;

    // ----------------------------------------------------------------
    // Enums
    // ----------------------------------------------------------------

    public enum DeviceType {
        DESKTOP, MOBILE, TABLET, BOT, UNKNOWN
    }

    // ----------------------------------------------------------------
    // Composite PK class
    // ----------------------------------------------------------------

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ClickEventId implements Serializable {
        private Long id;
        private OffsetDateTime clickedAt;
    }
}
