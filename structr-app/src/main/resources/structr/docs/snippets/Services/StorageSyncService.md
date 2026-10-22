# StorageSyncService

Synchronizes the Structr filesystem (File and Folder nodes) with external storage backends. Storage providers that support synchronization monitor their backend themselves and report changes to this service, which creates, updates, moves and deletes the corresponding nodes. Replaces the former DirectoryWatchService; the old service name is still accepted in `configured.services` as a deprecated alias.

## Configuration

A folder is synchronized when it is directly linked to a StorageConfiguration whose provider supports synchronization. A file linked to such a configuration only stores its content there; it is not scanned or watched. For the local filesystem provider, mount a directory by adding a `mountTarget` entry to the storage configuration. The service watches for changes and periodically rescans the backend.

## Folder Properties

| Property | Description |
|----------|-------------|
| `mountDoFulltextIndexing` | Add discovered files to the fulltext index |

## Storage Configuration Entries

| Entry | Description |
|-------|-------------|
| `mountTarget` | Path to the directory on the server filesystem (local provider) |
| `sync.watchContents` | `true` enables live change monitoring for immediate change detection, where the provider supports it; absent means not watched |
| `sync.scanInterval` | Seconds between rescans; absent or `0` means no periodic scan |
| `sync.fileType` | Node type created for discovered files (default: File) |
| `sync.folderType` | Node type created for discovered folders (default: Folder) |
| `sync.direction` | Direction of synchronization: `in` (external → Structr, the default when absent), `out` (Structr → external), or `both` |
| `sync.onVanished` | What happens to a node whose backing entry vanished from the external storage: `ignore` (keep it, the default when absent), `delete`, or `readonly` (keep it, mark it `isExternallyDeleted` and refuse changes until it reappears). Applies to live watch events and to rescans, inbound only |

## Settings

| Setting | Description |
|---------|-------------|
| `application.filesystem.followsymlinks` | Follow symbolic links when scanning directories |
| `log.directorywatchservice.scanquietly` | Suppress per-scan logging |

## Notes

- Nested folders with their own StorageConfiguration are synchronized independently of their parent
- File contents remain in the external storage and are not imported into the database
- Changes made to files in Structr are written to the external storage by the storage provider
