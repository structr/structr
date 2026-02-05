# FTP

Structr includes a built-in FTP server that provides file access to the virtual filesystem. Users can connect with any FTP client and browse, upload, or download files according to their permissions.

## Configuration

Enable and configure the FTP server in the Configuration Interface or in `structr.conf`:

| Setting | Description | Default |
|---------|-------------|---------|
| `application.ftp.enabled` | Enable FTP server | `false` |
| `application.ftp.port` | FTP port | `8021` |

## Authentication

FTP authentication uses Structr user accounts with password authentication. Users log in with their Structr username and password.

```bash
# Connect with lftp
lftp -p 8021 -u username localhost

# Connect with standard ftp client
ftp localhost 8021
```

## File Visibility

After authentication, the FTP connection shows files and folders based on the user's permissions in Structr's virtual filesystem.

**Regular users** see:
- Files and folders they have read access to
- File owners only for nodes they have read rights on
- Files are hidden if their parent folder is not accessible

**Admin users** see:
- All files and folders in the system
- All file owners

### Example: Regular User

```
$ lftp -p 8021 -u user1 localhost
Password: *****
lftp user1@localhost:~> ls
drwx------   1              0 Jun 30 15:22 testFolder
-rw-------   1 user1      347 Jun 30 09:24 test1.txt
-rw-------   1             25 Jun 30 15:41 test2.txt
-rw-------   1              5 Jun 30 09:24 test3.txt
-rw-------   1 user1        5 Jun 30 09:24 test4.txt
```

Files without visible owner (`test2.txt`, `test3.txt`) belong to users that `user1` cannot see.

### Example: Admin User

```
$ lftp -p 8021 -u admin localhost
Password: *****
lftp admin@localhost:~> ls
drwx------   1 admin        0 Jun 30 15:22 testFolder
-rw-------   1 user1      347 Jun 30 09:24 test1.txt
-rw-------   1 admin       25 Jun 30 09:24 test2.txt
-rw-------   1 user2        5 Jun 30 09:24 test3.txt
-rw-------   1 user1        5 Jun 30 09:24 test4.txt
```

Admin users see all files and their owners.

## Supported Operations

The FTP server supports standard file operations:

| Operation | Description |
|-----------|-------------|
| `ls` / `dir` | List files and folders |
| `cd` | Change directory |
| `get` | Download file |
| `put` | Upload file |
| `mkdir` | Create directory |
| `rm` | Delete file |
| `rmdir` | Delete directory |

All operations respect Structr's permission system. Users can only perform operations they have rights for.

## Use Cases

FTP access is useful for:

- **Bulk file transfers** - Upload or download many files at once
- **Automated backups** - Script file retrieval from Structr
- **Legacy integration** - Connect systems that only support FTP
- **Direct file management** - Use familiar FTP clients instead of the web interface

## Security Considerations

- FTP transmits credentials in plain text. Consider using FTPS or restricting access to trusted networks.
- The FTP server binds to all interfaces by default. Use firewall rules to limit access if needed.
- File permissions in FTP mirror Structr's security model - users cannot access files they don't have rights to.

## Related Topics

- Files & Folders - Structr's virtual filesystem
- Users & Groups - Managing user accounts and permissions
- Security - Access control and permissions
