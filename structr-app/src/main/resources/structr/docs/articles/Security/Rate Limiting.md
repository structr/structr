
Structr can limit how many requests a single client makes per second, to protect an instance from being overwhelmed by one client sending far too much: a runaway script, a broken retry loop, a crawler that ignores every convention, or someone deliberately hammering the server from one machine.

Be clear about the limits of this, though. It caps what a **single** client can do; it is not protection against a distributed denial-of-service attack. A distributed attacker spreads the load over many addresses, each staying comfortably under the limit, so the limiter never triggers. Defend against that at the edge, with a reverse proxy, CDN or web application firewall.

Rate limiting is disabled by default. To enable it, set `httpservice.ratelimiting.enabled` to `Enabled` in the Configuration Interface under Rate Limiting.

### How It Works

Each client gets a *leaking bucket*. The bucket holds up to `bucketsize` requests and drains continuously at `maxrequestspersecond`. A client may therefore burst up to the bucket size and is then held to the sustained rate.

The burst allowance is what makes this usable in front of a web application: a single page load pulls in stylesheets, scripts, images and REST calls all at once, easily dozens of requests in a moment, and that must not be treated as an attack.

A request that fits in the bucket is served normally. A request that does not is **not** served:

1. it is held for `rejectdelay` milliseconds, which slows the client down without occupying a request thread
2. it is then answered with `rejectstatus` (429 by default)
3. if the delay queue is already full, it is rejected straight away, without the delay

The delay is deliberate backpressure before the refusal, not a grace period: an over-limit request is always refused in the end, never served late. That makes the bucket size the number that matters most: if it does not comfortably cover a real page load, ordinary use will draw 429s rather than merely feel slow.

Only inbound HTTP requests are limited. Outbound requests made by your own scripts are not affected.

### Identifying Clients

Clients are identified by their remote address, which has one important consequence: if Structr runs behind a reverse proxy, every request appears to come from the proxy, so all users share a single bucket and one busy user can throttle everyone else.

To fix that, enable `httpservice.forwardedfor`, which takes the client address from the `X-Forwarded-For` / `Forwarded` headers instead.

Only enable this when Structr is genuinely reachable through a trusted reverse proxy. Those headers are ordinary request headers, so if clients can reach Structr directly they can set them themselves, claim any address they like, evade the limit entirely and make the logs untrustworthy.

The proxy must **overwrite** the header, not append to it. Structr is open source, so an attacker knows the exempt addresses without having to guess: with forwarded-for enabled and a proxy that passes a client-supplied `X-Forwarded-For` through, sending `X-Forwarded-For: 127.0.0.1` claims the loopback address, which `excludeaddresses` exempts by default. That is a complete bypass of rate limiting from a single header. In nginx use `proxy_set_header X-Forwarded-For $remote_addr` (which replaces the value) rather than `$proxy_add_x_forwarded_for` (which appends to whatever the client sent), and make sure Structr cannot be reached except through the proxy.

### Stricter Limits for Sign-In

The login and token endpoints get their own, much lower limit, configured by `httpservice.ratelimiting.auth.maxrequestspersecond` (5 by default). Guessing credentials only needs a handful of requests per second to be effective, so those endpoints deserve a far tighter budget than page serving. The paths follow the configured `loginservlet.path` and `tokenservlet.path`.

Its bucket is separate too, and deliberately small (`auth.bucketsize`, 10). The burst allowance is what an attacker gets for free before the sustained rate applies, so a large bucket here would let a hundred guesses through at once however low the rate is. Signing in is a single request rather than a burst, so a handful is enough to absorb retries and typos.

Set the rate to `0` to drop the separate limit and let the general one apply.

Self-registration and password reset have their own independent rate limits, under Security settings (`security.emailratelimit.*`).

### Configuration

| Setting | Default | Description |
|---------|---------|-------------|
| `httpservice.ratelimiting.enabled` | Disabled | Enable or disable rate limiting. |
| `httpservice.ratelimiting.maxrequestspersecond` | 100 | Sustained requests per second per client. |
| `httpservice.ratelimiting.bucketsize` | 100 | How large a burst a client may make before the sustained rate is enforced. |
| `httpservice.ratelimiting.idletimeout` | 1000 | How long, in milliseconds, an empty bucket is kept before the client is forgotten. |
| `httpservice.ratelimiting.maxtrackers` | 100000 | Maximum number of clients tracked at once, bounding the limiter's memory use. |
| `httpservice.ratelimiting.rejectdelay` | 1000 | How long, in milliseconds, an over-limit request is held before it is rejected. `0` rejects immediately. |
| `httpservice.ratelimiting.rejectqueuesize` | 1000 | How many delayed requests are held at once. |
| `httpservice.ratelimiting.rejectstatus` | 429 | Status code for a rejected request. 429 or 503 are the usual choices. |
| `httpservice.ratelimiting.excludeaddresses` | `127.0.0.1,::1` | Addresses or CIDR ranges exempt from rate limiting. |
| `httpservice.ratelimiting.excludepaths` | (empty) | Path specs exempt from rate limiting, for example `/structr/metrics/*`. |
| `httpservice.ratelimiting.auth.maxrequestspersecond` | 5 | Sustained requests per second on the login and token endpoints. `0` disables the separate limit. |
| `httpservice.ratelimiting.auth.bucketsize` | 10 | Burst allowed on the login and token endpoints. Keep small: the burst is what an attacker gets before the rate applies. |
| `httpservice.ratelimiting.rejectuntracked` | Disabled | Whether to refuse requests once `maxtrackers` addresses are already tracked. |
| `httpservice.ratelimiting.log.escalateafter` | 10 | Refusals from one address within the window that escalate the log entry to an error with full detail. |
| `httpservice.ratelimiting.log.distinctclients` | 20 | Distinct refused addresses within the window reported as a probable distributed flood. |
| `httpservice.forwardedfor` | Disabled | Take the client address from the forwarded-for headers, server-wide. Only behind a trusted proxy. Listed under HTTP Settings, not Rate Limiting. |

Two more settings shape the logging, and they live under **Logging** rather than Rate Limiting because they apply to every throttled log statement, not just refused requests:

| Setting | Default | Meaning |
|---------|---------|-------------|
| `log.throttle.window` | 60000 | Length of the throttling window in milliseconds. |
| `log.throttle.maxlines` | 200 | Hard ceiling on throttled entries per window per log site, whatever the caller varies. `0` removes it. |

Both take effect immediately, with no restart. They are shared on purpose: the authentication paths throttle their own logging the same way, and they do so whether or not rate limiting is enabled, which it is not by default. Turning rate limiting off therefore does not stop this throttling, and raising `log.throttle.maxlines` while chasing a problem raises it everywhere at once.

### What Gets Logged

Refused requests are logged with their remote address, because that address is what you need to block the source at firewall or host level, which is where a serious flood has to be stopped.

The logging is deliberately graduated, so that everyday overshoots stay quiet and real trouble stands out:

- **A client's first refusal in the window** is a warning naming the method, path and address. A brief overshoot is an everyday event and needs no more than that.
- **A client still being refused after `log.escalateafter` requests** in the same window is logged as an error with the full picture: address, method, path, user agent and the count. This is a single source flooding, and it names what to block.
- **`log.distinctclients` different addresses refused within one window** is logged once as an error, reporting the number of addresses and total refusals. Many sources at once is a distributed flood, which per-client rate limiting cannot stop; it has to be shed upstream.

The logging is itself rate limited, in two ways: each address is logged at most twice per `log.throttle.window`, and `log.throttle.maxlines` caps the entries per window no matter how many addresses are involved. The second ceiling matters because a flood that rotates its source address defeats per-address throttling on its own: the address table is bounded, so returning addresses look like first offences again. Once the ceiling is reached, refusals are still counted and totalled when the window ends, just not logged one by one. That is on purpose. Logging every refused request would let a flood of thousands of requests per second turn into thousands of log lines per second, exhausting disk or the log pipeline, which simply does the attacker's work for them.

Request bodies, cookies and `Authorization` headers are never logged, since that would put credentials into the log.

For lower-level detail you can raise `org.eclipse.jetty.server.handler.DoSHandler` to `DEBUG`, which logs every tracking decision. Leave that off in production; it logs per request.

If you know earlier Structr versions: the old `DoS ALERT: ...` warning per delayed request is gone, replaced by the throttled logging above.

### Under a Serious Attack

Structr does not stop serving when it is attacked, and that is deliberate. Shutting the HTTP service down would turn a partial problem into a total outage, take the administration interface offline with it, and hand an attacker a cheap way to kill the instance outright: they would only need to trip the threshold, which costs far less than sustaining a flood. Rate limiting already degrades in the right direction, refusing the offending client while everyone else continues to be served.

For loads beyond what per-client limiting can absorb, escalate in this order:

1. **`rejectuntracked`**: once `maxtrackers` addresses are being tracked, further ones are unlimited by default. Setting this to `Enabled` refuses them instead, capping the damage of a flood spread over very many addresses. Note that it will also refuse legitimate clients that arrive while the table is full.
2. **Shed at the connection level** with the settings below, which act before a request even exists.
3. **Block upstream.** Take the addresses from the error entries described above and block them at the firewall, or put a reverse proxy, CDN or web application firewall in front. This is the only layer that can absorb a genuinely large or distributed attack, because it stops the traffic before it reaches the application at all.

### Load Shedding

Rate limiting counts *requests*, so it cannot see a flood that never becomes a complete request: thousands of half-open connections, or a storm of connection attempts. Three connection-level guards close that gap, acting at TCP accept, before connection setup, TLS handshake and HTTP parsing. All are disabled by default, because a cap set too low locks out legitimate users, the administrator included.

| Setting | Default | Description |
|---------|---------|-------------|
| `httpservice.connections.max` | 0 | Maximum simultaneous connections; beyond it new ones are not accepted. `0` means unlimited. |
| `httpservice.accept.maxratepersecond` | 0 | Maximum new connections accepted per second. `0` means unlimited. |
| `httpservice.lowresources.enabled` | Disabled | Watch for low resources and shed idle connections while the condition lasts. |
| `httpservice.lowresources.maxmemory` | 0 | Heap usage in MB counting as low. `0` watches only the thread pool. |
| `httpservice.lowresources.idletimeout` | 60000 | Idle timeout in milliseconds applied while resources are low. |
| `httpservice.lowresources.stopaccepting` | Disabled | Also refuse new connections entirely while resources are low. |

**Connection limit and accept rate limit are safe to enable**: they only govern whether *new* connections are accepted and never touch established ones, so they cannot interrupt anyone already connected. Size the connection limit well above normal use, remembering that a browser opens several connections per user and that each open administration interface holds a websocket.

**The low resource monitor is the one guard that touches established connections**, and it needs a deliberate value. While resources are low it applies `lowresources.idletimeout` to *every* connection on the connector, so a short value closes the administration interface's websocket and any server-sent-event stream along with the idle connections you wanted to shed. Structr therefore defaults it to 60000, matching the websocket idle timeout; Jetty's own default of 1000 would break both. Do not lower it.

`lowresources.stopaccepting` is the closest thing to an emergency brake: while resources are low no new connection is accepted, and normal service resumes by itself when they recover. It is bounded and self-healing, which is why Structr has this rather than a switch that stops serving altogether. Stopping the HTTP service would convert a partial problem into a total outage, take the administration interface down with it, and let an attacker kill the instance simply by tripping a threshold.

### Exempting Trusted Clients

Monitoring systems and internal services often poll frequently and legitimately. Exempt them by address or range:

```
httpservice.ratelimiting.excludeaddresses = 127.0.0.1, ::1, 10.0.0.0/8, 192.168.1.50
```

Loopback is exempt by default, which also covers requests the server makes to itself.

Whole endpoints can be exempted instead, when it is the path rather than the caller that should be unrestricted:

```
httpservice.ratelimiting.excludepaths = /structr/metrics/*, /structr/health/*
```

### Choosing Limits

Start from what a real page load costs. Open the busiest screen in the application, count the requests in the browser's network tab, and make sure `bucketsize` comfortably exceeds it, otherwise ordinary use will draw 429s.

`maxrequestspersecond` then governs sustained traffic. The default of 100 suits an interactive application; a pure API backend with batch clients may need considerably more. If legitimate users see 429 responses, the limit is too low. Raise the bucket size first, since that absorbs bursts without lowering the sustained ceiling.
