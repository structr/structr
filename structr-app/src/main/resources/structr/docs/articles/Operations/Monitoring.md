# Monitoring

Structr provides several ways to monitor the health and performance of your instance: a web-based dashboard for interactive monitoring, and HTTP endpoints for integration with external monitoring systems like Prometheus and Grafana.

## Dashboard Monitoring

The Admin UI Dashboard provides real-time monitoring capabilities:

- Server Log – Live view of the server log with configurable refresh interval
- Event Log – Structured view of API requests, authentication events, and transactions with timing breakdowns
- Threads – List of all running threads with the ability to interrupt or kill stuck threads
- Access Statistics – Filterable table of request statistics

See the Dashboard chapter for details on using these features.

## System Resources

Structr monitors system resources to help you assess server capacity and diagnose performance issues. The Dashboard displays key metrics in the "About Structr" tab.

### Available Metrics

| Metric | Description |
|--------|-------------|
| Processors | Number of CPU cores available to the JVM |
| Free Memory | Currently unused heap memory |
| Total Memory | Heap memory currently allocated by the JVM |
| Max Memory | Maximum heap memory the JVM can allocate (configured via `application.heap.max_size`) |
| Uptime | Time since Structr started |
| Thread Count | Current number of active threads |
| Peak Thread Count | Highest thread count since startup |
| Daemon Thread Count | Number of daemon threads |
| CPU Load Average | System load average (1 minute) |
| Node Cache | Size and usage of the node cache |
| Relationship Cache | Size and usage of the relationship cache |

### Interpreting Memory Values

The three memory values relate to each other as follows:

- Free Memory is the unused portion of Total Memory
- Total Memory grows up to Max Memory as needed
- If Free Memory stays consistently low while Total Memory equals Max Memory, consider increasing the heap size

### Viewing System Resources

System resource information is available in two places:

- Dashboard – The "About Structr" tab shows processors, free memory, total memory, and max memory
- Health Check Endpoint – The `/structr/health` endpoint returns all metrics listed above in a machine-readable JSON format

## HTTP Access Statistics

Structr automatically collects statistics about HTTP requests to your application. These statistics help you understand usage patterns, identify slow endpoints, and detect unusual access behavior.

### Collected Metrics

For each endpoint (HTML pages and REST API), Structr tracks:

- Total request count
- Minimum response time
- Maximum response time
- Average response time

Statistics are grouped into buckets of the aggregation interval and kept for `application.stats.retention.days` days, which keeps memory usage bounded.

### Retention Across Restarts

Structr saves the statistics to `logs/http-access-statistics.json` in its base path every five minutes and when it stops, and restores them at startup, so they survive a restart. A crash loses at most the last five minutes.

The statistics describe the load on this server rather than your application, so they are not stored in the database and are not part of a deployment export. To keep them when you move the instance to another server, copy the `logs/` directory along with it, and include `logs/` in your backups if the history matters to you. Without the file, Structr starts with empty statistics. If you change `application.stats.aggregation.interval`, the saved statistics are discarded at the next start, because buckets of different sizes cannot be combined.

### Viewing Statistics

Access statistics are available in three places:

- Dashboard – The "About Structr" tab shows a filterable table with request statistics grouped by timestamp, request count, and HTTP method
- Health Check Endpoint – The `/structr/health` endpoint includes response time statistics in the `html:responseTime` and `json:responseTime` sections
- Statistics Endpoint – `/structr/rest/stats` returns the request counts as JSON, for monitoring systems and other remote tools

### Statistics Endpoint

`GET /structr/rest/stats` returns one object per HTTP method (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `HEAD`, `OPTIONS`) that has seen requests, each mapping the start of a time bucket, in milliseconds since the epoch, to the number of requests in it.

| Parameter | Description |
|-----------|-------------|
| `interval` | Bucket size in milliseconds (default: 60000). Must be a multiple of `application.stats.aggregation.interval`, otherwise the request is refused with 422 |
| `max` | Only return this many of the most recent buckets per method (default: all) |

Example: `/structr/rest/stats?interval=3600000&max=24` returns the requests per hour for the last 24 hours.

Admin users can always read the endpoint. Other users need a resource permission for `stats` with GET, which lets a monitoring account poll the statistics without admin rights. The endpoint only returns request counts, no paths, addresses or users.

### Configuration

Configure these settings in `structr.conf` or through the Configuration Interface:

| Setting | Default | Description |
|---------|---------|-------------|
| `application.stats.aggregation.interval` | 60000 | Aggregation interval in milliseconds. Statistics are grouped into buckets of this size. |
| `application.stats.retention.days` | 30 | How many days of statistics are kept, in memory and in `logs/http-access-statistics.json`. |

## Health Check Endpoint

The health check endpoint provides machine-readable status information for load balancers, container orchestration systems, and monitoring tools.

### Endpoints

| Endpoint | Purpose |
|----------|---------|
| `/structr/health` | Full health status in JSON format |
| `/structr/health/ready` | Readiness probe (HTTP status only) |

### Readiness Probe

The `/structr/health/ready` endpoint returns only an HTTP status code, making it suitable for Kubernetes readiness probes or load balancer health checks:

- `200 OK` – Structr is ready to accept requests
- `503 Service Unavailable` – Structr is starting up, shutting down, or a deployment is in progress

### Full Health Status

The `/structr/health` endpoint returns detailed status information in the `application/health+json` format:

- Memory utilization (free, max, total)
- CPU load average
- Uptime
- Thread counts (current, peak, daemon)
- Cache statistics (nodes, relationships)
- Response time statistics for HTML pages and REST endpoints

Access to the full health data is restricted by IP whitelist. Requests from non-whitelisted IPs receive only the HTTP status code.

### Configuration

Configure these settings in `structr.conf` or through the Configuration Interface:

| Setting | Default | Description |
|---------|---------|-------------|
| `healthcheckservlet.path` | /structr/health | Endpoint path |
| `healthcheckservlet.whitelist` | 127.0.0.1, localhost, ::1 | IPs allowed to access full health data |

## Prometheus Metrics

Structr exposes metrics in Prometheus format at `/structr/metrics`. This endpoint is designed for scraping by a Prometheus server.

The `MetricsServlet` is not part of the default servlet list. To activate it, open the Configuration Interface, navigate to Servlet Settings, add `MetricsServlet` to the list of enabled servlets in `httpservice.servlets`, save the configuration, and restart the HTTP service. Use the Configuration Interface rather than editing `structr.conf` by hand for this setting: the value in `structr.conf` replaces the default list instead of extending it, so a line containing only `MetricsServlet` would disable all other servlets.

### Available Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `structr_http_requests_total` | Counter | Total HTTP requests (labels: method, path, status) |
| `structr_http_request_duration_seconds` | Histogram | Request duration (labels: method, path) |

In addition to Structr-specific metrics, standard JVM metrics are exposed (memory, garbage collection, threads, etc.).

### Configuration

Configure these settings in `structr.conf` or through the Configuration Interface:

| Setting | Default | Description |
|---------|---------|-------------|
| `metricsservlet.path` | /structr/metrics | Endpoint path |
| `metricsservlet.whitelist` | 127.0.0.1, localhost, ::1 | IPs allowed to access metrics |

### Prometheus Configuration

To scrape metrics from Structr, add a job to your Prometheus configuration:

```yaml
scrape_configs:
  - job_name: 'structr'
    static_configs:
      - targets: ['localhost:8082']
    metrics_path: /structr/metrics
```

If Prometheus runs on a different machine, add its IP address to the whitelist in `structr.conf`:

```
metricsservlet.whitelist = 127.0.0.1, localhost, ::1, 10.0.0.50
```

### Grafana Dashboard

A pre-built Grafana dashboard for Structr is available at [grafana.com/grafana/dashboards/16770](https://grafana.com/grafana/dashboards/16770). You can import it using the dashboard ID `16770`.

## Query Histogram

The histogram endpoint provides detailed query performance analysis, useful for identifying slow queries and optimization opportunities.

It covers every database query since Structr started, grouped by statement: how often each statement ran, and its total, maximum, minimum and average time, from sending the query until its last result was read. Up to 10,000 distinct statements are listed individually; any further ones are counted together in one entry. The statistics are cleared on restart.

### Endpoint

`/structr/histogram`

### Parameters

| Parameter | Description |
|-----------|-------------|
| `sort` | Sort results by: `total`, `count`, `min`, `max`, `avg` (default: `total`) |
| `top` | Number of results to return (default: 1000) |

Example: `/structr/histogram?sort=avg&top=100`

### Configuration

Configure these settings in `structr.conf` or through the Configuration Interface:

| Setting | Default | Description |
|---------|---------|-------------|
| `histogramservlet.path` | /structr/histogram | Endpoint path |
| `histogramservlet.whitelist` | 127.0.0.1, localhost, ::1 | IPs allowed to access histogram data |

## Server Log Endpoint

The server log endpoint returns the server log over HTTP, for example to collect it from a script or a monitoring tool without opening the dashboard. Only admin users can read it; a resource permission for another user is refused with 403, because the log records everything the instance has been sent.

### Endpoint

`GET /structr/rest/_serverLog`

### Entries

The endpoint reads the log as entries: a line that starts with a timestamp begins an entry, and the lines without one that follow it, such as a stack trace, belong to it. `level` and `filter` select whole entries, so an error always comes back with its stack trace.

### Parameters

| Parameter | Description |
|-----------|-------------|
| `lines` | Number of lines to return (default: 50). Entries are never cut, so the response can contain a few more |
| `level` | Minimum level: `TRACE`, `DEBUG`, `INFO`, `WARN` or `ERROR`. `level=WARN` returns warnings and errors. The level is read from the level column, not from the message |
| `filter` | Only return entries that contain this text, in any of their lines |
| `since` | The `offset` of a previous response. Returns only what was logged after it, oldest first |
| `logFile` | The log file to read, one of `availableLogFiles` in the response (default: the current log file). Rotated logs are listed too, newest first, including the ones compressed with gzip; `since` cannot be used with a compressed log |

Without `since`, the endpoint returns the most recent entries. With `since`, it reads forward from that offset and returns the oldest new entries first, up to `lines`; the `offset` of the response then points to the first entry not yet returned, so the next request continues there and nothing is skipped.

### Response

| Field | Description |
|-------|-------------|
| `lines` | The lines of the selected entries, oldest first |
| `offset` | The byte position to pass as `since` in the next request; empty for a compressed log |
| `rotated` | `true` if `since` lay beyond the end of the file. The log was rotated, and the new file was read from the start |
| `logFile` | The file that was read |
| `availableLogFiles` | All log files that can be read: the current one first, then the rotated ones, newest first |

### Watching for Errors

To watch an instance for errors, make one request without `since` and keep its `offset`, then poll with `since` and `level=ERROR`:

```
GET /structr/rest/_serverLog?lines=1
GET /structr/rest/_serverLog?since=48213377&level=ERROR&lines=500
```

Each poll reads only the bytes logged since the previous one, so its cost does not depend on the size of the log, and a poll every few seconds is cheap. A line that is still being written is left for the next poll. Log in once and reuse the session or a token for the polls; sending the password with every request makes each one pay for the password hash.

## Related Topics

- Dashboard - Interactive monitoring in the Admin UI
- Configuration - Server settings
- Maintenance - Maintenance mode for planned downtime
