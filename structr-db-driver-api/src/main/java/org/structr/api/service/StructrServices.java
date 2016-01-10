package org.structr.api.service;

import java.util.Iterator;
import java.util.Properties;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.DatabaseService;

/**
 *
 */
public interface StructrServices {

	void registerInitializationCallback(final InitializationCallback callback);
	<T extends Service> T getService(final Class<T> serviceClass);
	DatabaseService getDatabaseService();

	public static void mergeConfiguration(final Properties baseConfig, final Properties additionalConfig) {

		baseConfig.putAll(additionalConfig);
		trim(baseConfig);
	}

	public static void loadConfiguration(final Properties properties, final PropertiesConfiguration config) {

		final Iterator<String> keys = config.getKeys();

		while (keys.hasNext()) {

			final String key = keys.next();
			properties.setProperty(key, config.getString(key));
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
