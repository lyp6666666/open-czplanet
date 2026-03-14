## 1. Project Setup & Configuration

- [ ] 1.1 Create `payment-service` Maven module with necessary dependencies (Spring Boot, MyBatis, etc.).
- [ ] 1.2 Add Alipay SDK and WeChat Pay SDK dependencies to `payment-service/pom.xml`.
- [ ] 1.3 Register `payment-service` in `ai-tutor-starter` and parent POM.
- [ ] 1.4 Create `PaymentProperties` class to map configuration for Alipay, WeChat, and the `skip-payment-check` switch.

## 2. Database Implementation

- [ ] 2.1 Create SQL migration script for `payment_order` table (fields: order_no, user_id, amount, channel, status, transaction_id, context_id, etc.).
- [ ] 2.2 Create `PaymentOrder` entity and `PaymentOrderMapper` (MyBatis).
- [ ] 2.3 Implement `PaymentOrderService` for CRUD operations on payment orders.

## 3. Payment Strategies Implementation

- [ ] 3.1 Define `PaymentStrategy` interface for unifying different payment channels.
- [ ] 3.2 Implement `AlipayPaymentStrategy` for App/Wap payment order generation.
- [ ] 3.3 Implement `WeChatPaymentStrategy` for App/JSAPI payment order generation.
- [ ] 3.4 Implement `PaymentFactory` or strategy pattern to select the correct provider based on request.

## 4. API & Callback Handling

- [ ] 4.1 Create `PaymentController` with endpoints to initiate payment (returns order info/signature).
- [ ] 4.2 Create `PaymentCallbackController` to handle asynchronous notifications from Alipay (`/notify/alipay`) and WeChat (`/notify/wechat`).
- [ ] 4.3 Implement signature verification logic for both providers.
- [ ] 4.4 Implement idempotent order status update logic: verify signature -> check amount -> update order status -> update application status.

## 5. Integration & Business Logic

- [ ] 5.1 Implement `TutorApplicationService` integration to update `tutor_application` status (e.g., `chat_access_status` = `ALLOWED`) upon successful payment.
- [ ] 5.2 Implement the "skip payment" logic: Check `tutor-application.skip-payment-check` property before enforcing payment restrictions (e.g., in an interceptor or service check).
- [ ] 5.3 Add an endpoint or logic to query the current payment/chat access status for an application.

## 6. Verification

- [ ] 6.1 Add unit tests for `PaymentOrderService` and strategy selection.
- [ ] 6.2 Add integration tests for the payment flow (mocking external provider calls).
- [ ] 6.3 Verify the `skip-payment-check` switch works as expected in a local environment.
