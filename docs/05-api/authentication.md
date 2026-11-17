# Authentication & Session Architecture

- Mechanism: Stateless JSON Web Tokens (JWT) using HMAC-SHA256 or RSA-256.
- Claims: user_id, company_id, email, roles[].
- Token Lifespan: Access token: 15 minutes; Refresh token: 7 days (stored with rotation in database).
- Header: Authorization: Bearer <access_token>
