## Context

The current AI Tutor platform consists of `tutor-appointment-service` and `videoCall-IM-service`, sharing a MySQL database. There is no dedicated backend for administrative tasks. Operations staff need tools to manage users, verify credentials, and resolve disputes. We will introduce a new backend module `ai-tutor-admin` to serve these needs.

## Goals / Non-Goals

**Goals:**
- **Dedicated Admin Backend**: Create `ai-tutor-admin` module to handle operational logic.
- **Secure Authentication**: Implement username/password login for admins, separate from user auth.
- **Job Approval**: Workflow for reviewing `student_job_posting` entries.
- **Verification**: Support manual CHSI verification for teachers.
- **Dispute Resolution**: Enable refund processing with access to chat history.
- **High Quality**: Adhere to enterprise standards with clear documentation, comments, and unit tests.

**Non-Goals:**
- **Teacher Service Approval**: Admin review of teacher service postings is out of scope.
- **Complex RBAC**: Initial implementation will support a single super-admin role or simple admin roles.
- **Mobile Auth**: Admin login will not use SMS verification.

## Decisions

### 1. Modular Architecture
- **Decision**: Create a new Maven module `ai-tutor-admin` within the existing project structure.
- **Rationale**: This allows direct reuse of existing Data Access Objects (DAOs) and Entities from `ai-tutor-common` or other modules, minimizing code duplication and avoiding the complexity of inter-service communication for simple CRUD operations.
- **Alternative**: Building a completely separate microservice would require duplicating data models or exposing internal APIs, increasing maintenance overhead.

### 2. Authentication Strategy
- **Decision**: Use a distinct authentication mechanism (e.g., Spring Security or Sa-Token) for admin users, stored in a new `sys_admin_user` table.
- **Rationale**: Admin security requirements (username/password, internal access) differ from the consumer app (mobile number/SMS, public access). Separation ensures security policies can be tuned independently.

### 3. Chat History Access
- **Decision**: The admin service will directly query the `message` table (shared DB) to retrieve chat history for refund disputes.
- **Rationale**: The data is already in the shared database. Direct query is the most efficient approach for this use case, avoiding the need to build a new data synchronization pipeline.

### 4. Code Reuse & Quality
- **Decision**: Extract shared entities and mappers into a common module if they are not already accessible. Enforce high code quality standards with comprehensive JavaDoc and JUnit tests.
- **Rationale**: To maintain maintainability and reliability as the system grows.

## Risks / Trade-offs

- **Risk**: Database coupling between `tutor-appointment-service` and `ai-tutor-admin`.
  - **Mitigation**: Strictly define ownership of data. Admin service should primarily be read-heavy or update specific status fields, avoiding complex business logic duplication.
- **Risk**: Performance impact on the shared database.
  - **Mitigation**: Admin queries (e.g., statistics) should be optimized and potentially use read replicas if available in the future. For now, data volume is manageable.
