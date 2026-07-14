# StorageSyncService

Synchronizes the Structr filesystem (File and Folder nodes) with external storage backends. Storage providers that support synchronization monitor their backend themselves and report changes to this service, which creates, updates, moves and deletes the corresponding nodes. Replaces the former DirectoryWatchService; the old service name is still accepted in `configured.services` as a deprecated alias.

## Configuration

A file or folder is synchronized when it is directly linked to a StorageConfiguration whose provider supports synchronization. For the local filesystem provider, mount a directory by adding a `mountTarget` entry to the storage configuration. The service watches for changes and periodically rescans the backend.

## Folder Properties

| Property | Description |
|----------|-------------|
| `mountWatchContents` | Enable live change monitoring for immediate change detection |
| `mountScanInterval` | Seconds between rescans (0 = no periodic scan) |
| `mountTargetFileType` | Node type created for discovered files (default: File) |
| `mountTargetFolderType` | Node type created for discovered folders (default: Folder) |
| `mountDoFulltextIndexing` | Add discovered files to the fulltext index |

## Storage Configuration Entries

| Entry | Description |
|-------|-------------|
| `mountTarget` | Path to the directory on the server filesystem (local provider) |
| `sync.direction` | Direction of synchronization: `in` (external → Structr, the default when absent), `out` (Structr → external), or `both` |
| `sync.deleteStale` | If `true`, rescans delete nodes whose backing entry vanished from the external storage (inbound scans only) |

## Settings

| Setting | Description |
|---------|-------------|
| `application.filesystem.followsymlinks` | Follow symbolic links when scanning directories |
| `log.directorywatchservice.scanquietly` | Suppress per-scan logging |

## Notes

- Nested folders with their own StorageConfiguration are synchronized independently of their parent
- File contents remain in the external storage and are not imported into the database
- Changes made to files in Structr are written to the external storage by the storage provider
