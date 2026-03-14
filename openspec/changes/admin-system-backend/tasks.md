## 1. Module Initialization

- [ ] 1.1 Create `ai-tutor-admin` module directory structure
- [ ] 1.2 Configure `pom.xml` for the new module (dependencies: web, mysql, mybatis, etc.) and add it to parent pom
- [ ] 1.3 Create `application.yml` with database and server configuration (port 8082)
- [ ] 1.4 Create main Spring Boot Application class

## 2. Database & Authentication

- [ ] 2.1 Create SQL migration script for `sys_admin_user` and `sys_role` tables
- [ ] 2.2 Create `SysAdminUser` entity and `SysAdminUserMapper`
- [ ] 2.3 Implement Admin Login API (`/api/admin/auth/login`) returning a token
- [ ] 2.4 Implement Authentication Interceptor/Filter to protect `/api/admin/**` endpoints
- [ ] 2.5 Add unit tests for admin authentication

## 3. Dashboard

- [ ] 3.1 Create `AdminDashboardController`
- [ ] 3.2 Implement service method to count total users and active teachers
- [ ] 3.3 Implement service method to count pending jobs, verifications, and refunds
- [ ] 3.4 Expose aggregated dashboard statistics API
- [ ] 3.5 Add unit tests for dashboard statistics

## 4. Student Job Approval

- [ ] 4.1 Create `AdminJobController`
- [ ] 4.2 Implement API to list pending `student_job_posting` (status=0)
- [ ] 4.3 Implement API to approve job posting (update status to 1)
- [ ] 4.4 Implement API to reject job posting (update status to 2, save reason)
- [ ] 4.5 Add unit tests for job approval workflow

## 5. Teacher Verification

- [ ] 5.1 Create `AdminVerificationController`
- [ ] 5.2 Implement API to list teachers with pending verification status
- [ ] 5.3 Implement API to get verification details (proof URLs)
- [ ] 5.4 Implement API to approve verification (update status to 2)
- [ ] 5.5 Implement API to reject verification (update status to 3, save reason)
- [ ] 5.6 Add unit tests for verification workflow

## 6. Refund & Chat History

- [ ] 6.1 Create `AdminRefundController`
- [ ] 6.2 Implement API to list `brokerage_order` with dispute status
- [ ] 6.3 Implement API to get dispute details including order info
- [ ] 6.4 Implement logic to query `message` table by `room_id` and format for display
- [ ] 6.5 Implement API to approve refund (update status, trigger refund logic)
- [ ] 6.6 Implement API to reject refund (update status to paid/final)
- [ ] 6.7 Add unit tests for refund and chat history retrieval
