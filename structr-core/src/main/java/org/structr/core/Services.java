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
package org.structr.core;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.config.Setting;
import org.structr.api.config.Settings;
import org.structr.api.service.Command;
import org.structr.api.service.InitializationCallback;
import org.structr.api.service.RunnableService;
import org.structr.api.service.Service;
import org.structr.api.service.StructrServices;
import org.structr.common.Permission;
import org.structr.common.Permissions;
import org.structr.common.SecurityContext;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeService;
import org.structr.schema.ConfigurationProvider;

//~--- classes ----------------------------------------------------------------

/**
 * Provides access to the service layer in structr.
 *
 * Use the command method to obtain an instance of the desired command.
 *
 *
 */
public class Services implements StructrServices {

	private static final Logger logger                                   = LoggerFactory.getLogger(StructrApp.class.getName());

	// Configuration constants
	public static final String LOG_SERVICE_INTERVAL                      = "structr.logging.interval";
	public static final String LOG_SERVICE_THRESHOLD                     = "structr.logging.threshold";
	public static final String WS_INDENTATION                            = "ws.indentation";

	// singleton instance
	private static int globalSessionTimeout            = -1;
	private static Services singletonInstance          = null;

	// non-static members
	private final List<InitializationCallback> callbacks       = new LinkedList<>();
	private final Set<Permission> permissionsForOwnerlessNodes = new LinkedHashSet<>();
	private final Map<String, Object> attributes               = new ConcurrentHashMap<>(10, 0.9f, 8);
	private final Map<Class, Service> serviceCache             = new ConcurrentHashMap<>(10, 0.9f, 8);
	private final Set<Class> registeredServiceClasses          = new LinkedHashSet<>();
	private final Set<String> configuredServiceClasses         = new LinkedHashSet<>();
	private ConfigurationProvider configuration                = null;
	private boolean initializationDone                         = false;
	private boolean overridingSchemaTypesAllowed               = true;
	private boolean shutdownDone                               = false;
	private String configuredServiceNames                      = null;
	private String configurationClass                          = null;

	private Services() { }

	public static Services getInstance() {

		if (singletonInstance == null) {

			singletonInstance = new Services();
			singletonInstance.initialize();
		}

		return singletonInstance;
	}

	/**
	 * Creates and returns a command of the given <code>type</code>. If a command is
	 * found, the corresponding service will be discovered and activated.
	 *
	 * @param <T>
	 * @param securityContext
	 * @param commandType the runtime type of the desired command
	 * @return the command
	 */
	public <T extends Command> T command(final SecurityContext securityContext, final Class<T> commandType) {

		try {

			final T command          = commandType.newInstance();
			final Class serviceClass = command.getServiceClass();

			// inject security context first
			command.setArgument("securityContext", securityContext);

			if ((serviceClass != null) && configuredServiceClasses.contains(serviceClass.getSimpleName())) {

				// search for already running service..
				Service service = serviceCache.get(serviceClass);
				if (service != null) {

					logger.debug("Initializing command ", commandType.getName());
					service.injectArguments(command);
				}
			}

			command.initialized();

			return command;

		} catch (Throwable t) {

			logger.error("Exception while creating command {}", commandType.getName());
		}

		return null;
	}

	private void initialize() {

		// read structr.conf
		final String configFileName = "structr.conf";
		final File configFile       = new File(configFileName);

		if (Settings.Testing.getValue()) {

			// simulate fully configured system
			logger.info("Starting Structr for testing (structr.conf will be ignored)..");

		} else if (configFile.exists()) {

			logger.info("Reading {}..", configFileName);
			Settings.loadConfiguration(configFileName);

		} else {

			// write structr.conf with random superadmin password
			logger.info("Writing {}..", configFileName);

			try {
				Settings.storeConfiguration(configFileName);

			} catch (IOException ioex) {
				logger.warn("Unable to write {}: {}", configFileName, ioex.getMessage());
			}
		}

		doInitialize();
	}

	private void doInitialize() {

		configurationClass     = Settings.Configuration.getValue();
		configuredServiceNames = Settings.Services.getValue();

		// create set of configured services
		configuredServiceClasses.addAll(Arrays.asList(configuredServiceNames.split("[ ,]+")));

		// if configuration is not yet established, instantiate it
		// this is the place where the service classes get the
		// opportunity to modify the default configuration
		getConfigurationProvider();

		logger.info("Starting services");

		// initialize other services
		for (final String serviceClassName : configuredServiceClasses) {

				Class serviceClass = getServiceClassForName(serviceClassName);
				if (serviceClass != null) {

					startService(serviceClass);
				}
		}

		logger.info("{} service(s) processed", serviceCache.size());
		logger.info("Registering shutdown hook.");

		// register shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {

				shutdown();
			}
		});

		// read permissions for ownerless nodes
		final String configForOwnerlessNodes = Settings.OwnerlessNodes.getValue();
		if (StringUtils.isNotBlank(configForOwnerlessNodes)) {

			for (final String permission : configForOwnerlessNodes.split("[, ]+")) {

				final String trimmed = permission.trim();
				if (StringUtils.isNotBlank(trimmed)) {

					final Permission val = Permissions.valueOf(trimmed);
					if (val != null) {

						permissionsForOwnerlessNodes.add(val);

					} else {

						logger.warn("Invalid permisson {}, ignoring.", trimmed);
					}
				}
			}

		} else {

			// default
			permissionsForOwnerlessNodes.add(Permission.read);
		}

		// only run initialization callbacks if Structr was started with
		// a configuration file, i.e. when this is NOT this first start.
		try {
			final ExecutorService service = Executors.newSingleThreadExecutor();
			service.submit(new Runnable() {

					@Override
					public void run() {

						// wait a second
						try { Thread.sleep(100); } catch (Throwable ignore) {}

						// call initialization callbacks from a different thread
						for (final InitializationCallback callback : singletonInstance.callbacks) {
							callback.initializationDone();
						}
					}

			}).get();

		} catch (Throwable t) {
			logger.warn("Exception while executing post-initialization tasks", t);
		}


		// Don't use logger here because start/stop scripts rely on this line.
		System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.ms").format(new Date()) + "  ---------------- Initialization complete ----------------");

		setOverridingSchemaTypesAllowed(false);

		initializationDone = true;
	}

	@Override
	public void registerInitializationCallback(final InitializationCallback callback) {

		callbacks.add(callback);

		// callbacks need to be sorted by priority
		Collections.sort(callbacks, new Comparator<InitializationCallback>() {

			@Override
			public int compare(final InitializationCallback o1, final InitializationCallback o2) {
				return Integer.valueOf(o1.priority()).compareTo(o2.priority());
			}

		});
	}

	public boolean isInitialized() {
		return initializationDone;
	}

	public boolean isOverridingSchemaTypesAllowed() {
		return overridingSchemaTypesAllowed;
	}

	public void setOverridingSchemaTypesAllowed(final boolean allow) {
		overridingSchemaTypesAllowed = allow;
	}


	public void shutdown() {

		initializationDone = false;

		if (!shutdownDone) {

			System.out.println("INFO: Shutting down...");
			for (Service service : serviceCache.values()) {

				shutdownService(service);
			}

			serviceCache.clear();

			// shut down configuration provider
			configuration.shutdown();

			// clear singleton instance
			singletonInstance = null;

			System.out.println("INFO: Shutdown complete");

			// signal shutdown is complete
			shutdownDone = true;
		}

	}

	/**
	 * Registers a service, enabling the service layer to automatically start
	 * autorun servies.
	 *
	 * @param serviceClass the service class to register
	 */
	public void registerServiceClass(Class serviceClass) {
		registeredServiceClasses.add(serviceClass);
	}

	public Class getServiceClassForName(final String serviceClassName) {

		for (Class serviceClass : registeredServiceClasses) {

			if (serviceClass.getSimpleName().equals(serviceClassName)) {
				return serviceClass;
			}

		}

		return null;
	}

	public ConfigurationProvider getConfigurationProvider() {

		// instantiate configuration provider
		if (configuration == null) {

			// when executing tests, the configuration class may already exist,
			// so we don't instantiate it again since all the entities are already
			// known to the ClassLoader and we would miss the code in all the static
			// initializers.
			try {

				configuration = (ConfigurationProvider)Class.forName(configurationClass).newInstance();
				configuration.initialize();

			} catch (Throwable t) {

				logger.error("Unable to instantiate configration provider of type {}", configurationClass);
			}
		}

		return configuration;
	}

	/**
	 * Store an attribute value in the service config
	 *
	 * @param name
	 * @param value
	 */
	public void setAttribute(final String name, final Object value) {
		synchronized (attributes) {
			attributes.put(name, value);
		}
	}

	/**
	 * Retrieve attribute value from service config
	 *
	 * @param name
	 * @return attribute
	 */
	public Object getAttribute(final String name) {
		return attributes.get(name);
	}

	/**
	 * Remove attribute value from service config
	 *
	 * @param name
	 */
	public void removeAttribute(final String name) {
		attributes.remove(name);
	}

	public void startService(final String serviceName) {

		final Class serviceClass = getServiceClassForName(serviceName);
		if (serviceClass != null) {

			startService(serviceClass);
		}
	}

	public void startService(final Class serviceClass) {

		logger.info("Creating service ", serviceClass.getName());

		Service service = null;

		try {

			service = (Service) serviceClass.newInstance();
			service.initialize(this);

			if (service instanceof RunnableService) {

				RunnableService runnableService = (RunnableService) service;

				if (runnableService.runOnStartup()) {

					logger.info("Starting RunnableService instance ", serviceClass.getName());

					// start RunnableService and cache it
					runnableService.startService();
				}
			}

			if (service.isRunning()) {

				// cache service instance
				serviceCache.put(serviceClass, service);
			}

		} catch (Throwable t) {

			logger.error("Service {} failed to start", service.getClass().getSimpleName(), t);
		}

		logger.info("Calling initialization callback");

		// initialization callback
		service.initialized();

		logger.info("Service initialized.");
	}

	public void shutdownService(final String serviceName) {

		final Class serviceClass = getServiceClassForName(serviceName);
		if (serviceClass != null) {

			shutdownService(serviceClass);
		}
	}

	public void shutdownService(final Class serviceClass) {

		final Service service = serviceCache.get(serviceClass);
		if (service != null) {

			shutdownService(service);
		}
	}

	private void shutdownService(final Service service) {

		try {

			if (service instanceof RunnableService) {

				RunnableService runnableService = (RunnableService) service;

				if (runnableService.isRunning()) {
					runnableService.stopService();
				}
			}

			service.shutdown();

		} catch (Throwable t) {

			System.out.println("WARNING: Failed to shut down " + service.getName() + ": " + t.getMessage());
		}

		// remove from service cache
		serviceCache.remove(service.getClass());
	}

	/**
	 * Return all registered services
	 *
	 * @return list of services
	 */
	public List<String> getServices() {

		List<String> services = new LinkedList<>();

		for (Class serviceClass : registeredServiceClasses) {

			final String serviceName = serviceClass.getSimpleName();

			if (configuredServiceClasses.contains(serviceName)) {

				services.add(serviceName);
			}
		}

		return services;
	}

	@Override
	public <T extends Service> T getService(final Class<T> type) {
		return (T) serviceCache.get(type);
	}

	@Override
	public DatabaseService getDatabaseService() {

		final NodeService nodeService = getService(NodeService.class);
		if (nodeService != null) {

			return nodeService.getGraphDb();
		}

		return null;
	}

	/**
	 * Return true if the given service is ready to be used,
         * means initialized and running.
	 *
	 * @param serviceClass
	 * @return isReady
	 */
	public boolean isReady(final Class serviceClass) {

                Service service = serviceCache.get(serviceClass);
                return (service != null && service.isRunning());
	}

	public Set<String> getResources() {

		final Set<String> resources = new LinkedHashSet<>();

		// scan through structr.conf and try to identify module-specific classes
		for (final Setting setting : Settings.getSettings()) {

			final Object configurationValue = setting.getValue();
			if (configurationValue != null) {

				for (final String value : configurationValue.toString().split("[\\s ,;]+")) {

					try {

						// try to load class and find source code origin
						final Class candidate = Class.forName(value);
						if (!candidate.getName().startsWith("org.structr")) {

							final String codeLocation = candidate.getProtectionDomain().getCodeSource().getLocation().toString();
							if (codeLocation.startsWith("file:") && codeLocation.endsWith(".jar") || codeLocation.endsWith(".war")) {

								final File file = new File(URI.create(codeLocation));
								if (file.exists()) {

									resources.add(file.getAbsolutePath());
								}
							}
						}

					} catch (Throwable ignore) { }
				}
			}
		}

		logger.info("Found {} possible resources: {}", new Object[] { resources.size(), resources } );

		return resources;
	}

	// ----- static methods -----
	/**
	 * Tries to parse the given String to an int value, returning
	 * defaultValue on error.
	 *
	 * @param value the source String to parse
	 * @param defaultValue the default value that will be returned when parsing fails
	 * @return the parsed value or the given default value when parsing fails
	 */
	public static int parseInt(String value, int defaultValue) {

		if (StringUtils.isBlank(value)) {
			return defaultValue;
		}

		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ignore) {}

		return defaultValue;
	}

	public static boolean parseBoolean(String value, boolean defaultValue) {

		if (StringUtils.isBlank(value)) {
			return defaultValue;
		}

		try {
			return Boolean.parseBoolean(value);
		} catch(Throwable ignore) {}

		return defaultValue;
	}

	public static int getGlobalSessionTimeout() {

		if (globalSessionTimeout == -1) {

			globalSessionTimeout = Settings.SessionTimeout.getValue();
		}

		return globalSessionTimeout;
	}

	public static Set<Permission> getPermissionsForOwnerlessNodes() {
		return getInstance().permissionsForOwnerlessNodes;
	}
}
