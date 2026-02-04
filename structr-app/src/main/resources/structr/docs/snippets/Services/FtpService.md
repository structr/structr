# FtpService

Provides FTP access to Structr's virtual filesystem. Users authenticate with their Structr credentials and see files according to their permissions.

## Settings

| Setting | Default | Description |
|---------|---------|-------------|
| `application.ftp.port` | 8021 | Port the FTP server listens on |
| `application.ftp.passivePortRange` | – | Port range for passive mode, for example "50000-50100". Required when running in Docker. |
