# Milestone 1: Complete Technical Architecture & Code Walkthrough

This document provides a deep, code-level explanation of everything built in Milestone 1 of the **Lab Resource Utilization Platform**. It is written to serve as your permanent technical reference and study guide.

---

## 1. Project Overview & Architectural Strategy

### 1.1 The Core Problem & Objectives
Research institutions and universities spend millions of dollars acquiring advanced laboratory equipment (e.g., spectrophotometers, electron microscopes, NMR spectrometers). In practice, these assets frequently suffer from:
* **Low Visibility:** Researchers in one department have no easy way of discovering available instruments in another department or partner institution.
* **Scheduling Conflicts & Double-Bookings:** Paper logs or informal email reservation threads lead to collisions and wasted preparation time.
* **Unstructured Approvals:** Lab managers lack centralized control over who uses expensive equipment and for what purpose.

**Milestone 1 Objective:** Establish the complete foundational architectureâ€”a secure, multi-tenant modular monolith backend with a responsive React frontend, stateless JWT authentication, role-based access control (RBAC), dynamic inventory management, and an automated conflict-detecting reservation engine.

### 1.2 Technology Stack Justification

| Technology | Role | Why It Was Chosen |
| :--- | :--- | :--- |
| **Java 21+ / Spring Boot 3.4.x** | Backend Framework | Provides enterprise-grade reliability, strict compile-time type safety, mature Object-Relational Mapping (Spring Data JPA / Hibernate), and battle-tested security (Spring Security 6). |
| **React 18 + Vite** | Frontend Framework | Fast build times, hot module replacement (HMR), component-based architecture for reusable UI widgets, and a rich ecosystem for complex data visualization. |
| **Tailwind CSS** | Styling System | Utility-first CSS allowing rapid, responsive, and consistent styling without context-switching between separate CSS files. |
| **PostgreSQL (with in-memory H2)** | Database Layer | PostgreSQL provides ACID compliance and relational integrity. In-memory H2 (in PostgreSQL compatibility mode) provides zero-configuration local execution without file-locking bugs. |
| **FullCalendar.js** | Scheduling UI | Industry-standard calendar library capable of rendering week, day, and month availability timelines directly from REST JSON payloads. |
| **JJWT 0.12.x** | Token Cryptography | Implements standard JSON Web Token creation and verification using HMAC-SHA256 digital signatures. |

### 1.3 Modular Monolith Architecture

Rather than microservices (which require complex Docker networks, distributed transaction managers, and independent deployment pipelines), we use a **Modular Monolith**:
* **Single deployable Spring Boot application.**
* **Strict package boundaries:** Domain logic is organized into isolated packages (`auth`, `institution`, `equipment`, `booking`, `common`, `config`).
* **Clean data flow:** Every package adheres to the uniform `Controller â†’ Service â†’ Repository â†’ Entity` architectural pattern.

```mermaid
graph TD
    Client["React Frontend (Port 5173)"]

    subgraph Spring_Boot_Backend ["Spring Boot Backend (Port 8080)"]
        Filter["Spring Security Filter Chain (AuthTokenFilter)"]

        subgraph Controllers ["Web Controllers (REST APIs)"]
            AC["AuthController (/api/auth)"]
            IC["InstitutionController (/api/institutions)"]
            EC["EquipmentController (/api/equipment)"]
            BC["BookingController (/api/bookings)"]
        end

        subgraph Services ["Service Layer (Business Logic)"]
            AS["AuthService (BCrypt & JWT)"]
            ES["EquipmentService (Filtering & Validation)"]
            BS["BookingService (Collision Detection Engine)"]
        end

        subgraph Repositories ["Data Access (Spring Data JPA)"]
            UR["UserRepository"]
            IR["InstitutionRepository"]
            DR["DepartmentRepository"]
            ER["EquipmentRepository"]
            BR["BookingRepository"]
        end
    end

    subgraph Database ["Relational Database (PostgreSQL / H2)"]
        T_Users[("users")]
        T_Inst[("institutions & departments")]
        T_Equip[("equipment")]
        T_Book[("bookings")]
    end

    Client -->|HTTP + Bearer JWT| Filter
    Filter --> Controllers
    Controllers --> Services
    Services --> Repositories
    Repositories --> Database
```

---

## 2. Project Directory Structure

```
d:\Projects\SpringBoard_Internship\
â”œâ”€â”€ backend/                               # Spring Boot 3.4.x Backend
â”‚   â”œâ”€â”€ .mvn/wrapper/                      # Maven Wrapper configuration
â”‚   â”œâ”€â”€ src/main/java/com/lab/platform/    # Java Source Code
â”‚   â”‚   â”œâ”€â”€ PlatformApplication.java       # Spring Boot main entry point
â”‚   â”‚   â”œâ”€â”€ common/                        # Common response envelopes & exceptions
â”‚   â”‚   â”œâ”€â”€ config/                        # Spring Security, CORS, and DataInitializer
â”‚   â”‚   â”œâ”€â”€ auth/                          # User entity, JWT, RBAC, AuthService & Controller
â”‚   â”‚   â”œâ”€â”€ institution/                   # Institution & Department entities, APIs
â”‚   â”‚   â”œâ”€â”€ equipment/                     # Equipment entity, status enum, search APIs
â”‚   â”‚   â””â”€â”€ booking/                       # Booking entity, calendar events, collision logic
â”‚   â”œâ”€â”€ src/main/resources/
â”‚   â”‚   â”œâ”€â”€ application.yml                # Dynamic configuration via environment variables
â”‚   â”‚   â””â”€â”€ application-postgres.yml       # Production PostgreSQL profile
â”‚   â”œâ”€â”€ src/test/java/com/lab/platform/    # Automated unit tests (JUnit 5 & Mockito)
â”‚   â”œâ”€â”€ mvnw.cmd / mvnw                    # Maven wrapper executable scripts
â”‚   â””â”€â”€ pom.xml                            # Project Object Model (dependencies & plugins)
â”œâ”€â”€ frontend/                              # React 18 + Vite Frontend
â”‚   â”œâ”€â”€ src/
â”‚   â”‚   â”œâ”€â”€ main.jsx                       # React DOM entry point
â”‚   â”‚   â”œâ”€â”€ App.jsx                        # Router & route definitions
â”‚   â”‚   â”œâ”€â”€ index.css                      # Tailwind base styles & FullCalendar overrides
â”‚   â”‚   â”œâ”€â”€ api/client.js                  # Axios client with JWT request interceptor
â”‚   â”‚   â”œâ”€â”€ context/AuthContext.jsx        # Global reactive authentication state
â”‚   â”‚   â”œâ”€â”€ components/common/             # Navbar, ProtectedRoute, RoleGuard, StatusBadge
â”‚   â”‚   â””â”€â”€ pages/                         # Login, Register, Catalog, Detail, Bookings, Approvals
â”‚   â”œâ”€â”€ index.html                         # Single Page Application HTML root
â”‚   â”œâ”€â”€ package.json                       # Node dependencies and scripts
â”‚   â”œâ”€â”€ vite.config.js                     # Vite dev server configuration & /api proxy
â”‚   â””â”€â”€ tailwind.config.js                 # Tailwind CSS configuration
â”œâ”€â”€ run-backend.bat                        # Windows 1-click backend runner
â”œâ”€â”€ run-frontend.bat                       # Windows 1-click frontend runner
â””â”€â”€ package.json                           # Root workspace script definitions
```

### Beginner Reading Order: Which Files to Study First
1. **[`backend/src/main/java/com/lab/platform/booking/BookingService.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/booking/BookingService.java):** Study this first to understand how business logic and collision detection work in Java.
2. **[`backend/src/main/java/com/lab/platform/auth/AuthTokenFilter.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/auth/AuthTokenFilter.java):** Shows how HTTP requests are intercepted and verified using JWT tokens.
3. **[`frontend/src/api/client.js`](file:///d:/Projects/SpringBoard_Internship/frontend/src/api/client.js):** Shows how the frontend attaches security tokens to outgoing requests.
4. **[`frontend/src/context/AuthContext.jsx`](file:///d:/Projects/SpringBoard_Internship/frontend/src/context/AuthContext.jsx):** Shows how login state is shared across all React components.
5. **[`frontend/src/pages/EquipmentDetail.jsx`](file:///d:/Projects/SpringBoard_Internship/frontend/src/pages/EquipmentDetail.jsx):** Shows how FullCalendar interacts with backend APIs.

---

## 3. Frontend Architecture Explained from Code

### 3.1 Entry Point & Root Component
* **[`main.jsx`](file:///d:/Projects/SpringBoard_Internship/frontend/src/main.jsx):** Mounts the React component tree into the `<div id="root"></div>` defined in `index.html`. Wraps the app in `<React.StrictMode>`.
* **[`App.jsx`](file:///d:/Projects/SpringBoard_Internship/frontend/src/App.jsx):** Provides the routing backbone using React Router (`<BrowserRouter>`, `<Routes>`, `<Route>`).
  * Wraps everything inside `<AuthProvider>` so that any component can inspect the logged-in user.
  * Public routes: `/` (Catalog), `/equipment/:id` (Details), `/login`, `/register`, `/forgot-password`.
  * Authenticated routes: Wrapped in `<ProtectedRoute>` (e.g., `/profile`, `/my-bookings`).
  * Role-restricted routes: Wrapped in `<RoleGuard allowedRoles={[...]}>` (e.g., `/pending-approvals`).

### 3.2 State Management with Context API
Instead of installing complex Redux libraries, we manage authentication using React's native Context API in **[`AuthContext.jsx`](file:///d:/Projects/SpringBoard_Internship/frontend/src/context/AuthContext.jsx)**:
* **State variables:** `user`, `token`, `isAuthenticated`, `loading`.
* **Session Persistence:** When the page reloads, an `useEffect` hook checks `localStorage.getItem('token')`. If present, the user session is immediately restored without forcing them to re-login.
* **Token Management:** `handleAuthSuccess(authData)` saves the JWT to `localStorage` and updates the React state, causing the entire UI (e.g., Navbar) to re-render reactively.

### 3.3 Axios HTTP Client & Request Interceptors
In **[`client.js`](file:///d:/Projects/SpringBoard_Internship/frontend/src/api/client.js)**, we configure a pre-configured Axios instance:
```javascript
// Request interceptor: automatically attaches JWT token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```
This guarantees that whenever a component calls `api.get()` or `api.post()`, the user's JWT token is automatically transmitted in the `Authorization: Bearer <token>` header without repeating code in every screen.

### 3.4 FullCalendar Integration
In **[`EquipmentDetail.jsx`](file:///d:/Projects/SpringBoard_Internship/frontend/src/pages/EquipmentDetail.jsx)**, we render the interactive schedule grid using `@fullcalendar/react`:
* Passes plugins: `[dayGridPlugin, timeGridPlugin, interactionPlugin]`.
* Loads events directly from backend endpoint `/api/bookings/equipment/${id}/calendar`.
* Color codes events: Confirmed reservations render in Emerald (`#10b981`), while Pending Approvals render in Amber (`#f59e0b`).
* Interactivity: When a user clicks or drags across open time slots on the grid, FullCalendar's `select` callback triggers `handleDateSelect(selectInfo)`, automatically extracting start and end timestamps and launching the reservation modal.

---

## 4. Backend Architecture Explained from Code

### 4.1 Layered Controller-Service-Repository Pattern
The backend strictly adheres to separation of concerns:

```
[HTTP Request] â”€â”€> [Controller] â”€â”€> [Service Layer] â”€â”€> [Repository] â”€â”€> [Database]
                         â”‚                 â”‚                  â”‚
                    Validates DTO      Executes Core      JPA / SQL
                   & Path Variables   Business Rules      Execution
```

1. **Controllers (e.g., [`BookingController.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/booking/BookingController.java)):**
   * Annotated with `@RestController` and `@RequestMapping("/api/bookings")`.
   * Accepts `@Valid @RequestBody BookingRequest request` and `@AuthenticationPrincipal UserDetails userDetails`.
   * Never talks directly to the database; delegates immediately to `BookingService`.
2. **Services (e.g., [`BookingService.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/booking/BookingService.java)):**
   * Annotated with `@Service` and `@Transactional`.
   * Contains validation logic (e.g., ensuring start time is in the future, checking minimum 15-minute duration, checking equipment operational status).
   * Orchestrates multi-step operations (e.g., collision checking, saving records, logging audit events).
3. **Repositories (e.g., [`BookingRepository.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/booking/BookingRepository.java)):**
   * Extends `JpaRepository<Booking, Long>`.
   * Spring Data JPA automatically generates SQL implementations at runtime based on method names (e.g., `findByUserIdOrderByStartTimeDesc`).
4. **Entities (e.g., [`Booking.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/booking/Booking.java)):**
   * Annotated with `@Entity` and `@Table(name = "bookings")`.
   * Represents relational tables with `@Id`, `@GeneratedValue`, `@Column`, and `@ManyToOne` relationships.

### 4.2 Security Layer (Spring Security 6 & JWT)
Configured in **[`SecurityConfig.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/config/SecurityConfig.java)**:
* **Stateless Session Management:** `sessionCreationPolicy(SessionCreationPolicy.STATELESS)` instructs Spring not to create HTTP sessions (`HttpSession`) on the server.
* **Filter Chain:** Before Spring's standard `UsernamePasswordAuthenticationFilter`, our custom **[`AuthTokenFilter.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/auth/AuthTokenFilter.java)** inspects incoming requests:
  1. Checks if the `Authorization` header starts with `"Bearer "`.
  2. Uses [`JwtUtils.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/auth/JwtUtils.java) to verify the HMAC-SHA256 signature and check expiration.
  3. Extracts the user's email.
  4. Loads user authorities from [`UserDetailsServiceImpl.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/auth/UserDetailsServiceImpl.java).
  5. Populates `SecurityContextHolder.getContext().setAuthentication(auth)`.
* **Method-Level RBAC:** `@EnableMethodSecurity` activates annotations like `@PreAuthorize("hasAnyRole('ROLE_LAB_MANAGER', ...)")` on sensitive endpoints.

### 4.3 Uniform Error Handling
In **[`GlobalExceptionHandler.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/common/GlobalExceptionHandler.java)**, any uncaught exception thrown anywhere in the application is intercepted by `@RestControllerAdvice`:
* `MethodArgumentNotValidException` (Bean validation failure) $\rightarrow$ Returns HTTP `400 Bad Request` with field-by-field error mappings.
* `ResourceNotFoundException` $\rightarrow$ Returns HTTP `404 Not Found`.
* `ConflictException` $\rightarrow$ Returns HTTP `409 Conflict`.
* `AccessDeniedException` $\rightarrow$ Returns HTTP `403 Forbidden`.

Every error response adheres to the identical JSON structure defined in [`ApiResponse.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/common/ApiResponse.java):
```json
{
  "success": false,
  "message": "Scheduling conflict: The requested time slot overlaps with an existing booking...",
  "data": null,
  "timestamp": "2026-10-22T14:30:00"
}
```

---

## 5. End-to-End Execution Flow: Collision-Protected Booking

This section traces a real transaction from the moment a researcher clicks "Confirm Reservation" on the frontend to the database commit and response.

```mermaid
sequenceDiagram
    autonumber
    participant UI as EquipmentDetail.jsx
    participant Axios as client.js (Interceptor)
    participant Filter as AuthTokenFilter.java
    participant Ctrl as BookingController.java
    participant Svc as BookingService.java
    participant Repo as BookingRepository.java
    participant DB as SQL Database

    UI->>Axios: api.post('/bookings', bookingForm)
    Note over Axios: Injects header "Authorization: Bearer <token>"
    Axios->>Filter: HTTP POST /api/bookings
    Filter->>Filter: jwtUtils.validateToken(token) -> Valid!
    Filter->>Ctrl: createBooking(@Valid request, userDetails)
    Ctrl->>Svc: createBooking(request, "researcher@lab.edu")

    rect rgb(240, 248, 255)
        Note over Svc: 1. Validate start < end & duration >= 15 min
        Note over Svc: 2. Check equipment.status == AVAILABLE
        Note over Svc, Repo: 3. Collision Query Execution
        Svc->>Repo: countOverlappingBookings(equipId, start, end, activeStatuses)
        Repo->>DB: SELECT COUNT(b) FROM bookings WHERE ...
        DB-->>Repo: Returns count = 0 (No collision)
    end

    Svc->>Repo: save(new Booking(PENDING_APPROVAL))
    Repo->>DB: INSERT INTO bookings ...
    DB-->>Repo: Saved entity with generated ID 1
    Repo-->>Svc: Booking entity
    Svc-->>Ctrl: Booking entity
    Ctrl-->>Axios: 200 OK + ApiResponse.ok(booking)
    Axios-->>UI: Promise resolves with success
    UI->>UI: setBookingSuccess("Reservation requested!")
    UI->>UI: fetchCalendarEvents() -> Calendar re-renders event in Amber
```

### Code Trace Reference
1. **Frontend Trigger:** [`EquipmentDetail.jsx`](file:///d:/Projects/SpringBoard_Internship/frontend/src/pages/EquipmentDetail.jsx#L96-L125) (`handleBookingSubmit`)
2. **Backend Entry:** [`BookingController.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/booking/BookingController.java#L23-L28) (`createBooking`)
3. **Collision Algorithm:** [`BookingService.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/booking/BookingService.java#L69-L83)
   ```java
   List<BookingStatus> conflictingStatuses = Arrays.asList(
           BookingStatus.PENDING_APPROVAL,
           BookingStatus.CONFIRMED,
           BookingStatus.IN_USE
   );

   long overlappingCount = bookingRepository.countOverlappingBookings(
           equipment.getId(),
           request.getStartTime(),
           request.getEndTime(),
           conflictingStatuses,
           null
   );

   if (overlappingCount > 0) {
       throw new ConflictException("Scheduling conflict: The requested time slot overlaps...");
   }
   ```
4. **Interval Overlap Query:** [`BookingRepository.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/booking/BookingRepository.java#L26-L34)
   $$\text{Collision condition: } (Start_{\text{existing}} < End_{\text{requested}}) \land (End_{\text{existing}} > Start_{\text{requested}})$$
