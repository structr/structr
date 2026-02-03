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
package org.structr.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.graalvm.home.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.config.RestartRequiredChangeHandler;
import org.structr.api.config.Setting;
import org.structr.api.config.Settings;
import org.structr.api.service.*;
import org.structr.api.util.CountResult;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.common.helper.PathHelper;
import org.structr.common.helper.VersionHelper;
import org.structr.core.app.StructrApp;
import org.structr.core.cluster.BroadcastReceiver;
import org.structr.core.cluster.ClusterManager;
import org.structr.core.cluster.StructrMessage;
import org.structr.core.function.SetLogLevelFunction;
import org.structr.core.graph.*;
import org.structr.cron.CronService;
import org.structr.docs.Documentable;
import org.structr.docs.documentables.service.FtpServiceDocumentable;
import org.structr.docs.documentables.service.MailServiceDocumentable;
import org.structr.docs.documentables.service.SSHServiceDocumentable;
import org.structr.files.external.DirectoryWatchService;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.SchemaHelper;
import org.structr.schema.SchemaService;
import org.structr.util.StructrLicenseManager;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class Services implements StructrServices, BroadcastReceiver {

	private static final Logger logger                 = LoggerFactory.getLogger(Services.class.getName());

	// singleton instance
	private static final String jvmIdentifier          = ManagementFactory.getRuntimeMXBean().getName();
	private static final long licenseCheckInterval     = TimeUnit.DAYS.toMillis(1);
	private static long lastLicenseCheck               = System.currentTimeMillis();
	private static Services singletonInstance          = null;
	private static boolean testingModeDisabled         = false;
	private static boolean overrideIndexManagement     = false;
	private static boolean skipIndexConfiguration      = false;
	private static Boolean cachedTestingFlag           = null;

	// non-static members
	private final Map<Class, Map<String, Service>> serviceCache = new ConcurrentHashMap<>(10, 0.9f, 8);
	private final Map<String, Class> registeredServiceClasses   = new LinkedHashMap<>();
	private final List<InitializationCallback> callbacks        = new LinkedList<>();
	private final Map<String, Object> cachedValues              = new ConcurrentHashMap<>(10, 0.9f, 8);
	private final Map<String, Object> applicationStore          = new ConcurrentHashMap<>(10, 0.9f, 8);
	private final ReentrantReadWriteLock reloading              = new ReentrantReadWriteLock(true);
	private LicenseManager licenseManager                       = null;
	private ConfigurationProvider configuration                 = null;
	private boolean isClusterStarted                            = true;
	private boolean initializationDone                          = false;
	private boolean overridingSchemaTypesAllowed                = true;
	private boolean shuttingDown                                = false;
	private boolean shutdownDone                                = false;
	private ClusterManager clusterManager;

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

	private static void checkJavaRuntime() {

		final int expectedMajorVersion = 25;
		final int expectedMinorVersion = 0;

		final Version expectedVersion  = org.graalvm.home.Version.create(expectedMajorVersion, expectedMinorVersion);
		final Version foundVersion     = org.graalvm.home.Version.getCurrent();
		boolean allowedVersion         = foundVersion.toString().startsWith(expectedVersion.toString());

		if (!allowedVersion) {

			logger.warn("Java Runtime Version mismatch; expected GraalVM version {}, found {}", expectedVersion, foundVersion);

			boolean enforceRuntime = Settings.EnforceRuntime.getValue();
			if (enforceRuntime) {

				logger.error("Strict Java Runtime Version check enabled. Aborting due to version mismatch. Please set application.runtime.enforce.recommended = false in structr.conf to enable start despite Java Runtime Version mismatch");
				System.exit(1);

			} else {

				logger.warn("Weak Java Runtime Version check enabled. Continuing despite version mismatch. To enable strong Java Runtime Version check set application.runtime.enforce.recommended = true in structr.conf");
			}
		}
	}

	/**
	 * Call this method to collect documentable services
	 *
	 * @param documentables
	 */
	public static void collectDocumentation(final List<Documentable> documentables) {

		documentables.add(new FtpServiceDocumentable());
		documentables.add(new SSHServiceDocumentable());
		documentables.add(new MailServiceDocumentable());
		documentables.add(CronService.getDocumentation());
		documentables.add(DirectoryWatchService.getDocumentation());
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

			final T command          = commandType.getDeclaredConstructor().newInstance();
			final Class serviceClass = command.getServiceClass();

			// inject security context first
			command.setArgument("securityContext", securityContext);

			if (serviceClass != null) {

				final String serviceName = getNameOfActiveService(serviceClass);

				// search for already running service..
				Service service = getService(serviceClass, serviceName);
				if (service == null) {

					// start service
					startService(serviceClass, serviceName, false);

					// reload service
					service = getService(serviceClass, serviceName);

					if (serviceClass.equals(NodeService.class)) {

						logger.debug("(Re-)Started NodeService, (re-)compiling dynamic schema");
						SchemaService.reloadSchema(new ErrorBuffer(), null, true, false);
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
		final File configFile = new File(Settings.ConfigFileName);

		if (Services.isTesting()) {

			// simulate fully configured system
			logger.info("Starting Structr for testing (structr.conf will be ignored)..");

		} else if (configFile.exists()) {

			logger.info("Reading {}..", Settings.ConfigFileName);
			Settings.loadConfiguration(Settings.ConfigFileName);

			SetLogLevelFunction.setLogLevel(Settings.LogLevel.getValue());

			// this might be the first start with a new / upgraded version
			// check if we need to do some migration maybe?

			if ("remote".equals(Settings.DatabaseDriverMode.getValue()) || Settings.ConnectionUser.isModified() || Settings.ConnectionUrl.isModified() || Settings.ConnectionPassword.isModified()) {

				if (!Settings.DatabaseDriver.isModified()) {

					logger.info("Migrating database connection configuration");

					// This is necessary because the default driver changed from bolt to in-memory, so we need to interpret an unmodified setting as "remote" and migrate.
					// Other config settings indicate that a remote driver was used, so we change the setting to use the neo4j driver.
					final DatabaseConnection connection = new DatabaseConnection();
					final String migratedServiceName    = "default-migrated";

					connection.setDisplayName("default-migrated");
					connection.setName(migratedServiceName);
					connection.setUrl(Settings.ConnectionUrl.getValue());
					connection.setDatabaseName(Settings.ConnectionDatabaseName.getValue());
					connection.setUsername(Settings.ConnectionUser.getValue());
					connection.setPassword(Settings.ConnectionPassword.getValue());
					connection.setDriver(Settings.DEFAULT_REMOTE_DATABASE_DRIVER);

					final ManageDatabasesCommand cmd = new ManageDatabasesCommand();

					try {

						cmd.addConnection(connection, false);

						setActiveServiceName(NodeService.class, migratedServiceName);

						Settings.DatabaseDriver.setValue(Settings.DatabaseDriver.getDefaultValue());
						Settings.ConnectionUrl.setValue(Settings.ConnectionUrl.getDefaultValue());
						Settings.ConnectionDatabaseName.setValue(Settings.ConnectionDatabaseName.getDefaultValue());
						Settings.ConnectionUser.setValue(Settings.ConnectionUser.getDefaultValue());
						Settings.ConnectionPassword.setValue(Settings.ConnectionPassword.getDefaultValue());
						Settings.DatabaseDriverMode.setValue(Settings.DatabaseDriverMode.getDefaultValue());

					} catch (FrameworkException fex) {
						logger.warn("Unable to migrate configuration: {}", fex.getMessage());
					}
				}
			}

			if (Settings.SuperUserPassword.isModified() && !Settings.SetupWizardCompleted.isModified()) {

				// when an existing config is detected and the superuser password is set, we can
				// safely assume that the configuration wizard can be skipped
				Settings.SetupWizardCompleted.setValue(true);
			}

			try {

				Settings.storeConfiguration(Settings.ConfigFileName, false);

			} catch (IOException ioex) {
				logger.warn("Unable to store migrated config: {}", ioex.getMessage());
			}
		}

		checkJavaRuntime();

		doInitialize();
	}

	private void doInitialize() {

		if (!isTesting()) {

			// read license
			licenseManager = new StructrLicenseManager();
		}

		// check settings for incompatibilities which would prevent proper function
		checkForSettingsConflicts();

		// if configuration is not yet established, instantiate it
		// this is the place where the service classes get the
		// opportunity to modify the default configuration
		getConfigurationProvider();

		// adjust configuration base on available memory etc.
		checkAvailableMemory();

		// join cluster if enabled
		joinCluster();

		// start registered services
		startServices();

		// register shutdown hook
		registerShutdownHook();

		// create admin users in all services that are active
		for (final NodeService nodeService : getServices(NodeService.class).values()) {

			nodeService.createAdminUser();
		}

		// register change handlers for various Settings
		registerSettingsChangeHandlers();

		// run migration service
		MigrationService.execute();

		logger.info("Started Structr {}", VersionHelper.getFullVersionInfo());
		logger.info("---------------- Initialization complete ----------------");

		setOverridingSchemaTypesAllowed(false);

		initializationDone = true;

		// run initialization callbacks
		runInitializationCallbacks();

		if (licenseManager != null && !Settings.DisableSendSystemInfo.getValue(false)) {
			new SystemInfoSender().start();
		}

		// if we are cluster coordinator, signal completion
		this.broadcastStartupComplete();
	}

	private void checkForSettingsConflicts() {

		// possibly check settings for configuration problems

	}

	private void registerSettingsChangeHandlers() {

		Settings.useFallbackLocale.setChangeHandler((setting, oldValue, newValue) -> FlushCachesCommand.flushLocalizationCache());
		Settings.fallbackLocale.setChangeHandler((setting, oldValue, newValue) -> FlushCachesCommand.flushLocalizationCache());

		Settings.UUIDv4AllowedFormats.setChangeHandler((setting, oldValue, newValue) -> RestartRequiredChangeHandler.logRestartRequiredMessage(setting, "Be aware that changing this setting, with existing data, could lead to data being inaccessible or being unable to start. This setting should only be changed with an empty database or in development."));

		Settings.UUIDv4CreateCompact.setChangeHandler((setting, oldValue, newValue) -> {

			// changing this setting only has an effect if we accept both UUIDv4 formats
			if (Settings.UUIDv4AllowedFormats.getValue().equals(Settings.POSSIBLE_UUID_V4_FORMATS.both.toString())) {

				RestartRequiredChangeHandler.logRestartRequiredMessage(setting);
			}
		});

		Settings.LogLevel.setChangeHandler((setting, oldValue, newValue) -> {
			SetLogLevelFunction.setLogLevel(newValue.toString());
		});

		Settings.DefaultUploadFolder.setChangeHandler((setting, oldValue, newValue) -> {

			final String cleanedNewValue = PathHelper.removeRelativeParts(newValue.toString());

			if (cleanedNewValue.equals("")) {

				logger.info("{}: Unable to save value '{}'. Default upload folder path requires a folder or folder path. Uploading to the root folder is not allowed. Resetting to default.", setting.getKey(), newValue);

				setting.setValue(setting.getDefaultValue());

			} else {

				setting.setValue(cleanedNewValue);
			}
		});

		Settings.EmailValidationRegex.setChangeHandler((setting, oldValue, newValue) -> {
			Settings.updateEmailValidationPattern();
		});
	}

	private void startServices() {

		final List<Class> configuredServiceClasses = getCongfiguredServiceClasses();

		logger.info("Starting services: {}", configuredServiceClasses.stream().map(Class::getSimpleName).collect(Collectors.toList()));

		final boolean maintenanceEnabled = Settings.MaintenanceModeEnabled.getValue();

		for (final Class serviceClass : configuredServiceClasses) {

			final StopServiceForMaintenanceMode stopAnnotation  = (StopServiceForMaintenanceMode)serviceClass.getAnnotation(StopServiceForMaintenanceMode.class);
			final StartServiceInMaintenanceMode startAnnotation = (StartServiceInMaintenanceMode)serviceClass.getAnnotation(StartServiceInMaintenanceMode.class);

			if (!maintenanceEnabled || (stopAnnotation == null && startAnnotation == null) || (stopAnnotation != null && startAnnotation != null)) {

				try {

					final String activeServiceName = getNameOfActiveService(serviceClass);
					startService(serviceClass, activeServiceName, false);

				} catch (FrameworkException ex) {
					logger.warn("Service {} failed to start: {}", serviceClass.getSimpleName(), ex.getMessage());
				}

			} else {

				logger.warn("Service {} not started in maintenance mode", serviceClass.getSimpleName());
			}
		}

		logger.info("{} service(s) processed", serviceCache.size());
	}

	private void checkAvailableMemory() {

		// do simple heap size check
		final Runtime runtime = Runtime.getRuntime();
		final long max        = runtime.maxMemory() / 1024 / 1024 / 1024;
		final int processors  = runtime.availableProcessors();

		logger.info("{} processors available, {} GB max heap memory", processors, max);

		if (max < 8) {

			logger.warn("Maximum heap size is smaller than recommended, this can lead to problems with large databases!");
			logger.warn("Please configure AT LEAST 8 GBs of heap memory using -Xmx8g.");

			// reduce fetch size
			final int maxFetchSize = (max < 1) ? 1_000 : 10_000;

			if (Settings.FetchSize.getValue() > maxFetchSize) {

				logger.info("Reducing fetch size setting '{}' to {} to reduce low-memory performance problems", Settings.FetchSize.getKey(), maxFetchSize);
				Settings.FetchSize.setValue(maxFetchSize);

				RuntimeEventLog.systemInfo("Reducing fetch size setting to reduce low-memory performance problems", Map.of("key", Settings.FetchSize.getKey(), "value", maxFetchSize));
			}
		}
	}

	private void joinCluster() {

		// Connect to cluster if enabled
		if (Settings.ClusterModeEnabled.getValue(false)) {

			logger.info("Cluster mode enabled");

			try {

				clusterManager = new ClusterManager();
				clusterManager.start(this);

				// wait for a connection
				while (!clusterManager.isConnected()) {
					try { Thread.sleep(100); } catch (Throwable t) {}
				}

				if (!clusterManager.isCoordinator()) {

					this.isClusterStarted = false;

					final long startTime = System.currentTimeMillis();

					logger.info("Waiting for cluster coordinator to complete startup..");

					// wait for coordinator to start
					while (!this.isClusterStarted) {

						logger.info("Requesting cluster coordinator status");

						// make coordinator send its startup status
						clusterManager.requestCoordinatorStatus();

						try { Thread.sleep(5000); } catch (Throwable t) {}

						// if cluster is not started after 5 minutes, we abort the process
						if (System.currentTimeMillis() > (startTime + (5 * 60 * 1000))) {

							logger.info("Timeout waiting for cluster startup, 5 minutes have passed, aborting");

							// hard exit
							System.exit(2);
						}
					}

					logger.info("Cluster coordinator has completed startup, continuing");

				} else {

					logger.info("I am cluster coordinator, continuing startup");

				}

			} catch (Exception ex) {

				logger.warn("Unable to connect to cluster: {}, disabling cluster mode", ex.getMessage());

				Settings.ClusterModeEnabled.setValue(false);
			}
		}
	}

	private void runInitializationCallbacks() {

		// only run initialization callbacks if Structr was started with
		// a configuration file, i.e. when this is NOT this first start.
		try {
			final ExecutorService service = Executors.newSingleThreadExecutor();
			service.submit(new Runnable() {

				@Override
				public void run() {

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
	}

	private void registerShutdownHook() {

		logger.info("Registering shutdown hook.");

		// register shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {

				shutdown();
			}
		});
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

	public String getUnavailableMessage() {
		return "Services is not initialized yet.";
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

			logger.info("Shutting down..");

			final List<Class> configuredServiceClasses = getCongfiguredServiceClasses();
			final List<Class> reverseServiceClassNames = new LinkedList<>(configuredServiceClasses);
			Collections.reverse(reverseServiceClassNames);

			for (final Class serviceClass : reverseServiceClassNames) {
				shutdownServices(serviceClass);
			}

			serviceCache.clear();

			// shut down configuration provider
			configuration.shutdown();

			// clear singleton instance
			singletonInstance = null;

			logger.info("Shutdown complete");

			// signal shutdown is complete
			shutdownDone = true;
		}
	}

	public boolean setMaintenanceMode(final Boolean maintenanceEnabled) {

		if (Settings.MaintenanceModeEnabled.getValue().equals(maintenanceEnabled)) {

			logger.info("Not setting maintenace mode to {} because it already is.", maintenanceEnabled);

		} else {

			Settings.MaintenanceModeEnabled.setValue(maintenanceEnabled);

			try {

				Settings.storeConfiguration(Settings.ConfigFileName, false);

				new Thread(new Runnable() {

					@Override
					public void run() {

						try { Thread.sleep(1000); } catch (Throwable t) {}

						logger.info("Setting maintenace mode = {}", maintenanceEnabled);

						final List<Class> configuredServiceClasses = getCongfiguredServiceClasses();
						final List<Class> reverseServiceClassNames = new LinkedList<>(configuredServiceClasses);
						Collections.reverse(reverseServiceClassNames);

						for (final Class serviceClass : reverseServiceClassNames) {

							final StopServiceForMaintenanceMode stopAnnotation = (StopServiceForMaintenanceMode)serviceClass.getAnnotation(StopServiceForMaintenanceMode.class);
							if (stopAnnotation != null) {

								shutdownServices(serviceClass);
							}
						}

						for (final Class serviceClass : configuredServiceClasses) {

							final StopServiceForMaintenanceMode stopAnnotation = (StopServiceForMaintenanceMode)serviceClass.getAnnotation(StopServiceForMaintenanceMode.class);
							if (stopAnnotation != null) {

								final StartServiceInMaintenanceMode startAnnotation = (StartServiceInMaintenanceMode)serviceClass.getAnnotation(StartServiceInMaintenanceMode.class);

								if (!maintenanceEnabled || startAnnotation != null) {

									try {

										final String activeServiceName = getNameOfActiveService(serviceClass);
										startService(serviceClass, activeServiceName, false);

									} catch (FrameworkException ex) {
										logger.warn("Service {} failed to start: {}", serviceClass.getSimpleName(), ex.getMessage());
									}
								}
							}
						}
					}
				}).start();

				return true;

			} catch (IOException ioe) {

				logger.warn("Storing configuration failed - unable to set maintenance mode");

				// revert settings in-memory back to previous value
				Settings.MaintenanceModeEnabled.setValue( !maintenanceEnabled );
			}
		}

		return false;
	}

	/**
	 * Registers a service, enabling the service layer to automatically start
	 * autorun servies.
	 *
	 * @param serviceClass the service class to register
	 */
	public void registerServiceClass(final Class serviceClass) {

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

				configuration = (ConfigurationProvider)Class.forName(configurationClass).getDeclaredConstructor().newInstance();
				configuration.initialize(licenseManager);

			} catch (Throwable t) {

				logger.error("Unable to instantiate configration provider of type {}: {}", configurationClass, t);
			}
		}

		return configuration;
	}

	/**
	 * Cache a value in the service config
	 *
	 * @param name
	 * @param value
	 */
	public void cacheValue(final String name, final Object value) {
		synchronized (cachedValues) {
			cachedValues.put(name, value);
		}
	}

	/**
	 * Retrieve a cached value from service config
	 *
	 * @param name
	 * @return attribute
	 */
	public Object getCachedValue(final String name) {
		return cachedValues.get(name);
	}

	/**
	 * Invalidate a cached value from service config
	 *
	 * @param name
	 */
	public void invalidateCachedValue(final String name) {
		cachedValues.remove(name);
	}

	public ServiceResult startService(final String serviceTypeAndName) throws FrameworkException {

		final String serviceTypeName = StringUtils.substringBefore(serviceTypeAndName, ".");
		final Class serviceType      = getServiceClassForName(serviceTypeName);

		if (serviceType != null) {

			final String serviceName = StringUtils.substringAfter(serviceTypeAndName, ".");

			return startService(serviceType, serviceName, false);
		}

		return new ServiceResult("Unknown service", false);
	}

	public boolean isConfigured(final Class serviceClass) {
		return getCongfiguredServiceClasses().contains(serviceClass);
	}

	public ServiceResult startService(final Class serviceClass, final String serviceName, final boolean disableRetry) throws FrameworkException {

		String errorMessage  = serviceClass.getSimpleName() + " " + serviceName + " failed to start.";
		boolean waitAndRetry = true;
		boolean isVital      = false;

		try {

			reloading.readLock().lock();

			if (!getCongfiguredServiceClasses().contains(serviceClass)) {

				logger.warn("Service {} is not listed in {}, will not be started.", serviceClass.getName(), "configured.services");
				return new ServiceResult("Service is not listed in configured.services", false);
			}

			logger.info("Creating {}..", serviceClass.getSimpleName());

			final Service service = (Service) serviceClass.getDeclaredConstructor().newInstance();
			final int retryDelay  = service.getRetryDelay();
			int retryCount        = service.getRetryCount();
			isVital               = service.isVital();

			while (waitAndRetry && retryCount-- > 0) {

				waitAndRetry = service.waitAndRetry() && !disableRetry;

				final ServiceResult result = service.initialize(this, serviceName);
				if (result.isSuccess()) {

					if (service instanceof RunnableService runnableService) {

						if (runnableService.runOnStartup()) {

							// start RunnableService and cache it
							runnableService.startService();
						}
					}

					if (service.isRunning()) {

						// cache service instance
						addService(serviceClass, service, serviceName);
					}

					// store service name after successful activation
					setActiveServiceName(serviceClass, serviceName);

					// initialization callback
					service.initialized();

					// abort wait and retry loop
					waitAndRetry = false;

					// success
					return new ServiceResult(true);

				} else if (!disableRetry && isVital && !waitAndRetry) {

					checkVitalService(serviceClass, null);

				} else {

					errorMessage = result.getMessage();
				}

				if (waitAndRetry) {

					if (retryCount > 0) {

						logger.warn("Retrying in {} seconds..", retryDelay);
						Thread.sleep(retryDelay * 1000L);

					} else {

						if (isVital) {
							checkVitalService(serviceClass, null);
						}
					}
				}
			}

		} catch (Throwable t) {

			logger.error("Exception occured when trying to start service " + serviceName, t);

                        if (!disableRetry && isVital) {

				checkVitalService(serviceClass, t);

			} else {

				throw new FrameworkException(503, errorMessage);
			}

		} finally {

			reloading.readLock().unlock();
		}

		return new ServiceResult(errorMessage, false);
	}

	public <T extends Service> void shutdownServices(final Class<T> serviceClass) {

		for (final Entry<String, T> entry : getServices(serviceClass).entrySet()) {

			shutdownService(entry.getValue(), entry.getKey());
		}
	}

	public void shutdownService(final String serviceTypeAndName) {

		final String serviceTypeName = StringUtils.substringBefore(serviceTypeAndName, ".");
		final Class serviceType      = getServiceClassForName(serviceTypeName);

		if (serviceType != null) {

			final String serviceName = StringUtils.substringAfter(serviceTypeAndName, ".");
			final Service service    = getService(serviceType, serviceName);

			shutdownService(service, serviceName);
		}
	}

	private void shutdownService(final Service service, final String name) {

		try {

			if (service instanceof RunnableService runnableService) {

				if (runnableService.isRunning()) {
					logger.info("Stopping {}..", service.getName());
					runnableService.stopService();
				}
			}

			service.shutdown();

		} catch (Throwable t) {

			logger.warn("Failed to shut down " + service.getName() + ": " + t.getMessage());
		}

		// remove from service cache
		removeService(service.getClass(), name);
	}

	/**
	 * Return all registered services
	 *
	 * @return list of services
	 */
	public List<Class> getRegisteredServiceClasses() {
		return new LinkedList<>(registeredServiceClasses.values());
	}

	public <T extends Service> T getServiceImplementation(final Class<T> type) {

		for (final Map<String, Service> serviceList : serviceCache.values()) {

			for (final Service service : serviceList.values()) {

				if (type.isAssignableFrom(service.getClass())) {
					return (T)service;
				}
			}
		}

		return null;
	}

	@Override
	public DatabaseService getDatabaseService() {

		try {

			reloading.readLock().lock();

			final String name             = getNameOfActiveService(NodeService.class);
			final NodeService nodeService = getService(NodeService.class, name);
			if (nodeService != null) {

				return nodeService.getDatabaseService();
			}

		} finally {

			reloading.readLock().unlock();
		}

		return null;
	}

	/**
	 * Return true if the given service is ready to be used,
         * means initialized and running.
	 *
	 * @param serviceClass
	 * @param name
	 * @return isReady
	 */
	public boolean isReady(final Class serviceClass, final String name) {

		final Service service = (Service)getServices(serviceClass).get(name);
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

	// ----- interface StructrServices -----
	@Override
	public void registerInitializationCallback(final InitializationCallback callback) {

		callbacks.add(callback);

		// callbacks need to be sorted by priority
		Collections.sort(callbacks, (o1, o2) -> { return Integer.valueOf(o1.priority()).compareTo(o2.priority()); });
	}

	@Override
	public <T extends Service> T getService(final Class<T> type, final String name) {

		final T service = getServices(type).get(name);
		if (service == null) {

			try {

				// try to start service
				startService(type, name, false);

			} catch (FrameworkException ex) {
				logger.warn("Service {} failed to start: {}", type.getSimpleName(), ex.getMessage());
			}
		}

		return service;
	}

	@Override
	public <T extends Service> Map<String, T> getServices(final Class<T> type) {

		Map<String, Service> serviceMap = serviceCache.get(type);
		if (serviceMap == null) {

			serviceMap = new ConcurrentHashMap<>();
			serviceCache.put(type, serviceMap);
		}

		return (Map)serviceMap;
	}

	@Override
	public LicenseManager getLicenseManager() {
		return licenseManager;
	}

	@Override
	public String getInstanceName() {
		return VersionHelper.getInstanceName();
	}

	@Override
	public String getVersion() {
		return VersionHelper.getVersion();
	}

	@Override
	public boolean hasExclusiveDatabaseAccess() {
		return clusterManager == null || clusterManager.isCoordinator();
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

	public String getEdition() {

		if (licenseManager != null) {
			return licenseManager.getEdition();
		}

		return "Community";
	}

	public void setActiveServiceName(final Class type, final String name) {

		if ("default".equals(name)) {

			Settings.getOrCreateStringSetting(type.getSimpleName(), "active").setValue(null);

		} else {

			Settings.getOrCreateStringSetting(type.getSimpleName(), "active").setValue(name);

		}
	}

	public ServiceResult activateService(final Class type, final String name) throws FrameworkException {

		try {

			reloading.writeLock().lock();

			FlushCachesCommand.flushAll();

			shutdownServices(type);

			final ServiceResult result = startService(type, name, true);
			if (result.isSuccess()) {

				// reload schema..
				SchemaService.reloadSchema(new ErrorBuffer(), null, true, false);
			}

			return result;

		} finally {

			reloading.writeLock().unlock();
		}
	}

	public <T extends Service> String getNameOfActiveService(final Class<T> type) {
		return Settings.getOrCreateStringSetting(type.getSimpleName(), "active").getValue("default");
	}

	public static void enableIndexConfiguration() {
		overrideIndexManagement = true;
		skipIndexConfiguration = false;
	}

	public static void disableIndexConfiguration() {
		overrideIndexManagement = true;
		skipIndexConfiguration = true;
	}

	public static void disableTestingMode() {
		testingModeDisabled = true;
	}

	public static boolean skipIndexConfiguration() {
		return skipIndexConfiguration;
	}

	public static boolean overrideIndexManagement() {
		return overrideIndexManagement;
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

	public Map<String, Object> getApplicationStore() {
		return applicationStore;
	}

	// ----- private methods -----
	private void checkVitalService(final Class service, final Throwable t) {

		if (t != null) {

			logger.error("Vital service {} failed to start with \"{}\", aborting.", service.getSimpleName(), t.getMessage() );
			System.err.println("Vital service " + service.getSimpleName() + " failed to start with \"" + t.getMessage() + "\", aborting.");

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

	private void removeService(final Class type, final String name) {
		getServices(type).remove(name);
	}

	private void addService(final Class type, final Service service, final String name) {
		getServices(type).put(name, service);
	}

	private void checkLicense() {

		if (licenseManager != null) {
			licenseManager.refresh();
		}
	}

	private void broadcastMessageToCluster(final String type, final Object payload) {
		this.broadcastMessageToCluster(type, payload, false);
	}

	private void broadcastMessageToCluster(final String type, final Object payload, final boolean waitForDelivery) {

		if (Settings.ClusterModeEnabled.getValue(false)) {

			if (this.clusterManager != null) {

				try {

					this.clusterManager.broadcast(type, payload, waitForDelivery);

				} catch (Exception ex) {

					ex.printStackTrace();
				}

			} else {

				logger.info("Cannot broadcast {}, channel is null!", type);
			}
		}
	}

	// ----- interface BroadcastReceiver -----
	public void broadcastDataChange(final List<Long> ids) {
		Services.getInstance().broadcastMessageToCluster("data-changed", ids);
	}

	public void broadcastLogin(final long userId) {
		try {
			Services.getInstance().broadcastMessageToCluster("data-changed", List.of(userId), true);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public void broadcastLogout(final long userId) {
		try {
			Services.getInstance().broadcastMessageToCluster("data-changed", List.of(userId), true);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public void broadcastSchemaChange() {
		try {
			Services.getInstance().broadcastMessageToCluster("schema-changed", null);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public void broadcastStartupComplete() {
		try {
			Services.getInstance().broadcastMessageToCluster("startup-complete", null);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
	public void receive(final String sender, final StructrMessage message) {

		final String type = message.getType();
		switch (type) {

			case "schema-changed":

				// only react to these changes if the cluster is started
				if (this.isClusterStarted) {
					SchemaHelper.reloadSchema(new ErrorBuffer(), null, true, false);
				}
				break;

			case "data-changed":
				break;

			case "startup-complete":

				this.isClusterStarted = true;
				break;

			case "status-requested":

				// send status update so other
				if (clusterManager != null && clusterManager.isCoordinator() && this.initializationDone) {
					this.broadcastMessageToCluster("startup-complete", null);
				}

				break;
		}
	}

	@Override
	public String getNodeName() {
		return "cluster-node-" + System.getenv("REPLICA");
	}

	// ----- nested classes -----
	private class SystemInfoSender extends Thread {

		public SystemInfoSender() {

			super("SystemInfoSenderThread");

			setDaemon(true);
		}

		@Override
		public void run() {

			final String systemInfoDataId = sendInitialSystemInfoData();
			if (systemInfoDataId != null) {

				while (true) {

					try {

						sendContinuedSystemInfoData(systemInfoDataId);
						Thread.sleep(TimeUnit.HOURS.toMillis(24));

					} catch (Throwable ignore) {}
				}
			}
		}

		// ----- private methods -----
		private String sendInitialSystemInfoData() {

			String id = null;

			try {

				final URL url                  = new URL("https://sysinfo.structr.com/structr/rest/SystemInfoData");
				final Map<String, Object> data = new LinkedHashMap<>();
				final URLConnection con        = url.openConnection();
				final HttpURLConnection http   = (HttpURLConnection)con;
				final Gson gson                = new GsonBuilder().create();
				final Runtime runtime          = Runtime.getRuntime();

				data.put("version",  VersionHelper.getFullVersionInfo());
				data.put("edition",  licenseManager.getEdition());
				data.put("hostId",   licenseManager.getHardwareFingerprint());
				data.put("jvm",      Runtime.version().toString());
				data.put("memory",   runtime.maxMemory() / 1024 / 1024);
				data.put("cpus",     runtime.availableProcessors());
				data.put("os",       System.getProperty("os.name"));

				http.setRequestProperty("ContentType", "application/json");
				http.setReadTimeout(1000);
				http.setConnectTimeout(1000);
				http.setRequestMethod("POST");
				http.setDoOutput(true);
				http.setDoInput(true);
				http.connect();

				// write request body
				try (final Writer writer = new OutputStreamWriter(http.getOutputStream())) {

					gson.toJson(data, writer);
					writer.flush();
				}

				final InputStream input = http.getInputStream();
				if (input != null) {

					// consume response
					final String responseText          = IOUtils.toString(input, StandardCharsets.UTF_8);
					final Map<String, Object> response = gson.fromJson(responseText, Map.class);

					if (response != null) {

						final Object result = response.get("result");
						if (result instanceof List list) {

							if (!list.isEmpty()) {

								final Object entry = list.get(0);
								if (entry instanceof String) {

									id = (String)entry;
								}
							}
						}
					}
				}

				final InputStream error = http.getErrorStream();
				if (error != null) {

					// consume error stream
					IOUtils.toString(error, StandardCharsets.UTF_8);
				}

				http.disconnect();

			} catch (Throwable ignore) {}

			return id;
		}

		private void sendContinuedSystemInfoData(final String id) {

			try {

				final URL url                  = new URL("https://sysinfo.structr.com/structr/rest/SystemInfoDataUpdate");
				final Map<String, Object> data = new LinkedHashMap<>();
				final URLConnection con        = url.openConnection();
				final HttpURLConnection http   = (HttpURLConnection)con;
				final Gson gson                = new GsonBuilder().create();

				try (final Tx tx = StructrApp.getInstance().tx()) {

					final DatabaseService db = Services.getInstance().getDatabaseService();
					final CountResult cr     = db.getNodeAndRelationshipCount();

					data.put("users",         cr.getUserCount());
					data.put("nodes",         cr.getNodeCount());
					data.put("relationships", cr.getRelationshipCount());

					tx.success();
				}

				data.put("now",      System.currentTimeMillis());
				data.put("uptime",   ManagementFactory.getRuntimeMXBean().getUptime());
				data.put("parentId", id);

				http.setRequestProperty("ContentType", "application/json");
				http.setReadTimeout(1000);
				http.setConnectTimeout(1000);
				http.setRequestMethod("POST");
				http.setDoOutput(true);
				http.setDoInput(true);
				http.connect();

				// write request body
				try (final Writer writer = new OutputStreamWriter(http.getOutputStream())) {

					gson.toJson(data, writer);
					writer.flush();
				}

				final InputStream input = http.getInputStream();
				if (input != null) {

					// consume response
					IOUtils.toString(input, StandardCharsets.UTF_8);
				}

				final InputStream error = http.getErrorStream();
				if (error != null) {

					// consume error stream
					IOUtils.toString(error, StandardCharsets.UTF_8);
				}

				http.disconnect();

			} catch (Throwable ignore) {}
		}
	}
}
