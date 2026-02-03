# Logging & Debugging

This chapter covers the logging system and various debugging techniques available in Structr.

## Server Log

Structr logs all server activity using the Log4J logging API with Logback as the implementation. The Logback configuration lives in the classpath in a file called `logback.xml`.

### Log Location

The server log location depends on your installation method:

- Debian package: `/var/log/structr.log`
- ZIP installation: `logs/structr.log` in the Structr directory

### Custom Log Configuration

The default `logback.xml` includes a reference to an optional `logback-include.xml` file where you can add custom settings. This is useful for changing the log level of individual Java packages to gain more detailed insight into internal processes.

Example `logback-include.xml` to enable debug logging for REST requests:

```xml
<included>
    <logger name="org.structr.rest" level="DEBUG"/>
</included>
```

Place this file in the same directory as `logback.xml` (typically the Structr installation directory or classpath).

### Viewing the Log

You can view the server log in several ways:

- Dashboard – The Server Log tab shows the log in real-time with configurable refresh interval
- Command line – Use `tail -f /var/log/structr.log` (Debian) or `tail -f logs/structr.log` (ZIP) to follow the log
- Log file – Open the file directly in a text editor

### Log Format

Each log entry follows the format:

```
Date Time [Thread] Level Logger - Message
```

Example:
```
2026-02-03 14:30:45.123 [qtp123456-42] INFO  o.s.rest.servlet.JsonRestServlet - GET /structr/rest/User
```

The components are:

- Date and time with milliseconds
- Thread name in brackets
- Log level (DEBUG, INFO, WARN, ERROR)
- Logger name (abbreviated package and class)
- The actual message

### Log Levels

Structr supports the standard log levels. Set the default level via `log.level` in `structr.conf`:

| Level | Description |
|-------|-------------|
| ERROR | Serious problems that need immediate attention |
| WARN | Potential issues that do not prevent operation |
| INFO | Normal operational messages (default) |
| DEBUG | Detailed information for troubleshooting |

Changes to `log.level` take effect immediately without restart.

For more granular control, use `logback-include.xml` to set different log levels for specific Java packages. This allows you to enable debug logging for one component while keeping other components at INFO level.

### Log Rotation

The Debian package includes automatic log rotation via the system's logrotate service. The default configuration:

- Rotates daily when the log exceeds 10MB
- Keeps 30 days of history
- Compresses old log files
- Log location: `/var/log/structr.log`

The configuration file is located at `/etc/logrotate.d/structr`:

```
/var/log/structr.log {
  su root adm
  copytruncate
  daily
  rotate 30
  dateext
  dateformat .%Y-%m-%d-%s
  size 10M
  compress
  delaycompress
}
```

If you installed Structr from the ZIP package, log rotation is not configured automatically. You can either set up logrotate manually or implement your own log management strategy.

When rotation is active, the Dashboard Server Log tab shows a log source selector where you can choose between the current and archived log files.

## Logging Configuration

Configure these settings in `structr.conf` or through the Configuration Interface:

| Setting | Default | Description |
|---------|---------|-------------|
| `log.level` | INFO | Default log level |
| `log.requests` | false | Log all incoming HTTP requests |
| `log.querytime.threshold` | 3000 | Log queries taking longer than this (milliseconds) |
| `log.callback.threshold` | 50000 | Log transactions with more callbacks than this |
| `log.functions.stacktrace` | false | Log full stack traces for function exceptions |
| `log.cypher.debug` | false | Log generated Cypher queries |
| `log.cypher.debug.ping` | false | Include WebSocket PING queries in Cypher debug log |
| `log.scriptprocess.commandline` | 2 | Script execution logging: 0=none, 1=path only, 2=path and parameters |
| `log.directorywatchservice.scanquietly` | false | Suppress directory watch service scan messages |

## Logging from Code

Use the `$.log()` function to write messages to the server log from your application code.

**JavaScript:**
```javascript
$.log('Processing order', order.id);
$.log('User logged in:', $.me.name);

// Template string syntax
$.log()`Processing batch ${page} of ${total}`);
```

**StructrScript:**
```
${log('Processing order', order.id)}
```

Log messages appear at INFO level with the logger name indicating the source location.

## JavaScript Debugging

Structr includes a JavaScript debugger based on GraalVM that integrates with Chrome DevTools.

### Enabling the Debugger

Set `application.scripting.debugger` to `true` in `structr.conf` or the Configuration Interface, then restart Structr.

When enabled, the debugger URL appears in:

- The server log at startup
- The Dashboard in the "About Structr" tab under "Scripting Debugger"

### Connecting Chrome DevTools

1. Open Chrome and navigate to `chrome://inspect`
2. Click "Configure" and add the debugger host and port
3. Your Structr instance appears under "Remote Target"
4. Click "inspect" to open DevTools

### Using the Debugger

Once connected, you can:

- Set breakpoints in your JavaScript code
- Step through code line by line
- Inspect variables and the call stack
- Evaluate expressions in the console

Note that the debugger pauses the entire request thread, so use it only in development environments.

## JVM Remote Debugging

For debugging Structr itself or complex Java interop scenarios, you can attach a Java debugger (IntelliJ IDEA, Eclipse, etc.) to the running JVM.

### Enabling Remote Debugging

**Debian package:**

Set the environment variable before starting Structr:

```bash
export ENABLE_STRUCTR_DEBUG=yes
systemctl restart structr
```

This enables debugging on port 5005.

**ZIP installation:**

Add the following JVM parameter to the start command or configuration:

```
-Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n
```

Parameters:

- `address=5005` – The port the debugger listens on
- `server=y` – Structr waits for debugger connections
- `suspend=n` – Start immediately without waiting for debugger (use `y` to pause until debugger connects)

### Connecting Your IDE

In IntelliJ IDEA:

1. Run → Edit Configurations → Add New → Remote JVM Debug
2. Set host to your server address and port to 5005
3. Click Debug to connect

In Eclipse:

1. Run → Debug Configurations → Remote Java Application
2. Set connection properties and click Debug

## Permission Debugging

When troubleshooting access control issues, add the `logPermissionResolution` parameter to your request:

```
GET /structr/rest/User?logPermissionResolution=true
```

This logs detailed information about how permissions are resolved for each object in the response, showing which grants or restrictions apply and why.

## Thread Inspection

The Dashboard Threads tab shows all running threads in the JVM. This helps identify:

- Stuck or hung requests
- Infinite loops in code
- Deadlocks between threads
- Long-running operations

Each thread shows its name, state, and stack trace. You can interrupt or kill threads directly from the interface, though killing threads should be used with caution as it may leave data in an inconsistent state.

## Event Log Debugging

The Dashboard Event Log provides structured information about requests and transactions:

- Authentication events with user details
- REST and HTTP requests with timing
- Transaction details including:
  - Changelog update time
  - Callback execution time
  - Validation time
  - Indexing time

Use the timing breakdown to identify performance bottlenecks in your application.

## Common Debugging Scenarios

### Package-specific Logging

To debug a specific area without flooding the log, create a `logback-include.xml` file:

```xml
<included>
    <!-- Debug REST API -->
    <logger name="org.structr.rest" level="DEBUG"/>
    
    <!-- Debug WebSocket communication -->
    <logger name="org.structr.websocket" level="DEBUG"/>
    
    <!-- Debug scripting -->
    <logger name="org.structr.core.script" level="DEBUG"/>
</included>
```

### Slow Queries

Enable `log.cypher.debug` to see the generated Cypher queries, then analyze them for:

- Missing indexes
- Inefficient patterns
- Large result sets

The `log.querytime.threshold` setting automatically logs queries exceeding the threshold.

### Permission Issues

Use `logPermissionResolution=true` on requests to see exactly how access is granted or denied.

### JavaScript Errors

Enable `log.functions.stacktrace` to get full stack traces when functions throw exceptions.

### Transaction Problems

The Event Log shows transaction timing. Look for transactions with high callback counts or long validation times.

## Related Topics

- Monitoring – System health and performance metrics
- Configuration – Server settings
- Dashboard – Admin UI features
