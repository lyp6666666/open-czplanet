## MODIFIED Requirements

### Requirement: Create Payment Order
The system SHALL create a `payment_order` record before interacting with a payment provider, and the record SHALL be the single source of truth for payment status.

#### Scenario: New Order Creation
- **WHEN** a user initiates a scan-code payment for a valid business order
- **THEN** the system SHALL create (or idempotently reuse) a `payment_order` record with status `PENDING`, linked to the business order via `contextType/contextId`

#### Scenario: Provider information is persisted
- **WHEN** the system successfully creates a provider order via YunGouOS
- **THEN** the system SHALL persist provider identifiers (e.g., provider orderNo / transactionId if available), payment data (QR url / code url), and expiration time

### Requirement: Update Payment Order Status
The system SHALL update the payment order status based on provider callbacks or provider-confirmed queries, and SHALL enforce an idempotent forward-only state machine.

#### Scenario: Payment Success
- **WHEN** a successful YunGouOS payment notification is received with valid signature and matching amount/channel/orderNo
- **THEN** the payment order status updates to `SUCCESS` and records `successTime` and provider transaction identifiers

#### Scenario: Duplicate success notification is idempotent
- **WHEN** the system receives a duplicate successful notification for an already `SUCCESS` order
- **THEN** the system SHALL NOT change the order state and SHALL NOT emit duplicate payment-success events

#### Scenario: Payment Closed or Expired
- **WHEN** a payment order passes its expiration time without success (or is explicitly closed)
- **THEN** the payment order status updates to `CLOSED` and becomes non-payable

### Requirement: Query Payment Order
The system SHALL allow querying the status of a payment order for user polling and internal reconciliation.

#### Scenario: Check Order Status
- **WHEN** a client queries an order by `orderNo`
- **THEN** the system returns the current status (`PENDING`, `SUCCESS`, `FAILED`, `CLOSED`) and key timestamps (created/expire/success if present)
