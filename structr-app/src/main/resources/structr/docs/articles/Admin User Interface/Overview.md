
The Structr Admin User Interface is a web-based console for building and managing Structr applications. From here, you can design your data model, build pages, manage users, and monitor your running application.


## Quick Reference

| I want to... | Go to                                                                                          |
|--------------|------------------------------------------------------------------------------------------------|
| Define data types and relationships | <a href="javascript:void(0);" onclick="window.parent.location.hash='#schema'">Schema</a>       |
| Write business logic and methods | <a href="javascript:void(0);" onclick="window.parent.location.hash='#code'">Code</a>           |
| View and edit data in the database | <a href="javascript:void(0);" onclick="window.parent.location.hash='#data'">Data</a>           |
| Build web pages and templates | <a href="javascript:void(0);" onclick="window.parent.location.hash='#pages'">Pages</a>         |
| Manage static files (CSS, JS, images) | <a href="javascript:void(0);" onclick="window.parent.location.hash='#sfiles'">Files</a>        |
| Manage users, groups, and permissions | <a href="javascript:void(0);" onclick="window.parent.location.hash='#security'">Security</a>   |
| Export or import my application | <a href="javascript:void(0);" onclick="window.parent.location.hash='#dashboard'">Dashboard</a> |
| Run scripts and queries interactively | Admin Console (Ctrl+Alt+C)                                                                     |


## Interface Structure

The interface is organized around a header bar that stays visible across all areas. The main navigation on the left side of the header takes you to the different functional areas: Dashboard, Pages, Files, Security, Schema, Code, Data, and more. Less frequently used items are available in the burger menu, which also contains the logout link. You can configure which items appear in the main navigation through the UI Settings on the Dashboard.

On the right side of the header, tools are available regardless of which area you are working in:

### Search

The magnifying glass icon opens a global search across all your data.

### Configuration
The wrench icon opens the Configuration Interface in a new browser tab. This separate interface provides access to all runtime settings that control Structr's behavior, from database connections to scheduled tasks. It requires authentication with the superuser password defined in `structr.conf`, adding an extra layer of security for these sensitive operations. For details, see the Configuration Interface section below.

### Admin Console

The terminal icon opens the Admin Console – a Quake-style terminal that slides down from the top of the screen. This is a powerful REPL for executing JavaScript, StructrScript, Cypher queries, and administrative commands. You can also open it with Ctrl+Alt+C.

![Admin Console](dashboard_admin-console.png)

### Notifications

The bell icon shows notifications and system alerts.

## The Main Areas

### Dashboard

This is the default landing page after login. Here you can view system information, check server logs, and use deployment tools to export and import your application.

![Dashboard](dashboard_about-structr.png)

### Pages

This is the visual editor for building web pages. You can use the tree view to see your page structure, drag and drop widgets, and preview your pages in real time.

![Pages](pages_element-details_general.png)

### Files

This is where you manage your static assets – CSS, JavaScript, images, and documents. You can upload files, organize them in folders, and reference them in your pages.

![Files](files_renamed-folder.png)

### Security

Here you can manage users and groups, configure resource access grants, and set up CORS.

![Security](security.png)

### Schema

This is the visual data modeler. Types appear as boxes, relationships as connecting lines. You can drag them to arrange the layout and click to edit their properties.

![Schema](schema.png)

### Code

Here you can write and organize your business logic. The same types as in the Schema area are displayed, but organized for writing and editing methods rather than visualizing relationships.

![Code](code.png)

### Data

Here you can browse and edit the objects in your database. Select a type, view all instances in a table, and edit values directly.

![Data](data.png)

### Flows

This is a visual workflow designer where you can create automated processes and data transformations.

![Flows](flows_run-flow.png)

### Job Queue

This area shows scheduled jobs and background tasks. Jobs created with `$.schedule()` appear here and can be monitored or cancelled. (Note: This area is currently labeled "Importer" in the UI but will be renamed in a future release.)

![Job Queue](importer.png)

### Localization

Here you can manage translations for multi-language applications.

![Localization](localization_created.png)

### Graph

This is an interactive graph explorer where you can visualize your data objects and their relationships.

### Virtual Types

Here you can configure dynamic types that transform or aggregate data from other sources.

### Mail Templates

Here you can create and edit email templates used in automated notifications.

## Browser Compatibility

The Admin UI is supported in Chrome, Firefox, Safari, and Edge. For the best experience, keep your browser updated to the latest version.
