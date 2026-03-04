## Why

The current platform lacks a dedicated administrative interface for operations staff to manage user content, verify teacher credentials, and handle financial disputes. This necessitates manual database operations or inefficient workflows. A centralized admin system is needed to streamline operational tasks, improve response times for user verification and refunds, and ensure content quality through systematic approval processes.

## What Changes

- **New Backend Module**: Introduce `ai-tutor-admin` as a separate Maven module within the existing project, sharing the database with existing services.
- **Admin Authentication**: Implement a secure, independent authentication system for administrators using username/password (no mobile verification required for admins).
- **Dashboard**: Create a system overview dashboard displaying key metrics like total users, active teachers, and pending tasks.
- **Job Approval Workflow**: Implement a review process specifically for student job postings (`student_job_posting`). *Note: Teacher service approval is explicitly excluded for now.*
- **Teacher Verification**: Enhance the teacher verification process to support manual CHSI (学信网) validation, allowing admins to view submitted credentials and approve/reject with reasons.
- **Refund Management**: detailed refund approval workflow that allows admins to view the chat history between the parent and tutor associated with the disputed order to make informed decisions.

## Capabilities

### New Capabilities
- `admin-auth`: Admin user management, role-based access control, and username/password login.
- `admin-dashboard`: Aggregated system statistics and operational overview.
- `student-job-approval`: Workflow for reviewing, approving, or rejecting student job postings.
- `teacher-verification-review`: Interface for admins to review teacher identity and education proofs, including CHSI verification support.
- `refund-dispute-resolution`: Workflow for handling refund requests, integrated with chat history retrieval for context.

### Modified Capabilities
- `teacher-verification`: Update existing verification status flow to accommodate admin review actions (if not already fully supported).
- `brokerage-order`: Update order status flow to support admin intervention in refund disputes.

## Impact

- **Codebase**: Addition of a new `ai-tutor-admin` module.
- **Database**:
  - New tables for admin users (`sys_admin_user`) and roles (`sys_role`).
  - Potential updates to `teacher_profile` and `brokerage_order` status state machines if current states are insufficient.
- **Security**: Strict separation of admin API endpoints (`/api/admin/**`) with distinct authentication logic.
- **Deployment**: The admin backend will be deployed as a separate service (or on a different port) alongside the existing microservices.
