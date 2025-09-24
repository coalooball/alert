package com.alert.system.dto;

import com.alert.system.entity.User;
import com.alert.system.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String username;
    private String department;
    private UserRole role;
    private LocalDateTime createdAt;

    public static UserResponse fromUser(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setDepartment(user.getDepartment());
        response.setRole(UserRole.fromString(user.getRole()));
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}