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
package org.structr.api.service;

import java.util.Iterator;
import java.util.Properties;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.DatabaseService;
import org.structr.api.config.Setting;
import org.structr.api.config.Settings;

/**
 *
 */
public interface StructrServices {

	void registerInitializationCallback(final InitializationCallback callback);
	<T extends Service> T getService(final Class<T> serviceClass);
	DatabaseService getDatabaseService();
	boolean isConfigured();

	public static void loadConfiguration(final PropertiesConfiguration config) {

		final Iterator<String> keys = config.getKeys();
		while (keys.hasNext()) {

			final String key   = keys.next();
			final String value = trim(config.getString(key));
			Setting<?> setting = Settings.getSetting(key);

			if (setting != null) {

				setting.fromString(value);

			} else {

				// create new StringSetting for unknown key
				Settings.createSettingForValue(key, value);
			}
		}
	}

	public static String trim(final String value) {
		return StringUtils.trim(value);
	}

	public static void trim(final Properties properties) {
		for (Object k : properties.keySet()) {
			properties.put(k, trim((String) properties.get(k)));
		}
	}
}
