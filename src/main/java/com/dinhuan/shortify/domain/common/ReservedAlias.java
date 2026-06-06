package com.dinhuan.shortify.domain.common;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "reserved_aliases")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservedAlias {

    @Id
    @Column(name = "alias", length = 64)
    private String alias;

    @Column(name = "reason", length = 255)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;
}
