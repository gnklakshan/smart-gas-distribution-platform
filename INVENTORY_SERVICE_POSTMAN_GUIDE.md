# Inventory Service - Postman Testing Guide

## Service Configuration

- **Base URL (direct):** `http://localhost:8082`
- **Base URL (recommended via gateway):** `http://localhost:8080`
- **Service Name:** inventory-service
- **Port:** 8082
- **Database:** PostgreSQL at `localhost:5433/inventorydb`
- **API Version:** v1

---

## Seeded Test Data

The databases are already seeded and running in Docker.

### Where to access the data

- **Inventory API via Gateway:** `http://localhost:8080/api/v1/inventory`
- **Inventory API direct:** `http://localhost:8082/api/v1/inventory`
- **User login API via Gateway:** `http://localhost:8080/api/v1/users/login`
- **Inventory database:** PostgreSQL on `localhost:5433`, database `inventorydb`
- **User database:** PostgreSQL on `localhost:5432`, database `userdb`
- **PostgreSQL credentials:** username `postgres`, password `postgres`

### Shared seeded password

All seeded users use the same password:

```text
Pass123!
```

### Seeded accounts

| Role | Name | NIC | Email | User ID |
|---|---|---|---|---|
| ADMIN | System Admin | `199001230001` | `admin@gastracker.lk` | `11111111-1111-1111-1111-111111111111` |
| DEALER | Silva Gas Station | `987654321V` | `dealer1@gastracker.lk` | `22222222-2222-2222-2222-222222222222` |
| DEALER | Kandy Gas Center | `876543210V` | `dealer2@gastracker.lk` | `33333333-3333-3333-3333-333333333333` |
| CITIZEN | John Citizen | `123456789V` | `citizen@gastracker.lk` | `44444444-4444-4444-4444-444444444444` |

### Seeded inventory records

| Dealer | Inventory ID | Dealer ID | Stock |
|---|---|---|---|
| Silva Gas Station | `934a62d3-0a28-4465-834c-bd355d29cd34` | `22222222-2222-2222-2222-222222222222` | `30` |
| Kandy Gas Center | `55555555-5555-5555-5555-555555555555` | `33333333-3333-3333-3333-333333333333` | `62` |

### Login request examples

**Admin login**
```json
POST /api/v1/users/login
{
  "nic": "199001230001",
  "password": "Pass123!"
}
```

**Dealer login**
```json
POST /api/v1/users/login
{
  "nic": "987654321V",
  "password": "Pass123!"
}
```

---

## Currently Available Endpoints

### 1. Health Check / Test Endpoint

**GET** `/api/v1/inventory/test`

- **Description:** Check if the inventory service is running
- **Authentication:** No
- **Request Body:** None

**Sample Response:**
```json
{
  "status": "ok",
  "service": "inventory-service",
  "message": "Inventory Service is running"
}
```

---

## Available Endpoints

### 2. Create Inventory for Dealer

**POST** `/api/v1/inventory`

- **Description:** Create a new inventory record for a dealer
- **Authentication:** Required (ADMIN role)
- **Request Headers:**
  ```
  Authorization: Bearer <JWT_TOKEN>
  Content-Type: application/json
  ```

**Sample Request Body:**
```json
{
  "dealerId": "22222222-2222-2222-2222-222222222222",
  "dealerName": "Silva Gas Station",
  "address": "45/B Galle Road, Colombo 03",
  "latitude": 6.9271,
  "longitude": 79.8612,
  "availableStock": 45
}
```

**Sample Response (201 Created):**
```json
{
  "id": "934a62d3-0a28-4465-834c-bd355d29cd34",
  "dealerId": "22222222-2222-2222-2222-222222222222",
  "dealerName": "Silva Gas Station",
  "address": "45/B Galle Road, Colombo 03",
  "latitude": 6.9271,
  "longitude": 79.8612,
  "availableStock": 45,
  "lastUpdated": "2026-05-12T09:32:25"
}
```

---

### 3. Get Inventory by ID

**GET** `/api/v1/inventory/{id}`

- **Description:** Retrieve inventory details by inventory ID
- **Authentication:** Required
- **Path Parameter:**
  - `id` (string, uuid): Inventory ID
- **Request Headers:**
  ```
  Authorization: Bearer <JWT_TOKEN>
  ```

**Sample Request:**
```
GET /api/v1/inventory/934a62d3-0a28-4465-834c-bd355d29cd34
```

**Sample Response (200 OK):**
```json
{
  "id": "934a62d3-0a28-4465-834c-bd355d29cd34",
  "dealerId": "22222222-2222-2222-2222-222222222222",
  "dealerName": "Silva Gas Station",
  "address": "45/B Galle Road, Colombo 03",
  "latitude": 6.9271,
  "longitude": 79.8612,
  "availableStock": 30,
  "lastUpdated": "2026-05-12T09:32:25"
}
```

---

### 4. Get Stock for Specific Dealer

**GET** `/api/v1/inventory/dealer/{dealerId}`

- **Description:** Get inventory stock for a specific dealer
- **Authentication:** Required
- **Path Parameter:**
  - `dealerId` (string, uuid): Dealer ID from user-service
- **Request Headers:**
  ```
  Authorization: Bearer <JWT_TOKEN>
  ```

**Sample Request:**
```
GET /api/v1/inventory/dealer/22222222-2222-2222-2222-222222222222
```

**Sample Response (200 OK):**
```json
{
  "id": "934a62d3-0a28-4465-834c-bd355d29cd34",
  "dealerId": "22222222-2222-2222-2222-222222222222",
  "dealerName": "Silva Gas Station",
  "address": "45/B Galle Road, Colombo 03",
  "latitude": 6.9271,
  "longitude": 79.8612,
  "availableStock": 30,
  "lastUpdated": "2026-05-12T09:32:25"
}
```

---

### 5. Get All Available Dealers (Stock > 0)

**GET** `/api/v1/inventory/available`

- **Description:** Retrieve list of all dealers with available stock greater than 0
- **Authentication:** Required
- **Request Headers:**
  ```
  Authorization: Bearer <JWT_TOKEN>
  ```

**Sample Response (200 OK):**
```json
[
  {
    "id": "934a62d3-0a28-4465-834c-bd355d29cd34",
    "dealerId": "22222222-2222-2222-2222-222222222222",
    "dealerName": "Silva Gas Station",
    "address": "45/B Galle Road, Colombo 03",
    "latitude": 6.9271,
    "longitude": 79.8612,
    "availableStock": 30,
    "lastUpdated": "2026-05-12T09:32:25"
  },
  {
    "id": "55555555-5555-5555-5555-555555555555",
    "dealerId": "33333333-3333-3333-3333-333333333333",
    "dealerName": "Kandy Gas Center",
    "address": "123 Peradeniya Road, Kandy",
    "latitude": 7.2906,
    "longitude": 80.6337,
    "availableStock": 62,
    "lastUpdated": "2026-05-12T09:20:00"
  }
]
```

---

### 6. Find Dealers by Location (Geolocation - Haversine)

**GET** `/api/v1/inventory/nearby`

- **Description:** Find dealers within a specified radius from given coordinates
- **Authentication:** Required
- **Query Parameters:**
  - `lat` (number, required): Latitude
  - `lng` (number, required): Longitude
  - `radius` (number, required): Search radius in kilometers
- **Request Headers:**
  ```
  Authorization: Bearer <JWT_TOKEN>
  ```

**Sample Request:**
```
GET /api/v1/inventory/nearby?lat=6.9271&lng=79.8612&radius=10
```

**Sample Response (200 OK):**
```json
[
  {
    "id": "934a62d3-0a28-4465-834c-bd355d29cd34",
    "dealerId": "22222222-2222-2222-2222-222222222222",
    "dealerName": "Silva Gas Station",
    "address": "45/B Galle Road, Colombo 03",
    "latitude": 6.9271,
    "longitude": 79.8612,
    "availableStock": 30,
    "lastUpdated": "2026-05-12T09:32:25",
    "distanceKm": 0.0
  },
  {
    "id": "55555555-5555-5555-5555-555555555555",
    "dealerId": "33333333-3333-3333-3333-333333333333",
    "dealerName": "Kandy Gas Center",
    "address": "123 Peradeniya Road, Kandy",
    "latitude": 7.2906,
    "longitude": 80.6337,
    "availableStock": 62,
    "lastUpdated": "2026-05-12T09:20:00",
    "distanceKm": 99.58
  }
]
```

---

### 7. Update Stock Level

**PUT** `/api/v1/inventory/{id}/stock`

- **Description:** Update the available stock for an inventory record
- **Authentication:** Required (DEALER role - can only update own inventory)
- **Path Parameter:**
  - `id` (string, uuid): Inventory ID
- **Request Headers:**
  ```
  Authorization: Bearer <JWT_TOKEN>
  Content-Type: application/json
  ```

**Sample Request Body:**
```json
{
  "availableStock": 30
}
```

**Sample Response (200 OK):**
```json
{
  "id": "934a62d3-0a28-4465-834c-bd355d29cd34",
  "dealerId": "22222222-2222-2222-2222-222222222222",
  "dealerName": "Silva Gas Station",
  "address": "45/B Galle Road, Colombo 03",
  "latitude": 6.9271,
  "longitude": 79.8612,
  "availableStock": 30,
  "lastUpdated": "2026-05-12T09:32:25"
}
```

---

## Error Responses

All endpoints may return error responses in the following format:

### 400 Bad Request
```json
{
  "timestamp": "2026-05-11T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: availableStock must be greater than or equal to 0",
  "path": "/api/v1/inventory/123e4567-e89b-12d3-a456-426614174000/stock"
}
```

### 401 Unauthorized
```json
{
  "timestamp": "2026-05-11T10:00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or missing JWT token",
  "path": "/api/v1/inventory/available"
}
```

### 403 Forbidden
```json
{
  "timestamp": "2026-05-11T10:00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "You can only update your own inventory",
  "path": "/api/v1/inventory/123e4567-e89b-12d3-a456-426614174000/stock"
}
```

### 404 Not Found
```json
{
  "timestamp": "2026-05-11T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Inventory not found with id: 123e4567-e89b-12d3-a456-426614174000",
  "path": "/api/v1/inventory/123e4567-e89b-12d3-a456-426614174000"
}
```

---

## Postman Collection Quick Setup

1. **Create a new collection** named "Inventory Service"
2. **Set Base URL** variable to `http://localhost:8080`
3. **Add Authorization variable** for JWT token obtained from user-service login
4. **Create requests** using the endpoints listed above

### Environment Variables to Set:
```
{
  "base_url": "http://localhost:8080",
  "jwt_token": "YOUR_JWT_TOKEN_HERE",
  "inventory_id": "934a62d3-0a28-4465-834c-bd355d29cd34",
  "dealer_id": "22222222-2222-2222-2222-222222222222",
  "user_latitude": 6.9271,
  "user_longitude": 79.8612,
  "search_radius": 10,
  "admin_nic": "199001230001",
  "dealer_nic": "987654321V",
  "seed_password": "Pass123!"
}
```

---

## Testing Workflow

1. **Start the service:** Ensure `docker-compose up -d` is running
2. **Verify connectivity:** Test the `/api/v1/inventory/test` endpoint
3. **Login as ADMIN:** `POST /api/v1/users/login` with NIC `199001230001` and password `Pass123!`
4. **Create inventory:** `POST /api/v1/inventory` using the admin token
5. **Login as DEALER:** `POST /api/v1/users/login` with NIC `987654321V` and password `Pass123!`
6. **Retrieve inventory:** Test `GET /available`, `GET /dealer/{dealerId}`, and `GET /{id}`
7. **Update stock:** `PUT /api/v1/inventory/{id}/stock` using the dealer token
8. **Nearby search:** Test `/api/v1/inventory/nearby?lat=6.9271&lng=79.8612&radius=10`

---

## Notes

- All timestamps are in ISO 8601 format (UTC)
- UUIDs are used for all ID fields
- Stock quantities must be non-negative integers
- Latitude/Longitude follow WGS84 coordinate system
- Haversine distance calculation for location-based queries
- Service auto-registers with Eureka discovery server on startup
