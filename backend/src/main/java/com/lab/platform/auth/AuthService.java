package com.lab.platform.auth;

import com.lab.platform.common.BadRequestException;
import com.lab.platform.common.ResourceNotFoundException;
import com.lab.platform.institution.Department;
import com.lab.platform.institution.DepartmentRepository;
import com.lab.platform.institution.Institution;
import com.lab.platform.institution.InstitutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthService(UserRepository userRepository,
                       InstitutionRepository institutionRepository,
                       DepartmentRepository departmentRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.institutionRepository = institutionRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("An account with this email address already exists.");
        }

        Institution institution = null;
        if (request.getInstitutionId() != null) {
            institution = institutionRepository.findById(request.getInstitutionId()).orElse(null);
        }

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId()).orElse(null);
        }

        // DYNAMIC SETUP LOGIC:
        // When the database has no registered accounts, the first user is granted ROLE_SYSTEM_ADMIN
        // so they can configure institutions, departments, and manage lab resources.
        // All subsequent public self-registrations are strictly restricted to ROLE_RESEARCHER.
        long totalUsers = userRepository.count();
        Role assignedRole = (totalUsers == 0) ? Role.ROLE_SYSTEM_ADMIN : Role.ROLE_RESEARCHER;

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(assignedRole);
        user.setInstitution(institution);
        user.setDepartment(department);
        user.setActive(true);

        User savedUser = userRepository.save(user);
        String token = jwtUtils.generateToken(savedUser);

        logger.info("Registered user: {} with assigned role: {}", savedUser.getEmail(), savedUser.getRole());

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getRole(),
                savedUser.getInstitution(),
                savedUser.getDepartment(),
                savedUser.getAvatarUrl()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BadRequestException("Invalid email or password."));

        if (!user.isActive()) {
            throw new BadRequestException("This account has been deactivated. Please contact your administrator.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password.");
        }

        String token = jwtUtils.generateToken(user);
        logger.info("User logged in successfully: {} with role {}", user.getEmail(), user.getRole());

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getInstitution(),
                user.getDepartment(),
                user.getAvatarUrl()
        );
    }

    @Transactional
    public AuthResponse loginWithGoogle(GoogleAuthRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        logger.info("[AUTH - GOOGLE OAUTH2] Processing Google authentication for email: {}", email);

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // Auto-provision dynamically if user does not exist
            long totalUsers = userRepository.count();
            Role assignedRole = (totalUsers == 0) ? Role.ROLE_SYSTEM_ADMIN : Role.ROLE_RESEARCHER;

            user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setFullName(request.getName());
            user.setAvatarUrl(request.getAvatarUrl());
            user.setRole(assignedRole);
            user.setActive(true);
            user = userRepository.save(user);
            logger.info("[AUTH - GOOGLE OAUTH2] Dynamically provisioned account for: {} with role: {}", email, assignedRole);
        } else {
            if (request.getAvatarUrl() != null && user.getAvatarUrl() == null) {
                user.setAvatarUrl(request.getAvatarUrl());
                userRepository.save(user);
            }
        }

        String token = jwtUtils.generateToken(user);
        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getInstitution(),
                user.getDepartment(),
                user.getAvatarUrl()
        );
    }

    @Transactional
    public String requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("No account registered with email: " + request.getEmail()));

        String resetToken = UUID.randomUUID().toString();
        user.setResetPasswordToken(resetToken);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        logger.info("[NOTIFICATION] Password reset token generated for {}: {}", user.getEmail(), resetToken);
        return "Password reset token generated. Token: " + resetToken;
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token."));

        if (user.getResetPasswordTokenExpiry() == null || user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This password reset token has expired. Please request a new one.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);

        logger.info("Password reset successfully confirmed for user: {}", user.getEmail());
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return new UserProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        user.setFullName(request.getFullName().trim());
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        User updated = userRepository.save(user);
        return new UserProfileResponse(updated);
    }
}
