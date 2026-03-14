## ADDED Requirements

### Requirement: View System Statistics
The system SHALL provide an API to retrieve aggregated system statistics, including total user count, active teacher count, and pending task counts.

#### Scenario: Retrieve Dashboard Stats
- **WHEN** an authenticated admin requests the dashboard statistics
- **THEN** the system returns a JSON object containing current counts for users, teachers, pending job approvals, pending verifications, and pending refunds
