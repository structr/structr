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

import org.apache.commons.lang.StringUtils;

import org.structr.common.Path;
import org.structr.core.module.InitializeModuleServiceCommand;
import org.structr.core.module.ModuleService;

//~--- JDK imports ------------------------------------------------------------

//import org.structr.common.xpath.NeoNodePointerFactory;
import java.util.Iterator;
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
 * @author cmorgner
 */
public class Services {

	// Application constants
	public static final String APPLICATION_TITLE   = "application.title";
	public static final String BASE_PATH           = "base.path";
	public static final String CONFIGURED_SERVICES = "configured.services";
	public static final String CONFIG_FILE_PATH    = "configfile.path";

	// Database-related constants
	public static final String DATABASE_PATH = "database.path";
	public static final String FILES_PATH    = "files.path";
	public static final String LOG_DATABASE_PATH = "log.database.path";
	public static final String REST_PATH     = "application.rest.path";
	public static final String FOREIGN_TYPE = "foreign.type.key";
	
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
	
	// geocoding
	public static final String GEOCODING_PROVIDER = "geocoding.provider";
	public static final String GEOCODING_LANGUAGE = "geocoding.language";
	public static final String GEOCODING_APIKEY   = "geocoding.apikey";
	
	private static Map<String, String> context    = null;
	private static final Logger logger            = Logger.getLogger(Services.class.getName());

	private static final Map<String, Object> attributes      = new ConcurrentHashMap<String, Object>(10, 0.9f, 8);
	private static final Map<Class, Service> serviceCache    = new ConcurrentHashMap<Class, Service>(10, 0.9f, 8);
	private static final Set<Class> registeredServiceClasses = new LinkedHashSet<Class>();
	private static final Set<Class> configuredServiceClasses = new LinkedHashSet<Class>();
	private static boolean initializationDone = false;
	private static String appTitle;
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
	 * @param commandType the runtime type of the desired command
	 * @return the command
	 * @throws NoSuchCommandException
	 */
	public static <T extends Command> T command(SecurityContext securityContext, Class<T> commandType) {

		logger.log(Level.FINER, "Creating command ", commandType.getName());

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

	public static void initialize(Map<String, String> envContext) {

		context = envContext;
		initialize();
	}

	public static void initialize() {

		logger.log(Level.INFO, "Initializing service layer");

		if (context == null) {

			logger.log(Level.SEVERE, "Could not initialize service layer: Service context is null");

			return;
		}

		configFilePath     = getConfigValue(context, Services.CONFIG_FILE_PATH, "./structr.conf");
		configuredServices = getConfigValue(context, Services.CONFIGURED_SERVICES,
			"ModuleService NodeService AgentService CloudService CacheService LogService NotificationService");
		appTitle          = getConfigValue(context, Services.APPLICATION_TITLE, "structr");
		tmpPath           = getConfigValue(context, Services.TMP_PATH, "/tmp");
		basePath          = getConfigValue(context, Services.BASE_PATH, ".");
		restPath          = getConfigValue(context, Services.REST_PATH, "/structr/rest");
		databasePath      = getConfigValue(context, Services.DATABASE_PATH, "./db");
		logDatabasePath   = getConfigValue(context, Services.LOG_DATABASE_PATH, "./logdb.dat");
		filesPath         = getConfigValue(context, Services.FILES_PATH, "./files");
		resources         = getConfigValue(context, Services.RESOURCES, "");
		serverIp          = getConfigValue(context, Services.SERVER_IP, "127.0.0.1");
		tcpPort           = getConfigValue(context, Services.TCP_PORT, "54555");
		udpPort           = getConfigValue(context, Services.UDP_PORT, "57555");
		smtpHost          = getConfigValue(context, Services.SMTP_HOST, "localhost");
		smtpPort          = getConfigValue(context, Services.SMTP_PORT, "25");
		smtpUser          = getConfigValue(context, Services.SMTP_USER, "");
		smtpPassword      = getConfigValue(context, Services.SMTP_PASSWORD, "");
		superuserUsername = getConfigValue(context, Services.SUPERUSER_USERNAME, "superadmin");
		superuserPassword = getConfigValue(context, Services.SUPERUSER_PASSWORD, "");    // intentionally no default password!
		jsonDepth         = getConfigValue(context, Services.JSON_OUTPUT_DEPTH, "3");
		
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
		EntityContext.init();

		logger.log(Level.INFO, "Initialization complete");
		
		initializationDone = true;
	}
	
	public static boolean isInitialized() {
		return initializationDone;
	}

	public static void shutdown() {

		logger.log(Level.INFO, "Shutting down service layer");

		// FIXME: services need to be stopped in reverse order!
		for (Iterator<Service> it = serviceCache.values().iterator(); it.hasNext(); ) {

			Service service = it.next();

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

	public static Object getConfigurationValue(String key) {
		if(context != null) {
			return context.get(key);
		}
		return null;
	}
	
	public static String getConfigurationValue(String key, String defaultValue) {
		
		Object value = getConfigurationValue(key);
		if(value == null) {
			return defaultValue;
		}

		return value.toString();
	}
	
	/**
	 * Store an attribute value in the service context
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
	 * Retrieve attribute value from service context
	 * 
	 * @param name
	 * @return 
	 */
	public static Object getAttribute(final String name) {
		return attributes.get(name);
	}
	
	/**
	 * Remove attribute value from service context
	 * 
	 * @param name
	 * @return 
	 */
	public static void removeAttribute(final String name) {
		attributes.remove(name);
	}
	
	private static Service createService(Class serviceClass) throws InstantiationException, IllegalAccessException {

		logger.log(Level.FINE, "Creating service ", serviceClass.getName());

		Service service = (Service) serviceClass.newInstance();

		// initialize newly created service (applies to all subclasses)
		logger.log(Level.FINEST, "Initializing service ", serviceClass.getName());
		service.initialize(context);

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
	 * Return the static application title
	 */
	public static String getApplicationTitle() {
		return appTitle;
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
	 * Return the static rest path
	 *
	 * @return
	 */
	public static String getRestPath() {
		return getPath(Path.Rest);
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
	 */
	public static String getTmpPath() {
		return getPath(Path.Temp);
	}

	/**
	 * Return the configuration file path.
	 */
	public static String getConfigFilePath() {
		return getPath(Path.ConfigFile);
	}

	/**
	 * Return the database path. This is the directory where the
	 * database files are stored.
	 */
	public static String getDatabasePath() {
		return getPath(Path.Database);
	}

	/**
	 * Return the log database path. This is the file path of the
	 * log database.
	 */
	public static String getLogDatabasePath() {
		return getPath(Path.LogDatabase);
	}

	/**
	 * Return the file path. This is the directory where the
	 * binary files of file and image nodes are stored.
	 */
	public static String getFilesPath() {
		return getPath(Path.Files);
	}

	/**
	 * Return the resources. This is a list of files that
	 * need to be scanned for entities, agents, services etc.
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
	 */
	public static String getTcpPort() {
		return tcpPort;
	}

	/**
	 * Return the UDP port remote clients can connect to
	 */
	public static String getUdpPort() {
		return udpPort;
	}

	/**
	 * Return the SMTP host for sending out e-mails
	 */
	public static String getSmtpHost() {
		return smtpHost;
	}

	/**
	 * Return the SMTP port for sending out e-mails
	 */
	public static String getSmtpPort() {
		return smtpPort;
	}

	/**
	 * Return the SMTP user for sending out e-mails
	 */
	public static String getSmtpUser() {
		return smtpUser;
	}

	/**
	 * Return the SMTP user for sending out e-mails
	 */
	public static String getSmtpPassword() {
		return smtpPassword;
	}

	/**
	 * Return the superuser username
	 */
	public static String getSuperuserUsername() {
		return superuserUsername;
	}

	/**
	 * Return the superuser username
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

		List<Service> services = new LinkedList<Service>();

		for (Iterator<Service> it = serviceCache.values().iterator(); it.hasNext(); ) {

			Service service = it.next();

			services.add(service);
		}

		return services;
	}
	
	public static <T extends Service> T getService(Class<T> type) {
		return (T)serviceCache.get(type);
	}

	public static Map<String, String> getContext() {
		return context;
	}

	public static String getConfigValue(Map<String, String> context, String key, String defaultValue) {

		String value = StringUtils.strip(context.get(key));

		if (value != null) {
			return value;
		}

		return value;
	}

	public static String getPath(Path path) {

		String ret = null;

		switch (path) {

			case ConfigFile :
				ret = configFilePath;

				break;

			case Base :
				ret = basePath;

				break;

			case Rest :
				ret = restPath;

				break;

			case Database :
				ret = getAbsolutePath(databasePath);

				break;

			case LogDatabase :
				ret = getAbsolutePath(logDatabasePath);

				break;

			case Files :
				ret = getAbsolutePath(filesPath);

				break;

			case Temp :
				ret = getAbsolutePath(tmpPath);

				break;
		}

		return (ret);
	}

	public static String getFilePath(Path path, String... pathParts) {

		StringBuilder ret = new StringBuilder();
		String filePath   = getPath(path);

		ret.append(filePath);
		ret.append(filePath.endsWith("/")
			   ? ""
			   : "/");

		for (String pathPart : pathParts) {
			ret.append(pathPart);
		}

		return (ret.toString());
	}

	public static int getOutputNestingDepth() {
		
		try { return Integer.parseInt(jsonDepth); } catch(Throwable t) {}
		
		return 3;
	}
	
	private static String getAbsolutePath(String path) {

		if (path.startsWith("/")) {
			return (path);
		}

		StringBuilder ret = new StringBuilder();

		ret.append(basePath);
		ret.append(basePath.endsWith("/")
			   ? ""
			   : "/");
		ret.append(path);

		return (ret.toString());
	}

	/**
	 * Return true if service is configured
	 *
	 * @param serviceClass
	 * @return
	 */
	private static boolean isConfigured(final Class serviceClass) {

		if(!configuredServiceClasses.contains(serviceClass)) {

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

	//~--- set methods ----------------------------------------------------

	/**
	 * Return all agents
	 *
	 * @return
	 * public static List<Service> getAgents() {
	 *
	 *   List<Service> services = new LinkedList<Service>();
	 *
	 *   for (Iterator<Service> it = serviceCache.values().iterator(); it.hasNext();) {
	 *       Service service = it.next();
	 *       if (service instanceof AgentService) {
	 *           services.add(service);
	 *       }
	 *   }
	 *   return services;
	 * }
	 */
	public static void setContext(final Map<String, String> envContext) {
		context = envContext;
	}
}
