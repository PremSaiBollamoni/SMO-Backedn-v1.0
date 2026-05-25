# 🏭 SMO - Smart Manufacturing Operations (Backend)

<div align="center">

![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Hibernate](https://img.shields.io/badge/Hibernate-JPA-59666C?style=for-the-badge&logo=hibernate&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)
![Version](https://img.shields.io/badge/version-1.0.0-blue?style=for-the-badge)

**Enterprise-grade REST API for intelligent garment manufacturing workflow management**

[Features](#-features) • [Architecture](#-architecture) • [Getting Started](#-getting-started) • [API Documentation](#-api-documentation) • [Deployment](#-deployment) • [Download](#-download-war)

---

## 📦 Download WAR

**Latest Release: v1.0.0 (May 6, 2026)**

[![Download WAR](https://img.shields.io/badge/Download-Backend--WAR-success?style=for-the-badge&logo=apache)](https://github.com/PremSaiBollamoni/SMO/raw/main/SMO-Backend-v1.0.0.war)

**Direct Download Link:**
```
https://github.com/PremSaiBollamoni/SMO/raw/main/SMO-Backend-v1.0.0.war
```

**File Size:** 55.06 MB  
**Deployment:** Apache Tomcat 10.1+  
**Java Version:** 17+

**Deployment:**
1. Download the WAR file
2. Copy to Tomcat: `/opt/tomcat/webapps/smo.war`
3. Configure database in environment variables
4. Restart Tomcat
5. Access at: `https://your-domain.com/smo`

</div>

---

## 📋 Overview

SMO Backend is a robust, scalable Spring Boot application that powers a comprehensive Smart Manufacturing Operations system for the garment industry. It provides real-time workflow orchestration, process planning, quality control, and production tracking with advanced features like parallel workflow execution and intelligent bin merging.

### 🎯 Key Highlights

- **🔄 Dynamic Workflow Engine** - DAG-based process routing with parallel branch execution and merge points
- **📊 Real-time Analytics** - Node metrics, operator performance tracking, and production insights
- **🏷️ QR-based Tracking** - Complete garment lifecycle traceability from cutting to packaging
- **🔐 Role-based Access Control** - Multi-role authentication (HR, GM, Supervisor, Process Planner, Operator)
- **⚡ High Performance** - Optimized JPA queries, connection pooling, and async processing
- **🎨 Clean Architecture** - Layered design with clear separation of concerns

---

## ✨ Features

### 🏗️ Core Modules

#### 1. **Process Planning & Workflow Management**
- Create and manage manufacturing process plans with approval workflows
- Define operations with semantic types: `SEQUENTIAL`, `PARALLEL_BRANCH`, `MERGE`
- Explicit edge-based workflow graph generation
- Clone and version process plans
- Real-time workflow visualization support

#### 2. **Production Tracking & Monitoring**
- QR code-based garment tracking across all operations
- Bin assignment and merging with validation
- WIP (Work in Progress) tracking
- Operation-level progress monitoring
- Automated bin history logging

#### 3. **Quality Control**
- Multi-stage QC checkpoints
- Defect tracking and reporting
- Final inspection workflows
- QC metrics and analytics

#### 4. **HR & Employee Management**
- Employee CRUD operations with role assignment
- Login credential management
- Role-based permissions (HR/Admin, Supervisor, GM, Process Planner, Operator)
- Employee performance tracking

#### 5. **Inventory & Store Management**
- Item and vendor management
- Purchase orders and GRN (Goods Receipt Note)
- Inventory stock tracking with movement history
- BOM (Bill of Materials) management
- Stock level alerts

#### 6. **Supervisor Operations**
- QR assignment to operators
- Work tracking and validation
- Bin merging operations
- Operator performance insights
- Work reassignment capabilities

#### 7. **Insights & Analytics**
- Dashboard metrics for all roles
- Supervisor floor insights
- Node-level performance metrics
- Production bottleneck identification
- Real-time KPI tracking
- GM-specific insights (pending/approved process plans)

#### 8. **Order Management System** ⭐ NEW
- Create and manage production orders with target quantities
- Link orders to products and process plans (routings)
- Order status tracking (DRAFT, ACTIVE, ON_HOLD, COMPLETED, CANCELLED)
- Bin-to-order linkage via foreign key relationship
- Real-time progress calculation from WIP tracking
- Strategic monitoring for GM oversight
- Order activation workflow

#### 9. **Service Discovery & Auto-Configuration**
- mDNS/Bonjour service discovery
- Automatic backend detection on local networks
- Network scanning fallback mechanism
- Health check endpoints
- Service information endpoints
- Support for any local network (192.168.x.x, 10.x.x.x, 172.x.x.x)

#### 10. **Interactive Workflow Monitoring**
- Clickable workflow nodes for real-time production insights
- Role-based node access control (GM and Supervisor only)
- Live metrics: WIP counts, active jobs, daily completion stats
- Node metrics endpoint with auto-refresh capabilities
- Strategic monitoring for GM, floor-level monitoring for Supervisor

#### 11. **Enhanced Workflow Progression System** ⭐ NEW
- Automatic routing progression through operations
- `current_operation_id` tracking in bin table
- Sequential operation validation and advancement
- Last operation detection for workflow completion
- Bin status management (NEW → ASSIGNED → ACTIVE → COMPLETED → FREE)
- WIP tracking with proper FK population (bin_id, operation_id)
- Merge operation with source bin reset to FREE status

#### 12. **QR Event Audit Trail** ⭐ NEW
- Complete QR scanning event logging
- Audit trail for all QR operations (ASSIGNMENT, TRACKING, MERGE)
- Event types: ASSIGNMENT, TRACKING, MERGE_SOURCE, MERGE_TARGET
- Captures: QR code, entity type, entity ID, operation ID, operator ID, timestamp
- Non-blocking event logging (failures don't break main operations)
- Historical event analysis and compliance reporting

#### 13. **Master Data Management System** ⭐ NEW
- Centralized management for foundational production data
- **Styles** - Garment style definitions with concept, labels, and patterns
- **GTG (Garment-to-Go)** - Style variants with size, color, sleeve type configurations
- **Buttons** - Button type catalog with codes and names
- **Labels** - Label inventory (woven, printed, care labels)
- **Machines** - Machine registry with types and status tracking
- **Threads** - Thread catalog with color codes
- Full CRUD operations for all master data entities
- Active/Inactive status management
- Dropdown population for dependent entities (GTG uses Styles, Buttons, Threads)

---

## 🏛️ Architecture

### Technology Stack

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│  REST Controllers • DTOs • Request/Response Models           │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      Business Layer                          │
│  Services • Validators • Business Logic • Workflow Engine    │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    Data Access Layer                         │
│  JPA Repositories • Entity Models • Database Mappings        │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                       Database Layer                         │
│              MySQL 8.0 • Relational Schema                   │
└─────────────────────────────────────────────────────────────┘
```

### Key Design Patterns

- **Repository Pattern** - Clean data access abstraction
- **DTO Pattern** - Decoupled API contracts from domain models
- **Service Layer Pattern** - Centralized business logic
- **Dependency Injection** - Loose coupling via Spring IoC
- **Builder Pattern** - Complex object construction (WorkflowEdge, Responses)

### Database Schema Highlights

- **20+ Core Tables** - Normalized relational design
- **Foreign Key Constraints** - Referential integrity
- **Indexed Columns** - Optimized query performance
- **Audit Fields** - Timestamps for tracking
- **Enum Types** - Type-safe operation classifications
- **Workflow Progression** - current_operation_id tracking in bin table
- **QR Event Logging** - Complete audit trail for all QR operations

---

## 🚀 Getting Started

### Prerequisites

```bash
Java 17+
Maven 3.8+
MySQL 8.0+
```

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/PremSaiBollamoni/SMO.git
cd SMO
```

2. **Configure database**
```bash
# Create MySQL database
mysql -u root -p
CREATE DATABASE smo;
```

3. **Update application properties**
```properties
# src/main/resources/application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/smo
spring.datasource.username=root
spring.datasource.password=your_password
```

4. **Build the project**
```bash
mvn clean install
```

5. **Run the application**
```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

### Docker Deployment

```bash
# Build Docker image
docker build -t smo-backend .

# Run container
docker run -p 8080:8080 \
  -e DB_URL=jdbc:mysql://host.docker.internal:3306/smo \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=your_password \
  smo-backend
```

---

## 📚 API Documentation

### Base URL
```
http://localhost:8080/api
```

### Authentication
```http
POST /api/auth/login
Content-Type: application/json

{
  "loginid": "string",
  "password": "string"
}
```

### Key Endpoints

#### Process Planning
```http
GET    /api/processplan/pending              # Get pending approvals
GET    /api/processplan/{routingId}          # Get process plan details
POST   /api/processplan/draft                # Create draft process plan
POST   /api/processplan/{id}/approve         # Approve process plan
POST   /api/processplan/{id}/reject          # Reject process plan
```

#### Production Operations
```http
GET    /api/production/operations             # List all operations
POST   /api/production/operations             # Create operation
GET    /api/production/routings               # List routings
POST   /api/production/routings               # Create routing
GET    /api/production/bundles                # List bundles
POST   /api/production/bundles                # Create bundle
```

#### Supervisor Operations
```http
POST   /api/supervisor/qr-assignment          # Assign QR to bin with order linkage
POST   /api/supervisor/tracking               # Track work progress
POST   /api/supervisor/merging                # Merge bins
GET    /api/supervisor/process-plans          # Get process plans
GET    /api/supervisor/operations/{routingId} # Get operations for routing
```

#### Order Management ⭐ NEW
```http
GET    /api/orders/active                     # Get active orders
GET    /api/orders/status/{orderNumber}       # Get order status and progress
POST   /api/orders                            # Create new order
POST   /api/orders/{orderId}/activate         # Activate order
```

#### HR Management
```http
GET    /api/hr/employees                      # List employees
POST   /api/hr/employees                      # Create employee
PUT    /api/hr/employees/{id}                 # Update employee
DELETE /api/hr/employees/{id}                 # Delete employee
GET    /api/hr/roles                          # List roles
POST   /api/hr/roles                          # Create role
```

#### Insights & Analytics
```http
GET    /api/insights/dashboard                # Dashboard metrics
GET    /api/insights/supervisor               # Supervisor insights
GET    /api/insights/gm                       # GM insights (pending/approved plans)
```

#### Service Discovery
```http
GET    /api/health                            # Health check endpoint
GET    /api/discovery/info                    # Service discovery info
GET    /api/discovery/ping                    # Service discovery ping
```

#### Node Metrics & Monitoring
```http
GET    /api/processplan/node-metrics          # Real-time node metrics
       ?routingId={id}&operationId={id}&actorEmpId={id}
       # Returns: WIP count, active jobs, daily completions
```

#### QR Event Audit Trail ⭐ NEW
```http
GET    /api/qr_event                          # Get all QR events
GET    /api/qr_event/{id}                     # Get specific QR event
POST   /api/qr_event                          # Create QR event (internal use)
       # Event types: ASSIGNMENT, TRACKING, MERGE_SOURCE, MERGE_TARGET
```

#### Master Data Management ⭐ NEW
```http
# Styles
GET    /api/gm/masterdata/styles              # Get all styles
GET    /api/gm/masterdata/styles/active       # Get active styles
POST   /api/gm/masterdata/styles              # Create style
PUT    /api/gm/masterdata/styles/{id}         # Update style
DELETE /api/gm/masterdata/styles/{id}         # Delete style

# GTG (Style Variants)
GET    /api/gm/masterdata/gtg                 # Get all GTGs
GET    /api/gm/masterdata/gtg/active          # Get active GTGs
POST   /api/gm/masterdata/gtg                 # Create GTG
PUT    /api/gm/masterdata/gtg/{id}            # Update GTG
DELETE /api/gm/masterdata/gtg/{id}            # Delete GTG

# Buttons
GET    /api/gm/masterdata/buttons             # Get all buttons
GET    /api/gm/masterdata/buttons/active      # Get active buttons
POST   /api/gm/masterdata/buttons             # Create button
PUT    /api/gm/masterdata/buttons/{id}        # Update button
DELETE /api/gm/masterdata/buttons/{id}        # Delete button

# Labels
GET    /api/gm/masterdata/labels              # Get all labels
POST   /api/gm/masterdata/labels              # Create label
PUT    /api/gm/masterdata/labels/{id}         # Update label
DELETE /api/gm/masterdata/labels/{id}         # Delete label

# Machines
GET    /api/gm/masterdata/machines            # Get all machines
GET    /api/gm/masterdata/machines/active     # Get active machines
POST   /api/gm/masterdata/machines            # Create machine
PUT    /api/gm/masterdata/machines/{id}       # Update machine
DELETE /api/gm/masterdata/machines/{id}       # Delete machine

# Threads
GET    /api/gm/masterdata/threads             # Get all threads
GET    /api/gm/masterdata/threads/active      # Get active threads
POST   /api/gm/masterdata/threads             # Create thread
PUT    /api/gm/masterdata/threads/{id}        # Update thread
DELETE /api/gm/masterdata/threads/{id}        # Delete thread
```

### Response Format

**Success Response:**
```json
{
  "status": "success",
  "data": { ... },
  "message": "Operation completed successfully"
}
```

**Error Response:**
```json
{
  "status": "error",
  "message": "Error description",
  "timestamp": "2026-04-25T10:30:00"
}
```

---

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_URL` | MySQL connection URL | `jdbc:mysql://localhost:3306/smo` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | - |
| `SMO_DATA_KEY` | AES encryption key (32 chars) | `SMO_DEFAULT_32_CHAR_SECRET_KEY_!` |
| `SERVER_PORT` | Application port | `8080` |

### Application Properties

```properties
# Server Configuration
server.port=8080
server.address=0.0.0.0

# Database Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Security
app.security.aes-key=${SMO_DATA_KEY}

# Service Discovery Configuration
smo.service.name=SMO-Backend
smo.service.version=1.0.0
app.base-url=http://localhost

# Production-Safe Logging
logging.level.com.cutm.smo=INFO
logging.level.org.springframework.web=WARN
logging.level.org.hibernate=WARN
```

---

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ProcessPlanServiceTest

# Generate coverage report
mvn jacoco:report
```

---

## 📦 Deployment

### Production Deployment (Tomcat)

The application is deployed as a WAR file to Apache Tomcat on the production server.

**Production URL:** `https://smobza.thegttech.com/smo`

#### Build WAR File

```bash
# Clean build and package
./mvnw clean package -DskipTests

# WAR file will be created at:
# target/smo-V0.1.war
```

#### Deploy to Tomcat

```bash
# 1. Copy WAR file to server
scp target/smo-V0.1.war user@smobza.thegttech.com:/opt/tomcat/webapps/smo.war

# 2. Restart Tomcat
sudo systemctl restart tomcat

# 3. Check deployment logs
tail -f /opt/tomcat/logs/catalina.out

# 4. Verify deployment
curl https://smobza.thegttech.com/smo/api/health
```

#### Production Configuration

**Server Details:**
- **URL:** https://smobza.thegttech.com/smo
- **Context Path:** /smo
- **Tomcat Version:** 10.1.41
- **Java Version:** 23.0.2 (compiled for Java 17)
- **Database:** MySQL 8.0 on localhost

**Environment Variables (Production):**
```bash
# Set in Tomcat's setenv.sh or systemd service file
export DB_URL=jdbc:mysql://localhost:3306/smo?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
export DB_USERNAME=root
export DB_PASSWORD=<production_password>
export SMO_DATA_KEY=<32_char_production_key>
```

#### Database Setup (Production)

```bash
# 1. Import database backup
mysql -u root -p smo < smo_backup.sql

# 2. Verify tables
mysql -u root -p -e "USE smo; SHOW TABLES;"

# 3. Check bin status
mysql -u root -p -e "USE smo; SELECT status, COUNT(*) FROM bin GROUP BY status;"
```

#### Production Testing

Run the comprehensive endpoint test script:

```powershell
# Windows PowerShell
./test_production_endpoints.ps1

# Expected output: 24/24 tests passing
```

**Critical Endpoints:**
- ✅ `/api/health` - Health check
- ✅ `/api/approved-orders` - Orders with active/assigned bins
- ✅ `/api/order-stats` - Order-specific statistics
- ✅ `/api/supervisor/*` - All supervisor operations
- ✅ `/api/gm/masterdata/*` - Master data management

### Render.com Deployment

The application includes a `render.yaml` configuration for one-click deployment:

```yaml
services:
  - type: web
    name: smo-backend
    env: java
    buildCommand: mvn clean package -DskipTests
    startCommand: java -jar target/smo-V0.1.war
```

### Production Checklist

- [x] Set strong `SMO_DATA_KEY` environment variable
- [x] Configure production database with proper credentials
- [x] Deploy WAR file to Tomcat server
- [x] Import production database from backup
- [x] Verify all 24 critical endpoints are working
- [x] Configure CORS for frontend domain
- [x] Test Strategic Monitor with order-specific stats
- [ ] Enable HTTPS/TLS (already configured)
- [ ] Set up database backups (scheduled)
- [ ] Configure logging and monitoring
- [ ] Set up CI/CD pipeline
- [ ] Enable rate limiting

**Production Status:** ✅ **LIVE** (Deployed May 6, 2026)

---

## 🏗️ Project Structure

```
src/main/java/com/cutm/smo/
├── config/              # Configuration classes
│   ├── CorsConfig.java
│   ├── DataInitializer.java
│   └── WebConfig.java
├── controller/          # REST Controllers
│   ├── AuthController.java
│   ├── ProcessPlanController.java
│   ├── ProductionController.java
│   ├── SupervisorController.java
│   ├── HrController.java
│   └── InsightsController.java
├── dto/                 # Data Transfer Objects
│   ├── ProcessPlanResponse.java
│   ├── WorkflowEdge.java
│   ├── NodeMetricsResponse.java
│   └── ...
├── models/              # JPA Entity Models
│   ├── Operation.java
│   ├── OperationType.java
│   ├── Routing.java
│   ├── RoutingStep.java
│   ├── Bin.java
│   └── ...
├── repositories/        # JPA Repositories
│   ├── OperationRepository.java
│   ├── RoutingRepository.java
│   └── ...
├── services/            # Business Logic Services
│   ├── ProcessPlanService.java
│   ├── SupervisorService.java
│   ├── HrService.java
│   ├── EnhancedQrAssignmentService.java
│   ├── EnhancedTrackingService.java
│   ├── EnhancedMergingService.java
│   ├── RoutingProgressionService.java
│   ├── QrEventService.java
│   └── ...
└── validation/          # Validators
    └── OperationValidator.java
```

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Code Style Guidelines

- Follow Java naming conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public methods
- Write unit tests for new features
- Keep methods focused and concise

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👥 Authors

**Prem Sai Bollamoni**
- GitHub: [@PremSaiBollamoni](https://github.com/PremSaiBollamoni)
- LinkedIn: [Prem Sai Bollamoni](https://linkedin.com/in/premsaibollamoni)

---

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Hibernate for robust ORM capabilities
- MySQL for reliable database management
- The open-source community for inspiration and tools

---

<div align="center">

**⭐ Star this repository if you find it helpful!**

Made with ❤️ for the garment manufacturing industry

</div>
