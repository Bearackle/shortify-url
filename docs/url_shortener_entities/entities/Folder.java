package com.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "folders",
       indexes = { @Index(name = "idx_folders_user_id", columnList = "user_id") },
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_folders_user_name",
                             columnNames = {"user_id", "name"})
       })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Folder {

    @Id
    @Column(name = "id", nullable = false)
    private Long id; // Snowflake ID

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_folders_user"))
    private User user;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    // ----------------------------------------------------------------
    // Relations
    // ----------------------------------------------------------------

    @OneToMany(mappedBy = "folder")
    @Builder.Default
    private List<ShortUrl> shortUrls = new ArrayList<>();
}
