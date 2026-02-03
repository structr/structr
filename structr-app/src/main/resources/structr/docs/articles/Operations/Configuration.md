# Configuration

Structr is configured through the `structr.conf` file located in the installation directory. This file uses a simple key-value format where each line contains a setting name and its value, separated by an equals sign.

## File Location

The location of `structr.conf` depends on how you installed Structr:

| Installation Type | File Location |
|-------------------|---------------|
| Debian Package | `/usr/lib/structr/structr.conf` |
| ZIP Distribution | `./structr.conf` (in the Structr directory) |

## Configuration Interface

The preferred way to edit configuration settings is through the Configuration Interface in the Admin UI. This interface displays all available settings organized by category, shows default values, and provides descriptions for each setting.

You can access the Configuration Interface by clicking the wrench icon in the Admin UI header bar. The interface opens in a new browser tab and requires authentication with the superuser password.

![Configuration Interface](configuration-interface.png)

After making changes, click the green button in the lower right corner to save them to `structr.conf`. Individual settings can be reset to their default value using the red button with the white X next to each field — this takes effect immediately. The interface also provides a reload function to apply configuration changes without restarting Structr.

## How Settings Are Stored

Structr has a large number of settings with built-in default values. The `structr.conf` file only stores settings that differ from these defaults. This keeps the file compact and makes it easy to see what has been customized.

A fresh installation contains only a single entry:

```
superuser.password = <generated-password>
```

As you customize your installation through the Configuration Interface, additional settings appear in the file. Settings that match their default values are not written to the file.

## Editing the File Directly

While the Configuration Interface is the recommended approach, you can also edit `structr.conf` directly with a text editor. This is useful for automation, version control, or when you need to configure Structr before starting it for the first time.

Each setting goes on its own line:

```
superuser.password = mysecretpassword
application.title = My Application
httpservice.maxfilesize = 1000
```

After editing the file manually, changes take effect after restarting Structr or using the reload function in the Configuration Interface.

## Essential Settings

While Structr has many configuration options, these are the settings you are most likely to need when setting up and running an instance.

| Category | Setting | Default | Description |
|----------|---------|---------|-------------|
| Instance | `application.instance.name` | (empty) | A name displayed in the top right corner of the Admin UI. |
| Instance | `application.instance.stage` | (empty) | A stage label (e.g., "DEV", "STAGING", "PROD") displayed alongside the instance name. |
| HTTP | `application.http.port` | 8082 | HTTP port. Requires restart. |
| HTTP | `application.https.port` | 8083 | HTTPS port. Requires restart. |
| HTTP | `application.https.enabled` | false | Enable HTTPS. Requires a keystore with SSL certificate. |
| Memory | `application.heap.min_size` | 1g | Minimum Java heap size (e.g., `512m`, `1g`). Requires restart. |
| Memory | `application.heap.max_size` | 4g | Maximum Java heap size (e.g., `2g`, `4g`, `8g`). Requires restart. |
| Storage | `files.path` | `<install-dir>/files` | Storage location for uploaded files and the virtual file system. |
| Logging | `log.level` | INFO | Log verbosity: `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`. Takes effect immediately. |
| Admin | `initialuser.name` | admin | Username for the initial admin. |
| Admin | `initialuser.password` | admin | Password for the initial admin. Change immediately after first login. |

## File Permissions

The `structr.conf` file contains sensitive information including database credentials and encryption keys. Restrict access to this file:

```bash
chmod 600 structr.conf
```

This ensures only the file owner can read and write the configuration. Other users on the system cannot access it.

When using Neo4j as database, also follow the [Neo4j file permission recommendations](https://neo4j.com/docs/operations-manual/current/configuration/file-locations/#file-locations-permissions).

## Data-at-Rest Encryption

To protect data stored on disk in case of physical hardware theft, enable filesystem-level encryption on the operating system. This is called data-at-rest encryption and must be configured at the OS level — Structr does not provide this directly.

Consult your operating system documentation for options like LUKS (Linux), BitLocker (Windows), or FileVault (macOS).

## Related Topics

- Email - SMTP configuration
- OAuth - Authentication provider settings
- JWT Authentication - Token settings
- Two-Factor Authentication - 2FA settings
