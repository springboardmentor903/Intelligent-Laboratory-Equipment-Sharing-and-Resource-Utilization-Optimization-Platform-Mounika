package com.lab.platform.booking;

import com.lab.platform.auth.Role;
import com.lab.platform.auth.User;
import com.lab.platform.auth.UserRepository;
import com.lab.platform.common.BadRequestException;
import com.lab.platform.common.ConflictException;
import com.lab.platform.equipment.Equipment;
import com.lab.platform.equipment.EquipmentRepository;
import com.lab.platform.equipment.EquipmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    private User researcher;
    private Equipment equipment;
    private LocalDateTime futureStart;
    private LocalDateTime futureEnd;

    @BeforeEach
    void setUp() {
        researcher = new User();
        researcher.setId(10L);
        researcher.setEmail("researcher@lab.edu");
        researcher.setFullName("Dr. Elena Rostova");
        researcher.setRole(Role.ROLE_RESEARCHER);

        equipment = new Equipment();
        equipment.setId(100L);
        equipment.setName("Bruker NMR 600");
        equipment.setStatus(EquipmentStatus.AVAILABLE);

        futureStart = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        futureEnd = futureStart.plusHours(2);
    }

    @Test
    @DisplayName("Should successfully create a booking when time slot is free")
    void testCreateBooking_Success() {
        BookingRequest request = new BookingRequest();
        request.setEquipmentId(100L);
        request.setStartTime(futureStart);
        request.setEndTime(futureEnd);
        request.setPurpose("Sample analysis");

        when(userRepository.findByEmail("researcher@lab.edu")).thenReturn(Optional.of(researcher));
        when(equipmentRepository.findById(100L)).thenReturn(Optional.of(equipment));
        // No overlapping bookings found
        when(bookingRepository.countOverlappingBookings(eq(100L), eq(futureStart), eq(futureEnd), anyCollection(), isNull()))
                .thenReturn(0L);

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(500L);
            return b;
        });

        Booking created = bookingService.createBooking(request, "researcher@lab.edu");

        assertNotNull(created);
        assertEquals(BookingStatus.PENDING_APPROVAL, created.getStatus());
        assertEquals("Sample analysis", created.getPurpose());
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when an overlapping booking exists")
    void testCreateBooking_CollisionConflict() {
        BookingRequest request = new BookingRequest();
        request.setEquipmentId(100L);
        request.setStartTime(futureStart);
        request.setEndTime(futureEnd);
        request.setPurpose("Conflicting reservation");

        when(userRepository.findByEmail("researcher@lab.edu")).thenReturn(Optional.of(researcher));
        when(equipmentRepository.findById(100L)).thenReturn(Optional.of(equipment));
        // Overlap found!
        when(bookingRepository.countOverlappingBookings(eq(100L), eq(futureStart), eq(futureEnd), anyCollection(), isNull()))
                .thenReturn(1L);

        assertThrows(ConflictException.class, () -> {
            bookingService.createBooking(request, "researcher@lab.edu");
        });

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should reject booking if equipment is Under Maintenance")
    void testCreateBooking_EquipmentUnderMaintenance() {
        equipment.setStatus(EquipmentStatus.UNDER_MAINTENANCE);

        BookingRequest request = new BookingRequest();
        request.setEquipmentId(100L);
        request.setStartTime(futureStart);
        request.setEndTime(futureEnd);
        request.setPurpose("Analysis");

        when(userRepository.findByEmail("researcher@lab.edu")).thenReturn(Optional.of(researcher));
        when(equipmentRepository.findById(100L)).thenReturn(Optional.of(equipment));

        assertThrows(BadRequestException.class, () -> {
            bookingService.createBooking(request, "researcher@lab.edu");
        });
    }

    @Test
    @DisplayName("Should approve booking and transition status to CONFIRMED")
    void testApproveBooking_Success() {
        User manager = new User();
        manager.setId(2L);
        manager.setEmail("manager@lab.edu");
        manager.setRole(Role.ROLE_LAB_MANAGER);

        Booking booking = new Booking();
        booking.setId(300L);
        booking.setStatus(BookingStatus.PENDING_APPROVAL);

        when(bookingRepository.findById(300L)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail("manager@lab.edu")).thenReturn(Optional.of(manager));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        BookingApprovalRequest req = new BookingApprovalRequest(true, "Approved for research");
        Booking result = bookingService.processApproval(300L, req, "manager@lab.edu");

        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
        assertEquals(manager, result.getApprovedBy());
    }
}
