# Milestone 1: Mentor Interview Questions & Practical Exercises

This document prepares you for technical questioning from your mentor and provides hands-on exercises to test your code literacy.

---

## Part 1: Mentor Technical Q&A

### Category 1: Architecture & Technology Stack

#### Q1: "Why did you choose a Modular Monolith instead of Microservices?"
* **Model Answer:**
  *"For our laboratory sharing platform, microservices would introduce distributed network latency, data synchronization challenges across services, and complicated container orchestrations before our core domain logic was even established.
  By choosing a modular monolith, we organize the codebase into strict domain packages (`auth`, `equipment`, `booking`, `institution`). Each domain maintains its own models, repositories, and services. This gives us clear separation of concerns, single-command development, and ACID transaction guarantees, while keeping the architecture modular enough to extract into microservices later if scaling demands it."*

#### Q2: "How does the React frontend communicate with the Spring Boot backend?"
* **Model Answer:**
  *"Communication is handled completely over HTTP using RESTful JSON APIs. In Vite, we configure a development proxy forwarding `/api` requests to `http://localhost:8080`.
  On the frontend, we use an Axios client with a request interceptor (`client.js`) that automatically checks `localStorage` for a JWT token and attaches it to the `Authorization: Bearer <token>` header on every request. On the backend, Spring Security's `AuthTokenFilter` intercepts every request, validates the cryptographic signature of the token, and establishes the authenticated security context before handing the request to our RestControllers."*

#### Q3: "Why did you choose JWT over traditional server-side session cookies?"
* **Model Answer:**
  *"Traditional server sessions require the backend to keep session state in memory or in a shared Redis cache. If you have thousands of active researchers, memory overhead grows, and horizontal server scaling requires sticky sessions.
  JWTs are completely stateless. The user's identity and assigned roles are encoded and digitally signed in the token itself. The Spring Boot backend only needs to verify the HMAC-SHA256 signature using its secret key. This makes the backend lighter, faster, and naturally scalable."*

---

### Category 2: Data Integrity & Business Logic

#### Q4: "How does your system prevent two researchers from booking the same instrument at the same time?"
* **Model Answer:**
  *"We enforce collision detection directly in the business logic and database query layer. When a reservation request arrives, `BookingService` queries `BookingRepository` using an interval overlap formula:
  $$(Start_{\text{existing}} < End_{\text{requested}}) \land (End_{\text{existing}} > Start_{\text{requested}})$$
  We filter for active reservations with statuses `PENDING_APPROVAL`, `CONFIRMED`, and `IN_USE`. If the count of overlapping rows is greater than zero, the service immediately halts execution and throws a `ConflictException`, which our `GlobalExceptionHandler` converts into an HTTP `409 Conflict` response with an explanatory message for the user."*

#### Q5: "How do you ensure a public user cannot register themselves as a Lab Manager or Administrator?"
* **Model Answer:**
  *"We enforce role security strictly on the backend in `AuthService.register()`, completely ignoring any privileged role passed in the HTTP request payload.
  When a user registers, the service checks `userRepository.count()`. If the database is completely empty (first run), it grants `ROLE_SYSTEM_ADMIN` so the administrator can configure the platform. For all subsequent public registrations, the role is hardcoded to `ROLE_RESEARCHER`. Administrative roles can only be granted by existing administrators through internal management endpoints."*

---

### Category 3: Error Handling & Debugging

#### Q6: "If a user reports that clicking 'Confirm Reservation' does nothing or shows an error, how would you debug it?"
* **Model Answer:**
  *"I would follow a structured 4-step debugging approach:
  1. **Browser DevTools (Network Tab):** Inspect the outgoing `POST /api/bookings` request. Check if the request was sent, verify the payload fields (`equipmentId`, `startTime`, `endTime`), and check the HTTP response status code (e.g., 400, 401, 403, 409, 500).
  2. **Browser Console:** Look for JavaScript exceptions, Axios unhandled promise rejections, or form validation warnings.
  3. **Backend Terminal Logs:** If the request reached the backend, check the Spring Boot console output. Look for Spring Security access denied logs or stack traces printed by `GlobalExceptionHandler`.
  4. **Database State:** If it's a 409 Conflict, inspect the `bookings` table via `/h2-console` to confirm whether another reservation genuinely occupies that time window."*

#### Q7: "What did you personally learn while building Milestone 1?"
* **Model Answer:**
  *"As a B.Tech CSE student knowing core Java, this project helped me transition from isolated syntax exercises to real-world software architecture. I learned:
  1. How Spring Security's filter chain operates and why stateless authentication is powerful.
  2. How to model relational databases cleanly with JPA entities and foreign keys.
  3. How interval mathematics applies to scheduling algorithms to guarantee zero collisions.
  4. How React's reactive state and Context API manage global user sessions without external complexity."*

---

## Part 2: Practical Code Literacy Exercises

Try these 4 exercises yourself using the project repository to verify your code understanding.

---

### Exercise 1: Trace the Endpoint Route
* **Task:** Find the exact Java class and method that handles the request when a Lab Manager clicks "Approve" on a reservation in the Approvals Queue.
* **Hint:** Look inside `backend/src/main/java/com/lab/platform/booking/` for REST controller mapping annotations.
* *[Check your answer in the solutions section below]*

---

### Exercise 2: Find the Validation Rule
* **Task:** Where in the code is it enforced that a booking must have a minimum duration of at least 15 minutes and cannot exceed 48 hours?
* **Hint:** Search in `BookingService.java` inside the `createBooking` method.
* *[Check your answer in the solutions section below]*

---

### Exercise 3: Explain the Security Filter
* **Task:** Open [`AuthTokenFilter.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/auth/AuthTokenFilter.java). Explain what happens if a request contains an invalid, expired, or tampered JWT token in the `Authorization` header.
* **Hint:** Look at lines 34 to 48 and check what is passed to `filterChain.doFilter(request, response)`.
* *[Check your answer in the solutions section below]*

---

### Exercise 4: Safe Error Simulation
* **Task:** Without changing any Java code, explain how you can deliberately trigger an HTTP 400 Bad Request error on the frontend registration screen.
* **Hint:** Inspect the validation constraints on [`RegisterRequest.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/auth/RegisterRequest.java).
* *[Check your answer in the solutions section below]*

---

## Part 3: Exercise Solutions (Review After Trying)

### Solution to Exercise 1
* **File:** [`BookingController.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/booking/BookingController.java#L47-L56)
* **Method:** `processApproval(@PathVariable Long id, @RequestBody BookingApprovalRequest request, ...)`
* **Route:** `PUT /api/bookings/{id}/approval`
* **Security:** Guarded by `@PreAuthorize("hasAnyRole('ROLE_LAB_MANAGER', 'ROLE_DEPARTMENT_HEAD', 'ROLE_INSTITUTION_ADMIN', 'ROLE_SYSTEM_ADMIN')")`.

### Solution to Exercise 2
* **File:** [`BookingService.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/booking/BookingService.java#L47-L54)
* **Code:**
  ```java
  Duration duration = Duration.between(request.getStartTime(), request.getEndTime());
  if (duration.toMinutes() < 15) {
      throw new BadRequestException("Minimum booking duration is 15 minutes.");
  }
  if (duration.toHours() > 48) {
      throw new BadRequestException("Maximum single booking duration cannot exceed 48 hours.");
  }
  ```

### Solution to Exercise 3
* In [`AuthTokenFilter.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/auth/AuthTokenFilter.java#L35):
  * `jwtUtils.validateToken(jwt)` catches `MalformedJwtException` or `ExpiredJwtException` and returns `false`.
  * The `if` block is skipped; `SecurityContextHolder` remains empty (unauthenticated).
  * `filterChain.doFilter(request, response)` forwards the request.
  * Spring Security's `filterChain` checks the route permissions in `SecurityConfig.java`. Because the route requires `.authenticated()`, Spring Security halts the request and immediately returns `HTTP 401 Unauthorized`.

### Solution to Exercise 4
* On the registration form, enter a password that has fewer than 6 characters (e.g., `12345`).
* [`RegisterRequest.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/auth/RegisterRequest.java#L19) specifies `@Size(min = 6, max = 50, message = "Password must be at least 6 characters")`.
* The backend will reject the request with `HTTP 400 Bad Request` and `GlobalExceptionHandler` will return `"Password must be at least 6 characters"`.
