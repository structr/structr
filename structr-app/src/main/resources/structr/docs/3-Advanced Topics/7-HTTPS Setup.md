# HTTPS Setup

Setting up HTTPS in Structr ensures secure communication between your web browser and the Structr server. This guide walks you through the complete process of configuring SSL/TLS certificates using Let's Encrypt, a free certificate authority.

## Overview

HTTPS (HyperText Transfer Protocol Secure) encrypts all communication between the browser and server, protecting sensitive data from interception. Structr supports automatic SSL certificate generation and renewal using Let's Encrypt.

## Prerequisites

Before setting up HTTPS, ensure you have:

1. **Admin Access**: Superuser credentials for your Structr instance
2. **Domain Name**: A valid domain pointing to your server (e.g., localhost or your custom domain)
3. **Port Access**: Ability to configure ports 80 (HTTP) and 443 (HTTPS)
4. **Internet Access**: Server must be reachable from the internet for certificate validation

## Step 1: Initial Configuration

### Access the Configuration Interface

1. Navigate to your Structr instance: `http://localhost:8082/structr/`
2. If this is a fresh installation, you'll see the initial configuration wizard
3. Configure your database connection as prompted

### Database Connection Setup

1. Click **"Configure a database connection"**
2. Select **"Create new database connection"**
3. Fill in your database parameters:
   - **Driver**: Choose your database type (Neo4j recommended)
   - **URL**: Database connection string
   - **Username**: Database username
   - **Password**: Database password
4. Click **"Set Neo4j defaults"** for standard Neo4j configurations
5. Test the connection and save

## Step 2: Configure Security Settings

### Set Let's Encrypt Domain

1. Navigate to the **Configuration** section (gear icon in admin interface)
2. Go to the **Security Settings** tab
3. Find the **Let's Encrypt** configuration section
4. Set the following parameter:
   ```
   letsencrypt.domains = localhost
   ```
   - For production, replace `localhost` with your actual domain
   - For multiple domains, separate with commas: `domain1.com,domain2.com`
5. Click **"Save"** to apply the changes

### Additional Security Settings (Optional)

```
# Certificate renewal settings
letsencrypt.challenge = http
letsencrypt.email = admin@yourdomain.com

# Advanced SSL settings
application.ssl.protocols = TLSv1.2,TLSv1.3
application.ssl.ciphers = ECDHE-RSA-AES256-GCM-SHA384,ECDHE-RSA-AES128-GCM-SHA256
```

## Step 3: Configure Server Ports

### Set HTTP and HTTPS Ports

1. Go to the **Server Settings** tab in Configuration
2. Configure the following parameters:

   **HTTP Port Configuration:**
   ```
   application.http.port = 80
   ```

   **HTTPS Port Configuration:**
   ```
   application.https.port = 443
   ```

3. Click **"Save"** after each setting

### Port Configuration Notes

- **Port 80**: Standard HTTP port, required for Let's Encrypt validation
- **Port 443**: Standard HTTPS port for secure connections
- **Firewall**: Ensure these ports are open on your server firewall
- **Privileges**: Running on ports 80/443 may require root privileges

## Step 4: Restart HTTP Service

### Using the Services Tab

1. Navigate to the **Services** tab in Configuration
2. Find **"HttpService.default"** in the services list
3. Click the **"Restart"** button next to the service
4. Wait for the service to restart completely
5. Verify the service shows as "Running"

### Alternative: Command Line Restart

If you have SSH access to the server:

```bash
# Restart Structr service
sudo systemctl restart structr

# Or restart just the HTTP service via API
curl -XPOST -HX-User:admin -HX-Password:admin \
  http://localhost:8082/structr/rest/maintenance/restartService \
  -d '{"serviceName":"HttpService"}'
```

## Step 5: Obtain Let's Encrypt Certificate

### Using the REST API

Execute the following command to request a certificate:

**Important Note**: Let's Encrypt certificates cannot be issued for `localhost`. For local development, see the [Development and Local Testing with Localhost](#development-and-local-testing-with-localhost) section below.

For production domains:

```bash
curl -XPOST \
  -HX-User:admin \
  -HX-Password:admin \
  http://your-domain.com/structr/rest/maintenance/letsencrypt \
  -d '{"server":"production","challenge":"http","wait":"10"}'
```

### Parameter Explanation

- **server**: `"production"` for real certificates, `"staging"` for testing
- **challenge**: `"http"` for HTTP-01 challenge, `"dns"` for DNS-01
- **wait**: Time in seconds to wait for challenge completion

### Alternative: Using the Admin Interface

1. Go to **Dashboard** → **Maintenance** tab
2. Find **"Let's Encrypt Certificate"** section
3. Configure parameters:
   - Server: Production
   - Challenge: HTTP
   - Wait time: 10 seconds
4. Click **"Execute"** to start certificate generation

### Certificate Generation Process

The certificate generation process:

1. **Domain Validation**: Let's Encrypt verifies domain ownership
2. **Challenge Response**: Server responds to validation challenge
3. **Certificate Issuance**: Certificate is generated and installed
4. **Automatic Renewal**: Certificate will auto-renew before expiration

### Monitoring Certificate Generation

```bash
# Check certificate generation status
tail -f /var/log/structr/structr.log | grep -i letsencrypt

# Verify certificate was created
ls -la /opt/structr/ssl/
```

## Step 6: Enable HTTPS

### Enable HTTPS in Configuration

1. Return to **Server Settings** tab
2. Set the following parameters:

   **Enable HTTPS:**
   ```
   application.https.enabled = true
   ```

   **Force HTTPS Redirect:**
   ```
   httpservice.force.https = true
   ```

3. Save each setting

### Configuration Details

- **application.https.enabled**: Activates HTTPS support
- **httpservice.force.https**: Redirects all HTTP traffic to HTTPS
- **SSL Certificate Path**: Automatically managed by Let's Encrypt integration

## Step 7: Final Service Restart

### Restart HTTP Service Again

1. Go back to the **Services** tab
2. Restart **"HttpService.default"** once more
3. Wait for the service to fully restart
4. The service should now be running with HTTPS enabled

### Verify HTTPS Configuration

After restarting, verify the configuration:

```bash
# Test HTTP redirect
curl -I http://localhost
# Should return 301/302 redirect to HTTPS

# Test HTTPS connection
curl -I https://localhost
# Should return 200 OK with valid certificate
```

## Step 8: Install Development License (Optional)

### For Demo/Development Environments

If you're setting up a demo or development environment:

1. **Contact Structr Support**: Request a demo/dev license
2. **License File**: Receive the license file via email
3. **Install License**:
   - Go to **Dashboard** → **About Structr**
   - Upload the license file in the license section
   - Restart Structr after license installation

### License Configuration

```bash
# Place license file in correct location
sudo cp structr-demo.license /opt/structr/license/

# Set proper permissions
sudo chown structr:structr /opt/structr/license/structr-demo.license

# Restart service
sudo systemctl restart structr
```

## Verification and Testing

### Test HTTPS Setup

1. **Access via HTTPS**: Navigate to `https://localhost`
2. **Check Certificate**: Click the lock icon in browser address bar
3. **Verify Redirect**: Access `http://localhost` should redirect to HTTPS
4. **Test Admin Interface**: Ensure admin login works over HTTPS

### SSL Certificate Verification

```bash
# Check certificate details
openssl s_client -connect localhost:443 -servername localhost

# Verify certificate expiration
echo | openssl s_client -connect localhost:443 2>/dev/null | \
  openssl x509 -noout -dates

# Test SSL Labs rating (external tool)
# Visit: https://www.ssllabs.com/ssltest/
```

### Common Verification Steps

1. **Browser Security**: No certificate warnings
2. **Green Lock Icon**: Appears in browser address bar
3. **HTTP Redirect**: All HTTP requests redirect to HTTPS
4. **Admin Access**: Configuration interface accessible via HTTPS

## Troubleshooting

### Common Issues and Solutions

#### Certificate Generation Fails

**Problem**: Let's Encrypt certificate generation fails

**Solutions**:

- Verify domain DNS points to your server
- Check firewall allows port 80 access
- Ensure no other service is using port 80
- Try staging server first: `"server":"staging"`

```bash
# Check port 80 availability
sudo netstat -tlnp | grep :80

# Test domain resolution
nslookup demoNN.structr.com

# Check firewall status
sudo ufw status
```

#### HTTPS Not Working After Setup

**Problem**: HTTPS connection fails or shows certificate errors

**Solutions**:

- Verify `application.https.enabled=true` is set
- Check certificate files exist in SSL directory
- Restart HttpService completely
- Clear browser cache and cookies

```bash
# Check SSL certificate files
ls -la /opt/structr/ssl/

# Verify HTTPS port is listening
sudo netstat -tlnp | grep :443

# Check Structr logs for SSL errors
tail -f /var/log/structr/structr.log | grep -i ssl
```

#### HTTP Still Accessible

**Problem**: HTTP traffic not redirecting to HTTPS

**Solutions**:

- Verify `httpservice.force.https=true` is set
- Restart HttpService after setting
- Check for configuration typos
- Verify both HTTP and HTTPS ports are configured

#### Permission Denied Errors

**Problem**: Cannot bind to ports 80/443

**Solutions**:

- Run Structr with appropriate privileges
- Use port forwarding from higher ports
- Configure systemd service with proper capabilities

```bash
# Check if ports require privileges
sudo netstat -tlnp | grep -E ':(80|443)'

# Grant capability to bind privileged ports
sudo setcap 'cap_net_bind_service=+ep' /usr/bin/java
```

### Configuration Validation

```bash
# Verify configuration settings
grep -E "(https|ssl|letsencrypt)" /opt/structr/structr.conf

# Check service status
sudo systemctl status structr

# Monitor real-time logs
sudo journalctl -u structr -f
```

## Maintenance and Renewal

### Automatic Certificate Renewal

Let's Encrypt certificates are valid for 90 days and should auto-renew:

```bash
# Check certificate expiration
echo | openssl s_client -connect demoNN.structr.com:443 2>/dev/null | \
  openssl x509 -noout -dates

# Force manual renewal (if needed)
curl -XPOST -HX-User:admin -HX-Password:admin \
  http://localhost/structr/rest/maintenance/letsencrypt \
  -d '{"server":"production","challenge":"http","wait":"10"}'
```

### Monitoring Certificate Status

```bash
# Set up monitoring script
cat > /opt/structr/scripts/check-ssl.sh << 'EOF'
#!/bin/bash
DOMAIN="localhost"
EXPIRY=$(echo | openssl s_client -connect $DOMAIN:443 2>/dev/null | \
         openssl x509 -noout -enddate | cut -d= -f2)
EXPIRY_DATE=$(date -d "$EXPIRY" +%s)
CURRENT_DATE=$(date +%s)
DAYS_UNTIL_EXPIRY=$(( (EXPIRY_DATE - CURRENT_DATE) / 86400 ))

if [ $DAYS_UNTIL_EXPIRY -lt 30 ]; then
    echo "WARNING: SSL certificate expires in $DAYS_UNTIL_EXPIRY days"
fi
EOF

chmod +x /opt/structr/scripts/check-ssl.sh

# Add to crontab for daily checking
echo "0 9 * * * /opt/structr/scripts/check-ssl.sh" | crontab -
```

### Backup Certificate Files

```bash
# Create certificate backup
sudo tar -czf /backup/structr-ssl-$(date +%Y%m%d).tar.gz /opt/structr/ssl/

# Restore from backup (if needed)
sudo tar -xzf /backup/structr-ssl-YYYYMMDD.tar.gz -C /
```

## Security Best Practices

### Additional Security Headers

Add security headers for enhanced protection:

```
# In Server Settings
application.security.headers.enabled = true
application.security.headers.hsts = true
application.security.headers.csp = true
application.security.headers.xframe = DENY
application.security.headers.xss = 1; mode=block
```

### SSL Configuration Hardening

```
# Disable weak protocols and ciphers
application.ssl.protocols = TLSv1.2,TLSv1.3
application.ssl.ciphers = ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-GCM-SHA256
application.ssl.dh.keysize = 2048
```

### Regular Security Updates

```bash
# Keep Structr updated
sudo apt update && sudo apt upgrade structr

# Monitor security advisories
# Subscribe to Structr security mailing list

# Regular certificate health checks
curl -s https://crt.sh/?q=localhost
```

## Next Steps

After successfully setting up HTTPS:

1. **Update Application URLs**: Change all internal links to use HTTPS
2. **Configure CDN**: Set up CloudFlare or similar for additional security
3. **Implement Security Headers**: Add CSP, HSTS, and other security headers
4. **Set Up Monitoring**: Monitor certificate expiration and SSL health
5. **Test Performance**: Verify HTTPS doesn't impact application performance

## Development and Local Testing with Localhost

### For Local Development

When developing locally on `localhost`, you have several options for HTTPS:

#### Option 1: Self-Signed Certificates (Development Only)

For local development, you can use self-signed certificates:

```bash
# Generate self-signed certificate for localhost
openssl req -x509 -newkey rsa:4096 -keyout localhost-key.pem -out localhost-cert.pem -days 365 -nodes -subj "/CN=localhost"

# Move certificates to Structr SSL directory
sudo mkdir -p /opt/structr/ssl/
sudo mv localhost-cert.pem /opt/structr/ssl/
sudo mv localhost-key.pem /opt/structr/ssl/
```

Then configure Structr to use these certificates:

```
# In Server Settings
application.https.enabled = true
application.https.port = 8443
application.ssl.certificate.path = /opt/structr/ssl/localhost-cert.pem
application.ssl.private.key.path = /opt/structr/ssl/localhost-key.pem
```

**Note**: Self-signed certificates will show security warnings in browsers but are sufficient for development.

#### Option 2: mkcert for Local Development

Use mkcert to create locally-trusted certificates:

```bash
# Install mkcert
brew install mkcert  # macOS
# or
sudo apt install mkcert  # Ubuntu

# Create local CA
mkcert -install

# Generate certificate for localhost
mkcert localhost 127.0.0.1 ::1

# Move certificates to Structr
sudo mkdir -p /opt/structr/ssl/
sudo mv localhost+2.pem /opt/structr/ssl/localhost-cert.pem
sudo mv localhost+2-key.pem /opt/structr/ssl/localhost-key.pem
```

#### Option 3: Development Ports (No HTTPS)

For simple local development, you can skip HTTPS and use development ports:

```
# In Server Settings
application.http.port = 8082
application.https.enabled = false
```

Access your application at: `http://localhost:8082/structr/`

### Production Domain Setup

For production deployment with a real domain:

1. **Register Domain**: Obtain a domain name (e.g., myapp.com)
2. **Configure DNS**: Point domain to your server's IP address
3. **Update Configuration**: Replace `localhost` with your domain in all settings
4. **Follow Let's Encrypt Process**: Use the standard Let's Encrypt procedure

### Testing HTTPS Locally

When testing HTTPS on localhost:

```bash
# Test HTTP to HTTPS redirect
curl -I http://localhost:8082
# Should redirect to https://localhost:8443 (or configured HTTPS port)

# Test HTTPS connection (with self-signed cert)
curl -k -I https://localhost:8443
# -k flag ignores certificate warnings for self-signed certs

# Test admin interface
curl -k -I https://localhost:8443/structr/
```

### Browser Configuration for Local HTTPS

When using self-signed certificates locally:

1. **Chrome**: Navigate to `https://localhost:8443`, click "Advanced", then "Proceed to localhost (unsafe)"
2. **Firefox**: Navigate to `https://localhost:8443`, click "Advanced", then "Accept the Risk and Continue"
3. **Safari**: Navigate to `https://localhost:8443`, click "Show Details", then "visit this website"

For mkcert certificates, browsers will trust them automatically without warnings.

### Local Development Workflow

1. **Start with HTTP**: Begin development using HTTP on port 8082
2. **Add HTTPS Later**: Implement HTTPS when ready for production-like testing
3. **Use Development Tools**: Leverage browser dev tools that work with self-signed certs
4. **Production Deployment**: Switch to real domain and Let's Encrypt for production

This approach allows you to develop locally while preparing for secure production deployment.

HTTPS is essential for production deployments and provides the foundation for secure web applications. The Let's Encrypt integration in Structr makes certificate management automatic and hassle-free.