# Lovable UI Prompt for Smart Gas Distribution Platform

Use this prompt in Lovable to generate the frontend UI for the Smart Gas Distribution Platform.

---

## 📋 Copy & Paste This Prompt to Lovable:

```
Build a comprehensive web application UI for the "Smart Gas Distribution Platform" - an LPG (Liquefied Petroleum Gas) distribution and virtual queue management system for Sri Lanka.

## System Overview
This is an event-driven microservices platform with three user types (CITIZEN, DEALER, ADMIN) that manages gas cylinder inventory, allocation approvals, and virtual queues.

## API Configuration
**Base URL:** `http://localhost:8080` (API Gateway)
**JWT Token:** Stored in localStorage after login, passed in `Authorization: Bearer <token>` header
**JWT Format:** Expires in 24 hours, contains user ID, email, and role

## API Endpoints

### User Service (`/api/v1/users`)
1. **POST /register** - Citizen self-registration
   - Body: `{ "nic": "123456789V", "email": "user@example.com", "password": "password", "name": "John Perera" }`
   - Returns: `{ "token": "JWT_TOKEN", "user": { "id": "uuid", "nic": "...", "email": "...", "name": "...", "role": "CITIZEN", "createdAt": "..." } }`

2. **POST /login** - Authenticate user
   - Body: `{ "nic": "123456789V", "password": "password" }`
   - Returns: Same as register (token + user object)

3. **POST /register/dealer** - Admin registers a dealer (ADMIN only)
   - Headers: `Authorization: Bearer <ADMIN_TOKEN>`
   - Body: `{ "nic": "987654321V", "email": "dealer@station.lk", "password": "password", "name": "Silva Gas Station", "phone": "+94771234567", "address": "45/B Galle Rd, Colombo 03", "businessName": "Silva Gas Distribution", "businessRegNo": "REG-2024-001" }`
   - Returns: `{ "token": "JWT_TOKEN", "user": { "id": "uuid", "role": "DEALER", "businessName": "...", "phone": "...", ... } }`

4. **GET /me** - Get current user profile
   - Returns: Current logged-in user object

5. **GET /{id}** - Get user by ID
   - Returns: User details

6. **PUT /{id}** - Update user profile
   - Body: `{ "name": "Updated Name", "email": "newemail@example.com", "phone": "+94771234567", "address": "New Address" }`

7. **GET /role/{role}** - List all users by role (ADMIN only)
   - Path: `/role/DEALER` or `/role/CITIZEN` or `/role/ADMIN`
   - Returns: Array of users

### Inventory Service (`/api/v1/inventory`)
1. **POST /** - Create inventory for dealer (ADMIN only)
   - Headers: `Authorization: Bearer <ADMIN_TOKEN>`
   - Body: `{ "dealerId": "uuid", "dealerName": "Silva Gas Station", "address": "45/B Galle Rd, Colombo 03", "latitude": 6.9271, "longitude": 79.8612, "availableStock": 50 }`
   - Returns: Inventory object with id, lastUpdated timestamp

2. **GET /{id}** - Get inventory by ID
   - Returns: `{ "id": "uuid", "dealerId": "uuid", "dealerName": "Silva Gas Station", "address": "45/B Galle Rd, Colombo 03", "latitude": 6.9271, "longitude": 79.8612, "availableStock": 50, "lastUpdated": "2026-05-11T10:00:00" }`

3. **GET /dealer/{dealerId}** - Get inventory for specific dealer
   - Returns: Single inventory object

4. **GET /available** - Get all dealers with stock > 0
   - Returns: Array of inventory objects

5. **GET /nearby?lat=6.9271&lng=79.8612&radius=10** - Find dealers within X km (geolocation - Haversine)
   - Query Params: lat (number), lng (number), radius (number in km)
   - Returns: Array of nearby dealers with stock

6. **PUT /{id}/stock** - Update stock level (DEALER can only update own)
   - Headers: `Authorization: Bearer <TOKEN>`
   - Body: `{ "availableStock": 30 }`
   - Returns: Updated inventory object

### Allocation Service (`/api/v1/allocations`)
1. **POST /request** - Dealer requests gas allocation
   - Headers: `Authorization: Bearer <DEALER_TOKEN>`
   - Body: `{ "requestedQuantity": 100 }`
   - Returns: `{ "id": "uuid", "dealerId": "uuid", "dealerName": "Silva Gas Station", "requestedQuantity": 100, "approvedQuantity": null, "status": "PENDING", "requestedAt": "2026-05-11T10:00:00" }`

2. **GET /pending** - Get all PENDING allocation requests (ADMIN only)
   - Returns: Array of PENDING allocations

3. **GET ?status=APPROVED** - Filter allocations by status (ADMIN only)
   - Query: `?status=PENDING` or `APPROVED` or `REJECTED` or `DELIVERED`
   - Returns: Filtered array

4. **PUT /{id}/approve** - Admin approves allocation (ADMIN only)
   - Headers: `Authorization: Bearer <ADMIN_TOKEN>`
   - Body: `{ "approvedQuantity": 100 }`
   - Returns: Updated allocation object with status "APPROVED"

5. **PUT /{id}/reject** - Admin rejects allocation (ADMIN only)
   - Headers: `Authorization: Bearer <ADMIN_TOKEN>`
   - Body: `{ "rejectionReason": "Insufficient stock" }`
   - Returns: Updated allocation object with status "REJECTED"

6. **PUT /{id}/confirm** - Dealer confirms delivery received (DEALER only)
   - Headers: `Authorization: Bearer <DEALER_TOKEN>`
   - Returns: Updated allocation object with status "DELIVERED"

7. **GET /dealer/{dealerId}** - Get dealer's allocation history (DEALER can only see own)
   - Returns: Array of allocations for that dealer

8. **GET /{id}** - Get single allocation details
   - Returns: Single allocation object

## User Roles & Permissions

### CITIZEN
- **Permissions:** Read-only access
- **Features:**
  - View profile (`GET /me`)
  - Search nearby dealers by location (`GET /inventory/nearby?lat=...&lng=...&radius=...`)
  - View available stock at dealers (`GET /inventory/available`)
  - View allocation status (future - queue management)

### DEALER
- **Permissions:** Manage own inventory and allocations
- **Features:**
  - View own profile (`GET /me`)
  - Update own stock levels (`PUT /inventory/{id}/stock`)
  - Request gas allocation (`POST /allocations/request`)
  - View own allocation history (`GET /allocations/dealer/{dealerId}`)
  - Confirm delivery received (`PUT /allocations/{id}/confirm`)
  - Register only by ADMIN

### ADMIN
- **Permissions:** Full system access
- **Features:**
  - All DEALER features
  - All CITIZEN features
  - Register new dealers (`POST /users/register/dealer`)
  - Create inventory records (`POST /inventory`)
  - Review pending allocations (`GET /allocations/pending`)
  - Approve/reject allocations (`PUT /allocations/{id}/approve` or `/reject`)
  - View all users by role (`GET /users/role/{role}`)
  - Delete users (`DELETE /users/{id}`)

## UI Features to Implement

### Authentication Pages
1. **Login Page**
   - Form fields: NIC (123456789V or 200012345678 format), password
   - "New user? Register here" link
   - Login button
   - Error messages for invalid credentials
   - Store JWT token in localStorage after successful login

2. **Register Page (Citizen)**
   - Form fields: NIC, email, password, confirm password, full name
   - Validation for NIC format, email, password strength
   - Create account button
   - "Already have account? Login" link

### Citizen Dashboard
1. **Stock Discovery**
   - Map view or list view showing nearby dealers
   - Search by location (enter lat/lng or current location)
   - Filter by radius (5km, 10km, 15km)
   - Display: Dealer name, address, available stock, distance
   - Mock geolocation: Colombo (6.9271, 79.8612) - default center

2. **Dealer Details Card**
   - Show dealer info: name, address, phone, available stock
   - Show location on map with pin
   - "Join Queue" button (future feature - placeholder)

3. **Profile Page**
   - Display user info: ID, NIC, email, name, created date
   - Edit button for name/email
   - Logout button

### Dealer Dashboard
1. **Stock Management**
   - Display current inventory: dealerName, address, availableStock, lastUpdated
   - "Update Stock" form to modify availableStock
   - Confirmation after update with success message
   - Stock history or trends (optional)

2. **Allocation Request Management**
   - Form to submit new allocation request (quantity input)
   - List of all my allocations with status badges:
     - PENDING (yellow badge)
     - APPROVED (green badge)
     - REJECTED (red badge)
     - DELIVERED (blue badge)
   - For APPROVED allocations: "Confirm Delivery Received" button
   - Display columns: ID, Requested Qty, Approved Qty, Status, Requested Date, Actions
   - Modal/detail view for each allocation

3. **Profile Page**
   - Display dealer info: ID, NIC, email, name, business name, phone, address
   - Edit button
   - Logout button

### Admin Dashboard
1. **User Management**
   - Tabs: Citizens, Dealers, Admins
   - List all users by role (GET /users/role/CITIZEN, etc.)
   - Columns: ID, NIC, Email, Name, Role, Created Date
   - "Register New Dealer" button → pops modal with form
   - Delete user button (trash icon)
   - Search/filter by email or name

2. **Dealer Registration Modal**
   - Form fields: NIC, email, password, name, phone, address, businessName, businessRegNo
   - Submit button
   - Success notification with dealer details

3. **Inventory Management**
   - List all dealer inventories
   - Columns: ID, Dealer Name, Address, Latitude, Longitude, Available Stock, Last Updated
   - "Create New Inventory" button → pops modal
   - Edit stock levels
   - Map view showing all dealer locations (optional)

4. **Inventory Creation Modal**
   - Form fields: Select Dealer (dropdown from /users/role/DEALER), Address, Latitude, Longitude, Initial Stock
   - Submit button

5. **Allocation Approvals Dashboard**
   - Filter tabs: PENDING, APPROVED, REJECTED, DELIVERED
   - Table columns: Allocation ID, Dealer Name, Requested Qty, Status, Requested Date, Actions
   - For PENDING: Show "Approve" button → modal with quantity input, and "Reject" button → modal with reason input
   - For APPROVED: Show approved quantity awarded
   - Display Dealer contact info (name, phone)

6. **Allocation Approval Modal**
   - Approve Modal: Input field for approved quantity, submit button
   - Reject Modal: Text area for rejection reason, submit button
   - Confirmation message after action

### Common Components
1. **Navigation Bar**
   - Logo/brand name
   - User name and role badge (CITIZEN, DEALER, ADMIN)
   - Logout button
   - Active page indicator

2. **Error Messages**
   - Toast notifications for API errors
   - Display error messages from API responses
   - Auto-dismiss after 5 seconds

3. **Loading States**
   - Show spinners during API calls
   - Disable buttons while loading

4. **Search & Filters**
   - Search inventory by dealer name
   - Filter by location/distance
   - Filter by status (for allocations)

## Data for Testing

### Sample Users
```
CITIZEN: NIC: 123456789V, Email: citizen@test.lk, Password: TestPass@123, Name: John Perera
DEALER: NIC: 987654321V, Email: dealer@test.lk, Password: DealerPass@123, Name: Silva Gas Station
ADMIN: NIC: 555555555V, Email: admin@test.lk, Password: AdminPass@123, Name: System Admin
```

### Sample Dealers (for inventory)
```
1. "Silva Gas Station" - 45/B Galle Rd, Colombo 03 - Lat: 6.9271, Lng: 79.8612, Stock: 50
2. "Lanka Gases" - 123 Main Street, Kandy - Lat: 7.2906, Lng: 80.6337, Stock: 75
3. "Colombo Gas Hub" - 88 Sir Baron Jayatilaka Mw, Colombo 05 - Lat: 6.8889, Lng: 79.8789, Stock: 30
```

### Sample Allocations
```
1. Dealer: "Silva Gas Station", Requested: 100 units, Status: PENDING
2. Dealer: "Lanka Gases", Requested: 150 units, Status: APPROVED, Approved: 140
3. Dealer: "Colombo Gas Hub", Requested: 80 units, Status: REJECTED, Reason: "Insufficient stock"
```

## Technical Requirements
- **Framework:** React or Vue (your choice based on preferences)
- **Styling:** Tailwind CSS for responsive design
- **State Management:** Use Context API or Pinia/Vuex as appropriate
- **HTTP Client:** Fetch API or axios
- **Authentication:** JWT stored in localStorage
- **Routing:** React Router or Vue Router
- **Map Integration:** Leaflet or Google Maps (optional, can use list view instead)
- **Responsive Design:** Mobile-first, works on desktop/tablet/mobile
- **UI Library:** Shadcn/ui, DaisyUI, or custom Tailwind components

## Styling Guide
- **Color Scheme:**
  - Primary: Blue (#3B82F6)
  - Success: Green (#10B981)
  - Warning: Yellow (#F59E0B)
  - Error: Red (#EF4444)
  - Neutral: Gray (#6B7280)
- **Typography:** Clean, readable fonts (Inter or similar)
- **Status Badges:**
  - PENDING: Yellow background, dark text
  - APPROVED: Green background, white text
  - REJECTED: Red background, white text
  - DELIVERED: Blue background, white text
  - CITIZEN: Light blue badge
  - DEALER: Purple badge
  - ADMIN: Red badge

## Key Features
1. ✅ JWT-based authentication (24-hour tokens)
2. ✅ Role-based access control (different dashboards for CITIZEN/DEALER/ADMIN)
3. ✅ Real-time inventory search and geolocation filtering
4. ✅ Allocation request workflow with approval/rejection
5. ✅ User management and dealer registration (admin)
6. ✅ Stock management for dealers
7. ✅ Responsive design
8. ✅ Error handling and validation
9. ✅ Loading states
10. ✅ Toast notifications

## Additional Notes
- The backend is already fully functional - only the frontend needs to be built
- All API endpoints are accessible through `http://localhost:8080` (API Gateway)
- JWT token must be included in the `Authorization: Bearer <token>` header for protected routes
- Token expires after 24 hours - user must log in again
- Citizens can view inventory but cannot modify it
- Dealers can only update their own inventory and see their own allocations
- Admins have full access to all resources
- Build a beautiful, intuitive UI that modern users expect

Make it visually appealing, user-friendly, and production-ready!
```

---

## 🚀 How to Use This Prompt

1. **Copy the entire prompt** starting from "Build a comprehensive web application UI..." through "Make it visually appealing..."
2. **Go to Lovable.dev**
3. **Create a new project**
4. **Paste the prompt** in the initial description
5. **Choose your preferred framework** (React or Vue)
6. **Let Lovable generate the UI**

---

## ✨ What Lovable Will Generate

- ✅ Complete authentication screens (login, register)
- ✅ Role-based dashboards (Citizen, Dealer, Admin)
- ✅ API integration with JWT token management
- ✅ Responsive layout with navigation
- ✅ Data tables for user/inventory/allocation management
- ✅ Forms for creating dealers, inventory, and allocation requests
- ✅ Real-time search and filtering
- ✅ Status badges and indicators
- ✅ Loading states and error handling
- ✅ Toast notifications
- ✅ Complete CRUD operations through UI

---

## 🔧 Post-Generation Steps

After Lovable generates the code:

1. **Update API Base URL** if needed (currently `http://localhost:8080`)
2. **Add environment variables** for API endpoints
3. **Test with your running services** by running `docker compose up -d`
4. **Customize styling** as per your brand guidelines
5. **Add additional features** like:
   - Map integration for geolocation
   - Queue management UI
   - Notifications
   - Reporting dashboard

---

## 📞 Support

If you need modifications to the generated code:
- Request Lovable to adjust styling
- Ask for additional pages or features
- Request API integration changes
- Request role-based customizations

---

## 🎯 Project Structure After Generation

Your generated Lovable project will have:
```
src/
├── components/        # Reusable components
├── pages/            # Page components
├── layouts/          # Layout components
├── services/         # API service layer
├── utils/            # Utility functions
├── context/          # Auth & state context
├── hooks/            # Custom React hooks
└── styles/           # CSS/Tailwind styles
```

---

**Happy coding! 🚀**
