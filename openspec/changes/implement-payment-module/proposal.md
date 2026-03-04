<!-- Use this as the structure for your output file. Fill in the sections. -->
## Why

To monetize the platform, we need to implement a payment system that requires users (specifically regarding teacher-student interactions) to pay before they can unlock chat functionality. This creates a closed loop for value exchange.

## What Changes

- **New Module**: Create a new Maven module `payment-service` to handle all payment-related logic, similar to `videoCall-IM-service`.
- **Integration**: Register the new module with `ai-tutor-starter` for unified startup.
- **Payment Gateways**:
  - Implement Alipay payment (referencing App Pay documentation as requested, adapting for the specific client/web flow).
  - Implement WeChat Pay (referencing JSAPI/Native documentation).
- **Payment Process**:
  - Generate payment orders/signatures.
  - Handle asynchronous callbacks (notify_url) from payment providers to ensure payment success.
  - Implement a mechanism to unlock chat functionality (`tutor_application` status update) upon successful payment.
- **Configuration**:
  - Add configuration for Alipay (`app_id`, `private_key`, `alipay_public_key`, etc.) and WeChat Pay (`mch_id`, `api_key`, etc.).
  - Implement a backend switch (`tutor-application.skip-payment-check`) to bypass payment for testing purposes.
- **Database**:
  - Update `tutor_application` table (or creating a new `payment_order` table linked to it) to track payment status (e.g., `PENDING`, `PAID`, `FAILED`).
  - Ensure `tutor_application` state transitions correctly when payment is completed.

## Capabilities

### New Capabilities
- `payment-processing`: Handles interaction with Alipay and WeChat Pay APIs, including order creation, signature generation, and callback processing.
- `payment-order-management`: Manages the lifecycle of payment orders, recording transaction details and status.

### Modified Capabilities
- `tutor-application-workflow`: Update the teacher application workflow to include a payment step. The application will not transition to a "chat allowed" state until payment is confirmed (unless the skip-payment switch is active).

## Impact

- **Codebase**: New `payment-service` module; updates to `ai-tutor-starter`; updates to `videoCall-IM-service` or `tutor-appointment-service` (depending on where the application logic resides) to check payment status.
- **Database**: Potential schema changes to `tutor_application` or new tables for payment records.
- **Configuration**: New configuration properties for payment keys and the test switch.
