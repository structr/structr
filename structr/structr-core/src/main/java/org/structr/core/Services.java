/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.Path;
import org.structr.core.module.InitializeModuleServiceCommand;
import org.structr.core.module.ModuleService;
//import org.structr.common.xpath.NeoNodePointerFactory;

/**
 * Main entry point for access to services in structr.
 *
 * <p>
 * Use the {@see #command} method to obtain an instance of the desired command.
 * </p>
 *
 * @author cmorgner
 */
public class Services {

    private static final Logger logger = Logger.getLogger(Services.class.getName());
    // Application constants
    public static final String APPLICATION_TITLE = "application.title";
    public static final String CONFIG_FILE_PATH = "configfile.path";
    public static final String SERVLET_CONTEXT = "servlet.context";
    public static final String TMP_PATH = "tmp.path";
    public static final String BASE_PATH = "base.path";
    // Database-related constants
    public static final String DATABASE_PATH = "database.path";
    public static final String FILES_PATH = "files.path";
    // Network-related constants
    public static final String SERVER_IP = "server.ip";
    public static final String TCP_PORT = "tcp.port";
    public static final String UDP_PORT = "udp.port";
    public static final String SMTP_HOST = "smtp.host";
    public static final String SMTP_PORT = "smtp.port";
    // LogService-related constants
    public static final String LOG_SERVICE_INTERVAL = "structr.logging.interval";
    public static final String LOG_SERVICE_THRESHOLD = "structr.logging.threshold";
    // ModuleService-related constants
    public static final String MODULES_PATH = "modules.path";
//    public static final String ENTITY_PACKAGES = "entity.packages";
    public static final String STRUCTR_PAGE_PREDICATE = "structr.page.predicate";
    // Security-related constants
    public static final String SUPERUSER_USERNAME = "superuser.username";
    public static final String SUPERUSER_PASSWORD = "superuser.password";


//    private static final Map<Class, Class> serviceClassCache = new ConcurrentHashMap<Class, Class>(10, 0.9f, 8);
    private static final Map<Class, Service> serviceCache = new ConcurrentHashMap<Class, Service>(10, 0.9f, 8);
    private static final Set<Class> registeredServiceClasses = new LinkedHashSet<Class>();
    private static Map<String, Object> context = null;
    private static String basePath;
    private static String databasePath;
    private static String filesPath;
    private static String appTitle;
    private static String modulesPath;
    private static String configFilePath;
    private static String tmpPath;
    private static String serverIp;
    private static String tcpPort;
    private static String udpPort;
    private static String smtpHost;
    private static String smtpPort;
    private static String superuserUsername;
    private static String superuserPassword;

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
     * Return the file path. This is the directory where the
     * binary files of file and image nodes are stored.
     */
    public static String getFilesPath() {
        return getPath(Path.Files);
    }

    /**
     * Return the modules path. This is the directory where the
     * modules are stored.
     */
    public static String getModulesPath() {
        return getPath(Path.Modules);
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
     * Creates and returns a command of the given <code>type</code>. If a command is
     * found, the corresponding service will be discovered and activated.
     *
     * @param commandType the runtime type of the desired command
     * @return the command
     * @throws NoSuchCommandException
     */
    public static Command command(Class commandType) {

        logger.log(Level.FINER, "Creating command ", commandType.getName());

        Class serviceClass = null;
        Command command = null;

        try {
            command = (Command) commandType.newInstance();
            serviceClass = command.getServiceClass();

            if (serviceClass != null) {
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
            logger.log(Level.SEVERE, "Exception while creating command " + commandType.getName(), t);
        }

        return (command);
    }

    public static void initialize(Map<String, Object> envContext) {
        context = envContext;
        initialize();
    }

    public static void initialize() {

        logger.log(Level.INFO, "Initializing service layer");

        if (context == null) {
            logger.log(Level.SEVERE, "Could not initialize service layer: Service context is null");
            return;
        }
	
	configFilePath = getConfigValue(context, Services.CONFIG_FILE_PATH, "/opt/structr/structr.conf");

        appTitle = getConfigValue(context, Services.APPLICATION_TITLE, "structr");
        tmpPath = getConfigValue(context, Services.TMP_PATH, "/tmp");
        basePath = getConfigValue(context, Services.BASE_PATH, "/opt/structr");
        databasePath = getConfigValue(context, Services.DATABASE_PATH, "/opt/structr/db");
        filesPath = getConfigValue(context, Services.FILES_PATH, "/opt/structr/files");
        modulesPath = getConfigValue(context, Services.MODULES_PATH, "/opt/structr/modules");
        serverIp = getConfigValue(context, Services.SERVER_IP, "127.0.0.1");
        tcpPort = getConfigValue(context, Services.TCP_PORT, "54555");
        udpPort = getConfigValue(context, Services.UDP_PORT, "57555");
        smtpHost = getConfigValue(context, Services.SMTP_HOST, "localhost");
        smtpPort = getConfigValue(context, Services.SMTP_PORT, "25");
        superuserUsername = getConfigValue(context, Services.SUPERUSER_USERNAME, "superadmin");
        superuserPassword = getConfigValue(context, Services.SUPERUSER_PASSWORD, ""); // intentionally no default password!

        logger.log(Level.INFO, "Starting services");

	// initialize module service (which can be thought of as the root service)
	Services.command(InitializeModuleServiceCommand.class).execute();

	// initialize other services
	for(Class serviceClass : registeredServiceClasses) {
	    if (Service.class.isAssignableFrom(serviceClass)) {
		    try {
			    createService(serviceClass);
		    } catch (Throwable t) {
			    logger.log(Level.WARNING, "Exception while registering service {0}: {1}", new Object[]{
					    serviceClass.getName(),
					    t.getMessage()
				    });
		    }
	    }
	}

	logger.log(Level.INFO, "{0} service(s) processed", registeredServiceClasses.size());
	registeredServiceClasses.clear();
	
	logger.log(Level.INFO, "Initialization complete");
    }

    public static void shutdown() {

        logger.log(Level.INFO, "Shutting down service layer");

        for (Iterator<Service> it = serviceCache.values().iterator(); it.hasNext();) {
            Service service = it.next();

            if (service instanceof RunnableService) {
                RunnableService runnableService = (RunnableService) service;
                runnableService.stopService();
            }

            service.shutdown();
        }

        serviceCache.clear();
//        serviceClassCache.clear();

        logger.log(Level.INFO, "Finished shutdown of service layer");
    }

    /**
     * Return all registered services
     *
     * @return
     */
    public static List<Service> getServices() {

        List<Service> services = new LinkedList<Service>();

        for (Iterator<Service> it = serviceCache.values().iterator(); it.hasNext();) {
            Service service = it.next();
            services.add(service);
        }
        return services;
    }

    /**
     * Registers a service, enabling the service layer to automatically start
     * autorun servies.
     * 
     * @param serviceClass the service class to register
     */
    public static void registerServiceClass(Class serviceClass) {
	    // register service classes except module service (which is initialized manually)
	    if(!serviceClass.equals(ModuleService.class)) {
		registeredServiceClasses.add(serviceClass);
	    }
    }
    
    /**
     * Return all agents
     *
     * @return
    public static List<Service> getAgents() {

        List<Service> services = new LinkedList<Service>();

        for (Iterator<Service> it = serviceCache.values().iterator(); it.hasNext();) {
            Service service = it.next();
            if (service instanceof AgentService) {
                services.add(service);
            }
        }
        return services;
    }
     */

    public static void setContext(final Map<String, Object> envContext) {
        context = envContext;
    }

    public static Map<String, Object> getContext() {
        return context;
    }

    public static String getConfigValue(Map<String, Object> context, String key, String defaultValue) {
        Object value = context.get(key);
        String ret = defaultValue;

        if (value != null) {
            ret = value.toString();
        }

        return (ret);
    }

    public static String getPath(Path path) {
        String ret = null;

        switch (path) {

            case ConfigFile:
                ret = configFilePath;
                break;

            case Base:
                ret = basePath;
                break;

            case Database:
                ret = getAbsolutePath(databasePath);
                break;

            case Files:
                ret = getAbsolutePath(filesPath);
                break;

            case Modules:
                ret = getAbsolutePath(modulesPath);
                break;

            case Temp:
                ret = getAbsolutePath(tmpPath);
                break;
        }

        return (ret);
    }

    public static String getFilePath(Path path, String... pathParts) {
        StringBuilder ret = new StringBuilder();
        String filePath = getPath(path);

        ret.append(filePath);
        ret.append(filePath.endsWith("/") ? "" : "/");

        for (String pathPart : pathParts) {
            ret.append(pathPart);
        }

        return (ret.toString());
    }

    // <editor-fold defaultstate="collapsed" desc="private methods">
    private static Service createService(Class serviceClass) throws InstantiationException, IllegalAccessException {

        logger.log(Level.FINE, "Creating service ", serviceClass.getName());
        Service service = (Service) serviceClass.newInstance();

        // initialize newly created service (applies to all subclasses)
        logger.log(Level.FINEST, "Initializing service ", serviceClass.getName());
        service.initialize(context);

        if (service instanceof RunnableService) {

		RunnableService runnableService = (RunnableService)service;
		if(runnableService.runOnStartup()) {

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

    private static String getAbsolutePath(String path) {
        if (path.startsWith("/")) {
            return (path);
        }

        StringBuilder ret = new StringBuilder();

        ret.append(basePath);
        ret.append(basePath.endsWith("/") ? "" : "/");
        ret.append(path);

        return (ret.toString());
    }
    // </editor-fold>
}
