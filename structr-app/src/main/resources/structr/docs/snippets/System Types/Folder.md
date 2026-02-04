# Folder

Represents directories in Structr's virtual filesystem. The folder structure you see is independent of where files are physically stored – you can reorganize freely without moving actual data. Key properties include `name`, `parent` for the containing folder, and `children` for contained files and subfolders.

## Details

You can mount folders to external storage locations like local directories or cloud providers, with automatic change detection. Each folder can use a different storage backend, so frequently accessed files can live on fast storage while archives go somewhere cheaper. Permissions on folders affect visibility of their contents. You can extend the Folder type or create subtypes for specialized use cases.
