# Milestone 1: Implementation Status & Mentor Review Readiness

This document records the exact verification status of **Milestone 1** of the Lab Resource Utilization Platform, documenting what is complete, what remains deferred for later milestones, known limitations, and pre-review readiness.

---

## 1. Feature-by-Feature Status Matrix

| Component / Requirement | Target Milestone | Current Status | Verification Evidence |
| :--- | :---: | :---: | :--- |
| **Modular Monolith Backend** | M1 | **COMPLETE** | Clean package boundaries (`auth`, `institution`, `equipment`, `booking`, `common`, `config`). Compiled with Java 21+ without warnings. |
| **Relational Database Schemas** | M1 | **COMPLETE** | Tables (`users`, `institutions`, `departments`, `equipment`, `bookings`) generated dynamically with foreign keys and indexes. |
| **Stateless JWT Authentication** | M1 | **COMPLETE** | Validated via `AuthServiceTest.testLogin_Success` and live endpoint tests returning 200 OK with Bearer tokens. |
| **6-Role RBAC System** | M1 | **COMPLETE** | Validated via `AuthServiceTest.testRegister_SubsequentUsers_RoleResearcher` and live tests blocking unauthorized equipment creation with 403 Forbidden. |
| **Dynamic Facility Setup** | M1 | **COMPLETE** | Dynamic `POST /api/institutions` and `POST /api/institutions/{id}/departments` APIs with frontend inline quick-add modals. Zero hardcoded seed data. |
| **Equipment Inventory CRUD** | M1 | **COMPLETE** | Multi-criteria search (keyword, category, status, institution) and registration modal for authorized staff. |
| **FullCalendar.js Scheduling Grid** | M1 | **COMPLETE** | Live week/day/month views rendering bookings with color coding directly from `/api/bookings/equipment/{id}/calendar`. |
| **Interval Collision Detection** | M1 | **COMPLETE** | Validated via `BookingServiceTest.testCreateBooking_CollisionConflict` and live API tests returning HTTP 409 Conflict on overlapping bookings. |
| **Maintenance Booking Lock** | M1 | **COMPLETE** | Validated via `BookingServiceTest.testCreateBooking_EquipmentUnderMaintenance` rejecting bookings when equipment is non-operational. |
| **Approvals Queue & Cancellation** | M1 | **COMPLETE** | Validated via `BookingServiceTest.testApproveBooking_Success` and live UI workflow confirming pending bookings. |
| **Real-time WebSockets Telemetry** | M2 | *Planned (M2)* | Deferred to Milestone 2 per the approved 4-milestone roadmap. |
| **Utilization Heatmaps & Idle Alerts** | M2 | *Planned (M2)* | Deferred to Milestone 2 per the approved 4-milestone roadmap. |
| **Inter-Institution Sharing Workflow** | M2 | *Planned (M2)* | Deferred to Milestone 2 per the approved 4-milestone roadmap. |
| **Maintenance Work Orders & Calibration Docs**| M3 | *Planned (M3)* | Deferred to Milestone 3 per the approved 4-milestone roadmap. |
| **Billing, Invoicing & Analytics Dashboards** | M3 | *Planned (M3)* | Deferred to Milestone 3 per the approved 4-milestone roadmap. |
| **Docker Compose & CI/CD Pipeline** | M4 | *Planned (M4)* | Deferred to Milestone 4 per the approved 4-milestone roadmap. |

---

## 2. Test Evidence & Automated Verification Results

### 2.1 Backend Unit & Integration Tests (JUnit 5 + Mockito)
Command: `.\mvnw.cmd test` inside `/backend`
* **Tests Executed:** 9
* **Failures:** 0
* **Errors:** 0
* **Skipped:** 0
* **Build Outcome:** `BUILD SUCCESS` (Execution time: 13.5s)

```
Results:
[INFO] Running com.lab.platform.auth.AuthServiceTest
- testRegister_FirstUser_SystemAdmin: PASSED
- testRegister_SubsequentUsers_RoleResearcher: PASSED
- testRegister_DuplicateEmail: PASSED
- testLogin_Success: PASSED
- testLogin_WrongPassword: PASSED
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0

[INFO] Running com.lab.platform.booking.BookingServiceTest
- testCreateBooking_Success: PASSED
- testCreateBooking_CollisionConflict: PASSED
- testCreateBooking_EquipmentUnderMaintenance: PASSED
- testApproveBooking_Success: PASSED
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```

### 2.2 Frontend Production Build
Command: `npm run build` inside `/frontend`
* **Vite Version:** 5.4.21
* **Transformed Modules:** 1,968
* **Output:** `dist/index.html` (0.73 kB), `dist/assets/index.js` (582 kB), `dist/assets/index.css` (25 kB)
* **Outcome:** `✓ built in 18.88s` with 0 errors.

---

## 3. Known Limitations & Technical Debt (For Mentor Disclosure)

To demonstrate intellectual honesty to your mentor, be prepared to mention these items if asked:

1. **In-Memory Database by Default:**
   * *Status:* The default local profile uses in-memory H2 in PostgreSQL compatibility mode.
   * *Impact:* Data resets when the backend process restarts. This is intentional for smooth development without database server dependencies. Real PostgreSQL is fully supported via environment variables (`SPRING_DATASOURCE_URL`).
2. **Calendar Refreshes on Action Rather than Push:**
   * *Status:* The availability calendar refreshes upon user booking submission or page load.
   * *Impact:* If two researchers have the page open simultaneously, real-time push updates require WebSockets, which are scheduled for **Milestone 2**.
3. **No External SMTP Mail Server Required:**
   * *Status:* Password reset tokens log directly to the server terminal (`[NOTIFICATION] Password reset token generated for...`).
   * *Impact:* Safe and zero-cost for development; real SMTP connectors will be wired in Milestone 3 with notification dispatchers.

---

## 4. Final Review Readiness Checklist

Before scheduling your mentor presentation, confirm the following:

- [x] Backend compiles and passes all 9 unit tests cleanly.
- [x] Frontend builds with Vite with zero errors.
- [x] Zero hardcoded records: starts cleanly with no dummy users or mock data.
- [x] First registered user dynamically bootstraps as `ROLE_SYSTEM_ADMIN`.
- [x] Subsequent registrations enforce `ROLE_RESEARCHER`.
- [x] Dynamic facility creation works (Institutions and Departments).
- [x] FullCalendar displays events and handles date/time selection.
- [x] Overlapping booking collisions are mathematically detected and rejected with HTTP 409 Conflict.
- [x] Approvals queue transitions reservation states properly.
- [x] Windows 1-click batch scripts (`run-backend.bat`, `run-frontend.bat`) tested and working.
- [x] **Milestone 2 implementation is strictly held until mentor approval is granted.**
