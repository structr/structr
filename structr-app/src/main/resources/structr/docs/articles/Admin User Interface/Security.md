# Security

The Security area is where you manage access control for your application. Here you create users and organize them into groups, define which REST endpoints are accessible to authenticated and anonymous users, and configure cross-origin request settings for browser-based clients. The permission model supports both role-based access through groups and fine-grained object-level permissions. Each of these concerns has its own tab.

![Security](/structr/docs/security.png)

## Users and Groups

The first tab, "Users and Groups", displays two lists side by side: users on the left, groups on the right. Both lists are paginated and filterable, which is helpful when you have many users.

### Creating Users and Groups

Click the Create button above either list to add a new user or group. If you've extended the User or Group types (by creating subclasses or adding the User trait to another type), a dropdown appears next to the button that lets you choose which type to create.

### Organizing Your Security Model

You can drag users onto groups to make them members, and drag groups onto other groups to create hierarchies. This flexibility lets you model complex organizational structures: departments containing teams, teams containing members, with permissions flowing through the hierarchy.

### Editing Users

Click a user to edit the name inline. For more options, hover over the user and click the menu icon to open the context menu.

#### General Dialog

Here you can edit essential user properties: name, password, and email address. Three flags control special behaviors:

- Is Admin User – Grants full access, bypassing all permission checks
- Skip Security Relationships – Optimizes performance for users who do not need fine-grained permissions
- Enable Two-Factor Authentication – Adds an extra security layer for this user
- Two-Factor Authentication confirmed for this User - this user has previously logged in via 2FA and will not be shown the setup QR code on next login
- Device Trust Possible for this User - Per-user setting to allow/disallow device trust (depends on global configuration)

The Failed Login Attempts counter (useful for diagnosing lockouts) and the Confirmation Key (used during self-registration) are also available here. The button "Rotate Device Trust Secret" replaces the secret that signs the device trust cookies of this user, which invalidates all devices the user has marked as trusted.

#### Advanced Dialog

This shows all user attributes in a raw table format.

#### Test as This User

This creates a link that opens a browser session as the user, so that you can see the application exactly as that user sees it, without knowing the password and without changing the account. The entry is offered for every user who is neither blocked nor an admin: a session as an admin would hand out full access, and this is for seeing the application as an ordinary user.

Enter the address the test should start at and click "Create link". Open the link in a private window or in another browser: opened in the window you are working in, it would replace your own session. The link works once and for two minutes. After that, create a new one.

Any address of the instance can carry the link's parameter, `_structrOneTimeToken`: a page of the application, a REST endpoint, or the Admin UI itself. A page then answers with the same address without the parameter, so the token does not stay in the address bar or the history.

The session ends after 60 minutes without activity, or earlier if the session timeout of the instance is shorter, and when the user logs out in that window. Every session opened this way is recorded in the Event Log as an Authentication event with the name of the admin who created the link. Only admins can create such a link; it cannot be created from a script. The feature is on by default and can be switched off with the setting `security.testsessions.enabled`; the entry then disappears from the menu and existing links stop working.

#### Security Dialog

This opens the access control dialog for the user object itself.

#### Delete User

This removes the account.

See the User Management chapter for detailed explanations of these settings.

### Editing Groups

Groups have names and members but fewer special properties. Click to edit the name inline. Use the context menu to access the Advanced dialog (all attributes), Security dialog (access control for the group object), or Delete Group. Groups of type LDAPGroup additionally offer the entry "LDAP Config", which opens the dialog for the LDAP path and filter settings of that group.

#### Type Permissions

The properties dialog of a group (double-click the group, or open the Advanced dialog) has a "Type Permissions" tab. It lists the types the members of the group may read, write or delete, or whose access control they may change, for all objects of the type at once. These are the schema-based permissions that the Code area shows for each type and all groups; here you see and change them for one group across all types.

By default, the tab only lists the types the group already has permissions on. Use "Add type..." to add another type, or "Show all types" to list every type of the schema, and the filter field to find a type by its name. A change is saved as soon as you click a checkbox. When you clear the last checkbox of a type, its permission is removed.

## Resource Permissions

The second tab, "Resource Permissions", controls which REST endpoints are accessible and to whom.

### The Resource Access Table

The "Filter/Search..." input above the table narrows the list down to grants whose signature contains the entered text. Each row represents a grant with:

- **Signature** – The URL pattern this grant applies to
- **Permissions** – Checkboxes for each HTTP method (GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH), separately for authenticated and non-authenticated users

### Creating Grants

Enter a signature in the input field next to the Create button and click Create. For details on signature syntax and configuration patterns, see the User Management chapter.

### Per-User and Per-Group Grants

Resource Access grants are themselves objects with their own access control. Click the lock icon at the end of any row to open the access control dialog for that grant.

This means you can create multiple grants for the same signature, each visible to different users or groups. One grant might allow read-only access for regular users, while another allows full access for administrators. Each user sees only the grants that apply to them.

### Visibility Options

The Settings menu on the right side of the tab bar includes options for showing visibility flags and bitmask columns in the table. The bitmask is a numeric representation of the permission flags, which can be useful for debugging.

## CORS Settings

The third tab, "CORS Settings", configures Cross-Origin Resource Sharing settings.

### The CORS Table

Each row configures CORS for one URL path. Enter a path in the input field, click Create, then fill in the columns. A "Filter/Search..." input above the table filters the rows by request URI.

#### Accepted Origins

This specifies which domains can make requests. Use `*` to allow any origin, or list specific domains like `https://example.com`. This becomes the `Access-Control-Allow-Origin` header.

#### Max Age

This tells browsers how long to cache the CORS preflight response, in seconds. Higher values reduce preflight requests but delay the effect of configuration changes.

#### Allow Methods

This lists which HTTP methods are permitted: `GET, POST, PUT, DELETE`, etc.

#### Allow Headers

This specifies which request headers clients can send: `Content-Type, Authorization`, etc.

#### Allow Credentials

This controls whether browsers include cookies and HTTP authentication with cross-origin requests.

#### Expose Headers

This determines which response headers JavaScript can access. By default, only a few headers are exposed; list additional ones here.

The delete button is in the second column.

For details on CORS concepts and configuration patterns, see the Authentication chapter in REST Interface.

## Related Topics

- User Management – Concepts behind users, groups, and permissions
- REST Interface/Authentication – Resource Access Permissions and CORS
