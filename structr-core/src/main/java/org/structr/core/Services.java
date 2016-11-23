/**
 * Copyright (C) 2010-2016 Structr GmbH
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
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.service.Command;
import org.structr.api.service.InitializationCallback;
import org.structr.api.service.RunnableService;
import org.structr.api.service.Service;
import org.structr.api.service.SingletonService;
import org.structr.api.service.StructrServices;
import org.structr.common.Permission;
import org.structr.common.Permissions;
import org.structr.common.SecurityContext;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeService;
import org.structr.module.JarConfigurationProvider;
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
	private static Properties baseConf                                   = null;

	// Configuration constants
	public static final String INITIAL_SEED_FILE                         = "seed.zip";
	public static final String BASE_PATH                                 = "base.path";
	public static final String CONFIGURED_SERVICES                       = "configured.services";
	public static final String CONFIG_FILE_PATH                          = "configfile.path";
	public static final String FILES_PATH                                = "files.path";
	public static final String DATA_EXCHANGE_PATH                        = "data.exchange.path";
	public static final String LOG_DATABASE_PATH                         = "log.database.path";
	public static final String FOREIGN_TYPE                              = "foreign.type.key";
	public static final String LOG_SERVICE_INTERVAL                      = "structr.logging.interval";
	public static final String LOG_SERVICE_THRESHOLD                     = "structr.logging.threshold";
	public static final String SERVER_IP                                 = "server.ip";
	public static final String SMTP_HOST                                 = "smtp.host";
	public static final String SMTP_PORT                                 = "smtp.port";
	public static final String SMTP_USER                                 = "smtp.user";
	public static final String SMTP_PASSWORD                             = "smtp.password";
	public static final String SMTP_USE_TLS                              = "smtp.tls.enabled";
	public static final String SMTP_REQUIRE_TLS                          = "smtp.tls.required";
	public static final String SUPERUSER_USERNAME                        = "superuser.username";
	public static final String SUPERUSER_PASSWORD                        = "superuser.password";
	public static final String TCP_PORT                                  = "tcp.port";
	public static final String TMP_PATH                                  = "tmp.path";
	public static final String UDP_PORT                                  = "udp.port";
	public static final String JSON_INDENTATION                          = "json.indentation";
	public static final String HTML_INDENTATION                          = "html.indentation";
	public static final String WS_INDENTATION                            = "ws.indentation";
	public static final String JSON_REDUNDANCY_REDUCTION                 = "json.redundancyReduction";
	public static final String GEOCODING_PROVIDER                        = "geocoding.provider";
	public static final String GEOCODING_LANGUAGE                        = "geocoding.language";
	public static final String GEOCODING_APIKEY                          = "geocoding.apikey";
	public static final String CONFIGURATION                             = "configuration.provider";
	public static final String TESTING                                   = "testing";
	public static final String MIGRATION_KEY                             = "NodeService.migration";
	public static final String ACCESS_CONTROL_MAX_AGE                    = "access.control.max.age";
	public static final String ACCESS_CONTROL_ALLOW_METHODS              = "access.control.allow.methods";
	public static final String ACCESS_CONTROL_ALLOW_HEADERS              = "access.control.allow.headers";
	public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS          = "access.control.allow.credentials";
	public static final String ACCESS_CONTROL_EXPOSE_HEADERS             = "access.control.expose.headers";
	public static final String APPLICATION_SESSION_TIMEOUT               = "application.session.timeout";
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
	public static final String SNAPSHOT_PATH                             = "snapshot.path";
	public static final String WEBSOCKET_FRONTEND_ACCESS                 = "WebSocketServlet.frontendAccess";

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
	private Properties structrConf                             = new Properties();
	private ConfigurationProvider configuration                = null;
	private boolean initializationDone                         = false;
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

	public static Services getInstanceForTesting(final Properties properties) {

		if (singletonInstance == null) {

			singletonInstance = new Services();
			singletonInstance.initialize(properties);
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

		Class serviceClass = null;
		T command          = null;

		try {

			command = commandType.newInstance();

			// inject security context first
			command.setArgument("securityContext", securityContext);

			serviceClass = command.getServiceClass();

			if ((serviceClass != null) && configuredServiceClasses.contains(serviceClass.getSimpleName())) {

				// search for already running service..
				Service service = serviceCache.get(serviceClass);

				if (service == null) {

					// service not cached
					service = createService(serviceClass);

				} else {

					// check RunnableService for isRunning()..
					if (service instanceof RunnableService) {

						RunnableService runnableService = (RunnableService) service;

						if (!runnableService.isRunning()) {

							runnableService.stopService();
							runnableService.shutdown();
							service = createService(serviceClass);
						}
					}
				}

				logger.debug("Initializing command ", commandType.getName());
				service.injectArguments(command);
			}

			command.initialized();

		} catch (Throwable t) {

			logger.error("Exception while creating command {}", commandType.getName());
		}

		return (command);
	}

	private void initialize() {

		final Properties config = getBaseConfiguration();

		// read structr.conf
		final String configTemplateFileName = "structr.conf_templ";
		final String configFileName         = "structr.conf";
		final File configTemplateFile       = new File(configTemplateFileName);
		final File configFile               = new File(configFileName);

		if (!configFile.exists() && !configTemplateFile.exists()) {

			logger.error("Unable to create config file, {} and {} do not exist, aborting. Please create a {} configuration file and try again.", new Object[] { configFileName, configTemplateFileName } );

			// exit immediately, since we can not proceed without configuration file
			System.exit(1);
		}

		if (!configFile.exists() && configTemplateFile.exists()) {

			logger.warn("Configuration file {} not found, copying from template {}. Please adapt newly created {} to your needs.", new Object[] { configFileName, configTemplateFileName } );

			try {
				Files.copy(configTemplateFile.toPath(), configFile.toPath());

			} catch (IOException ioex) {

				logger.error("Unable to create config file, copying of template failed.", ioex);

				System.exit(1);
			}
		}

		logger.info("Reading {}..", configFileName);

		try {

			PropertiesConfiguration.setDefaultListDelimiter('\0');
			StructrServices.loadConfiguration(config, new PropertiesConfiguration(configFileName));

		} catch (ConfigurationException ex) {
			logger.error("", ex);
		}

		StructrServices.mergeConfiguration(config, structrConf);

		initialize(config);
	}

	private void initialize(final Properties properties) {

		this.structrConf = properties;

		configurationClass     = properties.getProperty(Services.CONFIGURATION);
		configuredServiceNames = properties.getProperty(Services.CONFIGURED_SERVICES);

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

					try {

						final Service service = createService(serviceClass);
						if (service != null) {

							service.initialized();

						} else {

							logger.warn("Service {} was not started!", serviceClassName);
						}

					} catch (Throwable t) {

						logger.warn("Exception while registering service {}", serviceClassName);
					}
				}
		}

		logger.info("{} service(s) processed", serviceCache.size());
		registeredServiceClasses.clear();

		logger.info("Registering shutdown hook.");

		// register shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {

				shutdown();
			}
		});

		// read permissions for ownerless nodes
		final String configForOwnerlessNodes = this.structrConf.getProperty(Services.APPLICATION_SECURITY_OWNERLESS_NODES, "read");
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

	public void shutdown() {

		initializationDone = false;

		if (!shutdownDone) {

			System.out.println("INFO: Shutting down...");
			for (Service service : serviceCache.values()) {

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

	public String getConfigurationValue(String key) {
		return getConfigurationValue(key, "");
	}

	public String getConfigurationValue(String key, String defaultValue) {
		return getCurrentConfig().getProperty(key, defaultValue);
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

				logger.error("Unable to instantiate schema provider of type {}", configurationClass);
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

	private Service createService(Class serviceClass) {

		logger.debug("Creating service ", serviceClass.getName());

		Service service = null;

		try {

			service = (Service) serviceClass.newInstance();
			service.initialize(this, getCurrentConfig());

			if (service instanceof RunnableService) {

				RunnableService runnableService = (RunnableService) service;

				if (runnableService.runOnStartup()) {

					logger.debug("Starting RunnableService instance ", serviceClass.getName());

					// start RunnableService and cache it
					runnableService.startService();
					serviceCache.put(serviceClass, service);
				}

			} else if (service instanceof SingletonService) {

				// cache SingletonService
				serviceCache.put(serviceClass, service);
			}

		} catch (Throwable t) {

			if (service.isVital()) {

				logger.error("Vital service {} failed to start. Aborting", service.getClass().getSimpleName());
				t.printStackTrace();

				// hard(est) exit
				System.exit(1);

			} else {

				logger.error("Service {} failed to start", service.getClass().getSimpleName());
				t.printStackTrace();

			}
		}

		return service;
	}

	/**
	 * Return all registered services
	 *
	 * @return list of services
	 */
	public List<Service> getServices() {

		List<Service> services = new LinkedList<>();
		for (Service service : serviceCache.values()) {
			services.add(service);
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

	public String getConfigValue(final Map<String, String> config, final String key, final String defaultValue) {

		String value = StringUtils.strip(config.get(key));

		if (value != null) {
			return value;
		}

		return defaultValue;
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

	public Properties getCurrentConfig() {
		return structrConf;
	}

	public Set<String> getResources() {

		final Set<String> resources = new LinkedHashSet<>();

		// scan through structr.conf and try to identify module-specific classes
		for (final Object configurationValue : structrConf.values()) {

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

		logger.info("Found {} possible resources: {}", new Object[] { resources.size(), resources } );

		return resources;
	}

	public static Properties getBaseConfiguration() {

		if (baseConf == null) {

			baseConf = new Properties();

			baseConf.setProperty(CONFIGURATION,             JarConfigurationProvider.class.getName());
			baseConf.setProperty(CONFIGURED_SERVICES,       "NodeService AgentService CronService SchemaService");
			baseConf.setProperty(JSON_INDENTATION,          "true");
			baseConf.setProperty(HTML_INDENTATION,          "true");

			baseConf.setProperty(SUPERUSER_USERNAME,        "superadmin");
			baseConf.setProperty(SUPERUSER_PASSWORD,        RandomStringUtils.randomAlphanumeric(12));

			baseConf.setProperty(BASE_PATH,                 "");
			baseConf.setProperty(TMP_PATH,                  "/tmp");
			baseConf.setProperty(FILES_PATH,                System.getProperty("user.dir").concat("/files"));
			baseConf.setProperty(LOG_DATABASE_PATH,         System.getProperty("user.dir").concat("/logDb.dat"));

			baseConf.setProperty(SMTP_HOST,                 "localhost");
			baseConf.setProperty(SMTP_PORT,                 "25");
			baseConf.setProperty(TCP_PORT,                  "54555");
			baseConf.setProperty(UDP_PORT,                  "57555");
		}

		return baseConf;
	}

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
			globalSessionTimeout = parseInt(Services.getInstance().getConfigurationValue(APPLICATION_SESSION_TIMEOUT, "1800"), 1800);
		}

		return globalSessionTimeout;
	}

	public static Set<Permission> getPermissionsForOwnerlessNodes() {
		return getInstance().permissionsForOwnerlessNodes;
	}
}
