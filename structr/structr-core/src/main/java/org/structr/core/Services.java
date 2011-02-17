/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.Path;
import org.structr.core.agent.AgentService;
//import org.structr.common.xpath.NeoNodePointerFactory;

/**
 * Main entry point for access to services in structr.
 *
 * <p>
 * Use the {@see #createCommand} method to obtain an instance of the desired command.
 * </p>
 *
 * @author cmorgner
 */
public class Services {

    private static final Logger logger = Logger.getLogger(Services.class.getName());
    // application constants
    public static final String APPLICATION_TITLE = "application.title";
    public static final String CONFIG_FILE_PATH = "configfile.path";
    public static final String SERVLET_CONTEXT = "servlet.context";
    // database-related constants
    public static final String DATABASE_PATH_IDENTIFIER = "database.path";
    public static final String FILES_PATH_IDENTIFIER = "files.path";
    // LogService-related constants
    public static final String LOG_SERVICE_INTERVAL = "structr.logging.interval";
    public static final String LOG_SERVICE_THRESHOLD = "structr.logging.threshold";
    // ModuleService-related constants
    public static final String MODULES_PATH_IDENTIFIER = "modules.path";
    public static final String ENTITY_PACKAGES_IDENTIFIER = "entity.packages";
    public static final String STRUCTR_PAGE_PREDICATE = "structr.page.predicate";
    private static final Map<Class, Class> serviceClassCache = new ConcurrentHashMap<Class, Class>(5, 0.75f, 100);
    private static final Map<Class, Service> serviceCache = new ConcurrentHashMap<Class, Service>(5, 0.75f, 100);
    private static Map<String, Object> context = null;
    private static String basePath = "/opt/structr";
    private static String databasePath = "/opt/structr/structr-tfs2/files";
    private static String filesPath = "/opt/structr/structr-tfs2/files";
    private static String appTitle = "structr";
    private static String modulesPath = "/opt/structr/modules";
    private static String configFilePath = "/opt/structr/structr.conf";

    public static String getBasePath() {
        return (basePath);
    }

    /**
     * Return the static application title
     */
    public static String getApplicationTitle() {
        return appTitle;
    }

    /**
     * Return the static spatial layer name
     */
//    public static String getLayerName() {
//        return layerName;
//    }
    /**
     * Return the configuration file path.
     */
    public static String getConfigFilePath() {
        return configFilePath;
    }

    /**
     * Return the static database path. This is the directory where the
     * database files are stored.
     */
    public static String getDatabasePath() {
        return (getPath(Path.Database));
    }

    /**
     * Return the static file path. This is the directory where the
     * binary files of file and image nodes are stored.
     */
    public static String getFilesPath() {
        return (getPath(Path.Files));
    }

    /**
     * Return the static modules path. This is the directory where the
     * modules are stored.
     */
    public static String getModulesPath() {
        return (getPath(Path.Modules));
    }

    /* replaced by ModuleService
    public static String getEntityLocations() {
    return entityLocations;
    }

    public static String getEntityPackages() {
    return entityPackages;
    }

    public static Set<String> getCachedEntityTypes() {
    return entityClassCache.keySet();
    }
     */
    /**
     * Creates and returns a command of the given <code>type</code>. If a command is
     * found, the corresponding service will be discovered and activated.
     *
     * @param commandType the runtime type of the desired command
     * @return the command
     * @throws NoSuchCommandException
     */
    public static Command createCommand(Class commandType) {

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

        appTitle = getConfigValue(context, Services.APPLICATION_TITLE, "structr");
        databasePath = getConfigValue(context, Services.DATABASE_PATH_IDENTIFIER, "/opt/structr/structr-tfs2");
        filesPath = getConfigValue(context, Services.FILES_PATH_IDENTIFIER, "/opt/structr/structr-tfs2/files");
        modulesPath = getConfigValue(context, Services.MODULES_PATH_IDENTIFIER, "/opt/structr/modules");

        logger.log(Level.INFO, "Finished initialization of service layer");
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

//        entityClassCache.clear();
        serviceCache.clear();
        serviceClassCache.clear();

        logger.log(Level.INFO, "Finished shutdown of service layer");
    }

    /**
     * Return all registered services
     *
     * @return
     */
    public static List<Service> getServices() {

        List<Service> services = new ArrayList<Service>();

        for (Iterator<Service> it = serviceCache.values().iterator(); it.hasNext();) {
            Service service = it.next();
            services.add(service);
        }
        return services;
    }

    /**
     * Return all agents
     *
     * @return
     */
    public static List<Service> getAgents() {

        List<Service> services = new ArrayList<Service>();

        for (Iterator<Service> it = serviceCache.values().iterator(); it.hasNext();) {
            Service service = it.next();
            if (service instanceof AgentService) {
                services.add(service);
            }
        }
        return services;
    }

    public static void setContext(final Map<String, Object> envContext) {
        context = envContext;
    }

    /* replaced by ModuleService
    public static Class getEntityClass(final String name) {

    logger.log(Level.FINE, "name: {0}", name);

    Class nodeClass = entityClassCache.get(name);

    if (nodeClass == null && packages != null && name != null && !(name.isEmpty())) {

    for (String packagePath : packages) {

    try {
    nodeClass = Class.forName(packagePath + "." + name);
    // cache entry
    entityClassCache.put(name, nodeClass);
    break; // first match wins
    } catch (ClassNotFoundException ex) {
    logger.log(Level.FINE, "Class not found: {0}.{1}", new Object[]{packagePath, name});
    }
    }
    }

    return nodeClass;
    }
     */
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

            logger.log(Level.FINER, "Starting RunnableService instance ", serviceClass.getName());

            // start RunnableService and cache it
            RunnableService runnableService = (RunnableService) service;
            runnableService.startService();

            serviceCache.put(serviceClass, service);

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
