# Group

Organizes users for easier permission management. Instead of granting permissions to individual users, you grant them to groups – when a user joins a group, they automatically inherit all its permissions. Key properties include `name` and `members` for the collection of users and nested groups.

## Details

Groups can contain other groups, so you can build hierarchies where permissions flow down automatically. In the Admin UI, you manage membership via drag-and-drop. Groups also serve as integration points for LDAP, letting you map external directory groups to Structr. You can extend the Group type or create subtypes like Department or Team.
