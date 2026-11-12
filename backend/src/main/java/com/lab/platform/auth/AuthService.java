package com.lab.platform.auth;

import com.lab.platform.booking.Booking;
import com.lab.platform.booking.BookingRepository;
import com.lab.platform.booking.BookingStatus;
import com.lab.platform.common.BadRequestException;
import com.lab.platform.common.ConflictException;
import com.lab.platform.common.ResourceNotFoundException;
import com.lab.platform.common.UnauthorizedException;
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
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final DepartmentRepository departmentRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthService(UserRepository userRepository,
                       InstitutionRepository institutionRepository,
                       DepartmentRepository departmentRepository,
                       BookingRepository bookingRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.institutionRepository = institutionRepository;
        this.departmentRepository = departmentRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email address already exists.");
        }

        // Strict Requirement: Nobody must be able to register as a System Administrator.
        if (request.getRole() == Role.SYSTEM_ADMIN) {
            throw new BadRequestException("System Administrator accounts cannot be created via public registration.");
        }

        Institution institution = null;
        if (request.getInstitutionId() != null) {
            institution = institutionRepository.findById(request.getInstitutionId()).orElse(null);
        }

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId()).orElse(null);
        }

        // Default public registration role is RESEARCHER_STUDENT
        Role assignedRole = (request.getRole() != null) ? request.getRole() : Role.RESEARCHER_STUDENT;

        // If someone attempts Department Head registration through normal endpoint,
        // redirect them to the Department Head registration workflow
        if (assignedRole == Role.DEPARTMENT_HEAD) {
            throw new BadRequestException("Department Head registration must include department details via /api/auth/register-department-head.");
        }

        User user = new User();
        user.setEmail(email);
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

    @Transactional
    public String registerDepartmentHead(DepartmentHeadRegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email address already exists.");
        }

        Institution institution = institutionRepository.findById(request.getInstitutionId())
                .orElseThrow(() -> new ResourceNotFoundException("Institution not found with id: " + request.getInstitutionId()));

        // Department Head registration creates an inactive Department and an inactive User
        Department department = new Department();
        department.setName(request.getDepartmentName().trim());
        department.setCode(request.getDepartmentCode().trim().toUpperCase());
        department.setInstitution(institution);
        department.setActive(false);
        Department savedDept = departmentRepository.save(department);

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(Role.DEPARTMENT_HEAD);
        user.setInstitution(institution);
        user.setDepartment(savedDept);
        user.setActive(false); // Awaits Institution Administrator approval
        userRepository.save(user);

        logger.info("Department Head registration submitted for {} (Dept: {}). Awaiting approval.", email, savedDept.getName());
        return "Department Head registration submitted successfully. Your account and department are awaiting approval by your Institution Administrator.";
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BadRequestException("Invalid email or password."));

        if (!user.isActive()) {
            throw new BadRequestException("This account is currently inactive or awaiting administrative approval. Please contact your Institution Administrator.");
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
            user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setFullName(request.getName());
            user.setAvatarUrl(request.getAvatarUrl());
            user.setRole(Role.RESEARCHER_STUDENT);
            user.setActive(true);
            user = userRepository.save(user);
            logger.info("[AUTH - GOOGLE OAUTH2] Dynamically provisioned account for: {} with role: {}", email, Role.RESEARCHER_STUDENT);
        } else {
            if (!user.isActive()) {
                throw new BadRequestException("This account is currently inactive or awaiting administrative approval.");
            }
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

    @Transactional
    public void updateUserRoleAndDepartment(Long targetUserId, Role newRole, Long institutionId, Long departmentId, UserPrincipal caller) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found with id: " + targetUserId));

        // System Administrator can assign any role and change institution/department
        // Institution Administrator can only manage users within their own institution
        if (caller.getRole() == Role.SYSTEM_ADMIN) {
            // Check: Last active System Admin cannot be demoted
            if (target.getRole() == Role.SYSTEM_ADMIN && newRole != Role.SYSTEM_ADMIN) {
                if (userRepository.countByRoleAndActiveTrue(Role.SYSTEM_ADMIN) <= 1) {
                    throw new BadRequestException("The last active System Administrator cannot be demoted.");
                }
            }
        } else if (caller.getRole() == Role.INSTITUTION_ADMIN) {
            User callerUser = userRepository.findById(caller.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Caller user not found"));

            if (callerUser.getInstitution() == null || target.getInstitution() == null ||
                    !callerUser.getInstitution().getId().equals(target.getInstitution().getId())) {
                throw new UnauthorizedException("Institution Administrators can only manage users within their own institution.");
            }

            // Institution Admin cannot assign INSTITUTION_ADMIN or SYSTEM_ADMIN
            if (newRole == Role.SYSTEM_ADMIN || newRole == Role.INSTITUTION_ADMIN) {
                throw new UnauthorizedException("Institution Administrators cannot assign Administrator roles.");
            }

            if (target.getRole() == Role.SYSTEM_ADMIN || target.getRole() == Role.INSTITUTION_ADMIN) {
                throw new UnauthorizedException("Institution Administrators cannot modify Administrator accounts.");
            }
        } else {
            throw new UnauthorizedException("You do not have permission to modify user roles.");
        }

        if (newRole != null) {
            target.setRole(newRole);
        }

        if (institutionId != null) {
            Institution inst = institutionRepository.findById(institutionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Institution not found with id: " + institutionId));
            target.setInstitution(inst);
        }

        if (departmentId != null) {
            Department dept = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));
            target.setDepartment(dept);
        }

        userRepository.save(target);
        logger.info("User {} (ID: {}) updated with role: {} by {}", target.getEmail(), target.getId(), newRole, caller.getUsername());
    }

    @Transactional
    public void deactivateUser(Long targetUserId, UserPrincipal caller) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetUserId));

        if (target.getRole() == Role.SYSTEM_ADMIN) {
            if (userRepository.countByRoleAndActiveTrue(Role.SYSTEM_ADMIN) <= 1) {
                throw new BadRequestException("The last active System Administrator cannot be deactivated.");
            }
        }

        if (caller.getRole() != Role.SYSTEM_ADMIN) {
            User callerUser = userRepository.findById(caller.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Caller user not found"));
            if (caller.getRole() != Role.INSTITUTION_ADMIN || callerUser.getInstitution() == null ||
                    target.getInstitution() == null || !callerUser.getInstitution().getId().equals(target.getInstitution().getId())) {
                throw new UnauthorizedException("You do not have permission to deactivate this user.");
            }
            if (target.getRole() == Role.INSTITUTION_ADMIN || target.getRole() == Role.SYSTEM_ADMIN) {
                throw new UnauthorizedException("Cannot deactivate an administrator.");
            }
        }

        target.setActive(false);
        userRepository.save(target);

        // When a user is deactivated, their future bookings are cancelled with the reason "User deactivated"
        List<Booking> futureBookings = bookingRepository.findByUserIdOrderByStartTimeDesc(target.getId());
        LocalDateTime now = LocalDateTime.now();
        for (Booking b : futureBookings) {
            if (b.getStartTime().isAfter(now) &&
                    (b.getStatus() == BookingStatus.PENDING_APPROVAL || b.getStatus() == BookingStatus.CONFIRMED)) {
                b.setStatus(BookingStatus.CANCELLED);
                b.setCancellationReason("User deactivated");
                bookingRepository.save(b);
            }
        }
        logger.info("User {} (ID: {}) deactivated by {}. Future bookings cancelled.", target.getEmail(), target.getId(), caller.getUsername());
    }
}
