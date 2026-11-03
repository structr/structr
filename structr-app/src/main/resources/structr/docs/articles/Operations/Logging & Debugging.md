# Logging & Debugging

This chapter covers the logging system and various debugging techniques available in Structr.

## Server Log

Structr logs all server activity using the Log4J logging API with Logback as the implementation. The Logback configuration lives in the classpath in a file called `logback.xml`.

### Log Location

The server log location depends on your installation method:

- Debian package: `/var/log/structr/server.log`
- ZIP installation: `logs/server.log` in the Structr directory

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
- Command line – Use `tail -f /var/log/structr/server.log` (Debian) or `tail -f logs/server.log` (ZIP) to follow the log
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

| Installation | Server log | Request log | Rotated by |
|--------------|------------|-------------|------------|
| ZIP, Docker | `logs/server.log` | `logs/request.log` | Structr |
| Debian package | `/var/log/structr/server.log` | `/var/log/structr/request.log` | logrotate |

Structr rolls its logs over daily, compresses them with gzip and keeps 30 days, with at most 1 GB in total per log.

The Debian package leaves rotation to the system's logrotate service instead, configured in `/etc/logrotate.d/structr`:

```
/var/log/structr/*.log {
  su root adm
  copytruncate
  daily
  rotate 30
  dateext
  dateformat .%Y-%m-%d-%s
  size 10M
  compress
  delaycompress
  missingok
}
```

The environment variable `LOG_ROTATION` selects who rotates: `structr` (the default) or `external`, which the Debian package sets. With `external`, Structr only appends to the files, so any outside rotator works as long as it truncates the files in place, like logrotate's `copytruncate`. The variables `LOG_FILE` and `REQUEST_LOG_FILE` set other locations.

When rotation is active, the Dashboard Server Log tab shows a log source selector where you can choose between the current and archived log files.

## Logging Configuration

Configure these settings in `structr.conf` or through the Configuration Interface:

| Setting | Default | Description |
|---------|---------|-------------|
| `log.level` | INFO | Default log level |
| `log.requests` | false | Log all incoming HTTP requests to the request log, see [Log Rotation](#log-rotation) for its location |
| `log.requests.format` | ultimate | Format of the request log: `ultimate`, `apache` or `legacy`, see [Request Log](#request-log) |
| `log.querytime.threshold` | 3000 | Log queries taking longer than this (milliseconds) |
| `log.callback.threshold` | 50000 | Log transactions with more callbacks than this |
| `log.functions.shortenstacktrace` | true | Shorten the stack trace of a script exception to the frames before the transition into GraalVM internals. Set to `false` to log the full stack trace. |
| `log.cypher.debug` | false | Log generated Cypher queries |
| `log.cypher.debug.ping` | false | Include WebSocket PING queries in Cypher debug log |
| `log.scriptprocess.commandline` | 2 | Script execution logging: 0=none, 1=path only, 2=path and parameters |
| `log.directorywatchservice.scanquietly` | false | Suppress directory watch service scan messages |

## Request Log

With `log.requests` enabled, Structr writes one line per HTTP request to the request log (`logs/request.log`, or `/var/log/structr/request.log` with the Debian package). Changes to the request log settings take effect on restart.

The format is set with `log.requests.format`:

| Format | Content |
|--------|---------|
| `ultimate` (default) | Apache combined plus the time taken to serve the request, in microseconds |
| `apache` | Apache combined, as written by Apache and nginx |
| `legacy` | The format of earlier Structr versions: date, request time, request line, status and time taken in milliseconds |

`ultimate` is the format that the Screaming Frog Log File Analyser reads with the most detail, and `apache` is understood by practically every log analyser. Both are written exactly as Apache writes them: a missing value is written as `-`, and quotes, backslashes and control characters in a value are escaped, so that no header can break a line. The `ultimate` format corresponds to this Apache configuration:

```
LogFormat "%h %l %u %t \"%r\" %>s %b %D \"%{Referer}i\" \"%{User-Agent}i\"" sf_ultimate
```

Example:

```
66.249.66.1 - - [01/Jan/2017:09:00:00 +0000] "GET /contact.html HTTP/1.1" 200 250 500000 "http://www.example.com/" "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"
```

The fields are the client address, two unused fields, the time the request was received (UTC), the request line, the status code, the size of the response body in bytes, the time taken in microseconds, the referer and the user agent.

Behind a reverse proxy, the client address is the proxy's, unless `httpservice.forwardedfor` is enabled: then Structr takes it from the `X-Forwarded-For` header the proxy sets. Log analysers verify search engine bots by their address, so enable it when Structr runs behind a proxy, and make sure Structr is only reachable through that proxy.

## Logging from Code

Use the `$.log()` function to write messages to the server log from your application code.

**JavaScript:**
```javascript
$.log('Processing order', order.id);
$.log('User logged in:', $.me.name);

// Template string syntax
$.log(`Processing batch ${page} of ${total}`);
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

When enabled, Structr generates a unique debugger URL on each startup. This URL is intentionally unpredictable for security reasons. You can find it in:

- The server log at startup
- The Dashboard in the "About Structr" tab under "Scripting Debugger"

### Connecting Chrome DevTools

1. Copy the debugger URL from the Dashboard or server log
2. Open a new Chrome tab and paste the URL directly into the address bar
3. Press Enter to open DevTools

Note that you must manually paste the URL – clicking links to `chrome://` URLs is blocked by the browser for security reasons.

### Setting Breakpoints

Chrome DevTools does not display your complete application code upfront. Instead, code snippets appear only when execution reaches them. This makes setting breakpoints through the DevTools interface impractical.

To pause execution at a specific point, insert the `debugger` statement directly in your code:

```javascript
{
    let orders = $.find('Order', { status: 'pending' });
    
    debugger;  // Execution pauses here
    
    for (let order of orders) {
        // process order
    }
}
```

When execution hits the `debugger` statement, Chrome DevTools pauses and displays the surrounding code. From there you can:

- Step through code line by line
- Inspect variables and the call stack
- Evaluate expressions in the console
- Continue to the next `debugger` statement or until completion

Remove `debugger` statements before deploying to production.

### Limitations

The debugger pauses the entire request thread while waiting at a breakpoint. Use it only in development environments where blocking requests is acceptable.

## JVM Remote Debugging

For debugging Structr itself or complex Java interop scenarios, you can attach a Java debugger (IntelliJ IDEA, Eclipse, etc.) to the running JVM.

### Enabling Remote Debugging

**Debian package:**

The start script `bin/config` enables the debug agent when the environment variable `ENABLE_STRUCTR_DEBUG` has the value `yes`. The systemd unit does not read an environment file, and a variable exported in your shell does not reach the service, so set it in a systemd override:

```bash
systemctl edit structr
```

Add the following lines, save, and restart the service:

```ini
[Service]
Environment=ENABLE_STRUCTR_DEBUG=yes
```

```bash
systemctl restart structr
```

This enables debugging on port 5005.

**ZIP installation:**

Export `ENABLE_STRUCTR_DEBUG=yes` in the shell that runs `bin/start`, or add the following JVM parameter to the start command:

```
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
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

Structr always logs the stack trace when a function throws an exception, but shortens it by default. Set `log.functions.shortenstacktrace` to `false` to get the full stack trace including the GraalVM frames. The setting replaces the former `log.functions.stacktrace`, with the sense inverted.

### Transaction Problems

The Event Log shows transaction timing. Look for transactions with high callback counts or long validation times.

## Related Topics

- Monitoring – System health and performance metrics
- Configuration – Server settings
- Dashboard – Admin UI features
