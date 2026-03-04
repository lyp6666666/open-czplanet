## ADDED Requirements

### Requirement: List Refund Disputes
The system SHALL provide an API to list `brokerage_order` records with `status='DISPUTE'` or equivalent pending refund status.

#### Scenario: View Pending Refunds
- **WHEN** an authenticated admin requests the refund dispute list
- **THEN** the system returns a paginated list of orders in dispute

### Requirement: View Dispute Details with Chat
The system SHALL provide an API to retrieve the details of a disputed order along with the associated chat history between the parent and the tutor.

#### Scenario: View Dispute Context
- **WHEN** an authenticated admin requests details for a disputed order
- **THEN** the system returns the order details and a chronological list of chat messages from the associated room

### Requirement: Approve Refund
The system SHALL provide an API to approve a refund, triggering the refund process (e.g., via WeChat Pay) and updating the order status to `REFUNDED`.

#### Scenario: Approve Refund
- **WHEN** an authenticated admin approves a refund
- **THEN** the system initiates the refund logic, updates the order status, and returns a success response

### Requirement: Reject Refund
The system SHALL provide an API to reject a refund, updating the order status to `PAID` (or equivalent final state) and recording a reason.

#### Scenario: Reject Refund
- **WHEN** an authenticated admin rejects a refund request with a reason
- **THEN** the system updates the order status to finalized, saves the rejection reason, and returns a success response
