# Security

Structr provides a sophisticated multi-layered security system that combines traditional user and group management with permission resolution either based on data type, schema relationships, or single object properties.

This comprehensive approach allows for flexible and fine-grained access control across your applications.

## Overview

Structr's permission system operates on multiple levels to ensure secure and flexible access control:

- **User and Group Management** - Traditional role-based access control
- **Resource Permissions** - URL-based permission control for REST endpoints
- **Node-level Permissions** - Direct object access control
- **Graph-based Permission Resolution** - Advanced permission propagation through relationships
- **Visibility Flags** - Simple public/private access control

## Users and Groups

### Creating Users

Users can be created through the Admin UI or programmatically via the REST API.

**Via Admin UI:**
1. Navigate to Users and Groups section in the main menu
2. Click "Add User"
3. A new user will be created with a random default name
4. Rename the user as needed
5. Configure user properties through the Edit Properties dialog

**Via REST API:**
```bash
curl --request POST \
  --url http://localhost:8082/structr/rest/User \
  --header 'content-type: application/json' \
  --data '{
    "name": "john.doe",
    "eMail": "john.doe@example.com",
    "password": "securePassword123"
  }'
```

### User Properties

Key user attributes include:

| Property | Description |
|----------|-------------|
| `name` | Username for authentication |
| `eMail` | Email address (often used for login) |
| `password` | User password (stored as hash) |
| `isAdmin` | Administrator flag granting full system access |
| `passwordAttempts` | Failed login attempt counter |

### Setting User Passwords

To set or change a user password:

1. Navigate to Users and Groups view
2. Hover over the desired user
3. Click the properties icon
4. In the Edit Properties dialog, go to Node Properties tab
5. Find the Password field and enter the new password

**Security Note:** Structr never stores cleartext passwords, only secure hash values. Existing passwords are never displayed in the UI.

### Creating Groups

Groups provide a way to organize users and manage permissions collectively.

**Creating a Group:**
1. Navigate to Users and Groups view
2. Click "Add Group"
3. A new group will be created with a random default name
4. Rename the group as appropriate

**Adding Users to Groups:**
- Use drag-and-drop to move users into groups
- Groups can contain both users and other groups
- This allows creation of hierarchical group structures

### Group Inheritance

Access rights granted to a group are automatically inherited by all users in that group. This includes:
- Direct group members
- Users in nested subgroups
- Permissions flow down the group hierarchy

## Permission Levels

Structr implements a multi-layered security check system, executed in the following order:

1. **Administrator Check** - Users with `isAdmin=true` bypass all other checks
2. **Local Visibility Flags** - Simple public/private node flags
3. **Ownership** - Creator/owner-based permissions
4. **Permission Grants** - Explicit user/group permissions on nodes
5. **Graph-based Permission Resolution** - Relationship-based permission propagation

### Permission Types

Structr recognizes four basic permission types:

| Permission | Description |
|------------|-------------|
| **Read** | View object properties and relationships |
| **Write** | Modify object properties |
| **Delete** | Remove objects from the database |
| **AccessControl** | Modify security settings and permissions |

## Resource Permissions

Resource Permissions control access to REST API endpoints based on URL patterns and HTTP methods.

### Purpose

While the security system focuses on user context and database entity access, Resource Permissions control URL-based access to REST resources. For example, accessing `/structr/rest/Project` requires appropriate permissions for the `Project` resource.

### Permission Structure

Each Resource Permission defines:
- **Signature** - The resource path pattern (e.g., `Project`, `_login`, `SchemaType`)
- **HTTP Methods** - Which operations are allowed (GET, POST, PUT, DELETE)
- **User Categories** - Who can access (public users, authenticated users, specific groups)

### Common Permission Examples

| Signature | Purpose | Typical Methods |
|-----------|---------|----------------|
| `_login` | Authentication endpoint | POST |
| `_logout` | Logout endpoint | POST |
| `_token` | JWT token creation | POST |
| `_registration` | User self-registration | POST |
| `_resetPassword` | Password reset | POST |
| `Project` | Project data access | GET, POST, PUT, DELETE |
| `User` | User management | GET, POST, PUT, DELETE |

### Configuration Requirements

**For Authentication Endpoints:**
```
_login - Allow POST for public users
_logout - Allow POST for authenticated users
_token - Allow POST for all users
_registration - Allow POST for public users (if self-registration enabled)
_resetPassword - Allow POST for public users
```

**For Data Types:**
```
TypeName - Configure based on application requirements
TypeName/_Public - For public views (older versions)
```

## Node-Level Permissions

### Direct Permission Grants

Permissions can be granted directly on individual nodes to specific users or groups. These grants override inherited permissions and provide the most specific access control.

**Permission Hierarchy:**
1. Direct user permissions (highest priority)
2. Direct group permissions
3. Inherited group permissions
4. Graph-based resolution (lowest priority)

### Visibility Flags

Simple boolean flags control basic node visibility:

- **`visibleToPublicUsers`** - Node visible to anonymous users
- **`visibleToAuthenticatedUsers`** - Node visible to logged-in users

### Ownership

The creator of a node automatically receives full permissions (Read, Write, Delete, AccessControl) unless explicitly overridden.

## Graph-Based Permission Resolution

### Concept

Graph-based permission resolution allows permissions to propagate through relationships in the graph database. This enables sophisticated domain security models where access to related objects is automatically granted based on relationship configurations.

### Active Relationships

Relationships configured for permission propagation are called "active relationships" and are displayed in a different color in the schema editor.

### Permission Propagation Rules

For each relationship, you can configure how permissions propagate:

| Action | Description |
|--------|-------------|
| **ADD** | Grants additional permissions to users |
| **KEEP** | Maintains existing permissions |
| **REMOVE** | Revokes specific permissions |

### Example: Product Group Access

Consider a schema where users maintain ProductGroups and should access all Products within their groups:

1. User has READ permission on ProductGroup
2. Relationship from ProductGroup to Product is configured to KEEP READ permission
3. User automatically gets READ access to all Products in their ProductGroup
4. User does NOT get access to subgroups unless explicitly configured

### Hidden Properties

During permission propagation, sensitive properties can be hidden from users who gain access through relationship paths:

```
Hidden Properties: price, internalCost, supplierInfo
```

Properties listed in the Hidden Properties field are removed from JSON output for users accessing objects via permission resolution.

### Resolution Process

When a non-admin user accesses a private object:

1. Structr checks for direct permissions
2. If no direct permissions exist, it searches for connected paths
3. It traverses active relationships looking for ADD or KEEP permission rules
4. If a valid path is found, access is granted with potentially hidden properties
5. If no path exists, access is denied

## Security Best Practices

### User Management

- **Use strong passwords** with minimum length requirements
- **Enable account lockout** after failed login attempts (configurable via `security.passwordpolicy.maxfailedattempts`)
- **Grant minimal permissions** following the principle of least privilege
- **Regularly review user permissions** and remove unnecessary access
- **Use groups effectively** to simplify permission management

### Group Organization

- **Create logical group hierarchies** reflecting organizational structure
- **Use descriptive group names** that clearly indicate purpose
- **Implement role-based grouping** (e.g., Administrators, Editors, Viewers)
- **Avoid deeply nested structures** that complicate permission understanding

### Resource Permission Configuration

- **Configure permissions specifically** for your application's needs
- **Avoid overly permissive public access** unless required
- **Test endpoint access** using the REST console in admin mode
- **Document permission configurations** for maintenance and auditing

### Graph-Based Permissions

- **Design clear relationship models** with well-defined permission flows
- **Use hidden properties** to protect sensitive information
- **Test permission paths** thoroughly in development
- **Monitor performance** as complex graphs may impact query speed

## Configuration

### Required Settings

**User Autocreation (for registration):**
```
jsonrestservlet.user.autocreate = true
```

**Auto-login (for password reset):**
```
JsonRestServlet.user.autologin = true
```

**Password Policy:**
```
security.passwordpolicy.maxfailedattempts = 4
```

### Account Lockout Management

When a user exceeds failed login attempts:

1. Account becomes locked automatically
2. `passwordAttempts` property tracks failed attempts
3. Reset by setting `passwordAttempts` to 0 via REST API:

```bash
curl --request PUT \
  --url http://localhost:8082/structr/rest/User/[USER_UUID] \
  --header 'content-type: application/json' \
  --data '{"passwordAttempts": 0}'
```

## Troubleshooting

### Common Permission Issues

**"Access denied" errors:**
- Check Resource Permissions for the specific endpoint
- Verify user authentication status
- Confirm user has appropriate node-level permissions
- Review group memberships and inherited permissions

**Graph permission resolution failures:**
- Verify active relationships are properly configured
- Check for broken permission paths
- Ensure target objects exist and have correct visibility flags
- Test with admin user to isolate permission vs. data issues

**Authentication problems:**
- Verify Resource Permissions for authentication endpoints
- Check configuration settings (autocreate, autologin)
- Review mail configuration for registration/reset features
- Monitor failed login attempts and account lockout status

### Debugging Tools

**Admin Console REST Mode:**
Use the REST console to test endpoint access:
```
Mode set to 'REST'
anonymous@Structr> get /User
anonymous@Structr> auth admin admin
admin@Structr> get /User
```

**Log Analysis:**
Monitor log files for permission-related messages:
```
INFO org.structr.web.auth.UiAuthenticator - Resource permission found for signature 'User', but method 'GET' not allowed for public users.
```

**Permission Path Testing:**
Create test scenarios with known user/group/node configurations to verify expected behavior.

This multi-layered permission system provides both security and flexibility, enabling you to implement sophisticated access control models that match your application's requirements while maintaining clear and manageable permission structures.