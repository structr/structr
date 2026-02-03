# Dashboard

The Dashboard provides system information, server logs, deployment tools, and configuration options for the Admin UI. This is the default landing page after login and gives you a quick overview of the system state.

![Dashboard](dashboard_about-structr.png)

## About Me

This tab shows information about the currently logged-in user. You can verify your identity, check which groups you belong to, and see your active sessions. This is useful when troubleshooting permission issues or when working with multiple accounts.

The tab displays:

- Username and UUID
- Email address
- Working directory
- Session IDs
- Group memberships

The Session ID is particularly useful for debugging. If you need to see your application from another user's perspective, you can copy their Session ID from the table and set it in your browser. This allows you to experience exactly what that user sees without knowing their password.

![About Me](dashboard_about-me.png)

## About Structr

This tab shows detailed information about the Structr server instance. You can verify which version is running, which modules are available, and whether your license is valid.

![About Structr](dashboard_about-structr.png)

### Version and Modules

The version number identifies the exact build you are running. Indicators show whether newer releases or snapshots are available. Below the version, you will see a list of all active modules. Modules extend Structr's functionality – for example, the PDF module adds PDF generation capabilities, and the Excel module enables spreadsheet import and export.

### License and Database

The license section shows your licensee name, host ID, and the validity period (start and end date). You need the host ID when requesting a license from Structr.

The database section shows which driver is in use. Structr supports both embedded and external Neo4j databases.

### UUID Format

This displays the current UUID format. Structr supports UUIDs with and without dashes. The format is configured at installation time and should not be changed afterwards.

### Runtime Information

This section shows server resource information: number of processors, free memory, total memory, and maximum memory. You can monitor these values to assess server capacity and diagnose performance issues.

### Scripting Debugger

This shows whether the GraalVM scripting debugger is active. The debugger allows you to set breakpoints and step through JavaScript code using Chrome DevTools. To enable it, set `application.scripting.debugger = true` in `structr.conf`. See [Debugging JavaScript Code](#placeholder-debugging) for details.

### Access Statistics

This is a filterable table showing request statistics: timestamps, request counts, and HTTP methods used. You can use this to analyze usage patterns and identify unusual access behavior.

## Deployment

This tab provides tools for exporting and importing Structr applications and data.

![Deployment](dashboard_deployment.png)

### Application Deployment

The upper section handles application deployment – exporting and importing the structure of your application (schema, pages, files, templates, security settings, configuration).

Four options are available:

- **Export application to local directory** – Enter an absolute path on the server filesystem and click the button to export
- **Export and download application as ZIP file** – Downloads the export directly to your browser
- **Import application from local directory** – Enter the path to an existing export and click to import
- **Import application from URL** – Enter a URL to a ZIP file and click to download and import

### Data Deployment

The lower section handles data deployment – exporting and importing the actual objects in your database.

- **Export data to local directory** – Select which types to export, enter a path, and click to export
- **Import data from local directory** – Enter the path to an existing data export and click to import

You can follow the progress of any export or import operation in the Server Log tab or via the notifications in the UI.

For details on the export format, pre/post-deploy scripts, and alternative deployment methods, see the Deployment chapter in Operations.

## User-Defined Functions

This tab displays a table of all user-defined functions in the system. You can view and execute any function directly from this interface.

Each function is listed with its name and can be executed by clicking on it. This provides a quick way to run maintenance tasks, test functions, or trigger administrative operations without using the API or Admin Console.

## Server Log

This tab displays the server log in real-time. The log contains technical information about what Structr is doing: startup messages, errors, warnings, request processing, and transaction details.

![Server Log](dashboard_server-log.png)

### Controls

The log refreshes every second by default. You can click inside the log area to pause auto-refresh when you need to read a specific message. The available controls are:

- Copy to clipboard
- Download log file
- Refresh interval (1–10 seconds, or manual)
- Number of lines to display
- Log source selection (Structr supports multiple log files when rotation is enabled)

### Log Format

Each log entry follows the format: `Date Time [Thread] Level Logger - Message`

For example:
```
2026-01-28 09:40:18.126 [main] INFO org.structr.Server - Starting Structr 6.1-SNAPSHOT
```

The log levels are INFO (normal operation), WARN (potential issues that do not prevent operation), and ERROR (problems that need attention).

## Event Log

This tab shows a structured view of system events: API requests, authentication events, transactions, and administrative actions. Unlike the server log which contains free-form text, the event log presents events as filterable table rows with consistent columns.

![Event Log](dashboard_event-log.png)

### Event Types

The following event types are tracked:

- Authentication – Login and logout events with user information
- Rest – API requests with method, path, and user details
- Http – Page requests and OAuth login attempts
- Transaction – Database transactions with performance metrics (changelog updates, callbacks, validation, indexing times)
- Maintenance – Administrative commands

### Using the Event Log

The event log does not auto-refresh. Click the refresh button to update it. You can filter by event type to focus on specific activities. The transaction events include timing breakdowns that can help you identify performance bottlenecks.

## Threads

This tab lists all threads running in the Java Virtual Machine. Each row shows the thread name, state, and stack trace. You can use this tab to diagnose hanging requests, infinite loops, or deadlocks.

![Running Threads](dashboard_running-threads.png)

### Thread Management

Two actions are available for each thread:

- Interrupt – Requests graceful termination
- Kill – Forces immediate termination (use with caution)

Long-running threads may indicate problems in your application code, such as infinite loops or deadlocks.

## UI Settings

This tab lets you configure the Admin UI appearance and behavior. Changes take effect immediately and are stored per user.

![UI Configuration](dashboard_ui-config.png)

### Menu Configuration

Here you can configure which items appear in the main navigation bar and which are moved to the burger menu. This lets you prioritize the areas you use most frequently.

### Font Settings

You can set the main font, font size, and monospace font for the Admin UI. The monospace font is used in code editors and log displays.

### Behavior Settings

This section contains checkboxes for various UI behaviors, grouped by area:

- Global – Notification display and behavior
- Dashboard – Compact deployment UI option
- Pages – Inheritance behavior when creating elements, default tab selection
- Security – Group display, visibility flags in tables
- Importer – Job notifications
- Schema/Code – Database name display
- Code – Recently visited elements
- Data – Array size display limits

Note that the settings relevant to a specific area also appear in a Settings menu within that area. For example, the Pages settings are available both here and in the Pages area's own Settings menu. This allows you to adjust settings without navigating back to the Dashboard.
