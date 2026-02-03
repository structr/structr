
Structr provides built-in rate limiting to protect your application from being overwhelmed by too many requests. When enabled, requests that exceed the configured threshold are delayed, throttled, or rejected. This helps protect against denial-of-service attacks and misbehaving clients.

Rate limiting is disabled by default. To enable it, set `httpservice.dosfilter.ratelimiting` to `Enabled` in the Configuration Interface under DoS Filter Settings.

### How It Works

The rate limiter tracks requests per client IP address. When a client exceeds the allowed requests per second:

1. Initial excess requests are delayed by a configurable amount
2. If the client continues, requests are throttled (queued)
3. If the queue fills up, additional requests are rejected with an HTTP error code

This graduated response allows legitimate users who briefly spike their request rate to continue with a slight delay, while persistent offenders are blocked.

### Configuration

| Setting | Default | Description |
|---------|---------|-------------|
| `httpservice.dosfilter.ratelimiting` | Disabled | Enable or disable rate limiting. |
| `httpservice.dosfilter.maxrequestspersec` | 10 | Maximum requests per second before throttling begins. |
| `httpservice.dosfilter.delayms` | 100 | Delay in milliseconds applied to requests exceeding the limit. |
| `httpservice.dosfilter.maxwaitms` | 50 | Maximum time in milliseconds a request will wait for processing. |
| `httpservice.dosfilter.throttledrequests` | 5 | Number of requests that can be queued for throttling. |
| `httpservice.dosfilter.throttlems` | 30000 | Duration in milliseconds to throttle a client. |
| `httpservice.dosfilter.maxrequestms` | 30000 | Maximum time in milliseconds for a request to be processed. |
| `httpservice.dosfilter.maxidletrackerms` | 30000 | Time in milliseconds before an idle client tracker is removed. |
| `httpservice.dosfilter.insertheaders` | Enabled | Add rate limiting headers to responses. |
| `httpservice.dosfilter.remoteport` | Disabled | Include remote port in client identification. |
| `httpservice.dosfilter.ipwhitelist` | (empty) | Comma-separated list of IP addresses exempt from rate limiting. |
| `httpservice.dosfilter.managedattr` | Enabled | Enable JMX management attributes. |
| `httpservice.dosfilter.toomanycode` | 429 | HTTP status code returned when requests are rejected. |

### Monitoring

When rate limiting activates, Structr logs warnings with details about the affected client:

```
DoS ALERT: Request delayed=100ms, ip=192.168.1.100, overlimit=OverLimit[id=192.168.1.100, duration=PT0.016S, count=10], user=null
```

The log entry shows the IP address, the delay applied, and the request count that triggered the limit.

### Whitelisting Trusted Clients

Internal services or monitoring systems may need to make frequent requests without being throttled. Add their IP addresses to the whitelist:

```
httpservice.dosfilter.ipwhitelist = 10.0.0.1, 10.0.0.2, 192.168.1.50
```

Whitelisted IPs are completely exempt from rate limiting.
