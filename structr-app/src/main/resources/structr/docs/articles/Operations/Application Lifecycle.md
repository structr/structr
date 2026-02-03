# Application Lifecycle

Structr applications are developed, tested, and deployed through an export/import mechanism that integrates with version control systems like Git. This enables you to work on a local development instance, track changes in a repository, collaborate with team members, and deploy updates to staging or production servers.

The export creates a portable folder structure containing your application's schema, pages, templates, business logic, and configuration. This folder can be committed to a repository, shared with others, or imported into another Structr instance.

## Application vs. Data Deployment

Structr provides two deployment mechanisms: application deployment and data deployment.

Application deployment exports the structure of your application — schema, pages, templates, shared components, widgets, files marked for export, security settings, and business logic. This is everything needed to recreate the application on another system, but it does not include users, groups, or any data created during normal use of the application.

Data deployment exports the actual objects stored in your database. You select which types to export, making this useful for migrating user data, creating test datasets, or synchronizing content between environments.

## Typical Workflow

Application deployment enables collaborative development of Structr applications. Whether you work alone or in a team, the recommended workflow is based on a code repository such as Git.

The typical development cycle:

1. Develop and test changes on a local Structr instance
2. Export the application to a repository folder
3. Commit and push your changes
4. On the target server (staging or production), pull the latest changes
5. Import the application

When working in a team, each developer works on their local instance and merges changes through the repository. Structr does not merge during import — all conflict resolution happens in the version control system.

For detailed setup instructions with Docker and Git, see the Version Control Workflow section below.

Data deployment serves a different purpose. You use it less frequently, typically for initial data migration when setting up a new environment, creating backups of user-generated content, or populating test systems with sample data.

## Application Deployment

Application deployment exports everything that defines your application:

- Schema definitions and business logic from the Schema and Code areas
- Pages, shared components, and templates
- Files and folders marked for export
- Mail templates
- Widgets
- Site configurations
- Localizations
- Security settings (resource access grants)

This export does not include users, groups, or any data objects created by users of your application. You can deploy a new version of your application without affecting the data it operates on.

### Export Methods

You can trigger an application export in three ways:

#### Via Dashboard

Go to the Dashboard, open the Deployment tab, enter an absolute path on the server filesystem into the "Export application to local directory" field, and click the button. The path can be any location where Structr has write access, for example `/var/structr/deployments/my-app`.

To download the export as a ZIP file instead, use the "Export and download application as ZIP file" button.

#### Via Admin Console

Open the Admin Console and enter AdminShell mode. Then run:

```
export /path/to/export/
```

#### Via REST API

```bash
curl -X POST http://localhost:8082/structr/rest/maintenance/deploy \
  -H "Content-Type: application/json" \
  -H "X-User: admin" \
  -H "X-Password: admin" \
  -d '{"mode":"export", "target":"/path/to/export/"}'
```

### Import Methods

Application import is a destructive operation. Structr deletes all existing pages, schema definitions, templates, components, and other application data before importing the new version. User data (users, groups, and objects created by your application) remains untouched.

There is no conflict resolution or merging during import. If you need to merge changes from multiple developers, do this in your version control system before importing.

#### Via Dashboard

Enter the path to an existing export in the "Import application from local directory" field and click the button.

To import from a remote location, enter a URL to a ZIP file in the "Import application from URL" field. The URL must be publicly accessible without authentication. Structr downloads the ZIP file and imports its contents.

#### Via Admin Console

```
import /path/to/application/
```

#### Via REST API

```bash
curl -X POST http://localhost:8082/structr/rest/maintenance/deploy \
  -H "Content-Type: application/json" \
  -H "X-User: admin" \
  -H "X-Password: admin" \
  -d '{"mode":"import", "source":"/path/to/application/"}'
```

### Export Format

Structr exports applications to a specific folder structure. Each component type has its own folder and a corresponding JSON file with metadata:

| Folder/File | Contents |
|-------------|----------|
| `schema/` | Schema definitions and code from the Schema and Code areas |
| `pages/` | Pages from the Pages editor |
| `components/` | Shared components |
| `templates/` | Template elements |
| `files/` | Files from the virtual filesystem (only those marked for export) |
| `mail-templates/` | Mail templates |
| `security/` | Resource access grants |
| `modules/` | Application configuration and module definitions |
| `localizations.json` | Localization entries |
| `sites.json` | Site configurations |
| `widgets.json` | Widgets created in the Pages area |
| `application-configuration-data.json` | Schema layouts from the Schema editor |
| `deploy.conf` | Information about the exporting Structr instance |

Each folder has a corresponding `.json` file (e.g., `pages.json`, `files.json`) containing metadata like visibility flags, content types, and UUIDs for each item.

### Including Files in the Export

Files and folders in the virtual filesystem are not exported by default. To include a file or folder in the export, set the `includeInFrontendExport` flag on it. Child items inherit this flag from their parent folder, so setting it on a folder includes all its contents.

> **Note:** The flag is named `includeInFrontendExport` for historical reasons. It controls inclusion in application deployment exports.

### Pre- and Post-Deploy Scripts

You can include scripts that run automatically before or after import.

#### pre-deploy.conf

If a file named `pre-deploy.conf` is present in the application folder being imported, Structr executes it as a script before importing the data. Use this to create users or groups that are referenced in visibility settings of the exported files but may not exist in the target system.

```javascript
{
    let myUserGroup = $.getOrCreate('Group', 'name', 'myUserGroup');
    let myNestedUserGroup = $.getOrCreate('Group', 'name', 'myNestedUserGroup');

    if (!$.isInGroup(myUserGroup, myNestedUserGroup)) {
        $.addToGroup(myUserGroup, myNestedUserGroup);
    }
}
```

#### post-deploy.conf

If a file named `post-deploy.conf` is present, Structr executes it after the import completes successfully. Use this to create data that must exist in every instance of your application.

```javascript
{
    let necessaryUser = $.getOrCreate('User', 'name', 'necessaryUser');
    let myUserGroup = $.getOrCreate('Group', 'name', 'myUserGroup');

    if (!$.isInGroup(myUserGroup, necessaryUser)) {
        $.addToGroup(myUserGroup, necessaryUser);
    }
}
```

### Version Control Workflow

When running Structr with Docker using custom volume directories, you can integrate deployment with a Git repository. This allows you to store your application in version control and collaborate with other developers.

The typical workflow:

1. Clone your application repository to `./volumes/structr-repository` on the host system
2. Import the application in Structr's Dashboard under Deployment by entering `/var/lib/structr/repository/webapp` in the "Import application from local directory" field
3. Make changes in Structr (schema, pages, business logic, etc.)
4. Export the application by entering `/var/lib/structr/repository/webapp` in the "Export application to local directory" field
5. On the host system, commit and push your changes from `./volumes/structr-repository`
6. To deploy updates, pull the latest changes and repeat from step 2

This workflow keeps your application under version control while allowing you to use Structr's visual editors for development. Merging changes from multiple developers happens in Git, not during Structr import.

## Data Deployment

Data deployment exports the actual objects stored in your database. Unlike application deployment, you explicitly select which types to export. This gives you control over what data to migrate, back up, or synchronize between environments.

Common use cases include:

- Migrating users and groups to a new instance
- Creating backups of user-generated content
- Populating test environments with realistic data
- Synchronizing reference data between environments

### Data Export

#### Via Dashboard

Go to the Dashboard, open the Deployment tab, select the types you want to export, enter an absolute path in the "Export data to local directory" field, and click Export.

#### Via Admin Console

```
export-data /path/to/export/ Type1,Type2,Type3
```

#### Via REST API

```bash
curl -X POST http://localhost:8082/structr/rest/maintenance/deployData \
  -H "Content-Type: application/json" \
  -H "X-User: admin" \
  -H "X-Password: admin" \
  -d '{"mode":"export", "target":"/path/to/export/", "types":"Type1,Type2,Type3"}'
```

### Data Import

Data import adds new objects to the database. If an object with the same UUID already exists, it is replaced with the imported version. Objects that exist in the database but not in the import are left unchanged.

#### Via Dashboard

Enter the path to an existing data export in the "Import data from local directory" field and click the button.

#### Via Admin Console

```
import-data /path/to/data/
```

#### Via REST API

```bash
curl -X POST http://localhost:8082/structr/rest/maintenance/deployData \
  -H "Content-Type: application/json" \
  -H "X-User: admin" \
  -H "X-Password: admin" \
  -d '{"mode":"import", "source":"/path/to/data/"}'
```

### Data Export Format

A data deployment export contains:

| Folder/File | Contents |
|-------------|----------|
| `nodes/` | Export files for the selected node types |
| `relationships/` | Export files for relationships from/to the selected types |
| `pre-data-deploy.conf` | Script that runs before data import |
| `post-data-deploy.conf` | Script that runs after data import |

### Import Behavior

Data import runs without validation by default. Cardinality constraints are not enforced, validation rules are not applied, and `onCreate`/`onSave` methods are not executed. This is because nodes and relationships are imported sequentially, and enabling validation would likely cause errors that stop the import.

After importing data, rebuild the database indexes by going to the Schema area, clicking the Admin button, and selecting "Rebuild all Indexes".

The `pre-data-deploy.conf` and `post-data-deploy.conf` scripts work the same way as their application deployment counterparts.

## Monitoring Progress

You can follow the progress of any export or import operation in the Server Log tab on the Dashboard or via the notifications in the Structr UI.

## Related Topics

- Virtual File System - Managing files and the includeInFrontendExport flag
- Schema - Understanding schema definitions in deployment exports
