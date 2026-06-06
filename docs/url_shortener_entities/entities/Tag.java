package com.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tags",
       indexes = { @Index(name = "idx_tags_user_id", columnList = "user_id") },
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_tags_user_name",
                             columnNames = {"user_id", "name"})
       })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @Column(name = "id", nullable = false)
    private Long id; // Snowflake ID

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_tags_user"))
    private User user;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    /** Hex color, ví dụ: #6366f1 */
    @Column(name = "color", length = 7)
    @Builder.Default
    private String color = "#6366f1";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    // ----------------------------------------------------------------
    // Relations — owning side của many-to-many qua url_tags
    // ----------------------------------------------------------------

    @ManyToMany(mappedBy = "tags")
    @Builder.Default
    private Set<ShortUrl> shortUrls = new HashSet<>();
}
