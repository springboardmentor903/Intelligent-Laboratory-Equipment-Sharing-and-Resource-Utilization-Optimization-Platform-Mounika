# Lab Resource Utilization Platform

A Full-Stack Laboratory Equipment Sharing &amp; Resource Utilization Optimization Platform built for universities, research institutes, and multi-facility laboratories.

---

## Architecture Overview (Modular Monolith)

The platform is designed as a clean, maintainable **Modular Monolith** using Java 21+ with **Spring Boot 3.4.x** and **React 18** (Vite + Tailwind CSS).

```
Lab-Resource-Platform/
├── backend/
│   ├── src/main/java/com/lab/platform/
│   │   ├── common/           # Unified ApiResponse envelope, custom exceptions, GlobalExceptionHandler
│   │   ├── config/           # Spring Security 6, stateless JWT filter chain, CORS configuration
│   │   ├── auth/             # 6-Role RBAC system, JWT utils, User entity, AuthService & AuthController
│   │   ├── institution/      # Institution & Department entities, dynamic management APIs
│   │   ├── equipment/        # Equipment entity, specs, status lifecycle, filtering & search APIs
│   │   └── booking/          # Core reservation engine, interval collision detection, manager approvals
│   └── src/main/resources/
│       ├── application.yml   # Clean dynamic configuration via environment variables (PostgreSQL / in-memory H2)
│       └── application-postgres.yml # PostgreSQL production/Docker profile
├── frontend/
│   └── src/
│       ├── api/              # Axios client with automatic Bearer JWT injection and error interceptors
│       ├── context/          # AuthContext for reactive global authentication and user state
│       ├── components/       # Reusable UI widgets: Navbar, StatusBadge, ProtectedRoute, RoleGuard
│       └── pages/            # Login, Register, Catalog, Detail, Calendar, MyBookings, Approvals
├── run-backend.bat           # 1-Click Windows batch script to launch Backend
├── run-frontend.bat          # 1-Click Windows batch script to launch Frontend
└── package.json              # Root package with unified workspace scripts
```

---

## Zero Hardcoded Data & Dynamic Setup

This platform does **not hardcode mock data, demo users, or credentials**:
1. **Clean Initial State:** Starts with an empty database schema ready to accept your real laboratory data.
2. **First-Run Administrator Setup:** The very first user who creates an account through the Registration page is automatically designated as **System Administrator** (`ROLE_SYSTEM_ADMIN`).
3. **Subsequent Accounts:** Subsequent registrations are created as **Researchers / Students** (`ROLE_RESEARCHER`), preventing privilege escalation.
4. **Dynamic Facilities:** Administrators and staff can create Institutions and Departments directly in the UI using inline "+ Add New" buttons on registration and equipment screens.

---

## How to Run Locally

### Option A: 1-Click Launchers (Windows)
Double-click or run from PowerShell:
1. Run **`.\run-backend.bat`** (Starts Spring Boot on `http://localhost:8080`)
2. Run **`.\run-frontend.bat`** (Starts React Vite dev server on `http://localhost:5173`)

### Option B: Terminal Commands
#### Terminal 1: Backend
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

#### Terminal 2: Frontend
```powershell
cd frontend
npm install
npm run dev
```

Open your browser at: **`http://localhost:5173`**

---

## Database Configuration

The application supports **PostgreSQL** natively via environment variables, with a zero-configuration in-memory mode for rapid local development:

```bash
# Optional: To connect to your local PostgreSQL instance
set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/lab_resource_db
set SPRING_DATASOURCE_USERNAME=postgres
set SPRING_DATASOURCE_PASSWORD=your_password
```

---

## Running Automated Tests

To run the automated backend test suite:
```powershell
cd backend
.\mvnw.cmd test
```
All 9 unit tests verify:
1. First user dynamic promotion to `ROLE_SYSTEM_ADMIN`.
2. Public registration role restriction to `ROLE_RESEARCHER`.
3. Email uniqueness validation.
4. Password hashing with BCrypt.
5. Booking interval collision detection (blocking overlapping reservations with HTTP 409 Conflict).
6. Operational maintenance locks on equipment.
7. Manager approval workflows.
