## ADDED Requirements

### Requirement: List Pending Student Jobs
The system SHALL provide an API to list `student_job_posting` records with `status=0` (pending approval).

#### Scenario: View Pending Jobs
- **WHEN** an authenticated admin requests the pending jobs list
- **THEN** the system returns a paginated list of student job postings waiting for approval

### Requirement: Approve Student Job
The system SHALL provide an API to approve a student job posting, changing its status to `1` (published).

#### Scenario: Approve Job
- **WHEN** an authenticated admin approves a pending job posting
- **THEN** the system updates the job status to published and returns a success response

### Requirement: Reject Student Job
The system SHALL provide an API to reject a student job posting, changing its status to `2` (rejected) and recording a rejection reason.

#### Scenario: Reject Job
- **WHEN** an authenticated admin rejects a pending job posting with a reason
- **THEN** the system updates the job status to rejected, saves the rejection reason, and returns a success response
