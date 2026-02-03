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

Statistics are aggregated per time interval to keep memory usage bounded while still providing useful historical data.

### Viewing Statistics

Access statistics are available in two places:

- Dashboard – The "About Structr" tab shows a filterable table with request statistics grouped by timestamp, request count, and HTTP method
- Health Check Endpoint – The `/structr/health` endpoint includes response time statistics in the `html:responseTime` and `json:responseTime` sections

### Configuration

Configure these settings in `structr.conf` or through the Configuration Interface:

| Setting | Default | Description |
|---------|---------|-------------|
| `application.stats.aggregation.interval` | 60000 | Aggregation interval in milliseconds. Statistics are grouped into buckets of this size. |

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

### Endpoint

`/structr/histogram`

### Parameters

| Parameter | Description |
|-----------|-------------|
| `sort` | Sort results by: `total`, `count`, `min`, `max`, `avg` (default: `total`) |
| `top` | Number of results to return (default: 1000) |
| `reset` | If present, clears the histogram data after returning results |

Example: `/structr/histogram?sort=avg&top=100`

### Configuration

Configure these settings in `structr.conf` or through the Configuration Interface:

| Setting | Default | Description |
|---------|---------|-------------|
| `histogramservlet.path` | /structr/histogram | Endpoint path |
| `histogramservlet.whitelist` | 127.0.0.1, localhost, ::1 | IPs allowed to access histogram data |

## Related Topics

- Dashboard - Interactive monitoring in the Admin UI
- Configuration - Server settings
- Maintenance - Maintenance mode for planned downtime
