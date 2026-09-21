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
| `security.jwt.audience` | empty | Comma-separated list of values written into the `aud` claim of every token this instance issues. When set, verification rejects tokens whose audience does not intersect this list; when empty, no audience claim is emitted or verified. Enabling it invalidates all existing access and refresh tokens. |
| `security.jwt.expirationtime` | 60 | Access token expiration in minutes |
| `security.jwt.refreshtoken.expirationtime` | 1440 | Refresh token expiration in minutes (default: 24 hours) |

Besides the REST resource `/structr/rest/token` used in the examples below, Structr also serves a dedicated token servlet and a login servlet. Their paths are configured with `tokenservlet.path` (default `/structr/token`) and `loginservlet.path` (default `/structr/login`). The rate limiter for authentication endpoints uses these paths, see [Rate Limiting](/structr/docs/ontology/Security/Rate%20Limiting).

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

To obtain a new access token without sending user credentials again, send the refresh token in the `Refresh-Token` request header or as the `refresh_token` key in the JSON request body:

**curl:**

```bash
curl -X POST http://localhost:8082/structr/rest/token \
  -H "Refresh-Token: eyJhbGciOiJIUzI1NiJ9..."
```

**JavaScript:**

```javascript
const response = await fetch('/structr/rest/token', {
    method: 'POST',
    headers: {
        'Refresh-Token': refreshToken
    }
});

const data = await response.json();
const newAccessToken = data.result.access_token;
```

### Token Lifetime

The access token remains valid until:

- The expiration time is exceeded
- The refresh token that Structr created with it is revoked or used

A refresh token can be used exactly once. When you exchange it for a new access token, Structr removes it from the user and issues a new pair of tokens, which also invalidates the access token that was issued with it. Requesting a token with user credentials does not affect tokens issued earlier; a user can hold several valid token pairs at the same time.

The refresh token remains valid until:

- The expiration time is exceeded
- It is used to obtain a new access token
- You revoke it (see Revoking Tokens below)

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

Structr does not store tokens as separate objects. For each token pair it issues, it records an identifier in the `refreshTokens` property of the user, and an access token is only accepted while its identifier is still present there. Removing identifiers from that property revokes the corresponding tokens before they expire. The property is read-only for REST clients, so revocation happens through the following means.

### Logout

A POST request to `/structr/rest/logout`, authenticated with the access token, clears all refresh token identifiers of the current user and thereby revokes all of the user's access and refresh tokens. This requires a Resource Access Permission with the signature `_logout` that allows POST for authenticated users.

**curl:**

```bash
curl -X POST http://localhost:8082/structr/rest/logout \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**JavaScript:**

```javascript
await fetch('/structr/rest/logout', {
    method: 'POST',
    headers: {
        'Authorization': `Bearer ${accessToken}`
    }
});
```

### Revoking Tokens From Code

Internally, the Principal interface provides the methods `clearTokens()`, which removes all refresh token identifiers of a user, and `removeRefreshToken(tokenId)`, which removes a single identifier. Structr calls `clearTokens()` on logout and when a session times out, and `removeRefreshToken()` when a refresh token is exchanged. These methods are not exposed to scripts, and the `refreshTokens` property is read-only for admin users as well, so a PUT request or a plain `$.set()` is rejected with a 422 error. A script that runs with superuser privileges may write the property directly, for example to log out a user from all devices:

```javascript
{
    $.doPrivileged(() => {
        const user = $.find('User', { name: 'john.doe' })[0];
        $.set(user, 'refreshTokens', []);
    });
}
```

Removing single identifiers from the array revokes the corresponding token pairs only. Structr also removes expired refresh token identifiers automatically whenever it issues new tokens for a user.

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
| `security.jwks.audience` | **Required.** Comma-separated list of accepted `aud` values, usually the client id this application is registered under at the provider |
| `security.jwt.jwtissuer` | The expected issuer claim in the JWT |
| `security.jwks.admin.claim.key` | Token claim to check for admin privileges (optional) |
| `security.jwks.admin.claim.value` | Value that grants admin privileges (optional) |
| `security.jwks.group.claim.key` | Token claim containing group memberships (optional) |

> **Note:** `security.jwks.audience` is not optional, and tokens are refused while it is empty. Signature and issuer together say only that the provider issued the token, not that it was issued for this installation - the same provider issues tokens to every other application in the tenant, and to anybody who can sign up there. The `aud` claim is what distinguishes them.

### Microsoft Entra ID

To validate tokens issued by Microsoft Entra ID (formerly Azure Active Directory), configure the JWKS endpoint and issuer for your Azure tenant:

```
security.jwt.secrettype = jwks
security.jwks.provider = https://login.microsoftonline.com/<tenant-id>/discovery/v2.0/keys
security.jwks.audience = <application-client-id>
security.jwt.jwtissuer = https://login.microsoftonline.com/<tenant-id>/v2.0
security.jwks.admin.claim.key = roles
security.jwks.admin.claim.value = <your-admin-role-name>
security.jwks.group.claim.key = roles
```

Replace `<tenant-id>` with your Azure tenant ID, `<application-client-id>` with the application (client) ID of the app registration the tokens are issued for - whatever the tokens carry in `aud`, which may also be an App ID URI - and `<your-admin-role-name>` with the role value that should grant admin privileges in Structr.

In Azure Portal, configure your App Registration to include role claims in the token under "Token configuration".

After you configure these settings, Structr validates tokens in the Authorization header against the configured service.

### How It Works

When Structr receives a request with a JWT in the Authorization header:

1. Structr extracts the token and reads its header to identify the signing key (via the `kid` claim)
2. Structr fetches the public keys from the configured JWKS endpoint
3. Structr verifies the token signature using the appropriate public key
4. If validation succeeds, Structr processes the request in the context of the authenticated identity

Structr contacts the JWKS endpoint for every request that carries an externally issued token; it does not cache the public keys. If the endpoint cannot be reached, the token cannot be verified and the request is treated like one with an invalid token.

The identity Structr creates for a validated external token is temporary and not stored in the database. Its id and name are taken from the token claims named by `security.jwks.id.claim.key` and `security.jwks.name.claim.key`, both of which default to `oid`.

### Error Handling

If token validation fails, Structr returns an appropriate HTTP error:

| Status | Reason |
|--------|--------|
| 401 Unauthorized | Token is missing, expired, has an invalid signature, or could not be verified because the JWKS endpoint was unreachable |

## Best Practices

- **Use short expiration times for access tokens** - 15-60 minutes is typical. Use refresh tokens to obtain new access tokens.
- **Store refresh tokens securely** - Refresh tokens have longer lifetimes and should be protected.
- **Use HTTPS** - Always transmit tokens over encrypted connections.
- **Implement token refresh logic** - Check for 401 responses and automatically refresh tokens when they expire.
- **Revoke tokens on logout** - Call the logout endpoint when users log out so that Structr clears their refresh tokens and prevents token reuse.

## Related Topics

- User Management - Users, groups, and the permission system
- OAuth - Interactive authentication with external identity providers
- Two-Factor Authentication - Adding a second factor to login security
- REST Interface/Authentication - Resource Access Permissions and endpoint security
