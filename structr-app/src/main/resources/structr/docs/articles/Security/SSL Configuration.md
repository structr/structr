# SSL Configuration

Structr terminates TLS itself. The embedded Jetty server that serves pages, files and the REST interface can open an HTTPS connector next to the plain HTTP connector, and it reads the server certificate and its private key from a Java keystore file. There is no separate web server in front of Structr and no PEM file that Structr would read directly: everything HTTPS needs is the keystore, its password, and the port to listen on.

You can fill that keystore in two ways. The `letsencrypt` maintenance command obtains a certificate from Let's Encrypt and writes it into the keystore for you, including renewal on a schedule. If you have a certificate from another certificate authority, or you want a self-signed certificate for development, you build the keystore yourself with `openssl` and `keytool` and point Structr at it. Both ways end in the same two settings, `application.keystore.path` and `application.keystore.password`.

## Enabling HTTPS

HTTPS is configured in the Server section of the Configuration Interface, or directly in `structr.conf`. The HttpService reads these settings when it starts, so port changes and switching HTTPS on need a restart of the HttpService (Services tab of the Configuration Interface, Restart button) or a restart of Structr.

| Setting | Default | Description |
|---------|---------|-------------|
| `application.https.enabled` | `false` | Opens the HTTPS connector. Requires a keystore that contains the certificate and its private key. |
| `application.https.port` | `8083` | Port of the HTTPS connector. |
| `application.http.port` | `8082` | Port of the plain HTTP connector. It stays open when HTTPS is enabled; see Forcing HTTPS below for redirecting it. |
| `application.keystore.path` | `domain.key.keystore` | Path to the keystore file. A relative path is resolved against the working directory of the Structr process. |
| `application.keystore.password` | empty | Password of the keystore. Structr uses the same password for the store and for the private key entry inside it, so both must match. |
| `httpservice.sni.required` | `false` | Rejects TLS handshakes that carry no Server Name Indication. |
| `httpservice.sni.hostcheck` | `false` | Rejects requests whose Host header does not match the SNI name the handshake was made with. |

The default keystore path is deliberately the file the Let's Encrypt command writes (`letsencrypt.domain.key.filename` plus `.keystore`), so a Let's Encrypt setup does not need to change it. The working directory is `/usr/lib/structr` for the Debian package (set in the systemd unit) and `/var/lib/structr` in the Docker image. If you are not sure where the process runs, use an absolute path.

Jetty expects a PKCS12 keystore by default, which is also what the Let's Encrypt command creates. A JKS keystore works as well, because the Java runtime loads JKS files through the PKCS12 type in its default compatibility mode. Structr does not configure a keystore type of its own.

When HTTPS is enabled, the HTTPS connector negotiates HTTP/2 through ALPN and falls back to HTTP/1.1 for clients that do not support it. The plain HTTP connector offers HTTP/2 over cleartext (h2c) in the same way. The setting `httpservice.connection.ratelimit` (default 1000) caps the number of HTTP/2 frames a single connection may send per second.

## Obtaining a Certificate with Let's Encrypt

Let's Encrypt issues certificates through the ACME protocol. Structr implements the client side in the maintenance command `letsencrypt`: it registers an account, orders a certificate for the configured domains, answers the domain validation challenge, downloads the certificate chain and stores it together with the domain key in the keystore. Let's Encrypt has to reach your server under the domain name to validate it, so this only works for a server with a public DNS name. For `localhost` or an internal hostname, use a self-signed certificate as described below.

### Settings

The command reads its defaults from the Letsencrypt section in the Security group of the Configuration Interface. In most installations only `letsencrypt.domains` needs a value.

| Setting | Default | Description |
|---------|---------|-------------|
| `letsencrypt.domains` | empty | Domain names the certificate is issued for, separated by spaces. Required. |
| `letsencrypt.challenge.type` | `http` | Validation method, `http` or `dns`. The `challenge` parameter of the command overrides it. |
| `letsencrypt.wait` | `30` | Seconds to wait between publishing the challenge and asking Let's Encrypt to validate it. The `wait` parameter of the command overrides it. |
| `letsencrypt.production.server.url` | `acme://letsencrypt.org` | ACME directory used when the command runs with `server: production`. |
| `letsencrypt.staging.server.url` | `acme://letsencrypt.org/staging` | ACME directory used with `server: staging`. Staging certificates are not trusted by browsers but do not count against the rate limits. |
| `letsencrypt.user.key.filename` | `user.key` | File holding the ACME account key. Created on the first run. |
| `letsencrypt.domain.key.filename` | `domain.key` | File holding the private key of the certificate. Created on the first run and reused on renewal. |
| `letsencrypt.domain.csr.filename` | `domain.csr` | File the certificate signing request is written to. |
| `letsencrypt.domain.chain.filename` | `domain-chain.crt` | File the issued certificate chain is written to, in PEM format. |
| `letsencrypt.key.size` | `2048` | RSA key length used when the account key or the domain key is generated. |

All file names are resolved against the working directory of the Structr process, just like `application.keystore.path`.

### Running the Command

The command is available as a maintenance resource and as a scripting function. Both require a superuser or admin session. Over REST you call it with a JSON body:

```bash
curl -X POST http://your-domain.com/structr/rest/maintenance/letsencrypt \
  -H "X-User: admin" \
  -H "X-Password: admin" \
  -H "Content-Type: application/json" \
  -d '{"server": "production", "challenge": "http", "wait": 10, "reload": true}'
```

From a script, `$.maintenance('letsencrypt', { server: 'production', challenge: 'http', wait: 10, reload: true })` does the same. The result is an object with `success` (boolean) and `errors` (list of messages).

| Parameter | Description |
|-----------|-------------|
| `server` | Required. `production` requests a trusted certificate, `staging` a test certificate. Any other value falls back to staging. Without this parameter the command aborts with HTTP 422 and the message `No server supplied, aborting.` |
| `challenge` | `http` or `dns`. Overrides `letsencrypt.challenge.type`. |
| `wait` | Seconds to wait before validation. Overrides `letsencrypt.wait`. Accepts a number or a numeric string. |
| `reload` | `true` makes the running HttpService load the new keystore after the certificate was written. Defaults to `false`. |
| `mode` | `wait` (default) runs order, challenge, pause and validation in one go. `create` only creates the order and publishes the challenge; `verify` creates the order again and validates immediately. The split is useful with the DNS challenge when creating the TXT record takes longer than you want to wait inside one request. |
| `verbose` | `true` logs the JSON of the ACME order, authorizations and challenges. |
| `keepChallengeFiles` | `true` leaves the challenge files in the internal file system after the run; see the HTTP challenge below. |

Test a new setup against the staging server first. Let's Encrypt enforces rate limits on the production endpoint, and a misconfigured challenge that fails repeatedly can lock you out for a while.

### The HTTP Challenge

With `challenge: http`, Let's Encrypt fetches `http://<domain>/.well-known/acme-challenge/<token>` on port 80. Structr first tries to start a small temporary HTTP server on port 80 that answers exactly this path. That only works when port 80 is free and the Structr process is allowed to bind it. When binding fails, the log shows `Unable to start temporary HTTP server for challenge authorization, trying internal file server...` and the command falls back to creating the folder `/.well-known/acme-challenge/` and the token file in Structr's internal file system, both visible to public users. Structr then serves the file itself, which means the Structr HTTP port must be the one Let's Encrypt reaches on port 80, either because `application.http.port` is 80 or because a port forward or reverse proxy delivers port 80 to it. After the run, the command removes the `/.well-known` folder again unless `keepChallengeFiles` is set.

### The DNS Challenge

With `challenge: dns`, Let's Encrypt looks up a TXT record `_acme-challenge.<domain>.` whose value the command computes for each domain. Structr does not talk to your DNS provider itself. Instead, if a user-defined method named `onAcmeChallenge` exists, the command calls it with the parameters `{ type: 'dns', domain: <domain>, record: <record>, digest: <digest> }`, so you can create the record through your provider's API in that method. If no such method exists, the command logs the record and its value with the note that it will be probed after the waiting time, and you create the record by hand within `wait` seconds. DNS propagation is usually slower than the default 30 seconds, so raise `wait` accordingly or use `mode: create` and `mode: verify` in two separate calls.

Independent of the challenge type, a user-defined method named `afterAcmeChallenge`, if it exists, is called at the end of every run with `{ success: <boolean>, errors: [<messages>] }`. Use it to remove the TXT record again or to send a notification when a renewal fails.

### What the Command Writes

A successful run leaves these files in the working directory: the account key (`user.key`), the domain key (`domain.key`), the signing request (`domain.csr`), the certificate chain in PEM format (`domain-chain.crt`), and the keystore. The keystore is created as PKCS12 if it does not exist yet and is protected with `application.keystore.password`. The certificate chain and the domain key are stored under an alias built from the domain names and the domain key file name. The keystore file name comes from `application.keystore.path`; if that setting is blank, the command uses `letsencrypt.domain.key.filename` plus `.keystore`, which is `domain.key.keystore` with the default settings, and therefore matches the default keystore path.

Back up `user.key`, `domain.key` and the keystore together with `structr.conf`. Losing the account key is harmless, a new one is created; losing the keystore password makes the existing keystore unreadable.

### How the Server Picks Up the New Certificate

Jetty reads the keystore when the HttpService starts. Two things follow from that. When you enable HTTPS for the first time, the HttpService must be restarted after the keystore exists, because the HTTPS connector is created at startup. For every later run, passing `reload: true` is enough: the command asks the running HttpService to re-read `application.keystore.path` and `application.keystore.password` and to reload the TLS context, and new connections use the new certificate without a restart or a dropped connection. If HTTPS was not active when the server started, the reload does nothing and logs `Server started without SSL. Need to restart service.`

The order for a new installation is therefore: set `letsencrypt.domains` and `application.keystore.password`, run the command once against staging and then against production, set `application.https.enabled` to `true`, and restart the HttpService.

### Scheduled Renewal

Let's Encrypt certificates are valid for 90 days at the time of writing. Renewal is the same command again, so you automate it with a scheduled task, as recommended in the [Best Practices](/structr/docs/ontology/Building%20Applications/Best%20Practices) chapter. Create a user-defined function, for example `renewSSLCertificate`:

```javascript
{
	let result = $.maintenance('letsencrypt', {
		server: 'production',
		challenge: 'http',
		wait: 10,
		reload: true
	});

	if (result.success === true) {
		$.log('Certificate renewed.');
	} else {
		$.log('Certificate renewal failed: ' + result.errors);
	}
}
```

Then register it with the CronService by adding its name to `CronService.tasks` and defining `renewSSLCertificate.cronExpression`. Structr cron expressions have six fields starting with seconds, so `0 0 3 19 * *` runs at 03:00 on the 19th of every month. Avoid the first of the month at midnight, when Let's Encrypt sees the most traffic. Changes to `CronService.tasks` need a restart of the CronService. The [Scheduled Tasks](/structr/docs/ontology/Building%20Applications/Scheduled%20Tasks) chapter describes the cron syntax in detail.

## Using a Certificate from Another Certificate Authority

If a commercial or internal certificate authority issued your certificate, you typically hold three PEM files: the certificate, the private key and the intermediate chain. Structr does not read PEM files, so you combine them into a PKCS12 keystore with OpenSSL. Use one password for the store and the key; Structr passes `application.keystore.password` for both.

```bash
openssl pkcs12 -export \
  -in your-domain.crt \
  -inkey your-domain.key \
  -certfile intermediate-chain.crt \
  -name your-domain \
  -out /etc/structr/your-domain.p12 \
  -passout pass:changeit
```

Restrict the file to the user Structr runs as (`chown structr:structr` and `chmod 600`), then set `application.keystore.path` to `/etc/structr/your-domain.p12` and `application.keystore.password` to the password you chose, enable HTTPS and restart the HttpService. If you already have a JKS keystore, you can point the settings at it directly, or convert it with `keytool -importkeystore -srckeystore old.jks -destkeystore new.p12 -deststoretype PKCS12`.

When the authority renews the certificate, rebuild the keystore in the same place and restart the HttpService. The in-place reload is only triggered by the Let's Encrypt command, so a keystore you maintain yourself is picked up at the next start of the HttpService.

## Self-Signed Certificates for Development

For a development machine without a public domain, generate a self-signed certificate straight into a PKCS12 keystore with `keytool`, which ships with the JDK:

```bash
keytool -genkeypair \
  -alias localhost \
  -keyalg RSA -keysize 2048 -validity 365 \
  -storetype PKCS12 \
  -keystore localhost.p12 \
  -storepass changeit \
  -dname "CN=localhost" \
  -ext "SAN=dns:localhost,ip:127.0.0.1"
```

Set `application.keystore.path` to the path of `localhost.p12`, `application.keystore.password` to `changeit`, `application.https.enabled` to `true`, and restart the HttpService. Browsers warn about the certificate because no authority they trust signed it; accept the warning for development. If you prefer a certificate your machine trusts, create one with mkcert (`mkcert localhost 127.0.0.1 ::1`) and convert the resulting PEM files into a PKCS12 keystore with the `openssl pkcs12 -export` command from the previous section.

## Forcing HTTPS

By default the plain HTTP connector stays available next to HTTPS. Setting `httpservice.force.https` to `true` installs a redirect handler in front of everything Structr serves: every request that arrives over HTTP is answered with a 302 redirect to the same path on the `https` scheme and the configured `application.https.port`. In addition the session cookie is marked as secure-only, so a session can only be established over HTTPS.

Enable this setting only after HTTPS works. The redirect is installed whether or not the HTTPS connector actually came up, so with a broken keystore or `application.https.enabled` still `false` every HTTP request is redirected to a port nobody listens on. Changing the setting requires a restart of the HttpService.

Two related cookie settings matter here. `httpservice.cookies.secure` (default `true`) sets the Secure flag on the JSESSIONID cookie so browsers only send it over HTTPS, which is the desired behaviour for any HTTPS installation; on a plain HTTP installation it must be turned off, otherwise logins do not persist. `httpservice.cookies.samesite` (default `Lax`) sets the SameSite attribute; the value `None` only works together with the Secure flag.

## Protocols and Cipher Suites

Structr passes three settings from the HTTPS Settings section to Jetty's TLS context. They take the protocol and cipher suite names of the Java runtime (JSSE), not OpenSSL names.

| Setting | Default | Description |
|---------|---------|-------------|
| `httpservice.ssl.protocols.included` | `TLSv1.2` | Comma-separated list of TLS versions the server offers. Only the listed versions are enabled. |
| `httpservice.ssl.protocols.excluded` | `TLSv1,TLSv1.1` | Comma-separated list of TLS versions that are disabled even if included elsewhere. |
| `httpservice.ssl.ciphers.excluded` | empty | Comma-separated list of cipher suite names or regular expressions to disable. |

Note that with the default settings the server offers TLS 1.2 only. To also offer TLS 1.3, set `httpservice.ssl.protocols.included` to `TLSv1.2,TLSv1.3`. Leave `httpservice.ssl.ciphers.excluded` empty unless you have a concrete requirement: while it is empty, Jetty applies its own exclusion list, which already removes suites without forward secrecy and suites based on MD5 or SHA-1. As soon as you set the setting, your list replaces Jetty's list completely, so you have to spell out every suite you want disabled. Setting `httpservice.log.jetty.startupconfig` to `true` dumps the complete connector configuration, including the effective protocol and cipher lists, into the log when the HttpService starts.

## HSTS

Structr does not send a `Strict-Transport-Security` header from the TLS layer. The header comes from the servlets: `htmlservlet.customresponseheaders` is a comma-separated list of headers that the HTML servlet, the REST servlet, the login and logout servlets and the other servlets add to every response, and its default value already contains `Strict-Transport-Security:max-age=60`. A max-age of 60 seconds is a placeholder rather than a real HSTS policy; once HTTPS is stable, raise it to a value such as `max-age=31536000` in that setting. Do this only when every hostname the header is sent for is reachable over HTTPS, because browsers remember the policy for the whole max-age.

## Troubleshooting

The HttpService logs `Unable to configure SSL, please make sure that application.https.port, application.keystore.path and application.keystore.password are set correctly in structr.conf.` when HTTPS is enabled but the keystore path is empty. Structr then starts with the HTTP connector only.

If the keystore file does not exist or the password is wrong, Jetty fails while opening the connectors. The HttpService tries three times with ten seconds in between, each attempt logging `Error, retrying N more times after 10s - Caught:` followed by the Java message, for example `keystore password was incorrect`, and finally `Exception occurred when trying to start service HttpService`. Because Jetty starts all connectors of the server together, neither the HTTPS nor the HTTP port is served in this case. Fix the path or the password and restart.

If `reload: true` has no effect and the log says `Server started without SSL. Need to restart service.`, HTTPS was not active when the HttpService started. Enable `application.https.enabled` and restart the HttpService once; later renewals reload without a restart.

If the Let's Encrypt command reports `Unable to create certificate order`, check that `letsencrypt.domains` contains the full domain names and that they resolve to this server. If the HTTP challenge fails although the domain is correct, check whether port 80 reaches this Structr instance: the log line about the temporary HTTP server tells you whether Structr answered on port 80 itself or fell back to the internal file system, and in the second case `application.http.port` or a port forward has to deliver port 80 to Structr. If the DNS challenge fails, the TXT record was not visible yet when Let's Encrypt probed it; raise `wait` or split the run with `mode: create` and `mode: verify`. Set `verbose: true` to see the ACME responses, including the error object Let's Encrypt returns for a failed authorization.

If every HTTP request is redirected to a port that does not answer, `httpservice.force.https` is on while the HTTPS connector is not running. Turn the setting off, get HTTPS working, then turn it on again.

To inspect what the server actually presents, use `openssl s_client -connect your-domain.com:8083 -servername your-domain.com` and check the certificate chain, the negotiated protocol version and the expiry dates in its output.

## Related Topics

- [Configuration](/structr/docs/ontology/Operations/Configuration) - The `structr.conf` file, its location and the settings reference
- [Best Practices](/structr/docs/ontology/Building%20Applications/Best%20Practices) - Production checklist including HTTPS and certificate renewal
- [Scheduled Tasks](/structr/docs/ontology/Building%20Applications/Scheduled%20Tasks) - The CronService that runs the renewal function
- [User Management](/structr/docs/ontology/Security/User%20Management) - Users, groups and the admin privileges the maintenance command requires
