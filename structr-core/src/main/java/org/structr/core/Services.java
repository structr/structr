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
package org.structr.core;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.config.Setting;
import org.structr.api.config.Settings;
import org.structr.api.service.Command;
import org.structr.api.service.InitializationCallback;
import org.structr.api.service.LicenseManager;
import org.structr.api.service.RunnableService;
import org.structr.api.service.Service;
import org.structr.api.service.ServiceDependency;
import org.structr.api.service.StructrServices;
import org.structr.common.Permission;
import org.structr.common.Permissions;
import org.structr.common.SecurityContext;
import org.structr.common.VersionHelper;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeService;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.SchemaService;
import org.structr.util.StructrLicenseManager;

public class Services implements StructrServices {

	private static final Logger logger                                   = LoggerFactory.getLogger(StructrApp.class.getName());

	// singleton instance
	private static String jvmIdentifier                = ManagementFactory.getRuntimeMXBean().getName();
	private static final long licenseCheckInterval     = TimeUnit.HOURS.toMillis(2);
	private static Services singletonInstance          = null;
	private static boolean testingModeDisabled         = false;
	private static boolean calculateHierarchy          = false;
	private static boolean updateIndexConfiguration    = false;
	private static Boolean cachedTestingFlag           = null;
	private static long lastLicenseCheck               = 0L;

	// non-static members
	private final List<InitializationCallback> callbacks       = new LinkedList<>();
	private final Set<Permission> permissionsForOwnerlessNodes = new LinkedHashSet<>();
	private final Map<String, Object> attributes               = new ConcurrentHashMap<>(10, 0.9f, 8);
	private final Map<Class, Service> serviceCache             = new ConcurrentHashMap<>(10, 0.9f, 8);
	private final Map<String, Class> registeredServiceClasses  = new LinkedHashMap<>();
	private LicenseManager licenseManager                      = null;
	private ConfigurationProvider configuration                = null;
	private boolean initializationDone                         = false;
	private boolean overridingSchemaTypesAllowed               = true;
	private boolean shuttingDown                               = false;
	private boolean shutdownDone                               = false;

	private Services() { }

	public static Services getInstance() {

		if (singletonInstance == null) {

			singletonInstance = new Services();
			singletonInstance.initialize();
		}

		if (System.currentTimeMillis() > lastLicenseCheck + licenseCheckInterval) {

			lastLicenseCheck = System.currentTimeMillis();

			synchronized (Services.class) {

				singletonInstance.checkLicense();
			}
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

			if (serviceClass != null) {

				// search for already running service..
				Service service = serviceCache.get(serviceClass);
				if (service == null) {

					// start service
					startService(serviceClass);

					// reload service
					service = serviceCache.get(serviceClass);

					if (serviceClass.equals(NodeService.class)) {
						logger.debug("(Re-)Started NodeService, (re-)compiling dynamic schema");
						SchemaService.reloadSchema(new ErrorBuffer(), null);
					}

				}

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

		if (Services.isTesting()) {

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

		if (!isTesting()) {

			// read license
			licenseManager = new StructrLicenseManager(Settings.getBasePath() + "license.key");
		}

		// if configuration is not yet established, instantiate it
		// this is the place where the service classes get the
		// opportunity to modify the default configuration
		getConfigurationProvider();

		// do simple heap size check
		final Runtime runtime = Runtime.getRuntime();
		final long max        = runtime.maxMemory() / 1024 / 1024 / 1024;
		final int processors  = runtime.availableProcessors();

		logger.info("{} processors available, {} GB max heap memory", processors, max);
		if (max < 8) {

			logger.warn("Maximum heap size is smaller than recommended, this can lead to problems with large databases!");
			logger.warn("Please configure AT LEAST 8 GBs of heap memory using -Xmx8g.");
		}

		final List<Class> configuredServiceClasses = getCongfiguredServiceClasses();

		logger.info("Starting services: {}", configuredServiceClasses.stream().map(Class::getSimpleName).collect(Collectors.toList()));

		// initialize other services
		for (final Class serviceClass : configuredServiceClasses) {

			startService(serviceClass);
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

		final NodeService nodeService = getService(NodeService.class);
		if (nodeService != null) {

			nodeService.createAdminUser();
		}

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

		logger.info("Started Structr {}", VersionHelper.getFullVersionInfo());

		// Don't use logger here because start/stop scripts rely on this line.
		System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.ms").format(new Date()) + "  ---------------- Initialization complete ----------------");

		setOverridingSchemaTypesAllowed(false);

		initializationDone = true;
	}

	@Override
	public void registerInitializationCallback(final InitializationCallback callback) {

		callbacks.add(callback);

		// callbacks need to be sorted by priority
		Collections.sort(callbacks, (o1, o2) -> { return Integer.valueOf(o1.priority()).compareTo(o2.priority()); });
	}

	@Override
	public LicenseManager getLicenseManager() {
		return licenseManager;
	}

	public boolean isShutdownDone() {
		return shutdownDone;
	}

	public boolean isShuttingDown() {
		return shuttingDown;
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

		if (shuttingDown || !initializationDone) {

			return;
		}

		shuttingDown = true;

		if (!shutdownDone) {

			System.out.println("INFO: Shutting down...");

			final List<Class> configuredServiceClasses = getCongfiguredServiceClasses();
			final List<Class> reverseServiceClassNames = new LinkedList<>(configuredServiceClasses);
			Collections.reverse(reverseServiceClassNames);

			for (final Class serviceClass : reverseServiceClassNames) {
				shutdownService(serviceClass);
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

		registeredServiceClasses.put(serviceClass.getSimpleName(), serviceClass);

		// make it possible to select options in configuration editor
		Settings.Services.addAvailableOption(serviceClass.getSimpleName());
	}

	public Class getServiceClassForName(final String serviceClassName) {
		return registeredServiceClasses.get(serviceClassName);
	}

	public ConfigurationProvider getConfigurationProvider() {

		// instantiate configuration provider
		if (configuration == null) {

			final String configurationClass = Settings.Configuration.getValue();

			// when executing tests, the configuration class may already exist,
			// so we don't instantiate it again since all the entities are already
			// known to the ClassLoader and we would miss the code in all the static
			// initializers.
			try {

				configuration = (ConfigurationProvider)Class.forName(configurationClass).newInstance();
				configuration.initialize(licenseManager);

			} catch (Throwable t) {

				logger.error("Unable to instantiate configration provider of type {}: {}", configurationClass, t);
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

		int retryCount       = Settings.ServicesStartRetries.getValue(10);
		int retryDelay       = Settings.ServicesStartTimeout.getValue(30);
		boolean waitAndRetry = true;
		boolean isVital      = false;

		if (!getCongfiguredServiceClasses().contains(serviceClass)) {

			logger.warn("Service {} is not listed in {}, will not be started.", serviceClass.getName(), "configured.services");
			return;
		}

		logger.info("Creating {}..", serviceClass.getSimpleName());

		try {

			final Service service = (Service) serviceClass.newInstance();

			if (licenseManager != null && !licenseManager.isValid(service)) {

				logger.error("Configured service {} is not part of the currently licensed Structr Edition.", serviceClass.getSimpleName());
				return;
			}

			isVital    = service.isVital();
			retryCount = service.getRetryCount();
			retryDelay = service.getRetryDelay();

			while (waitAndRetry && retryCount-- > 0) {

				waitAndRetry = service.waitAndRetry();

				try {

					if (service.initialize(this)) {

						if (service instanceof RunnableService) {

							RunnableService runnableService = (RunnableService) service;

							if (runnableService.runOnStartup()) {

								// start RunnableService and cache it
								runnableService.startService();
							}
						}

						if (service.isRunning()) {

							// cache service instance
							serviceCache.put(serviceClass, service);
						}

						// initialization callback
						service.initialized();

						// abort wait and retry loop
						waitAndRetry = false;

					} else if (isVital && !waitAndRetry) {

						checkVitalService(serviceClass, null);
					}

				} catch (Throwable t) {

					logger.warn("Service {} failed to start: {}", serviceClass.getSimpleName(), t.getMessage());

					if (isVital && !waitAndRetry) {
						checkVitalService(serviceClass, t);
					}
				}

				if (waitAndRetry) {

					if (retryCount > 0) {

						logger.warn("Retrying in {} seconds..", retryDelay);
						Thread.sleep(retryDelay * 1000);

					} else {

						if (isVital) {
							checkVitalService(serviceClass, null);
						}
					}
				}
			}

		} catch (Throwable t) {

			if (isVital) {
				checkVitalService(serviceClass, t);
			}
		}
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
	public List<Class> getServices() {
		return new LinkedList<>(registeredServiceClasses.values());
	}

	@Override
	public <T extends Service> T getService(final Class<T> type) {
		return (T) serviceCache.get(type);
	}

	public <T extends Service> T getServiceImplementation(final Class<T> type) {

		for (final Service service : serviceCache.values()) {
			if (type.isAssignableFrom(service.getClass())) {
				return (T)service;
			}
		}

		return null;
	}

	@Override
	public DatabaseService getDatabaseService() {

		final NodeService nodeService = getService(NodeService.class);
		if (nodeService != null) {

			return nodeService.getDatabaseService();
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

		return resources;
	}

	public List<Class> getCongfiguredServiceClasses() {

		final String[] names                  = Settings.Services.getValue("").split("[ ,]+");
		final Map<Class, Class> dependencyMap = new LinkedHashMap<>();
		final List<Class> classes             = new LinkedList<>();

		for (final String name : names) {

			final String trimmed = name.trim();
			if (StringUtils.isNotBlank(trimmed)) {

				final Class serviceClass = getServiceClassForName(name);
				if (serviceClass != null ) {

					classes.add(serviceClass);
				}
			}
		}

		// extract annotation information for service dependency tree
		for (final Class service : classes) {

			final ServiceDependency annotation = (ServiceDependency)service.getAnnotation(ServiceDependency.class);
			if (annotation != null) {

				final Class dependency = annotation.value();
				if (dependency != null) {

					dependencyMap.put(service, dependency);
				}

			} else {

				// warn user
				if (!NodeService.class.equals(service)) {
					logger.warn("Service {} does not have @ServiceDependency annotation, this is likely a bug.", service);
				}
			}
		}

		// sort classes according to dependency order..
		classes.sort((s1, s2) -> {

			final Integer level1 = recursiveGetHierarchyLevel(dependencyMap, new LinkedHashSet<>(), s1, 0);
			final Integer level2 = recursiveGetHierarchyLevel(dependencyMap, new LinkedHashSet<>(), s2, 0);

			return level1.compareTo(level2);
		});

		return classes;
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

		return Settings.SessionTimeout.getValue();
	}

	public static Set<Permission> getPermissionsForOwnerlessNodes() {
		return getInstance().permissionsForOwnerlessNodes;
	}

	public String getEdition() {

		if (licenseManager != null) {
			return licenseManager.getEdition();
		}

		return "Community";
	}

	public static void enableCalculateHierarchy() {
		calculateHierarchy = true;
	}

	public static void enableUpdateIndexConfiguration() {
		updateIndexConfiguration = true;
	}

	public static void disableTestingMode() {
		testingModeDisabled      = true;
		calculateHierarchy       = true;
		updateIndexConfiguration = true;
	}

	public static boolean calculateHierarchy() {
		return calculateHierarchy;
	}

	public static boolean updateIndexConfiguration() {
		return updateIndexConfiguration;
	}

	public static boolean isTesting() {

		if (testingModeDisabled) {
			return false;
		}

		if (cachedTestingFlag != null) {
			return cachedTestingFlag;
		}

		for (final StackTraceElement[] stackTraces : Thread.getAllStackTraces().values()) {

			for (final StackTraceElement elem : stackTraces) {

				if (elem.getClassName().startsWith("org.junit.")) {
					cachedTestingFlag = true;
					return true;
				}

				if (elem.getClassName().startsWith("org.testng.")) {
					cachedTestingFlag = true;
					return true;
				}
			}
		}

		cachedTestingFlag = false;
		return false;
	}

	public static String getJVMIdentifier() {
		return jvmIdentifier;
	}

	// ----- private methods -----
	private void checkVitalService(final Class service, final Throwable t) {

		if (t != null) {

			logger.error("Vital service {} failed to start with {}, aborting.", service.getSimpleName(), t.getMessage() );
			System.err.println("Vital service " + service.getSimpleName() + " failed to start with " + t.getMessage() + ", aborting.");

		} else {

			logger.error("Vital service {} failed to start, aborting.", service.getSimpleName() );
			System.err.println("Vital service " + service.getSimpleName() + " failed to start, aborting.");

		}
		System.exit(1);
	}

	private int recursiveGetHierarchyLevel(final Map<Class, Class> dependencyMap, final Set<String> alreadyCalculated, final Class c, final int depth) {

		// stop at level 20
		if (depth > 20) {
			return 20;
		}

		Class dependency = dependencyMap.get(c);
		if (dependency == null) {

			return 0;

		} else  {

			// recurse upwards
			return recursiveGetHierarchyLevel(dependencyMap, alreadyCalculated, dependency, depth + 1) + 1;
		}
	}

	private void checkLicense() {

		if (licenseManager != null) {
			licenseManager.refresh();
		}
	}
}
