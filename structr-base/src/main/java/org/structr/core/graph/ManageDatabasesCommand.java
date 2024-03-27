/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.core.graph;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseFeature;
import org.structr.api.DatabaseService;
import org.structr.api.config.Setting;
import org.structr.api.config.Settings;
import org.structr.api.service.DatabaseConnection;
import org.structr.api.service.ServiceResult;
import org.structr.common.error.*;
import org.structr.core.Services;
import org.structr.core.function.Functions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static org.structr.api.service.DatabaseConnection.*;

/**
 */
public class ManageDatabasesCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(ManageDatabasesCommand.class);

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		final String command = (String)attributes.get("command");
		if (StringUtils.isNotBlank(command)) {

			switch (command) {

				case "list":
					// handled by getCommandResult
					break;

				case "add":
					addConnection(attributes, false);
					break;

				case "remove":
					removeConnection(attributes);
					break;

				case "activate":
					activateConnection(attributes);
					break;

				default:
					throw new FrameworkException(422, "ManageDatabasesCommand: unknown command " + command + ", valid options are [list, add, remove, activate].");
			}

			try {

				Settings.storeConfiguration(Settings.ConfigFileName);

			} catch (IOException ex) {

				logger.error(ExceptionUtils.getStackTrace(ex));
			}

		} else {

			throw new FrameworkException(422, "ManageDatabasesCommand: missing command, valid options are [list, add, remove, activate].");
		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return false;
	}

	@Override
	public Object getCommandResult() {
		return getConnections();
	}

	public void deactivateConnections() throws FrameworkException {
		Services.getInstance().activateService(NodeService.class, "default");
	}

	public void activateConnection(final Map<String, Object> data) throws FrameworkException {

		final ErrorBuffer errorBuffer = checkInput(data, true);
		if (errorBuffer.hasError()) {

			throw new FrameworkException(422, "Invalid data.", errorBuffer);
		}

		final String name = (String)data.get(KEY_NAME);
		if ("default".equals(name)) {

			final ServiceResult result = Services.getInstance().activateService(NodeService.class, "");
			if (!result.isSuccess()) {

				throw new FrameworkException(503, result.getMessage());
			}

		} else if (getConnectionNames().contains(name)) {

			final ServiceResult result = Services.getInstance().activateService(NodeService.class, name);
			if (!result.isSuccess()) {

				throw new FrameworkException(503, result.getMessage());
			}

		} else {

			throw new FrameworkException(422, "Connection with name " + name + " does not exist.");
		}
	}

	public void addConnection(final Map<String, Object> data, final boolean connectNow) throws FrameworkException {

		final ErrorBuffer errorBuffer = checkInput(data, false);
		if (errorBuffer.hasError()) {

			throw new FrameworkException(422, "Invalid data.", errorBuffer);
		}

		final String prefix = (String)data.get(KEY_NAME);
		final Set<String> connectionNames = getConnectionNames();

		if (!connectionNames.contains(prefix)) {

			setOrDefault(Settings.DatabaseDriver,         prefix, data, KEY_DRIVER);
			setOrDefault(Settings.ConnectionName,         prefix, data, KEY_DISPLAYNAME);
			setOrDefault(Settings.ConnectionUrl,          prefix, data, KEY_URL);
			setOrDefault(Settings.ConnectionDatabaseName, prefix, data, KEY_DATABASENAME);
			setOrDefault(Settings.ConnectionUser,         prefix, data, KEY_USERNAME);
			setOrDefault(Settings.ConnectionPassword,     prefix, data, KEY_PASSWORD);

			//setOrDefault(Settings.TenantIdentifier,      prefix, data, KEY_TENANT_IDENTIFIER);
			//setOrDefault(Settings.RelationshipCacheSize, prefix, data, KEY_RELATIONSHIP_CACHE_SIZE);
			//setOrDefault(Settings.NodeCacheSize,         prefix, data, KEY_NODE_CACHE_SIZE);
			//setOrDefault(Settings.UuidCacheSize,         prefix, data, KEY_UUID_CACHE_SIZE);
			//setOrDefault(Settings.ForceResultStreaming,  prefix, data, KEY_FORCE_STREAMING);

			connectionNames.add(prefix);

			Settings.DatabaseAvailableConnections.setValue(StringUtils.join(connectionNames, " "));

			if (connectNow) {

				try {
					activateConnection(data);

				} catch (FrameworkException fex) {

					// if connecting fails, remove
					removeConnection(data);
					throw fex;
				}
			}

		} else {

			throw new FrameworkException(422, "Configuration " + prefix + " already exists.");
		}
	}

	public void saveConnection(final Map<String, Object> data) throws FrameworkException {

		final ErrorBuffer errorBuffer = checkInput(data, false);
		if (errorBuffer.hasError()) {

			throw new FrameworkException(422, "Invalid data.", errorBuffer);
		}

		final String prefix               = (String)data.get(KEY_NAME);
		final Set<String> connectionNames = getConnectionNames();

		if (connectionNames.contains(prefix)) {

			setOrDefault(Settings.DatabaseDriver,         prefix, data, KEY_DRIVER);
			setOrDefault(Settings.ConnectionUrl,          prefix, data, KEY_URL);
			setOrDefault(Settings.ConnectionDatabaseName, prefix, data, KEY_DATABASENAME);
			setOrDefault(Settings.ConnectionUser,         prefix, data, KEY_USERNAME);
			setOrDefault(Settings.ConnectionPassword,     prefix, data, KEY_PASSWORD);
			//setOrDefault(Settings.TenantIdentifier,      prefix, data, KEY_TENANT_IDENTIFIER);
			//setOrDefault(Settings.RelationshipCacheSize, prefix, data, KEY_RELATIONSHIP_CACHE_SIZE);
			//setOrDefault(Settings.NodeCacheSize,         prefix, data, KEY_NODE_CACHE_SIZE);
			//setOrDefault(Settings.UuidCacheSize,         prefix, data, KEY_UUID_CACHE_SIZE);
			//setOrDefault(Settings.ForceResultStreaming,  prefix, data, KEY_FORCE_STREAMING);

		} else {

			throw new FrameworkException(422, "Configuration " + prefix + " does not exist.");
		}
	}

	public void removeConnection(final Map<String, Object> data) throws FrameworkException {

		final String prefix = (String)data.get(KEY_NAME);
		if (StringUtils.isNotBlank(prefix)) {

			if ("default".equals(prefix)) {

				throw new FrameworkException(422, "Cannot remove default connection.");
			}

			final Set<String> connectionNames = getConnectionNames();
			if (connectionNames.contains(prefix)) {

				Settings.DatabaseDriver.getPrefixedSetting(prefix).unregister();
				Settings.ConnectionName.getPrefixedSetting(prefix).unregister();
				Settings.ConnectionUrl.getPrefixedSetting(prefix).unregister();
				Settings.ConnectionDatabaseName.getPrefixedSetting(prefix).unregister();
				Settings.ConnectionUser.getPrefixedSetting(prefix).unregister();
				Settings.ConnectionPassword.getPrefixedSetting(prefix).unregister();
				Settings.TenantIdentifier.getPrefixedSetting(prefix).unregister();
				Settings.RelationshipCacheSize.getPrefixedSetting(prefix).unregister();
				Settings.NodeCacheSize.getPrefixedSetting(prefix).unregister();
				Settings.UuidCacheSize.getPrefixedSetting(prefix).unregister();
				Settings.ForceResultStreaming.getPrefixedSetting(prefix).unregister();

				connectionNames.remove(prefix);

				Settings.DatabaseAvailableConnections.setValue(StringUtils.join(connectionNames, " "));

				final Services services = Services.getInstance();

				if (prefix.equals(services.getNameOfActiveService(NodeService.class))) {

					services.shutdownServices(NodeService.class);
					services.setActiveServiceName(NodeService.class, "default");
				}

			} else {

				throw new FrameworkException(422, "Configuration " + prefix + " does not exist.");
			}

		} else {

			throw new FrameworkException(422, "Please supply the name of the connection to remove.", new EmptyPropertyToken("Connection", "name"));
		}
	}

	public List<DatabaseConnection> getConnections() {

		final List<DatabaseConnection> connections = new LinkedList<>();

		for (final String connection : getConnectionNames()) {

			// add connections defined in settings
			connections.add(new DatabaseConnection(getPrefixedSettings(connection, connection)));
		}

		return connections;
	}

	public boolean hasActiveConnection() {
		return getConnections().stream().map(DatabaseConnection::isActive).reduce(false, (t, u) -> t || u);
	}

	// ----- private methods -----
	private Map<String, Object> getPrefixedSettings(final String name, final String prefix) {

		final Map<String, Object> settings = new LinkedHashMap<>();

		settings.put(KEY_NAME,                    name);
		settings.put(KEY_DRIVER,                  Settings.DatabaseDriver.getPrefixedValue(prefix));
		settings.put(KEY_DISPLAYNAME,             Settings.ConnectionName.getPrefixedValue(prefix));
		settings.put(KEY_URL,                     Settings.ConnectionUrl.getPrefixedValue(prefix));
		settings.put(KEY_DATABASENAME,            Settings.ConnectionDatabaseName.getPrefixedValue(prefix));
		settings.put(KEY_USERNAME,                Settings.ConnectionUser.getPrefixedValue(prefix));
		settings.put(KEY_PASSWORD,                Settings.ConnectionPassword.getPrefixedValue(prefix));
		settings.put(KEY_TENANT_IDENTIFIER,       Settings.TenantIdentifier.getPrefixedValue(prefix));
		settings.put(KEY_RELATIONSHIP_CACHE_SIZE, Settings.RelationshipCacheSize.getPrefixedValue(prefix));
		settings.put(KEY_NODE_CACHE_SIZE,         Settings.NodeCacheSize.getPrefixedValue(prefix));
		settings.put(KEY_UUID_CACHE_SIZE,         Settings.UuidCacheSize.getPrefixedValue(prefix));
		settings.put(KEY_FORCE_STREAMING,         Settings.ForceResultStreaming.getPrefixedValue(prefix));

		// active?
		settings.put(KEY_ACTIVE, Settings.getOrCreateStringSetting("NodeService.active").getValue("").equals(prefix));

		return settings;
	}

	private Set<String> getConnectionNames() {

		final String availableConnectionNames = Settings.DatabaseAvailableConnections.getValue();
		final Set<String> names               = new LinkedHashSet<>();

		if (StringUtils.isNotBlank(availableConnectionNames)) {

			for (final String name : availableConnectionNames.split("[\\, ]+")) {

				final String trimmed = name.trim();
				if (!trimmed.isEmpty()) {

					names.add(trimmed);
				}
			}
		}

		return names;
	}

	private <T> void setOrDefault(final Setting<T> setting, final String prefix, final Map<String, Object> data, final String key) {

		Object value = data.get(key);
		if (value == null) {

			value = setting.getValue();
		}

		final Setting<T> prefixedSetting = setting.getPrefixedSetting(prefix);

		prefixedSetting.setValue((T)value);

		// default value needs to be null so the new setting return true for isModified()
		prefixedSetting.setDefaultValue(null);
	}

	private ErrorBuffer checkInput(final Map<String, Object> data, final boolean nameOnly) {

		final ErrorBuffer errorBuffer = new ErrorBuffer();

		if (StringUtils.isEmpty((String)data.get(KEY_NAME))) {
			errorBuffer.add(new EmptyPropertyToken("Connection", "name"));
		}

		final String name    = (String)data.get(KEY_NAME);
		final String cleaned = Functions.cleanString(name);

		data.put(KEY_DISPLAYNAME, name);
		data.put(KEY_NAME,        cleaned);

		// connection cannot be named "default"
		if ("default".equals((String) data.get(KEY_NAME))) {
			errorBuffer.add(new UniqueToken("Connection", "name", "default", null, null));
		}

		if (!nameOnly) {

			if (StringUtils.isEmpty((String) data.get(KEY_URL))) {
				errorBuffer.add(new EmptyPropertyToken("Connection", "url"));
			}

			try {

				final Object driverClassString = data.get(KEY_DRIVER);
				DatabaseService databaseService = null;

				if (driverClassString != null) {

					databaseService = (DatabaseService) Class.forName((String) driverClassString).getDeclaredConstructor().newInstance();

				} else {

					databaseService = (DatabaseService) Class.forName("org.structr.bolt.BoltDatabaseService").getDeclaredConstructor().newInstance();
				}

				if (databaseService == null) {

					errorBuffer.add(new SemanticErrorToken("Driver", "driver", "driver_not_found"));

				} else {

					if (databaseService.supportsFeature(DatabaseFeature.AuthenticationRequired)) {

						if (StringUtils.isEmpty((String) data.get(KEY_USERNAME))) {
							errorBuffer.add(new EmptyPropertyToken("Connection", "username"));
						}

						if (StringUtils.isEmpty((String) data.get(KEY_PASSWORD))) {
							errorBuffer.add(new EmptyPropertyToken("Connection", "password"));
						}
					}
				}

			} catch (ClassNotFoundException|InstantiationException|IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException ex) {
				errorBuffer.add(new SemanticErrorToken("Driver", "driver", "driver_error"));
			}
		}

		return errorBuffer;
	}
}