package com.dinhuan.shortify.dto.user;

import com.dinhuan.shortify.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserProfile {
    private Long id;
    private String displayName;
    private String email;
    private String planType;
}
