# Developer Guide — How to Build an API Endpoint from Scratch

This guide walks through building a new REST endpoint in any service of this project, following the exact patterns already used in `user-service`.

---

## Folder Structure

Each service follows this layered package structure:

```
src/main/java/com/gastracker/{service}/
│
├── controller/          HTTP layer — maps routes to service calls
│
├── dao/
│   ├── entity/          JPA entities — one class = one database table
│   ├── repository/      Spring Data JPA interfaces — database queries
│   └── specification/   Complex dynamic query filters (JPA Specifications)
│
├── dto/
│   ├── request/         What the client sends in the request body
│   ├── response/        What we send back — never expose entity directly
│   └── params/          Query/filter parameters (used with Specifications)
│
├── enums/               Enumerations shared across the service
│
├── exception/           Custom exception classes + global handler
│
├── service/             Business logic
│   ├── helper/          Utilities used by services (e.g. JwtHelper)
│   └── transformer/     Converts entity ↔ DTO
│
└── config/              Spring configuration (Security, Filters, Beans)
```

---

## Step-by-Step: Adding a New Endpoint

We will use a fictional example: **adding a `GET /api/v1/users/nic/{nic}` endpoint** that looks up a user by NIC number. Follow the same order every time.

---

### Step 1 — Enum (if needed)

If your feature introduces a new fixed set of values, add it to `enums/`.

```java
// enums/Role.java — already exists, no change needed for this example
public enum Role {
    CITIZEN, DEALER, ADMIN
}
```

Only create a new enum file when you introduce a new type, e.g. `GasType`, `TokenStatus`.

---

### Step 2 — Entity (`dao/entity/`)

The entity maps directly to a database table. Use JPA annotations.

```java
// dao/entity/User.java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String nic;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

Key rules:

- `@Data` — generates getters, setters, equals, hashCode, toString
- `@Builder` + `@NoArgsConstructor` + `@AllArgsConstructor` — needed together for JPA + builder pattern
- Never expose `password` in a response DTO
- Use `@PrePersist` to auto-set timestamps

---

### Step 3 — Repository (`dao/repository/`)

Extend `JpaRepository` and add method signatures for any custom queries you need. Spring Data JPA generates the SQL automatically from the method name.

```java
// dao/repository/UserRepository.java
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<User> findByNic(String nic);       // generated: SELECT * FROM users WHERE nic = ?
    boolean existsByNic(String nic);
}
```

Common method name patterns:

| Method signature                                             | SQL equivalent                      |
| ------------------------------------------------------------ | ----------------------------------- |
| `findById(String id)`                                        | `WHERE id = ?`                      |
| `findByEmail(String email)`                                  | `WHERE email = ?`                   |
| `existsByNic(String nic)`                                    | `SELECT COUNT(*) > 0 WHERE nic = ?` |
| `findByRoleAndCreatedAtAfter(Role role, LocalDateTime date)` | `WHERE role = ? AND created_at > ?` |

---

### Step 4 — Response DTO (`dto/response/`)

Never return the entity directly from a controller. Create a DTO that contains only what the client should see.

```java
// dto/response/UserResponse.java
@Data
@Builder
public class UserResponse {
    private String id;
    private String nic;
    private String email;
    private String name;
    private Role role;
    private LocalDateTime createdAt;
    // NOTE: no password field
}
```

---

### Step 5 — Request DTO (`dto/request/`) — only for POST/PUT

For endpoints that receive a body, create a request DTO with validation annotations.

```java
// dto/request/RegisterRequest.java
@Data
public class RegisterRequest {

    @NotBlank
    @Pattern(
        regexp = "^\\d{9}[VvXx]$|^\\d{12}$",
        message = "Invalid NIC format"
    )
    private String nic;

    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    @NotNull
    private Role role;
}
```

Common validation annotations:

| Annotation             | What it checks                   |
| ---------------------- | -------------------------------- |
| `@NotBlank`            | String is not null and not empty |
| `@NotNull`             | Value is not null                |
| `@Email`               | Valid email format               |
| `@Pattern(regexp=...)` | Matches regex                    |
| `@Min(n)` / `@Max(n)`  | Number range                     |
| `@Size(min=n, max=n)`  | String/collection length         |

---

### Step 6 — Transformer (`service/transformer/`)

The transformer converts an entity to a response DTO. Keep this logic here, not in the service or controller.

```java
// service/transformer/UserTransformer.java
@Component
public class UserTransformer {

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .nic(user.getNic())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
```

---

### Step 7 — Service (`service/`)

The service holds all business logic. It calls the repository, applies rules, and uses the transformer to build the response.

```java
// service/UserService.java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserTransformer userTransformer;

    // New method for our example endpoint
    public UserResponse getUserByNic(String nic) {
        User user = userRepository.findByNic(nic.toUpperCase())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userTransformer.toResponse(user);
    }
}
```

Rules:

- Inject dependencies via `@RequiredArgsConstructor` (constructor injection, no `@Autowired`)
- Always throw a typed custom exception — never `RuntimeException` directly
- Normalize inputs here (e.g. `.toUpperCase()` for NIC)

---

### Step 8 — Exception Handling (`exception/`)

Never throw `RuntimeException` directly. Create a typed exception per failure case and one global handler that converts them to proper HTTP responses.

**Custom exceptions** — one file per failure type:

```java
// exception/ResourceNotFoundException.java  → 404
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}

// exception/DuplicateResourceException.java  → 409
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) { super(message); }
}

// exception/InvalidCredentialsException.java  → 401
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() { super("Invalid credentials"); }
}
```

**Error response DTO** — put in `dto/response/`:

```java
// dto/response/ErrorResponse.java
@Data
@Builder
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;
}
```

**Global exception handler** — put in `config/`:

```java
// config/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .status(status.value())
                        .error(status.getReasonPhrase())
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
```

**Use them in the service:**

```java
// Instead of: throw new RuntimeException("User not found");
throw new ResourceNotFoundException("User not found");

// Instead of: throw new RuntimeException("Email already in use");
throw new DuplicateResourceException("Email already in use");

// For login failure:
throw new InvalidCredentialsException();
```

Error responses the client will receive:

```json
// 404
{ "status": 404, "error": "Not Found", "message": "User not found", "timestamp": "..." }

// 409
{ "status": 409, "error": "Conflict", "message": "An account already exists for this NIC", "timestamp": "..." }

// 401
{ "status": 401, "error": "Unauthorized", "message": "Invalid credentials", "timestamp": "..." }

// 400 — validation failure
{ "status": 400, "error": "Bad Request", "message": "nic: Invalid NIC format, email: must not be blank", "timestamp": "..." }
```

---

### Step 9 — Controller (`controller/`)

The controller only maps HTTP → service call → HTTP response. No business logic, no try/catch — exceptions bubble up to `GlobalExceptionHandler` automatically.

```java
// controller/UserController.java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Existing endpoints ...

    @GetMapping("/nic/{nic}")
    public ResponseEntity<UserResponse> getUserByNic(@PathVariable String nic) {
        return ResponseEntity.ok(userService.getUserByNic(nic));
    }
}
```

HTTP status codes to use:

| Scenario         | Status | Method                                                 |
| ---------------- | ------ | ------------------------------------------------------ |
| Successful read  | 200    | `ResponseEntity.ok(body)`                              |
| Created resource | 201    | `ResponseEntity.status(HttpStatus.CREATED).body(body)` |
| No content       | 204    | `ResponseEntity.noContent().build()`                   |
| Not found        | 404    | throw exception (handled globally)                     |

---

### Step 9 — Security (`config/SecurityConfig.java`)

Decide if the new endpoint is public or requires authentication.

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(
        "/api/v1/users/register",
        "/api/v1/users/login",
        "/api/v1/users/test",
        "/actuator/health"
    ).permitAll()                   // ← public endpoints listed here
    .anyRequest().authenticated()   // ← everything else requires a valid JWT
)
```

To make a new endpoint public, add its path to the `requestMatchers(...)` list.
To restrict to a specific role:

```java
.requestMatchers("/api/v1/users/admin/**").hasRole("ADMIN")
```

---

## Adding a New Service (checklist)

When you add a completely new microservice (e.g. `queue-service`):

- [ ] Create Spring Boot project with the same dependencies
- [ ] Copy the folder structure: `controller`, `dao`, `dto`, `enums`, `service`, `config`
- [ ] Add `application.yaml` with correct port, datasource, and Eureka config
- [ ] Add a `TestController` with `GET /api/v1/{resource}/test`
- [ ] Add a `Dockerfile` matching the other services
- [ ] Add the service to `docker-compose.yml`
- [ ] Add a route in `api-gateway/src/main/resources/application.yaml` under `spring.cloud.gateway.server.webmvc.routes`

---
