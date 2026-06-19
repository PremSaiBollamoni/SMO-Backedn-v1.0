# 🏭 PALMS - Parallel Assembly Line Management System (Backend)

<div align="center">

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0+-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.8+-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)
![Commits](https://img.shields.io/badge/Commits-461-blue?style=for-the-badge)

**RESTful API for garment factory management with real-time production tracking**

[Features](#-completed-features) • [API Docs](#-api-endpoints) • [Setup](#-getting-started) • [Architecture](#-architecture)

**Total Commits:** 461  
**Project Lead:** Prem Sai Bollamoni  
**Tracking Email:** premsai200804@gmail.com

---

## ✅ Completed Features

### 🔐 Authentication & Authorization
- [x] Employee authentication with role-based access control (RBAC)
- [x] 5 roles: Supervisor, Operator, HR, GM, Process Planner
- [x] Role-activity mapping system (comma-separated activities)
- [x] Secure password hashing
- [x] Session management

### 👨‍💼 HR & Administration
- [x] Employee CRUD operations
- [x] Role management with activity assignment
- [x] Attendance tracking (check-in/out with timestamps)
- [x] Employee profile management
- [x] Shift & break management
- [x] Auto-checkout of all checked-in employees on "Free All QRs"
- [x] QR mapping lifecycle (auto-freed after checkout)

### 🏭 Operations Management
- [x] Operation master data (27 Sub Assembly + 7 Full Garment = 34 total)
- [x] SAM (Standard Allowed Minutes) per operation
- [x] Skill grade assignment (A+-grade, A-grade, B-grade, C-grade, Helper)
- [x] Target pieces per shift
- [x] Sequential operation support
- [x] Full CRUD for operations
- [x] Auto operation code generation from name

### 🎯 Work Assignment & Tracking
- [x] Station management (workstation)
  - Create/update/delete stations
  - Link stations to operations
  - Machine code assignment (optional)
- [x] Tray management (reusable physical trays)
  - Tray creation on first scan
  - Tray status tracking (FREE/ASSIGNED)
  - Tray auto-creation with `orElseGet()` pattern
  - Auto-freed on job completion
- [x] Job assignment workflow
  - QR-based tray scanning
  - Automatic quantity input via dialog
  - Job creation with SAM-based estimates
  - Job completion tracking
  - Elapsed time calculation
- [x] Real-time job status
  - ON TRACK, OVERDUE, COMPLETED states
  - Live elapsed time tracking via Stream.periodic
  - Performance metrics (SAM vs actual)

### 📊 Reporting & Analytics
- [x] Attendance records with daily reports
- [x] Job history and completion tracking
- [x] Efficiency calculations (SAM vs actual time)
- [x] Employee performance metrics
- [x] Operation utilization

### 📥 Data Import
- [x] Excel import for operations
- [x] Smart header detection (finds headers anywhere in sheet)
- [x] Batch operation creation/update
- [x] Conflict resolution (operation exists = update)
- [x] Apache POI integration for robust parsing
- [x] Automatic tray and station creation

### 🔄 Database Features
- [x] JPA/Hibernate ORM
- [x] Transaction management (@Transactional)
- [x] Foreign key constraints
- [x] Automatic timestamp management (@CreationTimestamp)
- [x] Null-safe operations

---

## 🚀 Coming Soon

### 📱 Operator Module
- [ ] Personal job dashboard
- [ ] Efficiency tracking
- [ ] Performance analytics
- [ ] Work history

### 📈 Advanced Analytics
- [ ] Daily stock position board
- [ ] Bottleneck detection algorithm
- [ ] Pacemaker registry
- [ ] Production loss & shift risk scoring
- [ ] 12 standard PALMS reports
- [ ] Predictive analytics

### 🏪 Stock Management
- [ ] WIP (Work In Progress) tracking
- [ ] Stock position management
- [ ] Inventory reports
- [ ] Material allocation

### ✅ Quality Control
- [ ] QC inspector role
- [ ] Defect logging
- [ ] Quality reports
- [ ] Rework tracking

### 🎛️ Advanced Features
- [ ] Shift-wise efficiency tracking
- [ ] Concurrent operation support
- [ ] Dynamic line balancing
- [ ] SAM study time tracking
- [ ] Continuous improvement workflows

---

## 🏗️ Architecture

### Project Structure
```
backend/
├── src/main/java/com/cutm/smo/
│   ├── models/          # JPA entities
│   ├── repositories/    # Data access layer
│   ├── services/        # Business logic
│   ├── controller/      # REST endpoints
│   ├── dto/            # Data transfer objects
│   └── config/         # Spring configuration
├── src/main/resources/
│   ├── application.yml  # Configuration
│   └── db/changelog/    # Database migrations (future)
└── pom.xml            # Maven configuration
```

### Technology Stack
- **Framework:** Spring Boot 3.0+
- **Database:** MySQL 8.0
- **ORM:** JPA/Hibernate
- **Excel:** Apache POI 5.3.0
- **Build:** Maven 3.8+
- **Language:** Java 17+

### Data Flow
```
REST Controller → Service Layer → Repository → Database
         ↓
     Request/Response DTOs
         ↓
    JPA Entities
```

---

## 🗄️ Database Schema

### Core Tables

#### `employee` (1000+ rows capacity)
- emp_id (VARCHAR 20, PRIMARY KEY)
- emp_name, email, phone, address, dob
- blood_group, aadhar, pan, salary
- status, created_at

#### `role` (5 rows)
- role_id (INT, PRIMARY KEY)
- role_name (VARCHAR 50, UNIQUE)
- activities (VARCHAR 500 - comma-separated)
- status

#### `employee_role` (M:M linking)
- emp_id, role_id

#### `operation` (34 rows - optimized)
- op_id (BIGINT, PRIMARY KEY)
- op_code (VARCHAR 20, UNIQUE)
- op_name (VARCHAR 150)
- stage (VARCHAR 30) - formerly "zone"
- sequence_no (INT)
- sam (DECIMAL 8,3) - minutes per piece
- skill_grade (VARCHAR 20) - A+-grade, A-grade, etc.
- target_pcs (INT) - target pieces per shift
- status (ACTIVE/INACTIVE)
- created_at

#### `workstation` (50+ rows)
- ws_id (BIGINT, PRIMARY KEY)
- ws_code (VARCHAR 20, UNIQUE)
- op_id (FK to operation)
- machine_code (VARCHAR 30, UNIQUE, nullable)
- status

#### `job_assignment` (live jobs + history)
- job_id (BIGINT, PRIMARY KEY)
- ws_id (FK to workstation)
- emp_id (FK to employee)
- tray_id (FK to tray)
- op_id (FK to operation)
- tray_number (VARCHAR 50)
- bundleQty (INT) - quantity in tray
- status (IN_PROGRESS/COMPLETED)
- start_time, end_time
- assigned_by, unassigned_by
- created_at

#### `tray` (physical reusable trays)
- tray_id (BIGINT, PRIMARY KEY)
- tray_number (VARCHAR 50, UNIQUE)
- status (FREE/ASSIGNED)
- assigned_to (FK to employee)
- assigned_by, unassigned_by
- assigned_at, unassigned_at
- created_at

#### `attendance` (daily records)
- att_id (BIGINT, PRIMARY KEY)
- emp_id (FK to employee)
- temp_qr_token (VARCHAR 100)
- att_date (DATE)
- check_in, check_out (DATETIME)
- status (CHECKED_IN/CHECKED_OUT)
- machine_code (VARCHAR 30)
- marked_by (BIGINT)

#### `temp_qr_mapping` (daily mapping)
- mapping_id (BIGINT, PRIMARY KEY)
- qr_token (VARCHAR 100)
- emp_id (FK to employee)
- mapping_date (DATE)
- freed (BOOLEAN)
- mapped_by (BIGINT)

---

## 📡 API Endpoints

### Authentication
```
POST   /api/auth/login                    # Employee login
POST   /api/auth/logout                   # Logout
GET    /api/auth/verify                   # Verify token
```

### Employees
```
GET    /api/hr/employees                  # List employees
POST   /api/hr/employees                  # Create employee
PUT    /api/hr/employees/{empId}          # Update employee
DELETE /api/hr/employees/{empId}          # Delete employee
```

### Operations
```
GET    /api/hr/operations                 # List active operations
POST   /api/hr/operations                 # Create operation
PATCH  /api/hr/operations/{opId}/sam      # Update SAM/target/skill
DELETE /api/hr/operations/{opId}          # Deactivate operation
POST   /api/hr/import/upload              # Import operations from Excel
```

### Stations
```
GET    /api/hr/workstations               # List stations
POST   /api/hr/workstations               # Create station
PATCH  /api/hr/workstations/{wsId}        # Update station
DELETE /api/hr/workstations/{wsId}        # Delete station
PATCH  /api/hr/workstations/{wsId}/assign-operation
```

### Jobs
```
GET    /api/supervisor/jobs               # List all jobs
GET    /api/supervisor/jobs/active/{wsId} # Active jobs at station
POST   /api/supervisor/jobs/scan          # Scan tray + create/complete job
```

### Attendance
```
GET    /api/attendance/today              # Today's attendance
POST   /api/attendance/check-in           # Manual check-in
POST   /api/attendance/check-out          # Manual check-out
GET    /api/attendance/resolve-qr         # Resolve employee from QR
POST   /api/attendance/free-all-qrs       # End-of-day free all + auto-checkout
```

---

## 🛠️ Development

### Build
```bash
mvn clean package
```

### Run
```bash
java -jar target/smo-backend-1.0.0.jar
```

### Database Setup
```sql
-- Create database
CREATE DATABASE PALMSV1;

-- Create tables (via Spring Boot JPA auto-ddl)
-- Or use SQL migration scripts in db/changelog/
```

### Configuration
Edit `application.yml`:
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/PALMSV1
    username: root
    password: YourPassword
  jpa:
    hibernate:
      ddl-auto: update

cors:
  allowed-origins: http://localhost:3000,http://your-frontend-url
```

### Key Dependencies
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- mysql-connector-java 8.0+
- poi-ooxml 5.3.0 (Excel)
- lombok

---

## 🔑 Key Concepts

### QR Workflow
1. **Daily Mapping:** Admin maps QR codes to employees for the day
2. **Check-in:** Employee scans EMP-TEMP-XXX → attendance record created
3. **Job Scan:** Employee scans TRAY-XXX + qty → job assigned
4. **Completion:** Employee scans TRAY-XXX again → job completed, tray freed
5. **End-of-Day:** Supervisor clicks "Free All QRs" → auto-checkouts active employees

### Null Safety
- TempQrMapping.freed defaults to false
- Null checks: `j.getTray() != null ? j.getTray().getTrayNumber() : "UNKNOWN"`
- Optional pattern for repository queries

### Excel Import
- Uses Apache POI (works with .xlsx and .xls)
- Smart header detection (scans all rows, stops at first match)
- Auto-creates operations + stations + trays in transaction
- Conflict resolution: update if exists, create if new

---

## 📞 Support & Contact

**Project Lead:** Prem Sai Bollamoni  
**Email:** premsai200804@gmail.com  
**GitHub:** [@PremSaiBollamoni](https://github.com/PremSaiBollamoni)

**Total Commits:** 461

---

## 📄 License

Proprietary - All rights reserved

---

**Last Updated:** June 19, 2026  
**API Version:** 1.0.0  
**Database Version:** PALMSV1  
**Maintained by:** Prem Sai Bollamoni
