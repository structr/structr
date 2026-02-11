# User

Represents user accounts in your application. Every request to Structr runs in the context of a user – either authenticated or anonymous. Users can log in via HTTP headers, session cookies, JWT tokens, or OAuth providers. Key properties include `name` and `eMail` for identification, `password` (stored as secure hash), `isAdmin` for full system access, `blocked` for disabling accounts, and `locale` for localization.

## Details

Structr supports two-factor authentication via TOTP and automatically locks accounts after too many failed login attempts. For self-service scenarios, users can register themselves and confirm their account via email. You can extend the User type with custom properties or create subtypes like Employee or Customer.
