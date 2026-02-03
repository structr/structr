# Folder

Represents directories in Structr's virtual filesystem. Folders organize files and other folders into a hierarchical tree structure where the logical organization exists independently of physical storage. Key properties include `name` for identification, `parent` for the containing folder, and `children` for the collection of contained files and subfolders.

## Mounting and Permissions

Folders can be mounted to external storage locations, allowing integration of local directories or cloud storage providers with automatic change detection through filesystem events or periodic scanning. Each folder can use a different storage backend, enabling scenarios where frequently accessed files live on fast local storage while archives go to cheaper cloud storage. Permissions on folders affect visibility of contained files, as files are hidden if their parent folder is not accessible. You can extend the Folder type with additional properties or create subtypes for specialized organizational concepts.
