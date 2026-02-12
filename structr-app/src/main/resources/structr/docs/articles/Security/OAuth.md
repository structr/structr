Structr supports OAuth authentication through various external identity providers. OAuth allows users to authenticate using their existing accounts from services like Google, GitHub, or Microsoft Entra ID, eliminating the need for separate credentials in your application.

OAuth implements an interactive login flow where users authenticate through a provider's login page. For machine-to-machine authentication using pre-issued tokens, see the JWKS section in the JWT Authentication chapter.

For more information about how OAuth works, see the <a href="https://auth0.com/docs/flows/authorization-code-flow" target="_new">Authorization Code Flow documentation</a>.

## Supported Providers

Structr includes built-in support for the following OAuth providers:

- OpenID Connect (Auth0) – works with any OIDC-compliant provider
- Microsoft Entra ID (Azure AD) – enterprise Single Sign-On with Azure Active Directory
- Keycloak – open-source identity and access management
- Google
- Facebook
- GitHub
- LinkedIn

You can also configure custom OAuth providers by specifying the required endpoints.

## Configuration

Configure OAuth settings in `structr.conf` or through the Configuration Interface.

### Enabling Providers

Control which OAuth providers are available using the `oauth.servers` setting:

| Setting | Description |
|---------|-------------|
| `oauth.servers` | Space-separated list of enabled OAuth providers (e.g., `google github azure`). Defaults to all available providers: `auth0 azure facebook github google linkedin keycloak` |

### Provider Settings

Each provider requires a client ID and client secret. Most providers also support simplified tenant-based configuration where endpoints are constructed automatically.

#### Recommended Approach: Tenant-Based Configuration

For providers that support it, use the tenant/server settings and Structr will automatically construct the authorization, token, and userinfo endpoints:

#### Auth0

```
oauth.auth0.tenant = your-tenant.auth0.com
oauth.auth0.client_id = <your-client-id>
oauth.auth0.client_secret = <your-client-secret>
```

#### Microsoft Entra ID (Azure AD)

```
oauth.azure.tenant_id = <your-tenant-id>
oauth.azure.client_id = <your-client-id>
oauth.azure.client_secret = <your-client-secret>
```

#### Keycloak

```
oauth.keycloak.server_url = https://keycloak.example.com
oauth.keycloak.realm = master
oauth.keycloak.client_id = <your-client-id>
oauth.keycloak.client_secret = <your-client-secret>
```

#### Other Providers (Google, GitHub, Facebook, LinkedIn)

These providers use default endpoints and only require credentials:
```
oauth.google.client_id = <your-client-id>
oauth.google.client_secret = <your-client-secret>
```

### Complete Provider Settings Reference

The following table shows all available settings. Replace `<provider>` with the provider name (`auth0`, `azure`, `google`, `facebook`, `github`, `linkedin`, `keycloak`).

#### General Settings (All Providers)

| Setting | Required | Description |
|---------|----------|-------------|
| `oauth.<provider>.client_id` | **Yes** | Client ID from the OAuth provider |
| `oauth.<provider>.client_secret` | **Yes** | Client secret from the OAuth provider |
| `oauth.<provider>.redirect_uri` | No | Callback URL that the provider calls after successful authentication. Defaults to `/oauth/<provider>/auth` |
| `oauth.<provider>.error_uri` | No | Page to redirect to when authentication fails. Defaults to `/error` |
| `oauth.<provider>.return_uri` | No | Page to redirect to after successful login. Defaults to `/` |
| `oauth.<provider>.logout_uri` | No | Logout URI. Defaults to `/logout` |
| `oauth.<provider>.scope` | No | OAuth scope. Defaults vary by provider |

#### Tenant/Server-Based Configuration (Recommended)

##### Auth0

| Setting | Required | Description |
|---------|----------|-------------|
| `oauth.auth0.tenant` | Recommended | Auth0 tenant domain (e.g., `your-tenant.auth0.com`). When set, endpoints are built automatically |
| `oauth.auth0.authorization_path` | No | Path to authorization endpoint. Defaults to `/authorize` |
| `oauth.auth0.token_path` | No | Path to token endpoint. Defaults to `/oauth/token` |
| `oauth.auth0.userinfo_path` | No | Path to userinfo endpoint. Defaults to `/userinfo` |
| `oauth.auth0.audience` | No | The API audience (identifier) of your Auth0 API. Required for API access tokens |

##### Azure AD

| Setting | Required | Description |
|---------|----------|-------------|
| `oauth.azure.tenant_id` | **Yes** | Azure AD tenant ID, or `common` for multi-tenant apps, or `organizations` for work accounts only. Defaults to `common` |

##### Keycloak

| Setting | Required | Description |
|---------|----------|-------------|
| `oauth.keycloak.server_url` | **Yes** | Keycloak server URL (e.g., `https://keycloak.example.com`) |
| `oauth.keycloak.realm` | **Yes** | Keycloak realm name. Defaults to `master` |

#### Manual Endpoint Configuration (Advanced)

If you don't use tenant-based configuration or need to override endpoints:

| Setting | Description |
|---------|-------------|
| `oauth.<provider>.authorization_location` | Full URL of the authorization endpoint |
| `oauth.<provider>.token_location` | Full URL of the token endpoint |
| `oauth.<provider>.user_details_resource_uri` | Full URL where Structr retrieves user details |

### Required Global Setting

Enable automatic user creation so Structr can create user nodes for new OAuth users:

| Setting | Value |
|---------|-------|
| `jsonrestservlet.user.autocreate` | `true` |

### Provider-Specific Examples

#### Microsoft Entra ID (Azure AD)

```
oauth.servers = azure
oauth.azure.tenant_id = <your-tenant-id>
oauth.azure.client_id = <your-client-id>
oauth.azure.client_secret = <your-client-secret>
oauth.azure.return_uri = /
jsonrestservlet.user.autocreate = true
```

#### Google

```
oauth.servers = google
oauth.google.client_id = <your-client-id>
oauth.google.client_secret = <your-client-secret>
jsonrestservlet.user.autocreate = true
```

#### GitHub

```
oauth.servers = github
oauth.github.client_id = <your-client-id>
oauth.github.client_secret = <your-client-secret>
jsonrestservlet.user.autocreate = true
```

#### Keycloak

```
oauth.servers = keycloak
oauth.keycloak.server_url = https://keycloak.example.com
oauth.keycloak.realm = production
oauth.keycloak.client_id = <your-client-id>
oauth.keycloak.client_secret = <your-client-secret>
jsonrestservlet.user.autocreate = true
```

## Admin UI Integration

When you configure an OAuth provider, Structr automatically adds a login button for that provider to the Admin UI login form. Clicking this button redirects to the provider's login page and returns to the Structr backend after successful authentication. This enables Single Sign-On for administrators without additional configuration.

### Setting the isAdmin flag
Please note that in order to log into the Admin User Interface, the new user must be created with the `isAdmin` flag set to true. That means you need to implement a custom `onOAuthLogin` lifecycle method as described below, and select an "Admin Group" in Azure AD that Structr can use to identify administrators.

## Triggering Authentication

For your own application pages, trigger OAuth authentication by redirecting users to `/oauth/<provider>/login`.

### HTML

```html
<a href="/oauth/auth0/login">Login with Auth0</a>
<a href="/oauth/azure/login">Login with Microsoft</a>
<a href="/oauth/google/login">Login with Google</a>
<a href="/oauth/github/login">Login with GitHub</a>
<a href="/oauth/facebook/login">Login with Facebook</a>
<a href="/oauth/linkedin/login">Login with LinkedIn</a>
<a href="/oauth/keycloak/login">Login with Keycloak</a>
```

## Authentication Flow

When a user clicks the login link in your page or in the Admin UI login form, the following process is executed:

1. Structr redirects the user to the provider's authorization URL
2. The user authenticates with the provider (enters credentials, approves permissions)
3. The provider redirects back to Structr's callback URL with an authorization code
4. Structr exchanges the authorization code for an access token
5. Structr retrieves user details from the provider
6. Structr creates or updates the local User node
7. If configured, Structr calls the `onOAuthLogin` method on the User type
8. Structr creates a session and redirects to the configured return URL

## Customizing User Creation

When a user logs in via OAuth for the first time, Structr creates a new user node. You can customize this process by implementing the `onOAuthLogin` lifecycle method on your User type (or a User subtype).

### Method Parameters

The `onOAuthLogin` method receives information about the login through `$.methodParameters`:

| Parameter | Description |
|-----------|-------------|
| `provider` | The name of the OAuth provider (e.g., "google", "github", "azure") |
| `userinfo` | Object containing user details from the provider |

The `userinfo` object contains provider-specific fields. Common fields include:

| Field | Description |
|-------|-------------|
| `name` | User's display name |
| `email` | User's email address |
| `sub` | Unique identifier from the provider |
| `accessTokenClaims` | Claims from the access token (provider-specific) |

### Example: Basic User Setup

```javascript
{
    $.log('User ' + $.this.name + ' logged in via ' + $.methodParameters.provider);
    
    // Update user name from provider data
    const providerName = $.methodParameters.userinfo['name'];
    if (providerName && $.this.name !== providerName) {
        $.this.name = providerName;
    }
    
    // Set email if available
    const providerEmail = $.methodParameters.userinfo['email'];
    if (providerEmail) {
        $.this.eMail = providerEmail;
    }
}
```

### Example: Azure AD Integration with Group Mapping

This example shows how to integrate with Azure Active Directory (Entra ID), including mapping Azure groups to Structr admin privileges:

```javascript
{
    const ADMIN_GROUP = 'bc6fbf5f-34f9-4789-8443-76b194edfa09';  // Azure AD group ID
    
    $.log('User ' + $.this.name + ' just logged in via ' + $.methodParameters.provider);
    $.log('User information: ', JSON.stringify($.methodParameters.userinfo, null, 2));
    
    // Update username from Azure AD
    if ($.this.name !== $.methodParameters.userinfo['name']) {
        $.log('Updating username ' + $.this.name + ' to ' + $.methodParameters.userinfo['name']);
        $.this.name = $.methodParameters.userinfo['name'];
    }
    
    // Check Azure AD group membership for admin rights
    let azureGroups = $.methodParameters.userinfo['accessTokenClaims']['wids'];
    $.log('Azure AD groups: ', JSON.stringify(azureGroups, null, 2));
    
    if (azureGroups.includes(ADMIN_GROUP)) {
        $.this.isAdmin = true;
        $.log('Granted admin rights for ' + $.this.name);
    } else {
        $.this.isAdmin = false;
        $.log('User ' + $.this.name + ' does not have admin rights');
    }
}
```

### Example: Mapping Provider Groups to Structr Groups

```javascript
{
    const GROUP_MAPPING = {
        'azure-editors-group-id': 'Editors',
        'azure-viewers-group-id': 'Viewers',
        'azure-admins-group-id': 'Administrators'
    };
    
    let azureGroups = $.methodParameters.userinfo['accessTokenClaims']['groups'] || [];
    
    for (let azureGroupId in GROUP_MAPPING) {
        let structrGroupName = GROUP_MAPPING[azureGroupId];
        let structrGroup = $.first($.find('Group', 'name', structrGroupName));
        
        if (structrGroup) {
            if (azureGroups.includes(azureGroupId)) {
                $.add_to_group(structrGroup, $.this);
                $.log('Added ' + $.this.name + ' to group ' + structrGroupName);
            } else {
                $.remove_from_group(structrGroup, $.this);
                $.log('Removed ' + $.this.name + ' from group ' + structrGroupName);
            }
        }
    }
}
```

## Provider Setup

Each OAuth provider requires you to register your application and obtain client credentials. The general process is:

1. Create a developer account with the provider
2. Register a new application
3. Configure the redirect URI to match `oauth.<provider>.redirect_uri` exactly
4. Copy the client ID and client secret to your Structr configuration

### Redirect URI Format

Your redirect URI typically follows this pattern:

```
https://your-domain.com/oauth/<provider>/auth
```

Register this URL with the provider and ensure it matches your Structr configuration exactly. Mismatched redirect URIs are a common source of OAuth errors.

### Provider-Specific Setup Notes

#### Microsoft Entra ID (Azure AD)

- Register the application in Azure Portal under "App registrations"
- Configure "Redirect URIs" under Authentication – use `https://your-domain.com/oauth/azure/auth`
- Add required API permissions (e.g., User.Read, openid, profile)
- For group claims, configure "Token configuration" to include groups
- Use your Azure tenant ID in the `oauth.azure.tenant_id` setting, or use `common` for multi-tenant apps

#### Google

- Enable the "Google+ API" or "People API" in the Google Cloud Console
- Configure OAuth consent screen before creating credentials
- Uses default endpoints – only client credentials required

#### GitHub

- Set "Authorization callback URL" in your OAuth App settings
- Request appropriate scopes (e.g., `user:email` for email access)
- Uses default endpoints – only client credentials required

#### Keycloak

- Create a client in your Keycloak realm
- Set "Valid Redirect URIs" to `https://your-domain.com/oauth/keycloak/auth`
- Configure client authentication and standard flow
- Provide server URL and realm name for automatic endpoint construction

#### Auth0

- Create an application in the Auth0 dashboard
- Configure "Allowed Callback URLs" to `https://your-domain.com/oauth/auth0/auth`
- Copy your Auth0 tenant domain (e.g., `your-tenant.auth0.com`)
- Tenant-based configuration automatically constructs all endpoints

## Error Handling

When authentication fails, Structr redirects to the configured `error_uri` with error information in the query parameters.

Common error scenarios:

| Error | Cause | Solution |
|-------|-------|----------|
| `invalid_client` | Wrong client ID or secret | Verify credentials in Structr configuration |
| `redirect_uri_mismatch` | Redirect URI doesn't match | Ensure exact match between provider and Structr config |
| `access_denied` | User denied permission | User must approve the requested permissions |
| `server_error` | Provider-side error | Check provider status, retry later |

## Best Practices

- **Use HTTPS** - OAuth requires secure connections in production
- **Use tenant-based configuration** - Simplifies setup and reduces configuration errors
- **Validate user data** - Don't blindly trust data from providers; validate and sanitize
- **Map groups carefully** - Document the relationship between provider groups and Structr permissions
- **Handle token expiration** - OAuth tokens expire; implement refresh logic if needed
- **Log authentication events** - Track logins for security auditing
- **Enable only needed providers** - Use `oauth.servers` to limit available authentication methods

## Related Topics

- User Management - Users, groups, and the permission system
- JWT Authentication - Token-based authentication, including external JWKS providers for machine-to-machine scenarios
- Two-Factor Authentication - Adding a second factor after OAuth login
- REST Interface/Authentication - Resource Access Permissions and endpoint security