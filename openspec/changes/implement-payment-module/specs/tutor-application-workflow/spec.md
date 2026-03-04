## ADDED Requirements

### Requirement: Application Payment Check
The system SHALL require payment completion before allowing chat access for a tutor application, unless bypassed by configuration.

#### Scenario: Payment Required
- **WHEN** a user attempts to chat for an application in PENDING_PAYMENT status
- **THEN** the system denies access and prompts for payment

#### Scenario: Payment Bypassed
- **WHEN** the configuration `tutor-application.skip-payment-check` is true
- **THEN** the system allows chat access regardless of payment status

### Requirement: Unlock Chat Access
The system SHALL update the application status to allow chat upon successful payment.

#### Scenario: Successful Payment
- **WHEN** a payment order linked to an application transitions to PAID
- **THEN** the application status updates to allow chat (e.g., `chat_access_status` = `ALLOWED`)
