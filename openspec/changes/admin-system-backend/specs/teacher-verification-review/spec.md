## ADDED Requirements

### Requirement: List Pending Teacher Verifications
The system SHALL provide an API to list teachers with `realname_verify_status=1` (pending) or `edu_verify_status=1` (pending).

#### Scenario: View Pending Verifications
- **WHEN** an authenticated admin requests the pending verification list
- **THEN** the system returns a paginated list of teachers waiting for verification

### Requirement: View Verification Details
The system SHALL provide an API to retrieve the detailed verification proofs (images, IDs) for a specific teacher.

#### Scenario: View Proofs
- **WHEN** an authenticated admin requests verification details for a teacher
- **THEN** the system returns the URLs for ID cards, diplomas, and other submitted proofs

### Requirement: Approve Teacher Verification
The system SHALL provide an API to approve a teacher's verification request, updating the status to `2` (verified).

#### Scenario: Approve Verification
- **WHEN** an authenticated admin approves a verification request
- **THEN** the system updates the verification status to verified and returns a success response

### Requirement: Reject Teacher Verification
The system SHALL provide an API to reject a teacher's verification request, updating the status to `3` (rejected) and recording a reason.

#### Scenario: Reject Verification
- **WHEN** an authenticated admin rejects a verification request with a reason
- **THEN** the system updates the verification status to rejected, saves the rejection reason, and returns a success response
