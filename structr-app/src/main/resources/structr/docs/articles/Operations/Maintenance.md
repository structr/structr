# Maintenance

This chapter covers routine maintenance tasks for keeping your Structr instance running smoothly, including maintenance commands for database operations, the maintenance mode for planned downtime, and the process for updating to new versions.

## Maintenance Commands

Maintenance commands perform administrative operations on the database and application, such as rebuilding indexes, migrating data, or clearing caches. You can execute them through the Admin UI, the REST API, or programmatically in scripts.

### Executing via Admin UI

The Schema area provides access to common maintenance commands through the Admin menu:

#### Indexing – Nodes

- Rebuild Index – Recreates indexes for all or selected node types
- Add UUIDs – Adds UUIDs to nodes that lack one
- Create Labels – Creates Neo4j labels based on the type property

#### Indexing – Relationships

- Rebuild Index – Recreates indexes for relationships
- Add UUIDs – Adds UUIDs to relationships

#### Maintenance

- Flush Caches – Clears internal caches
- Clear Schema – Removes all custom types (use with caution)

### Executing via REST API

Send a POST request to the maintenance endpoint:

```
POST /structr/rest/maintenance/<command>
Content-Type: application/json

{
    "parameter1": "value1",
    "parameter2": "value2"
}
```

For example, to rebuild the index for a specific type:

```
POST /structr/rest/maintenance/rebuildIndex
Content-Type: application/json

{
    "type": "Article"
}
```

### Executing via Script

Use the `maintenance()` function to run commands from StructrScript or JavaScript. This requires admin privileges.

#### JavaScript

```javascript
{
    $.maintenance('rebuildIndex', { type: 'Article' });
}
```

#### StructrScript

```
${maintenance('rebuildIndex', 'type', 'Article')}
```

In StructrScript, pass parameters as alternating key-value pairs. In JavaScript, pass a map as the second argument.

### Available Commands

For a complete list of maintenance commands and their parameters, see the [Maintenance Commands](/structr/docs/ontology/References/Maintenance%20Commands) reference.

## Maintenance Mode

Maintenance mode allows you to perform updates or other maintenance tasks while showing visitors a friendly maintenance page instead of an error. The Admin UI and all services remain accessible on separate ports, so you can continue working while users see the maintenance notice.

### How It Works

When you enable maintenance mode:

- The main HTTP/HTTPS ports show a maintenance page to all visitors
- The Admin UI and API move to separate maintenance ports
- SSH and FTP services (if enabled) also move to their maintenance ports

This means you can perform maintenance tasks through the Admin UI while users cannot access the application.

### Configuration

Configure the ports and the maintenance page in the Configuration Interface under Server Settings → Maintenance:

| Setting | Default | Description |
|---------|---------|-------------|
| `maintenance.application.http.port` | 8182 | HTTP port for Admin UI access during maintenance. |
| `maintenance.application.https.port` | 8183 | HTTPS port during maintenance. |
| `maintenance.application.ssh.port` | 8122 | SSH port during maintenance. |
| `maintenance.application.ftp.port` | 8121 | FTP port during maintenance. |
| `maintenance.message` | (default text) | Message shown on the maintenance page. HTML is allowed. |
| `maintenance.resource.path` | (empty) | Path to a custom maintenance page. If empty, the default page with `maintenance.message` is shown. |

### Enabling Maintenance Mode

1. Open the Configuration Interface
2. Optionally customize the maintenance message or provide a custom page under Server Settings → Maintenance and save the configuration
3. Open the Maintenance tab and click the Enable button

The maintenance page appears immediately on the main ports. Access the Admin UI through the maintenance port (default: 8182) to continue working. The state is stored in the hidden setting `maintenance.enabled`, which does not appear among the editable settings. You can also switch the mode from a script or the REST API with the `maintenanceMode` maintenance command and the parameter `action` set to `enable` or `disable`.

### Disabling Maintenance Mode

1. Access the Configuration Interface through the maintenance port
2. Open the Maintenance tab and click the Disable button

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
   - **Docker:** Change the image tag to the new version and recreate the container. If you use a moving tag such as `structr/structr:7`, run `docker compose pull` first to fetch the new image. The [Configuration](/structr/docs/ontology/Operations/Configuration) chapter describes what each tag follows.
4. Start Structr: `systemctl start structr`
5. Disable maintenance mode

## Migrations

Structr migrates schema and data at startup. What it does with the database is governed by
`application.migration.mode`:

| mode | effect |
| --- | --- |
| `apply` (default) | The steps migrate for real. |
| `dry-run` | Every step runs, logs what it would change, and the change is rolled back. Startup then stops with exit status 4. |
| `off` | No migrations run at all. |

`dry-run` stops startup on purpose. A rolled back schema migration leaves the compiled schema in memory
out of step with the database, so the instance must not go on to serve requests. Read the report in the
server log, then set the mode to `apply` and start again.

It is a diagnostic to be run deliberately, which is why it is not the default: a service that refuses to
start is restarted by whatever supervises it, and a dry run changes nothing, so it finds the same work to
do on every restart. The Debian unit carries `RestartPreventExitStatus=4` so that one dry run leaves the
service stopped rather than looping. Under Docker or Kubernetes there is no such exemption, and the
container will restart until the mode is changed. To preview a migration without stopping anything, use
the `migrate` maintenance command on a running instance instead, which defaults to `dry-run`.

Each step is rolled back on its own, so a dry run never holds more in one transaction than that step
would have committed by itself.

`off` starts the instance against the database as it is. That is only useful when a migration is itself
the problem, because the application may not work correctly against unmigrated data.

Three of the steps are reporting steps that run in `apply` and `dry-run` alike: the check for notion
properties that need attention, the report on calls to the crypt functions, and the report on calls to the
HTTP functions that still use the pre-7.0 signature. The last one is not purely read-only: in `apply` mode
it rewrites the calls it can translate unambiguously and reports the rest for manual migration.

### Running a Migration Without Restarting

The `migrate` maintenance command runs the same steps on a running instance, with the mode as a
parameter rather than from the configuration:

```
POST /structr/rest/maintenance/migrate
{ "mode": "dry-run" }
```

```
POST /structr/rest/maintenance/migrate
{ "mode": "apply" }
```

`mode` defaults to `dry-run`. Unlike at startup, a dry run here does not stop anything: the instance is
already running, and the rollback leaves the database as it was. The results go to the server log.

On a cluster the migrations run on the coordinator only, so the command does nothing on other members.

### Minor Version Updates

Minor versions maintain backward compatibility. Schema and data migrations run when Structr starts,
subject to `application.migration.mode` above. Monitor the server log during startup to verify the
migration completed successfully.

### Major Version Updates

Major versions may include breaking changes to the schema, API, or scripting functions. Always:

- Read the migration guide for your target version
- Test the update in a staging environment
- Verify that your application works correctly before updating production
- Keep your backup until you have confirmed the update was successful

## Related Topics

- Application Lifecycle - Creating backups through application export
- [Backup & Recovery](/structr/docs/ontology/Operations/Backup%20%26%20Recovery) - Comprehensive backup strategies
- [Monitoring](/structr/docs/ontology/Operations/Monitoring) - Monitoring your Structr instance
