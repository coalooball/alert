package com.alert.system.controller;

import com.alert.system.dto.CreateUserRequest;
import com.alert.system.dto.UserResponse;
import com.alert.system.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            UserResponse user = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(
            @PathVariable UUID id,
            HttpServletRequest request) {

        String currentUserId = (String) request.getAttribute("userId");

        // Prevent users from deleting themselves
        if (id.toString().equals(currentUserId)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Cannot delete your own account");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        boolean deleted = userService.deleteUser(id);
        if (deleted) {
            Map<String, String> result = new HashMap<>();
            result.put("message", "User deleted successfully");
            return ResponseEntity.ok(result);
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}