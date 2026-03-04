## ADDED Requirements

### Requirement: Admin Login
The system SHALL provide an API for administrators to log in using a username and password. Upon successful authentication, the system SHALL return an access token.

#### Scenario: Successful Login
- **WHEN** an admin provides a valid username and password
- **THEN** the system returns an HTTP 200 response with a valid access token

#### Scenario: Invalid Credentials
- **WHEN** an admin provides an invalid username or password
- **THEN** the system returns an HTTP 401 response with an error message

### Requirement: Admin Token Verification
The system SHALL require a valid admin access token for all protected admin API endpoints.

#### Scenario: Access Protected Resource with Valid Token
- **WHEN** a request is made to a protected admin endpoint with a valid token in the Authorization header
- **THEN** the system processes the request and returns the resource

#### Scenario: Access Protected Resource without Token
- **WHEN** a request is made to a protected admin endpoint without a token
- **THEN** the system returns an HTTP 401 response
