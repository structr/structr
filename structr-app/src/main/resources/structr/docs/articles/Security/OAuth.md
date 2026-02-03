
Structr supports OAuth authentication through various external identity providers. OAuth allows users to authenticate using their existing accounts from services like Google, GitHub, or Auth0, eliminating the need for separate credentials in your application.

OAuth implements an interactive login flow where users authenticate through a provider's login page. For machine-to-machine authentication using pre-issued tokens, see the JWKS section in the JWT Authentication chapter.

For more information about how OAuth works, see the <a href="https://auth0.com/docs/flows/authorization-code-flow" target="_new">Authorization Code Flow documentation</a>.

## Supported Providers

Structr includes built-in support for the following OAuth providers:

- OpenID Connect (Auth0) – works with any OIDC-compliant provider, including **Microsoft Entra ID**
- Google
- Facebook
- Twitter
- GitHub
- LinkedIn

The OpenID Connect provider uses the `auth0` configuration key and connects to any OIDC-compliant identity provider. This makes it the recommended choice for enterprise Single Sign-On with Microsoft Entra ID, Okta, Keycloak, or similar identity platforms.

You can also configure custom OAuth providers by specifying the required endpoints.

## Configuration

Configure OAuth settings in `structr.conf` or through the Configuration Interface. The following example shows the generic settings for the `auth0` provider. Use these settings for Auth0, Microsoft Entra ID, or any other OIDC-compliant identity provider. The same pattern applies to all supported providers.

### Provider Settings

| Setting | Description |
|---------|-------------|
| `oauth.auth0.client_id` | Client ID from the OAuth provider |
| `oauth.auth0.client_secret` | Client secret from the OAuth provider |
| `oauth.auth0.authorization_location` | Authorization URL where Structr redirects users to authenticate |
| `oauth.auth0.token_location` | Token URL that Structr calls to obtain the access token |
| `oauth.auth0.redirect_uri` | Callback URL that the provider calls after successful authentication |
| `oauth.auth0.user_details_resource_uri` | URL where Structr retrieves user details (username, email, etc.) |
| `oauth.auth0.error_uri` | Page to redirect to when authentication fails |
| `oauth.auth0.return_uri` | Page to redirect to after successful login |

Replace `auth0` with the provider name (`google`, `facebook`, `twitter`, `github`, `linkedin`) for other providers.

### Microsoft Entra ID

Microsoft Entra ID (formerly Azure Active Directory) connects through the `auth0` provider configuration, which serves as Structr's generic OpenID Connect endpoint.

Replace the tenant ID with your Azure tenant ID and configure your client credentials from the Azure App Registration:

```
oauth.auth0.accesstoken.location = header
oauth.auth0.authorization_location = https://login.microsoftonline.com/<tenant-id>/oauth2/v2.0/authorize
oauth.auth0.client_id = <your-client-id>
oauth.auth0.client_secret = <your-client-secret>
oauth.auth0.error_uri = /error
oauth.auth0.logout_return_location = /
oauth.auth0.logout_return_uri = /
oauth.auth0.redirect_uri = /oauth/auth0/auth
oauth.auth0.return_uri = /
oauth.auth0.token_location = https://login.microsoftonline.com/<tenant-id>/oauth2/v2.0/token
oauth.auth0.user_details_resource_uri = https://graph.microsoft.com/oidc/userinfo
oauth.servers = auth0
```

For group-based permission mapping with Entra ID, see the Azure AD examples under Customizing User Creation.

### Required Global Setting

Enable automatic user creation so Structr can create user nodes for new OAuth users:

| Setting | Value |
|---------|-------|
| `jsonrestservlet.user.autocreate` | `true` |

## Admin UI Integration

When you configure an OAuth provider, Structr automatically adds a login button for that provider to the Admin UI login form. Clicking this button redirects to the provider's login page and returns to the Structr backend after successful authentication. This enables Single Sign-On for administrators without additional configuration.

## Triggering Authentication

For your own application pages, trigger OAuth authentication by redirecting users to `/oauth/<provider>/login`.

**HTML:**

```html
<a href="/oauth/auth0/login">Login with Auth0</a>
<a href="/oauth/google/login">Login with Google</a>
<a href="/oauth/github/login">Login with GitHub</a>
<a href="/oauth/facebook/login">Login with Facebook</a>
<a href="/oauth/twitter/login">Login with Twitter</a>
<a href="/oauth/linkedin/login">Login with LinkedIn</a>
```

**JavaScript:**

```javascript
function loginWithProvider(provider) {
    window.location.href = `/oauth/${provider}/login`;
}

// Example: Login button click handler
document.getElementById('google-login').addEventListener('click', () => {
    loginWithProvider('google');
});
```

## Authentication Flow

When a user clicks the login link:

1. Structr redirects the user to the provider's authorization URL
2. The user authenticates with the provider (enters credentials, approves permissions)
3. The provider redirects back to Structr's callback URL with an authorization code
4. Structr exchanges the authorization code for an access token
5. Structr retrieves user details from the provider
6. Structr creates or updates the local user node
7. If configured, Structr calls the `onOAuthLogin` method on the User type
8. Structr creates a session and redirects to the configured return URL

## Customizing User Creation

When a user logs in via OAuth for the first time, Structr creates a new user node. You can customize this process by implementing the `onOAuthLogin` lifecycle method on your User type (or a User subtype).

### Method Parameters

The `onOAuthLogin` method receives information about the login through `$.methodParameters`:

| Parameter | Description |
|-----------|-------------|
| `provider` | The name of the OAuth provider (e.g., "google", "github", "auth0") |
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

### Provider-Specific Notes

**Microsoft Entra ID (Azure AD):**
- Uses the `oauth.auth0.*` configuration settings
- Register the application in Azure Portal under "App registrations"
- Configure "Redirect URIs" under Authentication – use `https://your-domain.com/oauth/auth0/auth`
- Add required API permissions (e.g., User.Read, openid, profile)
- For group claims, configure "Token configuration" to include groups
- See the Azure AD examples under Customizing User Creation for group-to-permission mapping

**Google:**
- Enable the "Google+ API" or "People API" in the Google Cloud Console
- Configure OAuth consent screen before creating credentials

**GitHub:**
- Set "Authorization callback URL" in your OAuth App settings
- Request appropriate scopes (e.g., `user:email` for email access)

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
- **Validate user data** - Don't blindly trust data from providers; validate and sanitize
- **Map groups carefully** - Document the relationship between provider groups and Structr permissions
- **Handle token expiration** - OAuth tokens expire; implement refresh logic if needed
- **Log authentication events** - Track logins for security auditing

## Related Topics

- User Management - Users, groups, and the permission system
- JWT Authentication - Token-based authentication, including external JWKS providers for machine-to-machine scenarios
- Two-Factor Authentication - Adding a second factor after OAuth login
- REST Interface/Authentication - Resource Access Permissions and endpoint security
