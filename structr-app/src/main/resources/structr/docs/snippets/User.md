# User

Represents user accounts in your application. Users authenticate via multiple methods including HTTP headers, session cookies, JWT tokens, or OAuth providers. Every request to Structr is evaluated in the context of a user, either authenticated or anonymous. Key properties include `name` and `eMail` for identification, `password` for authentication (stored as secure hash), `isAdmin` for full system access bypassing all permission checks, `blocked` for disabling accounts, and `locale` for localization preferences.

## Security and Extensibility

The type supports two-factor authentication via TOTP, automatic account lockout after failed login attempts tracked in `passwordAttempts`, and self-registration with email confirmation using a temporary `confirmationKey`. You can extend the User type with additional properties in the schema or create subtypes for specialized user categories like Employee or Customer, inheriting all authentication and permission functionality.
