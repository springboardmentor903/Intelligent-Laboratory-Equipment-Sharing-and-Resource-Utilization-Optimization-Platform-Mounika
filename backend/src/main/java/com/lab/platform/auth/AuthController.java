package com.lab.platform.auth;

import com.lab.platform.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok("Registration successful", response));
    }

    @PostMapping("/register-department-head")
    public ResponseEntity<ApiResponse<String>> registerDepartmentHead(@Valid @RequestBody DepartmentHeadRegisterRequest request) {
        String msg = authService.registerDepartmentHead(request);
        return ResponseEntity.ok(ApiResponse.ok(msg, msg));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
        AuthResponse response = authService.loginWithGoogle(request);
        return ResponseEntity.ok(ApiResponse.ok("Google login successful", response));
    }

    @PostMapping("/password-reset-request")
    public ResponseEntity<ApiResponse<String>> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        String msg = authService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.ok(msg, msg));
    }

    @PostMapping("/password-reset-confirm")
    public ResponseEntity<ApiResponse<String>> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.ok("Password has been reset successfully. You can now log in."));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserProfileResponse profile = authService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse updated = authService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", updated));
    }

    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<String>> updateUserRole(
            @PathVariable Long userId,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) Long departmentId,
            @AuthenticationPrincipal UserPrincipal caller) {
        authService.updateUserRoleAndDepartment(userId, role, institutionId, departmentId, caller);
        return ResponseEntity.ok(ApiResponse.ok("User role and department updated successfully."));
    }

    @PutMapping("/users/{userId}/deactivate")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deactivateUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal caller) {
        authService.deactivateUser(userId, caller);
        return ResponseEntity.ok(ApiResponse.ok("User deactivated successfully. Future bookings cancelled."));
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getAllRoles() {
        List<Map<String, String>> roles = Arrays.stream(Role.values())
                .map(r -> Map.of("role", r.name(), "displayName", r.getDisplayName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(roles));
    }
}
