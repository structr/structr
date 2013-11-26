/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import org.apache.commons.lang.StringUtils;

import org.structr.module.ModuleService;

//~--- JDK imports ------------------------------------------------------------

//import org.structr.common.xpath.NeoNodePointerFactory;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.RandomStringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.StructrConf;
import org.structr.core.app.StructrApp;
import org.structr.schema.Configuration;

//~--- classes ----------------------------------------------------------------

/**
 * Provides access to the service layer in structr.
 *
 * <p>
 * Use the {@see #command} method to obtain an instance of the desired command.
 * </p>
 *
 * @author Christian Morgner
 */
public class Services {

	private static final Logger logger        = Logger.getLogger(StructrApp.class.getName());
	private static StructrConf DEFAULT_CONFIG = null;

	// Application constants
	public static final String APPLICATION_TITLE             = "application.title";
	public static final String APPLICATION_HOST              = "application.host";
	public static final String APPLICATION_HTTP_PORT         = "application.http.port";
	public static final String APPLICATION_HTTPS_PORT        = "application.https.port";
	public static final String APPLICATION_HTTPS_ENABLED     = "application.https.enabled";
	public static final String APPLICATION_FTP_PORT          = "application.ftp.port";
	public static final String APPLICATION_KEYSTORE_PATH     = "application.keystore.path";
	public static final String APPLICATION_KEYSTORE_PASSWORD = "application.keystore.password";
	public static final String BASE_PATH                     = "base.path";
	public static final String CONFIGURED_SERVICES           = "configured.services";
	public static final String CONFIG_FILE_PATH              = "configfile.path";
	public static final String DATABASE_PATH                 = "database.path";
	public static final String FILES_PATH                    = "files.path";
	public static final String LOG_DATABASE_PATH             = "log.database.path";
	public static final String FOREIGN_TYPE                  = "foreign.type.key";
	public static final String NEO4J_SHELL_ENABLED           = "neo4j.shell.enabled";
	public static final String LOG_SERVICE_INTERVAL          = "structr.logging.interval";
	public static final String LOG_SERVICE_THRESHOLD         = "structr.logging.threshold";
	public static final String SMTP_HOST                     = "smtp.host";
	public static final String SMTP_PORT                     = "smtp.port";
	public static final String SMTP_USER                     = "smtp.user";
	public static final String SMTP_PASSWORD                 = "smtp.password";
	public static final String SUPERUSER_USERNAME            = "superuser.username";
	public static final String SUPERUSER_PASSWORD            = "superuser.password";
	public static final String TCP_PORT                      = "tcp.port";
	public static final String TMP_PATH                      = "tmp.path";
	public static final String UDP_PORT                      = "udp.port";
	public static final String JSON_INDENTATION              = "json.indentation";
	public static final String GEOCODING_PROVIDER            = "geocoding.provider";
	public static final String GEOCODING_LANGUAGE            = "geocoding.language";
	public static final String GEOCODING_APIKEY              = "geocoding.apikey";
	public static final String CONFIGURATION                 = "configuration";
	public static final String TESTING                       = "testing";
	
	// singleton instance
	private static Services singletonInstance = null;
	
	// non-static members
	private final Map<String, Object> attributes       = new ConcurrentHashMap<>(10, 0.9f, 8);
	private final Map<Class, Service> serviceCache     = new ConcurrentHashMap<>(10, 0.9f, 8);
	private final Set<Class> registeredServiceClasses  = new LinkedHashSet<>();
	private final Set<String> configuredServiceClasses = new LinkedHashSet<>();
	private StructrConf structrConf                    = null;
	private Configuration configuration                = null;
	private boolean initializationDone                 = false;
	private String configuredServiceNames              = null;
	private String configurationClass                  = null;
	private String resources                           = null;

	private Services() { }
	
	public static Services getInstance() {
		
		if (singletonInstance == null) {
			
			singletonInstance = new Services();

			try {
				final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
				if (trace != null && trace.length > 0) {
					final Class mainClass = Class.forName(trace[trace.length-1].getClassName());
					singletonInstance.resources = mainClass.getProtectionDomain().getCodeSource().getLocation().toString();
				}
				
			} catch (Throwable t) { t.printStackTrace(); }

			singletonInstance.initialize();
			
			
		}
		
		return singletonInstance;
	}
	
	public static Services getInstance(final StructrConf properties) {
		
		if (singletonInstance == null) {
			
			singletonInstance = new Services();

			try {
				final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
				if (trace != null && trace.length > 0) {
					final Class mainClass = Class.forName(trace[trace.length-1].getClassName());
					singletonInstance.resources = mainClass.getProtectionDomain().getCodeSource().getLocation().toString();
				}
				
			} catch (Throwable t) { t.printStackTrace(); }

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
	public <T extends Command> T command(SecurityContext securityContext, Class<T> commandType) {

		Class serviceClass = null;
		T command          = null;

		try {

			command = commandType.newInstance();
			command.setSecurityContext(securityContext);

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

				logger.log(Level.FINEST, "Initializing command ", commandType.getName());
				service.injectArguments(command);
			}

		} catch (Throwable t) {
			
			t.printStackTrace();
			
			logger.log(Level.SEVERE, "Exception while creating command " + commandType.getName(), t);
		}

		return (command);
	}

	private void initialize() {
		
		final StructrConf properties = new StructrConf(getDefaultConfiguration());
		final String configFileName  = "structr.conf";	// TODO: make configurable

		try {
			// load config file and merge with defaults
			final FileInputStream fis = new FileInputStream(configFileName);
			properties.load(fis);
			fis.close();

			// do not write merged config, causes file to lose comments, format and order
			
			
//			// write merged config file to disk
//			final FileOutputStream fos = new FileOutputStream(configFileName);
//			properties.store(fos, "Updated " + new SimpleDateFormat("yyyy/MM/dd - HH:mm").format(System.currentTimeMillis()));
//			fos.flush();
//			fos.close();

		} catch (IOException ioex) {
			
			logger.log(Level.WARNING, "Unable to read configuration file {0}: {1}", new Object[] { configFileName, ioex.getMessage() } );
		}

		// initialize structr with this configuration
		initialize(properties);
	}
	
	private void initialize(final StructrConf properties) {

		logger.log(Level.INFO, "Initializing service layer");

		configurationClass     = properties.getProperty(Services.CONFIGURATION);
		configuredServiceNames = properties.getProperty(Services.CONFIGURED_SERVICES);
		
		// create set of configured services
		configuredServiceClasses.addAll(Arrays.asList(configuredServiceNames.split("[ ,]+")));

		// if configuration is not yet established, instantiate it
		// this is the place where the service classes get the
		// opportunity to modify the default configuration
		getConfiguration();
		
		// store configuration for later use
		this.structrConf = properties;
		
		logger.log(Level.INFO, "Starting services");

		// initialize other services
		for (Class serviceClass : registeredServiceClasses) {

			if (Service.class.isAssignableFrom(serviceClass) && configuredServiceClasses.contains(serviceClass.getSimpleName())) {

				try {
					createService(serviceClass);
					
				} catch (Throwable t) {

					logger.log(Level.WARNING, "Exception while registering service {0}: {1}", new Object[] { serviceClass.getName(), t.getMessage() });
				}
			}
		}

		logger.log(Level.INFO, "{0} service(s) processed", serviceCache.size());
		registeredServiceClasses.clear();

		logger.log(Level.INFO, "Initialization complete");
		
		initializationDone = true;
	}
	
	public boolean isInitialized() {
		return initializationDone;
	}

	public void shutdown() {

		logger.log(Level.INFO, "Shutting down service layer");
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

				logger.log(Level.WARNING, "Failed to shut down {0}: {1}",
					new Object[] { service.getName(),
						t.getMessage() });
			}
		}

		serviceCache.clear();

		// shut down configuration provider
		configuration.shutdown();
		
		// clear singleton instance
		singletonInstance = null;
		
		logger.log(Level.INFO, "Finished shutdown of service layer");
	}

	/**
	 * Registers a service, enabling the service layer to automatically start
	 * autorun servies.
	 *
	 * @param serviceClass the service class to register
	 */
	public void registerServiceClass(Class serviceClass) {
		
		registeredServiceClasses.add(serviceClass);
		
		// let service instance visit default configuration
		try {
			
			Service service = (Service)serviceClass.newInstance();
			service.visitConfiguration(getDefaultConfiguration());
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public String getConfigurationValue(String key) {
		return getConfigurationValue(key, null);
	}
	
	public String getConfigurationValue(String key, String defaultValue) {
		return getStructrConf().getProperty(key, defaultValue);
	}
	
	public Configuration getConfiguration() {

		// instantiate configuration provider
		if (configuration == null) {

			// when executing tests, the configuration class may already exist,
			// so we don't instantiate it again since all the entities are already
			// known to the ClassLoader and we would miss the code in all the static
			// initializers.
			try {

				configuration = (Configuration)Class.forName(configurationClass).newInstance();
				configuration.initialize();

			} catch (Throwable t) {

				logger.log(Level.SEVERE, "Unable to instantiate schema provider of type {0}: {1}", new Object[] { configurationClass, t.getMessage() });
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
	 * @return 
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
	
	private Service createService(Class serviceClass) throws InstantiationException, IllegalAccessException {

		logger.log(Level.FINE, "Creating service ", serviceClass.getName());

		Service service = (Service) serviceClass.newInstance();
		service.initialize(getStructrConf());

		if (service instanceof RunnableService) {

			RunnableService runnableService = (RunnableService) service;
			
			if (runnableService.runOnStartup()) {

				logger.log(Level.FINER, "Starting RunnableService instance ", serviceClass.getName());

				// start RunnableService and cache it
				runnableService.startService();
				serviceCache.put(serviceClass, service);
			}

		} else if (service instanceof SingletonService) {

			// cache SingletonService
			serviceCache.put(serviceClass, service);
		}

		return service;
	}

	/**
	 * Return all registered services
	 *
	 * @return
	 */
	public List<Service> getServices() {

		List<Service> services = new LinkedList<>();
		for (Service service : serviceCache.values()) {
			services.add(service);
		}

		return services;
	}
	
	public <T extends Service> T getService(final Class<T> type) {
		return (T) serviceCache.get(type);
	}

	public String getResources() {
		return resources;
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
	 * @return 
	 */
	public boolean isReady(final Class serviceClass) {
                Service service = serviceCache.get(serviceClass);
                return (service != null && service.isRunning());
	}
	
	public StructrConf getStructrConf() {
		return structrConf;
	}
	
	public static StructrConf getDefaultConfiguration() {

		/*
		- StructrProperties, addValue, removeValue
		- HttpService, mandatory config for ui
		- unified (core) main class to start Structr
		*/
			
		if (DEFAULT_CONFIG == null) {
			
			DEFAULT_CONFIG = new StructrConf();
			
			DEFAULT_CONFIG.setProperty(CONFIGURATION,             ModuleService.class.getName());
			DEFAULT_CONFIG.setProperty(CONFIGURED_SERVICES,       "NodeService AgentService CronService");
			DEFAULT_CONFIG.setProperty(NEO4J_SHELL_ENABLED,       "true");
			DEFAULT_CONFIG.setProperty(JSON_INDENTATION,          "true");

			DEFAULT_CONFIG.setProperty(SUPERUSER_USERNAME,        "superadmin");
			DEFAULT_CONFIG.setProperty(SUPERUSER_PASSWORD,        RandomStringUtils.random(12));

			DEFAULT_CONFIG.setProperty(APPLICATION_TITLE,         "structr server");
			DEFAULT_CONFIG.setProperty(APPLICATION_HOST,          "0.0.0.0");
			DEFAULT_CONFIG.setProperty(APPLICATION_HTTP_PORT,     "8082");

			DEFAULT_CONFIG.setProperty(APPLICATION_HTTPS_ENABLED, "false");
			DEFAULT_CONFIG.setProperty(APPLICATION_HTTPS_PORT,    "8083");
			DEFAULT_CONFIG.setProperty(APPLICATION_FTP_PORT,      "8022");

			DEFAULT_CONFIG.setProperty(BASE_PATH,                 "");
			DEFAULT_CONFIG.setProperty(TMP_PATH,                  "/tmp");
			DEFAULT_CONFIG.setProperty(DATABASE_PATH,             "./db");
			DEFAULT_CONFIG.setProperty(FILES_PATH,                "./files");
			DEFAULT_CONFIG.setProperty(LOG_DATABASE_PATH,         "./logDb.dat");

			DEFAULT_CONFIG.setProperty(SMTP_HOST,                 "localhost");
			DEFAULT_CONFIG.setProperty(SMTP_PORT,                 "25");
			DEFAULT_CONFIG.setProperty(TCP_PORT,                  "54555");
			DEFAULT_CONFIG.setProperty(UDP_PORT,                  "57555");
		}
		
		return DEFAULT_CONFIG;
	}
}
