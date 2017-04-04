/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * The Structr configuration settings.
 */
public class Settings {

	private static final Map<String, Setting> settings        = new LinkedHashMap<>();
	private static final Map<String, SettingsGroup> groups    = new LinkedHashMap<>();

	public static final SettingsGroup generalGroup            = new SettingsGroup("general",     "General Settings");
	public static final SettingsGroup serverGroup             = new SettingsGroup("server",      "Server Settings");
	public static final SettingsGroup databaseGroup           = new SettingsGroup("database",    "Database Configuration");
	public static final SettingsGroup applicationGroup        = new SettingsGroup("application", "Application Configuration");
	public static final SettingsGroup smtpGroup               = new SettingsGroup("smtp",        "Mail Configuration");
	public static final SettingsGroup advancedGroup           = new SettingsGroup("advanced",    "Advanced Settings");
	public static final SettingsGroup servletsGroup           = new SettingsGroup("servlets",    "Servlets");
	public static final SettingsGroup cronGroup               = new SettingsGroup("cron",        "Cron Jobs");
	public static final SettingsGroup oauthGroup              = new SettingsGroup("oauth",       "OAuth Settings");
	public static final SettingsGroup miscGroup               = new SettingsGroup("misc",        "Miscellaneous");

	// general settings
	public static final Setting<String> ApplicationTitle      = new StringSetting(generalGroup,  "application.title",      "Structr 2.1");
	public static final Setting<String> Configuration         = new StringSetting(generalGroup,  "configuration.provider", "org.structr.module.JarConfigurationProvider");
	public static final Setting<String> BasePath              = new StringSetting(generalGroup,  "base.path",              "");
	public static final Setting<String> TmpPath               = new StringSetting(generalGroup,  "tmp.path",               "/tmp");
	public static final Setting<String> FilesPath             = new StringSetting(generalGroup,  "files.path",             System.getProperty("user.dir").concat("/files"));
	public static final Setting<String> LogDatabasePath       = new StringSetting(generalGroup,  "log.database.path",      System.getProperty("user.dir").concat("/logDb.dat"));
	public static final Setting<String> DataExchangePath      = new StringSetting(generalGroup,  "data.exchange.path",     "");
	public static final Setting<String> SnapshotsPath         = new StringSetting(generalGroup,  "snapshot.path",          "snapshots/");
	public static final Setting<Boolean> LogSchemaOutput      = new BooleanSetting(generalGroup, "NodeExtender.log",       false);
	public static final Setting<Boolean> RequestLogging       = new BooleanSetting(generalGroup, "log.requests",           false);
	public static final Setting<String> LogPrefix             = new StringSetting(generalGroup,  "log.prefix",             "structr");
	public static final Setting<String> SuperUserName         = new StringSetting(generalGroup,  "superuser.username",     "superadmin");
	public static final Setting<String> SuperUserPassword     = new StringSetting(generalGroup,  "superuser.password",     RandomStringUtils.randomAlphanumeric(12));

	public static final Setting<Boolean> Testing              = new BooleanSetting(generalGroup, "testing",                false);
	public static final StringSetting Services                = new StringSetting(generalGroup, "configured.services",     "NodeService AgentService CronService SchemaService LogService HttpService FtpService SSHService");

	// server settings
	public static final Setting<String> ApplicationHost       = new StringSetting(serverGroup,  "application.host",              "0.0.0.0");
	public static final Setting<Integer> HttpPort             = new IntegerSetting(serverGroup, "application.http.port",         8082);
	public static final Setting<Integer> HttpsPort            = new IntegerSetting(serverGroup, "application.https.port",        8083);
	public static final Setting<Integer> SshPort              = new IntegerSetting(serverGroup, "application.ssh.port",          8022);
	public static final Setting<Integer> FtpPort              = new IntegerSetting(serverGroup, "application.ftp.port",          8021);
	public static final Setting<Boolean> HttpsEnabled         = new BooleanSetting(serverGroup, "application.https.enabled",     false);
	public static final Setting<String> KeystorePath          = new StringSetting(serverGroup,  "application.keystore.path",     "");
	public static final Setting<String> KeystorePassword      = new StringSetting(serverGroup,  "application.keystore.password", "");

	// HTTP service settings
	public static final Setting<String> ResourceHandlers      = new StringSetting(serverGroup,  "HttpService.resourceHandlers",    "StructrUiHandler");
	public static final Setting<String> LifecycleListeners    = new StringSetting(serverGroup,  "HttpService.lifecycle.listeners", "");
	public static final Setting<Boolean> GzipCompression      = new BooleanSetting(serverGroup, "HttpService.gzip.enabled",        true);
	public static final Setting<Boolean> Async                = new BooleanSetting(serverGroup, "HttpService.async",               true);
	public static final Setting<Boolean> JsonIndentation      = new BooleanSetting(serverGroup, "json.indentation",                true);
	public static final Setting<Boolean> HtmlIndentation      = new BooleanSetting(serverGroup, "html.indentation",                true);
	public static final Setting<Boolean> WsIndentation        = new BooleanSetting(serverGroup, "ws.indentation",                  true);

	public static final Setting<String> AccessControlMaxAge           = new StringSetting(serverGroup, "access.control.max.age",            "3600");
	public static final Setting<String> AccessControlAllowMethods     = new StringSetting(serverGroup,  "access.control.allow.methods",     "");
	public static final Setting<String> AccessControlAllowHeaders     = new StringSetting(serverGroup,  "access.control.allow.headers",     "");
	public static final Setting<String> AccessControlAllowCredentials = new StringSetting(serverGroup,  "access.control.allow.credentials", "");
	public static final Setting<String> AccessControlExposeHeaders    = new StringSetting(serverGroup,  "access.control.expose.headers",    "");

	public static final Setting<String> UiHandlerContextPath        = new StringSetting(serverGroup,  "StructrUiHandler.contextPath",        "/structr");
	public static final Setting<Boolean> UiHandlerDirectoriesListed = new BooleanSetting(serverGroup,  "StructrUiHandler.directoriesListed", false);
	public static final Setting<String> UiHandlerResourceBase       = new StringSetting(serverGroup,  "StructrUiHandler.resourceBase",       "src/main/resources/structr");
	public static final Setting<String> UiHandlerWelcomeFiles       = new StringSetting(serverGroup,  "StructrUiHandler.welcomeFiles",       "index.html");

	// database settings
	public static final Setting<String> DatabasePath           = new StringSetting(databaseGroup,  "database.path",                    "db");
	public static final Setting<String> DatabaseDriver         = new StringSetting(databaseGroup,  "database.driver",                  "org.structr.bolt.BoltDatabaseService");
	public static final Setting<String> DatabaseDriverMode     = new StringSetting(databaseGroup,  "database.driver.mode",             "embedded");
	public static final Setting<String> ConnectionUrl          = new StringSetting(databaseGroup,  "database.connection.url",          "bolt://localhost:7688");
	public static final Setting<String> TestingConnectionUrl   = new StringSetting(databaseGroup,  "testing.connection.url",           "bolt://localhost:7689");
	public static final Setting<String> ConnectionUser         = new StringSetting(databaseGroup,  "database.connection.username",     "neo4j");
	public static final Setting<String> ConnectionPassword     = new StringSetting(databaseGroup,  "database.connection.password",     "neo4j");
	public static final Setting<Integer> RelationshipCacheSize = new IntegerSetting(databaseGroup, "database.cache.relationship.size", 100000);
	public static final Setting<Integer> NodeCacheSize         = new IntegerSetting(databaseGroup, "database.cache.node.size",         100000);
	public static final Setting<Integer> UuidCacheSize         = new IntegerSetting(databaseGroup, "database.cache.uuid.size",         100000);
	public static final Setting<Integer> QueryCacheSize        = new IntegerSetting(databaseGroup, "database.cache.query.size",        1000);
	public static final Setting<Boolean> CypherDebugLogging    = new BooleanSetting(databaseGroup, "log.cypher.debug",                 false);

	// application settings
	public static final Setting<String> InstanceName          = new StringSetting(applicationGroup,   "application.instance.name",                 "");
	public static final Setting<String> InstanceStage         = new StringSetting(applicationGroup,   "application.instance.stage",                "");
	public static final Setting<Integer> SessionTimeout       = new IntegerSetting(applicationGroup,  "application.session.timeout",               1800);
	public static final Setting<Integer> ResolutionDepth      = new IntegerSetting(applicationGroup,  "application.security.resolution.depth",     5);
	public static final Setting<String> OwnerlessNodes        = new StringSetting(applicationGroup,   "application.security.ownerless.nodes",      "read");
	public static final Setting<Boolean> ChangelogEnabled     = new BooleanSetting(applicationGroup,  "application.changelog.enabled",             false);
	public static final Setting<Boolean> FilesystemEnabled    = new BooleanSetting(applicationGroup,  "application.filesystem.enabled",            false);
	public static final Setting<Boolean> UniquePaths          = new BooleanSetting(applicationGroup,  "application.filesystem.unique.paths",       false);
	public static final Setting<Integer> IndexingLimit        = new IntegerSetting(applicationGroup,  "application.filesystem.indexing.limit",     50000);
	public static final Setting<Integer> IndexingMinLength    = new IntegerSetting(applicationGroup,  "application.filesystem.indexing.minlength", 4);
	public static final Setting<Integer> IndexingMaxLength    = new IntegerSetting(applicationGroup,  "application.filesystem.indexing.maxlength", 40);
	public static final Setting<String> DefaultUploadFolder   = new StringSetting(applicationGroup,   "application.uploads.folder",                null);
	public static final Setting<String> HttpProxyUrl          = new StringSetting(applicationGroup,   "application.proxy.http.url",                null);
	public static final Setting<String> HttpProxyUser         = new StringSetting(applicationGroup,   "application.proxy.http.username",           null);
	public static final Setting<String> HttpProxyPassword     = new StringSetting(applicationGroup,   "application.proxy.http.password",           null);

	// mail settings
	public static final Setting<String> SmtpHost              = new StringSetting(smtpGroup,  "smtp.host",         "localhost");
	public static final Setting<Integer> SmtpPort             = new IntegerSetting(smtpGroup, "smtp.port",         25);
	public static final Setting<String> SmtpUser              = new StringSetting(smtpGroup,  "smtp.user",         "");
	public static final Setting<String> SmtpPassword          = new StringSetting(smtpGroup,  "smtp.password",     "");
	public static final Setting<Boolean> SmtpTlsEnabled       = new BooleanSetting(smtpGroup, "smtp.tls.enabled",  true);
	public static final Setting<Boolean> SmtpTlsRequired      = new BooleanSetting(smtpGroup, "smtp.tls.required", true);

	// advanced settings
	public static final Setting<String> ForeignTypeName          = new StringSetting(advancedGroup,  "foreign.type.key",         null);
	public static final Setting<Boolean> JsonRedundancyReduction = new BooleanSetting(advancedGroup, "json.redundancyReduction", true);
	public static final Setting<Boolean> JsonLenient             = new BooleanSetting(advancedGroup, "json.lenient",             false);

	public static final Setting<String> GeocodingProvider        = new StringSetting(advancedGroup,  "geocoding.provider",            "org.structr.common.geo.GoogleGeoCodingProvider");
	public static final Setting<String> GeocodingLanguage        = new StringSetting(advancedGroup,  "geocoding.language",            "de");
	public static final Setting<String> GeocodingApiKey          = new StringSetting(advancedGroup,  "geocoding.apikey",              null);
	public static final Setting<String> DefaultDateFormat        = new StringSetting(advancedGroup,  "DateProperty.defaultFormat",    "yyyy-MM-dd'T'HH:mm:ssZ");
	public static final Setting<Boolean> InheritanceDetection    = new BooleanSetting(advancedGroup, "importer.inheritancedetection", true);

	// servlets
	public static final Setting<String> Servlets              = new StringSetting(servletsGroup,  "HttpService.servlets",             "JsonRestServlet HtmlServlet WebSocketServlet CsvServlet UploadServlet");

	public static final Setting<String> RestServletPath       = new StringSetting(servletsGroup,  "JsonRestServlet.path",             "/structr/rest/*");
	public static final Setting<String> RestServletClass      = new StringSetting(servletsGroup,  "JsonRestServlet.class",            "org.structr.rest.servlet.JsonRestServlet");
	public static final Setting<String> RestAuthenticator     = new StringSetting(servletsGroup,  "JsonRestServlet.authenticator",    "org.structr.web.auth.UiAuthenticator");
	public static final Setting<String> RestDefaultView       = new StringSetting(servletsGroup,  "JsonRestServlet.defaultview",      "public");
	public static final Setting<Integer> RestOutputDepth      = new IntegerSetting(servletsGroup, "JsonRestServlet.outputdepth",      3);
	public static final Setting<String> RestResourceProvider  = new StringSetting(servletsGroup,  "JsonRestServlet.resourceprovider", "org.structr.web.common.UiResourceProvider");
	public static final Setting<String> RestUserClass         = new StringSetting(servletsGroup,  "JsonRestServlet.user.class",       "org.structr.dynamic.User");
	public static final Setting<Boolean> RestUserAutologin    = new BooleanSetting(servletsGroup, "JsonRestServlet.user.autologin",   false);
	public static final Setting<Boolean> RestUserAutocreate   = new BooleanSetting(servletsGroup, "JsonRestServlet.user.autocreate",  false);

	public static final Setting<String> HtmlServletPath           = new StringSetting(servletsGroup,  "HtmlServlet.path",                  "/structr/html/*");
	public static final Setting<String> HtmlServletClass          = new StringSetting(servletsGroup,  "HtmlServlet.class",                 "org.structr.web.servlet.HtmlServlet");
	public static final Setting<String> HtmlAuthenticator         = new StringSetting(servletsGroup,  "HtmlServlet.authenticator",         "org.structr.web.auth.UiAuthenticator");
	public static final Setting<String> HtmlDefaultView           = new StringSetting(servletsGroup,  "HtmlServlet.defaultview",           "public");
	public static final Setting<Integer> HtmlOutputDepth          = new IntegerSetting(servletsGroup, "HtmlServlet.outputdepth",           3);
	public static final Setting<String> HtmlResourceProvider      = new StringSetting(servletsGroup,  "HtmlServlet.resourceprovider",      "org.structr.web.common.UiResourceProvider");
	public static final Setting<Boolean> HtmlUserAutologin        = new BooleanSetting(servletsGroup, "HtmlServlet.user.autologin",        false);
	public static final Setting<Boolean> HtmlUserAutocreate       = new BooleanSetting(servletsGroup, "HtmlServlet.user.autocreate",       true);
	public static final Setting<String> HtmlResolveProperties     = new StringSetting(servletsGroup,  "HtmlServlet.resolveProperties",     "AbstractNode.name");
	public static final Setting<String> HtmlCustomResponseHeaders = new StringSetting(servletsGroup,  "HtmlServlet.customResponseHeaders", "Strict-Transport-Security:max-age=60,\nX-Content-Type-Options:nosniff,\nX-Frame-Options:SAMEORIGIN,\nX-XSS-Protection:1;mode=block");

	public static final Setting<String> WebsocketServletPath       = new StringSetting(servletsGroup,  "WebSocketServlet.path",              "/structr/ws/*");
	public static final Setting<String> WebsocketServletClass      = new StringSetting(servletsGroup,  "WebSocketServlet.class",             "org.structr.websocket.servlet.WebSocketServlet");
	public static final Setting<String> WebsocketAuthenticator     = new StringSetting(servletsGroup,  "WebSocketServlet.authenticator",     "org.structr.web.auth.UiAuthenticator");
	public static final Setting<String> WebsocketDefaultView       = new StringSetting(servletsGroup,  "WebSocketServlet.defaultview",       "public");
	public static final Setting<Integer> WebsocketOutputDepth      = new IntegerSetting(servletsGroup, "WebSocketServlet.outputdepth",       3);
	public static final Setting<String> WebsocketResourceProvider  = new StringSetting(servletsGroup,  "WebSocketServlet.resourceprovider",  "org.structr.web.common.UiResourceProvider");
	public static final Setting<Boolean> WebsocketUserAutologin    = new BooleanSetting(servletsGroup, "WebSocketServlet.user.autologin",    false);
	public static final Setting<Boolean> WebsocketUserAutocreate   = new BooleanSetting(servletsGroup, "WebSocketServlet.user.autocreate",   false);
	public static final Setting<Boolean> WebsocketFrontendAccess   = new BooleanSetting(servletsGroup, "WebSocketServlet.frontendAccess",    false);

	public static final Setting<String> CsvServletPath       = new StringSetting(servletsGroup,  "CsvServlet.path",              "/structr/csv/*");
	public static final Setting<String> CsvServletClass      = new StringSetting(servletsGroup,  "CsvServlet.class",             "org.structr.rest.servlet.CsvServlet");
	public static final Setting<String> CsvAuthenticator     = new StringSetting(servletsGroup,  "CsvServlet.authenticator",     "org.structr.web.auth.UiAuthenticator");
	public static final Setting<String> CsvDefaultView       = new StringSetting(servletsGroup,  "CsvServlet.defaultview",       "public");
	public static final Setting<Integer> CsvOutputDepth      = new IntegerSetting(servletsGroup, "CsvServlet.outputdepth",       3);
	public static final Setting<String> CsvResourceProvider  = new StringSetting(servletsGroup,  "CsvServlet.resourceprovider",  "org.structr.web.common.UiResourceProvider");
	public static final Setting<Boolean> CsvUserAutologin    = new BooleanSetting(servletsGroup, "CsvServlet.user.autologin",    false);
	public static final Setting<Boolean> CsvUserAutocreate   = new BooleanSetting(servletsGroup, "CsvServlet.user.autocreate",   false);
	public static final Setting<Boolean> CsvFrontendAccess   = new BooleanSetting(servletsGroup, "CsvServlet.frontendAccess",    false);

	public static final Setting<String> UploadServletPath       = new StringSetting(servletsGroup,  "UploadServlet.path",                  "/structr/csv/*");
	public static final Setting<String> UploadServletClass      = new StringSetting(servletsGroup,  "UploadServlet.class",                 "org.structr.web.servlet.UploadServlet");
	public static final Setting<String> UploadAuthenticator     = new StringSetting(servletsGroup,  "UploadServlet.authenticator",         "org.structr.web.auth.UiAuthenticator");
	public static final Setting<String> UploadDefaultView       = new StringSetting(servletsGroup,  "UploadServlet.defaultview",           "public");
	public static final Setting<Integer> UploadOutputDepth      = new IntegerSetting(servletsGroup, "UploadServlet.outputdepth",           3);
	public static final Setting<String> UploadResourceProvider  = new StringSetting(servletsGroup,  "UploadServlet.resourceprovider",      "org.structr.web.common.UiResourceProvider");
	public static final Setting<Boolean> UploadUserAutologin    = new BooleanSetting(servletsGroup, "UploadServlet.user.autologin",        false);
	public static final Setting<Boolean> UploadUserAutocreate   = new BooleanSetting(servletsGroup, "UploadServlet.user.autocreate",       false);
	public static final Setting<Boolean> UploadAllowAnonymous   = new BooleanSetting(servletsGroup, "UploadServlet.allowAnonymousUploads", false);
	public static final Setting<Integer> UploadMaxFileSize      = new IntegerSetting(servletsGroup, "UploadServlet.maxFileSize",           1000);
	public static final Setting<Integer> UploadMaxRequestSize   = new IntegerSetting(servletsGroup, "UploadServlet.maxRequestSize",        1200);

	public static final Setting<Boolean> DeploymentAllowAnonymous = new BooleanSetting(servletsGroup, "DeploymentServlet.allowAnonymousUploads", false);
	public static final Setting<Integer> DeploymentMaxFileSize    = new IntegerSetting(servletsGroup, "DeploymentServlet.maxFileSize",           1000);
	public static final Setting<Integer> DeploymentMaxRequestSize = new IntegerSetting(servletsGroup, "DeploymentServlet.maxRequestSize",        1200);

	// cron settings
	public static final Setting<String> CronTasks               = new StringSetting(cronGroup,  "CronService.tasks", "");

	// oauth settings
	public static final Setting<String> OAuthServers            = new StringSetting(oauthGroup,  "auth.servers", "github twitter linkedin google facebook");

	public static final Setting<String> OAuthGithubAuthLocation   = new StringSetting(oauthGroup, "oauth.github.authorization_location", "https://github.com/login/oauth/authorize");
	public static final Setting<String> OAuthGithubTokenLocation  = new StringSetting(oauthGroup, "oauth.github.token_location", "https://github.com/login/oauth/access_token");
	public static final Setting<String> OAuthGithubClientId       = new StringSetting(oauthGroup, "oauth.github.client_id", "");
	public static final Setting<String> OAuthGithubClientSecret   = new StringSetting(oauthGroup, "oauth.github.client_secret", "");
	public static final Setting<String> OAuthGithubRedirectUri    = new StringSetting(oauthGroup, "oauth.github.redirect_uri", "/oauth/github/auth");
	public static final Setting<String> OAuthGithubUserDetailsUri = new StringSetting(oauthGroup, "oauth.github.user_details_resource_uri", "https://api.github.com/user/emails");
	public static final Setting<String> OAuthGithubErrorUri       = new StringSetting(oauthGroup, "oauth.github.error_uri", "/login");
	public static final Setting<String> OAuthGithubReturnUri      = new StringSetting(oauthGroup, "oauth.github.return_uri", "/");

	public static final Setting<String> OAuthTwitterAuthLocation  = new StringSetting(oauthGroup, "oauth.twitter.authorization_location", "https://api.twitter.com/oauth/authorize");
	public static final Setting<String> OAuthTwitterTokenLocation = new StringSetting(oauthGroup, "oauth.twitter.token_location", "https://api.twitter.com/oauth/access_token");
	public static final Setting<String> OAuthTwitterClientId      = new StringSetting(oauthGroup, "oauth.twitter.client_id", "");
	public static final Setting<String> OAuthTwitterClientSecret  = new StringSetting(oauthGroup, "oauth.twitter.client_secret", "");
	public static final Setting<String> OAuthTwitterRedirectUri   = new StringSetting(oauthGroup, "oauth.twitter.redirect_uri", "/oauth/twitter/auth");
	public static final Setting<String> OAuthTwitterErrorUri      = new StringSetting(oauthGroup, "oauth.twitter.error_uri", "/login");
	public static final Setting<String> OAuthTwitterReturnUri     = new StringSetting(oauthGroup, "oauth.twitter.return_uri", "/");

	public static final Setting<String> OAuthLinkedInAuthLocation   = new StringSetting(oauthGroup, "oauth.linkedin.authorization_location", "https://www.linkedin.com/uas/oauth2/authorization");
	public static final Setting<String> OAuthLinkedInTokenLocation  = new StringSetting(oauthGroup, "oauth.linkedin.token_location", "https://www.linkedin.com/uas/oauth2/accessToken");
	public static final Setting<String> OAuthLinkedInClientId       = new StringSetting(oauthGroup, "oauth.linkedin.client_id", "");
	public static final Setting<String> OAuthLinkedInClientSecret   = new StringSetting(oauthGroup, "oauth.linkedin.client_secret", "");
	public static final Setting<String> OAuthLinkedInRedirectUri    = new StringSetting(oauthGroup, "oauth.linkedin.redirect_uri", "/oauth/linkedin/auth");
	public static final Setting<String> OAuthLinkedInUserDetailsUri = new StringSetting(oauthGroup, "oauth.linkedin.user_details_resource_uri", "https://api.linkedin.com/v1/people/~/email-address?secure-urls=true");
	public static final Setting<String> OAuthLinkedInErrorUri       = new StringSetting(oauthGroup, "oauth.linkedin.error_uri", "/login");
	public static final Setting<String> OAuthLinkedInReturnUri      = new StringSetting(oauthGroup, "oauth.linkedin.return_uri", "/");

	public static final Setting<String> OAuthGoogleAuthLocation   = new StringSetting(oauthGroup, "oauth.google.authorization_location", "https://accounts.google.com/o/oauth2/auth");
	public static final Setting<String> OAuthGoogleTokenLocation  = new StringSetting(oauthGroup, "oauth.google.token_location", "https://accounts.google.com/o/oauth2/token");
	public static final Setting<String> OAuthGoogleClientId       = new StringSetting(oauthGroup, "oauth.google.client_id", "");
	public static final Setting<String> OAuthGoogleClientSecret   = new StringSetting(oauthGroup, "oauth.google.client_secret", "");
	public static final Setting<String> OAuthGoogleRedirectUri    = new StringSetting(oauthGroup, "oauth.google.redirect_uri", "/oauth/google/auth");
	public static final Setting<String> OAuthGoogleUserDetailsUri = new StringSetting(oauthGroup, "oauth.google.user_details_resource_uri", "https://www.googleapis.com/oauth2/v3/userinfo");
	public static final Setting<String> OAuthGoogleErrorUri       = new StringSetting(oauthGroup, "oauth.google.error_uri", "/login");
	public static final Setting<String> OAuthGoogleReturnUri      = new StringSetting(oauthGroup, "oauth.google.return_uri", "/");

	public static final Setting<String> OAuthFacebookAuthLocation   = new StringSetting(oauthGroup, "oauth.facebook.authorization_location", "https://www.facebook.com/dialog/oauth");
	public static final Setting<String> OAuthFacebookTokenLocation  = new StringSetting(oauthGroup, "oauth.facebook.token_location", "https://graph.facebook.com/oauth/access_token");
	public static final Setting<String> OAuthFacebookClientId       = new StringSetting(oauthGroup, "oauth.facebook.client_id", "");
	public static final Setting<String> OAuthFacebookClientSecret   = new StringSetting(oauthGroup, "oauth.facebook.client_secret", "");
	public static final Setting<String> OAuthFacebookRedirectUri    = new StringSetting(oauthGroup, "oauth.facebook.redirect_uri", "/oauth/facebook/auth");
	public static final Setting<String> OAuthFacebookUserDetailsUri = new StringSetting(oauthGroup, "oauth.facebook.user_details_resource_uri", "https://graph.facebook.com/me");
	public static final Setting<String> OAuthFacebookErrorUri       = new StringSetting(oauthGroup, "oauth.facebook.error_uri", "/login");
	public static final Setting<String> OAuthFacebookReturnUri      = new StringSetting(oauthGroup, "oauth.facebook.return_uri", "/");

	// miscellaneous settings
	public static final Setting<Integer> TcpPort                                  = new IntegerSetting(miscGroup, "tcp.port",                          5455);
	public static final Setting<Integer> UdpPort                                  = new IntegerSetting(miscGroup, "udp.port",                          5755);
	public static final Setting<String> RegistrationCustomUserClass               = new StringSetting(cronGroup, "Registration.customUserClass",       "");
	public static final Setting<Boolean> RegistrationAllowLoginBeforeConfirmation = new BooleanSetting(cronGroup, "Registration.customUserAttributes", false);
	public static final Setting<String> RegistrationCustomAttributes              = new StringSetting(cronGroup, "Registration.customUserAttributes",  "");

	public static Collection<SettingsGroup> getGroups() {
		return groups.values();
	}

	public static Collection<Setting> getSettings() {
		return settings.values();
	}

	public static <T> Setting<T> getSetting(final String... keys) {
		return settings.get(StringUtils.join(keys, "."));
	}

	public static Setting<String> getStringSetting(final String... keys) {

		final String key        = StringUtils.join(keys, ".");
		Setting<String> setting = settings.get(key);

		if (setting == null) {

			setting = new StringSetting(miscGroup, key, "");
		}

		return setting;
	}

	public static Setting<Integer> getIntegerSetting(final String... keys) {

		final String key        = StringUtils.join(keys, ".");
		Setting<Integer> setting = settings.get(key);

		if (setting == null) {

			setting = new IntegerSetting(miscGroup, key, 0);
		}

		return setting;
	}

	public static Setting<Boolean> getBooleanSetting(final String... keys) {

		final String key         = StringUtils.join(keys, ".");
		Setting<Boolean> setting = settings.get(key);

		if (setting == null) {

			setting = new BooleanSetting(miscGroup, key, false);
		}

		return setting;
	}

	public static Setting<?> createSettingForValue(final String key, final String value) {

		// try to determine property value type, string, integer or boolean?
		
		final String lowerCaseValue = value.toLowerCase();

		// boolean
		if ("true".equals(lowerCaseValue) || "false".equals(lowerCaseValue)) {

			return new BooleanSetting(miscGroup, key, Boolean.parseBoolean(value));
		}

		// integer
		if (StringUtils.isNumeric(value)) {

			return new IntegerSetting(miscGroup, key, Integer.parseInt(value));
		}

		return new StringSetting(miscGroup, key, value);
	}

	// ----- package methods -----
	static void registerGroup(final SettingsGroup group) {
		groups.put(group.getKey(), group);
	}

	static void registerSetting(final Setting setting) {
		settings.put(setting.getKey(), setting);
	}
}
