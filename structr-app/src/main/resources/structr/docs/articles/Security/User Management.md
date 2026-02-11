# User Management

Structr provides a multi-layered security system that combines user and group management with flexible permission resolution. This chapter covers how to manage users and groups, how authentication works, and how permissions are resolved.

## Users

The User type is a built-in type in Structr that represents user accounts in your application. Users can authenticate, own objects, receive permissions, and belong to groups. Every request to Structr is evaluated in the context of a user - either an authenticated user or an anonymous user.

You can use the User type directly, extend it with additional properties, or create subtypes for specialized user categories in your application.

### Creating Users

You can create users through the Admin UI or programmatically.

**Via Admin UI:**

1. Navigate to the Security area
2. Click "Add User"
3. Structr creates a new user with a random default name
4. Rename the user and configure properties through the Edit dialog

**Via REST API (curl):**

```bash
curl -X POST http://localhost:8082/structr/rest/User \
  -H "Content-Type: application/json" \
  -H "X-User: admin" \
  -H "X-Password: admin" \
  -d '{
    "name": "john.doe",
    "eMail": "john.doe@example.com",
    "password": "securePassword123"
  }'
```

**Via REST API (JavaScript):**

```javascript
const response = await fetch('/structr/rest/User', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        name: 'john.doe',
        eMail: 'john.doe@example.com',
        password: 'securePassword123'
    })
});

const result = await response.json();
console.log('Created user:', result.result.id);
```

### User Properties

The following properties are available on user objects:

| Property | Type | Description |
|----------|------|-------------|
| `name` | String | Username for authentication |
| `eMail` | String | Email address, often used as an alternative login identifier |
| `password` | String | User password (stored as a secure hash, never in cleartext) |
| `isAdmin` | Boolean | Administrator flag that grants full system access, bypassing all permission checks |
| `blocked` | Boolean | When true, completely disables the account and prevents any action |
| `passwordAttempts` | Integer | Counter for failed login attempts; triggers account lockout when threshold is exceeded |
| `locale` | String | Preferred locale for localization (e.g., `de_DE`, `en_US`). Structr uses this value in the `$.locale()` function and for locale-aware formatting. |
| `publicKey` | String | SSH public key for filesystem access via the SSH service |
| `skipSecurityRelationships` | Boolean | Disables automatic creation of OWNS and SECURITY relationships when this user creates objects. Useful for admin users creating many objects where individual ownership tracking is not needed. |
| `confirmationKey` | String | Temporary authentication key used during self-registration. Replaces the password until the user confirms their account via the confirmation link. |
| `twoFactorSecret` | String | Secret key for TOTP two-factor authentication (see Two-Factor Authentication chapter) |
| `twoFactorConfirmed` | Boolean | Indicates whether the user has completed two-factor setup |
| `isTwoFactorUser` | Boolean | Enables two-factor authentication for this user (when 2FA level is set to optional) |

### Setting Passwords

Structr never stores cleartext passwords - only secure hash values. To set or change a password:

**Via Admin UI:**

1. Open the user's Edit Properties dialog
2. Go to the Node Properties tab
3. Enter the new password in the Password field

**Via REST API (curl):**

```bash
curl -X PUT http://localhost:8082/structr/rest/User/<UUID> \
  -H "Content-Type: application/json" \
  -H "X-User: admin" \
  -H "X-Password: admin" \
  -d '{"password": "newSecurePassword456"}'
```

**Via REST API (JavaScript):**

```javascript
await fetch('/structr/rest/User/<UUID>', {
    method: 'PUT',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        password: 'newSecurePassword456'
    })
});
```

You cannot display or recover existing passwords. If a user forgets their password, use the password reset flow or set a new password directly.

### Extending the User Type

You can customize the User type to fit your application's needs.

#### Adding Properties

To add properties to the User type, open the Schema area, locate the User type, and add new properties. For example, you might add a `phoneNumber` property or a `department` property. These properties then become available on all user objects.

#### Creating Subtypes

For more complex scenarios, you can create subtypes of User. This is useful when your application has different kinds of users with different properties or behaviors - for example, an `Employee` type and a `Customer` type, both inheriting from User.

To create a subtype, create a new type in the Schema and select User as its base class. The subtype inherits all User functionality (authentication, permissions, group membership) and can add its own properties and methods.

## Groups

The Group type organizes users and simplifies permission management. Instead of granting permissions to individual users, you grant them to groups and add users to those groups. When a user belongs to a group, they inherit all permissions granted to that group.

Groups also serve as the integration point for external directory services like LDAP. When you connect Structr to an LDAP server, directory groups can map to Structr groups, enabling centralized user management. For details, see the LDAP chapter.

### Creating Groups

**Via Admin UI:**

1. Navigate to the Security area
2. Click "Add Group"
3. Rename the group as appropriate

**Via REST API (curl):**

```bash
curl -X POST http://localhost:8082/structr/rest/Group \
  -H "Content-Type: application/json" \
  -H "X-User: admin" \
  -H "X-Password: admin" \
  -d '{"name": "Editors"}'
```

**Via REST API (JavaScript):**

```javascript
await fetch('/structr/rest/Group', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        name: 'Editors'
    })
});
```

### Managing Membership

In the Admin UI, drag and drop users into groups in the Security area. Groups can contain both users and other groups, allowing hierarchical structures.

**Via REST API (curl):**

```bash
# Add user to group
curl -X PUT http://localhost:8082/structr/rest/User/<USER_UUID> \
  -H "Content-Type: application/json" \
  -H "X-User: admin" \
  -H "X-Password: admin" \
  -d '{"groups": ["<GROUP_UUID>"]}'
```

**Via REST API (JavaScript):**

```javascript
await fetch('/structr/rest/User/<USER_UUID>', {
    method: 'PUT',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        groups: ['<GROUP_UUID>']
    })
});
```

### Group Inheritance

All members inherit access rights granted to a group. This includes direct group members, users in nested subgroups, and permissions that flow down the group hierarchy.

### Schema-Based Permissions

In addition to object-level permissions, Structr supports schema-based permissions that apply to all instances of a type. This feature allows you to grant a group access to all objects of a specific type without creating individual permission grants.

To configure schema-based permissions:

1. Open the Schema area
2. Select the type you want to configure
3. Open the Security tab
4. Configure which groups have read, write, delete, or accessControl permissions on all instances of this type

Schema-based permissions are evaluated efficiently and improve performance compared to individual object permissions, especially when you have many objects of the same type.

## User Categories

Structr distinguishes several categories of users based on their authentication status and privileges.

### Anonymous Users

Requests without authentication credentials are anonymous requests. The corresponding user is called the anonymous user or public user. Anonymous users are at the lowest level of the access control hierarchy and can only access objects explicitly marked as public.

### Authenticated Users

A request that includes valid credentials is an authenticated request. Authenticated users are at a higher level in the access control hierarchy and can access objects based on their permissions, group memberships, and ownership.

### Admin Users

Admin users have the `isAdmin` flag set to true. They can create, read, modify, and delete all nodes and relationships in the database. They can access all endpoints, modify the schema, and execute maintenance tasks. Admin users bypass all permission checks.

> **Note:** The `isAdmin` flag is required for users to log into the Structr Admin UI.

### Superuser

The superuser is a special account defined in `structr.conf` with the `superuser.password` setting. This account exists separately from regular admin users and serves specific purposes:

- Logging into the Configuration Interface
- Performing system-level operations that require elevated privileges beyond normal admin access

The superuser account is not stored in the database. It exists only through the configuration file setting.

## Authentication Methods

Authentication determines who is making a request. Structr supports multiple authentication methods that you can use depending on your application's needs.

| Method | Use Case |
|--------|----------|
| HTTP Headers | Simple API access, scripting |
| Session Cookies | Web applications with login forms |
| JSON Web Tokens | Stateless APIs, single-page applications |
| OAuth | Login via external providers (Google, GitHub, etc.) |

For details on JWT authentication, including token creation, refresh tokens, and external JWKS providers, see the JWT Authentication chapter.

For details on OAuth authentication with providers like Google, GitHub, or Auth0, see the OAuth chapter.

### Authentication Headers

You can provide username and password via the HTTP headers `X-User` and `X-Password`. When you secure the connection with TLS, the headers are encrypted and your credentials are protected.

> **Note:** Do not use authentication headers over unencrypted connections (http://…) except for localhost. Always use HTTPS for remote servers.

**curl:**

```bash
curl -s http://localhost:8082/structr/rest/Project \
  -H "X-User: admin" \
  -H "X-Password: admin"
```

**JavaScript:**

```javascript
const response = await fetch('/structr/rest/Project', {
    headers: {
        'X-User': 'admin',
        'X-Password': 'admin'
    }
});

const data = await response.json();
console.log(data.result);
```

Response:

```json
{
    "result": [
        {
            "id": "362cc05768044c7db886f0bec0061a0a",
            "type": "Project",
            "name": "Project #1"
        }
    ],
    "query_time": "0.000035672",
    "result_count": 1,
    "page_count": 1,
    "result_count_time": "0.000114435",
    "serialization_time": "0.001253579"
}
```

You must send the authentication headers with every request. For applications where this is impractical, use session-based authentication.

### Sessions

Session-based authentication lets you log in once and use a session cookie for subsequent requests. The server maintains session state and the cookie authenticates each request.

#### Prerequisites

Create a Resource Access Permission with the signature `_login` that allows POST for non-authenticated users. For details on Resource Access Permissions, see the REST Interface chapter.

#### Login

**curl:**

```bash
curl -si http://localhost:8082/structr/rest/login \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"name": "user", "password": "password"}'
```

**JavaScript:**

```javascript
const response = await fetch('/structr/rest/login', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    credentials: 'include',  // Important: include cookies
    body: JSON.stringify({
        name: 'user',
        password: 'password'
    })
});

if (response.ok) {
    const data = await response.json();
    console.log('Logged in as:', data.result.name);
}
```

Response:

```
HTTP/1.1 200 OK
Set-Cookie: JSESSIONID=f49d1dbb60be23612b0820453d996e41...;Path=/
Content-Type: application/json;charset=utf-8

{
    "result": {
        "id": "0490bebcbc2f4018857a492c532334c2",
        "type": "User",
        "isUser": true,
        "name": "user"
    }
}
```

The `Set-Cookie` header contains the session ID. Most HTTP clients handle session cookies automatically.

#### Logout

To end a session, send a POST request to the logout endpoint. Create a Resource Access Permission with the signature `_logout` that allows POST for authenticated users.

**curl:**

```bash
curl -si http://localhost:8082/structr/rest/logout \
  -X POST \
  -b "JSESSIONID=your-session-id"
```

**JavaScript:**

```javascript
await fetch('/structr/rest/logout', {
    method: 'POST',
    credentials: 'include'
});
```

## Permission System

Structr's permission system operates on multiple levels, checked in the following order:

1. **Administrator Check** - Users with `isAdmin=true` bypass all other checks
2. **Visibility Flags** - Simple public/private flags on objects
3. **Ownership** - Creator/owner permissions
4. **Permission Grants** - Explicit user/group permissions
5. **Schema-Based Permissions** - Type-level permissions for groups
6. **Graph-Based Resolution** - Permission propagation through relationships

### Permission Types

Four basic permissions control access to objects:

| Permission | Description |
|------------|-------------|
| **Read** | View object properties and relationships |
| **Write** | Modify object properties |
| **Delete** | Remove objects from the database |
| **AccessControl** | Modify security settings and permissions on the object |

### Visibility Flags

Every object has two visibility flags:

| Flag | Description |
|------|-------------|
| `visibleToPublicUsers` | Grants read access to anonymous users |
| `visibleToAuthenticatedUsers` | Grants read access to logged-in users |

These flags provide simple access control without explicit permission grants. Visibility grants read permission - the object appears in results and you can read its properties.

Note that these flags are independent: `visibleToPublicUsers` does not imply visibility for authenticated users, and `visibleToAuthenticatedUsers` does not imply visibility for anonymous users.

### Ownership

When a non-admin user creates an object, Structr automatically grants full permissions (Read, Write, Delete, AccessControl) through an OWNS relationship.

When an anonymous user creates an object (if a Resource Access Permission allows the request), the object becomes ownerless. You can configure default permissions for ownerless nodes in the Configuration Interface.

> **Note:** An object must first be visible to a user before they can modify it.

For admin users, you can disable automatic ownership creation by setting `skipSecurityRelationships = true`. This improves performance when creating many objects that do not need individual ownership tracking.

To prevent ownership for non-admin users, add an `onCreate` lifecycle method:

```javascript
{
    $.set($.this, 'owner', null);
}
```

### Permission Grants

You can grant specific permissions to users or groups on individual objects through SECURITY relationships.

**Granting permissions:**

```javascript
$.grant(user, node, 'read, write');
```

**Revoking permissions:**

```javascript
$.revoke(user, node, 'write');
```

Structr creates SECURITY relationships automatically when users create objects. To skip this for admin users, set `skipSecurityRelationships = true`. For non-admin users, use a lifecycle method:

```javascript
{
    $.revoke($.me, $.this, 'read, write, delete, accessControl');
}
```

> **Note:** If you skip both OWNS and SECURITY relationships, the creating user may lose access to the object. Use `grant()` or `copy_permissions()` to assign appropriate access.

### Graph-Based Permission Resolution

For complex scenarios, Structr can propagate permissions through relationships. This enables domain-specific security models where access to one object grants access to related objects.

#### How It Works

When you configure a relationship for permission propagation, Structr follows that relationship when resolving access. For example, if a user has READ permission on a ProductGroup, and you configure the relationship from ProductGroup to Product to propagate READ, the user automatically gets READ access to all Products in that group.

Relationships configured for permission propagation are called active relationships and appear in orange in the schema editor.

#### Propagation Direction

| Direction | Effect |
|-----------|--------|
| **None** | Permission resolution not active |
| **Source to Target** | Permissions propagate in the direction of the relationship |
| **Target to Source** | Permissions propagate against the direction of the relationship |
| **Both** | Permissions propagate in both directions |

#### Permission Actions

For each permission type (read, write, delete, accessControl), you can configure what happens when traversing the relationship:

| Action | Effect |
|--------|--------|
| **Add** | Grants this permission to users traversing the relationship |
| **Keep** | Maintains this permission if the user already has it |
| **Remove** | Revokes this permission when traversing |

#### Hidden Properties

When users gain access through permission propagation, you can hide sensitive properties from them. Configure hidden properties on the relationship, and Structr excludes those properties from JSON output for users accessing objects via that path.

#### Resolution Process

When a non-admin user accesses an object:

1. Structr checks for direct permissions
2. If none exist, Structr searches for connected paths through active relationships
3. Structr traverses relationships applying ADD, KEEP, or REMOVE rules
4. If a valid path with sufficient permissions is found, access is granted
5. If no path exists, access is denied

Permission resolution only follows active relationships. If your schema has a chain like ProductGroup → SubGroup → Product, but only ProductGroup → SubGroup is active, users with access to ProductGroup do not automatically access Products in SubGroups.

## Account Security

### Password Policy

Configure password requirements in `structr.conf`:

```properties
# Minimum password length
security.passwordpolicy.minlength = 8

# Maximum failed login attempts before lockout
security.passwordpolicy.maxfailedattempts = 4

# Complexity requirements
security.passwordpolicy.complexity.enforce = true
security.passwordpolicy.complexity.requiredigits = true
security.passwordpolicy.complexity.requirelowercase = true
security.passwordpolicy.complexity.requireuppercase = true
security.passwordpolicy.complexity.requirenonalphanumeric = true

# Clear all sessions when password changes
security.passwordpolicy.onchange.clearsessions = true
```

When you enable complexity enforcement, passwords must contain at least one character from each required category.

### Account Lockout

When a user exceeds the maximum failed login attempts configured in `security.passwordpolicy.maxfailedattempts`, Structr locks the account. The `passwordAttempts` property tracks failures.

To unlock an account, reset `passwordAttempts` to 0:

**curl:**

```bash
curl -X PUT http://localhost:8082/structr/rest/User/<UUID> \
  -H "Content-Type: application/json" \
  -H "X-User: admin" \
  -H "X-Password: admin" \
  -d '{"passwordAttempts": 0}'
```

**JavaScript:**

```javascript
await fetch('/structr/rest/User/<UUID>', {
    method: 'PUT',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        passwordAttempts: 0
    })
});
```

### Blocking Users

To manually disable a user account, set `blocked` to true. A blocked user cannot perform any action in Structr, regardless of their permissions or admin status. This is useful for temporarily suspending accounts without deleting them.

**curl:**

```bash
curl -X PUT http://localhost:8082/structr/rest/User/<UUID> \
  -H "Content-Type: application/json" \
  -H "X-User: admin" \
  -H "X-Password: admin" \
  -d '{"blocked": true}'
```

To unblock a user, set `blocked` to false.

### Two-Factor Authentication

Structr supports TOTP-based two-factor authentication. For configuration and implementation details, see the Two-Factor Authentication chapter.

### User Self-Registration

You can allow users to sign up themselves instead of creating accounts manually. The registration process uses double opt-in: users enter their email address, receive a confirmation email, and click a link to complete registration.

#### Prerequisites

- Configure SMTP settings so Structr can send emails (see the SMTP chapter)
- Create a Resource Access Permission with signature `_registration` allowing POST for public users
- Enable `jsonrestservlet.user.autocreate` in `structr.conf`

#### How It Works

1. User submits their email to the registration endpoint
2. Structr creates a user with a `confirmationKey` instead of a password
3. Structr sends a confirmation email with a unique link
4. User clicks the link, which validates the `confirmationKey`
5. Structr confirms the account and redirects to the target page
6. User can now set their password and log in normally

#### Mail Templates

Structr uses the following mail templates for registration emails. Create these as MailTemplate objects to overwrite the defaults:

| Template Name | Purpose | Default Value |
|---------------|---------|---------------|
| `CONFIRM_REGISTRATION_SENDER_ADDRESS` | Sender email address | `smtp.user` from structr.conf (if it contains a valid email address); otherwise `structr-mail-daemon@localhost` |
| `CONFIRM_REGISTRATION_SENDER_NAME` | Sender name | Structr Mail Daemon |
| `CONFIRM_REGISTRATION_SUBJECT` | Email subject | Welcome to Structr, please finalize registration |
| `CONFIRM_REGISTRATION_TEXT_BODY` | Plain text body | Go to ${link} to finalize registration. |
| `CONFIRM_REGISTRATION_HTML_BODY` | HTML body | `<div>Click <a href='${link}'>here</a> to finalize registration.</div>` |
| `CONFIRM_REGISTRATION_BASE_URL` | Base URL for the link | ${base_url} |
| `CONFIRM_REGISTRATION_TARGET_PAGE` | Redirect page after confirmation | register_thanks |
| `CONFIRM_REGISTRATION_ERROR_PAGE` | Redirect page on error | register_error |

The `${link}` variable in the body templates contains the confirmation URL.

> **Note:** You can use scripting in the TEXT_BODY and HTML_BODY templates. The script runs in the context of the user (the `me` keyword refers to the user being registered).

#### Registration Endpoint

**curl:**

```bash
curl -X POST http://localhost:8082/structr/rest/registration \
  -H "Content-Type: application/json" \
  -d '{"eMail": "user.name@example.com"}'
```

**JavaScript:**

```javascript
await fetch('/structr/rest/registration', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        eMail: 'user.name@example.com'
    })
});
```

The accepted attributes are configured in `registration.customuserattributes`. The `eMail` attribute is always supported.

### Password Reset

To allow users to regain access when they forget their password, Structr provides a password reset flow.

#### Prerequisites

- Configure SMTP settings so Structr can send emails (see the SMTP chapter)
- Create a Resource Access Permission with signature `_resetPassword` allowing POST for public users
- Enable `jsonrestservlet.user.autologin` in `structr.conf` to allow auto-login via the reset link

#### Mail Templates

Structr uses the following mail templates for password reset emails. Create these as MailTemplate objects to overwrite the defaults:

| Template Name | Purpose | Default Value |
|---------------|---------|---------------|
| `RESET_PASSWORD_SENDER_ADDRESS` | Sender email address | `smtp.user` from structr.conf (if it contains a valid email address); otherwise `structr-mail-daemon@localhost` |
| `RESET_PASSWORD_SENDER_NAME` | Sender name | Structr Mail Daemon |
| `RESET_PASSWORD_SUBJECT` | Email subject | Request to reset your Structr password |
| `RESET_PASSWORD_TEXT_BODY` | Plain text body | Go to ${link} to reset your password. |
| `RESET_PASSWORD_HTML_BODY` | HTML body | `<div>Click <a href='${link}'>here</a> to reset your password.</div>` |
| `RESET_PASSWORD_BASE_URL` | Base URL for the link | ${base_url} |
| `RESET_PASSWORD_TARGET_PAGE` | Redirect page for password entry | /reset-password |

The `${link}` variable contains the password reset URL. This link is valid only once.

#### Password Reset Endpoint

**curl:**

```bash
curl -X POST http://localhost:8082/structr/rest/reset-password \
  -H "Content-Type: application/json" \
  -d '{"eMail": "user.name@example.com"}'
```

**JavaScript:**

```javascript
await fetch('/structr/rest/reset-password', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        eMail: 'user.name@example.com'
    })
});
```

Structr sends an email with a link to the configured target page. When the user clicks the link, they are automatically logged in and can set a new password.

## Best Practices

- **Grant minimal permissions** - Follow the principle of least privilege
- **Use groups effectively** - Manage permissions through groups rather than individual grants
- **Use schema-based permissions for performance** - When all instances of a type should have the same permissions, configure them at the schema level
- **Test with non-admin users** - Admin users bypass all permission checks, so always test your permission design with regular users
- **Design clear permission flows** - When using graph-based resolution, document how permissions propagate through your data model
- **Monitor failed logins** - Watch for brute-force attempts through the `passwordAttempts` property
- **Enable two-factor authentication** - Require 2FA for admin users and sensitive operations

## Related Topics

- Two-Factor Authentication - TOTP-based second factor for login security
- JWT Authentication - Token-based authentication with JSON Web Tokens
- OAuth - Authentication with external providers like Google, GitHub, or Auth0
- SMTP - Configuring email for self-registration and password reset
- LDAP - Integrating with external directory services
- SSH Service - Configuring SSH access to the Structr filesystem
- REST Interface/Authentication - Resource Access Permissions and endpoint security
- Security (Admin UI) - Managing users, groups, and resource access permissions in the Admin UI
