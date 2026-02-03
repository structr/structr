# Group

Organizes users into logical units for permission management. Instead of granting permissions to individual users, you grant them to groups and add users to those groups. When a user belongs to a group, they inherit all permissions granted to that group, including direct grants, schema-based type-level permissions, and access rights that flow through the permission resolution system. Key properties include `name` for identification and `members` for the collection of users and nested groups.

## Hierarchy and Integration

Groups can contain both users and other groups, enabling hierarchical structures where nested group members automatically inherit permissions from parent groups. In the Admin UI Security area, you manage membership through drag-and-drop. Groups also serve as integration points for external directory services like LDAP, allowing directory groups to map to Structr groups for centralized user management. You can extend the Group type with additional properties or create subtypes for specialized organizational concepts like Department or Team.
