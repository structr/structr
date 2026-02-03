# Authentication

REST endpoints are protected by a security layer called Resource Access Permissions that controls which endpoints each type of user can access. This article explains how to configure these permissions and how to set up CORS for cross-origin requests.

For general information about authentication methods (sessions, JWT, OAuth, two-factor authentication), see the Security chapter.

## Resource Access Permissions

Non-admin users require explicit permission to fetch data from REST endpoints. Resource Access Permissions define which endpoints each user category can access. Consider the following request:

**curl:**

```bash
curl -s http://localhost:8082/structr/rest/User
```

**Response:**

```json
{
    "code": 401,
    "message": "Forbidden",
    "errors": []
}
```

Access to the User collection was denied. If you look at the log file, you can see a warning message because access to resources without authentication is prohibited by default:

```
2020-04-19 11:40:15.775 [qtp1049379734-90] INFO  o.structr.web.auth.UiAuthenticator - Found no resource access permission for anonymous users with signature 'User' and method 'GET'.
```

### Signature

Resource Access Permissions consist of a signature and a set of flags that control access to individual REST endpoints. The signature of an endpoint is based on its URL, replacing any UUID with `_id`, plus a special representation for the view (the view's name, capitalized and with a leading underscore).

The signature of a schema method equals its name, but capitalized. The following table shows examples for different URLs and the resulting signatures:

| Type | URL | Signature |
|------|-----|-----------|
| Collection | `/structr/rest/Project` | Project |
| Collection with view | `/structr/rest/Project/ui` | Project/_Ui |
| Collection with view | `/structr/rest/Project/info` | Project/_Info |
| Object with UUID | `/structr/rest/Project/362cc05768044c7db886f0bec0061a0a` | Project/_id |
| Object with UUID and view | `/structr/rest/Project/362cc05768044c7db886f0bec0061a0a/info` | Project/_id/_Info |
| Subcollection | `/structr/rest/Project/362cc05768044c7db886f0bec0061a0a/tasks` | Project/_id/Task |
| Schema Method | `/structr/rest/Project/362cc05768044c7db886f0bec0061a0a/doUpdate` | Project/_id/DoUpdate |

### Finding the Correct Signature

If access to an endpoint is denied because of a missing Resource Access Permission, you can find the required signature in the log file:

```
Found no resource access permission for anonymous users with signature 'User/_id' and method 'GET'.
```

### Flags

The flags property of a Resource Access Permission is a bitmask based on an integer value where each bit controls one permission. You can either set all flags at once with the corresponding integer value, or click the checkboxes in the Admin UI to toggle individual permissions.

## Anonymous Access

With the default configuration, anonymous users cannot access any endpoints. To allow anonymous access to an endpoint, you must grant permission explicitly and separately for each HTTP method. Use the "Non-authenticated Users" flags in Resource Access Permissions for this purpose.

**Without endpoint access permission:**

```bash
curl -s http://localhost:8082/structr/rest/Project
```

```json
{
    "code": 401,
    "message": "Forbidden",
    "errors": []
}
```

**With endpoint access permission:**

```bash
curl -s http://localhost:8082/structr/rest/Project
```

```json
{
    "result": [],
    "query_time": "0.000127127",
    "result_count": 0,
    "page_count": 0,
    "result_count_time": "0.000199823",
    "serialization_time": "0.001092944"
}
```

Now you can access the endpoint, but you still don't see any data because no project nodes are visible for anonymous users. Visibility is controlled separately through visibility flags on each object (see User Management in the Security chapter).

## Authenticated Users

With the default configuration, non-admin users cannot access any endpoints. To allow non-admin users access to an endpoint, you must grant permission explicitly and separately for each HTTP method. Use the "Authenticated Users" flags in Resource Access Permissions for this purpose.

## Cross-Origin Resource Sharing (CORS)

When your frontend runs on a different domain than your Structr backend, browsers block requests by default. This security feature is called the same-origin policy. CORS headers tell browsers which cross-origin requests to allow.

### When You Need CORS

CORS configuration is required when:

- Your frontend is served from a different domain than Structr
- You're developing locally with a frontend on a different port
- You're building a single-page application that calls the Structr API

### CORS Settings

Each CORS entry configures response headers for a URL path:

| Setting | HTTP Header | Purpose |
|---------|-------------|---------|
| Accepted Origins | `Access-Control-Allow-Origin` | Which domains can make requests (`*` for any) |
| Max Age | `Access-Control-Max-Age` | How long browsers cache preflight responses (seconds) |
| Allow Methods | `Access-Control-Allow-Methods` | Which HTTP methods are permitted |
| Allow Headers | `Access-Control-Allow-Headers` | Which request headers clients can send |
| Allow Credentials | `Access-Control-Allow-Credentials` | Whether to include cookies |
| Expose Headers | `Access-Control-Expose-Headers` | Which response headers JavaScript can access |

### Common Patterns

**For development with a local frontend:**

| Setting | Value |
|---------|-------|
| Path | /structr/rest |
| Accepted Origins | http://localhost:3000 |
| Allow Methods | GET, POST, PUT, DELETE, OPTIONS |
| Allow Headers | Content-Type, Authorization |
| Allow Credentials | true |

**For a public API:**

| Setting | Value |
|---------|-------|
| Path | /structr/rest |
| Accepted Origins | * |
| Allow Methods | GET, POST |
| Allow Headers | Content-Type |

Configure CORS settings in the Security area of the Admin UI under the CORS tab.

## Related Topics

- Security - Authentication methods, users, groups, and the permission system
- Data Access - Once authentication is configured, this article explains how to read, create, update, and delete objects
- Admin UI / Security - How to manage users, groups, and Resource Access Permissions in the Admin UI
