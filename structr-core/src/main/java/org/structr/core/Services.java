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

import java.util.Collections;
import org.apache.commons.lang.StringUtils;

import org.structr.common.Path;
import org.structr.core.module.InitializeModuleServiceCommand;
import org.structr.core.module.ModuleService;

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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;

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
	public static final String SERVLET_REAL_ROOT_PATH = "servlet.context";
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
	
	private static Map<String, String> config    = null;
	private static final Logger logger            = Logger.getLogger(Services.class.getName());

	private static final Map<String, Object> attributes      = new ConcurrentHashMap<>(10, 0.9f, 8);
	private static final Map<Class, Service> serviceCache    = new ConcurrentHashMap<>(10, 0.9f, 8);
	private static final Set<Class> registeredServiceClasses = new LinkedHashSet<>();
	private static final Set<Class> configuredServiceClasses = new LinkedHashSet<>();
	private static boolean initializationDone = false;
	private static String httpsEnabled;
	private static String appTitle;
	private static String appHost;
	private static String appHttpPort;
	private static String appHttpsPort;
	private static String appFtpPort;
	private static String keystorePath;
	private static String keystorePassword;
	private static String basePath;
	private static String restPath;
	private static String configFilePath;
	private static String configuredServices;
	private static String databasePath;
	private static String logDatabasePath;
	private static String filesPath;
	private static String resources;
	private static String serverIp;
	private static String smtpHost;
	private static String smtpPort;
	private static String smtpUser;
	private static String smtpPassword;
	private static String superuserPassword;
	private static String superuserUsername;
	private static String tcpPort;
	private static String tmpPath;
	private static String udpPort;
	private static String jsonDepth;

	//~--- methods --------------------------------------------------------

	/**
	 * Creates and returns a command of the given <code>type</code>. If a command is
	 * found, the corresponding service will be discovered and activated.
	 *
	 * @param <T>
	 * @param securityContext
	 * @param commandType the runtime type of the desired command
	 * @return the command
	 */
	private static <T extends Command> T command(SecurityContext securityContext, Class<T> commandType) {

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

	public static void initialize(final Map<String, String> initConfig) {

		config = initConfig;
		initialize();
	}

	public static void initialize() {

		logger.log(Level.INFO, "Initializing service layer");

		if (config == null) {

			logger.log(Level.SEVERE, "Could not initialize service layer: Service configuration is null");

			return;
		}

		configFilePath     = getConfigValue(config, Services.CONFIG_FILE_PATH, "./structr.conf");
		configuredServices = getConfigValue(config, Services.CONFIGURED_SERVICES,
			"ModuleService NodeService AgentService CloudService CacheService LogService NotificationService");
		appTitle          = getConfigValue(config, Services.APPLICATION_TITLE, "structr");
		appHost           = getConfigValue(config, Services.APPLICATION_HOST, "localhost");
		appHttpPort       = getConfigValue(config, Services.APPLICATION_HTTP_PORT, "8082");
		appHttpsPort      = getConfigValue(config, Services.APPLICATION_HTTPS_PORT, "8083");
		appFtpPort        = getConfigValue(config, Services.APPLICATION_FTP_PORT, "8022");
		httpsEnabled      = getConfigValue(config, Services.APPLICATION_HTTPS_ENABLED, "false");
		keystorePath      = getConfigValue(config, Services.APPLICATION_KEYSTORE_PATH, "");
		keystorePassword  = getConfigValue(config, Services.APPLICATION_KEYSTORE_PASSWORD, "");
		tmpPath           = getConfigValue(config, Services.TMP_PATH, "/tmp");
		basePath          = getConfigValue(config, Services.BASE_PATH, ".");
		restPath          = getConfigValue(config, Services.REST_PATH, "/structr/rest");
		databasePath      = getConfigValue(config, Services.DATABASE_PATH, "./db");
		logDatabasePath   = getConfigValue(config, Services.LOG_DATABASE_PATH, "./logdb.dat");
		filesPath         = getConfigValue(config, Services.FILES_PATH, "./files");
		resources         = getConfigValue(config, Services.RESOURCES, "");
		serverIp          = getConfigValue(config, Services.SERVER_IP, "127.0.0.1");
		tcpPort           = getConfigValue(config, Services.TCP_PORT, "54555");
		udpPort           = getConfigValue(config, Services.UDP_PORT, "57555");
		smtpHost          = getConfigValue(config, Services.SMTP_HOST, "localhost");
		smtpPort          = getConfigValue(config, Services.SMTP_PORT, "25");
		smtpUser          = getConfigValue(config, Services.SMTP_USER, "");
		smtpPassword      = getConfigValue(config, Services.SMTP_PASSWORD, "");
		superuserUsername = getConfigValue(config, Services.SUPERUSER_USERNAME, "superadmin");
		superuserPassword = getConfigValue(config, Services.SUPERUSER_PASSWORD, "");    // intentionally no default password!
		jsonDepth         = getConfigValue(config, Services.JSON_OUTPUT_DEPTH, "3");
		
		logger.log(Level.INFO, "Starting services");

		try {
			// initialize module service (which can be thought of as the root service)
			// securityContext can be null here, as the command is a null command
			Services.command(null, InitializeModuleServiceCommand.class).execute();

		} catch(FrameworkException fex) {
			
			// initialization failed!
			fex.printStackTrace();
		}

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
	
	public static boolean isInitialized() {
		return initializationDone;
	}

	public static void shutdown() {

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

//              serviceClassCache.clear();
		logger.log(Level.INFO, "Finished shutdown of service layer");
	}

	/**
	 * Registers a service, enabling the service layer to automatically start
	 * autorun servies.
	 *
	 * @param serviceClass the service class to register
	 */
	public static void registerServiceClass(Class serviceClass) {

		// register service classes except module service (which is initialized manually)
		if (!serviceClass.equals(ModuleService.class)) {
			registeredServiceClasses.add(serviceClass);
		}
	}

	public static String getConfigurationValue(String key) {
		
		if(config != null) {
			return config.get(key);
		}
		return null;
	}
	
	public static String getConfigurationValue(String key, String defaultValue) {
		
		String value = getConfigurationValue(key);
		if(value == null) {
			return defaultValue;
		}

		return value;
	}
	
	/**
	 * Store an attribute value in the service config
	 * 
	 * @param name
	 * @param value 
	 */
	public static void setAttribute(final String name, final Object value) {
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
	public static Object getAttribute(final String name) {
		return attributes.get(name);
	}
	
	/**
	 * Remove attribute value from service config
	 * 
	 * @param name 
	 */
	public static void removeAttribute(final String name) {
		attributes.remove(name);
	}
	
	private static Service createService(Class serviceClass) throws InstantiationException, IllegalAccessException {

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
	public static String getApplicationTitle() {
		return appTitle;
	}

	/**
	 * Return the application host name
	 * @return 
	 */
	public static String getApplicationHost() {
		return appHost;
	}
	
	/**
	 * Return the application HTTP port
	 * @return 
	 */
	public static String getHttpPort() {
		return appHttpPort;
	}
	
	/**
	 * Return the application FTP port
	 * @return 
	 */
	public static String getFtpPort() {
		return appFtpPort;
	}

	/**
	 * Return HTTPS enabled
	 * @return 
	 */
	public static boolean getHttpsEnabled() {
		return "true".equals(httpsEnabled);
	}

	/**
	 * Return the application HTTPS port
	 * @return 
	 */
	public static String getHttpsPort() {
		return appHttpsPort;
	}

	/**
	 * Return the keystore path
	 *
	 * @return
	 */
	public static String getKeystorePath() {
		return keystorePath;
	}

	/**
	 * Return the static rest path
	 *
	 * @return
	 */
	public static String getRestPath() {
		return restPath;
	}
	
	/**
	 * Return the keystore password
	 *
	 * @return
	 */
	public static String getKeystorePassword() {
		return keystorePassword;
	}

	/**
	 * Return the static base path
	 *
	 * @return
	 */
	public static String getBasePath() {
		return getPath(Path.Base);
	}

	/**
	 * Return the configured services
	 *
	 * @return
	 */
	public static String getConfiguredServices() {
		return configuredServices;
	}

	/**
	 * Return the static tmp path. This is the directory where the
	 * temporary files are stored
	 * @return 
	 */
	public static String getTmpPath() {
		return getPath(Path.Temp);
	}

	/**
	 * Return the configuration file path.
	 * @return 
	 */
	public static String getConfigFilePath() {
		return getPath(Path.ConfigFile);
	}

	/**
	 * Return the database path. This is the directory where the
	 * database files are stored.
	 * @return 
	 */
	public static String getDatabasePath() {
		return getPath(Path.Database);
	}

	/**
	 * Return the log database path. This is the file path of the
	 * log database.
	 * @return 
	 */
	public static String getLogDatabasePath() {
		return getPath(Path.LogDatabase);
	}

	/**
	 * Return the file path. This is the directory where the
	 * binary files of file and image nodes are stored.
	 * @return 
	 */
	public static String getFilesPath() {
		return getPath(Path.Files);
	}

	/**
	 * Return the resources. This is a list of files that
	 * need to be scanned for entities, agents, services etc.
	 * @return 
	 */
	public static String getResources() {
		return resources;
	}

	/**
	 * Return the local host address for the outside world
	 */
	public static String getServerIP() {
		return serverIp;
	}

	/**
	 * Return the TCP port remote clients can connect to
	 * @return 
	 */
	public static String getTcpPort() {
		return tcpPort;
	}

	/**
	 * Return the UDP port remote clients can connect to
	 * @return 
	 */
	public static String getUdpPort() {
		return udpPort;
	}

	/**
	 * Return the SMTP host for sending out e-mails
	 * @return 
	 */
	public static String getSmtpHost() {
		return smtpHost;
	}

	/**
	 * Return the SMTP port for sending out e-mails
	 * @return 
	 */
	public static String getSmtpPort() {
		return smtpPort;
	}

	/**
	 * Return the SMTP user for sending out e-mails
	 * @return 
	 */
	public static String getSmtpUser() {
		return smtpUser;
	}

	/**
	 * Return the SMTP user for sending out e-mails
	 * @return 
	 */
	public static String getSmtpPassword() {
		return smtpPassword;
	}

	/**
	 * Return the superuser username
	 * @return 
	 */
	public static String getSuperuserUsername() {
		return superuserUsername;
	}

	/**
	 * Return the superuser username
	 * @return 
	 */
	public static String getSuperuserPassword() {
		return superuserPassword;
	}

	/**
	 * Return all registered services
	 *
	 * @return
	 */
	public static List<Service> getServices() {

		List<Service> services = new LinkedList<>();
		for (Service service : serviceCache.values()) {
			services.add(service);
		}

		return services;
	}
	
	public static <T extends Service> T getService(final Class<T> type) {
		return (T) serviceCache.get(type);
	}

	public static Map<String, String> getContext() {
		return config;
	}

	public static String getConfigValue(final Map<String, String> config, final String key, final String defaultValue) {

		String value = StringUtils.strip(config.get(key));

		if (value != null) {
			return value;
		}

		return value;
	}

	public static String getPath(final Path path) {

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

	public static String getFilePath(final Path path, final String... pathParts) {

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

	public static int getOutputNestingDepth() {
		
		try { return Integer.parseInt(jsonDepth); } catch(Throwable t) {}
		
		return 3;
	}
	
	private static String getAbsolutePath(final String path) {

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
	private static boolean isConfigured(final Class serviceClass) {

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
	public static boolean isAvailable(final Class serviceClass) {
		return configuredServiceClasses.contains(serviceClass);
	}
        
	/**
	 * Return true if the given service is ready to be used,
         * means initialized and running.
	 * 
	 * @param serviceClass
	 * @return 
	 */
	public static boolean isReady(final Class serviceClass) {
                Service service = serviceCache.get(serviceClass);
                return (service != null && service.isRunning());
                
	}
	
}
