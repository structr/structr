The Access Control dialog is a standardized interface used across nearly all data types in Structr, with only minor variations based on the specific type you're working with.

![Access Control Dialog](/structr/docs//structr/docs//structr/docs/pages_access-control-dialog.png)

## Owner
At the top of the dialog, you'll see the current owner of the object. Use the dropdown to either assign a new owner or remove ownership entirely. These changes affect only the selected object by modifying its OWNS relationship in the database.

## Visibility
The visibility section lets you control who can see the current object and its children using the familiar visibility flags for authenticated and unauthenticated users. If you check "Apply visibility switches recursively", Structr propagates your visibility settings down through the entire hierarchy, which is especially useful when working with Pages, HTML elements, Templates, and Folders.

## Permissions
The permissions table at the bottom lets you grant read, write, delete, and access control permissions to specific users or groups. Use the dropdown in the first row to add permissions for additional users or groups. In certain contexts, you can apply these permissions recursively to child objects as well. Remove a permission by unchecking the last checkbox in its row. These changes affect only the selected object by modifying its SECURITY relationships in the database.