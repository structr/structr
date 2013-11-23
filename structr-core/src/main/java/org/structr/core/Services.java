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
import org.apache.commons.lang.StringUtils;

import org.structr.common.Path;
import org.structr.core.module.ModuleService;

//~--- JDK imports ------------------------------------------------------------

//import org.structr.common.xpath.NeoNodePointerFactory;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.RandomStringUtils;
import org.structr.common.SecurityContext;
import org.structr.core.app.StructrApp;
import org.structr.core.schema.Configuration;

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

	private static final Logger logger            = Logger.getLogger(StructrApp.class.getName());

	// Application constants
	public static final String APPLICATION_TITLE         = "application.title";
	public static final String APPLICATION_HOST          = "application.host";
	public static final String APPLICATION_HTTP_PORT     = "application.http.port";
	public static final String APPLICATION_HTTPS_PORT    = "application.https.port";
	public static final String APPLICATION_HTTPS_ENABLED = "application.https.enabled";
	public static final String REST_PATH                 = "application.rest.path";
	public static final String APPLICATION_FTP_PORT      = "application.ftp.port";

	// Keystore
	public static final String APPLICATION_KEYSTORE_PATH     = "application.keystore.path";
	public static final String APPLICATION_KEYSTORE_PASSWORD = "application.keystore.password";
	
	// Base constants
	public static final String BASE_PATH             = "base.path";
	public static final String CONFIGURED_SERVICES   = "configured.services";
	public static final String CONFIG_FILE_PATH      = "configfile.path";

	// Database-related constants
	public static final String DATABASE_PATH       = "database.path";
	public static final String FILES_PATH          = "files.path";
	public static final String LOG_DATABASE_PATH   = "log.database.path";
	public static final String FOREIGN_TYPE        = "foreign.type.key";
	public static final String NEO4J_SHELL_ENABLED = "neo4j.shell.enabled";
	
	// LogService-related constants
	public static final String LOG_SERVICE_INTERVAL  = "structr.logging.interval";
	public static final String LOG_SERVICE_THRESHOLD = "structr.logging.threshold";

	// Network-related constants
	public static final String SERVER_IP              = "server.ip";
	public static final String SMTP_HOST              = "smtp.host";
	public static final String SMTP_PORT              = "smtp.port";
	public static final String SMTP_USER              = "smtp.user";
	public static final String SMTP_PASSWORD          = "smtp.password";

	public static final String RESOURCES              = "resources";

	// Security-related constants
	public static final String SUPERUSER_USERNAME = "superuser.username";
	public static final String SUPERUSER_PASSWORD = "superuser.password";

	public static final String TCP_PORT           = "tcp.port";
	public static final String TMP_PATH           = "tmp.path";
	public static final String UDP_PORT           = "udp.port";
	
	public static final String JSON_OUTPUT_DEPTH  = "json.depth";
	public static final String JSON_INDENTATION   = "json.indentation";
	
	// geocoding
	public static final String GEOCODING_PROVIDER = "geocoding.provider";
	public static final String GEOCODING_LANGUAGE = "geocoding.language";
	public static final String GEOCODING_APIKEY   = "geocoding.apikey";
	
	// schema
	public static final String CONFIGURATION      = "configuration";
	
	// singleton instance
	private static Services singletonInstance = null;
	
	// non-static members
	private final Map<String, Object> attributes      = new ConcurrentHashMap<>(10, 0.9f, 8);
	private final Map<Class, Service> serviceCache    = new ConcurrentHashMap<>(10, 0.9f, 8);
	private final Set<Class> registeredServiceClasses = new LinkedHashSet<>();
	private final Set<Class> configuredServiceClasses = new LinkedHashSet<>();
	private Map<String, String> config                = null;
	private Configuration configuration               = null;
	private boolean initializationDone                = false;
	private String configurationClass                 = null;
	private String httpsEnabled                       = null;
	private String appTitle                           = null;
	private String appHost                            = null;
	private String appHttpPort                        = null;
	private String appHttpsPort                       = null;
	private String appFtpPort                         = null;
	private String keystorePath                       = null;
	private String keystorePassword                   = null;
	private String basePath                           = null;
	private String restPath                           = null;
	private String configFilePath                     = null;
	private String configuredServices                 = null;
	private String databasePath                       = null;
	private String logDatabasePath                    = null;
	private String filesPath                          = null;
	private String resources                          = null;
	private String serverIp                           = null;
	private String smtpHost                           = null;
	private String smtpPort                           = null;
	private String smtpUser                           = null;
	private String smtpPassword                       = null;
	private String superuserPassword                  = null;
	private String superuserUsername                  = null;
	private String tcpPort                            = null;
	private String tmpPath                            = null;
	private String udpPort                            = null;
	private String jsonDepth                          = null;

	private Services() { }
	
	public static Services getInstance(final Map<String, String> configuration) {
		
		if (singletonInstance == null) {
			singletonInstance = new Services();
		}

		if (configuration != null) {
			singletonInstance.initialize(configuration);
		}
		
		return singletonInstance;
	}
	
	public static Services getInstance() {
		return getInstance(null);
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

			if ((serviceClass != null) && isConfigured(serviceClass)) {

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

	private void initialize(final Map<String, String> initConfig) {

		config = initConfig;
		initialize();
	}

	private void initialize() {

		logger.log(Level.INFO, "Initializing service layer");

		if (config == null) {

			logger.log(Level.SEVERE, "Could not initialize service layer: Service configuration is null");

			return;
		}
		
		configFilePath      = getConfigValue(config, Services.CONFIG_FILE_PATH, "./structr.conf");
		configuredServices  = getConfigValue(config, Services.CONFIGURED_SERVICES, "NodeService AgentService CloudService CacheService LogService NotificationService");
		configurationClass  = getConfigValue(config, Services.CONFIGURATION, ModuleService.class.getName());
		appTitle            = getConfigValue(config, Services.APPLICATION_TITLE, "structr");
		appHost             = getConfigValue(config, Services.APPLICATION_HOST, "localhost");
		appHttpPort         = getConfigValue(config, Services.APPLICATION_HTTP_PORT, "8082");
		appHttpsPort        = getConfigValue(config, Services.APPLICATION_HTTPS_PORT, "8083");
		appFtpPort          = getConfigValue(config, Services.APPLICATION_FTP_PORT, "8022");
		httpsEnabled        = getConfigValue(config, Services.APPLICATION_HTTPS_ENABLED, "false");
		keystorePath        = getConfigValue(config, Services.APPLICATION_KEYSTORE_PATH, "");
		keystorePassword    = getConfigValue(config, Services.APPLICATION_KEYSTORE_PASSWORD, "");
		tmpPath             = getConfigValue(config, Services.TMP_PATH, "/tmp");
		basePath            = getConfigValue(config, Services.BASE_PATH, ".");
		restPath            = getConfigValue(config, Services.REST_PATH, "/structr/rest");
		databasePath        = getConfigValue(config, Services.DATABASE_PATH, "./db");
		logDatabasePath     = getConfigValue(config, Services.LOG_DATABASE_PATH, "./logdb.dat");
		filesPath           = getConfigValue(config, Services.FILES_PATH, "./files");
		resources           = getConfigValue(config, Services.RESOURCES, "");
		serverIp            = getConfigValue(config, Services.SERVER_IP, "127.0.0.1");
		tcpPort             = getConfigValue(config, Services.TCP_PORT, "54555");
		udpPort             = getConfigValue(config, Services.UDP_PORT, "57555");
		smtpHost            = getConfigValue(config, Services.SMTP_HOST, "localhost");
		smtpPort            = getConfigValue(config, Services.SMTP_PORT, "25");
		smtpUser            = getConfigValue(config, Services.SMTP_USER, "");
		smtpPassword        = getConfigValue(config, Services.SMTP_PASSWORD, "");
		superuserUsername   = getConfigValue(config, Services.SUPERUSER_USERNAME, "superadmin");
		superuserPassword   = getConfigValue(config, Services.SUPERUSER_PASSWORD, RandomStringUtils.randomAlphanumeric(12));
		jsonDepth           = getConfigValue(config, Services.JSON_OUTPUT_DEPTH, "3");
		
		logger.log(Level.INFO, "Starting services");
		
		// if configuration is not yet established, instantiate it
		getConfiguration();

		// initialize other services
		for (Class serviceClass : registeredServiceClasses) {

			if (Service.class.isAssignableFrom(serviceClass) && isConfigured(serviceClass)) {

				try {
					createService(serviceClass);
				} catch (Throwable t) {

					t.printStackTrace();
					
					logger.log(Level.WARNING, "Exception while registering service {0}: {1}",
						   new Object[] { serviceClass.getName(),
								  t.getMessage() });
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
		
		logger.log(Level.INFO, "Finished shutdown of service layer");
	}

	/**
	 * Registers a service, enabling the service layer to automatically start
	 * autorun servies.
	 *
	 * @param serviceClass the service class to register
	 */
	public void registerServiceClass(Class serviceClass) {

		// register service classes except module service (which is initialized manually)
		if (!serviceClass.equals(ModuleService.class)) {
			registeredServiceClasses.add(serviceClass);
		}
	}

	public String getConfigurationValue(String key) {
		
		if(config != null) {
			return config.get(key);
		}
		return null;
	}
	
	public String getConfigurationValue(String key, String defaultValue) {
		
		String value = getConfigurationValue(key);
		if(value == null) {
			return defaultValue;
		}

		return value;
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

		// initialize newly created service (applies to all subclasses)
		logger.log(Level.FINEST, "Initializing service ", serviceClass.getName());
		service.initialize(config);

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

	//~--- get methods ----------------------------------------------------

	/**
	 * Return the application title
	 * @return 
	 */
	public String getApplicationTitle() {
		return appTitle;
	}

	/**
	 * Return the application host name
	 * @return 
	 */
	public String getApplicationHost() {
		return appHost;
	}
	
	/**
	 * Return the application HTTP port
	 * @return 
	 */
	public String getHttpPort() {
		return appHttpPort;
	}
	
	/**
	 * Return the application FTP port
	 * @return 
	 */
	public String getFtpPort() {
		return appFtpPort;
	}

	/**
	 * Return HTTPS enabled
	 * @return 
	 */
	public boolean getHttpsEnabled() {
		return "true".equals(httpsEnabled);
	}

	/**
	 * Return the application HTTPS port
	 * @return 
	 */
	public String getHttpsPort() {
		return appHttpsPort;
	}

	/**
	 * Return the keystore path
	 *
	 * @return
	 */
	public String getKeystorePath() {
		return keystorePath;
	}

	/**
	 * Return the rest path
	 *
	 * @return
	 */
	public String getRestPath() {
		return restPath;
	}
	
	/**
	 * Return the keystore password
	 *
	 * @return
	 */
	public String getKeystorePassword() {
		return keystorePassword;
	}

	/**
	 * Return the base path
	 *
	 * @return
	 */
	public String getBasePath() {
		return getPath(Path.Base);
	}

	/**
	 * Return the configured services
	 *
	 * @return
	 */
	public String getConfiguredServices() {
		return configuredServices;
	}

	/**
	 * Return the tmp path. This is the directory where the
	 * temporary files are stored
	 * @return 
	 */
	public String getTmpPath() {
		return getPath(Path.Temp);
	}

	/**
	 * Return the configuration file path.
	 * @return 
	 */
	public String getConfigFilePath() {
		return getPath(Path.ConfigFile);
	}

	/**
	 * Return the database path. This is the directory where the
	 * database files are stored.
	 * @return 
	 */
	public String getDatabasePath() {
		return getPath(Path.Database);
	}

	/**
	 * Return the log database path. This is the file path of the
	 * log database.
	 * @return 
	 */
	public String getLogDatabasePath() {
		return getPath(Path.LogDatabase);
	}

	/**
	 * Return the file path. This is the directory where the
	 * binary files of file and image nodes are stored.
	 * @return 
	 */
	public String getFilesPath() {
		return getPath(Path.Files);
	}

	/**
	 * Return the resources. This is a list of files that
	 * need to be scanned for entities, agents, services etc.
	 * @return 
	 */
	public String getResources() {
		return resources;
	}

	/**
	 * Return the local host address for the outside world
	 */
	public String getServerIP() {
		return serverIp;
	}

	/**
	 * Return the TCP port remote clients can connect to
	 * @return 
	 */
	public String getTcpPort() {
		return tcpPort;
	}

	/**
	 * Return the UDP port remote clients can connect to
	 * @return 
	 */
	public String getUdpPort() {
		return udpPort;
	}

	/**
	 * Return the SMTP host for sending out e-mails
	 * @return 
	 */
	public String getSmtpHost() {
		return smtpHost;
	}

	/**
	 * Return the SMTP port for sending out e-mails
	 * @return 
	 */
	public String getSmtpPort() {
		return smtpPort;
	}

	/**
	 * Return the SMTP user for sending out e-mails
	 * @return 
	 */
	public String getSmtpUser() {
		return smtpUser;
	}

	/**
	 * Return the SMTP user for sending out e-mails
	 * @return 
	 */
	public String getSmtpPassword() {
		return smtpPassword;
	}

	/**
	 * Return the superuser username
	 * @return 
	 */
	public String getSuperuserUsername() {
		return superuserUsername;
	}

	/**
	 * Return the superuser username
	 * @return 
	 */
	public String getSuperuserPassword() {
		return superuserPassword;
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

	public Map<String, String> getContext() {
		return config;
	}

	public String getConfigValue(final Map<String, String> config, final String key, final String defaultValue) {

		String value = StringUtils.strip(config.get(key));

		if (value != null) {
			return value;
		}

		return defaultValue;
	}

	public String getPath(final Path path) {

		String returnPath = null;

		switch (path) {

			case ConfigFile :
				returnPath = configFilePath;

				break;

			case Base :
				returnPath = basePath;

				break;

			case Rest :
				returnPath = restPath;

				break;

			case Database :
				returnPath = getAbsolutePath(databasePath);

				break;

			case LogDatabase :
				returnPath = getAbsolutePath(logDatabasePath);

				break;

			case Files :
				returnPath = getAbsolutePath(filesPath);

				break;

			case Temp :
				returnPath = getAbsolutePath(tmpPath);

				break;
		}

		return returnPath;
	}

	public String getFilePath(final Path path, final String... pathParts) {

		StringBuilder returnPath = new StringBuilder();
		String filePath   = getPath(path);

		returnPath.append(filePath);
		returnPath.append(filePath.endsWith("/")
			   ? ""
			   : "/");

		for (String pathPart : pathParts) {
			returnPath.append(pathPart);
		}

		return returnPath.toString();
	}

	public int getOutputNestingDepth() {
		
		try { return Integer.parseInt(jsonDepth); } catch(Throwable t) {}
		
		return 3;
	}
	
	private String getAbsolutePath(final String path) {

		if (path == null) {
			return null;
		}
		
		if (path.startsWith("/")) {
			return path;
		}

		StringBuilder absolutePath = new StringBuilder();

		absolutePath.append(basePath);
		absolutePath.append(basePath.endsWith("/")
			   ? ""
			   : "/");
		absolutePath.append(path);

		return (absolutePath.toString());
	}

	/**
	 * Return true if service is configured
	 *
	 * @param serviceClass
	 * @return
	 */
	private boolean isConfigured(final Class serviceClass) {

		if(!configuredServiceClasses.contains(serviceClass)) {

			// Try once to find service class in string list "configuredServices".
			// If already initialized, there is no need to check the string field
			// again, because this method is called once for each registered service
			// when structr starts.
			if (initializationDone) {
				return false;
			}
			
			boolean configurationContainsClass = StringUtils.contains(configuredServices, serviceClass.getSimpleName());

			if (configurationContainsClass) {
				configuredServiceClasses.add(serviceClass);
			}

			return configurationContainsClass;
		}
		
		return true;
	}

	/**
	 * Return true if the given service is available to be potentially used
	 * 
	 * @param serviceClass
	 * @return 
	 */
	public boolean isAvailable(final Class serviceClass) {
		return configuredServiceClasses.contains(serviceClass);
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
	
	private void readConfiguration() {
		
		logger.log(Level.INFO, "Servlet context created");

		Map<String, String> configMap = new ConcurrentHashMap<>(20, 0.9f, 8);
		String configFilePath         = "structr.conf"; // servletContext.getInitParameter(Services.CONFIG_FILE_PATH);

		configMap.put(Services.CONFIG_FILE_PATH, configFilePath);

		try {

			// load config file
			Properties properties = new Properties();

			properties.load(new FileInputStream(configFilePath));

			String configuredServices = properties.getProperty(Services.CONFIGURED_SERVICES);

			logger.log(Level.INFO, "Config file configured services: {0}", configuredServices);

			String appTitle = properties.getProperty(Services.APPLICATION_TITLE);

			logger.log(Level.INFO, "Config file application title: {0}", appTitle);

			String tmpPath = properties.getProperty(Services.TMP_PATH);

			logger.log(Level.INFO, "Config file temp path: {0}", tmpPath);

			String basePath = properties.getProperty(Services.BASE_PATH);

			logger.log(Level.INFO, "Config file base path: {0}", basePath);

			String databasePath = properties.getProperty(Services.DATABASE_PATH);

			logger.log(Level.INFO, "Config file database path: {0}", databasePath);

			String filesPath = properties.getProperty(Services.FILES_PATH);

			logger.log(Level.INFO, "Config file files path: {0}", filesPath);

			String serverIp = properties.getProperty(Services.SERVER_IP);

			logger.log(Level.INFO, "Config file server IP: {0}", serverIp);

			String tcpPort = properties.getProperty(Services.TCP_PORT);

			logger.log(Level.INFO, "Config file TCP port: {0}", tcpPort);

			String udpPort = properties.getProperty(Services.UDP_PORT);

			logger.log(Level.INFO, "Config file UDP port: {0}", udpPort);

			String superuserUsername = properties.getProperty(Services.SUPERUSER_USERNAME);

			logger.log(Level.INFO, "Config file superuser username: {0}", superuserUsername);

			String superuserPassword = properties.getProperty(Services.SUPERUSER_PASSWORD);

			logger.log(Level.INFO, "Config file superuser password: {0}", superuserPassword);

			if (configuredServices != null) {

				configMap.put(Services.CONFIGURED_SERVICES, configuredServices);
			}

			if (appTitle != null) {

				configMap.put(Services.APPLICATION_TITLE, appTitle);
			}

			if (tmpPath != null) {

				configMap.put(Services.TMP_PATH, tmpPath);
			}

			if (basePath != null) {

				configMap.put(Services.BASE_PATH, basePath);
			}

			if (databasePath != null) {

				configMap.put(Services.DATABASE_PATH, databasePath);
			}

			if (filesPath != null) {

				configMap.put(Services.FILES_PATH, filesPath);
			}

			if (tcpPort != null) {

				configMap.put(Services.TCP_PORT, tcpPort);
			}

			if (serverIp != null) {

				configMap.put(Services.SERVER_IP, serverIp);
			}

			if (udpPort != null) {

				configMap.put(Services.UDP_PORT, udpPort);
			}

			if (superuserUsername != null) {

				configMap.put(Services.SUPERUSER_USERNAME, superuserUsername);
			}

			if (superuserPassword != null) {

				configMap.put(Services.SUPERUSER_PASSWORD, superuserPassword);
			}

			// load all properties into context
			for (String name : properties.stringPropertyNames()) {

				if (!configMap.containsKey(name)) {

					configMap.put(name, properties.getProperty(name));
				}

			}
			
		} catch (Throwable t) {

			// handle error
			logger.log(Level.WARNING, "Could not inititialize all values");
		}

	}
}
