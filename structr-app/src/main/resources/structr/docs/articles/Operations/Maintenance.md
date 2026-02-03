# Maintenance

This chapter covers routine maintenance tasks for keeping your Structr instance running smoothly, including the maintenance mode for planned downtime and the process for updating to new versions.

## Maintenance Mode

Maintenance mode allows you to perform updates or other maintenance tasks while showing visitors a friendly maintenance page instead of an error. The Admin UI and all services remain accessible on separate ports, so you can continue working while users see the maintenance notice.

### How It Works

When you enable maintenance mode:

- The main HTTP/HTTPS ports show a maintenance page to all visitors
- The Admin UI and API move to separate maintenance ports
- SSH and FTP services (if enabled) also move to their maintenance ports

This means you can perform maintenance tasks through the Admin UI while users cannot access the application.

### Configuration

Configure maintenance mode in the Configuration Interface under Server Settings → Maintenance:

| Setting | Default | Description |
|---------|---------|-------------|
| `maintenance.enabled` | false | Enable maintenance mode. |
| `maintenance.application.http.port` | 8182 | HTTP port for Admin UI access during maintenance. |
| `maintenance.application.https.port` | 8183 | HTTPS port during maintenance. |
| `maintenance.application.ssh.port` | 8122 | SSH port during maintenance. |
| `maintenance.application.ftp.port` | 8121 | FTP port during maintenance. |
| `maintenance.message` | (default text) | Message shown on the maintenance page. HTML is allowed. |
| `maintenance.resource.path` | (empty) | Path to a custom maintenance page. If empty, the default page with `maintenance.message` is shown. |

### Enabling Maintenance Mode

1. Open the Configuration Interface
2. Navigate to Server Settings → Maintenance
3. Optionally customize the maintenance message or provide a custom page
4. Set `maintenance.enabled` to `true`
5. Save the configuration

The maintenance page appears immediately on the main ports. Access the Admin UI through the maintenance port (default: 8182) to continue working.

### Disabling Maintenance Mode

1. Access the Configuration Interface through the maintenance port
2. Set `maintenance.enabled` to `false`
3. Save the configuration

The application returns to normal operation immediately.

## Updates and Upgrades

Structr follows semantic versioning. Minor version updates (e.g., 5.1 → 5.2) include automatic migration and are generally safe. Major version updates (e.g., 5.x → 6.0) may include breaking changes and require more careful planning.

### Before You Update

1. **Create a backup** – Back up your database and the `files` directory
2. **Export your application** – Create an application deployment export as an additional safeguard
3. **Check the release notes** – Review changes, especially for major versions
4. **For major versions** – Read the migration guide and test the update in a non-production environment first

### Update Process

The update process is straightforward:

1. Enable maintenance mode (optional but recommended for production)
2. Stop Structr: `systemctl stop structr`
3. Install the new version:
   - **Debian package:** `dpkg -i structr-<version>.deb`
   - **ZIP distribution:** Extract and replace the installation files
4. Start Structr: `systemctl start structr`
5. Disable maintenance mode

### Minor Version Updates

Minor versions maintain backward compatibility. Schema and data migrations happen automatically when Structr starts. Monitor the server log during startup to verify the migration completed successfully.

### Major Version Updates

Major versions may include breaking changes to the schema, API, or scripting functions. Always:

- Read the migration guide for your target version
- Test the update in a staging environment
- Verify that your application works correctly before updating production
- Keep your backup until you have confirmed the update was successful

## Related Topics

- Application Lifecycle - Creating backups through application export
- Backup and Recovery - Comprehensive backup strategies
- Health Checks and Monitoring - Monitoring your Structr instance
