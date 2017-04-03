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
	public static final SettingsGroup miscGroup               = new SettingsGroup("misc",        "Miscellaneous");

	// general settings
	public static final Setting<String> ApplicationTitle      = new StringSetting(generalGroup,  "application.title",      "Structr 2.1");
	public static final Setting<String> Configuration         = new StringSetting(generalGroup,  "configuration.provider", "org.structr.module.JarConfigurationProvider");
	public static final Setting<String> BasePath              = new StringSetting(generalGroup,  "base.path",              "");
	public static final Setting<String> TmpPath               = new StringSetting(generalGroup,  "tmp.path",               "/tmp");
	public static final Setting<String> FilesPath             = new StringSetting(generalGroup,  "files.path",             System.getProperty("user.dir").concat("/files"));
	public static final Setting<String> LogDatabasePath       = new StringSetting(generalGroup,  "log.database.path",      System.getProperty("user.dir").concat("/logDb.dat"));
	public static final Setting<String> DataExchangePath      = new StringSetting(generalGroup,  "data.exchange.path",     "");
	public static final Setting<Boolean> RequestLogging       = new BooleanSetting(generalGroup, "log.requests",           false);
	public static final Setting<String> LogPrefix             = new StringSetting(generalGroup,  "log.prefix",             "structr");
	public static final Setting<String> SuperuserName         = new StringSetting(generalGroup,  "superuser.username",     "superadmin");
	public static final Setting<String> SuperuserPassword     = new StringSetting(generalGroup,  "superuser.password",     RandomStringUtils.randomAlphanumeric(12));

	public static final Setting<Boolean> Testing              = new BooleanSetting(generalGroup, "testing",                false);
	public static final StringSetting Services                = new StringSetting(generalGroup, "configured.services",     "HttpService");

	// server settings
	public static final Setting<String> ApplicationHost       = new StringSetting(serverGroup,  "application.host",              "0.0.0.0");
	public static final Setting<Integer> HttpPort             = new IntegerSetting(serverGroup, "application.http.port",         8082);
	public static final Setting<Integer> HttpsPort            = new IntegerSetting(serverGroup, "application.https.port",        8083);
	public static final Setting<Boolean> HttpsEnabled         = new BooleanSetting(serverGroup, "application.https.enabled",     false);
	public static final Setting<String> KeystorePath          = new StringSetting(serverGroup,  "application.keystore.path",     "");
	public static final Setting<String> KeystorePassword      = new StringSetting(serverGroup,  "application.keystore.password", "");

	// HTTP service settings
	public static final Setting<String> Servlets              = new StringSetting(serverGroup,  "HttpService.servlets",            "JsonRestServlet HtmlServlet WebSocketServlet CsvServlet UploadServlet");
	public static final Setting<String> ResourceHandlers      = new StringSetting(serverGroup,  "HttpService.resourceHandlers",    "StructrUiHandler");
	public static final Setting<String> LifecycleListeners    = new StringSetting(serverGroup,  "HttpService.lifecycle.listeners", "");
	public static final Setting<Boolean> GzipCompression      = new BooleanSetting(serverGroup, "HttpService.gzip.enabled",        true);
	public static final Setting<Boolean> Async                = new BooleanSetting(serverGroup, "HttpService.async",               true);
	public static final Setting<Boolean> JsonIndentation      = new BooleanSetting(serverGroup, "json.indentation",                true);
	public static final Setting<Boolean> HtmlIndentation      = new BooleanSetting(serverGroup, "html.indentation",                true);

	// database settings
	public static final Setting<String> DatabasePath           = new StringSetting(databaseGroup,  "database.path",                    "db");
	public static final Setting<String> DatabaseDriver         = new StringSetting(databaseGroup,  "database.driver",                  "org.structr.bolt.BoltDatabaseService");
	public static final Setting<String> DatabaseDriverMode     = new StringSetting(databaseGroup,  "database.driver.mode",             "embedded");
	public static final Setting<String> ConnectionUrl          = new StringSetting(databaseGroup,  "database.connection.url",          "bolt://localhost:7688");
	public static final Setting<String> ConnectionUser         = new StringSetting(databaseGroup,  "database.connection.username",     "neo4j");
	public static final Setting<String> ConnectionPassword     = new StringSetting(databaseGroup,  "database.connection.password",     "neo4j");
	public static final Setting<Integer> RelationshipCacheSize = new IntegerSetting(databaseGroup, "database.cache.relationship.size", 100000);
	public static final Setting<Integer> NodeCacheSize         = new IntegerSetting(databaseGroup, "database.cache.node.size",         100000);
	public static final Setting<Integer> QueryCacheSize        = new IntegerSetting(databaseGroup, "database.cache.query.size",        1000);
	public static final Setting<Boolean> CypherDebugLogging    = new BooleanSetting(databaseGroup, "log.cypher.debug",                 false);

	// application settings
	public static final Setting<Integer> SessionTimeout       = new IntegerSetting(databaseGroup, "application.session.timeout",           1800);
	public static final Setting<Integer> ResolutionDepth      = new IntegerSetting(databaseGroup, "application.security.resolution.depth", 5);
	public static final Setting<String> OwnerlessNodes        = new StringSetting(databaseGroup,  "application.security.ownerless.nodes",  "read");
	public static final Setting<Boolean> ChangelogEnabled     = new BooleanSetting(databaseGroup,  "application.changelog.enabled",        false);

	/*
	public static final String APPLICATION_SESSION_TIMEOUT               = "application.session.timeout";
	public static final String APPLICATION_SECURITY_RESOLUTION_DEPTH     = "application.security.resolution.depth";
	public static final String APPLICATION_SECURITY_OWNERLESS_NODES      = "application.security.ownerless.nodes";
	public static final String APPLICATION_CHANGELOG_ENABLED             = "application.changelog.enabled";
	public static final String APPLICATION_UUID_CACHE_SIZE               = "application.cache.uuid.size";
	public static final String APPLICATION_NODE_CACHE_SIZE               = "application.cache.node.size";
	public static final String APPLICATION_REL_CACHE_SIZE                = "application.cache.relationship.size";
	public static final String APPLICATION_FILESYSTEM_ENABLED            = "application.filesystem.enabled";
	public static final String APPLICATION_FILESYSTEM_INDEXING_LIMIT     = "application.filesystem.indexing.limit";
	public static final String APPLICATION_FILESYSTEM_INDEXING_MINLENGTH = "application.filesystem.indexing.word.minlength";
	public static final String APPLICATION_FILESYSTEM_INDEXING_MAXLENGTH = "application.filesystem.indexing.word.maxlength";
	public static final String APPLICATION_FILESYSTEM_UNIQUE_PATHS       = "application.filesystem.unique.paths";
	public static final String APPLICATION_INSTANCE_NAME                 = "application.instance.name";
	public static final String APPLICATION_INSTANCE_STAGE                = "application.instance.stage";
	public static final String APPLICATION_DEFAULT_UPLOAD_FOLDER         = "application.uploads.folder";
	public static final String APPLICATION_PROXY_HTTP_URL                = "application.proxy.http.url";
	public static final String APPLICATION_PROXY_HTTP_USERNAME           = "application.proxy.http.username";
	public static final String APPLICATION_PROXY_HTTP_PASSWORD           = "application.proxy.http.password";
	*/

	// mail settings
	public static final Setting<String> SmtpHost              = new StringSetting(smtpGroup,  "smtp.host",         "localhost");
	public static final Setting<Integer> SmtpPort             = new IntegerSetting(smtpGroup, "smtp.port",         25);
	public static final Setting<String> SmtpUser              = new StringSetting(smtpGroup,  "smtp.user",         "");
	public static final Setting<String> SmtpPasswor           = new StringSetting(smtpGroup,  "smtp.password",     "");
	public static final Setting<Boolean> SmtpTlsEnabled       = new BooleanSetting(smtpGroup, "smtp.tls.enabled",  true);
	public static final Setting<Boolean> SmtpTlsRequired      = new BooleanSetting(smtpGroup, "smtp.tls.required", true);

	// miscellaneous settings
	public static final Setting<Integer> TcpPort              = new IntegerSetting(miscGroup, "tcp.port",   5455);
	public static final Setting<Integer> UdpPort              = new IntegerSetting(miscGroup, "udp.port",   5755);

	public static Collection<SettingsGroup> getGroups() {
		return groups.values();
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

		final String key        = StringUtils.join(keys, ".");
		Setting<Boolean> setting = settings.get(key);

		if (setting == null) {

			setting = new BooleanSetting(miscGroup, key, false);
		}

		return setting;
	}

	static void registerGroup(final SettingsGroup group) {
		groups.put(group.getKey(), group);
	}

	static void registerSetting(final Setting setting) {
		settings.put(setting.getKey(), setting);
	}
}
