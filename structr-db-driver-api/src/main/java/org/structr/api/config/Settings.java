/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.api.config;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.regex.Pattern;

/**
 * The Structr configuration settings.
 */
public class Settings {

	private static String uuidOnlyRegex;
	private static String uuidPartRegex;
	private static final Logger logger         = LoggerFactory.getLogger(Settings.class);

	private static Pattern uuidPattern;

	public static final String ConfigFileName                 = "structr.conf";

	public static final String DEFAULT_DATABASE_DRIVER        = "org.structr.memory.MemoryDatabaseService";
	public static final String DEFAULT_REMOTE_DATABASE_DRIVER = "org.structr.bolt.BoltDatabaseService";

	public static final String MAINTENANCE_PREFIX             = "maintenance";

	public static final String CRON_EXPRESSION_INFO_HTML      = "A cron expression is defined as <pre>&lt;s&gt; &lt;m&gt; &lt;h&gt; &lt;dom&gt; &lt;m&gt; &lt;dow&gt;</pre> It is similar to a normal cron expression with an additional \"seconds\" field at the beginning. Search for \"cron\" or \"periodic task scheduler\" in the documentation to find more info and examples.";

	private static final Set<PosixFilePermission> expectedConfigFilePermissions = Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

	public enum POSSIBLE_UUID_V4_FORMATS {
		without_dashes,
		with_dashes,
		both
	}

	private static final Map<String, Setting> settings        = new TreeMap<>();
	private static final Map<String, SettingsGroup> groups    = new TreeMap<>();

	public static final SettingsGroup generalGroup            = new SettingsGroup("general",     "General Settings");
	public static final SettingsGroup serverGroup             = new SettingsGroup("server",      "Server Settings");
	public static final SettingsGroup dosFilterGroup          = new SettingsGroup("dosfilter",   "DoS Filter Settings");
	public static final SettingsGroup databaseGroup           = new SettingsGroup("database",    "Database Configuration");
	public static final SettingsGroup applicationGroup        = new SettingsGroup("application", "Application Configuration");
	public static final SettingsGroup smtpGroup               = new SettingsGroup("smtp",        "Mail Configuration");
	public static final SettingsGroup advancedGroup           = new SettingsGroup("advanced",    "Advanced Settings");
	public static final SettingsGroup servletsGroup           = new SettingsGroup("servlets",    "Servlet Settings");
	public static final SettingsGroup cronGroup               = new SettingsGroup("cron",        "Cron Jobs");
	public static final SettingsGroup securityGroup           = new SettingsGroup("security",    "Security Settings");
	public static final SettingsGroup oauthGroup              = new SettingsGroup("oauth",       "OAuth Settings");
	public static final SettingsGroup miscGroup               = new SettingsGroup("misc",        "Miscellaneous");
	public static final SettingsGroup licensingGroup          = new SettingsGroup("licensing",   "Licensing");

	// general settings
	public static final Setting<String> ApplicationTitle            = new StringSetting(generalGroup,          "Application", "application.title",                            "Structr", "The title of the application as shown in the log file. This entry exists for historical reasons and has no functional impact other than appearing in the log file.");
	public static final Setting<String> InstanceName                = new StringSetting(generalGroup,          "Application", "application.instance.name",                    "", "The name of the Structr instance (displayed in the top right corner of structr-ui)");
	public static final Setting<String> InstanceStage               = new StringSetting(generalGroup,          "Application", "application.instance.stage",                   "", "The stage of the Structr instance (displayed in the top right corner of structr-ui)");
	public static final Setting<Integer> CypherConsoleMaxResults    = new IntegerSetting(generalGroup,         "Application", "application.console.cypher.maxresults",        10, "The maximum number of results returned by a cypher query in the admin console. If a query yields more results, an error message is shown.");
	public static final Setting<Boolean> EnforceRuntime             = new BooleanSetting(generalGroup,         "Application", "application.runtime.enforce.recommended",      false, "Enforces version check for Java runtime.");
	public static final Setting<Boolean> DisableSendSystemInfo      = new BooleanSetting(generalGroup,         "Application", "application.systeminfo.disabled",              false, "Disables transmission of telemetry information. This information is used to improve the software and to better adapt to different hardware configurations.");
	public static final Setting<Boolean> RequestParameterLegacyMode = new BooleanSetting(generalGroup,         "Application", "application.legacy.requestparameters.enabled", false, "Enables pre-4.0 request parameter names (sort, page, pageSize, etc. instead of _sort, _page, _pageSize, ...)");

	public static final Setting<String> JavaHeapMin                 = new StringSetting(generalGroup, "Application", "application.heap.min_size", "1g", "Minimum Java heap size (-Xms). Examples: 512m, 1g, 2g. Note: Changes require a restart of Structr.");
	public static final Setting<String> JavaHeapMax                 = new StringSetting(generalGroup, "Application", "application.heap.max_size", "4g", "Maximum Java heap size (-Xmx). Examples: 2g, 4g, 8g. Note: Changes require a restart of Structr.");
	public static final Setting<String> ApplicationTimezone         = new StringSetting(generalGroup, "Application", "application.timezone", "", "Application timezone (e.g. UTC, Europe/Berlin). If not set, falls back to system timezone or UTC. Note: Changes require a restart of Structr.");

	public static final Setting<String> UUIDv4AllowedFormats        = new ChoiceSetting(generalGroup,          "Application", "application.uuid.allowedformats",             "without_dashes", Settings.getAllowedUUIDv4FormatOptions(), "Configures which UUIDv4 types are allowed: With dashes, without dashes or both.").setLongDescription("""
		<br><strong>WARNING</strong>: Allowing both UUIDv4 formats to be accepted is not supported and strongly recommended against! It should only be used for temporary migration scenarios!<br>
		<br><strong>WARNING</strong>: If changed after data was already created, this could prevent access to data objects. Only change this setting with an empty database.<br>
		<br><strong>INFO</strong>: Requires a restart to take effect.
		""");
	public static final Setting<Boolean> UUIDv4CreateCompact        = new BooleanSetting(generalGroup,         "Application", "application.uuid.createcompact",              true, "Determines if UUIDs are created with or without dashes. This setting is only used if <strong>" + Settings.UUIDv4AllowedFormats.getKey() + "</strong> is set to <strong>" + POSSIBLE_UUID_V4_FORMATS.both.toString() + "</strong>.<br><br><strong>WARNING</strong>: Requires a restart to take effect.");
	public static final Setting<String> EmailValidationRegex        = new StringSetting(generalGroup,          "Application", "application.email.validation.regex", "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$", "Regular expression used to validate email addresses for User.eMail and is_valid_email() function.");
	private static Pattern emailValidationPattern = Pattern.compile(Settings.EmailValidationRegex.getValue());

	// scripting related settings
	public static final Setting<Boolean> ScriptingDebugger          = new BooleanSetting(generalGroup,         "Scripting",   "application.scripting.debugger",               false,"Enables <b>Chrome</b> debugger initialization in scripting engine. The current debugger URL will be shown in the server log and also made available on the dashboard.");
	public static final Setting<Boolean> WrapJSInMainFunction       = new BooleanSetting(generalGroup,         "Scripting",   "application.scripting.js.wrapinmainfunction",  false,"Forces js scripts to be wrapped in a main function for legacy behaviour.");

	public static final Setting<String> AllowedHostClasses          = new StringSetting(generalGroup,          "Scripting",   "application.scripting.allowedhostclasses",     "", "Space-separated list of fully-qualified Java class names that you can load dynamically in a scripting environment.");
	public static final Setting<String> ScriptingPolyglotAccess     = new StringSetting(generalGroup,          "Scripting",   "application.scripting.polyglot.access",        "ALL", "Controls cross-language interop inside GraalVM scripting contexts. <code>ALL</code> (default) permits unrestricted cross-language calls such as <code>Polyglot.eval('python', ...)</code> from JavaScript. <code>NONE</code> blocks cross-language calls for a tighter sandbox; scripts that stay within one language are unaffected. Requires a restart to take effect.");
	public static final Setting<Integer> ScriptingStatementLimit    = new IntegerSetting(generalGroup,         "Scripting",   "application.scripting.polyglot.statement.limit", 10_000_000, "Maximum number of statements a single scripting context may execute before GraalVM cancels it. Bounds runaway scripts (e.g. <code>while (true) {}</code>) without affecting normal workloads. <code>0</code> disables the limit. Requires a restart to take effect.");

	// clustering
	public static final Setting<Boolean> ClusterModeEnabled            = new BooleanSetting(generalGroup,         "Application", "application.cluster.enabled",                  false, "Enables cluster mode (experimental)");
	public static final Setting<String> ClusterName                    = new StringSetting(generalGroup,          "Application", "application.cluster.name",                    "structr", "The name of the Structr cluster");
	public static final Setting<Boolean> ClusterDebugLogEnabled        = new BooleanSetting(generalGroup,         "Application", "application.cluster.log.enabled",                  false, "Enables debug logging for cluster mode communication");

	// stats
	public static final Setting<Integer> HttpStatsAggregationInterval  = new IntegerSetting(generalGroup,            "Logging",     "application.stats.aggregation.interval", 60_000,"Minimum aggregation interval for HTTP request stats.");

	public static final Setting<String> BasePath                       = new StringSetting(generalGroup,             "Paths",       "base.path",                             ".", "Path of the Structr working directory. All files will be located relative to this directory.");
	public static final Setting<String> TmpPath                        = new StringSetting(generalGroup,             "Paths",       "tmp.path",                              System.getProperty("java.io.tmpdir"), "Path to the temporary directory. Uses <code>java.io.tmpdir</code> by default");
	public static final Setting<String> DatabasePath                   = new StringSetting(generalGroup,             "Paths",       "database.path",                         System.getProperty("user.dir").concat(File.separator + "db"), "Path to the Neo4j database folder");
	public static final Setting<String> FilesPath                      = new StringSetting(generalGroup,             "Paths",       "files.path",                            System.getProperty("user.dir").concat(File.separator + "files"), "Path to the Structr file storage folder");
	public static final Setting<String> ChangelogPath                  = new StringSetting(generalGroup,             "Paths",       "changelog.path",                        System.getProperty("user.dir").concat(File.separator + "changelog"), "Path to the Structr changelog storage folder");
	public static final Setting<String> DataExchangePath               = new StringSetting(generalGroup,             "Paths",       "data.exchange.path",                    "exchange" + File.separator, "IMPORTANT: Path is relative to base.path");
	public static final Setting<String> ScriptsPath                    = new StringSetting(generalGroup,             "Paths",       "scripts.path",                          "scripts", "Path to the Structr scripts folder. IMPORTANT: Path is relative to base.path");

	public static final Setting<Boolean> AllowSymbolicLinksInScriptPaths = new BooleanSetting(generalGroup,          "Scripts",     "scripts.path.allowsymboliclinks",       false, "Setting to true disables an additional check that disallows symbolic links in script paths.");
	public static final Setting<Boolean> AllowPathTraversalInScriptPaths = new BooleanSetting(generalGroup,          "Scripts",     "scripts.path.allowpathtraversal",       false, "Setting to true disables an additional check that disallows path traversals (.. in path).");

	public static final Setting<String> LogLevel                       = new ChoiceSetting(generalGroup,             "Logging",     "log.level",                             "INFO", Settings.getAvailableLogLevels(), "Configures the default log level. Takes effect immediately.");
	public static final Setting<Integer> QueryTimeLoggingThreshold     = new IntegerSetting(generalGroup,            "Logging",     "log.querytime.threshold",               3000, "Milliseconds after which a long-running query will be logged.");
	public static final Setting<Integer> CallbackLoggingThreshold      = new IntegerSetting(generalGroup,            "Logging",     "log.callback.threshold",                50000, "Number of callbacks after which a transaction will be logged.");
	public static final Setting<Boolean> RequestLogging                = new BooleanSetting(generalGroup,            "Logging",     "log.requests",                          false);
	public static final Setting<Boolean> LogFunctionsStackTrace        = new BooleanSetting(generalGroup,            "Logging",     "log.functions.stacktrace",              false, "If true, the full stacktrace is logged for exceptions in system functions.");
	public static final Setting<Integer> LogScriptProcessCommandLine   = new IntegerChoiceSetting(generalGroup,      "Logging",     "log.scriptprocess.commandline",         2, Settings.getScriptProcessLogCommandLineOptions(), "Configures the default logging behaviour for the command line generated for script processes. This applies to the exec()- and exec_binary() functions, as well as some processes handling media conversion or processing. For the exec() and exec_binary() function, this can be overridden for each call of the function.");
	public static final Setting<Boolean> LogDirectoryWatchServiceQuiet = new BooleanSetting(generalGroup,            "Logging",     "log.directorywatchservice.scanquietly", false, "Prevents logging of each scan process for every sync root processed by the storage sync service (formerly the directory watch service, the setting key is kept for compatibility)");

	public static final Setting<Boolean> SetupWizardCompleted          = new BooleanSetting(generalGroup,            "hidden",      "setup.wizard.completed",                false);
	public static final Setting<String> Configuration                  = new StringSetting(generalGroup,             "hidden",      "configuration.provider",                "org.structr.module.JarConfigurationProvider", "Fully-qualified class name of a Java class in the current class path that implements the <code>org.structr.schema.ConfigurationProvider</code> interface.");
	public static final StringMultiChoiceSetting Services              = new StringMultiChoiceSetting(generalGroup,  "Services",    "configured.services",                   "NodeService SchemaService AgentService CronService HttpService MigrationService StorageSyncService", "Services that are listed in this configuration key will be started when Structr starts.");
	public static final Setting<Integer> ServicesStartTimeout          = new IntegerSetting(generalGroup,            "Services",    "services.start.timeout",                30);
	public static final Setting<Integer> ServicesStartRetries          = new IntegerSetting(generalGroup,            "Services",    "services.start.retries",                10);

	public static final Setting<Integer> NodeServiceStartTimeout = new IntegerSetting(generalGroup,  "Services",    "nodeservice.start.timeout",     10);
	public static final Setting<Integer> NodeServiceStartRetries = new IntegerSetting(generalGroup,  "Services",    "nodeservice.start.retries",     10);

	// server settings
	public static final Setting<String> ApplicationHost       = new StringSetting(serverGroup,  "Interfaces", "application.host",              "0.0.0.0", "The listen address of the Structr server. You can set this to your domain name if that name resolves to the IP of the server the instance is running on.");
	public static final Setting<Integer> HttpPort             = new IntegerSetting(serverGroup, "Interfaces", "application.http.port",         8082, "HTTP port the Structr server will listen on");
	public static final Setting<Integer> HttpsPort            = new IntegerSetting(serverGroup, "Interfaces", "application.https.port",        8083, "HTTPS port the Structr server will listen on (if SSL is enabled)");
	public static final Setting<Integer> SshPort              = new IntegerSetting(serverGroup, "Interfaces", "application.ssh.port",          8022, "SSH port the Structr server will listen on (if SSHService is enabled)");
	public static final Setting<Integer> FtpPort              = new IntegerSetting(serverGroup, "Interfaces", "application.ftp.port",          8021, "FTP port the Structr server will listen on (if FtpService is enabled)");
	public static final Setting<String> FTPPassivePortRange   = new StringSetting(serverGroup, "Interfaces", "application.ftp.passivePortRange",          null, "FTP port range for pasv mode. Needed if Structr is run in a docker container, so the port mapping can be done correctly.");

	public static final Setting<Boolean> HttpsEnabled         = new BooleanSetting(serverGroup, "Interfaces", "application.https.enabled",     false, "Whether SSL is enabled");
	public static final Setting<String> KeystorePath          = new StringSetting(serverGroup,  "Interfaces", "application.keystore.path",     "domain.key.keystore", "The path to the JKS keystore containing the SSL certificate. Default value is 'domain.key.keystore' which fits with the default value for letsencrypt.domain.key.filename which is 'domain.key'.");
	public static final Setting<String> KeystorePassword      = new StringSetting(serverGroup,  "Interfaces", "application.keystore.password", "", "The password for the JKS keystore");
	public static final Setting<String> RestPath              = new StringSetting(serverGroup,  "hidden",     "application.rest.path",         "/structr/rest", "Defines the URL path of the Structr REST server. Should not be changed because it is hard-coded in many parts of the application.");
	public static final Setting<String> BaseUrlOverride       = new StringSetting(serverGroup,  "Interfaces", "application.baseurl.override",  "", "Overrides the baseUrl that can be used to prefix links to local web resources. By default, the value is assembled from the protocol, hostname and port of the server instance Structr is running on");
	public static final Setting<String> ApplicationRootPath   = new StringSetting(serverGroup, "Interfaces", "application.root.path", "", "Root path of the application, e.g. in case Structr is being run behind a reverse proxy with additional path prefix in URI. If set, the value must start with a '/' and have no trailing '/'. A valid value would be <code>/xyz</code> ");

	public static final Setting<Integer> MaintenanceHttpPort          = new IntegerSetting(serverGroup, "Maintenance", MAINTENANCE_PREFIX + "." + HttpPort.getKey(),         8182, "HTTP port the Structr server will listen on in maintenance mode");
	public static final Setting<Integer> MaintenanceHttpsPort         = new IntegerSetting(serverGroup, "Maintenance", MAINTENANCE_PREFIX + "." + HttpsPort.getKey(),        8183, "HTTPS port the Structr server will listen on (if SSL is enabled) in maintenance mode");
	public static final Setting<Integer> MaintenanceSshPort           = new IntegerSetting(serverGroup, "Maintenance", MAINTENANCE_PREFIX + "." + SshPort.getKey(),          8122, "SSH port the Structr server will listen on (if SSHService is enabled) in maintenance mode");
	public static final Setting<Integer> MaintenanceFtpPort           = new IntegerSetting(serverGroup, "Maintenance", MAINTENANCE_PREFIX + "." + FtpPort.getKey(),          8121, "FTP port the Structr server will listen on (if FtpService is enabled) in maintenance mode");
	public static final Setting<String> MaintenanceResourcePath       = new StringSetting(serverGroup, "Maintenance", MAINTENANCE_PREFIX + ".resource.path",                 "", "The local folder for static resources served in maintenance mode. If no path is provided the a default maintenance page with customizable text is shown in maintenance mode.");
	public static final Setting<String> MaintenanceMessage            = new StringSetting(serverGroup, "Maintenance", MAINTENANCE_PREFIX + ".message",                       "The server is undergoing maintenance. It will be available again shortly.", "Text for default maintenance page (HTML is allowed)");
	public static final Setting<Boolean> MaintenanceModeEnabled       = new BooleanSetting(serverGroup, "hidden", MAINTENANCE_PREFIX + ".enabled",                           false, "Enables maintenance mode where all ports can be changed to prevent users from accessing the application during maintenance.");

	// HTTP service settings
	public static final Setting<String> LifecycleListeners       = new StringSetting(serverGroup,  "hidden",        "httpservice.lifecycle.listeners",      "");
	public static final Setting<Boolean> GzipCompression         = new BooleanSetting(serverGroup, "HTTP Settings", "httpservice.gzip.enabled",             true,  "Use GZIP compression for HTTP transfers");
	public static final Setting<Integer> HttpConnectionRateLimit = new IntegerSetting(serverGroup, "HTTP Settings", "httpservice.connection.ratelimit",     1000, "Defines the rate limit of HTTP/2 frames per connection for the HTTP Service.");
	public static final Setting<Boolean> Async                   = new BooleanSetting(serverGroup, "HTTP Settings", "httpservice.async",                    true,  "Whether the HttpServices uses asynchronous request handling. Disable this option if you encounter problems with HTTP responses.");
	public static final Setting<Boolean> HttpBasicAuthEnabled    = new BooleanSetting(serverGroup, "HTTP Settings", "httpservice.httpbasicauth.enabled",    false, "Enables HTTP Basic Auth support for pages and files");
	public static final Setting<Boolean> SNIRequired             = new BooleanSetting(serverGroup, "HTTP Settings", "httpservice.sni.required",             false,  "Enables strict SNI check for the http service.");
	public static final Setting<Boolean> SNIHostCheck            = new BooleanSetting(serverGroup, "HTTP Settings", "httpservice.sni.hostcheck",            false,  "Enables SNI host check.");
	public static final Setting<Boolean> JsonIndentation         = new BooleanSetting(serverGroup, "HTTP Settings", "json.indentation",                     true,  "Whether JSON output should be indented (beautified) or compacted");
	public static final Setting<Boolean> HtmlIndentation         = new BooleanSetting(serverGroup, "HTTP Settings", "html.indentation",                     true,  "Whether the page source should be indented (beautified) or compacted. Note: Does not work for template/content nodes which contain raw HTML");
	public static final Setting<Boolean> WsIndentation           = new BooleanSetting(serverGroup, "HTTP Settings", "ws.indentation",                       false, "Prettyprints websocket responses if set to true.");
	public static final Setting<Integer> SessionTimeout          = new IntegerSetting(serverGroup, "HTTP Settings", "application.session.timeout",          1800,  "The session timeout for inactive HTTP sessions in seconds. Default is 1800. Values lower or equal than 0 indicate that sessions never time out.");
	public static final Setting<Integer> MaxSessionsPerUser      = new IntegerSetting(serverGroup, "HTTP Settings", "application.session.max.number",       -1,    "The maximum number of active sessions per user. Default is -1 (unlimited).");
	public static final Setting<Boolean> ClearSessionsOnStartup  = new BooleanSetting(serverGroup, "HTTP Settings", "application.session.clear.onstartup",  false, "Clear all sessions on startup if set to true.");
	public static final Setting<Boolean> ClearSessionsOnShutdown = new BooleanSetting(serverGroup, "HTTP Settings", "application.session.clear.onshutdown", false, "Clear all sessions on shutdown if set to true.");

	public static final Setting<Boolean> ForceHttps             = new BooleanSetting(serverGroup, "HTTPS Settings", "httpservice.force.https",             false, "Enables redirecting HTTP requests from the configured HTTP port to the configured HTTPS port (only works if HTTPS is active).");
	public static final Setting<Boolean> HttpOnly               = new BooleanSetting(serverGroup, "HTTPS Settings", "httpservice.cookies.httponly",        false, "Set HttpOnly to true for cookies. Please note that this will disable backend access!");
	public static final Setting<String> CookieSameSite          = new ChoiceSetting(serverGroup,  "HTTPS Settings", "httpservice.cookies.samesite",        "Lax", Settings.getStringsAsSet("Lax", "Strict", "None"), "Sets the SameSite attribute for the JSESSIONID cookie. For SameSite=None the Secure flag must also be set, otherwise the cookie will be rejected by the browser!");
	public static final Setting<Boolean> CookieSecure           = new BooleanSetting(serverGroup, "HTTPS Settings", "httpservice.cookies.secure",          false, "Sets the secure flag for the JSESSIONID cookie.");
	public static final Setting<Boolean> dumpJettyStartupConfig = new BooleanSetting(serverGroup, "HTTPS Settings", "httpservice.log.jetty.startupconfig", false);
	public static final Setting<String> excludedProtocols       = new StringSetting(serverGroup,  "HTTPS Settings", "httpservice.ssl.protocols.excluded",  "TLSv1,TLSv1.1");
	public static final Setting<String> includedProtocols       = new StringSetting(serverGroup,  "HTTPS Settings", "httpservice.ssl.protocols.included",  "TLSv1.2");
	public static final Setting<String> disabledCipherSuites    = new StringSetting(serverGroup,  "HTTPS Settings", "httpservice.ssl.ciphers.excluded",    "");

	public static final Setting<String> AccessControlAcceptedOrigins  = new StringSetting(serverGroup, "CORS Settings", "access.control.accepted.origins",  "", "Comma-separated list of accepted origins, sets the <code>Access-Control-Allow-Origin</code> header.");
	public static final Setting<Integer> AccessControlMaxAge          = new IntegerSetting(serverGroup, "CORS Settings", "access.control.max.age",           3600, "Sets the value of the <code>Access-Control-Max-Age</code> header. Unit is seconds.");
	public static final Setting<String> AccessControlAllowMethods     = new StringSetting(serverGroup, "CORS Settings", "access.control.allow.methods",     "", "Sets the value of the <code>Access-Control-Allow-Methods</code> header. Comma-delimited list of the allowed HTTP request methods.");
	public static final Setting<String> AccessControlAllowHeaders     = new StringSetting(serverGroup, "CORS Settings", "access.control.allow.headers",     "Authorization,refresh_token", "Sets the value of the <code>Access-Control-Allow-Headers</code> header.");
	public static final Setting<String> AccessControlAllowCredentials = new StringSetting(serverGroup, "CORS Settings", "access.control.allow.credentials", "", "Sets the value of the <code>Access-Control-Allow-Credentials</code> header.");
	public static final Setting<String> AccessControlExposeHeaders    = new StringSetting(serverGroup, "CORS Settings", "access.control.expose.headers",    "", "Sets the value of the <code>Access-Control-Expose-Headers</code> header.");

	// Rate limiting / DoSFilter settings
	//
	// The DoSFilter is a Jetty servlet filter that only intercepts INBOUND HTTP requests handled
	// by the servlet container. Outbound HTTP requests (e.g. from scripts via HttpHelper) are NOT
	// affected.
	//
	// The global settings below define the GLOBAL DEFAULT TIER that is used as documentation anchor
	// and reference. Actual protection is configured per servlet via the <servletname>.dosfilter.*
	// settings further down in the servlet sections. To disable protection globally regardless of
	// per-servlet configuration, set httpservice.dosfilter.ratelimiting to false.
	public static final Setting<Boolean> RateLimiting             = new BooleanSetting(dosFilterGroup, "DoS Filter Settings", "httpservice.dosfilter.ratelimiting",          false, "Main switch for rate limiting using Jetty's DoSFilter. When false, no DoSFilter is attached to any servlet regardless of per-servlet configuration.");
	public static final Setting<Integer> MaxRequestsPerSec        = new IntegerSetting(dosFilterGroup, "DoS Filter Settings", "httpservice.dosfilter.maxrequestspersec",     10, "The maximum number of requests from a connection per second. Requests in excess of this are first delayed, then throttled.");
	public static final Setting<Integer> DelayMs                  = new IntegerSetting(dosFilterGroup, "DoS Filter Settings", "httpservice.dosfilter.delayMs",               100, "The delay given to all requests over the rate limit, before they are considered at all. -1 means just reject request, 0 means no delay, otherwise it is the delay.");
	public static final Setting<Integer> MaxWaitMs                = new IntegerSetting(dosFilterGroup, "DoS Filter Settings", "httpservice.dosfilter.maxwaitms",             50, "How long to blocking wait for the throttle semaphore in milliseconds.");
	public static final Setting<Integer> ThrottledRequests        = new IntegerSetting(dosFilterGroup, "DoS Filter Settings", "httpservice.dosfilter.throttledrequests",     5, "The number of requests over the rate limit able to be considered at once.");
	public static final Setting<Integer> ThrottleMs               = new IntegerSetting(dosFilterGroup, "DoS Filter Settings", "httpservice.dosfilter.throttlems",            30000, "How long to async wait for semaphore in milliseconds.");
	public static final Setting<Integer> MaxRequestMs             = new IntegerSetting(dosFilterGroup, "DoS Filter Settings", "httpservice.dosfilter.maxrequestms",          30000, "How long to allow a request to run in milliseconds.");
	public static final Setting<Integer> MaxIdleTrackerMs         = new IntegerSetting(dosFilterGroup, "DoS Filter Settings", "httpservice.dosfilter.maxidletrackerms",      30000, "How long to keep track of request rates for a connection before deciding that the user has gone away and discarding it, in milliseconds.");
	public static final Setting<Boolean> InsertHeaders            = new BooleanSetting(dosFilterGroup, "DoS Filter Settings", "httpservice.dosfilter.insertheaders",         true, "If true, insert the DoSFilter headers into the response.");
	public static final Setting<Boolean> RemotePort               = new BooleanSetting(dosFilterGroup, "DoS Filter Settings", "httpservice.dosfilter.remoteport",            false, "If true then rate is tracked by IP+port (effectively connection). If false, rate is tracked by IP address only.");
	public static final Setting<String> IpWhitelist               = new StringSetting(dosFilterGroup,  "DoS Filter Settings", "httpservice.dosfilter.ipwhitelist",           "127.0.0.1", "A comma-separated list of IP addresses that will not be rate limited. Defaults to localhost.");
	public static final Setting<Boolean> ManagedAttr              = new BooleanSetting(dosFilterGroup, "DoS Filter Settings", "httpservice.dosfilter.managedattr",           true, "If set to true, this servlet is set as a ServletContext attribute with the filter name as the attribute name. This allows context external mechanisms (e.g. JMX via ContextHandler managed attribute) to manage the configuration of the filter.");
	public static final Setting<Integer> TooManyCode              = new IntegerSetting(dosFilterGroup, "DoS Filter Settings", "httpservice.dosfilter.toomanycode",           429, "The HTTP status code to send if there are too many requests. By default is 429 (too many requests), but 503 (service unavailable) is another option.");

	// database settings
	public static final Setting<String> DatabaseAvailableConnections = new StringSetting(databaseGroup,  "hidden",                  "database.available.connections",   null);
	public static final Setting<String> DatabaseDriverMode           = new ChoiceSetting(databaseGroup,  "hidden",                  "database.driver.mode",             "embedded", Settings.getStringsAsSet("embedded", "remote"));
	public static final Setting<String> DatabaseDriver               = new StringSetting(databaseGroup,  "hidden",                  "database.driver",                  DEFAULT_DATABASE_DRIVER);
	public static final Setting<String> ConnectionName               = new StringSetting(databaseGroup,  "hidden",                  "database.connection.name",         "default");
	public static final Setting<String> SampleConnectionUrl          = new StringSetting(databaseGroup,  "hidden",                  "database.connection.url.sample",   "bolt://localhost:7687");
	public static final Setting<String> ConnectionUrl                = new StringSetting(databaseGroup,  "hidden",                  "database.connection.url",          "bolt://localhost:7688");
	public static final Setting<String> TestingConnectionUrl         = new StringSetting(databaseGroup,  "hidden",                  "testing.connection.url",           "bolt://localhost:7689");
	public static final Setting<String> ConnectionUser               = new StringSetting(databaseGroup,  "hidden",                  "database.connection.username",     "neo4j");
	public static final Setting<String> ConnectionPassword           = new StringSetting(databaseGroup,  "hidden",                  "database.connection.password",     "neo4j");
	public static final Setting<String> ConnectionDatabaseName       = new StringSetting(databaseGroup,  "hidden",                  "database.connection.databasename", "neo4j");
	public static final Setting<String> TenantIdentifier             = new StringSetting(databaseGroup,  "hidden",                  "database.tenant.identifier",       "");
	public static final Setting<Integer> UuidCacheSize               = new IntegerSetting(databaseGroup, "hidden",                  "database.cache.uuid.size",         1000000, "Size of the database driver relationship cache");
	public static final Setting<Boolean> ForceResultStreaming        = new BooleanSetting(databaseGroup, "Result Streaming",        "database.result.lazy",             false, "Forces Structr to use lazy evaluation for relationship queries");
	public static final Setting<Boolean> CypherDebugLogging          = new BooleanSetting(databaseGroup, "Debugging",               "log.cypher.debug",                 false, "Turns on debug logging for the generated Cypher queries");
	public static final Setting<Boolean> CypherDebugLoggingPing      = new BooleanSetting(databaseGroup, "Debugging",               "log.cypher.debug.ping",            false, "Turns on debug logging for the generated Cypher queries of the websocket PING command. Can only be used in conjunction with log.cypher.debug");
	public static final Setting<Integer> ResultCountSoftLimit        = new IntegerSetting(databaseGroup, "Soft result count limit", "database.result.softlimit",        10_000, "Soft result count limit for a single query (can be overridden by setting the <code>_pageSize</code> request parameter or by adding the request parameter <code>_disableSoftLimit</code> to a non-null value)");
	public static final Setting<Integer> FetchSize                   = new IntegerSetting(databaseGroup, "Result fetch size",       "database.result.fetchsize",        100_000, "Number of database records to fetch per batch when fetching large results");
	public static final Setting<Integer> PrefetchingThreshold        = new IntegerSetting(databaseGroup, "Prefetching",             "database.prefetching.threshold",   100, "How many identical queries must run in a transaction to activate prefetching for that query.");
	public static final Setting<Integer> PrefetchingMaxDuration      = new IntegerSetting(databaseGroup, "Prefetching",             "database.prefetching.maxduration", 1000, "How long a prefetching query may take before prefetching will be deactivated for that query.");
	public static final Setting<Integer> PrefetchingMaxCount         = new IntegerSetting(databaseGroup, "Prefetching",             "database.prefetching.maxcount",    50_000, "How many results a prefetching query may return before prefetching will be deactivated for that query.");

	// Neo4j specific settings
	public static final Setting<String> Neo4jDefaultUsername         = new StringSetting(databaseGroup,  "hidden",                  "database.neo4j.default.username",   "neo4j");
	public static final Setting<String> Neo4jDefaultPassword         = new StringSetting(databaseGroup,  "hidden",                  "database.neo4j.default.password",   "neo4j");

	// application settings
	public static final Setting<Boolean> ChangelogEnabled            = new BooleanSetting(applicationGroup, "Changelog",    "application.changelog.enabled",                   false, "Turns on logging of changes to nodes and relationships");
	public static final Setting<Boolean> UserChangelogEnabled        = new BooleanSetting(applicationGroup, "Changelog",    "application.changelog.user_centric.enabled",      false, "Turns on user-centric logging of what a user changed/created/deleted");
	public static final Setting<Boolean> FilesystemEnabled           = new BooleanSetting(applicationGroup, "Filesystem",   "application.filesystem.enabled",                  false, "If enabled, Structr will create a separate home directory for each user. The home directory of authenticated users will override the default upload folder setting. See Filesystem for more information.");
	public static final Setting<Boolean> UniquePaths                 = new BooleanSetting(applicationGroup, "Filesystem",   "application.filesystem.unique.paths",             true,  "If enabled, Structr will not allow files/folders of the same name in the same folder and automatically rename the file.");
	public static final Setting<String> UniquePathsInsertionPosition = new ChoiceSetting(applicationGroup,  "Filesystem",    "application.filesystem.unique.insertionposition", "beforeextension", Settings.getStringsAsSet("start", "beforeextension", "end"), "Defines the insertion position of the uniqueness criterion (currently a timestamp).<dl><dt>start</dt><dd>prefixes the name with a timestamp</dd><dt>beforeextension</dt><dd>puts the timestamp before the last dot (or at the end if the name does not contain a dot)</dd><dt>end</dt><dd>appends the timestamp after the complete name</dd></dl>");
	public static final Setting<String> DefaultChecksums             = new StringSetting(applicationGroup,  "Filesystem",   "application.filesystem.checksums.default",        "",    "List of additional checksums to be calculated on file creation by default. (<code>File.checksum</code> is always popuplated with an xxHash)<dl><dt>crc32</dt><dd>Cyclic Redundancy Check - long value</dd><dt>md5</dt><dd>md5 algorithm - 32 character hex string</dd><dt>sha1</dt><dd>SHA-1 algorithm - 40 character hex string</dd><dt>sha512</dt><dd>SHA-512 algorithm - 128 character hex string</dd></dl>");
	public static final Setting<Boolean> IndexingEnabled             = new BooleanSetting(applicationGroup, "Filesystem",   "application.filesystem.indexing.enabled",         true,  "Whether indexing is enabled globally (can be controlled separately for each file)");
	public static final Setting<Integer> IndexingMaxFileSize         = new IntegerSetting(applicationGroup, "Filesystem",   "application.filesystem.indexing.maxsize",         10,    "Maximum size (MB) of a file to be indexed");
	public static final Setting<Boolean> FollowSymlinks              = new BooleanSetting(applicationGroup, "Filesystem",   "application.filesystem.mount.followsymlinks",     true);
	public static final Setting<String> DefaultUploadFolder          = new StringSetting(applicationGroup,  "Filesystem",   "application.uploads.folder",                      "/._structr_uploads", "The default upload folder for files uploaded via the UploadServlet. This must be a valid folder path and can not be empty. Final slashes are automatically removed. Uploads to the root directory are not allowed.");

	public static final Setting<Boolean> FeedItemIndexRemoteDocument        = new BooleanSetting(applicationGroup, "Indexing",   "application.feeditem.indexing.remote",             true,  "Whether indexing for type FeedItem will index the target URL of the FeedItem or the description");
	public static final Setting<Boolean> FeedItemContentIndexingEnabled     = new BooleanSetting(applicationGroup, "Indexing",   "application.feeditemcontent.indexing.enabled",     true,  "Whether indexing is enabled for type FeedItemContent");
	public static final Setting<Integer> FeedItemContentIndexingLimit       = new IntegerSetting(applicationGroup, "Indexing",   "application.feeditemcontent.indexing.limit",       50000, "Maximum number of words to be indexed per FeedItemContent.");
	public static final Setting<Integer> FeedItemContentIndexingMinLength   = new IntegerSetting(applicationGroup, "Indexing",   "application.feeditemcontent.indexing.minlength",   3,     "Minimum length of words to be indexed for FeedItemContent");
	public static final Setting<Integer> FeedItemContentIndexingMaxLength   = new IntegerSetting(applicationGroup, "Indexing",   "application.feeditemcontent.indexing.maxlength",   30,    "Maximum length of words to be indexed for FeedItemContent");
	public static final Setting<Boolean> RemoteDocumentIndexingEnabled      = new BooleanSetting(applicationGroup, "Indexing",   "application.remotedocument.indexing.enabled",      true,  "Whether indexing is enabled for type RemoteDocument");
	public static final Setting<Integer> RemoteDocumentIndexingLimit        = new IntegerSetting(applicationGroup, "Indexing",   "application.remotedocument.indexing.limit",        50000, "Maximum number of words to be indexed per RemoteDocument.");
	public static final Setting<Integer> RemoteDocumentIndexingMinLength    = new IntegerSetting(applicationGroup, "Indexing",   "application.remotedocument.indexing.minlength",    3,     "Minimum length of words to be indexed for RemoteDocument");
	public static final Setting<Integer> RemoteDocumentIndexingMaxLength    = new IntegerSetting(applicationGroup, "Indexing",   "application.remotedocument.indexing.maxlength",    30,    "Maximum length of words to be indexed for RemoteDocument");

	public static final Setting<String> HttpProxyUrl              = new StringSetting(applicationGroup,  "Proxy",        "application.proxy.http.url",                  "");
	public static final Setting<Integer> HttpProxyPort            = new IntegerSetting(applicationGroup, "Proxy",        "application.proxy.http.port", null);
	public static final Setting<String> HttpProxyUser             = new StringSetting(applicationGroup,  "Proxy",        "application.proxy.http.username",             "");
	public static final Setting<String> HttpProxyPassword         = new StringSetting(applicationGroup,  "Proxy",        "application.proxy.http.password",             "");
	public static final ChoiceSetting   ProxyServletMode          = new ChoiceSetting(applicationGroup,  "Proxy",        "application.proxy.mode",                      "disabled", Set.of("disabled", "protected", "public"), "Sets the mode of the proxy servlet. Possible values are 'disabled' (off, servlet responds with 503 error code), 'protected' (only authenticated requests allowed) and 'public' (anonymous requests allowed). Default is disabled.");

	public static final Setting<Integer> HttpConnectionRequestTimeout = new IntegerSetting(applicationGroup, "Outbound Connections","application.httphelper.timeouts.connectionrequest",   60,            "Timeout for outbound connections in <b>seconds</b> to wait when requesting a connection from the connection manager. A timeout value of zero is interpreted as an infinite timeout.");
	public static final Setting<Integer> HttpConnectTimeout           = new IntegerSetting(applicationGroup, "Outbound Connections","application.httphelper.timeouts.connect",             60,            "Timeout for outbound connections in <b>seconds</b> to wait until a connection is established. A timeout value of zero is interpreted as an infinite timeout.");
	public static final Setting<Integer> HttpSocketTimeout            = new IntegerSetting(applicationGroup, "Outbound Connections","application.httphelper.timeouts.socket",              600,           "Socket timeout for outbound connections in <b>seconds</b> to wait for data or, put differently, a maximum inactivity period between two consecutive data packets. A timeout value of zero is interpreted as an infinite timeout.");
	public static final Setting<String>  HttpUserAgent                = new StringSetting(applicationGroup,  "Outbound Connections","application.httphelper.useragent",                    "curl/7.35.0", "User agent string for outbound connections");
	public static final Setting<String>  HttpDefaultCharset           = new StringSetting(applicationGroup,  "Outbound Connections","application.httphelper.charset",                      "ISO-8859-1",  "Default charset for outbound connections");
	public static final Setting<String>  OutgoingURLWhitelist         = new StringSetting(applicationGroup,  "Outbound Connections","application.httphelper.urlwhitelist",                 "*",           "A comma-separated list of URL patterns that can be used in HTTP request scripting functions (GET, PUT, POST etc.). If this value is anything other than *, whitelisting is applied to all outgoing requests.");
	public static final Setting<String>  HttpHostnameVerification     = new StringSetting(applicationGroup,  "Outbound Connections","application.httphelper.hostname.verification",        "STRICT",      "Controls hostname verification for outbound HTTPS calls that pass <code>validateCertificates=false</code>. <code>STRICT</code> (default) keeps the JVM default hostname verifier active even when certificate-chain validation has been disabled, so a certificate still has to match the host it is presented for. <code>LENIENT</code> disables hostname verification as well (legacy behaviour). Calls made with <code>validateCertificates=true</code> are unaffected.");

	public static final Setting<Boolean> SchemaAutoMigration      = new BooleanSetting(applicationGroup, "Schema",       "application.schema.automigration",            false,  "Enable automatic migration of schema information between versions (if possible -- may delete schema nodes)");
	public static final Setting<Boolean> AllowUnknownPropertyKeys = new BooleanSetting(applicationGroup, "Schema",       "application.schema.allowunknownkeys",         false,  "Enables get() and set() built-in functions to use property keys that are not defined in the schema.");

	public static final Setting<Boolean> logMissingLocalizations  = new BooleanSetting(applicationGroup, "Localization", "application.localization.logmissing",         false,  "Turns on logging for requested but non-existing localizations.");
	public static final Setting<Boolean> useFallbackLocale        = new BooleanSetting(applicationGroup, "Localization", "application.localization.usefallbacklocale",  false,  "Turns on usage of fallback locale if for the current locale no localization is found");
	public static final Setting<String> fallbackLocale            = new StringSetting(applicationGroup,  "Localization", "application.localization.fallbacklocale",     "en_US","The default locale used, if no localization is found and using a fallback is active.");

	public static final Setting<String> SchemaDeploymentFormat         = new ChoiceSetting(applicationGroup,  "Deployment",   "deployment.schema.format",                      "tree", Settings.getStringsAsSet("file", "tree"), "Configures how the schema is exported in a deployment export. <code>file</code> exports the schema as a single file. <code>tree</code> exports the schema as a tree where methods/function properties are written to single files in a tree structure.");
	public static final Setting<Integer> DeploymentNodeImportBatchSize = new IntegerSetting(applicationGroup, "Deployment",   "deployment.data.import.nodes.batchsize",         1000,   "Sets the batch size for data deployment when importing nodes.");
	public static final Setting<Integer> DeploymentRelImportBatchSize  = new IntegerSetting(applicationGroup, "Deployment",   "deployment.data.import.relationships.batchsize", 1000,   "Sets the batch size for data deployment when importing relationships.");
	public static final Setting<Integer> DeploymentNodeExportBatchSize = new IntegerSetting(applicationGroup, "Deployment",   "deployment.data.export.nodes.batchsize",         100,   "Sets the batch size for data deployment when exporting nodes.<br><br>The relationships for each node are collected and exported while the node itself is exported. It can make sense to reduce this number, if all/most nodes have very high amount of relationships.");

	public static final Setting<String> GlobalSecret              = new StringSetting(applicationGroup,  "Encryption",   "application.encryption.secret", null,   "Sets the global secret for encrypted string properties. Using this configuration setting is one of several possible ways to set the secret. Using the <code>set_encryption_key()</code> function is a way to set the encryption key without persisting it on disk.").setIsProtected();

	public static final Setting<Boolean> CallbacksOnLogout      = new BooleanSetting(applicationGroup, "Login/Logout behavior",   "callbacks.logout.onsave",       false, "Setting this to true enables the execution of the User.onSave method when a user logs out. Disabled by default because the global login handler onStructrLogout would be the right place for such functionality.");
	public static final Setting<Boolean> CallbacksOnLogin       = new BooleanSetting(applicationGroup, "Login/Logout behavior",   "callbacks.login.onsave",      false, "Setting this to true enables the execution of the User.onSave method for login actions. This will also trigger for failed login attempts and for two-factor authentication intermediate steps. Disabled by default because the global login handler onStructrLogin would be the right place for such functionality.");


	// mail settings
	public static final Setting<String> SmtpHost              = new StringSetting(smtpGroup,  "SMTP Settings", "smtp.host",         "localhost", "Address of the SMTP server used to send e-mails");
	public static final Setting<Integer> SmtpPort             = new IntegerSetting(smtpGroup, "SMTP Settings", "smtp.port",         25,          "SMTP server port to use when sending e-mails");
	public static final Setting<String> SmtpUser              = new StringSetting(smtpGroup,  "SMTP Settings", "smtp.user",         "");
	public static final Setting<String> SmtpPassword          = new StringSetting(smtpGroup,  "SMTP Settings", "smtp.password",     "");
	public static final Setting<Boolean> SmtpTlsEnabled       = new BooleanSetting(smtpGroup, "SMTP Settings", "smtp.tls.enabled",  true,        "Attempt STARTTLS encryption if the server supports it.");
	public static final Setting<Boolean> SmtpTlsRequired      = new BooleanSetting(smtpGroup, "SMTP Settings", "smtp.tls.required", true,        "Require STARTTLS; fail rather than send unencrypted.");
	public static final Setting<Boolean> SmtpTesting          = new BooleanSetting(smtpGroup, "hidden",        "smtp.testing.only", false);

	// advanced settings
	public static final Setting<Boolean> JsonRedundancyReduction      = new BooleanSetting(advancedGroup, "JSON",   "json.redundancyreduction",       true,  "If enabled, nested nodes (which were already rendered in the current output) are rendered with limited set of attribute (id, type, name).");
	public static final Setting<Boolean> JsonLenient                  = new BooleanSetting(advancedGroup, "JSON",   "json.lenient",                   false, "Whether to use lenient serialization, e.g. allow to serialize NaN, -Infinity, Infinity instead of just returning null. Note: as long as Javascript doesn’t support NaN etc., most of the UI will be broken");
	public static final Setting<Integer> JsonReduceNestedObjectsDepth = new IntegerSetting(advancedGroup, "JSON",   "json.reductiondepth",            0,     "For restricted views (ui, custom, all), only a limited amount of attributes (id, type, name) are rendered for nested objects after this depth. The default is 0, meaning that on the root depth (0), all attributes are rendered and reduction starts at depth 1.<br><br>Can be overridden on a per-request basis by using the request parameter <code>" + (Settings.RequestParameterLegacyMode.getValue() ? "" : "_")  + "outputReductionDepth</code>");

	public static final Setting<String> GeocodingProvider          = new StringSetting(advancedGroup,  "Geocoding",   "geocoding.provider",            "org.structr.common.geo.GoogleGeoCodingProvider", "Geocoding configuration");
	public static final Setting<String> GeocodingLanguage          = new StringSetting(advancedGroup,  "Geocoding",   "geocoding.language",            "de", "Geocoding configuration");
	public static final Setting<String> GeocodingApiKey            = new StringSetting(advancedGroup,  "Geocoding",   "geocoding.apikey",              "", "Geocoding configuration");
	public static final Setting<String> DefaultDateFormat          = new StringSetting(advancedGroup,  "Date Format", "dateproperty.defaultformat",    "yyyy-MM-dd'T'HH:mm:ssZ", "Default ISO8601 date format pattern. Used when serializing date properties and regular date objects and can be overridden for each schema property of type Date.").setLongDescription("""
			The Java SimpleDateFormat class is used for formatting Date objects. It provides the following pattern characters:

			| Letter | Date or Time Component |
			| --- | --- |
			| G | Era designator |
			| y | Year |
			| Y | Week year |
			| M | Month in year |
			| w | Week in year |
			| W | Week in month |
			| D | Day in year |
			| d | Day in month |
			| F | Day of week in month |
			| E | Day name in week |
			| u | Day number of week (1 = Monday, ..., 7 = Sunday) |
			| a | AM/PM marker |
			| H | Hour in day (0-23) |
			| k | Hour in day (1-24) |
			| K | Hour in AM/PM (0-11) |
			| h | Hour in AM/PM (1-12) |
			| m | Minute in hour |
			| s | Second in minute |
			| S | Millisecond |
			| z | General time zone |
			| Z | RFC 822 time zone |
			| X | ISO 8601 time zone |

			Each character can be repeated multiple times to control the output format.

			| Pattern | Description |
			| --- | --- |
			| d | prints one or two numbers (e.g. "1", "5" or "20") |
			| dd | prints two numbers (e.g. "01", "05" or "20") |
			| EEE | prints the shortened name of the weekday (e.g. "Mon") |
			| EEEE | prints the long name of the weekday (e.g. "Monday") |
			""");
	public static final Setting<String> ZonedDateTimeFormatOverride = new StringSetting(advancedGroup,  "ZonedDateTime Format Override", "zoneddatetimeproperty.format.override",    "", "Optional format pattern for ZonedDateTime properties and objects. Can be overridden for each schema property of type ZonedDateTime. If left empty, the default (and recommended) DateTimeFormatter.ISO_ZONED_DATE_TIME format will be used. Setting is being validated on write and only values that serialize/parse correctly will be accepted.").setLongDescription("""
			The Java DateTimeFormatter class is used for formatting ZonedDateTime objects. It provides the following pattern characters:
			(All letters 'A' to 'Z' and 'a' to 'z' are reserved as pattern letters)

			| Symbol | Meaning | Presentation | Examples |
			| --- | --- | --- | --- |
			| G | era | text | AD; Anno Domini; A |
			| u | year | year | 2004; 04 |
			| y | year-of-era | year | 2004; 04 |
			| D | day-of-year | number | 189 |
			| M/L | month-of-year | number/text | 7; 07; Jul; July; J |
			| d | day-of-month | number | 10 |
			| g | modified-julian-day | number | 2451334 |
			| Q/q | quarter-of-year | number/text | 3; 03; Q3; 3rd quarter |
			| Y | week-based-year | year | 1996; 96 |
			| w | week-of-week-based-year | number | 27 |
			| W | week-of-month | number | 4 |
			| E | day-of-week | text | Tue; Tuesday; T |
			| e/c | localized day-of-week | number/text | 2; 02; Tue; Tuesday; T |
			| F | aligned-week-of-month | number | 3 |
			| a | am-pm-of-day | text | PM |
			| B | period-of-day | text | in the morning |
			| h | clock-hour-of-am-pm (1-12) | number | 12 |
			| K | hour-of-am-pm (0-11) | number | 0 |
			| k | clock-hour-of-day (1-24) | number | 24 |
			| H | hour-of-day (0-23) | number | 0 |
			| m | minute-of-hour | number | 30 |
			| s | second-of-minute | number | 55 |
			| S | fraction-of-second | fraction | 978 |
			| A | milli-of-day | number | 1234 |
			| n | nano-of-second | number | 987654321 |
			| N | nano-of-day | number | 1234000000 |
			| V | time-zone ID | zone-id | America/Los_Angeles; Z; -08:30 |
			| v | generic time-zone name | zone-name | Pacific Time; PT |
			| z | time-zone name | zone-name | Pacific Standard Time; PST |
			| O | localized zone-offset | offset-O | GMT+8; GMT+08:00; UTC-08:00 |
			| X | zone-offset 'Z' for zero | offset-X | Z; -08; -0830; -08:30; -083015; -08:30:15 |
			| x | zone-offset | offset-x | +0000; -08; -0830; -08:30; -083015; -08:30:15 |
			| Z | zone-offset | offset-Z | +0000; -0800; -08:00 |
			| p | pad next | pad modifier | 1 |
			| '' | escape for text | delimiter | ' |
			| ' | single quote | literal |  |
			""");
	public static final Setting<Boolean> InheritanceDetection      = new BooleanSetting(advancedGroup, "hidden",      "importer.inheritancedetection", true);

	// servlets
	public static final StringMultiChoiceSetting Servlets     = new StringMultiChoiceSetting(servletsGroup, "General", "httpservice.servlets",
		"JsonRestServlet HtmlServlet CsvServlet UploadServlet ProxyServlet DeploymentServlet LoginServlet LogoutServlet TokenServlet HealthCheckServlet HistogramServlet OpenAPIServlet FlowServlet",
		Settings.getStringsAsSet("JsonRestServlet", "HtmlServlet", "CsvServlet", "UploadServlet", "ProxyServlet", "DeploymentServlet", "FlowServlet", "LoginServlet", "LogoutServlet", "TokenServlet", "EventSourceServlet", "HealthCheckServlet", "HistogramServlet", "OpenAPIServlet", "MetricsServlet"),
		"Servlets that are listed in this configuration key will be available in the HttpService. Changes to this setting require a restart of the HttpService in the 'Services' tab.");

	public static final Setting<Boolean> ConfigServletEnabled = new BooleanSetting(servletsGroup,  "ConfigServlet", "configservlet.enabled",             true, "Enables the config servlet (available under <code>http(s)://&lt;your-server&gt;/structr/config</code>)");
	public static final Setting<Boolean> ConfigServletSessionFixationProtection = new BooleanSetting(servletsGroup, "ConfigServlet", "configservlet.sessionfixation.protection", false, "Regenerates the HTTP session ID on successful login to the ConfigServlet to prevent session fixation attacks. Disabled by default because it can cause issues with certain reverse proxy or load balancer configurations.");

	public static final Setting<String> RestServletPath       = new StringSetting(servletsGroup,            "hidden", "jsonrestservlet.path",                         "/structr/rest/*", "URL pattern for REST server. Do not change unless you know what you are doing.");
	public static final Setting<String> RestServletClass      = new StringSetting(servletsGroup,            "hidden", "jsonrestservlet.class",                        "org.structr.rest.servlet.JsonRestServlet", "FQCN of servlet class to use in the REST server. Do not change unless you know what you are doing.");
	public static final Setting<String> RestAuthenticator     = new StringSetting(servletsGroup,            "hidden", "jsonrestservlet.authenticator",                "org.structr.web.auth.UiAuthenticator", "FQCN of authenticator class to use in the REST server. Do not change unless you know what you are doing.");
	public static final Setting<String> RestDefaultView       = new StringSetting(servletsGroup,            "hidden", "jsonrestservlet.defaultview",                  "public", "Default view to use when no view is given in the URL");
	public static final Setting<Integer> RestOutputDepth      = new IntegerSetting(servletsGroup,           "JsonRestServlet", "jsonrestservlet.outputdepth",                  3, "Maximum nesting depth of JSON output");
	public static final Setting<String> RestResourceProvider  = new StringSetting(servletsGroup,            "hidden", "jsonrestservlet.resourceprovider",             "org.structr.web.common.UiResourceProvider", "FQCN of resource provider class to use in the REST server. Do not change unless you know what you are doing.");
	public static final Setting<Boolean> RestUserAutologin    = new BooleanSetting(servletsGroup,           "JsonRestServlet", "jsonrestservlet.user.autologin",               false, "Only works in conjunction with the jsonrestservlet.user.autocreate key. Will log in user after self registration.");
	public static final Setting<Boolean> RestUserAutocreate   = new BooleanSetting(servletsGroup,           "JsonRestServlet", "jsonrestservlet.user.autocreate",              false, "Enable this to support user self registration");
	public static final Setting<String> InputValidationMode   = new ChoiceSetting(servletsGroup,            "JsonRestServlet", "jsonrestservlet.unknowninput.validation.mode", "accept_warn", getStringsAsSet("accept", "accept_warn", "ignore", "ignore_warn", "reject", "reject_warn"), "Controls how Structr reacts to unknown keys in JSON input. <code>accept</code> allows the unknown key to be written. <code>ignore</code> removes the key. <code>reject</code> rejects the complete request. The <code>warn</code> options behave identical but also log a warning.");

	public static final Setting<String> FlowServletPath       = new StringSetting(servletsGroup,  "hidden", "flowservlet.path",             "/structr/flow/*", "The URI under which requests are accepted by the servlet. Needs to include a wildcard at the end.");
	public static final Setting<String> FlowServletClass      = new StringSetting(servletsGroup,  "hidden", "flowservlet.class",            "org.structr.flow.servlet.FlowServlet");
	public static final Setting<String> FlowAuthenticator     = new StringSetting(servletsGroup,  "hidden", "flowservlet.authenticator",    "org.structr.web.auth.UiAuthenticator");
	public static final Setting<String> FlowDefaultView       = new StringSetting(servletsGroup,  "FlowServlet", "flowservlet.defaultview",      "public", "Default view to use when no view is given in the URL.");
	public static final Setting<Integer> FlowOutputDepth      = new IntegerSetting(servletsGroup, "FlowServlet", "flowservlet.outputdepth",      3, "Maximum nesting depth of JSON output.");
	public static final Setting<String> FlowResourceProvider  = new StringSetting(servletsGroup,  "hidden", "flowservlet.resourceprovider", "org.structr.web.common.UiResourceProvider");

	public static final Setting<String> HtmlServletPath           = new StringSetting(servletsGroup,  "hidden", "htmlservlet.path",                  "/structr/html/*", "URL pattern for HTTP server. Do not change unless you know what you are doing.");
	public static final Setting<String> HtmlServletClass          = new StringSetting(servletsGroup,  "hidden", "htmlservlet.class",                 "org.structr.web.servlet.HtmlServlet", "FQCN of servlet class to use for HTTP requests. Do not change unless you know what you are doing.");
	public static final Setting<String> HtmlAuthenticator         = new StringSetting(servletsGroup,  "hidden", "htmlservlet.authenticator",         "org.structr.web.auth.UiAuthenticator", "FQCN of authenticator class to use for HTTP requests. Do not change unless you know what you are doing.");
	public static final Setting<String> HtmlDefaultView           = new StringSetting(servletsGroup,  "HtmlServlet", "htmlservlet.defaultview",           "public", "Not used for HtmlServlet");
	public static final Setting<Integer> HtmlOutputDepth          = new IntegerSetting(servletsGroup, "HtmlServlet", "htmlservlet.outputdepth",           3, "Not used for HtmlServlet");
	public static final Setting<String> HtmlResourceProvider      = new StringSetting(servletsGroup,  "hidden", "htmlservlet.resourceprovider",      "org.structr.web.common.UiResourceProvider", "FQCN of resource provider class to use in the HTTP server. Do not change unless you know what you are doing.");
	public static final Setting<String> HtmlResolveProperties     = new StringSetting(servletsGroup,  "HtmlServlet", "htmlservlet.resolveproperties",     "NodeInterface.name", "Space-separated list of properties that are tried to find the 'current' object (restart of HttpService required).");
	public static final Setting<String> HtmlCustomResponseHeaders = new TextSetting(servletsGroup,    "HtmlServlet", "htmlservlet.customresponseheaders", "Strict-Transport-Security:max-age=60,X-Content-Type-Options:nosniff,X-Frame-Options:SAMEORIGIN,X-XSS-Protection:1;mode=block", "List of custom response headers that will be added to every HTTP response");

	public static final Setting<String> PdfServletPath           = new StringSetting(servletsGroup,  "hidden", "pdfservlet.path",                  "/structr/pdf/*", "The URI under which requests are accepted by the servlet. Needs to include a wildcard at the end.");
	public static final Setting<String> PdfServletClass          = new StringSetting(servletsGroup,  "hidden", "pdfservlet.class",                 "org.structr.pdf.servlet.PdfServlet");
	public static final Setting<String> PdfAuthenticator         = new StringSetting(servletsGroup,  "hidden", "pdfservlet.authenticator",         "org.structr.web.auth.UiAuthenticator");
	public static final Setting<String> PdfDefaultView           = new StringSetting(servletsGroup,  "PdfServlet", "pdfservlet.defaultview",           "public", "Default view to use when no view is given in the URL.");
	public static final Setting<Integer> PdfOutputDepth          = new IntegerSetting(servletsGroup, "PdfServlet", "pdfservlet.outputdepth",           3, "Maximum nesting depth of JSON output.");
	public static final Setting<String> PdfResourceProvider      = new StringSetting(servletsGroup,  "hidden", "pdfservlet.resourceprovider",      "org.structr.web.common.UiResourceProvider");
	public static final Setting<String> PdfResolveProperties     = new StringSetting(servletsGroup,  "PdfServlet", "pdfservlet.resolveproperties",     "NodeInterface.name", "Space-separated list of properties that are tried to find the 'current' object (restart of HttpService required).");
	public static final Setting<String> PdfCustomResponseHeaders = new TextSetting(servletsGroup,    "PdfServlet", "pdfservlet.customresponseheaders", "Strict-Transport-Security:max-age=60,X-Content-Type-Options:nosniff,X-Frame-Options:SAMEORIGIN,X-XSS-Protection:1;mode=block", "List of custom response headers that will be added to every HTTP response");

	public static final Setting<String> WebsocketServletPath       = new StringSetting(servletsGroup,  "hidden", "websocketservlet.path",              "/structr/ws/*", "URL pattern for WebSockets. Do not change unless you know what you are doing.");
	// NOTE: there is no websocketservlet.class - the WebSocketServlet was removed in the Jetty 12 migration;
	// WebSockets are now handled by Jetty's WebSocketUpgradeHandler (see HttpService), not a servlet class.
	public static final Setting<String> WebsocketAuthenticator     = new StringSetting(servletsGroup,  "hidden", "websocketservlet.authenticator",     "org.structr.web.auth.UiAuthenticator", "FQCN of authenticator class to use for WebSockets. Do not change unless you know what you are doing.");
	public static final Setting<String> WebsocketDefaultView       = new StringSetting(servletsGroup,  "hidden", "websocketservlet.defaultview",       "public", "Unused");
	public static final Setting<Integer> WebsocketOutputDepth      = new IntegerSetting(servletsGroup, "WebSocketServlet", "websocketservlet.outputdepth",       3, "Maximum nesting depth of JSON output");
	public static final Setting<String> WebsocketResourceProvider  = new StringSetting(servletsGroup,  "hidden", "websocketservlet.resourceprovider",  "org.structr.web.common.UiResourceProvider", "FQCN of resource provider class to use with WebSockets. Do not change unless you know what you are doing.");
	public static final Setting<Boolean> WebsocketUserAutologin    = new BooleanSetting(servletsGroup, "hidden", "websocketservlet.user.autologin",    false, "Unused");
	public static final Setting<Boolean> WebsocketUserAutocreate   = new BooleanSetting(servletsGroup, "hidden", "websocketservlet.user.autocreate",   false, "Unused");

	public static final Setting<String> CsvServletPath       = new StringSetting(servletsGroup,  "hidden", "csvservlet.path",              "/structr/csv/*", "URL pattern for CSV output. Do not change unless you know what you are doing.");
	public static final Setting<String> CsvServletClass      = new StringSetting(servletsGroup,  "hidden", "csvservlet.class",             "org.structr.rest.servlet.CsvServlet", "Servlet class to use for CSV output. Do not change unless you know what you are doing.");
	public static final Setting<String> CsvAuthenticator     = new StringSetting(servletsGroup,  "hidden", "csvservlet.authenticator",     "org.structr.web.auth.UiAuthenticator", "FQCN of Authenticator class to use for CSV output. Do not change unless you know what you are doing.");
	public static final Setting<String> CsvDefaultView       = new StringSetting(servletsGroup,  "CsvServlet", "csvservlet.defaultview",       "public", "Default view to use when no view is given in the URL");
	public static final Setting<Integer> CsvOutputDepth      = new IntegerSetting(servletsGroup, "CsvServlet", "csvservlet.outputdepth",       3, "Maximum nesting depth of JSON output");
	public static final Setting<String> CsvResourceProvider  = new StringSetting(servletsGroup,  "hidden", "csvservlet.resourceprovider",  "org.structr.web.common.UiResourceProvider", "FQCN of resource provider class to use in the REST server. Do not change unless you know what you are doing.");
	public static final Setting<Boolean> CsvUserAutologin    = new BooleanSetting(servletsGroup, "hidden", "csvservlet.user.autologin",    false, "Unused");
	public static final Setting<Boolean> CsvUserAutocreate   = new BooleanSetting(servletsGroup, "hidden", "csvservlet.user.autocreate",   false, "Unused");
	public static final Setting<Boolean> CsvFrontendAccess   = new BooleanSetting(servletsGroup, "hidden", "csvservlet.frontendaccess",    false, "Unused");

	public static final Setting<String> UploadServletPath       = new StringSetting(servletsGroup,  "hidden", "uploadservlet.path",                  "/structr/upload", "URL pattern for file upload. Do not change unless you know what you are doing.");
	public static final Setting<String> UploadServletClass      = new StringSetting(servletsGroup,  "hidden", "uploadservlet.class",                 "org.structr.web.servlet.UploadServlet", "FQCN of servlet class to use for file upload. Do not change unless you know what you are doing.");
	public static final Setting<String> UploadAuthenticator     = new StringSetting(servletsGroup,  "hidden", "uploadservlet.authenticator",         "org.structr.web.auth.UiAuthenticator", "FQCN of authenticator class to use for file upload. Do not change unless you know what you are doing.");
	public static final Setting<String> UploadDefaultView       = new StringSetting(servletsGroup,  "UploadServlet", "uploadservlet.defaultview",           "public", "Default view to use when no view is given in the URL");
	public static final Setting<Integer> UploadOutputDepth      = new IntegerSetting(servletsGroup, "UploadServlet", "uploadservlet.outputdepth",           3, "Maximum nesting depth of JSON output");
	public static final Setting<String> UploadResourceProvider  = new StringSetting(servletsGroup,  "hidden", "uploadservlet.resourceprovider",      "org.structr.web.common.UiResourceProvider", "FQCN of resource provider class to use for file upload. Do not change unless you know what you are doing.	");
	public static final Setting<Boolean> UploadUserAutologin    = new BooleanSetting(servletsGroup, "hidden", "uploadservlet.user.autologin",        false, "Unused");
	public static final Setting<Boolean> UploadUserAutocreate   = new BooleanSetting(servletsGroup, "hidden", "uploadservlet.user.autocreate",       false, "Unused");
	public static final Setting<Boolean> UploadAllowAnonymous   = new BooleanSetting(servletsGroup, "UploadServlet", "uploadservlet.allowanonymousuploads", false, "Allows anonymous users to upload files.");
	public static final Setting<Integer> UploadMaxFileSize      = new IntegerSetting(servletsGroup, "UploadServlet", "uploadservlet.maxfilesize",           1000, "Maximum allowed file size for single file uploads. Unit is Megabytes");
	public static final Setting<Integer> UploadMaxRequestSize   = new IntegerSetting(servletsGroup, "UploadServlet", "uploadservlet.maxrequestsize",        1200, "Maximum allowed request size for single file uploads. Unit is Megabytes");

	public static final Setting<String> LoginServletPath       = new StringSetting(servletsGroup,  "hidden", "loginservlet.path",                  "/structr/login", "The URI under which requests are accepted by the servlet. Needs to include a wildcard at the end.");
	public static final Setting<String> LoginServletClass      = new StringSetting(servletsGroup,  "hidden", "loginservlet.class",                 "org.structr.web.servlet.LoginServlet");
	public static final Setting<String> LoginAuthenticator     = new StringSetting(servletsGroup,  "hidden", "loginservlet.authenticator",         "org.structr.web.auth.UiAuthenticator");
	public static final Setting<String> LoginResourceProvider  = new StringSetting(servletsGroup,  "hidden", "loginservlet.resourceprovider",      "org.structr.web.common.UiResourceProvider");
	public static final Setting<String> LoginDefaultView       = new StringSetting(servletsGroup,  "LoginServlet", "loginservlet.defaultview",           "public", "Default view to use when no view is given in the URL.");
	public static final Setting<Integer> LoginOutputDepth      = new IntegerSetting(servletsGroup, "LoginServlet", "loginservlet.outputdepth",	   3, "Maximum nesting depth of JSON output.");

	public static final Setting<String> LogoutServletPath       = new StringSetting(servletsGroup,  "hidden", "logoutservlet.path",                  "/structr/logout", "The URI under which requests are accepted by the servlet. Needs to include a wildcard at the end.");
	public static final Setting<String> LogoutServletClass      = new StringSetting(servletsGroup,  "hidden", "logoutservlet.class",                 "org.structr.web.servlet.LogoutServlet");
	public static final Setting<String> LogoutAuthenticator     = new StringSetting(servletsGroup,  "hidden", "logoutservlet.authenticator",         "org.structr.web.auth.UiAuthenticator");
	public static final Setting<String> LogoutResourceProvider  = new StringSetting(servletsGroup,  "hidden", "logoutservlet.resourceprovider",      "org.structr.web.common.UiResourceProvider");
	public static final Setting<String> LogoutDefaultView       = new StringSetting(servletsGroup,  "LogoutServlet", "logoutservlet.defaultview",           "public", "Default view to use when no view is given in the URL.");
	public static final Setting<Integer> LogoutOutputDepth      = new IntegerSetting(servletsGroup, "LogoutServlet", "logoutservlet.outputdepth",	   3, "Maximum nesting depth of JSON output.");

	public static final Setting<String> TokenServletPath       = new StringSetting(servletsGroup,  "hidden", "tokenservlet.path",                  "/structr/token", "The URI under which requests are accepted by the servlet. Needs to include a wildcard at the end.");
	public static final Setting<String> TokenServletClass      = new StringSetting(servletsGroup,  "hidden", "tokenservlet.class",                 "org.structr.web.servlet.TokenServlet");
	public static final Setting<String> TokenAuthenticator     = new StringSetting(servletsGroup,  "hidden", "tokenservlet.authenticator",         "org.structr.web.auth.UiAuthenticator");
	public static final Setting<String> TokenResourceProvider  = new StringSetting(servletsGroup,  "hidden", "tokenservlet.resourceprovider",      "org.structr.web.common.UiResourceProvider");
	public static final Setting<String> TokenDefaultView       = new StringSetting(servletsGroup,  "TokenServlet", "tokenservlet.defaultview",           "public", "Default view to use when no view is given in the URL.");
	public static final Setting<Integer> TokenOutputDepth      = new IntegerSetting(servletsGroup, "TokenServlet", "tokenservlet.outputdepth",	   3, "Maximum nesting depth of JSON output.");

	public static final Setting<String> DeploymentServletPath                = new StringSetting(servletsGroup,  "hidden", "deploymentservlet.path",                      "/structr/deploy");
	public static final Setting<String> DeploymentServletClass               = new StringSetting(servletsGroup,  "hidden", "deploymentservlet.class",                     "org.structr.web.servlet.DeploymentServlet");
	public static final Setting<String> DeploymentAuthenticator              = new StringSetting(servletsGroup,  "hidden", "deploymentservlet.authenticator",             "org.structr.web.auth.UiAuthenticator");
	public static final Setting<String> DeploymentDefaultView                = new StringSetting(servletsGroup,  "hidden", "deploymentservlet.defaultview",               "public");
	public static final Setting<Integer> DeploymentOutputDepth               = new IntegerSetting(servletsGroup, "hidden", "deploymentservlet.outputdepth",               3);
	public static final Setting<String> DeploymentResourceProvider           = new StringSetting(servletsGroup,  "hidden", "deploymentservlet.resourceprovider",          "org.structr.web.common.UiResourceProvider");
	public static final Setting<Boolean> DeploymentUserAutologin             = new BooleanSetting(servletsGroup, "hidden", "deploymentservlet.user.autologin",            false);
	public static final Setting<Boolean> DeploymentUserAutocreate            = new BooleanSetting(servletsGroup, "hidden", "deploymentservlet.user.autocreate",           false);
	public static final Setting<String> DeploymentFileGroupName              = new StringSetting(servletsGroup, "DeploymentServlet", "deploymentservlet.filegroup.name", "", "For unix based file systems only. Adds the group ownership to the created deployment files.");

	public static final Setting<String> ProxyServletPath       = new StringSetting(servletsGroup,  "hidden", "proxyservlet.path",                  "/structr/proxy");
	public static final Setting<String> ProxyServletClass      = new StringSetting(servletsGroup,  "hidden", "proxyservlet.class",                 "org.structr.web.servlet.ProxyServlet");
	public static final Setting<String> ProxyAuthenticator     = new StringSetting(servletsGroup,  "hidden", "proxyservlet.authenticator",         "org.structr.web.auth.UiAuthenticator");
	public static final Setting<String> ProxyDefaultView       = new StringSetting(servletsGroup,  "hidden", "proxyservlet.defaultview",           "public");
	public static final Setting<Integer> ProxyOutputDepth      = new IntegerSetting(servletsGroup, "hidden", "proxyservlet.outputdepth",           3);
	public static final Setting<String> ProxyResourceProvider  = new StringSetting(servletsGroup,  "hidden", "proxyservlet.resourceprovider",      "org.structr.web.common.UiResourceProvider");

	public static final Setting<String> EventSourceServletPath       = new StringSetting(servletsGroup,  "hidden", "eventsourceservlet.path",                  "/structr/EventSource");
	public static final Setting<String> EventSourceServletClass      = new StringSetting(servletsGroup,  "hidden", "eventsourceservlet.class",                 "org.structr.web.servlet.EventSourceServlet");
	public static final Setting<String> EventSourceAuthenticator     = new StringSetting(servletsGroup,  "hidden", "eventsourceservlet.authenticator",         "org.structr.web.auth.UiAuthenticator");
	public static final Setting<String> EventSourceResourceProvider  = new StringSetting(servletsGroup,  "hidden", "eventsourceservlet.resourceprovider",      "org.structr.web.common.UiResourceProvider");
	public static final Setting<String> EventSourceDefaultView       = new StringSetting(servletsGroup,  "hidden", "eventsourceservlet.defaultview",           "public");
	public static final Setting<Integer> EventSourceOutputDepth      = new IntegerSetting(servletsGroup, "hidden", "eventsourceservlet.outputdepth",	   1);

	public static final Setting<String> HealthCheckServletPath       = new StringSetting(servletsGroup,  "HealthCheckServlet", "healthcheckservlet.path",      "/structr/health");
	public static final Setting<String> HealthCheckServletClass      = new StringSetting(servletsGroup,  "hidden", "healthcheckservlet.class",                 "org.structr.web.servlet.HealthCheckServlet");
	public static final Setting<String> HealthCheckAuthenticator     = new StringSetting(servletsGroup,  "hidden", "healthcheckservlet.authenticator",         "org.structr.web.auth.UiAuthenticator");
	public static final Setting<String> HealthCheckResourceProvider  = new StringSetting(servletsGroup,  "hidden", "healthcheckservlet.resourceprovider",      "org.structr.web.common.UiResourceProvider");
	public static final Setting<String> HealthCheckDefaultView       = new StringSetting(servletsGroup,  "hidden", "healthcheckservlet.defaultview",           "public");
	public static final Setting<Integer> HealthCheckOutputDepth      = new IntegerSetting(servletsGroup, "hidden", "healthcheckservlet.outputdepth",           1);
	public static final Setting<String> HealthCheckWhitelist         = new StringSetting(servletsGroup,  "HealthCheckServlet", "healthcheckservlet.whitelist", "127.0.0.1, localhost, ::1", "IP addresses in this list are allowed to access the health check endpoint at /structr/health.");

	public static final Setting<String> HistogramServletPath       = new StringSetting(servletsGroup,  "hidden", "histogramservlet.path",                "/structr/histogram");
	public static final Setting<String> HistogramServletClass      = new StringSetting(servletsGroup,  "hidden", "histogramservlet.class",               "org.structr.web.servlet.HistogramServlet");
	public static final Setting<String> HistogramAuthenticator     = new StringSetting(servletsGroup,  "hidden", "histogramservlet.authenticator",       "org.structr.web.auth.UiAuthenticator");
	public static final Setting<String> HistogramResourceProvider  = new StringSetting(servletsGroup,  "hidden", "histogramservlet.resourceprovider",    "org.structr.web.common.UiResourceProvider");
	public static final Setting<String> HistogramDefaultView       = new StringSetting(servletsGroup,  "hidden", "histogramservlet.defaultview",         "public");
	public static final Setting<Integer> HistogramOutputDepth      = new IntegerSetting(servletsGroup, "hidden", "histogramservlet.outputdepth",         1);
	public static final Setting<String> HistogramWhitelist         = new StringSetting(servletsGroup,  "HistogramServlet", "histogramservlet.whitelist", "127.0.0.1, localhost, ::1", "IP addresses in this list are allowed to access the query histogram endpoint at /structr/histogram.");

	public static final Setting<String> OpenAPIServletPath       = new StringSetting(servletsGroup,  "OpenAPIServlet", "openapiservlet.path",                          "/structr/openapi/*");
	public static final Setting<String> OpenAPIServletClass      = new StringSetting(servletsGroup,  "hidden", "openapiservlet.class",                         "org.structr.rest.servlet.OpenAPIServlet");
	public static final Setting<String> OpenAPIAuthenticator     = new StringSetting(servletsGroup,  "hidden", "openapiservlet.authenticator",                 "org.structr.web.auth.UiAuthenticator");
	public static final Setting<String> OpenAPIResourceProvider  = new StringSetting(servletsGroup,  "hidden", "openapiservlet.resourceprovider",              "org.structr.web.common.UiResourceProvider");
	public static final Setting<String> OpenAPIDefaultView       = new StringSetting(servletsGroup,  "hidden", "openapiservlet.defaultview",                   "public");
	public static final Setting<Integer> OpenAPIOutputDepth      = new IntegerSetting(servletsGroup, "hidden", "openapiservlet.outputdepth",                   1);
	public static final Setting<String> OpenAPIServerTitle       = new StringSetting(servletsGroup,  "OpenAPIServlet", "openapiservlet.server.title",      "Structr REST Server", "The main title of the OpenAPI server definition.");
	public static final Setting<String> OpenAPIServerVersion     = new StringSetting(servletsGroup,  "OpenAPIServlet", "openapiservlet.server.version", "1.0.1", "The version number of the OpenAPI definition");

	// Prometheus MetricsServlet
	public static final Setting<String> MetricsServletPath              = new StringSetting(servletsGroup,  "MetricsServlet", "metricsservlet.path",      "/structr/metrics");
	public static final Setting<String> MetricsServletClass             = new StringSetting(servletsGroup,  "hidden", "metricsservlet.class",             "org.structr.rest.servlet.MetricsServlet");
	public static final Setting<String> MetricsServletAuthenticator     = new StringSetting(servletsGroup,  "hidden", "metricsservlet.authenticator",     "org.structr.web.auth.UiAuthenticator");
	public static final Setting<String> MetricsServletResourceProvider  = new StringSetting(servletsGroup,  "hidden", "metricsservlet.resourceprovider",  "org.structr.web.common.UiResourceProvider");
	public static final Setting<String> MetricsServletDefaultView       = new StringSetting(servletsGroup,  "hidden", "metricsservlet.defaultview",       "public");
	public static final Setting<String> MetricsServletWhitelist         = new StringSetting(servletsGroup,  "MetricsServlet", "metricsservlet.whitelist", "127.0.0.1, localhost, ::1", "Comma-separated list of IP addresses that are allowed to access the health check endpoint at /structr/metrics.");


	// -----------------------------------------------------------------------------
	// Per-servlet DoSFilter settings
	//
	// Each active servlet has its own set of DoSFilter parameters. The filter is
	// only attached to a given servlet when both the main switch
	// httpservice.dosfilter.ratelimiting AND the servlet's own <name>.dosfilter.enabled
	// are true. Defaults match the global DoS Filter Settings block.
	//
	// Enabled by default on all request-handling servlets. Disabled by default on
	// EventSourceServlet because SSE is a long-lived streaming connection that would
	// be killed by DoSFilter's maxRequestMs.
	// -----------------------------------------------------------------------------

	// JsonRestServlet DoS settings
	public static final Setting<Boolean> JsonRestDosEnabled           = new BooleanSetting(servletsGroup, "JsonRestServlet", "jsonrestservlet.dosfilter.enabled",           true,       "Enable DoSFilter for the JSON REST servlet.");
	public static final Setting<Integer> JsonRestDosMaxRequestsPerSec = new IntegerSetting(servletsGroup, "JsonRestServlet", "jsonrestservlet.dosfilter.maxrequestspersec", 10,         "Max inbound requests per second per client for the JSON REST servlet. Excess requests are delayed, then throttled.");
	public static final Setting<Integer> JsonRestDosDelayMs           = new IntegerSetting(servletsGroup, "JsonRestServlet", "jsonrestservlet.dosfilter.delayMs",           100,        "Delay in milliseconds for over-limit requests on the JSON REST servlet. -1 = reject, 0 = no delay.");
	public static final Setting<Integer> JsonRestDosMaxWaitMs         = new IntegerSetting(servletsGroup, "JsonRestServlet", "jsonrestservlet.dosfilter.maxwaitms",         50,         "Blocking wait time in ms for the throttle semaphore on the JSON REST servlet.");
	public static final Setting<Integer> JsonRestDosThrottledRequests = new IntegerSetting(servletsGroup, "JsonRestServlet", "jsonrestservlet.dosfilter.throttledrequests", 5,          "Requests over the rate limit considered at once on the JSON REST servlet.");
	public static final Setting<Integer> JsonRestDosThrottleMs        = new IntegerSetting(servletsGroup, "JsonRestServlet", "jsonrestservlet.dosfilter.throttlems",        30000,      "Async wait time in ms for the throttle semaphore on the JSON REST servlet.");
	public static final Setting<Integer> JsonRestDosMaxRequestMs      = new IntegerSetting(servletsGroup, "JsonRestServlet", "jsonrestservlet.dosfilter.maxrequestms",      300000,     "Maximum duration in ms an inbound JSON REST request may run before being aborted by the filter. Default is 5 minutes to accommodate legitimate long-running scripts and batch operations.");
	public static final Setting<Integer> JsonRestDosMaxIdleTrackerMs  = new IntegerSetting(servletsGroup, "JsonRestServlet", "jsonrestservlet.dosfilter.maxidletrackerms",  30000,      "How long (ms) to keep tracking request rates for an idle JSON REST connection.");
	public static final Setting<Boolean> JsonRestDosInsertHeaders     = new BooleanSetting(servletsGroup, "JsonRestServlet", "jsonrestservlet.dosfilter.insertheaders",     true,       "Insert DoSFilter response headers on the JSON REST servlet.");
	public static final Setting<Boolean> JsonRestDosRemotePort        = new BooleanSetting(servletsGroup, "JsonRestServlet", "jsonrestservlet.dosfilter.remoteport",        false,      "Track rate per IP+port (connection) instead of per IP on the JSON REST servlet.");
	public static final Setting<String>  JsonRestDosIpWhitelist       = new StringSetting(servletsGroup,  "JsonRestServlet", "jsonrestservlet.dosfilter.ipwhitelist",       "127.0.0.1","Comma-separated IPs exempt from rate limiting on the JSON REST servlet.");
	public static final Setting<Boolean> JsonRestDosManagedAttr       = new BooleanSetting(servletsGroup, "JsonRestServlet", "jsonrestservlet.dosfilter.managedattr",       true,       "Expose the JSON REST servlet's DoSFilter as a ServletContext attribute for external management (e.g. JMX).");
	public static final Setting<Integer> JsonRestDosTooManyCode       = new IntegerSetting(servletsGroup, "JsonRestServlet", "jsonrestservlet.dosfilter.toomanycode",       429,        "HTTP status code returned when the JSON REST rate limit is exceeded (429 or 503 typical).");

	// HtmlServlet DoS settings
	public static final Setting<Boolean> HtmlDosEnabled           = new BooleanSetting(servletsGroup, "HtmlServlet", "htmlservlet.dosfilter.enabled",           true,       "Enable DoSFilter for the HTML servlet.");
	public static final Setting<Integer> HtmlDosMaxRequestsPerSec = new IntegerSetting(servletsGroup, "HtmlServlet", "htmlservlet.dosfilter.maxrequestspersec", 10,         "Max inbound requests per second per client for the HTML servlet. Excess requests are delayed, then throttled.");
	public static final Setting<Integer> HtmlDosDelayMs           = new IntegerSetting(servletsGroup, "HtmlServlet", "htmlservlet.dosfilter.delayMs",           100,        "Delay in milliseconds for over-limit requests on the HTML servlet. -1 = reject, 0 = no delay.");
	public static final Setting<Integer> HtmlDosMaxWaitMs         = new IntegerSetting(servletsGroup, "HtmlServlet", "htmlservlet.dosfilter.maxwaitms",         50,         "Blocking wait time in ms for the throttle semaphore on the HTML servlet.");
	public static final Setting<Integer> HtmlDosThrottledRequests = new IntegerSetting(servletsGroup, "HtmlServlet", "htmlservlet.dosfilter.throttledrequests", 5,          "Requests over the rate limit considered at once on the HTML servlet.");
	public static final Setting<Integer> HtmlDosThrottleMs        = new IntegerSetting(servletsGroup, "HtmlServlet", "htmlservlet.dosfilter.throttlems",        30000,      "Async wait time in ms for the throttle semaphore on the HTML servlet.");
	public static final Setting<Integer> HtmlDosMaxRequestMs      = new IntegerSetting(servletsGroup, "HtmlServlet", "htmlservlet.dosfilter.maxrequestms",      30000,      "Maximum duration in ms an inbound HTML request may run before being aborted by the filter.");
	public static final Setting<Integer> HtmlDosMaxIdleTrackerMs  = new IntegerSetting(servletsGroup, "HtmlServlet", "htmlservlet.dosfilter.maxidletrackerms",  30000,      "How long (ms) to keep tracking request rates for an idle HTML connection.");
	public static final Setting<Boolean> HtmlDosInsertHeaders     = new BooleanSetting(servletsGroup, "HtmlServlet", "htmlservlet.dosfilter.insertheaders",     true,       "Insert DoSFilter response headers on the HTML servlet.");
	public static final Setting<Boolean> HtmlDosRemotePort        = new BooleanSetting(servletsGroup, "HtmlServlet", "htmlservlet.dosfilter.remoteport",        false,      "Track rate per IP+port (connection) instead of per IP on the HTML servlet.");
	public static final Setting<String>  HtmlDosIpWhitelist       = new StringSetting(servletsGroup,  "HtmlServlet", "htmlservlet.dosfilter.ipwhitelist",       "127.0.0.1","Comma-separated IPs exempt from rate limiting on the HTML servlet.");
	public static final Setting<Boolean> HtmlDosManagedAttr       = new BooleanSetting(servletsGroup, "HtmlServlet", "htmlservlet.dosfilter.managedattr",       true,       "Expose the HTML servlet's DoSFilter as a ServletContext attribute for external management (e.g. JMX).");
	public static final Setting<Integer> HtmlDosTooManyCode       = new IntegerSetting(servletsGroup, "HtmlServlet", "htmlservlet.dosfilter.toomanycode",       429,        "HTTP status code returned when the HTML rate limit is exceeded (429 or 503 typical).");

	// CsvServlet DoS settings
	public static final Setting<Boolean> CsvDosEnabled           = new BooleanSetting(servletsGroup, "CsvServlet", "csvservlet.dosfilter.enabled",           true,       "Enable DoSFilter for the CSV servlet.");
	public static final Setting<Integer> CsvDosMaxRequestsPerSec = new IntegerSetting(servletsGroup, "CsvServlet", "csvservlet.dosfilter.maxrequestspersec", 10,         "Max inbound requests per second per client for the CSV servlet. Excess requests are delayed, then throttled.");
	public static final Setting<Integer> CsvDosDelayMs           = new IntegerSetting(servletsGroup, "CsvServlet", "csvservlet.dosfilter.delayMs",           100,        "Delay in milliseconds for over-limit requests on the CSV servlet. -1 = reject, 0 = no delay.");
	public static final Setting<Integer> CsvDosMaxWaitMs         = new IntegerSetting(servletsGroup, "CsvServlet", "csvservlet.dosfilter.maxwaitms",         50,         "Blocking wait time in ms for the throttle semaphore on the CSV servlet.");
	public static final Setting<Integer> CsvDosThrottledRequests = new IntegerSetting(servletsGroup, "CsvServlet", "csvservlet.dosfilter.throttledrequests", 5,          "Requests over the rate limit considered at once on the CSV servlet.");
	public static final Setting<Integer> CsvDosThrottleMs        = new IntegerSetting(servletsGroup, "CsvServlet", "csvservlet.dosfilter.throttlems",        30000,      "Async wait time in ms for the throttle semaphore on the CSV servlet.");
	public static final Setting<Integer> CsvDosMaxRequestMs      = new IntegerSetting(servletsGroup, "CsvServlet", "csvservlet.dosfilter.maxrequestms",      30000,      "Maximum duration in ms an inbound CSV request may run before being aborted by the filter.");
	public static final Setting<Integer> CsvDosMaxIdleTrackerMs  = new IntegerSetting(servletsGroup, "CsvServlet", "csvservlet.dosfilter.maxidletrackerms",  30000,      "How long (ms) to keep tracking request rates for an idle CSV connection.");
	public static final Setting<Boolean> CsvDosInsertHeaders     = new BooleanSetting(servletsGroup, "CsvServlet", "csvservlet.dosfilter.insertheaders",     true,       "Insert DoSFilter response headers on the CSV servlet.");
	public static final Setting<Boolean> CsvDosRemotePort        = new BooleanSetting(servletsGroup, "CsvServlet", "csvservlet.dosfilter.remoteport",        false,      "Track rate per IP+port (connection) instead of per IP on the CSV servlet.");
	public static final Setting<String>  CsvDosIpWhitelist       = new StringSetting(servletsGroup,  "CsvServlet", "csvservlet.dosfilter.ipwhitelist",       "127.0.0.1","Comma-separated IPs exempt from rate limiting on the CSV servlet.");
	public static final Setting<Boolean> CsvDosManagedAttr       = new BooleanSetting(servletsGroup, "CsvServlet", "csvservlet.dosfilter.managedattr",       true,       "Expose the CSV servlet's DoSFilter as a ServletContext attribute for external management (e.g. JMX).");
	public static final Setting<Integer> CsvDosTooManyCode       = new IntegerSetting(servletsGroup, "CsvServlet", "csvservlet.dosfilter.toomanycode",       429,        "HTTP status code returned when the CSV rate limit is exceeded (429 or 503 typical).");

	// UploadServlet DoS settings
	public static final Setting<Boolean> UploadDosEnabled           = new BooleanSetting(servletsGroup, "UploadServlet", "uploadservlet.dosfilter.enabled",           true,       "Enable DoSFilter for the Upload servlet.");
	public static final Setting<Integer> UploadDosMaxRequestsPerSec = new IntegerSetting(servletsGroup, "UploadServlet", "uploadservlet.dosfilter.maxrequestspersec", 10,         "Max inbound requests per second per client for the Upload servlet. Excess requests are delayed, then throttled.");
	public static final Setting<Integer> UploadDosDelayMs           = new IntegerSetting(servletsGroup, "UploadServlet", "uploadservlet.dosfilter.delayMs",           100,        "Delay in milliseconds for over-limit requests on the Upload servlet. -1 = reject, 0 = no delay.");
	public static final Setting<Integer> UploadDosMaxWaitMs         = new IntegerSetting(servletsGroup, "UploadServlet", "uploadservlet.dosfilter.maxwaitms",         50,         "Blocking wait time in ms for the throttle semaphore on the Upload servlet.");
	public static final Setting<Integer> UploadDosThrottledRequests = new IntegerSetting(servletsGroup, "UploadServlet", "uploadservlet.dosfilter.throttledrequests", 5,          "Requests over the rate limit considered at once on the Upload servlet.");
	public static final Setting<Integer> UploadDosThrottleMs        = new IntegerSetting(servletsGroup, "UploadServlet", "uploadservlet.dosfilter.throttlems",        30000,      "Async wait time in ms for the throttle semaphore on the Upload servlet.");
	public static final Setting<Integer> UploadDosMaxRequestMs      = new IntegerSetting(servletsGroup, "UploadServlet", "uploadservlet.dosfilter.maxrequestms",      30000,      "Maximum duration in ms an inbound Upload request may run before being aborted by the filter. Note: large uploads may require raising this.");
	public static final Setting<Integer> UploadDosMaxIdleTrackerMs  = new IntegerSetting(servletsGroup, "UploadServlet", "uploadservlet.dosfilter.maxidletrackerms",  30000,      "How long (ms) to keep tracking request rates for an idle Upload connection.");
	public static final Setting<Boolean> UploadDosInsertHeaders     = new BooleanSetting(servletsGroup, "UploadServlet", "uploadservlet.dosfilter.insertheaders",     true,       "Insert DoSFilter response headers on the Upload servlet.");
	public static final Setting<Boolean> UploadDosRemotePort        = new BooleanSetting(servletsGroup, "UploadServlet", "uploadservlet.dosfilter.remoteport",        false,      "Track rate per IP+port (connection) instead of per IP on the Upload servlet.");
	public static final Setting<String>  UploadDosIpWhitelist       = new StringSetting(servletsGroup,  "UploadServlet", "uploadservlet.dosfilter.ipwhitelist",       "127.0.0.1","Comma-separated IPs exempt from rate limiting on the Upload servlet.");
	public static final Setting<Boolean> UploadDosManagedAttr       = new BooleanSetting(servletsGroup, "UploadServlet", "uploadservlet.dosfilter.managedattr",       true,       "Expose the Upload servlet's DoSFilter as a ServletContext attribute for external management (e.g. JMX).");
	public static final Setting<Integer> UploadDosTooManyCode       = new IntegerSetting(servletsGroup, "UploadServlet", "uploadservlet.dosfilter.toomanycode",       429,        "HTTP status code returned when the Upload rate limit is exceeded (429 or 503 typical).");

	// ProxyServlet DoS settings
	public static final Setting<Boolean> ProxyDosEnabled           = new BooleanSetting(servletsGroup, "ProxyServlet", "proxyservlet.dosfilter.enabled",           true,       "Enable DoSFilter for the Proxy servlet.");
	public static final Setting<Integer> ProxyDosMaxRequestsPerSec = new IntegerSetting(servletsGroup, "ProxyServlet", "proxyservlet.dosfilter.maxrequestspersec", 10,         "Max inbound requests per second per client for the Proxy servlet. Excess requests are delayed, then throttled.");
	public static final Setting<Integer> ProxyDosDelayMs           = new IntegerSetting(servletsGroup, "ProxyServlet", "proxyservlet.dosfilter.delayMs",           100,        "Delay in milliseconds for over-limit requests on the Proxy servlet. -1 = reject, 0 = no delay.");
	public static final Setting<Integer> ProxyDosMaxWaitMs         = new IntegerSetting(servletsGroup, "ProxyServlet", "proxyservlet.dosfilter.maxwaitms",         50,         "Blocking wait time in ms for the throttle semaphore on the Proxy servlet.");
	public static final Setting<Integer> ProxyDosThrottledRequests = new IntegerSetting(servletsGroup, "ProxyServlet", "proxyservlet.dosfilter.throttledrequests", 5,          "Requests over the rate limit considered at once on the Proxy servlet.");
	public static final Setting<Integer> ProxyDosThrottleMs        = new IntegerSetting(servletsGroup, "ProxyServlet", "proxyservlet.dosfilter.throttlems",        30000,      "Async wait time in ms for the throttle semaphore on the Proxy servlet.");
	public static final Setting<Integer> ProxyDosMaxRequestMs      = new IntegerSetting(servletsGroup, "ProxyServlet", "proxyservlet.dosfilter.maxrequestms",      300000,     "Maximum duration in ms an inbound Proxy request may run before being aborted by the filter. Default is 5 minutes since proxied upstream calls may take a while.");
	public static final Setting<Integer> ProxyDosMaxIdleTrackerMs  = new IntegerSetting(servletsGroup, "ProxyServlet", "proxyservlet.dosfilter.maxidletrackerms",  30000,      "How long (ms) to keep tracking request rates for an idle Proxy connection.");
	public static final Setting<Boolean> ProxyDosInsertHeaders     = new BooleanSetting(servletsGroup, "ProxyServlet", "proxyservlet.dosfilter.insertheaders",     true,       "Insert DoSFilter response headers on the Proxy servlet.");
	public static final Setting<Boolean> ProxyDosRemotePort        = new BooleanSetting(servletsGroup, "ProxyServlet", "proxyservlet.dosfilter.remoteport",        false,      "Track rate per IP+port (connection) instead of per IP on the Proxy servlet.");
	public static final Setting<String>  ProxyDosIpWhitelist       = new StringSetting(servletsGroup,  "ProxyServlet", "proxyservlet.dosfilter.ipwhitelist",       "127.0.0.1","Comma-separated IPs exempt from rate limiting on the Proxy servlet.");
	public static final Setting<Boolean> ProxyDosManagedAttr       = new BooleanSetting(servletsGroup, "ProxyServlet", "proxyservlet.dosfilter.managedattr",       true,       "Expose the Proxy servlet's DoSFilter as a ServletContext attribute for external management (e.g. JMX).");
	public static final Setting<Integer> ProxyDosTooManyCode       = new IntegerSetting(servletsGroup, "ProxyServlet", "proxyservlet.dosfilter.toomanycode",       429,        "HTTP status code returned when the Proxy rate limit is exceeded (429 or 503 typical).");

	// DeploymentServlet DoS settings
	public static final Setting<Boolean> DeploymentDosEnabled           = new BooleanSetting(servletsGroup, "DeploymentServlet", "deploymentservlet.dosfilter.enabled",           true,       "Enable DoSFilter for the Deployment servlet.");
	public static final Setting<Integer> DeploymentDosMaxRequestsPerSec = new IntegerSetting(servletsGroup, "DeploymentServlet", "deploymentservlet.dosfilter.maxrequestspersec", 10,         "Max inbound requests per second per client for the Deployment servlet. Excess requests are delayed, then throttled.");
	public static final Setting<Integer> DeploymentDosDelayMs           = new IntegerSetting(servletsGroup, "DeploymentServlet", "deploymentservlet.dosfilter.delayMs",           100,        "Delay in milliseconds for over-limit requests on the Deployment servlet. -1 = reject, 0 = no delay.");
	public static final Setting<Integer> DeploymentDosMaxWaitMs         = new IntegerSetting(servletsGroup, "DeploymentServlet", "deploymentservlet.dosfilter.maxwaitms",         50,         "Blocking wait time in ms for the throttle semaphore on the Deployment servlet.");
	public static final Setting<Integer> DeploymentDosThrottledRequests = new IntegerSetting(servletsGroup, "DeploymentServlet", "deploymentservlet.dosfilter.throttledrequests", 5,          "Requests over the rate limit considered at once on the Deployment servlet.");
	public static final Setting<Integer> DeploymentDosThrottleMs        = new IntegerSetting(servletsGroup, "DeploymentServlet", "deploymentservlet.dosfilter.throttlems",        30000,      "Async wait time in ms for the throttle semaphore on the Deployment servlet.");
	public static final Setting<Integer> DeploymentDosMaxRequestMs      = new IntegerSetting(servletsGroup, "DeploymentServlet", "deploymentservlet.dosfilter.maxrequestms",      600000,     "Maximum duration in ms an inbound Deployment request may run before being aborted by the filter. Default is 10 minutes. Large deployments may require raising this further.");
	public static final Setting<Integer> DeploymentDosMaxIdleTrackerMs  = new IntegerSetting(servletsGroup, "DeploymentServlet", "deploymentservlet.dosfilter.maxidletrackerms",  30000,      "How long (ms) to keep tracking request rates for an idle Deployment connection.");
	public static final Setting<Boolean> DeploymentDosInsertHeaders     = new BooleanSetting(servletsGroup, "DeploymentServlet", "deploymentservlet.dosfilter.insertheaders",     true,       "Insert DoSFilter response headers on the Deployment servlet.");
	public static final Setting<Boolean> DeploymentDosRemotePort        = new BooleanSetting(servletsGroup, "DeploymentServlet", "deploymentservlet.dosfilter.remoteport",        false,      "Track rate per IP+port (connection) instead of per IP on the Deployment servlet.");
	public static final Setting<String>  DeploymentDosIpWhitelist       = new StringSetting(servletsGroup,  "DeploymentServlet", "deploymentservlet.dosfilter.ipwhitelist",       "127.0.0.1","Comma-separated IPs exempt from rate limiting on the Deployment servlet.");
	public static final Setting<Boolean> DeploymentDosManagedAttr       = new BooleanSetting(servletsGroup, "DeploymentServlet", "deploymentservlet.dosfilter.managedattr",       true,       "Expose the Deployment servlet's DoSFilter as a ServletContext attribute for external management (e.g. JMX).");
	public static final Setting<Integer> DeploymentDosTooManyCode       = new IntegerSetting(servletsGroup, "DeploymentServlet", "deploymentservlet.dosfilter.toomanycode",       429,        "HTTP status code returned when the Deployment rate limit is exceeded (429 or 503 typical).");

	// FlowServlet DoS settings
	public static final Setting<Boolean> FlowDosEnabled           = new BooleanSetting(servletsGroup, "FlowServlet", "flowservlet.dosfilter.enabled",           true,       "Enable DoSFilter for the Flow servlet.");
	public static final Setting<Integer> FlowDosMaxRequestsPerSec = new IntegerSetting(servletsGroup, "FlowServlet", "flowservlet.dosfilter.maxrequestspersec", 10,         "Max inbound requests per second per client for the Flow servlet. Excess requests are delayed, then throttled.");
	public static final Setting<Integer> FlowDosDelayMs           = new IntegerSetting(servletsGroup, "FlowServlet", "flowservlet.dosfilter.delayMs",           100,        "Delay in milliseconds for over-limit requests on the Flow servlet. -1 = reject, 0 = no delay.");
	public static final Setting<Integer> FlowDosMaxWaitMs         = new IntegerSetting(servletsGroup, "FlowServlet", "flowservlet.dosfilter.maxwaitms",         50,         "Blocking wait time in ms for the throttle semaphore on the Flow servlet.");
	public static final Setting<Integer> FlowDosThrottledRequests = new IntegerSetting(servletsGroup, "FlowServlet", "flowservlet.dosfilter.throttledrequests", 5,          "Requests over the rate limit considered at once on the Flow servlet.");
	public static final Setting<Integer> FlowDosThrottleMs        = new IntegerSetting(servletsGroup, "FlowServlet", "flowservlet.dosfilter.throttlems",        30000,      "Async wait time in ms for the throttle semaphore on the Flow servlet.");
	public static final Setting<Integer> FlowDosMaxRequestMs      = new IntegerSetting(servletsGroup, "FlowServlet", "flowservlet.dosfilter.maxrequestms",      300000,     "Maximum duration in ms an inbound Flow request may run before being aborted by the filter. Default is 5 minutes to accommodate legitimate long-running flows.");
	public static final Setting<Integer> FlowDosMaxIdleTrackerMs  = new IntegerSetting(servletsGroup, "FlowServlet", "flowservlet.dosfilter.maxidletrackerms",  30000,      "How long (ms) to keep tracking request rates for an idle Flow connection.");
	public static final Setting<Boolean> FlowDosInsertHeaders     = new BooleanSetting(servletsGroup, "FlowServlet", "flowservlet.dosfilter.insertheaders",     true,       "Insert DoSFilter response headers on the Flow servlet.");
	public static final Setting<Boolean> FlowDosRemotePort        = new BooleanSetting(servletsGroup, "FlowServlet", "flowservlet.dosfilter.remoteport",        false,      "Track rate per IP+port (connection) instead of per IP on the Flow servlet.");
	public static final Setting<String>  FlowDosIpWhitelist       = new StringSetting(servletsGroup,  "FlowServlet", "flowservlet.dosfilter.ipwhitelist",       "127.0.0.1","Comma-separated IPs exempt from rate limiting on the Flow servlet.");
	public static final Setting<Boolean> FlowDosManagedAttr       = new BooleanSetting(servletsGroup, "FlowServlet", "flowservlet.dosfilter.managedattr",       true,       "Expose the Flow servlet's DoSFilter as a ServletContext attribute for external management (e.g. JMX).");
	public static final Setting<Integer> FlowDosTooManyCode       = new IntegerSetting(servletsGroup, "FlowServlet", "flowservlet.dosfilter.toomanycode",       429,        "HTTP status code returned when the Flow rate limit is exceeded (429 or 503 typical).");

	// LoginServlet DoS settings
	public static final Setting<Boolean> LoginDosEnabled           = new BooleanSetting(servletsGroup, "LoginServlet", "loginservlet.dosfilter.enabled",           true,       "Enable DoSFilter for the Login servlet. Recommended to protect against brute-force and credential-stuffing attacks.");
	public static final Setting<Integer> LoginDosMaxRequestsPerSec = new IntegerSetting(servletsGroup, "LoginServlet", "loginservlet.dosfilter.maxrequestspersec", 10,         "Max inbound requests per second per client for the Login servlet. Excess requests are delayed, then throttled.");
	public static final Setting<Integer> LoginDosDelayMs           = new IntegerSetting(servletsGroup, "LoginServlet", "loginservlet.dosfilter.delayMs",           100,        "Delay in milliseconds for over-limit requests on the Login servlet. -1 = reject, 0 = no delay.");
	public static final Setting<Integer> LoginDosMaxWaitMs         = new IntegerSetting(servletsGroup, "LoginServlet", "loginservlet.dosfilter.maxwaitms",         50,         "Blocking wait time in ms for the throttle semaphore on the Login servlet.");
	public static final Setting<Integer> LoginDosThrottledRequests = new IntegerSetting(servletsGroup, "LoginServlet", "loginservlet.dosfilter.throttledrequests", 5,          "Requests over the rate limit considered at once on the Login servlet.");
	public static final Setting<Integer> LoginDosThrottleMs        = new IntegerSetting(servletsGroup, "LoginServlet", "loginservlet.dosfilter.throttlems",        30000,      "Async wait time in ms for the throttle semaphore on the Login servlet.");
	public static final Setting<Integer> LoginDosMaxRequestMs      = new IntegerSetting(servletsGroup, "LoginServlet", "loginservlet.dosfilter.maxrequestms",      30000,      "Maximum duration in ms an inbound Login request may run before being aborted by the filter.");
	public static final Setting<Integer> LoginDosMaxIdleTrackerMs  = new IntegerSetting(servletsGroup, "LoginServlet", "loginservlet.dosfilter.maxidletrackerms",  30000,      "How long (ms) to keep tracking request rates for an idle Login connection.");
	public static final Setting<Boolean> LoginDosInsertHeaders     = new BooleanSetting(servletsGroup, "LoginServlet", "loginservlet.dosfilter.insertheaders",     true,       "Insert DoSFilter response headers on the Login servlet.");
	public static final Setting<Boolean> LoginDosRemotePort        = new BooleanSetting(servletsGroup, "LoginServlet", "loginservlet.dosfilter.remoteport",        false,      "Track rate per IP+port (connection) instead of per IP on the Login servlet.");
	public static final Setting<String>  LoginDosIpWhitelist       = new StringSetting(servletsGroup,  "LoginServlet", "loginservlet.dosfilter.ipwhitelist",       "127.0.0.1","Comma-separated IPs exempt from rate limiting on the Login servlet.");
	public static final Setting<Boolean> LoginDosManagedAttr       = new BooleanSetting(servletsGroup, "LoginServlet", "loginservlet.dosfilter.managedattr",       true,       "Expose the Login servlet's DoSFilter as a ServletContext attribute for external management (e.g. JMX).");
	public static final Setting<Integer> LoginDosTooManyCode       = new IntegerSetting(servletsGroup, "LoginServlet", "loginservlet.dosfilter.toomanycode",       429,        "HTTP status code returned when the Login rate limit is exceeded (429 or 503 typical).");

	// LogoutServlet DoS settings
	public static final Setting<Boolean> LogoutDosEnabled           = new BooleanSetting(servletsGroup, "LogoutServlet", "logoutservlet.dosfilter.enabled",           true,       "Enable DoSFilter for the Logout servlet.");
	public static final Setting<Integer> LogoutDosMaxRequestsPerSec = new IntegerSetting(servletsGroup, "LogoutServlet", "logoutservlet.dosfilter.maxrequestspersec", 10,         "Max inbound requests per second per client for the Logout servlet. Excess requests are delayed, then throttled.");
	public static final Setting<Integer> LogoutDosDelayMs           = new IntegerSetting(servletsGroup, "LogoutServlet", "logoutservlet.dosfilter.delayMs",           100,        "Delay in milliseconds for over-limit requests on the Logout servlet. -1 = reject, 0 = no delay.");
	public static final Setting<Integer> LogoutDosMaxWaitMs         = new IntegerSetting(servletsGroup, "LogoutServlet", "logoutservlet.dosfilter.maxwaitms",         50,         "Blocking wait time in ms for the throttle semaphore on the Logout servlet.");
	public static final Setting<Integer> LogoutDosThrottledRequests = new IntegerSetting(servletsGroup, "LogoutServlet", "logoutservlet.dosfilter.throttledrequests", 5,          "Requests over the rate limit considered at once on the Logout servlet.");
	public static final Setting<Integer> LogoutDosThrottleMs        = new IntegerSetting(servletsGroup, "LogoutServlet", "logoutservlet.dosfilter.throttlems",        30000,      "Async wait time in ms for the throttle semaphore on the Logout servlet.");
	public static final Setting<Integer> LogoutDosMaxRequestMs      = new IntegerSetting(servletsGroup, "LogoutServlet", "logoutservlet.dosfilter.maxrequestms",      30000,      "Maximum duration in ms an inbound Logout request may run before being aborted by the filter.");
	public static final Setting<Integer> LogoutDosMaxIdleTrackerMs  = new IntegerSetting(servletsGroup, "LogoutServlet", "logoutservlet.dosfilter.maxidletrackerms",  30000,      "How long (ms) to keep tracking request rates for an idle Logout connection.");
	public static final Setting<Boolean> LogoutDosInsertHeaders     = new BooleanSetting(servletsGroup, "LogoutServlet", "logoutservlet.dosfilter.insertheaders",     true,       "Insert DoSFilter response headers on the Logout servlet.");
	public static final Setting<Boolean> LogoutDosRemotePort        = new BooleanSetting(servletsGroup, "LogoutServlet", "logoutservlet.dosfilter.remoteport",        false,      "Track rate per IP+port (connection) instead of per IP on the Logout servlet.");
	public static final Setting<String>  LogoutDosIpWhitelist       = new StringSetting(servletsGroup,  "LogoutServlet", "logoutservlet.dosfilter.ipwhitelist",       "127.0.0.1","Comma-separated IPs exempt from rate limiting on the Logout servlet.");
	public static final Setting<Boolean> LogoutDosManagedAttr       = new BooleanSetting(servletsGroup, "LogoutServlet", "logoutservlet.dosfilter.managedattr",       true,       "Expose the Logout servlet's DoSFilter as a ServletContext attribute for external management (e.g. JMX).");
	public static final Setting<Integer> LogoutDosTooManyCode       = new IntegerSetting(servletsGroup, "LogoutServlet", "logoutservlet.dosfilter.toomanycode",       429,        "HTTP status code returned when the Logout rate limit is exceeded (429 or 503 typical).");

	// TokenServlet DoS settings
	public static final Setting<Boolean> TokenDosEnabled           = new BooleanSetting(servletsGroup, "TokenServlet", "tokenservlet.dosfilter.enabled",           true,       "Enable DoSFilter for the Token servlet. Recommended to protect JWT token issuance and refresh endpoints.");
	public static final Setting<Integer> TokenDosMaxRequestsPerSec = new IntegerSetting(servletsGroup, "TokenServlet", "tokenservlet.dosfilter.maxrequestspersec", 10,         "Max inbound requests per second per client for the Token servlet. Excess requests are delayed, then throttled.");
	public static final Setting<Integer> TokenDosDelayMs           = new IntegerSetting(servletsGroup, "TokenServlet", "tokenservlet.dosfilter.delayMs",           100,        "Delay in milliseconds for over-limit requests on the Token servlet. -1 = reject, 0 = no delay.");
	public static final Setting<Integer> TokenDosMaxWaitMs         = new IntegerSetting(servletsGroup, "TokenServlet", "tokenservlet.dosfilter.maxwaitms",         50,         "Blocking wait time in ms for the throttle semaphore on the Token servlet.");
	public static final Setting<Integer> TokenDosThrottledRequests = new IntegerSetting(servletsGroup, "TokenServlet", "tokenservlet.dosfilter.throttledrequests", 5,          "Requests over the rate limit considered at once on the Token servlet.");
	public static final Setting<Integer> TokenDosThrottleMs        = new IntegerSetting(servletsGroup, "TokenServlet", "tokenservlet.dosfilter.throttlems",        30000,      "Async wait time in ms for the throttle semaphore on the Token servlet.");
	public static final Setting<Integer> TokenDosMaxRequestMs      = new IntegerSetting(servletsGroup, "TokenServlet", "tokenservlet.dosfilter.maxrequestms",      30000,      "Maximum duration in ms an inbound Token request may run before being aborted by the filter.");
	public static final Setting<Integer> TokenDosMaxIdleTrackerMs  = new IntegerSetting(servletsGroup, "TokenServlet", "tokenservlet.dosfilter.maxidletrackerms",  30000,      "How long (ms) to keep tracking request rates for an idle Token connection.");
	public static final Setting<Boolean> TokenDosInsertHeaders     = new BooleanSetting(servletsGroup, "TokenServlet", "tokenservlet.dosfilter.insertheaders",     true,       "Insert DoSFilter response headers on the Token servlet.");
	public static final Setting<Boolean> TokenDosRemotePort        = new BooleanSetting(servletsGroup, "TokenServlet", "tokenservlet.dosfilter.remoteport",        false,      "Track rate per IP+port (connection) instead of per IP on the Token servlet.");
	public static final Setting<String>  TokenDosIpWhitelist       = new StringSetting(servletsGroup,  "TokenServlet", "tokenservlet.dosfilter.ipwhitelist",       "127.0.0.1","Comma-separated IPs exempt from rate limiting on the Token servlet.");
	public static final Setting<Boolean> TokenDosManagedAttr       = new BooleanSetting(servletsGroup, "TokenServlet", "tokenservlet.dosfilter.managedattr",       true,       "Expose the Token servlet's DoSFilter as a ServletContext attribute for external management (e.g. JMX).");
	public static final Setting<Integer> TokenDosTooManyCode       = new IntegerSetting(servletsGroup, "TokenServlet", "tokenservlet.dosfilter.toomanycode",       429,        "HTTP status code returned when the Token rate limit is exceeded (429 or 503 typical).");

	// EventSourceServlet DoS settings (disabled by default -- SSE connections are long-lived by design)
	public static final Setting<Boolean> EventSourceDosEnabled           = new BooleanSetting(servletsGroup, "EventSourceServlet", "eventsourceservlet.dosfilter.enabled",           false,      "Enable DoSFilter for the EventSource (SSE) servlet. Disabled by default: SSE is a long-lived streaming protocol and maxRequestMs would terminate client subscriptions.");
	public static final Setting<Integer> EventSourceDosMaxRequestsPerSec = new IntegerSetting(servletsGroup, "EventSourceServlet", "eventsourceservlet.dosfilter.maxrequestspersec", 10,         "Max inbound requests per second per client for the EventSource servlet. Excess requests are delayed, then throttled.");
	public static final Setting<Integer> EventSourceDosDelayMs           = new IntegerSetting(servletsGroup, "EventSourceServlet", "eventsourceservlet.dosfilter.delayMs",           100,        "Delay in milliseconds for over-limit requests on the EventSource servlet. -1 = reject, 0 = no delay.");
	public static final Setting<Integer> EventSourceDosMaxWaitMs         = new IntegerSetting(servletsGroup, "EventSourceServlet", "eventsourceservlet.dosfilter.maxwaitms",         50,         "Blocking wait time in ms for the throttle semaphore on the EventSource servlet.");
	public static final Setting<Integer> EventSourceDosThrottledRequests = new IntegerSetting(servletsGroup, "EventSourceServlet", "eventsourceservlet.dosfilter.throttledrequests", 5,          "Requests over the rate limit considered at once on the EventSource servlet.");
	public static final Setting<Integer> EventSourceDosThrottleMs        = new IntegerSetting(servletsGroup, "EventSourceServlet", "eventsourceservlet.dosfilter.throttlems",        30000,      "Async wait time in ms for the throttle semaphore on the EventSource servlet.");
	public static final Setting<Integer> EventSourceDosMaxRequestMs      = new IntegerSetting(servletsGroup, "EventSourceServlet", "eventsourceservlet.dosfilter.maxrequestms",      30000,      "Maximum duration in ms an inbound EventSource request may run before being aborted by the filter. WARNING: SSE connections are long-lived; this will terminate them if enabled is true.");
	public static final Setting<Integer> EventSourceDosMaxIdleTrackerMs  = new IntegerSetting(servletsGroup, "EventSourceServlet", "eventsourceservlet.dosfilter.maxidletrackerms",  30000,      "How long (ms) to keep tracking request rates for an idle EventSource connection.");
	public static final Setting<Boolean> EventSourceDosInsertHeaders     = new BooleanSetting(servletsGroup, "EventSourceServlet", "eventsourceservlet.dosfilter.insertheaders",     true,       "Insert DoSFilter response headers on the EventSource servlet.");
	public static final Setting<Boolean> EventSourceDosRemotePort        = new BooleanSetting(servletsGroup, "EventSourceServlet", "eventsourceservlet.dosfilter.remoteport",        false,      "Track rate per IP+port (connection) instead of per IP on the EventSource servlet.");
	public static final Setting<String>  EventSourceDosIpWhitelist       = new StringSetting(servletsGroup,  "EventSourceServlet", "eventsourceservlet.dosfilter.ipwhitelist",       "127.0.0.1","Comma-separated IPs exempt from rate limiting on the EventSource servlet.");
	public static final Setting<Boolean> EventSourceDosManagedAttr       = new BooleanSetting(servletsGroup, "EventSourceServlet", "eventsourceservlet.dosfilter.managedattr",       true,       "Expose the EventSource servlet's DoSFilter as a ServletContext attribute for external management (e.g. JMX).");
	public static final Setting<Integer> EventSourceDosTooManyCode       = new IntegerSetting(servletsGroup, "EventSourceServlet", "eventsourceservlet.dosfilter.toomanycode",       429,        "HTTP status code returned when the EventSource rate limit is exceeded (429 or 503 typical).");

	// HealthCheckServlet DoS settings
	public static final Setting<Boolean> HealthCheckDosEnabled           = new BooleanSetting(servletsGroup, "HealthCheckServlet", "healthcheckservlet.dosfilter.enabled",           true,       "Enable DoSFilter for the HealthCheck servlet.");
	public static final Setting<Integer> HealthCheckDosMaxRequestsPerSec = new IntegerSetting(servletsGroup, "HealthCheckServlet", "healthcheckservlet.dosfilter.maxrequestspersec", 10,         "Max inbound requests per second per client for the HealthCheck servlet. Excess requests are delayed, then throttled.");
	public static final Setting<Integer> HealthCheckDosDelayMs           = new IntegerSetting(servletsGroup, "HealthCheckServlet", "healthcheckservlet.dosfilter.delayMs",           100,        "Delay in milliseconds for over-limit requests on the HealthCheck servlet. -1 = reject, 0 = no delay.");
	public static final Setting<Integer> HealthCheckDosMaxWaitMs         = new IntegerSetting(servletsGroup, "HealthCheckServlet", "healthcheckservlet.dosfilter.maxwaitms",         50,         "Blocking wait time in ms for the throttle semaphore on the HealthCheck servlet.");
	public static final Setting<Integer> HealthCheckDosThrottledRequests = new IntegerSetting(servletsGroup, "HealthCheckServlet", "healthcheckservlet.dosfilter.throttledrequests", 5,          "Requests over the rate limit considered at once on the HealthCheck servlet.");
	public static final Setting<Integer> HealthCheckDosThrottleMs        = new IntegerSetting(servletsGroup, "HealthCheckServlet", "healthcheckservlet.dosfilter.throttlems",        30000,      "Async wait time in ms for the throttle semaphore on the HealthCheck servlet.");
	public static final Setting<Integer> HealthCheckDosMaxRequestMs      = new IntegerSetting(servletsGroup, "HealthCheckServlet", "healthcheckservlet.dosfilter.maxrequestms",      30000,      "Maximum duration in ms an inbound HealthCheck request may run before being aborted by the filter.");
	public static final Setting<Integer> HealthCheckDosMaxIdleTrackerMs  = new IntegerSetting(servletsGroup, "HealthCheckServlet", "healthcheckservlet.dosfilter.maxidletrackerms",  30000,      "How long (ms) to keep tracking request rates for an idle HealthCheck connection.");
	public static final Setting<Boolean> HealthCheckDosInsertHeaders     = new BooleanSetting(servletsGroup, "HealthCheckServlet", "healthcheckservlet.dosfilter.insertheaders",     true,       "Insert DoSFilter response headers on the HealthCheck servlet.");
	public static final Setting<Boolean> HealthCheckDosRemotePort        = new BooleanSetting(servletsGroup, "HealthCheckServlet", "healthcheckservlet.dosfilter.remoteport",        false,      "Track rate per IP+port (connection) instead of per IP on the HealthCheck servlet.");
	public static final Setting<String>  HealthCheckDosIpWhitelist       = new StringSetting(servletsGroup,  "HealthCheckServlet", "healthcheckservlet.dosfilter.ipwhitelist",       "127.0.0.1","Comma-separated IPs exempt from rate limiting on the HealthCheck servlet.");
	public static final Setting<Boolean> HealthCheckDosManagedAttr       = new BooleanSetting(servletsGroup, "HealthCheckServlet", "healthcheckservlet.dosfilter.managedattr",       true,       "Expose the HealthCheck servlet's DoSFilter as a ServletContext attribute for external management (e.g. JMX).");
	public static final Setting<Integer> HealthCheckDosTooManyCode       = new IntegerSetting(servletsGroup, "HealthCheckServlet", "healthcheckservlet.dosfilter.toomanycode",       429,        "HTTP status code returned when the HealthCheck rate limit is exceeded (429 or 503 typical).");

	// HistogramServlet DoS settings
	public static final Setting<Boolean> HistogramDosEnabled           = new BooleanSetting(servletsGroup, "HistogramServlet", "histogramservlet.dosfilter.enabled",           true,       "Enable DoSFilter for the Histogram servlet.");
	public static final Setting<Integer> HistogramDosMaxRequestsPerSec = new IntegerSetting(servletsGroup, "HistogramServlet", "histogramservlet.dosfilter.maxrequestspersec", 10,         "Max inbound requests per second per client for the Histogram servlet. Excess requests are delayed, then throttled.");
	public static final Setting<Integer> HistogramDosDelayMs           = new IntegerSetting(servletsGroup, "HistogramServlet", "histogramservlet.dosfilter.delayMs",           100,        "Delay in milliseconds for over-limit requests on the Histogram servlet. -1 = reject, 0 = no delay.");
	public static final Setting<Integer> HistogramDosMaxWaitMs         = new IntegerSetting(servletsGroup, "HistogramServlet", "histogramservlet.dosfilter.maxwaitms",         50,         "Blocking wait time in ms for the throttle semaphore on the Histogram servlet.");
	public static final Setting<Integer> HistogramDosThrottledRequests = new IntegerSetting(servletsGroup, "HistogramServlet", "histogramservlet.dosfilter.throttledrequests", 5,          "Requests over the rate limit considered at once on the Histogram servlet.");
	public static final Setting<Integer> HistogramDosThrottleMs        = new IntegerSetting(servletsGroup, "HistogramServlet", "histogramservlet.dosfilter.throttlems",        30000,      "Async wait time in ms for the throttle semaphore on the Histogram servlet.");
	public static final Setting<Integer> HistogramDosMaxRequestMs      = new IntegerSetting(servletsGroup, "HistogramServlet", "histogramservlet.dosfilter.maxrequestms",      30000,      "Maximum duration in ms an inbound Histogram request may run before being aborted by the filter.");
	public static final Setting<Integer> HistogramDosMaxIdleTrackerMs  = new IntegerSetting(servletsGroup, "HistogramServlet", "histogramservlet.dosfilter.maxidletrackerms",  30000,      "How long (ms) to keep tracking request rates for an idle Histogram connection.");
	public static final Setting<Boolean> HistogramDosInsertHeaders     = new BooleanSetting(servletsGroup, "HistogramServlet", "histogramservlet.dosfilter.insertheaders",     true,       "Insert DoSFilter response headers on the Histogram servlet.");
	public static final Setting<Boolean> HistogramDosRemotePort        = new BooleanSetting(servletsGroup, "HistogramServlet", "histogramservlet.dosfilter.remoteport",        false,      "Track rate per IP+port (connection) instead of per IP on the Histogram servlet.");
	public static final Setting<String>  HistogramDosIpWhitelist       = new StringSetting(servletsGroup,  "HistogramServlet", "histogramservlet.dosfilter.ipwhitelist",       "127.0.0.1","Comma-separated IPs exempt from rate limiting on the Histogram servlet.");
	public static final Setting<Boolean> HistogramDosManagedAttr       = new BooleanSetting(servletsGroup, "HistogramServlet", "histogramservlet.dosfilter.managedattr",       true,       "Expose the Histogram servlet's DoSFilter as a ServletContext attribute for external management (e.g. JMX).");
	public static final Setting<Integer> HistogramDosTooManyCode       = new IntegerSetting(servletsGroup, "HistogramServlet", "histogramservlet.dosfilter.toomanycode",       429,        "HTTP status code returned when the Histogram rate limit is exceeded (429 or 503 typical).");

	// OpenAPIServlet DoS settings
	public static final Setting<Boolean> OpenAPIDosEnabled           = new BooleanSetting(servletsGroup, "OpenAPIServlet", "openapiservlet.dosfilter.enabled",           true,       "Enable DoSFilter for the OpenAPI servlet.");
	public static final Setting<Integer> OpenAPIDosMaxRequestsPerSec = new IntegerSetting(servletsGroup, "OpenAPIServlet", "openapiservlet.dosfilter.maxrequestspersec", 10,         "Max inbound requests per second per client for the OpenAPI servlet. Excess requests are delayed, then throttled.");
	public static final Setting<Integer> OpenAPIDosDelayMs           = new IntegerSetting(servletsGroup, "OpenAPIServlet", "openapiservlet.dosfilter.delayMs",           100,        "Delay in milliseconds for over-limit requests on the OpenAPI servlet. -1 = reject, 0 = no delay.");
	public static final Setting<Integer> OpenAPIDosMaxWaitMs         = new IntegerSetting(servletsGroup, "OpenAPIServlet", "openapiservlet.dosfilter.maxwaitms",         50,         "Blocking wait time in ms for the throttle semaphore on the OpenAPI servlet.");
	public static final Setting<Integer> OpenAPIDosThrottledRequests = new IntegerSetting(servletsGroup, "OpenAPIServlet", "openapiservlet.dosfilter.throttledrequests", 5,          "Requests over the rate limit considered at once on the OpenAPI servlet.");
	public static final Setting<Integer> OpenAPIDosThrottleMs        = new IntegerSetting(servletsGroup, "OpenAPIServlet", "openapiservlet.dosfilter.throttlems",        30000,      "Async wait time in ms for the throttle semaphore on the OpenAPI servlet.");
	public static final Setting<Integer> OpenAPIDosMaxRequestMs      = new IntegerSetting(servletsGroup, "OpenAPIServlet", "openapiservlet.dosfilter.maxrequestms",      30000,      "Maximum duration in ms an inbound OpenAPI request may run before being aborted by the filter.");
	public static final Setting<Integer> OpenAPIDosMaxIdleTrackerMs  = new IntegerSetting(servletsGroup, "OpenAPIServlet", "openapiservlet.dosfilter.maxidletrackerms",  30000,      "How long (ms) to keep tracking request rates for an idle OpenAPI connection.");
	public static final Setting<Boolean> OpenAPIDosInsertHeaders     = new BooleanSetting(servletsGroup, "OpenAPIServlet", "openapiservlet.dosfilter.insertheaders",     true,       "Insert DoSFilter response headers on the OpenAPI servlet.");
	public static final Setting<Boolean> OpenAPIDosRemotePort        = new BooleanSetting(servletsGroup, "OpenAPIServlet", "openapiservlet.dosfilter.remoteport",        false,      "Track rate per IP+port (connection) instead of per IP on the OpenAPI servlet.");
	public static final Setting<String>  OpenAPIDosIpWhitelist       = new StringSetting(servletsGroup,  "OpenAPIServlet", "openapiservlet.dosfilter.ipwhitelist",       "127.0.0.1","Comma-separated IPs exempt from rate limiting on the OpenAPI servlet.");
	public static final Setting<Boolean> OpenAPIDosManagedAttr       = new BooleanSetting(servletsGroup, "OpenAPIServlet", "openapiservlet.dosfilter.managedattr",       true,       "Expose the OpenAPI servlet's DoSFilter as a ServletContext attribute for external management (e.g. JMX).");
	public static final Setting<Integer> OpenAPIDosTooManyCode       = new IntegerSetting(servletsGroup, "OpenAPIServlet", "openapiservlet.dosfilter.toomanycode",       429,        "HTTP status code returned when the OpenAPI rate limit is exceeded (429 or 503 typical).");

	// MetricsServlet DoS settings
	public static final Setting<Boolean> MetricsDosEnabled           = new BooleanSetting(servletsGroup, "MetricsServlet", "metricsservlet.dosfilter.enabled",           true,       "Enable DoSFilter for the Metrics servlet.");
	public static final Setting<Integer> MetricsDosMaxRequestsPerSec = new IntegerSetting(servletsGroup, "MetricsServlet", "metricsservlet.dosfilter.maxrequestspersec", 10,         "Max inbound requests per second per client for the Metrics servlet. Excess requests are delayed, then throttled.");
	public static final Setting<Integer> MetricsDosDelayMs           = new IntegerSetting(servletsGroup, "MetricsServlet", "metricsservlet.dosfilter.delayMs",           100,        "Delay in milliseconds for over-limit requests on the Metrics servlet. -1 = reject, 0 = no delay.");
	public static final Setting<Integer> MetricsDosMaxWaitMs         = new IntegerSetting(servletsGroup, "MetricsServlet", "metricsservlet.dosfilter.maxwaitms",         50,         "Blocking wait time in ms for the throttle semaphore on the Metrics servlet.");
	public static final Setting<Integer> MetricsDosThrottledRequests = new IntegerSetting(servletsGroup, "MetricsServlet", "metricsservlet.dosfilter.throttledrequests", 5,          "Requests over the rate limit considered at once on the Metrics servlet.");
	public static final Setting<Integer> MetricsDosThrottleMs        = new IntegerSetting(servletsGroup, "MetricsServlet", "metricsservlet.dosfilter.throttlems",        30000,      "Async wait time in ms for the throttle semaphore on the Metrics servlet.");
	public static final Setting<Integer> MetricsDosMaxRequestMs      = new IntegerSetting(servletsGroup, "MetricsServlet", "metricsservlet.dosfilter.maxrequestms",      30000,      "Maximum duration in ms an inbound Metrics request may run before being aborted by the filter.");
	public static final Setting<Integer> MetricsDosMaxIdleTrackerMs  = new IntegerSetting(servletsGroup, "MetricsServlet", "metricsservlet.dosfilter.maxidletrackerms",  30000,      "How long (ms) to keep tracking request rates for an idle Metrics connection.");
	public static final Setting<Boolean> MetricsDosInsertHeaders     = new BooleanSetting(servletsGroup, "MetricsServlet", "metricsservlet.dosfilter.insertheaders",     true,       "Insert DoSFilter response headers on the Metrics servlet.");
	public static final Setting<Boolean> MetricsDosRemotePort        = new BooleanSetting(servletsGroup, "MetricsServlet", "metricsservlet.dosfilter.remoteport",        false,      "Track rate per IP+port (connection) instead of per IP on the Metrics servlet.");
	public static final Setting<String>  MetricsDosIpWhitelist       = new StringSetting(servletsGroup,  "MetricsServlet", "metricsservlet.dosfilter.ipwhitelist",       "127.0.0.1","Comma-separated IPs exempt from rate limiting on the Metrics servlet.");
	public static final Setting<Boolean> MetricsDosManagedAttr       = new BooleanSetting(servletsGroup, "MetricsServlet", "metricsservlet.dosfilter.managedattr",       true,       "Expose the Metrics servlet's DoSFilter as a ServletContext attribute for external management (e.g. JMX).");
	public static final Setting<Integer> MetricsDosTooManyCode       = new IntegerSetting(servletsGroup, "MetricsServlet", "metricsservlet.dosfilter.toomanycode",       429,        "HTTP status code returned when the Metrics rate limit is exceeded (429 or 503 typical).");

	// cron settings
	public static final Setting<String> CronTasks                   = new StringSetting(cronGroup,  "", "CronService.tasks", "", "List of cron task configurations or method names separated by space. This only configures the list of tasks. For each task, there needs to be another configuration entry named '<taskname>.cronExpression' with the appropriate cron schedule configuration. Restart of CronService required.");
	public static final Setting<Boolean> CronAllowParallelExecution = new BooleanSetting(cronGroup,  "", "CronService.allowparallelexecution", false, "Enables the parallel execution of *the same* cron job. This can happen if the method runs longer than the defined cron interval. Since this could lead to problems, the default is false.");

	//security settings
	public static final Setting<String> SuperUserName                  = new StringSetting(securityGroup,     "Superuser",            "superuser.username",                    "superadmin", "Name of the superuser. If set to empty string, superuser access is prevented completely.");
	public static final Setting<String> SuperUserPassword              = new PasswordSetting(securityGroup,   "Superuser",            "superuser.password",                    null, "Password of the superuser").setIsProtected();
	public static final Setting<Integer> ResolutionDepth               = new IntegerSetting(applicationGroup, "Application Security", "application.security.resolution.depth", 5);
	public static final Setting<Boolean> XMLParserSecurity             = new BooleanSetting(applicationGroup, "Application Security", "application.xml.parser.security", true, "Enables various security measures for XML parsing to prevent exploits.");
	public static final Setting<Boolean> SsrfProtection               = new BooleanSetting(applicationGroup, "Application Security", "application.security.ssrf.protection", true, "Enables SSRF protection for outbound HTTP requests. When enabled, requests to private/internal IP ranges (loopback, link-local, site-local) are blocked. Disable only for testing or when internal network access is explicitly required.");

	public static final Setting<String> AuthenticationPropertyKeys      = new StringSetting(securityGroup,     "Authentication", "security.authentication.propertykeys", null, "List of property keys separated by space in the form of <Type>.<key> (example: 'Member.memberId') to be used in addition to the default 'Principal.name Principal.eMail'");

	public static final Setting<Boolean> InitialAdminUserCreate        = new BooleanSetting(securityGroup,    "Initial Admin User",   "initialuser.create",    true,    "Enables or disables the creation of an initial admin user when connecting to a database that has never been used with structr.");
	public static final Setting<String> InitialAdminUserName           = new StringSetting(securityGroup,     "Initial Admin User",   "initialuser.name",      "admin", "Name of the initial admin user. This will only be set if the user is created.");
	public static final Setting<String> InitialAdminUserPassword       = new PasswordSetting(securityGroup,   "Initial Admin User",   "initialuser.password",  "admin", "Password of the initial admin user. This will only be set if the user is created.");

	public static final Setting<Integer> TwoFactorLevel                = new IntegerChoiceSetting(securityGroup, "Two Factor Authentication", "security.twofactorauthentication.level",                1,             Settings.getTwoFactorSettingOptions());
	public static final Setting<String> TwoFactorIssuer                = new StringSetting(securityGroup,        "Two Factor Authentication", "security.twofactorauthentication.issuer",               "Structr",     "Must be URL-compliant in order to scan the created QR code");
	public static final Setting<String> TwoFactorAlgorithm             = new ChoiceSetting(securityGroup,        "Two Factor Authentication", "security.twofactorauthentication.algorithm",            "SHA1",        Settings.getStringsAsSet("SHA1", "SHA256", "SHA512"), "Respected by the most recent Google Authenticator implementations. <i>Warning: Changing this setting after users are already confirmed will effectively lock them out. Set [User].twoFactorConfirmed to false to show them a new QR code.</i>");
	public static final Setting<Integer> TwoFactorDigits               = new IntegerChoiceSetting(securityGroup, "Two Factor Authentication", "security.twofactorauthentication.digits",               6,             Settings.getTwoFactorDigitsOptions(), "Respected by the most recent Google Authenticator implementations. <i>Warning: Changing this setting after users are already confirmed may lock them out. Set [User].twoFactorConfirmed to false to show them a new QR code.</i>");
	public static final Setting<Integer> TwoFactorPeriod               = new IntegerSetting(securityGroup,       "Two Factor Authentication", "security.twofactorauthentication.period",               30,            "Defines the period that a TOTP code will be valid for, in seconds.<br>Respected by the most recent Google Authenticator implementations. <i>Warning: Changing this setting after users are already confirmed will effectively lock them out. Set [User].twoFactorConfirmed to false to show them a new QR code.</i>");
	public static final Setting<Integer> TwoFactorLoginTimeout         = new IntegerSetting(securityGroup,       "Two Factor Authentication", "security.twofactorauthentication.logintimeout",         30,            "Defines how long the two-factor login time window in seconds is. After entering the username and password the user has this amount of time to enter a two factor token before he has to re-authenticate via password");
	public static final Setting<String> TwoFactorLoginPage             = new StringSetting(securityGroup,        "Two Factor Authentication", "security.twofactorauthentication.loginpage",            "/twofactor",  "The application page where the user enters the current two factor token");
	public static final Setting<String> TwoFactorWhitelistedIPs        = new StringSetting(securityGroup,        "Two Factor Authentication", "security.twofactorauthentication.whitelistedips",       "",            "Comma-separated list of IPs for which two factor authentication is disabled. Both IPv4 and IPv6 are supported. CIDR notation is also supported. (e.g. 192.168.0.1/24 or 2A01:598:FF30:C500::/64)");

	public static final Setting<String> JWTSecretType                     = new ChoiceSetting(securityGroup, "JWT Auth",  "security.jwt.secrettype", "secret", Settings.getStringsAsSet("secret", "keypair", "jwks"), "Selects the secret type that will be used to sign or verify a given access or refresh token");
	public static final Setting<String> JWTSecret                         = new StringSetting(securityGroup, "JWT Auth",  "security.jwt.secret", "", "Used if 'security.jwt.secrettype'=secret. The secret that will be used to sign and verify all tokens issued and sent to Structr. Must have a min. length of 32 characters.").setIsProtected();
	public static final Setting<String> JWTIssuer                         = new StringSetting(securityGroup, "JWT Auth",  "security.jwt.jwtIssuer", "structr", "The issuer for the JWTs created by this Structr instance.");
	public static final Setting<String> JWTAudience                       = new StringSetting(securityGroup, "JWT Auth",  "security.jwt.audience", "", "Comma-separated list of audience values to bind into <code>aud</code> claims of JWTs issued by this instance. When non-empty, every new token carries these values in <code>aud</code> and verification rejects tokens whose audience does not intersect this list. When empty (default), no audience claim is emitted or verified. <b>Enabling this invalidates all existing access and refresh tokens</b> — plan a login flush.");
	public static final Setting<Integer> JWTExpirationTimeout             = new IntegerSetting(securityGroup, "JWT Auth",  "security.jwt.expirationtime", 60, "Access token timeout in minutes.");
	public static final Setting<Integer> JWTRefreshTokenExpirationTimeout = new IntegerSetting(securityGroup, "JWT Auth",  "security.jwt.refreshtoken.expirationtime", 1440,"Refresh token timeout in minutes.");
	public static final Setting<String> JWTKeyStore                       = new StringSetting(securityGroup, "JWT Auth",  "security.jwt.keystore", "", "Used if 'security.jwt.secrettype'=keypair. A valid keystore file containing a private/public keypair that can be used to sign and verify JWTs");
	public static final Setting<String> JWTKeyStorePassword               = new StringSetting(securityGroup, "JWT Auth",  "security.jwt.keystore.password", "","The password for the given 'security.jwt.keystore'");
	public static final Setting<String> JWTKeyAlias                       = new StringSetting(securityGroup, "JWT Auth",  "security.jwt.key.alias", "","The alias of the private key of the given 'security.jwt.keystore'");
	public static final Setting<String> JWKSProvider                      = new StringSetting(securityGroup, "JWT Auth",  "security.jwks.provider", "","URL of the JWKS provider");
	public static final Setting<String> JWKSGroupClaimKey                 = new StringSetting(securityGroup, "JWT Auth",  "security.jwks.group.claim.key", "","The name of the key in the JWKS response claims whose value(s) will be used to look for Group nodes with a matching jwksReferenceId.");
	public static final Setting<String> JWKSObjectIdClaimKey              = new StringSetting(securityGroup, "JWT Auth",  "security.jwks.id.claim.key", "oid","The name of the key in the JWKS response claims whose value will be used as the ID of the temporary principal object.");
	public static final Setting<String> JWKSObjectNameClaimKey            = new StringSetting(securityGroup, "JWT Auth",  "security.jwks.name.claim.key", "oid","The name of the key in the JWKS response claims whose value will be used as the name of the temporary principal object.");
	public static final Setting<String> JWKSAdminClaimKey                 = new StringSetting(securityGroup, "JWT Auth",  "security.jwks.admin.claim.key", "","The name of the key in the JWKS response claims in whose values is searched for a value matching the value of security.jwks.admin.claim.value.");
	public static final Setting<String> JWKSAdminClaimValue               = new StringSetting(securityGroup, "JWT Auth",  "security.jwks.admin.claim.value", "","The value that must be present in the JWKS response claims object with the key given in security.jwks.admin.claim.key in order to give the requesting user admin privileges.");

	public static final Setting<Boolean> PasswordForceChange                 = new BooleanSetting(securityGroup, "Password Policy", "security.passwordpolicy.forcechange",                         false, "Indicates if a forced password change is active");
	public static final Setting<Boolean> PasswordClearSessionsOnChange       = new BooleanSetting(securityGroup, "Password Policy", "security.passwordpolicy.onchange.clearsessions",              false, "Clear all sessions of a user on password change.");
	public static final Setting<Integer> PasswordForceChangeDays             = new IntegerSetting(securityGroup, "Password Policy", "security.passwordpolicy.maxage",                              90,    "The number of days after which a user has to change his password");
	public static final Setting<Integer> PasswordForceChangeReminder         = new IntegerSetting(securityGroup, "Password Policy", "security.passwordpolicy.remindtime",                          14,    "The number of days (before the user must change the password) where a warning should be issued. (Has to be handled in application code)");
	public static final Setting<Integer> PasswordAttempts                    = new IntegerSetting(securityGroup, "Password Policy", "security.passwordpolicy.maxfailedattempts",                   4,     "The maximum number of failed login attempts before a user is blocked. (Can be disabled by setting to zero or a negative number)");
	public static final Setting<Boolean> PasswordResetFailedCounterOnPWReset = new BooleanSetting(securityGroup, "Password Policy", "security.passwordpolicy.resetFailedAttemptsOnPasswordReset",  true,  "Configures if resetting the users password also resets the failed login attempts counter");

	public static final Setting<Boolean> PasswordComplexityEnforce                = new BooleanSetting(securityGroup, "Password Policy - Complexity", "security.passwordpolicy.complexity.enforce",                false, "Configures if password complexity is enforced for user passwords. If active, changes which violate the complexity rules, will result in an error and must be accounted for.");
	public static final Setting<Integer> PasswordComplexityMinLength              = new IntegerSetting(securityGroup, "Password Policy - Complexity", "security.passwordpolicy.complexity.minlength",              8,     "The minimum length for user passwords (only active if the enforce setting is active)");
	public static final Setting<Boolean> PasswordComplexityRequireUpperCase       = new BooleanSetting(securityGroup, "Password Policy - Complexity", "security.passwordpolicy.complexity.requireuppercase",       false, "Require at least one upper case character in user passwords (only active if the enforce setting is active)");
	public static final Setting<Boolean> PasswordComplexityRequireLowerCase       = new BooleanSetting(securityGroup, "Password Policy - Complexity", "security.passwordpolicy.complexity.requirelowercase",       false, "Require at least one lower case character in user passwords (only active if the enforce setting is active)");
	public static final Setting<Boolean> PasswordComplexityRequireDigit           = new BooleanSetting(securityGroup, "Password Policy - Complexity", "security.passwordpolicy.complexity.requiredigits",          false, "Require at least one digit in user passwords (only active if the enforce setting is active)");
	public static final Setting<Boolean> PasswordComplexityRequireNonAlphaNumeric = new BooleanSetting(securityGroup, "Password Policy - Complexity", "security.passwordpolicy.complexity.requirenonalphanumeric", false, "Require at least one non alpha-numeric character in user passwords (only active if the enforce setting is active)");

	public static final Setting<Boolean> SSHPublicKeyOnly                         = new BooleanSetting(securityGroup, "SSH", "application.ssh.forcepublickey",    true, "Force use of public key authentication for SSH connections");

	public static final Setting<String> RegistrationCustomUserClass               = new StringSetting(securityGroup,  "User self-registration", "registration.customuserclass",              "");
	public static final Setting<Boolean> RegistrationAllowLoginBeforeConfirmation = new BooleanSetting(securityGroup, "User self-registration", "registration.allowloginbeforeconfirmation", false, "Enables self-registered users to login without clicking the activation link in the registration email.");
	public static final Setting<String> RegistrationCustomAttributes              = new StringSetting(securityGroup,  "User self-registration", "registration.customuserattributes",         "name", "Attributes the registering user is allowed to provide. All other attributes are discarded. (eMail is always allowed)");
	public static final Setting<String> EmailRateLimitWhitelist                   = new StringSetting(securityGroup,  "User self-registration", "security.emailratelimit.whitelist",         "", "Comma-separated source IPs exempt from the registration and password-reset rate limits, e.g. 127.0.0.1,::1 during development. Leave empty in production: behind a reverse proxy without forwarded-for handling, all requests can appear to come from a whitelisted IP.");

	public static final Setting<Integer> ConfirmationKeyPasswordResetValidityPeriod = new IntegerSetting(securityGroup, "Confirmation Key Validity", "confirmationkey.passwordreset.validityperiod", 30,    "Validity period (in minutes) of the confirmation key generated when a user resets his password. Default is 30.");
	public static final Setting<Integer> ConfirmationKeyRegistrationValidityPeriod  = new IntegerSetting(securityGroup, "Confirmation Key Validity", "confirmationkey.registration.validityperiod",  2880,  "Validity period (in minutes) of the confirmation key generated during self registration. Default is 2 days (2880 minutes)");
	public static final Setting<Boolean> ConfirmationKeyValidWithoutTimestamp       = new BooleanSetting(securityGroup, "Confirmation Key Validity", "confirmationkey.validwithouttimestamp",        false, "How to interpret confirmation keys without a timestamp");

	public static final Setting<Integer> LetsEncryptWaitBeforeAuthorization         = new IntegerSetting(securityGroup,  "Letsencrypt", "letsencrypt.wait", 300, "Wait for this amount of seconds before trying to authorize challenge. Default is 300 seconds (5 minutes).");
	public static final Setting<String> LetsEncryptChallengeType                    = new ChoiceSetting(securityGroup,   "Letsencrypt", "letsencrypt.challenge.type", "http", Settings.getStringsAsSet("http", "dns"), "Challenge type for Let's Encrypt authorization. Possible values are 'http' and 'dns'.");
	public static final Setting<String> LetsEncryptDomains                          = new StringSetting(securityGroup,   "Letsencrypt", "letsencrypt.domains", "", "List of domains separated by space to fetch and update Let's Encrypt certificates for");
	public static final Setting<String> LetsEncryptProductionServerURL              = new StringSetting(securityGroup,   "Letsencrypt", "letsencrypt.production.server.url", "acme://letsencrypt.org", "URL of Let's Encrypt server. Default is 'acme://letsencrypt.org'");
	public static final Setting<String> LetsEncryptStagingServerURL                 = new StringSetting(securityGroup,   "Letsencrypt", "letsencrypt.staging.server.url", "acme://letsencrypt.org/staging", "URL of Let's Encrypt staging server for testing only. Default is 'acme://letsencrypt.org/staging'.");
	public static final Setting<String> LetsEncryptUserKeyFilename                  = new StringSetting(securityGroup,   "Letsencrypt", "letsencrypt.user.key.filename", "user.key", "File name of the Let's Encrypt user key. Default is 'user.key'.");
	public static final Setting<String> LetsEncryptDomainKeyFilename                = new StringSetting(securityGroup,   "Letsencrypt", "letsencrypt.domain.key.filename", "domain.key", "File name of the Let's Encrypt domain key. Default is 'domain.key'.");
	public static final Setting<String> LetsEncryptDomainCSRFileName                = new StringSetting(securityGroup,   "Letsencrypt", "letsencrypt.domain.csr.filename", "domain.csr", "File name of the Let's Encrypt CSR. Default is 'domain.csr'.");
	public static final Setting<String> LetsEncryptDomainChainFilename              = new StringSetting(securityGroup,   "Letsencrypt", "letsencrypt.domain.chain.filename", "domain-chain.crt", "File name of the Let's Encrypt domain chain. Default is 'domain-chain.crt'.");
	public static final Setting<Integer> LetsEncryptKeySize                         = new IntegerSetting(securityGroup,  "Letsencrypt", "letsencrypt.key.size", 2048, "Encryption key length. Default is 2048.");


	// OAuth General Settings
	public static final Setting<String> OAuthServers          = new StringSetting(oauthGroup, "General", "oauth.servers", "auth0 azure facebook github google linkedin keycloak", "List of available OAuth services separated by space. Defaults to a list of all available services.");
	public static final Setting<Boolean> OAuthVerboseLogging  = new BooleanSetting(oauthGroup, "General", "oauth.logging.verbose", false, "Optional. Enables verbose logging for OAuth login. Useful for debugging.");

	// GitHub OAuth Settings
	public static final Setting<String> OAuthGithubAuthLocation   = new StringSetting(oauthGroup, "GitHub", "oauth.github.authorization_location", "", "Optional. URL of the authorization endpoint. Uses default GitHub endpoint if not set.");
	public static final Setting<String> OAuthGithubTokenLocation  = new StringSetting(oauthGroup, "GitHub", "oauth.github.token_location", "", "Optional. URL of the token endpoint. Uses default GitHub endpoint if not set.");
	public static final Setting<String> OAuthGithubClientId       = new StringSetting(oauthGroup, "GitHub", "oauth.github.client_id", "", "Required. Client ID from your GitHub OAuth application.");
	public static final Setting<String> OAuthGithubClientSecret   = new StringSetting(oauthGroup, "GitHub", "oauth.github.client_secret", "", "Required. Client secret from your GitHub OAuth application.").setIsProtected();
	public static final Setting<String> OAuthGithubRedirectUri    = new StringSetting(oauthGroup, "GitHub", "oauth.github.redirect_uri", "/oauth/github/auth", "Optional. Structr endpoint for the OAuth authorization callback. Defaults to '/oauth/github/auth'.");
	public static final Setting<String> OAuthGithubUserDetailsUri = new StringSetting(oauthGroup, "GitHub", "oauth.github.user_details_resource_uri", "", "Optional. User details endpoint. Defaults to 'https://api.github.com/user'.");
	public static final Setting<String> OAuthGithubErrorUri       = new StringSetting(oauthGroup, "GitHub", "oauth.github.error_uri", "/error", "Optional. Redirect URI on unsuccessful authentication. Defaults to '/login'.");
	public static final Setting<String> OAuthGithubReturnUri      = new StringSetting(oauthGroup, "GitHub", "oauth.github.return_uri", "/", "Optional. Redirect URI on successful authentication. Defaults to '/'.");
	public static final Setting<String> OAuthGithubLogoutUri      = new StringSetting(oauthGroup, "GitHub", "oauth.github.logout_uri", "/logout", "Optional. Logout URI. Defaults to '/logout'.");
	public static final Setting<String> OAuthGithubScope          = new StringSetting(oauthGroup, "GitHub", "oauth.github.scope", "user:email", "Optional. OAuth scope. Defaults to 'user:email'.");

	// LinkedIn OAuth Settings
	public static final Setting<String> OAuthLinkedInAuthLocation   = new StringSetting(oauthGroup, "LinkedIn", "oauth.linkedin.authorization_location", "", "Optional. URL of the authorization endpoint. Uses default LinkedIn endpoint if not set.");
	public static final Setting<String> OAuthLinkedInTokenLocation  = new StringSetting(oauthGroup, "LinkedIn", "oauth.linkedin.token_location", "", "Optional. URL of the token endpoint. Uses default LinkedIn endpoint if not set.");
	public static final Setting<String> OAuthLinkedInClientId       = new StringSetting(oauthGroup, "LinkedIn", "oauth.linkedin.client_id", "", "Required. Client ID from your LinkedIn OAuth application.");
	public static final Setting<String> OAuthLinkedInClientSecret   = new StringSetting(oauthGroup, "LinkedIn", "oauth.linkedin.client_secret", "", "Required. Client secret from your LinkedIn OAuth application.").setIsProtected();
	public static final Setting<String> OAuthLinkedInRedirectUri    = new StringSetting(oauthGroup, "LinkedIn", "oauth.linkedin.redirect_uri", "/oauth/linkedin/auth", "Optional. Structr endpoint for the OAuth authorization callback. Defaults to '/oauth/linkedin/auth'.");
	public static final Setting<String> OAuthLinkedInUserDetailsUri = new StringSetting(oauthGroup, "LinkedIn", "oauth.linkedin.user_details_resource_uri", "", "Optional. User details endpoint. Defaults to 'https://api.linkedin.com/v2/userinfo'.");
	public static final Setting<String> OAuthLinkedInErrorUri       = new StringSetting(oauthGroup, "LinkedIn", "oauth.linkedin.error_uri", "/error", "Optional. Redirect URI on unsuccessful authentication. Defaults to '/login'.");
	public static final Setting<String> OAuthLinkedInReturnUri      = new StringSetting(oauthGroup, "LinkedIn", "oauth.linkedin.return_uri", "/", "Optional. Redirect URI on successful authentication. Defaults to '/'.");
	public static final Setting<String> OAuthLinkedInLogoutUri      = new StringSetting(oauthGroup, "LinkedIn", "oauth.linkedin.logout_uri", "/logout", "Optional. Logout URI. Defaults to '/logout'.");
	public static final Setting<String> OAuthLinkedInScope          = new StringSetting(oauthGroup, "LinkedIn", "oauth.linkedin.scope", "openid profile email", "Optional. OAuth scope. Defaults to 'openid profile email'.");

	// Google OAuth Settings
	public static final Setting<String> OAuthGoogleAuthLocation   = new StringSetting(oauthGroup, "Google", "oauth.google.authorization_location", "", "Optional. URL of the authorization endpoint. Uses default Google endpoint if not set.");
	public static final Setting<String> OAuthGoogleTokenLocation  = new StringSetting(oauthGroup, "Google", "oauth.google.token_location", "", "Optional. URL of the token endpoint. Uses default Google endpoint if not set.");
	public static final Setting<String> OAuthGoogleClientId       = new StringSetting(oauthGroup, "Google", "oauth.google.client_id", "", "Required. Client ID from your Google Cloud Console OAuth credentials.");
	public static final Setting<String> OAuthGoogleClientSecret   = new StringSetting(oauthGroup, "Google", "oauth.google.client_secret", "", "Required. Client secret from your Google Cloud Console OAuth credentials.").setIsProtected();
	public static final Setting<String> OAuthGoogleRedirectUri    = new StringSetting(oauthGroup, "Google", "oauth.google.redirect_uri", "/oauth/google/auth", "Optional. Structr endpoint for the OAuth authorization callback. Defaults to '/oauth/google/auth'.");
	public static final Setting<String> OAuthGoogleUserDetailsUri = new StringSetting(oauthGroup, "Google", "oauth.google.user_details_resource_uri", "", "Optional. User details endpoint. Defaults to 'https://www.googleapis.com/oauth2/v3/userinfo'.");
	public static final Setting<String> OAuthGoogleErrorUri       = new StringSetting(oauthGroup, "Google", "oauth.google.error_uri", "/error", "Optional. Redirect URI on unsuccessful authentication. Defaults to '/login'.");
	public static final Setting<String> OAuthGoogleReturnUri      = new StringSetting(oauthGroup, "Google", "oauth.google.return_uri", "/", "Optional. Redirect URI on successful authentication. Defaults to '/'.");
	public static final Setting<String> OAuthGoogleLogoutUri      = new StringSetting(oauthGroup, "Google", "oauth.google.logout_uri", "/logout", "Optional. Logout URI. Defaults to '/logout'.");
	public static final Setting<String> OAuthGoogleScope          = new StringSetting(oauthGroup, "Google", "oauth.google.scope", "email", "Optional. OAuth scope. Defaults to 'email'.");

	// Facebook OAuth Settings
	public static final Setting<String> OAuthFacebookAuthLocation   = new StringSetting(oauthGroup, "Facebook", "oauth.facebook.authorization_location", "", "Optional. URL of the authorization endpoint. Uses default Facebook endpoint if not set.");
	public static final Setting<String> OAuthFacebookTokenLocation  = new StringSetting(oauthGroup, "Facebook", "oauth.facebook.token_location", "", "Optional. URL of the token endpoint. Uses default Facebook endpoint if not set.");
	public static final Setting<String> OAuthFacebookClientId       = new StringSetting(oauthGroup, "Facebook", "oauth.facebook.client_id", "", "Required. App ID from your Facebook Developer application.");
	public static final Setting<String> OAuthFacebookClientSecret   = new StringSetting(oauthGroup, "Facebook", "oauth.facebook.client_secret", "", "Required. App secret from your Facebook Developer application.").setIsProtected();
	public static final Setting<String> OAuthFacebookRedirectUri    = new StringSetting(oauthGroup, "Facebook", "oauth.facebook.redirect_uri", "/oauth/facebook/auth", "Optional. Structr endpoint for the OAuth authorization callback. Defaults to '/oauth/facebook/auth'.");
	public static final Setting<String> OAuthFacebookUserDetailsUri = new StringSetting(oauthGroup, "Facebook", "oauth.facebook.user_details_resource_uri", "", "Optional. User details endpoint. Defaults to 'https://graph.facebook.com/me'.");
	public static final Setting<String> OAuthFacebookErrorUri       = new StringSetting(oauthGroup, "Facebook", "oauth.facebook.error_uri", "/error", "Optional. Redirect URI on unsuccessful authentication. Defaults to '/login'.");
	public static final Setting<String> OAuthFacebookReturnUri      = new StringSetting(oauthGroup, "Facebook", "oauth.facebook.return_uri", "/", "Optional. Redirect URI on successful authentication. Defaults to '/'.");
	public static final Setting<String> OAuthFacebookLogoutUri      = new StringSetting(oauthGroup, "Facebook", "oauth.facebook.logout_uri", "/logout", "Optional. Logout URI. Defaults to '/logout'.");
	public static final Setting<String> OAuthFacebookScope          = new StringSetting(oauthGroup, "Facebook", "oauth.facebook.scope", "email", "Optional. OAuth scope. Defaults to 'email'.");

	// Auth0 OAuth Settings
	public static final Setting<String> OAuthAuth0Tenant                = new StringSetting(oauthGroup, "Auth0", "oauth.auth0.tenant", "", "Required (recommended). Auth0 tenant domain (e.g., 'your-tenant.auth0.com'). When set, authorization_location and token_location are built automatically.");
	public static final Setting<String> OAuthAuth0AuthorizationPath     = new StringSetting(oauthGroup, "Auth0", "oauth.auth0.authorization_path", "/authorize", "Optional. Path to authorization endpoint. Only used with tenant setting. Defaults to '/authorize'.");
	public static final Setting<String> OAuthAuth0TokenPath             = new StringSetting(oauthGroup, "Auth0", "oauth.auth0.token_path", "/oauth/token", "Optional. Path to token endpoint. Only used with tenant setting. Defaults to '/oauth/token'.");
	public static final Setting<String> OAuthAuth0UserinfoPath          = new StringSetting(oauthGroup, "Auth0", "oauth.auth0.userinfo_path", "/userinfo", "Optional. Path to userinfo endpoint. Only used with tenant setting. Defaults to '/userinfo'.");
	public static final Setting<String> OAuthAuth0AuthLocation          = new StringSetting(oauthGroup, "Auth0", "oauth.auth0.authorization_location", "", "Required if tenant not set. Full URL of the authorization endpoint. Ignored if tenant is configured.");
	public static final Setting<String> OAuthAuth0TokenLocation         = new StringSetting(oauthGroup, "Auth0", "oauth.auth0.token_location", "", "Required if tenant not set. Full URL of the token endpoint. Ignored if tenant is configured.");
	public static final Setting<String> OAuthAuth0ClientId              = new StringSetting(oauthGroup, "Auth0", "oauth.auth0.client_id", "", "Required. Client ID from your Auth0 application.");
	public static final Setting<String> OAuthAuth0ClientSecret          = new StringSetting(oauthGroup, "Auth0", "oauth.auth0.client_secret", "", "Required. Client secret from your Auth0 application.").setIsProtected();
	public static final Setting<String> OAuthAuth0RedirectUri           = new StringSetting(oauthGroup, "Auth0", "oauth.auth0.redirect_uri", "/oauth/auth0/auth", "Optional. Structr endpoint for the OAuth authorization callback. Defaults to '/oauth/auth0/auth'.");
	public static final Setting<String> OAuthAuth0UserDetailsUri        = new StringSetting(oauthGroup, "Auth0", "oauth.auth0.user_details_resource_uri", "", "Optional. User details endpoint. Built from tenant if not set.");
	public static final Setting<String> OAuthAuth0ErrorUri              = new StringSetting(oauthGroup, "Auth0", "oauth.auth0.error_uri", "/error", "Optional. Redirect URI on unsuccessful authentication. Defaults to '/login'.");
	public static final Setting<String> OAuthAuth0ReturnUri             = new StringSetting(oauthGroup, "Auth0", "oauth.auth0.return_uri", "/", "Optional. Redirect URI on successful authentication. Defaults to '/'.");
	public static final Setting<String> OAuthAuth0LogoutUri             = new StringSetting(oauthGroup, "Auth0", "oauth.auth0.logout_uri", "/logout", "Optional. Logout URI. Defaults to '/logout'.");
	public static final Setting<String> OAuthAuth0Scope                 = new StringSetting(oauthGroup, "Auth0", "oauth.auth0.scope", "openid profile email", "Optional. OAuth scope. Defaults to 'openid profile email'.");
	public static final Setting<String> OAuthAuth0Audience              = new StringSetting(oauthGroup, "Auth0", "oauth.auth0.audience", "", "Optional. The API audience (identifier) of your Auth0 API. Required for API access tokens.");

	// Azure Active Directory OAuth Settings
	public static final Setting<String> OAuthAzureTenantId              = new StringSetting(oauthGroup, "Azure", "oauth.azure.tenant_id", "common", "Required. Azure AD tenant ID, or 'common' for multi-tenant apps, or 'organizations' for work accounts only.");
	public static final Setting<String> OAuthAzureAuthLocation          = new StringSetting(oauthGroup, "Azure", "oauth.azure.authorization_location", "", "Optional. URL of the authorization endpoint. Built automatically from tenant_id if not set.");
	public static final Setting<String> OAuthAzureTokenLocation         = new StringSetting(oauthGroup, "Azure", "oauth.azure.token_location", "", "Optional. URL of the token endpoint. Built automatically from tenant_id if not set.");
	public static final Setting<String> OAuthAzureClientId              = new StringSetting(oauthGroup, "Azure", "oauth.azure.client_id", "", "Required. Application (client) ID from Azure AD app registration.");
	public static final Setting<String> OAuthAzureClientSecret          = new StringSetting(oauthGroup, "Azure", "oauth.azure.client_secret", "", "Required. Client secret from Azure AD app registration.").setIsProtected();
	public static final Setting<String> OAuthAzureRedirectUri           = new StringSetting(oauthGroup, "Azure", "oauth.azure.redirect_uri", "/oauth/azure/auth", "Optional. Structr endpoint for the OAuth authorization callback. Defaults to '/oauth/azure/auth'.");
	public static final Setting<String> OAuthAzureUserDetailsUri        = new StringSetting(oauthGroup, "Azure", "oauth.azure.user_details_resource_uri", "", "Optional. User details endpoint. Defaults to 'https://graph.microsoft.com/v1.0/me'.");
	public static final Setting<String> OAuthAzureErrorUri              = new StringSetting(oauthGroup, "Azure", "oauth.azure.error_uri", "/error", "Optional. Redirect URI on unsuccessful authentication. Defaults to '/login'.");
	public static final Setting<String> OAuthAzureReturnUri             = new StringSetting(oauthGroup, "Azure", "oauth.azure.return_uri", "/", "Optional. Redirect URI on successful authentication. Defaults to '/'.");
	public static final Setting<String> OAuthAzureLogoutUri             = new StringSetting(oauthGroup, "Azure", "oauth.azure.logout_uri", "/logout", "Optional. Logout URI. Defaults to '/logout'.");
	public static final Setting<String> OAuthAzureScope                 = new StringSetting(oauthGroup, "Azure", "oauth.azure.scope", "openid profile email", "Optional. OAuth scope. Defaults to 'openid profile email'.");

	// Keycloak OAuth Settings
	public static final Setting<String> OAuthKeycloakServerUrl          = new StringSetting(oauthGroup, "Keycloak", "oauth.keycloak.server_url", "", "Required. Keycloak server URL (e.g., 'https://keycloak.example.com').");
	public static final Setting<String> OAuthKeycloakRealm              = new StringSetting(oauthGroup, "Keycloak", "oauth.keycloak.realm", "master", "Required. Keycloak realm name. Defaults to 'master'.");
	public static final Setting<String> OAuthKeycloakAuthLocation       = new StringSetting(oauthGroup, "Keycloak", "oauth.keycloak.authorization_location", "", "Optional. URL of the authorization endpoint. Built automatically from server_url and realm if not set.");
	public static final Setting<String> OAuthKeycloakTokenLocation      = new StringSetting(oauthGroup, "Keycloak", "oauth.keycloak.token_location", "", "Optional. URL of the token endpoint. Built automatically from server_url and realm if not set.");
	public static final Setting<String> OAuthKeycloakClientId           = new StringSetting(oauthGroup, "Keycloak", "oauth.keycloak.client_id", "", "Required. Client ID from your Keycloak client configuration.");
	public static final Setting<String> OAuthKeycloakClientSecret       = new StringSetting(oauthGroup, "Keycloak", "oauth.keycloak.client_secret", "", "Required. Client secret from your Keycloak client configuration.").setIsProtected();
	public static final Setting<String> OAuthKeycloakRedirectUri        = new StringSetting(oauthGroup, "Keycloak", "oauth.keycloak.redirect_uri", "/oauth/keycloak/auth", "Optional. Structr endpoint for the OAuth authorization callback. Defaults to '/oauth/keycloak/auth'.");
	public static final Setting<String> OAuthKeycloakUserDetailsUri     = new StringSetting(oauthGroup, "Keycloak", "oauth.keycloak.user_details_resource_uri", "", "Optional. User details endpoint. Built automatically from server_url and realm if not set.");
	public static final Setting<String> OAuthKeycloakErrorUri           = new StringSetting(oauthGroup, "Keycloak", "oauth.keycloak.error_uri", "/error", "Optional. Redirect URI on unsuccessful authentication. Defaults to '/login'.");
	public static final Setting<String> OAuthKeycloakReturnUri          = new StringSetting(oauthGroup, "Keycloak", "oauth.keycloak.return_uri", "/", "Optional. Redirect URI on successful authentication. Defaults to '/'.");
	public static final Setting<String> OAuthKeycloakLogoutUri          = new StringSetting(oauthGroup, "Keycloak", "oauth.keycloak.logout_uri", "/logout", "Optional. Logout URI. Defaults to '/logout'.");
	public static final Setting<String> OAuthKeycloakScope              = new StringSetting(oauthGroup, "Keycloak", "oauth.keycloak.scope", "openid profile email", "Optional. OAuth scope. Defaults to 'openid profile email'.");

	// licence settings
	public static final Setting<String> LicenseKey                = new StringSetting(licensingGroup,   "Licensing", "license.key",                   "", "Base64-encoded string that contains the complete license data, typically saved as 'license.key' in the main directory.");
	public static final Setting<Integer> LicenseValidationTimeout = new IntegerSetting(licensingGroup,  "Licensing", "license.validation.timeout",    10, "Timeout in seconds for license validation requests.");
	public static final Setting<Boolean> LicenseAllowFallback     = new BooleanSetting(licensingGroup,  "Licensing", "license.allow.fallback",      true, "Allow Structr to fall back to the Community License if no valid license exists (or license cannot be validated). Set this to false in production environments to prevent Structr from starting without a license.");

	public static Collection<SettingsGroup> getGroups() {
		return groups.values();
	}

	public static SettingsGroup getGroup(final String key) {
		return groups.get(key);
	}

	public static Collection<Setting> getSettings() {
		return settings.values();
	}

	public static <T> Setting<T> getSetting(final String... keys) {
		return settings.get(StringUtils.join(toLowerCase(keys), "."));
	}

	public static <T> Setting<T> getCaseSensitiveSetting(final String... keys) {
		return settings.get(StringUtils.join(keys, "."));
	}

	public static Setting<String> getStringSetting(final String... keys) {

		final String key        = StringUtils.join(toLowerCase(keys), ".");
		Setting<String> setting = settings.get(key);

		return setting;
	}

	public static Setting<String> getOrCreateStringSetting(final String... keys) {

		final String key        = StringUtils.join(toLowerCase(keys), ".");
		Setting<String> setting = settings.get(key);

		if (setting == null) {

			setting = new StringSetting(miscGroup, key, null);
		}

		return setting;
	}

	public static Setting<Integer> getIntegerSetting(final String... keys) {

		final String key        = StringUtils.join(toLowerCase(keys), ".");
		Setting<Integer> setting = settings.get(key);

		return setting;
	}

	public static Setting<Integer> getOrCreateIntegerSetting(final String... keys) {

		final String key        = StringUtils.join(toLowerCase(keys), ".");
		Setting<Integer> setting = settings.get(key);

		if (setting == null) {

			setting = new IntegerSetting(miscGroup, key, null);
		}

		return setting;
	}

	public static Setting<Boolean> getBooleanSetting(final String... keys) {

		final String key         = StringUtils.join(toLowerCase(keys), ".");
		Setting<Boolean> setting = settings.get(key);

		return setting;
	}

	public static Setting<Boolean> getOrCreateBooleanSetting(final String... keys) {

		final String key         = StringUtils.join(toLowerCase(keys), ".");
		Setting<Boolean> setting = settings.get(key);

		if (setting == null) {

			setting = new BooleanSetting(miscGroup, key, null);
		}

		return setting;
	}

	public static Setting<?> createSettingForValue(final SettingsGroup group, final String key, final String value) {
		return createSettingForValue(group, key, value, false);
	}

	public static Setting<?> createSettingForValue(final SettingsGroup group, final String key, final String value, final boolean forceString) {

		if (value != null && !forceString) {

			// try to determine property value type, string, integer or boolean?
			final String lowerCaseValue = value.toLowerCase();

			// boolean
			if ("true".equals(lowerCaseValue) || "false".equals(lowerCaseValue)) {

				final Setting<Boolean> setting = new BooleanSetting(group, key);
				setting.setIsDynamic(true);
				setting.updateKey(key);
				setting.setValue(Boolean.parseBoolean(value));

				return setting;
			}

			// integer
			if (Settings.isNumeric(value)) {

				final Setting<Integer> setting = new IntegerSetting(group, key);
				setting.setIsDynamic(true);
				setting.updateKey(key);
				setting.setValue(Integer.parseInt(value));

				return setting;
			}
		}

		final Setting<String> setting = new StringSetting(group, key);
		setting.setIsDynamic(true);
		setting.updateKey(key);
		setting.setValue(value);

		return setting;
	}

	public static void storeConfiguration(final String fileName) throws IOException {

		storeConfiguration(fileName, true);
	}

	public static void storeConfiguration(final String fileName, final boolean warnForNotRecommendedPermissions) throws IOException {

		try {

			final FileBasedConfigurationBuilder<PropertiesConfiguration> builder = getDefaultPropertiesConfigurationBuilder(fileName);

			// If file does not exist, create it and set default permissions
			final Path filePath     = Path.of(fileName);
			final boolean didCreate = filePath.toFile().createNewFile();
			if (didCreate) {

				try {

					Files.setPosixFilePermissions(filePath, expectedConfigFilePermissions);

				} catch (UnsupportedOperationException | IOException e) {
					// happens on non-POSIX filesystems, ignore
				}
			}

			final PropertiesConfiguration config = builder.getConfiguration();

			// clear data loaded from disk, so we can delete keys
			config.clear();

			for (final Setting setting : settings.values()) {

				// store only modified/dynamic settings and the superuser password
				if (setting.isModified() || setting.isDynamic() || "superuser.password".equals(setting.getKey())) {

					config.setProperty(setting.getKey(), setting.getValue());

					setting.setIsModified(false);
				}
			}

			final FileHandler fileHandler = builder.getFileHandler();
			final long freeSpace          = fileHandler.getFile().getFreeSpace();

			if (freeSpace < 1024 * 1024) {
				logger.error("Refusing to start with less than 1 MB of disk space.");
				System.exit(1);
			}

			builder.save();

			checkConfigurationFilePermissions(builder, warnForNotRecommendedPermissions);

		} catch (ConfigurationException ex) {

            logger.error("Unable to store configuration: {}", ex.getMessage());
		}
	}

	public static FileBasedConfigurationBuilder<PropertiesConfiguration> getDefaultPropertiesConfigurationBuilder() {

        return getDefaultPropertiesConfigurationBuilder(Settings.ConfigFileName);
	}

	private static FileBasedConfigurationBuilder<PropertiesConfiguration> getDefaultPropertiesConfigurationBuilder(final String fileName) {

		return new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
				   .configure(new Parameters().fileBased()
							  .setFileName(fileName)
							  .setThrowExceptionOnMissing(true)
							  .setListDelimiterHandler(new DefaultListDelimiterHandler('\0'))
				   );
	}

	public static String getExpectedConfigurationFilePermissionsAsString () {

		return PosixFilePermissions.toString(expectedConfigFilePermissions);
	}

	public static String getActualConfigurationFilePermissionsAsString (final FileBasedConfigurationBuilder<?> builder) {

		try {

			final Set<PosixFilePermission> actualPermissions = getActualConfigurationFilePermissions(builder);

			return PosixFilePermissions.toString(actualPermissions);

		} catch (UnsupportedOperationException | IOException e) {
			// happens on non-POSIX filesystems, ignore
		}

		return "";
	}

	private static Set<PosixFilePermission> getActualConfigurationFilePermissions (final FileBasedConfigurationBuilder<?> builder) throws UnsupportedOperationException, IOException{

		if (builder != null) {

			final FileHandler fileHandler = builder.getFileHandler();
			final String pathString = fileHandler.getPath();
			if (pathString != null) {
				return Files.getPosixFilePermissions(Path.of(pathString));
			}
		}

		return null;
	}

	public static boolean checkConfigurationFilePermissions(final FileBasedConfigurationBuilder<?> builder, final boolean warn) {

		// default to true for non-POSIX filesystems
		boolean isOk = true;

		try {

			final Set<PosixFilePermission> actualPermissions = getActualConfigurationFilePermissions(builder);

			isOk = actualPermissions != null && actualPermissions.equals(Settings.expectedConfigFilePermissions);

			if (!isOk && warn) {

				logger.warn("Permissions for configuration file '{}' do not match the expected permissions (Actual: {}, Expected: {}). Please check if this should be the case and otherwise fix the permissions", builder.getFileHandler().getFileName(), PosixFilePermissions.toString(actualPermissions), PosixFilePermissions.toString(expectedConfigFilePermissions));
			}

		} catch (UnsupportedOperationException | IOException e) {
			// happens on non-POSIX filesystems, ignore
		}

		return isOk;
	}

	public static void loadConfiguration(final String fileName) {

		try {

			final FileBasedConfigurationBuilder<PropertiesConfiguration> builder = getDefaultPropertiesConfigurationBuilder(fileName);

			final PropertiesConfiguration config = builder.getConfiguration();
			final Iterator<String> keys          = config.getKeys();

			Settings.checkConfigurationFilePermissions(builder, true);

			while (keys.hasNext()) {

				final String key   = keys.next();
				final String lcKey = key.toLowerCase();
				final String value = config.getString(key);
				Setting<?> setting = Settings.getSetting(lcKey);

				if (setting != null && setting.isDynamic()) {

					// unregister dynamic settings so the type can change (and cronExpressions are put in correct group)
					setting.unregister();
					setting = null;
				}

				if (setting != null) {

					setting.fromString(value);

				} else {

					// unknown setting => dynamic

					SettingsGroup targetGroup = miscGroup;

					// put key in cron group if it contains ".cronExpression"
					if (key.contains(".cronExpression")) {
						targetGroup = cronGroup;
					}

					// create new StringSetting for unknown key
					Settings.createSettingForValue(targetGroup, key, value, key.contains(Settings.ConnectionPassword.getKey()));
				}
			}

			Settings.initializeValidUUIDPatternOnce();

		} catch (ConfigurationException ex) {

			logger.error("Unable to load configuration: " + ex.getMessage());
		}
	}

	public static <T>Setting<T> getSettingOrMaintenanceSetting(final Setting<T> setting) {

		return MaintenanceModeEnabled.getValue() ? setting.getPrefixedSetting(Settings.MAINTENANCE_PREFIX) : setting;
	}

	public static String getBasePath() {

		return checkPath(BasePath.getValue());
	}

	public static String getFullSettingPath(Setting<String> pathSetting) {

		return getBasePath() + checkPath(pathSetting.getValue());

	}

	private static String checkPath(final String path) {

		if (path.endsWith("/")) {
			return path;
		}

		return path + "/";
	}

	private static String[] toLowerCase(final String... input) {

		final ArrayList<String> lower = new ArrayList(input.length);

		for (final String i : input) {

			lower.add(i.toLowerCase());
		}

		return lower.toArray(new String[0]);
	}

	// ----- package methods -----
	static void registerGroup(final SettingsGroup group) {
		groups.put(group.getKey(), group);
	}

	static void registerSetting(final Setting setting) {

		final Setting oldSetting = settings.get(setting.getKey());

		if (oldSetting != null) {
			setting.setValue(oldSetting.getValue());
			oldSetting.unregister();
		}

		settings.put(setting.getKey(), setting);
	}

	static void unregisterSetting(final Setting setting) {
		settings.remove(setting.getKey());
	}

	public static Set<String> getStringsAsSet(final String... choices) {
		return new LinkedHashSet<>(Arrays.asList(choices));
	}

	public static Map<Integer, String> getTwoFactorSettingOptions() {
		final Map<Integer, String> options = new LinkedHashMap();
		options.put(0, "off");
		options.put(1, "optional");
		options.put(2, "forced");
		return options;
	}

	public static Map<Integer, String> getTwoFactorDigitsOptions() {
		final Map<Integer, String> options = new LinkedHashMap();
		options.put(6, "6 Digits");
		options.put(8, "8 Digits");
		return options;
	}

	public static Map<String, String> getAllowedUUIDv4FormatOptions() {

		final Map<String, String> options = new LinkedHashMap();
		options.put(POSSIBLE_UUID_V4_FORMATS.without_dashes.toString(), "Without Dashes");
		options.put(POSSIBLE_UUID_V4_FORMATS.with_dashes.toString(), "With Dashes");
		options.put(POSSIBLE_UUID_V4_FORMATS.both.toString(), "Both (Read warning!)");
		return options;
	}

	public static Map<String, String> getAvailableLogLevels() {

		final Map<String, String> options = new LinkedHashMap();
		options.put("ALL", "ALL");
		options.put("TRACE", "TRACE");
		options.put("DEBUG", "DEBUG");
		options.put("INFO", "INFO");
		options.put("WARN", "WARN");
		options.put("ERROR", "ERROR");
		return options;
	}


	public enum SCRIPT_PROCESS_LOG_STYLE {
		NOTHING(0), SCRIPT_PATH(1), CUSTOM(2);

		SCRIPT_PROCESS_LOG_STYLE(int l) {}

		public static SCRIPT_PROCESS_LOG_STYLE get(int i) {

			switch (i) {
				case 0: return NOTHING;
				case 1: return SCRIPT_PATH;
				default: return CUSTOM;
			}
		}
	}

	public static Map<Integer, String> getScriptProcessLogCommandLineOptions() {
		final Map<Integer, String> options = new LinkedHashMap();
		options.put(0, "0 - Do not log command line");
		options.put(1, "1 - Log full path to script without parameters");
		options.put(2, "2 - Log full path to script and parameters as configured");
		return options;
	}

	public static boolean isNumeric(final String source) {

		try {

			final Integer value = Integer.parseInt(source);
			if (value.toString().equals(source)) {

				// value is not changed by parsing and toString()
				return true;
			}

		} catch (Throwable t) {}

		return false;
	}

	public static String getValidUUIDRegexString() {

		if (Settings.uuidPattern == null) {
			initializeValidUUIDPatternOnce();
		}

		return Settings.uuidOnlyRegex;
	}

	public static String getValidUUIDRegexStringForURLParts() {

		if (Settings.uuidPattern == null) {
			initializeValidUUIDPatternOnce();
		}

		return Settings.uuidPartRegex;
	}

	private static void initializeValidUUIDPatternOnce() {

		if (Settings.uuidPattern != null && Settings.uuidOnlyRegex != null) {
			// prevent update
			return;
		}

		switch (Settings.UUIDv4AllowedFormats.getValue()) {
			case "with_dashes":
				Settings.uuidOnlyRegex = "^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}$";
				Settings.uuidPartRegex = "[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}";
				break;

			case "both":
				Settings.uuidOnlyRegex = "^[a-fA-F0-9]{32}$|^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}$";
				Settings.uuidPartRegex = "[a-fA-F0-9]{32}|[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}";
				break;

			default:
			case "without_dashes":
				Settings.uuidOnlyRegex = "^[a-fA-F0-9]{32}$";
				Settings.uuidPartRegex = "[a-fA-F0-9]{32}";
				break;
		}

		Settings.uuidPattern = Pattern.compile(Settings.uuidOnlyRegex);
	}

	public static boolean isValidUuid(final String id) {

		// make sure the UUID pattern is always initialized
		if (Settings.uuidPattern == null) {
			initializeValidUUIDPatternOnce();
		}

		if (id != null) {

			if (Settings.uuidPattern.matcher(id).matches()) {
				return true;
			}
		}

		return false;
	}

	public static boolean isValidEmail(final String email) {

		if (email == null) {
			return false;
		}

		return Settings.emailValidationPattern.matcher(email).matches();
	}

	public static void updateEmailValidationPattern() {
		emailValidationPattern = Pattern.compile(Settings.EmailValidationRegex.getValue());
	}
}
