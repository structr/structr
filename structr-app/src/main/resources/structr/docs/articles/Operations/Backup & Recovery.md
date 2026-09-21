# Backup & Recovery

A Structr installation stores data in two separate locations: the graph database (Neo4j) holds all objects and relationships, while binary file contents and configuration are stored in the Structr installation directory. A complete backup must include both.

## What to Back Up

| Component | Location | Contains |
|-----------|----------|----------|
| Database | Neo4j data directory | All objects, relationships, schema, users, permissions |
| Structr directory | Structr installation directory | Binary files, configuration, certificates, scripts |

The Structr directory contains several important subdirectories and files:

- `files/` – Uploaded files, images, documents (binary content)
- `structr.conf` – Server settings, credentials, customizations
- `scripts/` – Host scripts registered for execution
- SSL certificates and keystores
- Other runtime configuration

The database and Structr directory must be backed up together to maintain consistency. A file referenced in the database must exist in the `files/` directory, and vice versa.

## Application Backup

To back up your application structure without data, use the Deployment Export feature. This creates a portable folder containing schema definitions, pages, templates, components, and configuration files that can be stored in version control.

Application backups are useful for:

- Version control of your application
- Deploying the same application to multiple environments
- Recovering the application structure after a fresh database setup

See the Application Lifecycle chapter for details on deployment exports.

## Graph Dump with the sync Command

The `sync` maintenance command writes the whole graph, or a subset selected by a Cypher query, to a single ZIP file and reads such a file back in. Unlike a deployment export it contains the raw nodes and relationships including users, groups and application data, so it is a way to move or restore the complete database content without touching the Neo4j data directory. It is admin only and runs through the REST maintenance endpoint:

```
POST /structr/rest/maintenance/sync
{ "mode": "export", "file": "/backups/structr-2026-09-21.zip" }
```

| Parameter | Description |
|-----------|-------------|
| `mode` | `export` writes nodes, relationships and the binary content of files into the ZIP; `exportDb` writes the graph only, without file content; `import` reads a ZIP created by either mode |
| `file` | Path of the ZIP file on the server (required) |
| `query` | Cypher query that selects the export set, for example `MATCH (n) RETURN n`; without it the whole graph is exported |
| `validate` | Whether imported nodes are validated (default `true`) |
| `batchSize` | Transaction batch size for large imports |

Importing adds the objects from the file to the existing database; it does not clear the database first. Restore into an empty database or run `clearDatabase` beforehand when a clean state is required.

## Full Backup (Cold Backup)

A cold backup taken with all services stopped is the most reliable way to back up a Structr installation. It guarantees consistency between the database and binary files.

### Server Installation

1. Stop Structr: `systemctl stop structr`
2. Stop Neo4j: `systemctl stop neo4j`
3. Back up the following:
   - Neo4j data directory (typically `/var/lib/neo4j/data/`)
   - Structr installation directory (typically `/usr/lib/structr/` for Debian packages)
4. Start Neo4j: `systemctl start neo4j`
5. Start Structr: `systemctl start structr`

### Docker Installation

1. Stop the containers: `docker-compose down`
2. Back up the Docker volumes:
   - Neo4j data volume
   - Structr data volume (files, configuration, scripts)
3. Start the containers: `docker-compose up -d`

You can find your volume locations with `docker volume inspect <volume-name>`.

### VM Snapshots

If Structr and Neo4j run on the same virtual machine, creating a VM snapshot is the simplest backup method. Stop both services before taking the snapshot to ensure consistency.

## Restore

### Server Installation

1. Stop Structr: `systemctl stop structr`
2. Stop Neo4j: `systemctl stop neo4j`
3. Replace the Neo4j data directory with the backup
4. Replace the Structr installation directory with the backup
5. Start Neo4j: `systemctl start neo4j`
6. Start Structr: `systemctl start structr`

### Docker Installation

1. Stop the containers: `docker-compose down`
2. Replace the volume contents with the backup data
3. Start the containers: `docker-compose up -d`

## Backup Strategy Recommendations

- Schedule backups during low-traffic periods to minimize downtime
- Test restore procedures regularly in a non-production environment
- Keep multiple backup generations (daily, weekly, monthly)
- Store backups in a separate location from the production system
- Document your backup and restore procedures

## Related Topics

- Application Lifecycle - Deployment export and import
- Configuration - Server settings and file locations
- Maintenance - Maintenance mode for planned downtime
