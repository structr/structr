# JWT Authentication

Structr supports authentication and authorization with JSON Web Tokens (JWTs). JWTs enable stateless authentication where the server does not need to maintain session state. This approach is particularly useful for APIs, single-page applications, and mobile apps.

You can learn more about JWT at [https://jwt.io/](https://jwt.io/).

## Configuration

Structr supports three methods for signing and verifying JWTs:

- **Secret Key** – a shared secret for signing and verification
- **Java KeyStore** – a private/public keypair stored in a JKS file
- **External JWKS** – validation against an external identity provider like Microsoft Entra ID

### Secret Key

To use JWTs with a secret key, configure the following settings in `structr.conf` or through the Configuration Interface:

| Setting | Value |
|---------|-------|
| `security.jwt.secrettype` | `secret` |
| `security.jwt.secret` | Your secret key (at least 32 characters) |

### Java KeyStore

When you want to sign and verify JWTs with a private/public keypair, you first need to create a Java KeyStore file containing your keys.

Create a new keypair in a new KeyStore file with the following keytool command:

```bash
keytool -genkey -alias jwtkey -keyalg RSA -keystore server.jks -storepass jkspassword
```

Store the KeyStore file in the same directory as your `structr.conf` file.

Configure the following settings:

| Setting | Value |
|---------|-------|
| `security.jwt.secrettype` | `keypair` |
| `security.jwt.keystore` | The name of your KeyStore file |
| `security.jwt.keystore.password` | The password to your KeyStore file |
| `security.jwt.key.alias` | The alias of the key in the KeyStore file |

## Token Settings

You can adjust token expiration and issuer in the configuration:

| Setting | Default | Description |
|---------|---------|-------------|
| `security.jwt.jwtissuer` | `structr` | The issuer field in the JWT |
| `security.jwt.expirationtime` | 60 | Access token expiration in minutes |
| `security.jwt.refreshtoken.expirationtime` | 1440 | Refresh token expiration in minutes (default: 24 hours) |

## Creating Tokens

Structr creates JWT access tokens through a request to the token resource. With each access token, Structr also creates a refresh token that you can use to obtain further access tokens without sending user credentials again.

Structr provides the tokens in the response body and stores them as HttpOnly cookies in the browser.

### Prerequisites

Create a Resource Access Permission with the signature `_token` that allows POST for public users.

### Requesting a Token

**curl:**

```bash
curl -X POST http://localhost:8082/structr/rest/token \
  -H "Content-Type: application/json" \
  -d '{
    "name": "admin",
    "password": "admin"
  }'
```

**JavaScript:**

```javascript
const response = await fetch('/structr/rest/token', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        name: 'admin',
        password: 'admin'
    })
});

const data = await response.json();
const accessToken = data.result.access_token;
const refreshToken = data.result.refresh_token;
```

Response:

```json
{
  "result": {
    "access_token": "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJzdHJ1Y3RyIiwic3ViIjoiYWRtaW4iLCJleHAiOjE1OTc5MjMzNjh9...",
    "refresh_token": "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJzdHJ1Y3RyIiwidHlwZSI6InJlZnJlc2giLCJleHAiOjE1OTgwMDYxNjh9...",
    "expiration_date": "1597923368582",
    "token_type": "Bearer"
  },
  "result_count": 1,
  "page_count": 1,
  "result_count_time": "0.000041704",
  "serialization_time": "0.000166971"
}
```

### Refreshing a Token

To obtain a new access token without sending user credentials again, include the refresh token in the request header:

**curl:**

```bash
curl -X POST http://localhost:8082/structr/rest/token \
  -H "refresh_token: eyJhbGciOiJIUzI1NiJ9..."
```

**JavaScript:**

```javascript
const response = await fetch('/structr/rest/token', {
    method: 'POST',
    headers: {
        'refresh_token': refreshToken
    }
});

const data = await response.json();
const newAccessToken = data.result.access_token;
```

### Token Lifetime

The access token remains valid until:

- The expiration time is exceeded
- You revoke the token
- You revoke the refresh token that Structr created with it
- The user creates a new token (which invalidates the previous one)

The refresh token remains valid until:

- The expiration time is exceeded
- You revoke the token

## Authenticating Requests

To authenticate a request with a JWT, you have two options.

### Cookie-Based Authentication

When you request a token from a browser, Structr stores the access token as an HttpOnly cookie. The browser automatically sends this cookie with subsequent requests, so you do not need additional configuration.

**JavaScript:**

```javascript
// After obtaining a token, subsequent requests are automatically authenticated
const response = await fetch('/structr/rest/User', {
    credentials: 'include'  // Include cookies
});

const data = await response.json();
```

### Bearer Token Authentication

For API access or when cookies are not available, send the access token in the HTTP Authorization header:

**curl:**

```bash
curl http://localhost:8082/structr/rest/User \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**JavaScript:**

```javascript
const response = await fetch('/structr/rest/User', {
    headers: {
        'Authorization': `Bearer ${accessToken}`
    }
});

const data = await response.json();
```

## Revoking Tokens

Structr stores tokens in the database, allowing you to revoke them before they expire. This is useful for implementing logout functionality or invalidating compromised tokens.

### Viewing Active Tokens

You can query the `RefreshToken` type to see active tokens for a user:

**curl:**

```bash
curl http://localhost:8082/structr/rest/RefreshToken \
  -H "X-User: admin" \
  -H "X-Password: admin"
```

**JavaScript:**

```javascript
const response = await fetch('/structr/rest/RefreshToken', {
    headers: {
        'Authorization': `Bearer ${accessToken}`
    }
});

const tokens = await response.json();
```

### Revoking a Specific Token

To revoke a token, delete the corresponding RefreshToken object:

**curl:**

```bash
curl -X DELETE http://localhost:8082/structr/rest/RefreshToken/<UUID> \
  -H "X-User: admin" \
  -H "X-Password: admin"
```

**JavaScript:**

```javascript
await fetch(`/structr/rest/RefreshToken/${tokenId}`, {
    method: 'DELETE',
    headers: {
        'Authorization': `Bearer ${accessToken}`
    }
});
```

When you delete a refresh token, the associated access token also becomes invalid.

### Revoking All Tokens for a User

To log out a user from all devices, delete all their refresh tokens:

**curl:**

```bash
curl -X DELETE "http://localhost:8082/structr/rest/RefreshToken?user=<USER_UUID>" \
  -H "X-User: admin" \
  -H "X-Password: admin"
```

## External JWKS Providers

Structr can validate JWTs issued by external authentication systems like Microsoft Entra ID, Keycloak, Auth0, or other OIDC-compliant identity providers. This enables machine-to-machine authentication where external systems send requests to Structr with pre-issued tokens.

When an external system (such as an Entra ID service principal) sends a request to Structr with a JWT in the Authorization header, Structr validates the token by fetching the public key from the configured JWKS endpoint. Structr does not manage these external identities - it only validates the tokens they produce.

This capability is particularly useful for:

- Integrating with enterprise identity providers
- Machine-to-machine authentication using service principals
- Centralizing authentication across multiple applications

> **Note:** JWKS validation handles incoming requests with externally-issued tokens. For interactive user login through external providers, see the OAuth chapter.

### Configuration

To enable external token validation, configure the JWKS provider settings:

| Setting | Description |
|---------|-------------|
| `security.jwt.secrettype` | Set to `jwks` for external JWKS validation |
| `security.jwks.provider` | The JWKS endpoint URL of the external service |
| `security.jwt.jwtissuer` | The expected issuer claim in the JWT |
| `security.jwks.admin.claim.key` | Token claim to check for admin privileges (optional) |
| `security.jwks.admin.claim.value` | Value that grants admin privileges (optional) |
| `security.jwks.group.claim.key` | Token claim containing group memberships (optional) |

### Microsoft Entra ID

To validate tokens issued by Microsoft Entra ID (formerly Azure Active Directory), configure the JWKS endpoint and issuer for your Azure tenant:

```
security.jwt.secrettype = jwks
security.jwks.provider = https://login.microsoftonline.com/<tenant-id>/discovery/v2.0/keys
security.jwt.jwtissuer = https://login.microsoftonline.com/<tenant-id>/v2.0
security.jwks.admin.claim.key = roles
security.jwks.admin.claim.value = <your-admin-role-name>
security.jwks.group.claim.key = roles
```

Replace `<tenant-id>` with your Azure tenant ID and `<your-admin-role-name>` with the role value that should grant admin privileges in Structr.

In Azure Portal, configure your App Registration to include role claims in the token under "Token configuration".

After you configure these settings, Structr validates tokens in the Authorization header against the configured service.

### How It Works

When Structr receives a request with a JWT in the Authorization header:

1. Structr extracts the token and reads its header to identify the signing key (via the `kid` claim)
2. Structr fetches the public keys from the configured JWKS endpoint
3. Structr verifies the token signature using the appropriate public key
4. If validation succeeds, Structr processes the request in the context of the authenticated identity

Structr caches the public keys from the JWKS endpoint to avoid fetching them on every request.

### Error Handling

If token validation fails, Structr returns an appropriate HTTP error:

| Status | Reason |
|--------|--------|
| 401 Unauthorized | Token is missing, expired, or has an invalid signature |
| 503 Service Unavailable | JWKS endpoint is unreachable |

When the JWKS endpoint is temporarily unavailable, Structr uses cached keys if available. If no cached keys exist, the request fails with a 503 error.

## Best Practices

- **Use short expiration times for access tokens** - 15-60 minutes is typical. Use refresh tokens to obtain new access tokens.
- **Store refresh tokens securely** - Refresh tokens have longer lifetimes and should be protected.
- **Use HTTPS** - Always transmit tokens over encrypted connections.
- **Implement token refresh logic** - Check for 401 responses and automatically refresh tokens when they expire.
- **Revoke tokens on logout** - Delete refresh tokens when users log out to prevent token reuse.

## Related Topics

- User Management - Users, groups, and the permission system
- OAuth - Interactive authentication with external identity providers
- Two-Factor Authentication - Adding a second factor to login security
- REST Interface/Authentication - Resource Access Permissions and endpoint security
