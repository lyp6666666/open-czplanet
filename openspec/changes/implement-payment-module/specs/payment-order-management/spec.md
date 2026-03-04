## ADDED Requirements

### Requirement: Create Payment Order
The system SHALL create a payment order record before interacting with a payment provider.

#### Scenario: New Order Creation
- **WHEN** a user initiates a payment for a tutor application
- **THEN** the system creates a payment_order record with status PENDING, linked to the application ID

### Requirement: Update Payment Order Status
The system SHALL update the payment order status based on provider callbacks or manual queries.

#### Scenario: Payment Success
- **WHEN** a successful payment notification is received
- **THEN** the payment order status updates to PAID

#### Scenario: Payment Failure
- **WHEN** a payment failure notification is received
- **THEN** the payment order status updates to FAILED

### Requirement: Query Payment Order
The system SHALL allow querying the status of a payment order.

#### Scenario: Check Order Status
- **WHEN** a client or backend service queries an order by ID
- **THEN** the system returns the current status (PENDING, PAID, FAILED)
