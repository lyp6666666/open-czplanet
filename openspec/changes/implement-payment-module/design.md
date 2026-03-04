<!-- Use this as the structure for your output file. Fill in the sections. -->
## Context

The current system facilitates teacher-student matching and communication but lacks a monetization layer. To enable payments for services (specifically unlocking chat capabilities), we need to integrate third-party payment providers (Alipay and WeChat Pay). This requires a secure, reliable backend service to handle order creation, payment notifications, and status synchronization.

## Goals / Non-Goals

**Goals:**
- **Dedicated Payment Service**: Create `payment-service` to encapsulate all payment logic.
- **Alipay Integration**: Support Alipay App/Wap payment flow.
- **WeChat Pay Integration**: Support WeChat Pay JSAPI/Native flow.
- **Secure Callbacks**: Implement robust handling of asynchronous payment notifications with signature verification.
- **Status Synchronization**: Automatically update `tutor_application` status upon successful payment to unlock chat.
- **Testability**: Implement a configuration switch to bypass payment for testing environments.

**Non-Goals:**
- **Refunds**: Automated refund processing is out of scope for this initial implementation (can be manual or added later).
- **Subscription Models**: Focusing on one-time payments for now.

## Decisions

### 1. Architecture
- **Decision**: Create a new Maven module `payment-service`.
- **Rationale**: Decouples payment logic from business logic (`tutor-appointment-service`) and communication logic (`videoCall-IM-service`). Allows for independent scaling and maintenance.

### 2. Database Schema
- **Decision**: Create a new `payment_order` table.
- **Rationale**: Instead of just adding columns to `tutor_application`, a dedicated table allows tracking multiple payment attempts, transaction IDs from providers, amounts, and detailed status logs. It will link to `tutor_application` via `application_id` (or `context_id`).
- **Schema Draft**:
  - `id`: PK
  - `order_no`: Unique order number for internal tracking (sent to provider).
  - `user_id`: Payer ID.
  - `amount`: Payment amount.
  - `channel`: `ALIPAY`, `WECHAT`.
  - `status`: `PENDING`, `SUCCESS`, `FAILED`.
  - `transaction_id`: Provider's transaction ID.
  - `context_id`: ID of the business object (e.g., `tutor_application.id`).
  - `context_type`: Type of business object.

### 3. Payment Flow
- **Decision**: Backend-driven order creation.
- **Flow**:
  1. Client requests payment for an application.
  2. Backend creates `payment_order` (PENDING) and calls Provider API to get order string/prepay ID.
  3. Backend returns info to Client.
  4. Client invokes Provider SDK.
  5. Provider notifies Backend via Webhook (Async Notify).
  6. Backend verifies signature, updates `payment_order` to SUCCESS, and updates `tutor_application` status.

### 4. Test Switch
- **Decision**: Use a Spring configuration property `payment.enabled` (or reuse `tutor-application.skip-payment-check`).
- **Rationale**: Allows developers and QA to test the application flow without making real payments. Logic will check this property before enforcing payment requirements.

## Risks / Trade-offs

- **Risk**: Payment callback not received due to network issues.
  - **Mitigation**: Implement a scheduled task (e.g., every 5 minutes) to query payment status from the provider for PENDING orders older than X minutes.
- **Risk**: Double payment or duplicate callbacks.
  - **Mitigation**: Idempotency checks in the callback handler based on `order_no`.
