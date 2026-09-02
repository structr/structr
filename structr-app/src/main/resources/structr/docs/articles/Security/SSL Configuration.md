# SSL Configuration

HTTPS encrypts all communication between the browser and server, protecting sensitive data from interception. Structr supports automatic SSL certificate management through Let's Encrypt as well as manual certificate installation.

## Prerequisites

Before configuring HTTPS, you need superuser credentials for your Structr instance and a domain name pointing to your server. Let's Encrypt does not work with localhost, so you need a real domain for automatic certificates. The server must be reachable from the internet on ports 80 and 443 for Let's Encrypt validation. If you want to use the standard ports 80 and 443, you also need appropriate privileges to bind to these privileged ports.

## Certificate Types

Structr supports two types of SSL certificates:

**Let's Encrypt certificates** are issued by a trusted certificate authority and recognized by all browsers without warnings. They require a publicly accessible domain and internet connectivity for validation. Let's Encrypt certificates are free and automatically renewed.

**Self-signed certificates** are generated locally without a certificate authority. Browsers will show security warnings because they cannot verify the certificate's authenticity. Self-signed certificates are suitable for local development and testing, but not for production use.

For production deployments, use Let's Encrypt. For local development where Let's Encrypt is not possible, use self-signed certificates or mkcert.

## Let's Encrypt

Let's Encrypt provides free, automatically renewed SSL certificates. When you request a certificate, Structr handles the domain validation process and stores the resulting certificate files in the `/opt/structr/ssl/` directory. You do not need to manually manage these files - Structr configures the paths automatically.

### Configuration

Configure Let's Encrypt in the Configuration Interface under Security Settings. Often, only `letsencrypt.domains` needs to be configured and the defaults for all other settings work fine.

| Setting | Description |
|---------|-------------|
| `letsencrypt.domains` | Your domain name(s), space-separated for multiple domains |
| `letsencrypt.challenge` | Validation method: `http` (default) or `dns` |

### Requesting a Certificate

After configuring the domain, request a certificate through the REST API:

```bash
curl -X POST http://your-domain.com/structr/rest/maintenance/letsencrypt \
  -H "X-User: admin" \
  -H "X-Password: admin" \
  -H "Content-Type: application/json" \
  -d '{"server": "production", "challenge": "http", "wait": "10"}'
```

The parameters control the certificate request:

| Parameter | Description |
|-----------|-------------|
| `server` | `production` for real certificates, `staging` for testing |
| `challenge` | `http` for HTTP-01 validation, `dns` for DNS-01 |
| `wait` | Seconds to wait for challenge completion |
| `reload` | Reload HTTPS certificate without restart (default: false) |

This maintenance command can also be run via scripting and thus also automated.

### Certificate Renewal

Let's Encrypt certificates duration is subject to change. Check the current lifetime at https://letsencrypt.org/. At the time of writing the certificate lifetime is 90 days.

To manually trigger renewal, execute the certificate request again or automate the process.

### Automating Certificate Updates

By combining a scheduled job with the `letsencrypt` maintenance command, we can automate certificate renewal.

First, create the following user-defined method `renewSSLCertificate` and test it with a small change: Set the config `server` to `staging`, as per Let's Encrypt's best practices. If successful, set `server` back to `production` and continue.

```javascript
{
	let config = {
		server: "production",
		wait: 10,
		reload: true
	};

	let result = $.maintenance('letsencrypt', config);

	if (result?.success === true) {
		$.log('Successfully updated certificate.');
	} else {
		$.log(result?.errors);
	}
}
```

Then configure the internal Cron scheduler to automatically call this method periodically.

1. Add `renewSSLCertificate` to the configuration setting `cronservice.tasks`
2. Create a custom Cron expression `renewSSLCertificate.cronExpression` and configure to your liking
	- it is advisable to not set this job to run at the first of the month at midnight because of increased traffic to Let's Encrypt
	- example configuration: `0 0 3 19 * *` = At 03:00 AM, on day 19 of the month

## Enabling HTTPS

Once you have a certificate (from Let's Encrypt or manually installed), enable HTTPS in the Configuration Interface under Server Settings:

| Setting | Description |
|---------|-------------|
| `application.http.port` | HTTP port, typically 80 |
| `application.https.port` | HTTPS port, typically 443 |
| `application.https.enabled` | Set to `true` to enable HTTPS |
| `httpservice.force.https` | Set to `true` to redirect all HTTP traffic to HTTPS |

After changing these settings, restart the HTTP service. You can do this in the Services tab of the Configuration Interface, through the REST API, or via the command line:

```bash
curl -X POST http://localhost:8082/structr/rest/maintenance/restartService \
  -H "X-User: admin" \
  -H "X-Password: admin" \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "HttpService"}'
```

```bash
sudo systemctl restart structr
```

## Manual Certificate Installation

If you have certificates from another certificate authority or need to use existing certificates, you can install them manually. Manual certificates are stored in the same directory as Let's Encrypt certificates (`/opt/structr/ssl/`), but you must configure the paths explicitly.

Place your certificate files in the Structr SSL directory and set appropriate ownership:

```bash
sudo mkdir -p /opt/structr/ssl/
sudo cp your-certificate.pem /opt/structr/ssl/
sudo cp your-private-key.pem /opt/structr/ssl/
sudo chown -R structr:structr /opt/structr/ssl/
```

Configure the certificate paths in the Configuration Interface under Server Settings:

| Setting | Description |
|---------|-------------|
| `application.ssl.certificate.path` | Path to your certificate file |
| `application.ssl.private.key.path` | Path to your private key file |

### SSL Hardening

For enhanced security, configure protocol and cipher settings:

| Setting | Recommended Value |
|---------|-------------------|
| `application.ssl.protocols` | `TLSv1.2,TLSv1.3` |
| `application.ssl.ciphers` | `ECDHE-RSA-AES256-GCM-SHA384,ECDHE-RSA-AES128-GCM-SHA256` |
| `application.ssl.dh.keysize` | `2048` |

## Local Development

Let's Encrypt requires a publicly accessible domain and cannot issue certificates for localhost. For local development, you need to use self-signed certificates instead. Unlike Let's Encrypt certificates, self-signed certificates are not trusted by browsers, so you will see security warnings. This is acceptable for development but not for production.

You have two options for creating local certificates: generating a self-signed certificate manually with OpenSSL, or using mkcert which creates certificates trusted by your local machine.

### Self-Signed Certificates

Generate a self-signed certificate for development:

```bash
openssl req -x509 -newkey rsa:4096 \
  -keyout localhost-key.pem \
  -out localhost-cert.pem \
  -days 365 -nodes \
  -subj "/CN=localhost"

sudo mkdir -p /opt/structr/ssl/
sudo mv localhost-cert.pem localhost-key.pem /opt/structr/ssl/
```

Configure Structr to use these certificates:

| Setting | Value |
|---------|-------|
| `application.https.enabled` | `true` |
| `application.https.port` | `8443` |
| `application.ssl.certificate.path` | `/opt/structr/ssl/localhost-cert.pem` |
| `application.ssl.private.key.path` | `/opt/structr/ssl/localhost-key.pem` |

Browsers will show security warnings for self-signed certificates. Click through the warning to access your application during development.

### Using mkcert

For a smoother development experience, use mkcert to create locally-trusted certificates. Install mkcert through your package manager (`brew install mkcert` on macOS, `sudo apt install mkcert` on Ubuntu), then create and install a local certificate authority:

```bash
mkcert -install
mkcert localhost 127.0.0.1 ::1

sudo mkdir -p /opt/structr/ssl/
sudo mv localhost+2.pem /opt/structr/ssl/localhost-cert.pem
sudo mv localhost+2-key.pem /opt/structr/ssl/localhost-key.pem
```

Browsers will trust mkcert certificates without warnings.

## Verification

After enabling HTTPS, verify your configuration by testing the HTTPS connection with `curl -I https://your-domain.com`. If you enabled HTTP-to-HTTPS redirection, test that as well with `curl -I http://your-domain.com` - this should return a 301 or 302 redirect to the HTTPS URL.

You can check certificate details with OpenSSL:

```bash
echo | openssl s_client -connect your-domain.com:443 2>/dev/null | \
  openssl x509 -noout -dates
```

In the browser, navigate to your domain and check for the lock icon in the address bar. Click the icon to view certificate details and verify the certificate is valid and issued by the expected authority.

## Troubleshooting

### Certificate Generation Fails

When Let's Encrypt certificate generation fails, first verify that your domain's DNS correctly points to your server. The server must be reachable from the internet on ports 80 and 443 for the validation challenge. Check that no other service (like Apache or nginx) is using port 80. Review the Structr log for detailed error messages:

```bash
tail -f /var/log/structr/structr.log | grep -i letsencrypt
```

### HTTPS Not Working

If HTTPS connections fail after setup, verify that `application.https.enabled` is set to `true` and that the certificate files exist in the SSL directory. Make sure you restarted the HTTP service after changing configuration. Check whether the HTTPS port is listening and not blocked by a firewall:

```bash
sudo netstat -tlnp | grep :443
ls -la /opt/structr/ssl/
```

### Permission Denied on Ports 80/443

Binding to ports below 1024 requires elevated privileges. You can either grant the capability to bind privileged ports to Java, or use higher ports with port forwarding:

```bash
# Grant capability to Java
sudo setcap 'cap_net_bind_service=+ep' /usr/bin/java

# Or use port forwarding
sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 8082
sudo iptables -t nat -A PREROUTING -p tcp --dport 443 -j REDIRECT --to-port 8443
```

### HTTP Not Redirecting to HTTPS

If HTTP traffic is not redirecting to HTTPS, verify that `httpservice.force.https` is set to `true` and restart the HTTP service after changing the setting. Also clear your browser cache, as browsers may have cached the non-redirecting response.

## Related Topics

- Security - Authentication methods, users, groups, and permissions
- Configuration Interface - Managing Structr settings including SSL configuration
- Operations - Server management and maintenance tasks
