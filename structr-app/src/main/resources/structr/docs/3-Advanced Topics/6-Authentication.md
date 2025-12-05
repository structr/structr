# Authentication

Authentication in Structr provides multiple methods for securing your applications and managing user access. This guide covers the various authentication mechanisms available, from traditional session-based login to modern JWT tokens and OAuth integration.

## Overview

Structr supports several authentication methods:

- **Session-based authentication** - Traditional cookie-based sessions
- **JSON Web Tokens (JWT)** - Stateless token-based authentication
- **OAuth providers** - Third-party authentication (GitHub, Auth0, etc.)
- **Request header authentication** - Direct credential passing

Each method serves different use cases and security requirements, allowing you to choose the most appropriate approach for your application.

## Session-Based Authentication

### Login Process

Users can authenticate using a simple HTTP POST request to the REST interface. Upon successful authentication, Structr returns a session cookie that browsers automatically include in subsequent requests.

**Endpoint:** `POST /structr/rest/login`

**Request payload:**
```json
{
  "eMail": "username",
  "password": "password"
}
```

**Example using curl:**
```bash
curl --request POST \
  --url http://localhost:8082/structr/rest/login \
  --header 'content-type: application/json' \
  --data '{
    "eMail": "admin",
    "password": "admin"
  }'
```

**Requirements:**
- A resource access grant with signature `_login` must exist
- The grant must allow POST requests for public users

### Logout Process

To end a user session, make an HTTP POST request to the logout endpoint.

**Endpoint:** `POST /structr/rest/logout`

**Requirements:**
- A resource access grant with signature `_logout` must exist
- The grant must allow POST requests for authenticated users

## JSON Web Tokens (JWT)

JWT authentication provides a stateless, scalable solution for modern applications. Structr supports JWT signing with either secret keys or public/private key pairs.

### Configuration Methods

#### Using Secret Key

Configure the following settings in `structr.conf`:

| Key | Value |
|-----|-------|
| `security.jwt.method` | `secret` |
| `security.jwt.secret` | Your secret key (minimum 32 characters) |

#### Using Java KeyStore

For enhanced security, use a public/private key pair stored in a Java KeyStore:

1. **Create a KeyStore file:**
```bash
keytool -genkey -alias jwtkey -keyalg RSA -keystore server.jks -storepass jkspassword
```

2. **Place the KeyStore file** beside your `structr.conf` file

3. **Configure settings:**

| Key | Value |
|-----|-------|
| `security.jwt.method` | `keypair` |
| `security.jwt.keystore` | Your KeyStore filename |
| `security.jwt.keystore.password` | KeyStore password |
| `security.jwt.key.alias` | Key alias in KeyStore |

### Token Creation

Request tokens from the token resource endpoint:

**Endpoint:** `POST /structr/rest/token`

**Initial authentication:**
```javascript
fetch("http://localhost:8082/structr/rest/token", {
  method: "POST",
  body: JSON.stringify({
    name: "admin",
    password: "admin"
  })
})
```

**Using refresh token:**
```javascript
fetch("http://localhost:8082/structr/rest/token", {
  method: "POST",
  headers: {
    "refresh_token": "REFRESH_TOKEN_HERE"
  }
})
```

**Response format:**
```json
{
  "result": {
    "access_token": "ACCESS_TOKEN",
    "refresh_token": "REFRESH_TOKEN", 
    "expiration_date": "1597923368582",
    "token_type": "Bearer"
  }
}
```

**Requirements:**
- A resource access grant with signature `_token` must exist
- The grant must allow POST requests for all users

### Token Usage

Authenticate requests using JWT tokens in two ways:

1. **Automatic cookies** - Tokens stored as HttpOnly cookies (sent automatically)
2. **Authorization header** - Manual token inclusion

**Example with Authorization header:**
```javascript
fetch("http://localhost:8082/structr/rest/User", {
  method: "GET",
  headers: {
    "authorization": "Bearer ACCESS_TOKEN_HERE"
  }
})
```

### Token Lifetime

Access tokens remain valid until:
- Expiration time is exceeded
- Token is explicitly revoked
- Associated refresh token is revoked
- A new token is created

Refresh tokens remain valid until:
- Expiration time is exceeded
- Token is explicitly revoked

### Configuration Options

Customize JWT behavior with these settings:

| Key | Description |
|-----|-------------|
| `security.jwt.expirationtime` | Access token expiration time |
| `security.jwt.refreshtoken.expirationtime` | Refresh token expiration time |
| `security.jwt.issuer` | JWT issuer field value |

## External JWT Providers (JWKS)

Structr can validate tokens issued by external authentication systems like Keycloak or Auth0 through their JWKS endpoints.

**Configuration:**
Set `security.jwks.provider` to the provider's JWKS endpoint URL in `structr.conf`.

Once configured, Structr validates tokens in the Authorization header against the specified service.

## OAuth Authentication

Structr supports OAuth authentication through various providers including GitHub and Auth0. This enables users to authenticate using their existing accounts with these services.

### Configuration

Configure OAuth providers in `structr.conf` using provider-specific settings. Example for Auth0:

| Key | Description |
|-----|-------------|
| `oauth.auth0.client_id` | Client ID from provider |
| `oauth.auth0.client_secret` | Client secret from provider |
| `oauth.auth0.authorization_location` | Provider's authorization endpoint |
| `oauth.auth0.token_location` | Provider's token endpoint |
| `oauth.auth0.redirect_uri` | Callback URL for your application |
| `oauth.auth0.user_details_resource_uri` | Provider's user info endpoint |
| `oauth.auth0.error_uri` | Error redirect page |
| `oauth.auth0.return_uri` | Success redirect page |

### Additional Settings

- `jsonrestservlet.user.autocreate` - Enable automatic user creation for OAuth users

### Usage

Trigger OAuth authentication by redirecting users to:
`/oauth/<provider>/login`

**Example for Auth0:**
```html
<a href="/oauth/auth0/login">Login with Auth0</a>
```

## Request Header Authentication

For development or specific use cases, you can pass credentials directly in request headers:

```bash
curl --request POST \
  --url http://localhost:8082/structr/rest/User \
  --header 'x-user: admin' \
  --header 'x-password: admin'
```

**Security Warning:** This method poses security risks and should only be used over secure connections (HTTPS or VPN).

## User Registration

### Self-Registration Process

Enable users to register themselves through a double opt-in process:

**Endpoint:** `POST /structr/rest/registration`

**Request:**
```javascript
fetch("http://localhost:8082/structr/rest/registration", {
  method: "POST",
  body: JSON.stringify({
    eMail: "user.name@mail.com"
  })
})
```

**Requirements:**
- Resource access grant with signature `_registration` for public users
- `jsonrestservlet.user.autocreate` enabled in `structr.conf`
- Mail configuration properly set up

### Email Templates

Customize registration emails using these mail template keys:

| Key | Purpose | Default |
|-----|---------|---------|
| `CONFIRM_REGISTRATION_SENDER_ADDRESS` | Sender email | structr-mail-daemon@localhost |
| `CONFIRM_REGISTRATION_SENDER_NAME` | Sender name | Structr Mail Daemon |
| `CONFIRM_REGISTRATION_SUBJECT` | Email subject | Welcome to Structr, please finalize registration |
| `CONFIRM_REGISTRATION_TEXT_BODY` | Plain text body | Go to ${link} to finalize registration. |
| `CONFIRM_REGISTRATION_HTML_BODY` | HTML body | Click <a href='${link}'>here</a> to finalize registration. |

## Password Reset

### Reset Process

Allow users to reset forgotten passwords:

**Endpoint:** `POST /structr/rest/reset-password`

**Request:**
```javascript
fetch("http://localhost:8082/structr/rest/reset-password", {
  method: "POST",
  body: JSON.stringify({
    eMail: "user.name@mail.com"
  })
})
```

**Requirements:**
- Resource access grant with signature `_resetPassword` for public users
- `JsonRestServlet.user.autologin` set to true for auto-login functionality

### Email Templates

Configure password reset emails:

| Key | Purpose | Default |
|-----|---------|---------|
| `RESET_PASSWORD_SENDER_ADDRESS` | Sender email | structr-mail-daemon@localhost |
| `RESET_PASSWORD_SENDER_NAME` | Sender name | Structr Mail Daemon |
| `RESET_PASSWORD_SUBJECT` | Email subject | Request to reset your Structr password |
| `RESET_PASSWORD_TEXT_BODY` | Plain text body | Go to ${link} to reset your password. |
| `RESET_PASSWORD_HTML_BODY` | HTML body | Click <a href='${link}'>here</a> to reset your password. |

## Security Considerations

### Best Practices

- **Use HTTPS** for all authentication in production environments
- **Implement proper CORS policies** for web applications
- **Regularly rotate JWT secrets** and KeyStore certificates
- **Set appropriate token expiration times** based on security requirements
- **Monitor authentication logs** for suspicious activities

### Resource Access Grants

All authentication endpoints require proper resource access grants:

- `_login` - For login functionality
- `_logout` - For logout functionality  
- `_token` - For JWT token creation
- `_registration` - For user self-registration
- `_resetPassword` - For password reset functionality

Configure these grants in the Structr admin interface under Security > Resource Access Grants.

### Configuration Dependencies

Ensure these configurations are properly set:

- **Mail settings** - Required for registration and password reset emails
- **User autocreation** - Enable `jsonrestservlet.user.autocreate` for registration
- **Auto-login** - Enable `JsonRestServlet.user.autologin` for password reset links

## Troubleshooting

### Common Issues

**Authentication fails:**
- Verify resource access grants are configured correctly
- Check that required configuration settings are enabled
- Ensure user credentials are correct

**JWT tokens not working:**
- Verify JWT configuration method (secret vs keypair)
- Check token expiration settings
- Ensure proper Authorization header format

**Email notifications not sent:**
- Verify mail configuration settings
- Check SMTP server connectivity
- Review mail template configurations

**OAuth authentication fails:**
- Verify provider configuration settings
- Check redirect URIs match provider settings
- Ensure client ID and secret are correct

This authentication system provides flexible options for securing your Structr applications while maintaining ease of use for both developers and end users.