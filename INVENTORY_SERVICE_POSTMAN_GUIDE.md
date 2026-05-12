# Inventory Service - Postman Testing Guide

## Service Configuration

- **Base URL:** `http://localhost:8082`
- **Service Name:** inventory-service
- **Port:** 8082
- **Database:** PostgreSQL at `localhost:5433/inventorydb`
- **API Version:** v1

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

## Planned Endpoints (To Be Implemented)

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
  "dealerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
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
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "dealerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "dealerName": "Silva Gas Station",
  "address": "45/B Galle Road, Colombo 03",
  "latitude": 6.9271,
  "longitude": 79.8612,
  "availableStock": 45,
  "lastUpdated": "2026-05-11T09:30:00"
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
GET /api/v1/inventory/123e4567-e89b-12d3-a456-426614174000
```

**Sample Response (200 OK):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "dealerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "dealerName": "Silva Gas Station",
  "address": "45/B Galle Road, Colombo 03",
  "latitude": 6.9271,
  "longitude": 79.8612,
  "availableStock": 45,
  "lastUpdated": "2026-05-11T09:30:00"
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
GET /api/v1/inventory/dealer/f47ac10b-58cc-4372-a567-0e02b2c3d479
```

**Sample Response (200 OK):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "dealerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "dealerName": "Silva Gas Station",
  "address": "45/B Galle Road, Colombo 03",
  "latitude": 6.9271,
  "longitude": 79.8612,
  "availableStock": 45,
  "lastUpdated": "2026-05-11T09:30:00"
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
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "dealerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "dealerName": "Silva Gas Station",
    "address": "45/B Galle Road, Colombo 03",
    "latitude": 6.9271,
    "longitude": 79.8612,
    "availableStock": 45,
    "lastUpdated": "2026-05-11T09:30:00"
  },
  {
    "id": "223e4567-e89b-12d3-a456-426614174001",
    "dealerId": "e47ac10b-58cc-4372-a567-0e02b2c3d470",
    "dealerName": "Lanka Gases",
    "address": "123 Main Street, Kandy",
    "latitude": 7.2906,
    "longitude": 80.6337,
    "availableStock": 62,
    "lastUpdated": "2026-05-11T08:15:00"
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
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "dealerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "dealerName": "Silva Gas Station",
    "address": "45/B Galle Road, Colombo 03",
    "latitude": 6.9271,
    "longitude": 79.8612,
    "availableStock": 45,
    "lastUpdated": "2026-05-11T09:30:00"
  },
  {
    "id": "223e4567-e89b-12d3-a456-426614174001",
    "dealerId": "e47ac10b-58cc-4372-a567-0e02b2c3d470",
    "dealerName": "Lanka Gases",
    "address": "123 Main Street, Kandy",
    "latitude": 7.1234,
    "longitude": 80.6337,
    "availableStock": 62,
    "lastUpdated": "2026-05-11T08:15:00"
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
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "dealerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "dealerName": "Silva Gas Station",
  "address": "45/B Galle Road, Colombo 03",
  "latitude": 6.9271,
  "longitude": 79.8612,
  "availableStock": 30,
  "lastUpdated": "2026-05-11T10:00:00"
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
2. **Set Base URL** variable to `http://localhost:8082`
3. **Add Authorization variable** for JWT token obtained from user-service
4. **Create requests** using the endpoints listed above

### Environment Variables to Set:
```
{
  "base_url": "http://localhost:8082",
  "jwt_token": "YOUR_JWT_TOKEN_HERE",
  "inventory_id": "123e4567-e89b-12d3-a456-426614174000",
  "dealer_id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "user_latitude": 6.9271,
  "user_longitude": 79.8612,
  "search_radius": 10
}
```

---

## Testing Workflow

1. **Start the service:** Ensure `docker-compose up -d` is running
2. **Verify connectivity:** Test the `/api/v1/inventory/test` endpoint
3. **Get JWT token:** From user-service login endpoint
4. **Create inventory:** POST request with sample payload
5. **Retrieve inventory:** GET requests to verify data
6. **Update stock:** PUT request to modify available stock
7. **Location search:** Test geolocation queries with different coordinates

---

## Notes

- All timestamps are in ISO 8601 format (UTC)
- UUIDs are used for all ID fields
- Stock quantities must be non-negative integers
- Latitude/Longitude follow WGS84 coordinate system
- Haversine distance calculation for location-based queries
- Service auto-registers with Eureka discovery server on startup
