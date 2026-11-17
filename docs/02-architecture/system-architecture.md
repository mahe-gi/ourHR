# System Architecture Specification

## 1. High-Level Architecture
ourHR is implemented as a Modular Monolith backend in Java Spring Boot paired with decoupled frontend clients.

```
+-------------------------------------------------------------+
|                        Client Layer                         |
|   [ React Vite Web ]     [ React Native ]   [ Next.js Page ]|
+------------------------------+------------------------------+
                               | HTTPS / REST (JSON)
+------------------------------v------------------------------+
|                   Spring Boot Application                   |
|  +-------------------------------------------------------+  |
|  | Security Filter Chain (JWT Auth + RBAC + Tenant Filter)|  |
|  +---------------------------+---------------------------+  |
|                              |                              |
|  +---------------------------v---------------------------+  |
|  | Modular Domain Services                               |  |
|  | - CompanyService           - EmployeeService          |  |
|  | - AttendanceService        - LeaveService             |  |
|  | - PayrollService           - StatutoryComplianceEngine|  |
|  | - ExporterService (ECR/CSV)- CAAuditService           |  |
|  +---------------------------+---------------------------+  |
|                              | Spring Data JPA              |
|  +---------------------------v---------------------------+  |
|  | PostgreSQL (Multi-tenant schema via @TenantId)        |  |
|  +-------------------------------------------------------+  |
+-------------------------------------------------------------+
                               | S3 API
+------------------------------v------------------------------+
|           S3-Compatible Storage (R2 / MinIO / S3)           |
+-------------------------------------------------------------+
```

## 2. Component Boundaries
- Web & Mobile Clients: Presentation and local validation only; all business calculations executed by backend.
- Spring MVC Controllers: Contract-first REST API endpoints accepting validated DTOs.
- Domain Services: Business rules (50% basic validator, leave ledger math, 6-step payroll state machine).
- S3 Storage Adapter: Secure abstraction for document and image uploads, returning pre-signed or secure URLs.
