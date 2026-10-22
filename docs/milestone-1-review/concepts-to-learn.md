# Milestone 1: Technical Concepts & Learning Roadmap

As a B.Tech CSE student learning full-stack development, this guide gives you the precise conceptual foundation needed to explain, defend, and extend the **Lab Resource Utilization Platform**.

Every concept is explained in simple everyday language first, followed by its formal technical definition, where it appears in your project, a real code example, and what you should say to your mentor.

---

## 1. General Software Engineering & Architecture

### 1.1 Modular Monolith
* **Simple Explanation:** Imagine a large house where every room has its own door, key, and specific purpose (kitchen, bedroom, workshop). Everything is under one roof, but the rooms are neatly separated.
* **Technical Definition:** A software architecture pattern where all application modules reside in a single deployable artifact and codebase, but maintain strict domain boundaries and encapsulation between modules.
* **Where It Appears:** [`backend/src/main/java/com/lab/platform/`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/) (separated into `auth`, `institution`, `equipment`, `booking`).
* **Why It Is Needed:** Avoids the operational overhead, network latency, and deployment complexity of microservices while preventing spaghetti code.
* **Code Example:** [`BookingService.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/booking/BookingService.java) does not directly manipulate user passwords; it interacts with `UserRepository` only to query public profile details.
* **What to Tell Mentor:** *"We selected a modular monolith architecture because it gives us strict domain separation while allowing single-command startup and unified database transactions."*
* **Priority:** **Must Learn Now**

### 1.2 Separation of Concerns (SoC) & MVC Pattern
* **Simple Explanation:** The person taking your order at a restaurant (waiter) is not the one cooking the meal (chef), and the chef is not the one growing the vegetables (farmer).
* **Technical Definition:** Dividing a program into distinct sections, where each section addresses a separate concern: Presentation (Controller/UI), Business Logic (Service), and Data Persistence (Repository).
* **Where It Appears:** `EquipmentController` $\rightarrow$ `EquipmentService` $\rightarrow$ `EquipmentRepository`.
* **Why It Is Needed:** If business rules change, you only edit the Service layer without breaking the HTTP endpoints or database schema.
* **What to Tell Mentor:** *"Controllers only validate inputs and format responses; all domain business logic is strictly encapsulated in Service classes."*
* **Priority:** **Must Learn Now**

---

## 2. Frontend Fundamentals (React & SPA)

### 2.1 Single Page Application (SPA)
* **Simple Explanation:** When you click a link on the site, the browser does not flash white or reload the whole HTML page from scratch. Instead, JavaScript dynamically swaps out the screen content instantly.
* **Technical Definition:** A web application that loads a single HTML document and dynamically updates the DOM as the user interacts with the app, using client-side routing.
* **Where It Appears:** [`frontend/index.html`](file:///d:/Projects/SpringBoard_Internship/frontend/index.html) has only one `<div id="root"></div>`; [`App.jsx`](file:///d:/Projects/SpringBoard_Internship/frontend/src/App.jsx) swaps pages via `react-router-dom`.
* **Why It Is Needed:** Delivers desktop-app responsiveness and seamless transitions.
* **What to Tell Mentor:** *"Our React frontend is an SPA; React Router handles navigation locally without triggering browser document reloads."*
* **Priority:** **Must Learn Now**

### 2.2 Component Hierarchy & State (useState)
* **Simple Explanation:** Building a web page using Lego bricks. Each brick (component) can remember its own internal information (state), such as whether a dialog box is open or closed.
* **Technical Definition:** Self-contained UI units that accept inputs (`props`) and maintain internal reactive data (`state`) that triggers DOM re-renders upon mutation.
* **Where It Appears:** [`EquipmentCatalog.jsx`](file:///d:/Projects/SpringBoard_Internship/frontend/src/pages/EquipmentCatalog.jsx) uses `const [showAddModal, setShowAddModal] = useState(false);`.
* **Why It Is Needed:** Keeps UI in sync with underlying data automatically.
* **Code Example:**
  ```javascript
  const [keyword, setKeyword] = useState('');
  // Whenever user types in the search input:
  onChange={(e) => setKeyword(e.target.value)}
  ```
* **What to Tell Mentor:** *"We use React hooks like `useState` for component-level reactivity and `useEffect` for fetching equipment when filters change."*
* **Priority:** **Must Learn Now**

### 2.3 React Context API (Global State)
* **Simple Explanation:** A loudspeaker in a school. Instead of whispering a message from student to student across 10 classrooms, the principal makes an announcement on the PA system, and everyone hears it.
* **Technical Definition:** A React feature that allows state to be shared across the entire component tree without explicitly passing props through intermediate components (drilling).
* **Where It Appears:** [`frontend/src/context/AuthContext.jsx`](file:///d:/Projects/SpringBoard_Internship/frontend/src/context/AuthContext.jsx).
* **Why It Is Needed:** Any component (Navbar, Booking modal, Profile) needs to know if a user is logged in without passing `user` through 5 layers of components.
* **Code Example:**
  ```javascript
  export const useAuth = () => useContext(AuthContext);
  // Inside Navbar.jsx:
  const { user, isAuthenticated, logout } = useAuth();
  ```
* **What to Tell Mentor:** *"AuthContext provides reactive, application-wide access to the current authenticated user and JWT token."*
* **Priority:** **Must Learn Now**

---

## 3. JavaScript Concepts Actually Used

### 3.1 Promises and async / await
* **Simple Explanation:** Ordering food at a counter and receiving a buzzer. You can sit down and drink water; when the buzzer goes off (promise resolves), your food is ready.
* **Technical Definition:** An abstraction for handling asynchronous non-blocking operations, where `async/await` provides synchronous-looking syntax over JavaScript Promises.
* **Where It Appears:** [`frontend/src/pages/Login.jsx`](file:///d:/Projects/SpringBoard_Internship/frontend/src/pages/Login.jsx#L19-L32).
* **Code Example:**
  ```javascript
  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await login(email, password); // Waits for network response without freezing UI
      navigate('/');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };
  ```
* **Priority:** **Must Learn Now**

### 3.2 Array Transformation (`map`, `filter`, `find`)
* **Simple Explanation:** Taking a stack of index cards, throwing out the ones that don't match your criteria (`filter`), and transforming the rest into neat summary cards (`map`).
* **Technical Definition:** Functional higher-order methods that iterate over collections and produce new arrays without mutating the source array.
* **Where It Appears:** Rendering equipment cards in [`EquipmentCatalog.jsx`](file:///d:/Projects/SpringBoard_Internship/frontend/src/pages/EquipmentCatalog.jsx#L294).
* **Code Example:**
  ```javascript
  {equipmentList.map((item) => (
    <div key={item.id} className="...">
      <h3>{item.name}</h3>
    </div>
  ))}
  ```
* **Priority:** **Must Learn Now**

---

## 4. Backend & Spring Boot Fundamentals

### 4.1 Inversion of Control (IoC) & Dependency Injection (DI)
* **Simple Explanation:** Instead of building your own engine, tires, and seats when you want a car, a factory delivers a fully assembled car to your door.
* **Technical Definition:** An architectural pattern where the framework container creates, manages, and injects object dependencies at runtime, rather than classes instantiating them manually via `new`.
* **Where It Appears:** Constructor injection in every Spring service and controller.
* **Code Example in [`BookingController.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/booking/BookingController.java#L19-L21):**
  ```java
  @RestController
  public class BookingController {
      private final BookingService bookingService;

      // Spring automatically injects the BookingService bean here:
      public BookingController(BookingService bookingService) {
          this.bookingService = bookingService;
      }
  }
  ```
* **What to Tell Mentor:** *"We use constructor-based dependency injection because it makes components loosely coupled, testable with Mockito, and enforces immutability with final fields."*
* **Priority:** **Must Learn Now**

### 4.2 Spring Boot Annotations
* **`@RestController`:** Combines `@Controller` and `@ResponseBody`, instructing Spring to serialize method return values directly into JSON.
* **`@Service`:** Marks a class holding business logic as a managed Spring bean.
* **`@Repository`:** Marks a data-access class, enabling automatic exception translation for database errors.
* **`@Entity`:** Identifies a Java class as a persistent relational database table handled by JPA/Hibernate.
* **`@Transactional`:** Wraps a method in an ACID database transaction. If an exception occurs, all database modifications are automatically rolled back.
* **Priority:** **Must Learn Now**

---

## 5. HTTP, REST APIs & JSON

### 5.1 HTTP Methods & Status Codes
* **`GET`:** Safe, idempotent retrieval (e.g., `GET /api/equipment`).
* **`POST`:** Creates a new resource (e.g., `POST /api/bookings`).
* **`PUT`:** Full update / state change (e.g., `PUT /api/bookings/1/approval`).
* **Status Codes Used in Project:**
  * **`200 OK`:** Successful request execution.
  * **`400 Bad Request`:** Invalid inputs or validation constraints failed.
  * **`401 Unauthorized`:** Missing or expired JWT token.
  * **`403 Forbidden`:** Authenticated user lacks the necessary Role (RBAC block).
  * **`404 Not Found`:** Requested entity ID does not exist.
  * **`409 Conflict`:** Business collision (e.g., overlapping reservation slot).
* **Priority:** **Must Learn Now**

---

## 6. Database Concepts (JPA & Relational Modeling)

### 6.1 Object-Relational Mapping (ORM) & Hibernate
* **Simple Explanation:** A translator between two different languages. In Java, we think in Objects (`User`, `Equipment`); in PostgreSQL, the database thinks in Rows and Columns. ORM bridges them automatically.
* **Technical Definition:** A software technique that automatically converts data between incompatible type systems in relational databases and object-oriented programming languages.
* **Where It Appears:** In [`Equipment.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/equipment/Equipment.java), Java fields like `private BigDecimal hourlyRate;` map directly to the `hourly_rate NUMERIC(10,2)` column.
* **Priority:** **Must Learn Now**

### 6.2 Relational Foreign Keys (`@ManyToOne`)
* **Simple Explanation:** A tag on every piece of equipment that points to which department owns it.
* **Technical Definition:** A field in one table that uniquely identifies a row in another table, enforcing referential integrity.
* **Where It Appears:** [`Booking.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/booking/Booking.java#L16-L23) has `@ManyToOne private Equipment equipment;` and `@ManyToOne private User user;`.
* **Priority:** **Must Learn Now**

---

## 7. Authentication & Security

### 7.1 Stateless JSON Web Tokens (JWT)
* **Simple Explanation:** A digital hand stamp at an amusement park. When you pay at the gate, they stamp your hand with special UV ink. When you get in line for rollercoasters, ride operators don't check a master book; they just shine a UV light on your hand stamp.
* **Technical Definition:** A compact, URL-safe means of representing claims between two parties. Composed of three Base64URL parts: `Header.Payload.Signature`.
* **Where It Appears:** [`JwtUtils.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/auth/JwtUtils.java).
* **Why It Is Needed:** The server doesn't keep sessions in RAM. If 10,000 students log in, memory usage does not explode.
* **Code Example:**
  ```java
  Jwts.builder()
      .subject(user.getEmail())
      .claim("role", user.getRole().name())
      .issuedAt(now)
      .expiration(expiryDate)
      .signWith(key) // Cryptographic signature with secret key
      .compact();
  ```
* **Priority:** **Must Learn Now**

### 7.2 Password Hashing with BCrypt
* **Simple Explanation:** A one-way blender. You can turn strawberries into a smoothie, but you can never turn the smoothie back into a strawberry. When a user logs in, we blend their entered password and compare smoothies.
* **Technical Definition:** An adaptive one-way cryptographic hash function incorporating salt to protect against rainbow table attacks.
* **Where It Appears:** [`AuthService.java`](file:///d:/Projects/SpringBoard_Internship/backend/src/main/java/com/lab/platform/auth/AuthService.java#L63) uses `passwordEncoder.encode(request.getPassword())`.
* **Priority:** **Must Learn Now**

---

## 8. Concepts Reserved for Later Milestones (Do Not Confuse with M1)

The following concepts appear in the project roadmap but are **explicitly not part of Milestone 1**:
* **WebSockets & STOMP Protocol:** (Milestone 2) For live telemetry and real-time equipment state badges.
* **RabbitMQ Message Queues:** (Milestone 3) For asynchronous notification workers and heavy report rendering.
* **Docker Multi-Stage Builds & Compose:** (Milestone 4) For containerized production orchestration.
* **Elasticsearch Indexing:** (Milestone 4) For full-text distributed search.
