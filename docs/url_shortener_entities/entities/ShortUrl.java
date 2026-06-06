package com.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "short_urls", indexes = {
        // Hot path — redirect lookup
        @Index(name = "idx_redirect_lookup", columnList = "short_code"),
        // Cleanup job — tìm link hết hạn
        @Index(name = "idx_expires_at",      columnList = "expires_at"),
        // User dashboard
        @Index(name = "idx_short_urls_user_id", columnList = "user_id"),
        @Index(name = "idx_short_urls_folder",  columnList = "folder_id")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortUrl {

    @Id
    @Column(name = "id", nullable = false)
    private Long id; // Snowflake ID

    @Column(name = "short_code", nullable = false, unique = true, length = 16)
    private String shortCode;

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    // ----------------------------------------------------------------
    // Relations — FK
    // ----------------------------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",
                foreignKey = @ForeignKey(name = "fk_short_urls_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id",
                foreignKey = @ForeignKey(name = "fk_short_urls_folder"))
    private Folder folder;

    // ----------------------------------------------------------------
    // Metadata
    // ----------------------------------------------------------------

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "preview_title", columnDefinition = "TEXT")
    private String previewTitle;

    @Column(name = "preview_image", columnDefinition = "TEXT")
    private String previewImage;

    @Column(name = "custom_alias", nullable = false)
    @Builder.Default
    private boolean customAlias = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    // ----------------------------------------------------------------
    // Password protection
    // ----------------------------------------------------------------

    @Column(name = "password_protected", nullable = false)
    @Builder.Default
    private boolean passwordProtected = false;

    /**
     * bcrypt hash của password. NULL khi passwordProtected = false.
     * Validate bằng @PrePersist / @PreUpdate — mirror CHECK constraint DB.
     */
    @Column(name = "password_hash", columnDefinition = "TEXT")
    private String passwordHash;

    // ----------------------------------------------------------------
    // UTM auto-append
    // ----------------------------------------------------------------

    @Column(name = "utm_source",   length = 255)
    private String utmSource;

    @Column(name = "utm_medium",   length = 255)
    private String utmMedium;

    @Column(name = "utm_campaign", length = 255)
    private String utmCampaign;

    // ----------------------------------------------------------------
    // Stats (denormalized — cập nhật async qua batch job)
    // ----------------------------------------------------------------

    @Column(name = "click_count", nullable = false)
    @Builder.Default
    private Long clickCount = 0L;

    // ----------------------------------------------------------------
    // Timestamps
    // ----------------------------------------------------------------

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime updatedAt;

    @Column(name = "expires_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime expiresAt;

    // ----------------------------------------------------------------
    // Relations — children
    // ----------------------------------------------------------------

    @OneToMany(mappedBy = "shortUrl", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ClickEvent> clickEvents = new ArrayList<>();

    @OneToMany(mappedBy = "shortUrl", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DailyStat> dailyStats = new ArrayList<>();

    /**
     * Owning side của many-to-many với Tag.
     * JoinTable tương ứng bảng url_tags trong DB.
     */
    @ManyToMany
    @JoinTable(
            name = "url_tags",
            joinColumns        = @JoinColumn(name = "short_url_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"),
            foreignKey         = @ForeignKey(name = "fk_url_tags_short_url"),
            inverseForeignKey  = @ForeignKey(name = "fk_url_tags_tag")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    // ----------------------------------------------------------------
    // Lifecycle validation — mirror DB CHECK constraint
    // ----------------------------------------------------------------

    @PrePersist
    @PreUpdate
    private void validatePasswordConsistency() {
        if (passwordProtected && (passwordHash == null || passwordHash.isBlank())) {
            throw new IllegalStateException(
                    "ShortUrl: passwordHash must not be null when passwordProtected=true");
        }
        if (!passwordProtected && passwordHash != null) {
            throw new IllegalStateException(
                    "ShortUrl: passwordHash must be null when passwordProtected=false");
        }
    }

    // ----------------------------------------------------------------
    // Business helpers
    // ----------------------------------------------------------------

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(OffsetDateTime.now());
    }

    public boolean isAccessible() {
        return isActive && !isExpired();
    }

    /**
     * Trả về original_url đã gắn UTM params (nếu có).
     */
    public String buildRedirectUrl() {
        if (utmSource == null && utmMedium == null && utmCampaign == null) {
            return originalUrl;
        }
        StringBuilder sb = new StringBuilder(originalUrl);
        sb.append(originalUrl.contains("?") ? "&" : "?");
        if (utmSource   != null) sb.append("utm_source=").append(utmSource).append("&");
        if (utmMedium   != null) sb.append("utm_medium=").append(utmMedium).append("&");
        if (utmCampaign != null) sb.append("utm_campaign=").append(utmCampaign);
        // Xoá trailing "&" nếu có
        String result = sb.toString();
        return result.endsWith("&") ? result.substring(0, result.length() - 1) : result;
    }
}
