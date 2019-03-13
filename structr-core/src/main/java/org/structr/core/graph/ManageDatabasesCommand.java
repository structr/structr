/**
 * Copyright (C) 2010-2019 Structr GmbH
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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.config.Setting;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;

/**
 */
public class ManageDatabasesCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final String KEY_ACTIVE                  = "active";
	private static final String KEY_NAME                    = "name";
	private static final String KEY_DRIVER                  = "driver";
	private static final String KEY_MODE                    = "mode";
	private static final String KEY_URL                     = "url";
	private static final String KEY_USERNAME                = "username";
	private static final String KEY_PASSWORD                = "password";
	private static final String KEY_TENANT_IDENTIFIER       = "tenantIdentifier";
	private static final String KEY_RELATIONSHIP_CACHE_SIZE = "relationshipCacheSize";
	private static final String KEY_NODE_CACHE_SIZE         = "nodeCacheSize";
	private static final String KEY_UUID_CACHE_SIZE         = "uuidCacheSize";
	private static final String KEY_FORCE_STREAMING         = "forceStreaming";

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		final String command = (String)attributes.get("command");
		if (StringUtils.isNotBlank(command)) {

			switch (command) {

				case "list":
					getPayload().addAll(getConnections());
					break;

				case "add":
					addConnection(attributes);
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

				Settings.storeConfiguration("structr.conf");

			} catch (IOException ex) {
				ex.printStackTrace();
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

	// ----- private methods -----
	private void activateConnection(final Map<String, Object> data) throws FrameworkException {

		final String name = (String)data.get("name");
		if (name != null) {

			if ("default".equals(name)) {

				// activate default connection..
				//Services.getInstance().setActiveService(NodeService.class, "");
				Settings.DatabaseConnection.setValue("");

			} else if (getConnectionNames().contains(name)) {

				//Services.getInstance().setActiveService(NodeService.class, name);
				Settings.DatabaseConnection.setValue(name);

			} else {

				throw new FrameworkException(422, "Connection with name " + name + " does not exist.");
			}

		} else {

			throw new FrameworkException(422, "Please supply the name of the connection to activate.");
		}
	}

	private void addConnection(final Map<String, Object> data) throws FrameworkException {

		final String prefix = (String)data.get(KEY_NAME);
		if (StringUtils.isNotBlank(prefix)) {

			if ("default".equals(prefix)) {

				throw new FrameworkException(422, "Cannot overwrite default connection.");
			}

			final Set<String> connectionNames = getConnectionNames();
			if (!connectionNames.contains(prefix)) {

				setOrDefault(Settings.DatabaseDriver,        prefix, data, KEY_DRIVER);
				setOrDefault(Settings.DatabaseDriverMode,    prefix, data, KEY_MODE);
				setOrDefault(Settings.ConnectionUrl,         prefix, data, KEY_URL);
				setOrDefault(Settings.ConnectionUser,        prefix, data, KEY_USERNAME);
				setOrDefault(Settings.ConnectionPassword,    prefix, data, KEY_PASSWORD);
				setOrDefault(Settings.TenantIdentifier,      prefix, data, KEY_TENANT_IDENTIFIER);
				setOrDefault(Settings.RelationshipCacheSize, prefix, data, KEY_RELATIONSHIP_CACHE_SIZE);
				setOrDefault(Settings.NodeCacheSize,         prefix, data, KEY_NODE_CACHE_SIZE);
				setOrDefault(Settings.UuidCacheSize,         prefix, data, KEY_UUID_CACHE_SIZE);
				setOrDefault(Settings.ForceResultStreaming,  prefix, data, KEY_FORCE_STREAMING);

				connectionNames.add(prefix);

				Settings.DatabaseAvailableConnections.setValue(StringUtils.join(connectionNames, " "));

			} else {

				throw new FrameworkException(422, "Configuration " + prefix + " already exists.");
			}

		} else {

			throw new FrameworkException(422, "Please supply the name of the connection to add.");
		}
	}

	private void removeConnection(final Map<String, Object> data) throws FrameworkException {

		final String prefix = (String)data.get(KEY_NAME);
		if (StringUtils.isNotBlank(prefix)) {

			if ("default".equals(prefix)) {

				throw new FrameworkException(422, "Cannot remove default connection.");
			}

			final Set<String> connectionNames = getConnectionNames();
			if (connectionNames.contains(prefix)) {

				Settings.DatabaseDriver.getPrefixedSetting(prefix).unregister();
				Settings.DatabaseDriverMode.getPrefixedSetting(prefix).unregister();
				Settings.ConnectionUrl.getPrefixedSetting(prefix).unregister();
				Settings.ConnectionUser.getPrefixedSetting(prefix).unregister();
				Settings.ConnectionPassword.getPrefixedSetting(prefix).unregister();
				Settings.TenantIdentifier.getPrefixedSetting(prefix).unregister();
				Settings.RelationshipCacheSize.getPrefixedSetting(prefix).unregister();
				Settings.NodeCacheSize.getPrefixedSetting(prefix).unregister();
				Settings.UuidCacheSize.getPrefixedSetting(prefix).unregister();
				Settings.ForceResultStreaming.getPrefixedSetting(prefix).unregister();

				connectionNames.remove(prefix);

				Settings.DatabaseAvailableConnections.setValue(StringUtils.join(connectionNames, " "));

			} else {

				throw new FrameworkException(422, "Configuration " + prefix + " does not exist.");
			}

		} else {

			throw new FrameworkException(422, "Please supply the name of the connection to remove.");
		}
	}

	private List<Map<String, Object>> getConnections() {

		final List<Map<String, Object>> connections = new LinkedList<>();

		// add default connection
		connections.add(getPrefixedSettings("default", ""));

		for (final String connection : getConnectionNames()) {

			// add connections defined in settings
			connections.add(getPrefixedSettings(connection, connection));
		}

		return connections;
	}

	private Map<String, Object> getPrefixedSettings(final String name, final String prefix) {

		final Map<String, Object> settings = new LinkedHashMap<>();

		settings.put(KEY_NAME,                    name);
		settings.put(KEY_DRIVER,                  Settings.DatabaseDriver.getPrefixedValue(prefix));
		settings.put(KEY_MODE,                    Settings.DatabaseDriverMode.getPrefixedValue(prefix));
		settings.put(KEY_URL,                     Settings.ConnectionUrl.getPrefixedValue(prefix));
		settings.put(KEY_USERNAME,                Settings.ConnectionUser.getPrefixedValue(prefix));
		settings.put(KEY_PASSWORD,                Settings.ConnectionPassword.getPrefixedValue(prefix));
		settings.put(KEY_TENANT_IDENTIFIER,       Settings.TenantIdentifier.getPrefixedValue(prefix));
		settings.put(KEY_RELATIONSHIP_CACHE_SIZE, Settings.RelationshipCacheSize.getPrefixedValue(prefix));
		settings.put(KEY_NODE_CACHE_SIZE,         Settings.NodeCacheSize.getPrefixedValue(prefix));
		settings.put(KEY_UUID_CACHE_SIZE,         Settings.UuidCacheSize.getPrefixedValue(prefix));
		settings.put(KEY_FORCE_STREAMING,         Settings.ForceResultStreaming.getPrefixedValue(prefix));

		// active?
		settings.put(KEY_ACTIVE, Settings.DatabaseConnection.getValue("").equals(prefix));

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

		setting.getPrefixedSetting(prefix).setValue((T)value);
	}
}