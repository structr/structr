# Settings

## access.control.accepted.origins
Comma-separated list of accepted origins, sets the <code>Access-Control-Allow-Origin</code> header.

## access.control.allow.credentials
Sets the value of the <code>Access-Control-Allow-Credentials</code> header.

## access.control.allow.headers
Sets the value of the <code>Access-Control-Allow-Headers</code> header.

## access.control.allow.methods
Sets the value of the <code>Access-Control-Allow-Methods</code> header. Comma-delimited list of the allowed HTTP request methods.

## access.control.expose.headers
Sets the value of the <code>Access-Control-Expose-Headers</code> header.

## access.control.max.age
Sets the value of the <code>Access-Control-Max-Age</code> header. Unit is seconds.

## application.baseurl.override
Overrides the baseUrl that can be used to prefix links to local web resources. By default, the value is assembled from the protocol, hostname and port of the server instance Structr is running on

## application.changelog.enabled
Turns on logging of changes to nodes and relationships

## application.changelog.user_centric.enabled
Turns on user-centric logging of what a user changed/created/deleted

## application.cluster.enabled
Enables cluster mode (experimental)

## application.cluster.log.enabled
Enables debug logging for cluster mode communication

## application.cluster.name
The name of the Structr cluster

## application.console.cypher.maxresults
The maximum number of results returned by a cypher query in the admin console. If a query yields more results, an error message is shown.

## application.email.validation.regex
Regular expression used to validate email addresses for User.eMail and is_valid_email() function.

## application.encryption.secret
Sets the global secret for encrypted string properties. Using this configuration setting is one of several possible ways to set the secret. Using the <code>set_encryption_key()</code> function is a way to set the encryption key without persisting it on disk.

## application.feeditem.indexing.remote
Whether indexing for type FeedItem will index the target URL of the FeedItem or the description

## application.feeditemcontent.indexing.enabled
Whether indexing is enabled for type FeedItemContent

## application.feeditemcontent.indexing.limit
Maximum number of words to be indexed per FeedItemContent.

## application.feeditemcontent.indexing.maxlength
Maximum length of words to be indexed for FeedItemContent

## application.feeditemcontent.indexing.minlength
Minimum length of words to be indexed for FeedItemContent

## application.filesystem.checksums.default
List of additional checksums to be calculated on file creation by default. (<code>File.checksum</code> is always popuplated with an xxHash)<dl><dt>crc32</dt><dd>Cyclic Redundancy Check - long value</dd><dt>md5</dt><dd>md5 algorithm - 32 character hex string</dd><dt>sha1</dt><dd>SHA-1 algorithm - 40 character hex string</dd><dt>sha512</dt><dd>SHA-512 algorithm - 128 character hex string</dd></dl>

## application.filesystem.enabled
If enabled, Structr will create a separate home directory for each user. The home directory of authenticated users will override the default upload folder setting. See Filesystem for more information.

## application.filesystem.indexing.enabled
Whether indexing is enabled globally (can be controlled separately for each file)

## application.filesystem.indexing.maxsize
Maximum size (MB) of a file to be indexed

## application.filesystem.unique.insertionposition
Defines the insertion position of the uniqueness criterion (currently a timestamp).<dl><dt>start</dt><dd>prefixes the name with a timestamp</dd><dt>beforeextension</dt><dd>puts the timestamp before the last dot (or at the end if the name does not contain a dot)</dd><dt>end</dt><dd>appends the timestamp after the complete name</dd></dl>

## application.filesystem.unique.paths
If enabled, Structr will not allow files/folders of the same name in the same folder and automatically rename the file.

## application.ftp.passiveportrange
FTP port range for pasv mode. Needed if Structr is run in a docker container, so the port mapping can be done correctly.

## application.ftp.port
FTP port the Structr server will listen on (if FtpService is enabled)

## application.heap.max_size
Maximum Java heap size (-Xmx). Examples: 2g, 4g, 8g. Note: Changes require a restart of Structr.

## application.heap.min_size
Minimum Java heap size (-Xms). Examples: 512m, 1g, 2g. Note: Changes require a restart of Structr.

## application.host
The listen address of the Structr server. You can set this to your domain name if that name resolves to the IP of the server the instance is running on.

## application.http.port
HTTP port the Structr server will listen on

## application.httphelper.charset
Default charset for outbound connections

## application.httphelper.timeouts.connect
Timeout for outbound connections in <b>seconds</b> to wait until a connection is established. A timeout value of zero is interpreted as an infinite timeout.

## application.httphelper.timeouts.connectionrequest
Timeout for outbound connections in <b>seconds</b> to wait when requesting a connection from the connection manager. A timeout value of zero is interpreted as an infinite timeout.

## application.httphelper.timeouts.socket
Socket timeout for outbound connections in <b>seconds</b> to wait for data or, put differently, a maximum inactivity period between two consecutive data packets. A timeout value of zero is interpreted as an infinite timeout.

## application.httphelper.urlwhitelist
A comma-separated list of URL patterns that can be used in HTTP request scripting functions (GET, PUT, POST etc.). If this value is anything other than *, whitelisting is applied to all outgoing requests.

## application.httphelper.useragent
User agent string for outbound connections

## application.https.enabled
Whether SSL is enabled

## application.https.port
HTTPS port the Structr server will listen on (if SSL is enabled)

## application.instance.name
The name of the Structr instance (displayed in the top right corner of structr-ui)

## application.instance.stage
The stage of the Structr instance (displayed in the top right corner of structr-ui)

## application.keystore.password
The password for the JKS keystore

## application.keystore.path
The path to the JKS keystore containing the SSL certificate. Default value is 'domain.key.keystore' which fits with the default value for letsencrypt.domain.key.filename which is 'domain.key'.

## application.legacy.requestparameters.enabled
Enables pre-4.0 request parameter names (sort, page, pageSize, etc. instead of _sort, _page, _pageSize, ...)

## application.localization.fallbacklocale
The default locale used, if no localization is found and using a fallback is active.

## application.localization.logmissing
Turns on logging for requested but non-existing localizations.

## application.localization.usefallbacklocale
Turns on usage of fallback locale if for the current locale no localization is found

## application.proxy.mode
Sets the mode of the proxy servlet. Possible values are 'disabled' (off, servlet responds with 503 error code), 'protected' (only authenticated requests allowed) and 'public' (anonymous requests allowed). Default is disabled.

## application.remotedocument.indexing.enabled
Whether indexing is enabled for type RemoteDocument

## application.remotedocument.indexing.limit
Maximum number of words to be indexed per RemoteDocument.

## application.remotedocument.indexing.maxlength
Maximum length of words to be indexed for RemoteDocument

## application.remotedocument.indexing.minlength
Minimum length of words to be indexed for RemoteDocument

## application.rest.path
Defines the URL path of the Structr REST server. Should not be changed because it is hard-coded in many parts of the application.

## application.root.path
Root path of the application, e.g. in case Structr is being run behind a reverse proxy with additional path prefix in URI. If set, the value must start with a '/' and have no trailing '/'. A valid value would be <code>/xyz</code> 

## application.runtime.enforce.recommended
Enforces version check for Java runtime.

## application.schema.allowunknownkeys
Enables get() and set() built-in functions to use property keys that are not defined in the schema.

## application.schema.automigration
Enable automatic migration of schema information between versions (if possible -- may delete schema nodes)

## application.scripting.allowedhostclasses
Space-separated list of fully-qualified Java class names that you can load dynamically in a scripting environment.

## application.scripting.debugger
Enables <b>Chrome</b> debugger initialization in scripting engine. The current debugger URL will be shown in the server log and also made available on the dashboard.

## application.scripting.js.wrapinmainfunction
Forces js scripts to be wrapped in a main function for legacy behaviour.

## application.session.clear.onshutdown
Clear all sessions on shutdown if set to true.

## application.session.clear.onstartup
Clear all sessions on startup if set to true.

## application.session.max.number
The maximum number of active sessions per user. Default is -1 (unlimited).

## application.session.timeout
The session timeout for inactive HTTP sessions in seconds. Default is 1800. Values lower or equal than 0 indicate that sessions never time out.

## application.ssh.forcepublickey
Force use of public key authentication for SSH connections

## application.ssh.port
SSH port the Structr server will listen on (if SSHService is enabled)

## application.stats.aggreation.interval
Minimum aggregation interval for HTTP request stats.

## application.systeminfo.disabled
Disables transmission of telemetry information. This information is used to improve the software and to better adapt to different hardware configurations.

## application.timezone
Application timezone (e.g. UTC, Europe/Berlin). If not set, falls back to system timezone or UTC. Note: Changes require a restart of Structr.

## application.title
The title of the application as shown in the log file. This entry exists for historical reasons and has no functional impact other than appearing in the log file.

## application.uploads.folder
The default upload folder for files uploaded via the UploadServlet. This must be a valid folder path and can not be empty (uploads to the root directory are not allowed).

## application.uuid.allowedformats
 		Configures which UUIDv4 types are allowed: With dashes, without dashes or both.<br>
 		<br><strong>WARNING</strong>: Allowing both UUIDv4 formats to be accepted is not supported and strongly recommended against! It should only be used for temporary migration scenarios!<br>
 		<br><strong>WARNING</strong>: If changed after data was already created, this could prevent access to data objects. Only change this setting with an empty database.<br>
 		<br><strong>INFO</strong>: Requires a restart to take effect.


## application.uuid.createcompact
Determines if UUIDs are created with or without dashes. This setting is only used if <strong>application.uuid.allowedformats</strong> is set to <strong>both</strong>.<br><br><strong>WARNING</strong>: Requires a restart to take effect.

## application.xml.parser.security
Enables various security measures for XML parsing to prevent exploits.

## base.path
Path of the Structr working directory. All files will be located relative to this directory.

## callbacks.login.onsave
Setting this to true enables the execution of the User.onSave method for login actions. This will also trigger for failed login attempts and for two-factor authentication intermediate steps. Disabled by default because the global login handler onStructrLogin would be the right place for such functionality.

## callbacks.logout.onsave
Setting this to true enables the execution of the User.onSave method when a user logs out. Disabled by default because the global login handler onStructrLogout would be the right place for such functionality.

## changelog.path
Path to the Structr changelog storage folder

## configservlet.enabled
Enables the config servlet (available under <code>http(s)://&lt;your-server&gt;/structr/config</code>)

## configuration.provider
Fully-qualified class name of a Java class in the current class path that implements the <code>org.structr.schema.ConfigurationProvider</code> interface.

## configured.services
Services that are listed in this configuration key will be started when Structr starts.

## confirmationkey.passwordreset.validityperiod
Validity period (in minutes) of the confirmation key generated when a user resets his password. Default is 30.

## confirmationkey.registration.validityperiod
Validity period (in minutes) of the confirmation key generated during self registration. Default is 2 days (2880 minutes)

## confirmationkey.validwithouttimestamp
How to interpret confirmation keys without a timestamp

## cronservice.allowparallelexecution
Enables the parallel execution of *the same* cron job. This can happen if the method runs longer than the defined cron interval. Since this could lead to problems, the default is false.

## cronservice.tasks
List with cron task configurations or method names. This only configures the list of tasks. For each task, there needs to be another configuration entry named '<taskname>.cronExpression' with the appropriate cron schedule configuration.

## csvservlet.authenticator
FQCN of Authenticator class to use for CSV output. Do not change unless you know what you are doing.

## csvservlet.class
Servlet class to use for CSV output. Do not change unless you know what you are doing.

## csvservlet.defaultview
Default view to use when no view is given in the URL

## csvservlet.frontendaccess
Unused

## csvservlet.outputdepth
Maximum nesting depth of JSON output

## csvservlet.path
URL pattern for CSV output. Do not change unless you know what you are doing.

## csvservlet.resourceprovider
FQCN of resource provider class to use in the REST server. Do not change unless you know what you are doing.

## csvservlet.user.autocreate
Unused

## csvservlet.user.autologin
Unused

## data.exchange.path
IMPORTANT: Path is relative to base.path

## database.cache.uuid.size
Size of the database driver relationship cache

## database.prefetching.maxcount
How many results a prefetching query may return before prefetching will be deactivated for that query.

## database.prefetching.maxduration
How long a prefetching query may take before prefetching will be deactivated for that query.

## database.prefetching.threshold
How many identical queries must run in a transaction to activate prefetching for that query.

## database.result.fetchsize
Number of database records to fetch per batch when fetching large results

## database.result.lazy
Forces Structr to use lazy evaluation for relationship queries

## database.result.softlimit
Soft result count limit for a single query (can be overridden by setting the <code>_pageSize</code> request parameter or by adding the request parameter <code>_disableSoftLimit</code> to a non-null value)

## dateproperty.defaultformat
Default ISO8601 date format pattern

## deployment.data.export.nodes.batchsize
Sets the batch size for data deployment when exporting nodes.<br><br>The relationships for each node are collected and exported while the node itself is exported. It can make sense to reduce this number, if all/most nodes have very high amount of relationships.

## deployment.data.import.nodes.batchsize
Sets the batch size for data deployment when importing nodes.

## deployment.data.import.relationships.batchsize
Sets the batch size for data deployment when importing relationships.

## deployment.schema.format
Configures how the schema is exported in a deployment export. <code>file</code> exports the schema as a single file. <code>tree</code> exports the schema as a tree where methods/function properties are written to single files in a tree structure.

## deploymentservlet.filegroup.name
For unix based file systems only. Adds the group ownership to the created deployment files.

## files.path
Path to the Structr file storage folder

## flowservlet.defaultview
Default view to use when no view is given in the URL.

## flowservlet.outputdepth
Maximum nesting depth of JSON output.

## flowservlet.path
The URI under which requests are accepted by the servlet. Needs to include a wildcard at the end.

## geocoding.apikey
Geocoding configuration

## geocoding.language
Geocoding configuration

## geocoding.provider
Geocoding configuration

## healthcheckservlet.whitelist
IP addresses in this list are allowed to access the health check endpoint at /structr/health.

## histogramservlet.whitelist
IP addresses in this list are allowed to access the query histogram endpoint at /structr/histogram.

## html.indentation
Whether the page source should be indented (beautified) or compacted. Note: Does not work for template/content nodes which contain raw HTML

## htmlservlet.authenticator
FQCN of authenticator class to use for HTTP requests. Do not change unless you know what you are doing.

## htmlservlet.class
FQCN of servlet class to use for HTTP requests. Do not change unless you know what you are doing.

## htmlservlet.customresponseheaders
List of custom response headers that will be added to every HTTP response

## htmlservlet.defaultview
Not used for HtmlServlet

## htmlservlet.outputdepth
Not used for HtmlServlet

## htmlservlet.path
URL pattern for HTTP server. Do not change unless you know what you are doing.

## htmlservlet.resolveproperties
Specifies the list of properties that are be used to resolve entities from URL paths.

## htmlservlet.resourceprovider
FQCN of resource provider class to use in the HTTP server. Do not change unless you know what you are doing.

## httpservice.async
Whether the HttpServices uses asynchronous request handling. Disable this option if you encounter problems with HTTP responses.

## httpservice.connection.ratelimit
Defines the rate limit of HTTP/2 frames per connection for the HTTP Service.

## httpservice.cookies.httponly
Set HttpOnly to true for cookies. Please note that this will disable backend access!

## httpservice.cookies.samesite
Sets the SameSite attribute for the JSESSIONID cookie. For SameSite=None the Secure flag must also be set, otherwise the cookie will be rejected by the browser!

## httpservice.cookies.secure
Sets the secure flag for the JSESSIONID cookie.

## httpservice.force.https
Enables redirecting HTTP requests from the configured HTTP port to the configured HTTPS port (only works if HTTPS is active).

## httpservice.gzip.enabled
Use GZIP compression for HTTP transfers

## httpservice.httpbasicauth.enabled
Enables HTTP Basic Auth support for pages and files

## httpservice.servlets
Servlets that are listed in this configuration key will be available in the HttpService. Changes to this setting require a restart of the HttpService in the 'Services' tab.

## httpservice.sni.hostcheck
Enables SNI host check.

## httpservice.sni.required
Enables strict SNI check for the http service.

## httpservice.uricompliance
	Configures the URI compliance for the Jetty server. This is simply passed down and is Jetty's own specification.
	<dl>
		<dt>RFC3986</dt>
		<dd>Compliance mode that exactly follows <a href='https://tools.ietf.org/html/rfc3986'>RFC3986</a>, including allowing all additional ambiguous URI Violations.</dd>

		<dt>JETTY_DEFAULT</dt>
		<dd>Compliance mode that extends <a href='https://tools.ietf.org/html/rfc3986'>RFC3986</a> compliance with additional violations to avoid most ambiguous URIs. This mode does allow ambiguous path separator within a URI segment e.g. <code>/foo/b%2fr</code>, but disallows all out violations.</dd>

		<dt>LEGACY</dt>
		<dd>LEGACY compliance mode that models Jetty-9.4 behavior by allowing ambiguous path segments e.g. <code>/foo/%2e%2e/bar</code>, ambiguous empty segments e.g. <code>//</code>, ambiguous path separator within a URI segment e.g. <code>/foo/b%2fr</code>, ambiguous path encoding within a URI segment e.g. <code>/%2557EB-INF</code> and UTF-16 encoding e.g. <code>/foo%u2192bar</code>.</dd>

		<dt>RFC3986_UNAMBIGUOUS</dt>
		<dd>Compliance mode that follows <a href='https://tools.ietf.org/html/rfc3986'>RFC3986</a> plus it does not allow any ambiguous URI violations.</dd>

		<dt>UNSAFE</dt>
		<dd>Compliance mode that allows all URI Violations, including allowing ambiguous paths in non canonicalized form.</dd>
	</dl>

	<br><strong>WARNING</strong>: Requires a restart (of at least the HttpService).


## initialuser.create
Enables or disables the creation of an initial admin user when connecting to a database that has never been used with structr.

## initialuser.name
Name of the initial admin user. This will only be set if the user is created.

## initialuser.password
Password of the initial admin user. This will only be set if the user is created.

## json.indentation
Whether JSON output should be indented (beautified) or compacted

## json.lenient
Whether to use lenient serialization, e.g. allow to serialize NaN, -Infinity, Infinity instead of just returning null. Note: as long as Javascript doesn’t support NaN etc., most of the UI will be broken

## json.output.dateformat
Output format pattern for date objects in JSON

## json.output.forcearrays
If enabled, collections with a single element are always represented as a collection.

## json.reductiondepth
For restricted views (ui, custom, all), only a limited amount of attributes (id, type, name) are rendered for nested objects after this depth. The default is 0, meaning that on the root depth (0), all attributes are rendered and reduction starts at depth 1.<br><br>Can be overridden on a per-request basis by using the request parameter <code>_outputReductionDepth</code>

## json.redundancyreduction
If enabled, nested nodes (which were already rendered in the current output) are rendered with limited set of attribute (id, type, name).

## jsonrestservlet.authenticator
FQCN of authenticator class to use in the REST server. Do not change unless you know what you are doing.

## jsonrestservlet.class
FQCN of servlet class to use in the REST server. Do not change unless you know what you are doing.

## jsonrestservlet.defaultview
Default view to use when no view is given in the URL

## jsonrestservlet.outputdepth
Maximum nesting depth of JSON output

## jsonrestservlet.path
URL pattern for REST server. Do not change unless you know what you are doing.

## jsonrestservlet.resourceprovider
FQCN of resource provider class to use in the REST server. Do not change unless you know what you are doing.

## jsonrestservlet.unknowninput.validation.mode
Controls how Structr reacts to unknown keys in JSON input. <code>accept</code> allows the unknown key to be written. <code>ignore</code> removes the key. <code>reject</code> rejects the complete request. The <code>warn</code> options behave identical but also log a warning.

## jsonrestservlet.user.autocreate
Enable this to support user self registration

## jsonrestservlet.user.autologin
Only works in conjunction with the jsonrestservlet.user.autocreate key. Will log in user after self registration.

## jsonrestservlet.user.class
User class that is instantiated when new users are created via the servlet

## letsencrypt.challenge.type
Challenge type for Let's Encrypt authorization. Possible values are 'http' and 'dns'.

## letsencrypt.domain.chain.filename
File name of the Let's Encrypt domain chain. Default is 'domain-chain.crt'.

## letsencrypt.domain.csr.filename
File name of the Let's Encrypt CSR. Default is 'domain.csr'.

## letsencrypt.domain.key.filename
File name of the Let's Encrypt domain key. Default is 'domain.key'.

## letsencrypt.domains
Space-separated list of domains to fetch and update Let's Encrypt certificates for

## letsencrypt.key.size
Encryption key length. Default is 2048.

## letsencrypt.production.server.url
URL of Let's Encrypt server. Default is 'acme://letsencrypt.org'

## letsencrypt.staging.server.url
URL of Let's Encrypt staging server for testing only. Default is 'acme://letsencrypt.org/staging'.

## letsencrypt.user.key.filename
File name of the Let's Encrypt user key. Default is 'user.key'.

## letsencrypt.wait
Wait for this amount of seconds before trying to authorize challenge. Default is 300 seconds (5 minutes).

## license.allow.fallback
Allow Structr to fall back to the Community License if no valid license exists (or license cannot be validated). Set this to false in production environments to prevent Structr from starting without a license.

## license.key
Base64-encoded string that contains the complete license data, typically saved as 'license.key' in the main directory.

## license.validation.timeout
Timeout in seconds for license validation requests.

## log.callback.threshold
Number of callbacks after which a transaction will be logged.

## log.cypher.debug
Turns on debug logging for the generated Cypher queries

## log.cypher.debug.ping
Turns on debug logging for the generated Cypher queries of the websocket PING command. Can only be used in conjunction with log.cypher.debug

## log.directorywatchservice.scanquietly
Prevents logging of each scan process for every folder processed by the directory watch service

## log.functions.stacktrace
If true, the full stacktrace is logged for exceptions in system functions.

## log.level
Configures the default log level. Takes effect immediately.

## log.querytime.threshold
Milliseconds after which a long-running query will be logged.

## log.scriptprocess.commandline
Configures the default logging behaviour for the command line generated for script processes. This applies to the exec()- and exec_binary() functions, as well as some processes handling media conversion or processing. For the exec() and exec_binary() function, this can be overridden for each call of the function.

## loginservlet.defaultview
Default view to use when no view is given in the URL.

## loginservlet.outputdepth
Maximum nesting depth of JSON output.

## loginservlet.path
The URI under which requests are accepted by the servlet. Needs to include a wildcard at the end.

## logoutservlet.defaultview
Default view to use when no view is given in the URL.

## logoutservlet.outputdepth
Maximum nesting depth of JSON output.

## logoutservlet.path
The URI under which requests are accepted by the servlet. Needs to include a wildcard at the end.

## maintenance.application.ftp.port
FTP port the Structr server will listen on (if FtpService is enabled) in maintenance mode

## maintenance.application.http.port
HTTP port the Structr server will listen on in maintenance mode

## maintenance.application.https.port
HTTPS port the Structr server will listen on (if SSL is enabled) in maintenance mode

## maintenance.application.ssh.port
SSH port the Structr server will listen on (if SSHService is enabled) in maintenance mode

## maintenance.enabled
Enables maintenance mode where all ports can be changed to prevent users from accessing the application during maintenance.

## maintenance.message
Text for default maintenance page (HTML is allowed)

## maintenance.resource.path
The local folder for static resources served in maintenance mode. If no path is provided the a default maintenance page with customizable text is shown in maintenance mode.

## metricsservlet.whitelist
IP addresses in this list are allowed to access the health check endpoint at /structr/metrics.

## oauth.auth0.accesstoken.location
Where to encode  the access token when accessing the userinfo endpoint. Set this to header if you use an OICD-compliant service. 

## oauth.auth0.audience
The API audience of the application in Auth0.

## oauth.auth0.authorization_location
URL of the authorization endpoint.

## oauth.auth0.client_id
Client ID use for oauth.

## oauth.auth0.client_secret
Client secret used for oauth.

## oauth.auth0.error_uri
Structr redirects to this URI on unsuccessful authentication.

## oauth.auth0.logout_location
URL of the logout endpoint.

## oauth.auth0.logout_return_location_parameter
Provider specific URL parameter that carries the value of the return location after successfull logout.

## oauth.auth0.logout_return_uri
Structr redirects to this URI on successfull logout.

## oauth.auth0.redirect_uri
Structr redirects to this URI on successful authentification.

## oauth.auth0.return_uri
Structr redirects to this URI on successful authentification.

## oauth.auth0.scope
Specifies the scope of the authentifcation.

## oauth.auth0.token_location
URL of the token endpoint.

## oauth.auth0.user_details_resource_uri
Points to the user details endpoint of the service provider.

## oauth.azure.accesstoken.location
Where to encode  the access token when accessing the userinfo endpoint. Set this to header if you use an OICD-compliant service. 

## oauth.azure.authorization_location
URL of the authorization endpoint.

## oauth.azure.client_id
Client ID use for oauth.

## oauth.azure.client_secret
Client secret used for oauth.

## oauth.azure.error_uri
Structr redirects to this URI on unsuccessful authentication.

## oauth.azure.logout_location
URL of the logout endpoint.

## oauth.azure.logout_return_location_parameter
Provider specific URL parameter that carries the value of the return location after successfull logout.

## oauth.azure.logout_return_uri
Structr redirects to this URI on successfull logout.

## oauth.azure.redirect_uri
Structr redirects to this URI on successful authentification.

## oauth.azure.return_uri
Structr redirects to this URI on successful authentification.

## oauth.azure.scope
Specifies the scope of the authentifcation.

## oauth.azure.token_location
URL of the token endpoint.

## oauth.azure.user_details_resource_uri
Points to the user details endpoint of the service provider.

## oauth.facebook.accesstoken.location
Where to encode  the access token when accessing the userinfo endpoint. Set this to header if you use an OICD-compliant service. 

## oauth.facebook.authorization_location
URL of the authorization endpoint.

## oauth.facebook.client_id
Client ID used for oauth.

## oauth.facebook.client_secret
Client secret used for oauth

## oauth.facebook.error_uri
Structr redirects to this URI on unsuccessful authentication.

## oauth.facebook.redirect_uri
Structr redirects to this URI on successful authentification.

## oauth.facebook.return_uri
Structr redirects to this URI on successful authentification.

## oauth.facebook.scope
Specifies the scope of the authentifcation.

## oauth.facebook.token_location
URL of the token endpoint.

## oauth.facebook.user_details_resource_uri
Points to the user details endpoint of the service provider.

## oauth.github.accesstoken.location
Where to encode  the access token when accessing the userinfo endpoint. Set this to header if you use an OICD-compliant service. 

## oauth.github.authorization_location
URL of the authorization endpoint.

## oauth.github.client_id
Client ID used for oauth.

## oauth.github.client_secret
Client secret used for oauth

## oauth.github.error_uri
Structr redirects to this URI on unsuccessful authentication.

## oauth.github.redirect_uri
Structr endpoint for the service oauth authorization.

## oauth.github.return_uri
Structr redirects to this URI on successful authentification.

## oauth.github.scope
Specifies the scope of the authentifcation. Defaults to 'user:email'.

## oauth.github.token_location
URL of the token endpoint.

## oauth.github.user_details_resource_uri
Points to the user details endpoint of the service provider.

## oauth.google.accesstoken.location
Where to encode  the access token when accessing the userinfo endpoint. Set this to header if you use an OICD-compliant service. 

## oauth.google.authorization_location
URL of the authorization endpoint.

## oauth.google.client_id
Client ID used for oauth.

## oauth.google.client_secret
Client secret used for oauth

## oauth.google.error_uri
Structr redirects to this URI on unsuccessful authentication.

## oauth.google.redirect_uri
Structr redirects to this URI on successful authentification.

## oauth.google.return_uri
Structr redirects to this URI on successful authentification.

## oauth.google.scope
Specifies the scope of the authentifcation.

## oauth.google.token_location
URL of the token endpoint.

## oauth.linkedin.accesstoken.location
Where to encode  the access token when accessing the userinfo endpoint. Set this to header if you use an OICD-compliant service. 

## oauth.linkedin.authorization_location
URL of the authorization endpoint.

## oauth.linkedin.client_id
Client ID used for oauth.

## oauth.linkedin.client_secret
Client secret used for oauth

## oauth.linkedin.error_uri
Structr redirects to this URI on unsuccessful authentication.

## oauth.linkedin.redirect_uri
Structr redirects to this URI on successful authentification.

## oauth.linkedin.return_uri
Structr redirects to this URI on successful authentification.

## oauth.linkedin.scope
oauth.linkedin.scope

## oauth.linkedin.token_location
URL of the token endpoint.

## oauth.linkedin.user_details_resource_uri
Points to the user details endpoint of the service provider.

## oauth.linkedin.user_profile_resource_uri
Points to the user profile endpoint of the service provider.

## oauth.logging.verbose
Enables verbose logging for oauth login

## oauth.servers
Space-separated List of available oauth services. Defaults to a list of all available services.

## openapiservlet.server.title
The main title of the OpenAPI server definition.

## openapiservlet.server.version
The version number of the OpenAPI definition

## pdfservlet.customresponseheaders
List of custom response headers that will be added to every HTTP response

## pdfservlet.defaultview
Default view to use when no view is given in the URL.

## pdfservlet.outputdepth
Maximum nesting depth of JSON output.

## pdfservlet.path
The URI under which requests are accepted by the servlet. Needs to include a wildcard at the end.

## pdfservlet.resolveproperties
Specifies the list of properties that are be used to resolve entities from URL paths.

## registration.allowloginbeforeconfirmation
Enables self-registered users to login without clicking the activation link in the registration email.

## registration.customuserattributes
Attributes the registering user is allowed to provide. All other attributes are discarded. (eMail is always allowed)

## security.authentication.propertykeys
List of property keys separated by space in the form of <Type>.<key> (example: 'Member.memberId') to be used in addition to the default 'Principal.name Principal.eMail'

## security.jwks.admin.claim.key
The name of the key in the JWKS response claims in whose values is searched for a value matching the value of security.jwks.admin.claim.value.

## security.jwks.admin.claim.value
The value that must be present in the JWKS response claims object with the key given in security.jwks.admin.claim.key in order to give the requesting user admin privileges.

## security.jwks.group.claim.key
The name of the key in the JWKS response claims whose value(s) will be used to look for Group nodes with a matching jwksReferenceId.

## security.jwks.id.claim.key
The name of the key in the JWKS response claims whose value will be used as the ID of the temporary principal object.

## security.jwks.name.claim.key
The name of the key in the JWKS response claims whose value will be used as the name of the temporary principal object.

## security.jwks.provider
URL of the JWKS provider

## security.jwt.expirationtime
Access token timeout in minutes.

## security.jwt.jwtissuer
The issuer for the JWTs created by this Structr instance.

## security.jwt.key.alias
The alias of the private key of the given 'security.jwt.keystore'

## security.jwt.keystore
Used if 'security.jwt.secrettype'=keypair. A valid keystore file containing a private/public keypair that can be used to sign and verify JWTs

## security.jwt.keystore.password
The password for the given 'security.jwt.keystore'

## security.jwt.refreshtoken.expirationtime
Refresh token timeout in minutes.

## security.jwt.secret
Used if 'security.jwt.secrettype'=secret. The secret that will be used to sign and verify all tokens issued and sent to Structr. Must have a min. length of 32 characters.

## security.jwt.secrettype
Selects the secret type that will be used to sign or verify a given access or refresh token

## security.passwordpolicy.complexity.enforce
Configures if password complexity is enforced for user passwords. If active, changes which violate the complexity rules, will result in an error and must be accounted for.

## security.passwordpolicy.complexity.minlength
The minimum length for user passwords (only active if the enforce setting is active)

## security.passwordpolicy.complexity.requiredigits
Require at least one digit in user passwords (only active if the enforce setting is active)

## security.passwordpolicy.complexity.requirelowercase
Require at least one lower case character in user passwords (only active if the enforce setting is active)

## security.passwordpolicy.complexity.requirenonalphanumeric
Require at least one non alpha-numeric character in user passwords (only active if the enforce setting is active)

## security.passwordpolicy.complexity.requireuppercase
Require at least one upper case character in user passwords (only active if the enforce setting is active)

## security.passwordpolicy.forcechange
Indicates if a forced password change is active

## security.passwordpolicy.maxage
The number of days after which a user has to change his password

## security.passwordpolicy.maxfailedattempts
The maximum number of failed login attempts before a user is blocked. (Can be disabled by setting to zero or a negative number)

## security.passwordpolicy.onchange.clearsessions
Clear all sessions of a user on password change.

## security.passwordpolicy.remindtime
The number of days (before the user must change the password) where a warning should be issued. (Has to be handled in application code)

## security.passwordpolicy.resetfailedattemptsonpasswordreset
Configures if resetting the users password also resets the failed login attempts counter

## security.twofactorauthentication.algorithm
Respected by the most recent Google Authenticator implementations. <i>Warning: Changing this setting after users are already confirmed will effectively lock them out. Set [User].twoFactorConfirmed to false to show them a new QR code.</i>

## security.twofactorauthentication.digits
Respected by the most recent Google Authenticator implementations. <i>Warning: Changing this setting after users are already confirmed may lock them out. Set [User].twoFactorConfirmed to false to show them a new QR code.</i>

## security.twofactorauthentication.issuer
Must be URL-compliant in order to scan the created QR code

## security.twofactorauthentication.loginpage
The application page where the user enters the current two factor token

## security.twofactorauthentication.logintimeout
Defines how long the two-factor login time window in seconds is. After entering the username and password the user has this amount of time to enter a two factor token before he has to re-authenticate via password

## security.twofactorauthentication.period
Defines the period that a TOTP code will be valid for, in seconds.<br>Respected by the most recent Google Authenticator implementations. <i>Warning: Changing this setting after users are already confirmed will effectively lock them out. Set [User].twoFactorConfirmed to false to show them a new QR code.</i>

## security.twofactorauthentication.whitelistedips
A comma-separated (,) list of IPs for which two factor authentication is disabled. Both IPv4 and IPv6 are supported. CIDR notation is also supported. (e.g. 192.168.0.1/24 or 2A01:598:FF30:C500::/64)

## smtp.host
Address of the SMTP server used to send e-mails

## smtp.port
SMTP server port to use when sending e-mails

## smtp.tls.enabled
Whether to use TLS when sending e-mails

## smtp.tls.required
Whether TLS is required when sending e-mails

## superuser.password
Password of the superuser

## superuser.username
Name of the superuser

## tmp.path
Path to the temporary directory. Uses <code>java.io.tmpdir</code> by default

## tokenservlet.defaultview
Default view to use when no view is given in the URL.

## tokenservlet.outputdepth
Maximum nesting depth of JSON output.

## tokenservlet.path
The URI under which requests are accepted by the servlet. Needs to include a wildcard at the end.

## uploadservlet.allowanonymousuploads
Allows anonymous users to upload files.

## uploadservlet.authenticator
FQCN of authenticator class to use for file upload. Do not change unless you know what you are doing.

## uploadservlet.class
FQCN of servlet class to use for file upload. Do not change unless you know what you are doing.

## uploadservlet.defaultview
Default view to use when no view is given in the URL

## uploadservlet.maxfilesize
Maximum allowed file size for single file uploads. Unit is Megabytes

## uploadservlet.maxrequestsize
Maximum allowed request size for single file uploads. Unit is Megabytes

## uploadservlet.outputdepth
Maximum nesting depth of JSON output

## uploadservlet.path
URL pattern for file upload. Do not change unless you know what you are doing.

## uploadservlet.resourceprovider
FQCN of resource provider class to use for file upload. Do not change unless you know what you are doing.	

## uploadservlet.user.autocreate
Unused

## uploadservlet.user.autologin
Unused

## websocketservlet.authenticator
FQCN of authenticator class to use for WebSockets. Do not change unless you know what you are doing.

## websocketservlet.class
FQCN of servlet class to use for WebSockets. Do not change unless you know what you are doing.

## websocketservlet.defaultview
Unused

## websocketservlet.outputdepth
Maximum nesting depth of JSON output

## websocketservlet.path
URL pattern for WebSockets. Do not change unless you know what you are doing.

## websocketservlet.resourceprovider
FQCN of resource provider class to use with WebSockets. Do not change unless you know what you are doing.

## websocketservlet.user.autocreate
Unused

## websocketservlet.user.autologin
Unused

## ws.indentation
Prettyprints websocket responses if set to true.

## zoneddatetimeproperty.defaultformat
Default zoneddatetime format pattern
