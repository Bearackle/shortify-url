package com.dinhuan.shortify.domain.user;

import com.dinhuan.shortify.domain.folder.Folder;
import com.dinhuan.shortify.domain.security.ApiKey;
import com.dinhuan.shortify.domain.shorturl.ShortUrl;
import com.dinhuan.shortify.domain.tag.Tag;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "id", nullable = false)
    private Long id; // Snowflake ID — sinh từ application (SnowflakeIdGenerator)

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, columnDefinition = "TEXT")
    private String passwordHash;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 20)
    @Builder.Default
    private PlanType planType = PlanType.FREE;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime updatedAt;

    // ----------------------------------------------------------------
    // Relations
    // ----------------------------------------------------------------

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ShortUrl> shortUrls = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ApiKey> apiKeys = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Folder> folders = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Tag> tags = new ArrayList<>();

    // ----------------------------------------------------------------
    // Enum
    // ----------------------------------------------------------------

    public enum PlanType {
        FREE, PRO, TEAM, ENTERPRISE
    }
}
