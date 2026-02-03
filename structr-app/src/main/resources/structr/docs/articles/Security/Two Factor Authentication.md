# Two-Factor Authentication

Structr supports two-factor authentication (2FA) using the TOTP (Time-Based One-Time Password) standard. When enabled, users must provide a code from an authenticator app in addition to their password. This adds a second layer of security that protects accounts even if passwords are compromised.

TOTP is compatible with common authenticator apps like Google Authenticator, Microsoft Authenticator, Authy, and others.

## Prerequisites

Because TOTP relies on synchronized time, ensure that both the Structr server and users' mobile devices are synced to an NTP server. Time drift of more than 30 seconds can cause authentication failures.

## Configuration

Configure two-factor authentication in `structr.conf` or through the Configuration Interface.

### Application Settings

| Setting | Default | Description |
|---------|---------|-------------|
| `security.twofactorauthentication.level` | 1 | Enforcement level: 0 = disabled, 1 = optional (per-user), 2 = required for all users |
| `security.twofactorauthentication.issuer` | structr | The issuer name displayed in authenticator apps |
| `security.twofactorauthentication.algorithm` | SHA1 | Hash algorithm: SHA1, SHA256, or SHA512 |
| `security.twofactorauthentication.digits` | 6 | Code length: 6 or 8 digits |
| `security.twofactorauthentication.period` | 30 | Code validity period in seconds |
| `security.twofactorauthentication.logintimeout` | 30 | Time window in seconds to enter the code after password authentication |
| `security.twofactorauthentication.loginpage` | /twofactor | Application page for entering the two-factor code |
| `security.twofactorauthentication.whitelistedIPs` | | Comma-separated list of IP addresses that bypass two-factor authentication |

> **Note:** Changing `algorithm`, `digits`, or `period` after users have already enrolled invalidates their existing authenticator setup. Set `twoFactorConfirmed = false` on affected users so they receive a new QR code on their next login.

### Enforcement Levels

The `level` setting controls how two-factor authentication applies to users:

| Level | Behavior |
|-------|----------|
| 0 | Two-factor authentication is completely disabled |
| 1 | Optional - users can enable 2FA individually via the `isTwoFactorUser` property |
| 2 | Required - all users must use two-factor authentication |

## User Properties

Three properties on the User type control two-factor authentication:

| Property | Type | Description |
|----------|------|-------------|
| `isTwoFactorUser` | Boolean | Enables two-factor authentication for this user. Only effective when level is set to 1 (optional). |
| `twoFactorConfirmed` | Boolean | Indicates whether the user has completed two-factor setup. Automatically set to true after first successful 2FA login. Set to false to force re-enrollment. |
| `twoFactorSecret` | String | The secret key used to generate TOTP codes. Automatically generated when the user first enrolls. |

## Authentication Flow

The two-factor login process works as follows:

1. User submits username and password to `/structr/rest/login`
2. If credentials are valid and 2FA is enabled, Structr returns HTTP status 202 (Accepted)
3. The response headers contain a temporary token and, for first-time setup, QR code data
4. User scans the QR code with their authenticator app (first time only)
5. User enters the 6-digit code from their authenticator app
6. User submits the code with the temporary token to `/structr/rest/login`
7. If the code is valid, Structr creates a session and returns HTTP status 200

## Implementation

To implement two-factor authentication in your application, you need two pages: a login page and a two-factor code entry page.

### Login Page

Create a login form that detects the two-factor response. When the server returns status 202, redirect to the two-factor page with the token and optional QR data.

**JavaScript:**

```javascript
async function login(username, password) {
    const response = await fetch('/structr/rest/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            name: username,
            password: password
        })
    });

    if (response.status === 202) {
        // Two-factor authentication required
        const token = response.headers.get('token');
        const qrdata = response.headers.get('qrdata') || '';
        const twoFactorPage = response.headers.get('twoFactorLoginPage');
        
        window.location.href = `${twoFactorPage}?token=${token}&qrdata=${qrdata}`;
    } else if (response.ok) {
        // Login successful, no 2FA required
        window.location.href = '/';
    } else {
        // Login failed
        const error = await response.json();
        console.error('Login failed:', error);
    }
}
```

**curl:**

```bash
curl -si http://localhost:8082/structr/rest/login \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"name": "user", "password": "password"}'
```

When two-factor authentication is required, the response looks like:

```
HTTP/1.1 202 Accepted
token: eyJhbGciOiJIUzI1NiJ9...
twoFactorLoginPage: /twofactor
qrdata: iVBORw0KGgoAAAANSUhEUgAA...
```

The response headers contain:

| Header | Description |
|--------|-------------|
| `token` | Temporary token for the two-factor login (valid for the configured timeout period) |
| `twoFactorLoginPage` | The configured page for entering the two-factor code |
| `qrdata` | Base64-encoded PNG image of the QR code (only present if `twoFactorConfirmed` is false) |

### Two-Factor Page

Create a page that displays the QR code for first-time setup and accepts the TOTP code.

**JavaScript:**

```javascript
document.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(location.search);
    const token = params.get('token');
    const qrdata = params.get('qrdata');
    
    // Display QR code for first-time setup
    if (qrdata) {
        const qrImage = document.getElementById('qrcode');
        // Convert URL-safe base64 back to standard base64
        const standardBase64 = qrdata.replaceAll('_', '/').replaceAll('-', '+');
        qrImage.src = 'data:image/png;base64,' + standardBase64;
        qrImage.style.display = 'block';
        
        document.getElementById('setup-instructions').style.display = 'block';
    }
    
    // Handle form submission
    document.getElementById('twoFactorForm').addEventListener('submit', async (event) => {
        event.preventDefault();
        
        const code = document.getElementById('code').value;
        
        const response = await fetch('/structr/rest/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                twoFactorToken: token,
                twoFactorCode: code
            })
        });
        
        if (response.ok) {
            window.location.href = '/';
        } else {
            document.getElementById('error').textContent = 'Invalid code. Please try again.';
        }
    });
});
```

**curl:**

```bash
curl -si http://localhost:8082/structr/rest/login \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"twoFactorToken": "eyJhbGciOiJIUzI1NiJ9...", "twoFactorCode": "123456"}'
```

### Example HTML Structure

```html
<!DOCTYPE html>
<html>
<head>
    <title>Two-Factor Authentication</title>
</head>
<body>
    <h1>Two-Factor Authentication</h1>
    
    <div id="setup-instructions" style="display: none;">
        <p>Scan this QR code with your authenticator app:</p>
        <img id="qrcode" alt="QR Code" />
        <p>Then enter the 6-digit code shown in your app.</p>
    </div>
    
    <form id="twoFactorForm">
        <label for="code">Authentication Code:</label>
        <input type="text" id="code" name="code" 
               pattern="[0-9]{6,8}" maxlength="8" 
               autocomplete="one-time-code" required />
        <button type="submit">Verify</button>
    </form>
    
    <p id="error" style="color: red;"></p>
    
    <script src="twofactor.js"></script>
</body>
</html>
```

## Managing User Enrollment

### Enabling 2FA for a User

When the enforcement level is set to 1 (optional), enable two-factor authentication for individual users by setting `isTwoFactorUser` to true.

**curl:**

```bash
curl -X PUT http://localhost:8082/structr/rest/User/<UUID> \
  -H "Content-Type: application/json" \
  -H "X-User: admin" \
  -H "X-Password: admin" \
  -d '{"isTwoFactorUser": true}'
```

**JavaScript:**

```javascript
await fetch('/structr/rest/User/<UUID>', {
    method: 'PUT',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        isTwoFactorUser: true
    })
});
```

The user will see the QR code on their next login.

### Re-Enrolling a User

To force a user to set up two-factor authentication again (for example, if they lost their phone), set `twoFactorConfirmed` to false:

**curl:**

```bash
curl -X PUT http://localhost:8082/structr/rest/User/<UUID> \
  -H "Content-Type: application/json" \
  -H "X-User: admin" \
  -H "X-Password: admin" \
  -d '{"twoFactorConfirmed": false}'
```

The user will receive a new QR code on their next login. Their authenticator app will need to be updated with the new secret.

### Disabling 2FA for a User

To disable two-factor authentication for a user (when level is 1):

**curl:**

```bash
curl -X PUT http://localhost:8082/structr/rest/User/<UUID> \
  -H "Content-Type: application/json" \
  -H "X-User: admin" \
  -H "X-Password: admin" \
  -d '{"isTwoFactorUser": false}'
```

## IP Whitelisting

For trusted networks or automated systems, you can bypass two-factor authentication based on IP address. Add IP addresses to the `security.twofactorauthentication.whitelistedIPs` setting:

```
security.twofactorauthentication.whitelistedIPs = 192.168.1.100, 10.0.0.0/24
```

Requests from whitelisted IPs proceed with password authentication only, even if the user has two-factor authentication enabled.

## Troubleshooting

### Invalid Code Errors

If users consistently receive "invalid code" errors:

1. **Check time synchronization** - The most common cause is time drift between the server and the user's device. Ensure both are synced to NTP.
2. **Verify the period setting** - If you changed `security.twofactorauthentication.period`, users need to re-enroll.
3. **Check the algorithm** - Some older authenticator apps only support SHA1.

### Lost Authenticator Access

If a user loses access to their authenticator app:

1. An administrator sets `twoFactorConfirmed = false` on the user
2. The user logs in with username and password
3. The user scans the new QR code with their authenticator app
4. The user completes the login with the new code

### QR Code Not Displaying

If the QR code does not display:

1. Check that `qrdata` is present in the response headers
2. Verify the base64 conversion (URL-safe to standard)
3. Ensure the `twoFactorConfirmed` property is false

## Related Topics

- User Management - User properties and account security
- JWT Authentication - Token-based authentication
- OAuth - Authentication with external providers
