# OpenAPI Swagger Documentation Guide

## 🎯 Overview

PALMS Backend is now fully documented using **OpenAPI 3.0** and **Springdoc OpenAPI** (Swagger UI). This provides interactive API documentation and testing capabilities.

---

## 📍 Accessing Swagger UI

### Development Server
```
http://localhost:8080/swagger-ui.html
```

### Production Server
```
https://api.palms.local/swagger-ui.html
```

### Local Network
```
http://192.168.0.102:8080/swagger-ui.html
```

---

## 📡 API Documentation Endpoints

### Interactive UI
```http
GET http://localhost:8080/swagger-ui.html
```
Browser-based interactive API explorer with "Try it out" feature

### OpenAPI JSON
```http
GET http://localhost:8080/api-docs
```
Machine-readable OpenAPI 3.0 specification (JSON format)

### Swagger YAML
```http
GET http://localhost:8080/api-docs.yaml
```
Machine-readable OpenAPI 3.0 specification (YAML format)

---

## 🎨 Features

### ✅ Interactive Testing
- Click "Try it out" button on any endpoint
- Enter parameters and request body
- Execute request directly from browser
- View response status, headers, and body
- Copy cURL commands

### ✅ Documented Endpoints
- **Authentication**: Login endpoint with request/response examples
- **Operations**: CRUD operations for manufacturing operations
- **Stations**: Workstation management
- **Jobs**: Job assignment and tracking
- **Attendance**: Check-in/out and QR mapping
- **Import**: Excel file upload

### ✅ Schema Documentation
- All request/response models documented
- Data types and validation rules shown
- Required vs optional fields marked
- Example values provided

### ✅ Error Responses
- HTTP status codes documented
- Error messages explained
- Validation error details shown

---

## 🔐 Authentication in Swagger

### Manual Testing
1. Call `POST /api/auth/login` with employee credentials:
   ```json
   {
     "loginid": "9999",
     "password": "password"
   }
   ```

2. Copy the `accessToken` from response

3. Click the **Authorize** button (🔒) in Swagger UI top-right

4. Paste token in format: `Bearer YOUR_TOKEN_HERE`

5. All subsequent requests will include authorization header

---

## 📊 Endpoint Categories

### 🔐 Authentication Tag
- **POST /api/auth/login** - Employee login
- **POST /api/auth/logout** - Logout
- **GET /api/auth/verify** - Verify token

### 📋 Operations Tag
- **GET /api/hr/operations** - List all operations
- **POST /api/hr/operations** - Create operation
- **PATCH /api/hr/operations/{opId}/sam** - Update SAM
- **DELETE /api/hr/operations/{opId}** - Deactivate

### 🏪 Stations Tag
- **GET /api/hr/workstations** - List stations
- **POST /api/hr/workstations** - Create station
- **PATCH /api/hr/workstations/{wsId}** - Update station
- **DELETE /api/hr/workstations/{wsId}** - Delete station

### 👥 Employees Tag
- **GET /api/hr/employees** - List employees
- **POST /api/hr/employees** - Create employee
- **PUT /api/hr/employees/{empId}** - Update employee
- **DELETE /api/hr/employees/{empId}** - Delete employee

### 📊 Jobs Tag
- **GET /api/supervisor/jobs** - List all jobs
- **POST /api/supervisor/jobs/scan** - Scan tray & assign
- **GET /api/supervisor/jobs/active/{wsId}** - Active jobs

### ✅ Attendance Tag
- **GET /api/attendance/today** - Today's records
- **POST /api/attendance/check-in** - Check-in
- **POST /api/attendance/check-out** - Check-out
- **POST /api/attendance/free-all-qrs** - Auto-checkout

### 📥 Import Tag
- **POST /api/hr/import/upload** - Excel import

---

## 💡 Usage Examples

### Example 1: Create an Operation

1. Go to **Operations** section
2. Click `POST /api/hr/operations`
3. Click **Try it out**
4. Enter request body:
   ```json
   {
     "opCode": "OP-001",
     "opName": "Hemming",
     "stage": "Sub Assembly",
     "sequenceNo": 1,
     "sam": 0.5,
     "skillGrade": "A-grade",
     "targetPcs": 100
   }
   ```
5. Click **Execute**
6. View response (should be 200 OK)

### Example 2: Get Active Operations

1. Click `GET /api/hr/operations`
2. Click **Try it out**
3. Click **Execute**
4. See list of active operations

### Example 3: Update Operation SAM

1. Click `PATCH /api/hr/operations/{opId}/sam`
2. Enter `opId` in parameter field
3. Enter request body with fields to update
4. Click **Execute**

---

## 🔍 Filtering & Sorting

### By Operation Status
- Only ACTIVE operations shown by default
- Use query parameter to filter

### By Sequence Number
- Operations sorted by `sequenceNo` ascending
- Affects list display order

### By Stage
- Filter operations by manufacturing stage
- Sub Assembly vs Full Garment

---

## 📦 Request/Response Examples

### Login Request
```json
{
  "loginid": "9999",
  "password": "YourPassword123"
}
```

### Login Response
```json
{
  "empId": "9999",
  "empName": "Prem Sai Bollamoni",
  "roles": ["SUPERVISOR", "HR"],
  "allRoles": ["SUPERVISOR", "HR", "GM"],
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Operation Request
```json
{
  "opCode": "OP-002",
  "opName": "Button Attachment",
  "stage": "Full Garment",
  "sequenceNo": 2,
  "sam": 0.3,
  "skillGrade": "B-grade",
  "targetPcs": 150
}
```

### Operation Response
```json
{
  "opId": 1,
  "opCode": "OP-002",
  "opName": "Button Attachment",
  "stage": "Full Garment",
  "sequenceNo": 2,
  "sam": 0.3,
  "skillGrade": "B-grade",
  "targetPcs": 150,
  "status": "ACTIVE",
  "createdAt": "2026-06-19T10:30:00"
}
```

---

## 🚀 Integration with Frontend

### Swagger API to Flutter
1. Open Swagger UI
2. Select endpoint
3. See the exact request format
4. Copy cURL command
5. Use with Dio HTTP client

### Example cURL from Swagger
```bash
curl -X GET "http://localhost:8080/api/hr/operations" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Convert to Dart/Dio
```dart
final response = await dio.get(
  '/api/hr/operations',
  options: Options(
    headers: {'Authorization': 'Bearer $token'}
  ),
);
```

---

## 🔗 Server Configuration

### Development
```properties
# application.properties
server.port=8080
server.address=0.0.0.0
```

### Production
```properties
spring.profiles.active=prod
server.port=8080
```

---

## 📝 Best Practices

### Testing Endpoints
1. Start with READ operations (GET)
2. Test with sample data first
3. Use Authorize button for secured endpoints
4. Check response status codes
5. Validate response structure

### Error Handling
- 200: Success
- 400: Bad request (invalid data)
- 401: Unauthorized (missing/invalid token)
- 404: Not found (resource doesn't exist)
- 409: Conflict (duplicate entry)
- 500: Server error

### Performance
- Swagger UI loads instantly
- Interactive testing has <500ms latency
- All requests logged for debugging

---

## 🔧 Configuration Details

### Swagger Config File
```
src/main/java/com/cutm/smo/config/OpenAPIConfig.java
```

### Application Properties
```
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.operations-sorter=method
springdoc.swagger-ui.tags-sorter=alpha
```

---

## 📞 Support

**For API Questions:**
- Check the Swagger documentation first
- Try the "Try it out" feature to test
- Review request/response examples
- Check error responses for hints

**For Issues:**
- Email: premsai200804@gmail.com
- GitHub: https://github.com/PremSaiBollamoni/PALMS-Backend

---

## 📚 Related Documentation

- [Backend README](README.md) - Full architecture & setup
- [API Endpoints](README.md#-api-endpoints) - Detailed endpoint list
- [Database Schema](README.md#-database-schema) - Data model

---

**Last Updated:** June 19, 2026  
**Swagger Version:** OpenAPI 3.0 with Springdoc 2.0.2  
**Backend Version:** 1.0.0
