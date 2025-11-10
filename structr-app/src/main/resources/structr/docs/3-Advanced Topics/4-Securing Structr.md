# Securing Structr

The overall security of a system is only as strong as its weakest link in the chain. To make a Structr-based system really secure, security measures have to be implemented and tested (!) on multiple levels, which are:

1. Data Transport - make sure all communication between components is encrypted
2. API Endpoints - allow only authorized access to endpoints
3. Passwords - make sure no weak passwords are used
4. Server - minimize the risk of unauthorized access to server components
5. Database - encrypt data stored in the database

The typical steps to make a Structr instance as secure as possible are described in the following chapters.


## Data Transport

The goal is to enable encrypted data transport between all components of the system.

<svg width="100%" viewBox="0 0 120 62" xmlns="http://www.w3.org/2000/svg"><rect x="10" y="1" width="20" height="20" rx="2" stroke="currentColor" stroke-width="0.5" fill="none"/><text style="font-family:sans-serif;font-size:2px;font-weight:600" x="32" y="8">Client (web browser, REST client)</text><line x1="20" y1="21" x2="20" y2="30" stroke="currentColor" stroke-width="0.5" /><rect x="10" y="30" width="20" height="10" rx="0" stroke="currentColor" stroke-width="0.5" fill="none"/><text style="font-family:sans-serif;font-size:1.75px;font-weight:400" x="22" y="26">HTML, JSON, Binary data over HTTPS</text><text style="font-family:sans-serif;font-size:2px;font-weight:600" x="32" y="34">Structr server</text><line x1="20" y1="40" x2="20" y2="50" stroke="currentColor" stroke-width="0.5" /><text style="font-family:sans-serif;font-size:1.75px;font-weight:400" x="22" y="46">Binary data over Bolt</text><rect x="10" y="50" width="20" height="10" rx="0" stroke="currentColor" stroke-width="0.5" fill="none"/><text style="font-family:sans-serif;font-size:2px;font-weight:600" x="32" y="54">Database server</text></svg>

### HTTPS

To secure the data transmission between the clients (Web browser, REST client etc.) and the server, the server has to be configured to support HTTPS and to force all incoming traffic to use HTTPS only.

The following steps are necessary to allow secure connections between HTTP clients and the Structr server only:

#### Create a certificate

We recommend using a TLS/SSL certificate from the free [Let's-Encrypt](https://letsencrypt.org/) service.

1. Create the certificate using the `/maintenance/letsencrypt` API endpoint or the `letsencrypt` method in Structr's `maintenance` API.
2. Enable HTTPS in the server configuration (`application.https.enabled = true`)
3. Configure port 80 for HTTP (`application.http.port = 80`)
4. Configure port 443 for HTTPS (`application.https.port = 443`)
5. Make the server redirect HTTP requests to HTTPS (`httpservice.force.https = true`)
6. Restart the web server

When configured correctly, the Structr server should be listening on port 80 and 443. Any request to the non-HTTPS port 80 should be redirected to port 443:

    $ curl -i http://my-server.example.org
    HTTP/1.1 302 Found
    Date: Tue, 12 Mar 3456 12:34:56 GMT
    Location: https://my-server.example.org/
    Content-Length: 0

## Securing API Endpoints

By default, access to any API endpoint is blocked for non-admin users. To allow access through one or more of the HTTP methods,  and has to be allowed explicitly for authenticated users, each specific access path and method.

Structr's security system supports the HTTP verb methods GET, PUT, POST, DELETE, OPTIONS, HEAD and PATCH.

## Enable Password Security Rules

Set the following configuration parameters to `true` in the configuration file `structr.conf`:

    security.passwordpolicy.complexity.enforce = true
    security.passwordpolicy.complexity.requiredigits = true
    security.passwordpolicy.complexity.requirelowercase = true
    security.passwordpolicy.complexity.requirenonalphanumeric = true
    security.passwordpolicy.complexity.requireuppercase = true
    security.passwordpolicy.onchange.clearsessions = true

## Database Encryption

### Encrypted strings

Instead of storing information in readable plain text, you can use the property type `EncryptedString` to store text in an encrypted format. Encryption and Decryption is based on the AES algorithm, using a pre-shared key that is stored in the configuration file.

## Securing the Structr Server

### File permissions

The installation of Structr leads to a number of folders and files on disk of the server instance used to operate Structr.

The following file permissions have to be in place in order to secure Structr from attacks by malicous operating system users:

- structr.conf: `-rw-------` (600)

When using Neo4j as database, please make sure that the following [recommendations for file permissions](https://neo4j.com/docs/operations-manual/current/configuration/file-locations/#file-locations-permissions) for the database files are met.

### Database-level encryption

To secure the data stored in the database in the case someone has stolen the physical hardware, you need to have to enable encryption for single files or an encrypted filesystem in place on operating system level.

This is called data-at-rest encryption and can only be enabled by means of the operating system in use. Please refer to the specific documentation of the OS you are using.