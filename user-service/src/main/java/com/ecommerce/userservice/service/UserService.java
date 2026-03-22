package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dao.User;
import com.ecommerce.userservice.dto.*;
import com.ecommerce.userservice.exception.*;
import com.ecommerce.userservice.mapper.UserMapper;
import com.ecommerce.userservice.repository.UserRepository;
import com.ecommerce.userservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service @RequiredArgsConstructor @Slf4j
public class UserService {

    private final UserRepository repo;
    private final UserMapper     mapper;
    private final PasswordEncoder encoder;
    private final JwtService      jwt;

    // AUTH
    public AuthResponse register(RegisterRequest req) {
        log.info("Register email={}", req.getEmail());
        if (repo.existsByEmail(req.getEmail()))
            throw new UserAlreadyExistsException("Email already registered: " + req.getEmail());

        User user = User.builder()
                .email(req.getEmail())
                .password(encoder.encode(req.getPassword()))
                .firstName(req.getFirstName()).lastName(req.getLastName())
                .phone(req.getPhone())
                .role(User.Role.ROLE_USER).enabled(true).accountNonLocked(true)
                .failedLoginAttempts(0).addresses(new ArrayList<>()).build();

        User saved = repo.save(user);
        log.info("Registered userId={}", saved.getId());
        return buildAuth(saved);
    }

    public AuthResponse login(LoginRequest req) {
        log.info("Login email={}", req.getEmail());
        User user = repo.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.isAccountNonLocked())
            throw new AccountLockedException("Account locked. Contact support.");

        if (!encoder.matches(req.getPassword(), user.getPassword())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= 5) {
                user.setAccountNonLocked(false);
                log.warn("Account locked userId={}", user.getId());
            }
            repo.save(user);
            throw new BadCredentialsException("Invalid credentials");
        }

        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            repo.save(user);
        }
        log.info("Login success userId={}", user.getId());
        return buildAuth(user);
    }

    // ── PROFILE ───────────────────────────────────────────────────────────────
    public UserResponse getById(String userId) {
        return mapper.toResponse(findOrThrow(userId));
    }

    public Page<UserResponse> getAll(Pageable p) {
        return repo.findByEnabledTrue(p).map(mapper::toResponse);
    }

    public UserResponse updateProfile(String userId, UpdateProfileRequest req) {
        User user = findOrThrow(userId);
        mapper.updateFromRequest(req, user);
        return mapper.toResponse(repo.save(user));
    }

    public void deactivate(String userId) {
        User user = findOrThrow(userId);
        user.setEnabled(false);
        repo.save(user);
        log.info("User deactivated userId={}", userId);
    }

    // ── Private ───────────────────────────────────────────────────────────────
    private User findOrThrow(String userId) {
        return repo.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }

    private AuthResponse buildAuth(User user) {
        return AuthResponse.builder()
                .accessToken(jwt.generateToken(user))
                .tokenType("Bearer")
                .expiresIn(jwt.getExpiration() / 1000)
                .userId(user.getId()).email(user.getEmail())
                .firstName(user.getFirstName()).lastName(user.getLastName())
                .role(user.getRole().name()).build();
    }
}
