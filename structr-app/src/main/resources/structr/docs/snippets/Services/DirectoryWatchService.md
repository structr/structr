# DirectoryWatchService

Synchronizes Structr folders with directories on the server filesystem. Monitors mounted directories for changes and updates file metadata automatically.

## Configuration

Mount a directory by setting the `mountTarget` property on a Folder's storage configuration. The service watches for file changes and periodically rescans the directory.

## Folder Properties

| Property | Description |
|----------|-------------|
| `mountTarget` | Path to the directory on the server filesystem |
| `mountWatchContents` | Enable filesystem event monitoring for immediate change detection |
| `mountScanInterval` | Seconds between directory rescans (0 = no periodic scan) |

## Settings

| Setting | Description |
|---------|-------------|
| `application.filesystem.followsymlinks` | Follow symbolic links when scanning directories |

## Notes

- Only works on filesystems that support directory watch events
- File contents remain on disk and are not imported into the database
- Changes made to files in Structr are written to the corresponding files on disk
