package com.alert.system.service;

import com.alert.system.dto.CreateUserRequest;
import com.alert.system.dto.LoginRequest;
import com.alert.system.dto.LoginResponse;
import com.alert.system.dto.UserResponse;
import com.alert.system.entity.User;
import com.alert.system.repository.UserRepository;
import com.alert.system.security.JwtUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import com.alert.system.enums.UserRole;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${app.init-db:false}")
    private boolean initDb;

    @PostConstruct
    public void init() {
        // Create default admin user if not exists
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setDepartment("系统管理部");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setRole(UserRole.ADMIN.getValue());
            userRepository.save(admin);
            System.out.println("✅ Default admin user created: admin/admin123");
        }
    }

    public LoginResponse login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPasswordHash())) {
            throw new RuntimeException("Invalid username or password");
        }

        User user = userOpt.get();
        String token = jwtUtils.generateToken(user);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUser(UserResponse.fromUser(user));

        return response;
    }

    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setDepartment(request.getDepartment());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole().getValue());

        User savedUser = userRepository.save(user);
        return UserResponse.fromUser(savedUser);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    public boolean deleteUser(UUID userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
            return true;
        }
        return false;
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}