package com.lab.platform.auth;

import com.lab.platform.common.BadRequestException;
import com.lab.platform.institution.DepartmentRepository;
import com.lab.platform.institution.InstitutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private InstitutionRepository institutionRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFullName("Alice Student");
        registerRequest.setEmail("alice@test.edu");
        registerRequest.setPassword("SecretPass123");
    }

    @Test
    @DisplayName("First user registered on an empty database is granted ROLE_SYSTEM_ADMIN")
    void testRegister_FirstUser_SystemAdmin() {
        when(userRepository.existsByEmail("alice@test.edu")).thenReturn(false);
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("SecretPass123")).thenReturn("hashedPasswordXYZ");
        when(jwtUtils.generateToken(any(User.class))).thenReturn("mockJwtToken");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(1L);
            return u;
        });

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals(Role.ROLE_SYSTEM_ADMIN, response.getRole());
    }

    @Test
    @DisplayName("Public registration for subsequent users must strictly assign ROLE_RESEARCHER")
    void testRegister_SubsequentUsers_RoleResearcher() {
        when(userRepository.existsByEmail("alice@test.edu")).thenReturn(false);
        when(userRepository.count()).thenReturn(1L); // Not first user
        when(passwordEncoder.encode("SecretPass123")).thenReturn("hashedPasswordXYZ");
        when(jwtUtils.generateToken(any(User.class))).thenReturn("mockJwtToken");

        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(99L);
            return u;
        });

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("mockJwtToken", response.getToken());
        assertEquals(Role.ROLE_RESEARCHER, response.getRole());
        assertEquals("alice@test.edu", response.getEmail());

        verify(passwordEncoder, times(1)).encode("SecretPass123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Registration should fail when email already exists")
    void testRegister_DuplicateEmail() {
        when(userRepository.existsByEmail("alice@test.edu")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> {
            authService.register(registerRequest);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Login should succeed with correct credentials")
    void testLogin_Success() {
        User user = new User();
        user.setId(1L);
        user.setEmail("alice@test.edu");
        user.setPasswordHash("hashedPass");
        user.setFullName("Alice Student");
        user.setRole(Role.ROLE_RESEARCHER);
        user.setActive(true);

        when(userRepository.findByEmail("alice@test.edu")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SecretPass123", "hashedPass")).thenReturn(true);
        when(jwtUtils.generateToken(user)).thenReturn("validToken123");

        LoginRequest loginRequest = new LoginRequest("alice@test.edu", "SecretPass123");
        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("validToken123", response.getToken());
        assertEquals(Role.ROLE_RESEARCHER, response.getRole());
    }

    @Test
    @DisplayName("Login should fail with incorrect password")
    void testLogin_WrongPassword() {
        User user = new User();
        user.setEmail("alice@test.edu");
        user.setPasswordHash("hashedPass");
        user.setActive(true);

        when(userRepository.findByEmail("alice@test.edu")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "hashedPass")).thenReturn(false);

        LoginRequest loginRequest = new LoginRequest("alice@test.edu", "WrongPassword");
        assertThrows(BadRequestException.class, () -> {
            authService.login(loginRequest);
        });
    }
}
