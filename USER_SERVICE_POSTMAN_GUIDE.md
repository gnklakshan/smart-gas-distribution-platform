# User Service - Postman Testing Guide

## Service Configuration

- **Base URL:** `http://localhost:8081`
- **Service Name:** user-service
- **Port:** 8081
- **Database:** PostgreSQL at `localhost:5432/userdb`
- **API Version:** v1
- **JWT Expiration:** 24 hours

---

## Overview

The User Service manages authentication and user management for the Smart Gas Distribution Platform. It provides endpoints for:
- User registration (Citizens and Dealers)
- User login with JWT token generation
- User profile management
- Role-based access (CITIZEN, DEALER, ADMIN)

---

## Authentication

Most endpoints require a **JWT token** obtained from the login endpoint.

**Authorization Header Format:**
```
Authorization: Bearer <JWT_TOKEN>
```

**JWT Token Details:**
- Algorithm: HMAC-SHA256 (HS256)
- Expiration: 24 hours from creation
- Contains: User UUID, Email, and Role

---

## Public Endpoints (No Authentication Required)

### 1. Health Check / Test Endpoint

**GET** `/api/v1/users/test`

- **Description:** Check if the user service is running
- **Authentication:** No
- **Request Body:** None

**Sample Response:**
```json
{
  "status": "ok",
  "service": "user-service",
  "message": "User Service is running"
}
```

---

### 2. Register as Citizen

**POST** `/api/v1/users/register`

- **Description:** Self-register as a citizen/consumer
- **Authentication:** No
- **Request Headers:**
  ```
  Content-Type: application/json
  ```

**Sample Request Body:**
```json
{
  "nic": "123456789V",
  "email": "citizen@example.com",
  "password": "SecurePassword@123",
  "name": "John Citizen"
}
```

**Sample Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJmNDdhYzEwYi01OGNjLTQzNzItYTU2Ny0wZTAyYjJjM2Q0NzkiLCJlbWFpbCI6ImNpdGl6ZW5AZXhhbXBsZS5jb20iLCJyb2xlIjoiQ0lUSVpFTiIsImlhdCI6MTcxNTQyODAwMCwiZXhwIjoxNzE1NTE0NDAwfQ.xyz123...",
  "user": {
    "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "nic": "123456789V",
    "email": "citizen@example.com",
    "name": "John Citizen",
    "role": "CITIZEN",
    "phone": null,
    "address": null,
    "businessName": null,
    "businessRegNo": null,
    "createdAt": "2026-05-11T09:30:00"
  }
}
```

---

## Authenticated Endpoints

### 3. User Login

**POST** `/api/v1/users/login`

- **Description:** Authenticate user with NIC and password, receive JWT token
- **Authentication:** No
- **Request Headers:**
  ```
  Content-Type: application/json
  ```

**Sample Request Body:**
```json
{
  "nic": "123456789V",
  "password": "SecurePassword@123"
}
```

**Sample Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJmNDdhYzEwYi01OGNjLTQzNzItYTU2Ny0wZTAyYjJjM2Q0NzkiLCJlbWFpbCI6ImNpdGl6ZW5AZXhhbXBsZS5jb20iLCJyb2xlIjoiQ0lUSVpFTiIsImlhdCI6MTcxNTQyODAwMCwiZXhwIjoxNzE1NTE0NDAwfQ.xyz123...",
  "user": {
    "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "nic": "123456789V",
    "email": "citizen@example.com",
    "name": "John Citizen",
    "role": "CITIZEN",
    "phone": null,
    "address": null,
    "businessName": null,
    "businessRegNo": null,
    "createdAt": "2026-05-11T09:30:00"
  }
}
```

---

### 4. Register Dealer (ADMIN Only)

**POST** `/api/v1/users/register/dealer`

- **Description:** Register a new dealer (Gas Station/Distribution center)
- **Authentication:** Required (ADMIN role only)
- **Authorization:** ADMIN only
- **Request Headers:**
  ```
  Authorization: Bearer <ADMIN_JWT_TOKEN>
  Content-Type: application/json
  ```

**Sample Request Body:**
```json
{
  "nic": "987654321V",
  "email": "dealer@gasstations.com",
  "password": "DealerPass@123",
  "name": "Silva Gas Station",
  "phone": "+94771234567",
  "address": "45/B Galle Road, Colombo 03",
  "businessName": "Silva Gas Distribution",
  "businessRegNo": "REG2023001"
}
```

**Sample Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJlNDdhYzEwYi01OGNjLTQzNzItYTU2Ny0wZTAyYjJjM2Q0NzAwIiwiZW1haWwiOiJkZWFsZXJAZ2Fzc3RhdGlvbnMuY29tIiwicm9sZSI6IkRFQUxFUiIsImlhdCI6MTcxNTQyODAwMCwiZXhwIjoxNzE1NTE0NDAwfQ.xyz123...",
  "user": {
    "id": "e47ac10b-58cc-4372-a567-0e02b2c3d480",
    "nic": "987654321V",
    "email": "dealer@gasstations.com",
    "name": "Silva Gas Station",
    "role": "DEALER",
    "phone": "+94771234567",
    "address": "45/B Galle Road, Colombo 03",
    "businessName": "Silva Gas Distribution",
    "businessRegNo": "REG2023001",
    "createdAt": "2026-05-11T09:35:00"
  }
}
```

---

### 5. Get Current User Profile

**GET** `/api/v1/users/me`

- **Description:** Get the authenticated user's own profile
- **Authentication:** Required
- **Request Headers:**
  ```
  Authorization: Bearer <JWT_TOKEN>
  ```

**Sample Response (200 OK):**
```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "nic": "123456789V",
  "email": "citizen@example.com",
  "name": "John Citizen",
  "role": "CITIZEN",
  "phone": null,
  "address": null,
  "businessName": null,
  "businessRegNo": null,
  "createdAt": "2026-05-11T09:30:00"
}
```

---

### 6. Get User by ID

**GET** `/api/v1/users/{id}`

- **Description:** Get user details by user ID
- **Authentication:** Required
- **Authorization:** ADMIN or the user themselves
- **Path Parameter:**
  - `id` (string, uuid): User ID
- **Request Headers:**
  ```
  Authorization: Bearer <JWT_TOKEN>
  ```

**Sample Request:**
```
GET /api/v1/users/f47ac10b-58cc-4372-a567-0e02b2c3d479
```

**Sample Response (200 OK):**
```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "nic": "123456789V",
  "email": "citizen@example.com",
  "name": "John Citizen",
  "role": "CITIZEN",
  "phone": null,
  "address": null,
  "businessName": null,
  "businessRegNo": null,
  "createdAt": "2026-05-11T09:30:00"
}
```

---

### 7. Update User Information

**PUT** `/api/v1/users/{id}`

- **Description:** Update user profile information
- **Authentication:** Required
- **Authorization:** ADMIN or the user themselves
- **Path Parameter:**
  - `id` (string, uuid): User ID
- **Request Headers:**
  ```
  Authorization: Bearer <JWT_TOKEN>
  Content-Type: application/json
  ```

**Sample Request Body:**
```json
{
  "name": "John Citizen Updated",
  "email": "newemail@example.com",
  "phone": "+94771234568",
  "address": "New Address 123"
}
```

**Sample Response (200 OK):**
```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "nic": "123456789V",
  "email": "newemail@example.com",
  "name": "John Citizen Updated",
  "role": "CITIZEN",
  "phone": "+94771234568",
  "address": "New Address 123",
  "businessName": null,
  "businessRegNo": null,
  "createdAt": "2026-05-11T09:30:00"
}
```

---

### 8. Get Users by Role (ADMIN Only)

**GET** `/api/v1/users/role/{role}`

- **Description:** Get all users with a specific role (CITIZEN, DEALER, or ADMIN)
- **Authentication:** Required (ADMIN only)
- **Authorization:** ADMIN only
- **Path Parameter:**
  - `role` (string): Role to filter by (CITIZEN, DEALER, ADMIN)
- **Request Headers:**
  ```
  Authorization: Bearer <ADMIN_JWT_TOKEN>
  ```

**Sample Request:**
```
GET /api/v1/users/role/DEALER
```

**Sample Response (200 OK):**
```json
[
  {
    "id": "e47ac10b-58cc-4372-a567-0e02b2c3d480",
    "nic": "987654321V",
    "email": "dealer@gasstations.com",
    "name": "Silva Gas Station",
    "role": "DEALER",
    "phone": "+94771234567",
    "address": "45/B Galle Road, Colombo 03",
    "businessName": "Silva Gas Distribution",
    "businessRegNo": "REG2023001",
    "createdAt": "2026-05-11T09:35:00"
  },
  {
    "id": "d57ac10b-58cc-4372-a567-0e02b2c3d481",
    "nic": "876543210V",
    "email": "lankagases@example.com",
    "name": "Lanka Gases",
    "role": "DEALER",
    "phone": "+94776543210",
    "address": "123 Main Street, Kandy",
    "businessName": "Lanka Gas Distribution Ltd",
    "businessRegNo": "REG2023002",
    "createdAt": "2026-05-11T10:00:00"
  }
]
```

---

### 9. Delete User (ADMIN Only)

**DELETE** `/api/v1/users/{id}`

- **Description:** Delete a user account
- **Authentication:** Required (ADMIN only)
- **Authorization:** ADMIN only
- **Path Parameter:**
  - `id` (string, uuid): User ID to delete
- **Request Headers:**
  ```
  Authorization: Bearer <ADMIN_JWT_TOKEN>
  ```

**Sample Request:**
```
DELETE /api/v1/users/f47ac10b-58cc-4372-a567-0e02b2c3d479
```

**Sample Response (200 OK):**
```json
{
  "message": "User deleted successfully"
}
```

---

## Error Responses

### 400 Bad Request
```json
{
  "status": "error",
  "message": "Validation failed: Email is invalid",
  "timestamp": "2026-05-11T10:00:00"
}
```

### 401 Unauthorized
```json
{
  "status": "error",
  "message": "Invalid credentials",
  "timestamp": "2026-05-11T10:00:00"
}
```

### 403 Forbidden
```json
{
  "status": "error",
  "message": "Access denied. Admin role required",
  "timestamp": "2026-05-11T10:00:00"
}
```

### 404 Not Found
```json
{
  "status": "error",
  "message": "User not found with id: f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "timestamp": "2026-05-11T10:00:00"
}
```

### 409 Conflict
```json
{
  "status": "error",
  "message": "NIC already exists",
  "timestamp": "2026-05-11T10:00:00"
}
```

---

## NIC Format

The system supports two NIC formats:

1. **Old Format:** `123456789V` (9 digits + letter)
2. **New Format:** `200012345678` (12 digits)

**Examples:**
- Old: `123456789V`, `987654321X`, `555555555V`
- New: `199912345678`, `200001234567`, `202001234567`

---

## Testing Workflow

### **Step 1: Start Services**
```bash
docker-compose up -d
```

### **Step 2: Verify User Service is Running**
Make a request to: `GET /api/v1/users/test`

### **Step 3: Register Test Users**

**Option A: Register as Citizen**
```
POST /api/v1/users/register
Body: {
  "nic": "123456789V",
  "email": "testcitizen@example.com",
  "password": "TestPass@123",
  "name": "Test Citizen"
}
```
**Save the returned JWT token in Postman variable: `citizen_token`**

**Option B: Register as Dealer (if you have ADMIN access)**
```
POST /api/v1/users/register/dealer
Headers: Authorization: Bearer <ADMIN_TOKEN>
Body: {
  "nic": "987654321V",
  "email": "testdealer@example.com",
  "password": "DealerPass@123",
  "name": "Test Gas Station",
  "phone": "+94771234567",
  "address": "123 Test Street",
  "businessName": "Test Gas Dealer",
  "businessRegNo": "REG2026001"
}
```
**Save the returned JWT token and user ID in Postman variables: `dealer_token`, `dealer_id`**

### **Step 4: Use Tokens with Other Services**

Now you can use the JWT tokens from user-service to authenticate with inventory-service:

```
Authorization: Bearer {{citizen_token}}
```
or
```
Authorization: Bearer {{dealer_token}}
```

---

## Postman Environment Setup

Create environment variables for testing:

```json
{
  "base_url": "http://localhost:8081",
  "citizen_email": "testcitizen@example.com",
  "citizen_password": "TestPass@123",
  "citizen_nic": "123456789V",
  "citizen_token": "",
  "dealer_email": "testdealer@example.com",
  "dealer_password": "DealerPass@123",
  "dealer_nic": "987654321V",
  "dealer_token": "",
  "dealer_id": "",
  "admin_token": ""
}
```

### **Script to Extract Token (Postman Tests tab)**

Add this script to extract and save the JWT token after login/register:

```javascript
// In the "Tests" tab of register/login request
if (pm.response.code === 200 || pm.response.code === 201) {
  var jsonData = pm.response.json();
  if (jsonData.token) {
    pm.environment.set("citizen_token", jsonData.token);
    pm.environment.set("citizen_id", jsonData.user.id);
    console.log("Token saved: " + jsonData.token.substring(0, 20) + "...");
  }
}
```

---

## Complete Testing Sequence

1. ✅ **GET** `/api/v1/users/test` - Verify service running
2. ✅ **POST** `/api/v1/users/register` - Register citizen (saves token)
3. ✅ **GET** `/api/v1/users/me` - Get own profile
4. ✅ **PUT** `/api/v1/users/{id}` - Update profile
5. ✅ **POST** `/api/v1/users/login` - Login (verify token works)
6. ✅ **GET** `/api/v1/users/{id}` - Get specific user
7. ✅ **POST** `/api/v1/users/register/dealer` - Register dealer (ADMIN only)
8. ✅ **GET** `/api/v1/users/role/DEALER` - List all dealers (ADMIN only)

---

## Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| "NIC already exists" | Use a different NIC value each time you register |
| "Email already exists" | Use a unique email for each test |
| "Invalid JWT token" | Token may have expired (24hr expiry). Re-login to get new token |
| "Access denied" | User doesn't have required role (check token permissions) |
| "Invalid credentials" | Verify NIC and password match registered user |

---

## Security Notes

⚠️ **Important:**
- Never share JWT tokens in production
- Tokens expire after 24 hours
- Always use HTTPS in production (not HTTP)
- Passwords are BCrypt hashed before storage
- Admin account should be created with proper access controls

---

## Integration with Inventory Service

Once you have a valid JWT token from user-service, use it to test inventory-service endpoints:

**Example:**
```
GET /api/v1/inventory/available
Authorization: Bearer <JWT_TOKEN_FROM_USER_SERVICE>
```

The token contains:
- User ID
- User email
- User role (CITIZEN, DEALER, ADMIN)

These are validated by inventory-service to enforce authorization rules.
