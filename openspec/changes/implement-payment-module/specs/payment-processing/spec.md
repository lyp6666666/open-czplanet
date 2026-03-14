## ADDED Requirements

### Requirement: Alipay App Payment
The system SHALL support creating Alipay App payment orders.

#### Scenario: Create Alipay Order
- **WHEN** a client requests an Alipay payment for a valid application
- **THEN** the system returns a signed order string compatible with Alipay App SDK

### Requirement: WeChat App Payment
The system SHALL support creating WeChat App payment orders.

#### Scenario: Create WeChat Order
- **WHEN** a client requests a WeChat payment for a valid application
- **THEN** the system returns a payment parameter object (including appId, partnerId, prepayId, package, nonceStr, timestamp, sign) compatible with WeChat App SDK

### Requirement: Payment Callback Handling
The system SHALL expose an endpoint to receive asynchronous payment notifications from Alipay and WeChat.

#### Scenario: Valid Alipay Callback
- **WHEN** the system receives a callback from Alipay with a valid signature and trade status TRADE_SUCCESS
- **THEN** the system updates the corresponding payment order to PAID and returns "success" to Alipay

#### Scenario: Valid WeChat Callback
- **WHEN** the system receives a callback from WeChat with a valid signature and result_code SUCCESS
- **THEN** the system updates the corresponding payment order to PAID and returns a success XML/JSON response to WeChat

#### Scenario: Invalid Signature
- **WHEN** the system receives a callback with an invalid signature
- **THEN** the system rejects the request and does not update the order status
