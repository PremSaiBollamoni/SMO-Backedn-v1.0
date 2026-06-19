# 🏭 PALMS Backend - RESTful API Server

<div align="center">

<img src="https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java"/>
<img src="https://img.shields.io/badge/Spring%20Boot-3.0+-6DB33F?style=flat-square&logo=spring-boot&logoColor=white" alt="Spring Boot"/>
<img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white" alt="MySQL"/>
<img src="https://img.shields.io/badge/Maven-3.8+-C71A36?style=flat-square&logo=apache-maven&logoColor=white" alt="Maven"/>
<img src="https://img.shields.io/badge/Commits-400+-success?style=flat-square" alt="Commits"/>

**Production-grade Spring Boot API for garment factory management**

[Features](#-features) • [API Docs](#-api-endpoints) • [Setup](#-setup) • [Database](#-database)

</div>

---

## 📋 Overview

PALMS Backend is a robust RESTful API built with Spring Boot 3.0+ and MySQL 8.0. It provides comprehensive endpoints for employee management, operations planning, work assignment, and real-time job tracking with QR-based workflow automation.

### 🎯 Key Metrics
- **REST Endpoints:** 40+
- **Database Tables:** 15+
- **Total Commits:** 400+
- **Request/Response Format:** JSON
- **Authentication:** Role-Based Access Control

---

## ✨ Core Features

### 🔐 Authentication & Authorization
✅ Employee login with credentials  
✅ Multi-role support per employee  
✅ Role-based access control (RBAC)  
✅ Activity-based permissions  
✅ Stateless token validation  

### 👥 Employee Management
✅ Complete CRUD operations  
✅ Role assignment & management  
✅ Employee profile management  
✅ Shift scheduling  
✅ Break management  

### 📊 Operations & Master Data
✅ 34 standard operations (27 Sub Asm + 7 Full Garment)  
✅ SAM (Standard Allowed Minutes) management  
✅ Skill grade assignment (A+, A, B, C, Helper)  
✅ Target pieces per shift  
✅ Sequential operation support  
✅ Excel batch import with smart headers  

### 🎯 Supervisor Operations
✅ Station/Workstation management  
✅ Operation-to-station linking  
✅ Machine code assignment  
✅ Work assignment via QR codes  
✅ Job creation & completion  
✅ Real-time job status tracking  

### 📈 Tracking & Reporting
✅ Attendance records (daily check-in/out)  
✅ Job assignment history  
✅ Efficiency calculations  
✅ Performance metrics  
✅ Operation utilization  
✅ Employee productivity stats  

### 🔄 Advanced Features
✅ Tray lifecycle management (FREE → ASSIGNED → FREE)  
✅ Auto-tray creation on first job scan  
✅ QR mapping with daily reset  
✅ Auto-checkout of all employees  
✅ Transaction-based operations  
✅ Null-safe data handling  

---

## 🏗️ Architecture

### Technology Stack
```
Framework:    Spring Boot 3.0+
ORM:          JPA/Hibernate
Database:     MySQL 8.0
Build:        Maven 3.8+
Language:     Java 17+
Excel:        Apache POI 5.3.0
Validation:   Spring Validation Framework
Logging:      SLF4J + Logback
```

### Project Structure
```
src/main/java/com/cutm/smo/
├── controller/
│   ├── AuthController
│   ├── EmployeeController
│   ├── OperationController
│   ├── WorkstationController
│   ├── JobController
│   ├── AttendanceController
│   └── ImportController
│
├── service/
│   ├── AuthService
│   ├── EmployeeService
│   ├── OperationService
│   ├── WorkstationService
│   ├── JobService
│   ├── AttendanceService
│   └── ImportService
│
├── repository/
│   ├── EmployeeRepository
│   ├── OperationRepository
│   ├── WorkstationRepository
│   ├── JobAssignmentRepository
│   ├── TrayRepository
│   ├── AttendanceRepository
│   └── TempQrMappingRepository
│
├── models/
│   ├── Employee
│   ├── Role
│   ├── Operation
│   ├── Workstation
│   ├── JobAssignment
│   ├── Tray
│   ├── Attendance
│   └── TempQrMapping
│
├── dto/
│   ├── LoginRequest/Response
│   ├── EmployeeRequest/Response
│   ├── OperationRequest
│   ├── JobRequest
│   └── ImportResult
│
└── config/
    ├── DataInitializer
    └── AppConfig
```

---

## 🗄️ Database Schema

### Master Data Tables
| Table | Columns | Purpose |
|-------|---------|---------|
| `employee` | emp_id, emp_name, email, phone, salary, status | Employee master |
| `role` | role_id, role_name, activities, status | User roles (5 fixed) |
| `employee_role` | emp_id, role_id | Employee-Role mapping |
| `operation` | op_id, op_code, op_name, stage, sam, skill_grade, target_pcs, sequence_no | 34 operations |
| `workstation` | ws_id, ws_code, op_id, machine_code, status | Physical stations |

### Transactional Tables
| Table | Columns | Purpose |
|-------|---------|---------|
| `job_assignment` | job_id, ws_id, emp_id, tray_id, status, start_time, end_time | Active/completed jobs |
| `tray` | tray_id, tray_number, status, assigned_to, assigned_at | Reusable trays |
| `attendance` | att_id, emp_id, emp_qr_token, check_in, check_out, status | Daily attendance |
| `temp_qr_mapping` | mapping_id, qr_token, emp_id, mapping_date, freed | Daily QR assignment |

### Entity Relationships
```
Employee (1) ──── (M) Role
Employee (1) ──── (M) Attendance
Employee (1) ──── (M) Job Assignment
Operation (1) ──── (M) Workstation
Operation (1) ──── (M) Job Assignment
Workstation (1) ──── (M) Job Assignment
Tray (1) ──── (M) Job Assignment
```

---

## 📡 API Endpoints

### Authentication
```http
POST /api/auth/login
  Request: { empId, password }
  Response: { empId, empName, roles, allRoles, accessToken }

POST /api/auth/logout
  Request: { empId }
  Response: { success }

GET /api/auth/verify
  Response: { valid, empId, roles }
```

### Employees
```http
GET /api/employees
  Response: [ { empId, empName, email, roles, status } ]

POST /api/employees
  Request: { empId, empName, email, phone, salary, roleIds }
  Response: { empId, empName, email, roles }

PUT /api/employees/{empId}
  Request: { empName, email, phone, salary }
  Response: { success }

DELETE /api/employees/{empId}
  Response: { success }
```

### Operations
```http
GET /api/operations
  Response: [ { opId, opCode, opName, stage, sam, skillGrade, targetPcs, sequenceNo } ]

POST /api/operations
  Request: { opCode, opName, stage, sam, skillGrade, targetPcs, sequenceNo }
  Response: { opId, opCode, opName, ... }

PATCH /api/operations/{opId}/sam
  Request: { opName, stage, sam, skillGrade, targetPcs, sequenceNo }
  Response: { success }

DELETE /api/operations/{opId}
  Response: { success }
```

### Workstations
```http
GET /api/workstations
  Response: [ { wsId, wsCode, operation, machineCode, status } ]

POST /api/workstations
  Request: { wsCode, machineCode, opId }
  Response: { wsId, wsCode, operation, machineCode }

PATCH /api/workstations/{wsId}
  Request: { wsCode, machineCode }
  Response: { success }

DELETE /api/workstations/{wsId}
  Response: { success }
```

### Jobs
```http
GET /api/jobs
  Response: [ { jobId, empName, opName, trayNumber, quantity, status, startTime, endTime } ]

POST /api/jobs/scan
  Request: { empQrToken, trayQrToken, quantity }
  Response: { jobId, status, estMinutes, message }

GET /api/jobs/active/{wsId}
  Response: [ { jobId, empName, opName, elapsedSeconds, estMinutes } ]
```

### Attendance
```http
GET /api/attendance/today
  Response: [ { attId, empId, empName, checkIn, checkOut, status } ]

POST /api/attendance/check-in
  Request: { empQrToken, machineCode }
  Response: { attId, empName, status }

POST /api/attendance/check-out
  Request: { empQrToken, markedBy }
  Response: { empName, checkOut, status }

POST /api/attendance/free-all-qrs
  Response: { freedCount, checkedOutCount }
```

### Import
```http
POST /api/import/upload (multipart/form-data)
  Request: { file: Excel file }
  Response: { operationsCreated, operationsUpdated, stationsCreated, stationsLinked }
```

---

## 🚀 Setup & Deployment

### Prerequisites
- Java 17+ (OpenJDK or Oracle JDK)
- Maven 3.8+
- MySQL 8.0+
- Git

### Local Development

```bash
# Clone repository
git clone https://github.com/PremSaiBollamoni/PALMS-Backend.git
cd PALMS-Backend

# Create database
mysql -u root -p
CREATE DATABASE PALMSV1 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;

# Configure application.properties
vim src/main/resources/application.properties
# Update: spring.datasource.url=jdbc:mysql://localhost:3306/PALMSV1
# Update: spring.datasource.password=YOUR_PASSWORD

# Build & run
mvn clean package
java -jar target/palms-backend-1.0.0.jar

# Server runs on: http://localhost:8080
```

### Docker Deployment

```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/palms-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# Build image
docker build -t palms-backend:latest .

# Run container
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/PALMSV1 \
  -e SPRING_DATASOURCE_PASSWORD=password \
  palms-backend:latest
```

### Production Configuration

```properties
# application-prod.properties
server.port=8080
server.compression.enabled=true
server.compression.min-response-size=1024

spring.datasource.url=jdbc:mysql://mysql-prod:3306/PALMSV1
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.show-sql=false

logging.level.root=WARN
logging.level.com.cutm.smo=INFO
```

---

## 🔑 Key Concepts

### SAM (Standard Allowed Minutes)
Industry standard time per piece:
```
Expected Duration = SAM × Quantity
Example: 0.5 min/piece × 100 pieces = 50 minutes expected
```

### Efficiency Calculation
```
Efficiency % = (Expected Time / Actual Time) × 100

Colors:
- Green (≥95%): On-Time, Good performance
- Orange (<95%): Overdue, Needs attention
```

### QR Workflow
```
EMP-TEMP-XXX (Employee QR) + TRAY-XXX (Tray QR)
        ↓
Auto-creates Job Assignment
        ↓
Job tracked in real-time
        ↓
Scan TRAY-XXX again to complete
        ↓
Tray status → FREE (reusable)
```

### Multi-Role System
```
Employee → Multiple Roles (Admin can assign)
        ↓
Role Picker on login
        ↓
Can switch roles dynamically
        ↓
Different dashboards per role
```

### Transactional Integrity
- All operations use `@Transactional`
- Foreign key constraints enforced
- Optimistic locking for concurrent updates
- Null-safe entity handling

---

## 📊 Data Validation

### Employee
- emp_id: VARCHAR(20), NOT NULL, UNIQUE
- emp_name: VARCHAR(150), NOT NULL
- email: VARCHAR(100), UNIQUE
- salary: DECIMAL(10,2)

### Operation
- op_code: VARCHAR(20), NOT NULL, UNIQUE
- op_name: VARCHAR(150), NOT NULL
- sam: DECIMAL(8,3), NOT NULL
- sequence_no: INT, NOT NULL
- status: ENUM('ACTIVE', 'INACTIVE')

### Workstation
- ws_code: VARCHAR(20), NOT NULL, UNIQUE
- op_id: BIGINT, FOREIGN KEY
- machine_code: VARCHAR(30), NULLABLE, UNIQUE

---

## 🛠️ Development Standards

### Code Style
```java
// Package private unless needed otherwise
// Meaningful variable names
// Max method complexity: 10 cyclomatic complexity
// Use lombok: @Data, @RequiredArgsConstructor, @Slf4j

@Service
@RequiredArgsConstructor
@Slf4j
public class MyService {
    private final MyRepository repo;
    
    @Transactional
    public void doSomething() {
        // Implementation
    }
}
```

### Error Handling
```java
// Throw ResponseStatusException for REST errors
throw new ResponseStatusException(
    HttpStatus.NOT_FOUND, 
    "Operation not found"
);

// Use service layer exceptions for business logic
throw new BusinessException("Invalid operation state");
```

### Testing
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=EmployeeServiceTest

# Test coverage
mvn clean test jacoco:report
```

---

## 📱 Performance Metrics

### Database Optimization
- ✅ Indexed on frequently queried columns
- ✅ Foreign key constraints for referential integrity
- ✅ Connection pooling (HikariCP)
- ✅ Query optimization with proper joins

### API Response Times
- Single record fetch: <50ms
- List with pagination: <100ms
- Excel import (100 rows): <500ms
- Job scan & creation: <200ms

---

## 📞 Support & Contact

**Project Lead:** Prem Sai Bollamoni  
**Email:** premsai200804@gmail.com  
**GitHub:** [@PremSaiBollamoni](https://github.com/PremSaiBollamoni)

**Related Repositories:**
- Frontend: https://github.com/PremSaiBollamoni/PALMS-Frontend
- Backend: https://github.com/PremSaiBollamoni/PALMS-Backend

---

## 📄 License

Proprietary - All rights reserved © 2026

---

**Last Updated:** June 19, 2026  
**API Version:** 1.0.0  
**Database Version:** PALMSV1  
**Total Commits:** 400+
