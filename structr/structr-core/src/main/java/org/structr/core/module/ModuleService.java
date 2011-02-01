/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import javax.servlet.ServletContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.structr.common.Path;
import org.structr.core.Command;
import org.structr.core.Module;
import org.structr.core.Predicate;
import org.structr.core.Services;
import org.structr.core.SingletonService;
import org.structr.core.entity.StructrNode;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Modules need to be installed and uninstalled manually
 * This service keeps an index of installed / activated modules for efficient access
 * files:
 *  - $BASEDIR/modules.conf			-> properties file
 *  - $BASEDIR/modules/*.jar			-> module JAR
 *  - $BASEDIR/modules/index/$NAME.index	-> serialized instance of Module.java
 *
 * The entity class cache needs to be initialized with the structr core entities even
 * when no modules exist.
 *
 *
 * @author Christian Morgner
 */
public class ModuleService implements SingletonService {

    private static final Logger logger = Logger.getLogger(ModuleService.class.getName());
    private static final String MODULES_CONF = "modules.conf";
    private static final Map<String, Class> entityClassCache = new ConcurrentHashMap<String, Class>(100, 0.75f, 100);
    private static final Set<String> entityPackages = new LinkedHashSet<String>();
    private static final Set<String> pagePackages = new LinkedHashSet<String>();
    private Predicate structrPagePredicate = null;
    private ServletContext servletContext = null;
    private boolean initialized = false;

    public void extendConfigDocument(Document xmlConfig) {
        if (initialized) {
            Element rootElement = xmlConfig.getDocumentElement();

            // TODO: configure pages, page packages and automapping in the given xmlConfig document!
            for (String pagePackage : pagePackages) {
                Attr attr = xmlConfig.createAttribute("package");
                attr.setValue(pagePackage);

                Element pagesElement = xmlConfig.createElement("pages");
                pagesElement.setAttributeNode(attr);

                rootElement.appendChild(pagesElement);

            }

        } else {
            logger.log(Level.INFO, "NOT extending Click config file, ModuleService is not initialized..");
        }
    }

    public Set<String> getEntityPackages() {
        return (entityPackages);
    }

    public Set<String> getCachedEntityTypes() {
        return entityClassCache.keySet();
    }

    public Map<String, Class> getCachedEntities() {
        return (entityClassCache);
    }

    public Class getEntityClass(final String name) {
        Class ret = null;

        if (name != null && name.length() > 0) {
            ret = entityClassCache.get(name);
            if (ret == null) {
                for (String possiblePath : entityPackages) {
                    if (possiblePath != null) {
                        try {
                            Class nodeClass = Class.forName(possiblePath + "." + name);
                            entityClassCache.put(name, nodeClass);

                            // first match wins
                            break;

                        } catch (ClassNotFoundException ex) {
                            // ignore
                        }
                    }
                }
            }
        }

        return (ret);
    }

    public void reload() {
        logger.log(Level.INFO, "Reloading modules..");

        // reload everything..
        initialized = false;
        entityClassCache.clear();

        initializeModules();
    }

    /**
     * Loads and activates the given module.
     *
     * @param moduleName the file name of the module to activate
     * @param recreateIndex whether to recreate the index file
     */
    public void activateModule(String moduleName, boolean recreateIndex) {
        boolean success = true;

        try {
            Module module = loadModule(moduleName, recreateIndex);
            if (module != null) {
                importIndex(module);

            } else {
                logger.log(Level.WARNING, "Module was null!");
            }

        } catch (IOException ioex) {
            logger.log(Level.WARNING, "Error loading module {0}: {1}", new Object[]{moduleName, ioex});
            success = false;
        }

        // save changes to modules.conf
        Properties modulesConf = getModulesConf();
        if (modulesConf != null) {
            if (success) {
                modulesConf.setProperty(moduleName, "active");

            } else {
                modulesConf.setProperty(moduleName, "inactive");

            }

            // save changes to disk
            saveModulesConf(modulesConf);

        } else {
            logger.log(Level.WARNING, "Could not write {0}", MODULES_CONF);
        }
    }

    /**
     * Reverses the actions of activateModule.
     *
     * @param moduleName
     */
    public void deactivateModule(String moduleName) {
        boolean success = true;

        try {
            // we need to load the module index in order to
            // be able to remove the deployed resources.
            Module module = loadModule(moduleName, false);
            if (module != null) {
                unimportIndex(module);

            }

            // delete index file
            unloadModule(moduleName);

        } catch (IOException ioex) {
            logger.log(Level.WARNING, "Error unloading module {0}: {1}", new Object[]{moduleName, ioex});
            success = false;
        }

        // save changes to modules.conf
        Properties modulesConf = getModulesConf();
        if (modulesConf != null) {
            if (success) {
                modulesConf.setProperty(moduleName, "inactive");

            }

            // save changes to disk
            saveModulesConf(modulesConf);

        } else {
            logger.log(Level.WARNING, "Could not write {0}", MODULES_CONF);
        }
    }

    /**
     * Scans the modules.conf file and returns a Set containing all modules that
     * are active.
     *
     * @return a Set of active module names
     */
    public Set<String> getActiveModuleNames() {
        Set<String> ret = new LinkedHashSet<String>();
        Properties modulesConf = getModulesConf();

        if (modulesConf != null) {
            for (Entry entry : modulesConf.entrySet()) {
                String key = entry.getKey().toString();
                String value = entry.getValue().toString();

                logger.log(Level.INFO, "Module: {0} ({1})", new Object[]{key, value});

                if ("active".equalsIgnoreCase(value)) {
                    ret.add(key);
                }
            }

        } else {
            logger.log(Level.WARNING, "Could not read {0}", MODULES_CONF);
        }

        return (ret);
    }

    /**
     * Scans the modules.conf file and returns a Set containing all modules that
     * are NOT active.
     *
     * @return a Set of inactive module names
     */
    public Set<String> getInactiveModuleNames() {
        Set<String> ret = new LinkedHashSet<String>();
        Properties modulesConf = getModulesConf();

        if (modulesConf != null) {
            for (Entry entry : modulesConf.entrySet()) {
                String key = entry.getKey().toString();
                String value = entry.getValue().toString();

                logger.log(Level.INFO, "Module: {0} ({1})", new Object[]{key, value});

                if ("inactive".equalsIgnoreCase(value)) {
                    ret.add(key);
                }
            }

        } else {
            logger.log(Level.WARNING, "Could not read {0}", MODULES_CONF);
        }

        return (ret);
    }

    /**
     * Returns the names of all modules in the config file.
     *
     * @return
     */
    public Set<String> getAllModuleNames() {
        Set<String> ret = new LinkedHashSet<String>();
        Properties modulesConf = getModulesConf();

        if (modulesConf != null) {
            for (Object keyObject : modulesConf.keySet()) {
                String key = keyObject.toString();
                ret.add(key);
            }

        } else {
            logger.log(Level.WARNING, "Could not read {0}", MODULES_CONF);
        }

        return (ret);
    }

    // <editor-fold defaultstate="collapsed" desc="interface SingletonService">
    @Override
    public void injectArguments(Command command) {
        if (command != null) {
            command.setArgument("moduleService", this);
        }
    }

    @Override
    public void initialize(Map<String, Object> context) {
        this.servletContext = (ServletContext) context.get(Services.SERVLET_CONTEXT);
        this.structrPagePredicate = (Predicate) context.get(Services.STRUCTR_PAGE_PREDICATE);

        if (this.structrPagePredicate == null) {
            throw new RuntimeException(Services.STRUCTR_PAGE_PREDICATE + " not set, aborting!");
        }

        // initialize default structr entites
        String entityPackagesFromContext = (String) context.get(Services.ENTITY_PACKAGES_IDENTIFIER);
        for (String entityPackageFromContext : entityPackagesFromContext.split("[, ]+")) {
            entityPackages.add(entityPackageFromContext);
        }

        initializeModules();
    }

    @Override
    public void shutdown() {
        entityClassCache.clear();
    }

    @Override
    public boolean isRunning() {
        // we're always running :)

        return (true);
    }

    @Override
    public String getName() {
        return (ModuleService.class.getName());
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="private methods">
    private Properties getModulesConf() {
        String modulesConfPath = Services.getFilePath(Path.Base, MODULES_CONF);
        Properties ret = new Properties();

        if (modulesConfPath != null) {
            File modulesConfFile = new File(modulesConfPath);
            if (modulesConfFile.exists()) {
                try {
                    // config file exists, read existing, active modules
                    FileInputStream fis = new FileInputStream(modulesConfFile);
                    ret.load(fis);
                    fis.close();

                } catch (IOException ioex) {
                    logger.log(Level.WARNING, "Could not read {0}: {1}", new Object[]{MODULES_CONF, ioex});
                }

            } else {
                logger.log(Level.INFO, "{0} does not exist, creating it..", MODULES_CONF);

                try {
                    modulesConfFile.createNewFile();

                } catch (IOException ioex) {
                    logger.log(Level.WARNING, "Could not create {0}: {1}", new Object[]{MODULES_CONF, ioex});

                }
            }
        }

        return (ret);
    }

    private void saveModulesConf(Properties modulesConf) {
        String modulesConfPath = Services.getFilePath(Path.Base, MODULES_CONF);

        if (modulesConfPath != null) {
            File modulesConfFile = new File(modulesConfPath);
            try {
                FileOutputStream fos = new FileOutputStream(modulesConfFile);
                modulesConf.store(fos, "");
                fos.flush();
                fos.close();

            } catch (IOException ioex) {
                logger.log(Level.WARNING, "Could not write {0}: {1}", new Object[]{MODULES_CONF, ioex});
            }
        }
    }

    /**
     * Initialized the modules.
     */
    private void initializeModules() {
        Set<String> activeModuleNames = getActiveModuleNames();

        logger.log(Level.INFO, "Found {0} active modules", activeModuleNames.size());

        for (String activeModuleName : activeModuleNames) {
            activateModule(activeModuleName, false);
        }

        initialized = true;
    }

    /**
     * Loads the given module, creating an index if it does not already exist.
     *
     * @param moduleName the name of the module
     * @param recreateIndex whether to recreate the index regardless of its existance
     *
     * @return the index for the given module
     *
     * @throws IOException
     */
    private Module loadModule(String moduleName, boolean recreateIndex) throws IOException {
        String indexPath = createIndexFileName(moduleName);
        File indexFile = new File(indexPath);
        Module ret = null;

        if (indexFile.exists() && !recreateIndex) {
            try {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(indexFile));
                ret = (Module) ois.readObject();

            } catch (Throwable t) {
                logger.log(Level.WARNING, "Unable to read module index from {0}: {1}", new Object[]{indexPath, t});
            }

        } else {
            ret = createModuleIndex(moduleName);
        }

        return (ret);
    }

    /**
     * Reverses the actions of loadModule, removing the module index file.
     *
     * @param moduleName the name of the module that is to be removed
     */
    private void unloadModule(String moduleName) {
        String indexPath = createIndexFileName(moduleName);
        File indexFile = new File(indexPath);

        if (indexFile.exists()) {
            indexFile.delete();
        }
    }

    /**
     * Processes the information from the given module and makes them available for the
     * service layer.
     *
     * @param module the module to process
     *
     * @throws IOException
     */
    private void importIndex(Module module) throws IOException {
        String modulePath = module.getModulePath();
        JarFile jarFile = new JarFile(modulePath);

        // 1a.: iterate over raw class names and deploy them
        Set<String> classes = module.getClasses();
        for (final String className : classes) {
            try {
                // deploy class file to WEB-INF/classes
                String sourcePath = className.replaceAll("[\\.]+", "/").concat(".class");
                String destinationFile = createResourceFileName(servletContext, "WEB-INF/classes", sourcePath);

                ZipEntry entry = jarFile.getEntry(sourcePath);
                if (entry != null) {
                    InputStream inputStream = jarFile.getInputStream(entry);
                    if (inputStream != null) {
                        deployFile(inputStream, new File(destinationFile));

                    } else {
                        logger.log(Level.WARNING, "Invalid input stream: {0}", sourcePath);
                    }

                } else {
                    logger.log(Level.WARNING, "Invalid entry: {0}", sourcePath);
                }

            } catch (Throwable t) {
                // ignore
                logger.log(Level.WARNING, "error deploying class {0}: {1}", new Object[]{className, t});
            }

        }

        // 1b.: iterate over properties files and deploy them
        Set<String> properties = module.getProperties();
        for (final String propertiesName : properties) {
            try {
                // deploy properties file to WEB-INF/classes
                String sourcePath = propertiesName.replaceAll("[\\.]+", "/").concat(".properties");
                String destinationFile = createResourceFileName(servletContext, "WEB-INF/classes", sourcePath);

                ZipEntry entry = jarFile.getEntry(sourcePath);
                if (entry != null) {
                    InputStream inputStream = jarFile.getInputStream(entry);
                    if (inputStream != null) {
                        deployFile(inputStream, new File(destinationFile));

                    } else {
                        logger.log(Level.WARNING, "Invalid input stream: {0}", sourcePath);
                    }

                } else {
                    logger.log(Level.WARNING, "Invalid entry: {0}", sourcePath);
                }

            } catch (Throwable t) {
                // ignore
                logger.log(Level.WARNING, "error deploying properties file {0}: {1}", new Object[]{propertiesName, t});
            }

        }

        // 2.: instantiate classes (this needs to be a distinct step because otherwise we wouldn't be able to instantiate classes with nested classes!)
        for (final String className : classes) {
            try {
                // instantiate class..
                Class clazz = Class.forName(className);
                if (StructrNode.class.isAssignableFrom(clazz)) {
                    String simpleName = clazz.getSimpleName();
                    String fullName = clazz.getName();

                    entityClassCache.put(simpleName, clazz);
                    entityPackages.add(fullName.substring(0, fullName.lastIndexOf(".")));

                }

                if (structrPagePredicate.evaluate(clazz)) {
                    String fullName = clazz.getName();

                    String packageName = fullName.substring(0, fullName.lastIndexOf("."));
                    pagePackages.add(packageName);

                    // this did the trick!
                    String parentPackage = packageName.substring(0, packageName.lastIndexOf("."));
                    pagePackages.add(parentPackage);

                }

            } catch (Throwable t) {
                // ignore
                logger.log(Level.WARNING, "error instantiating class {0}: {1}", new Object[]{className, t});
            }

        }

        // 3.: iterate over resources and deploy them
        Set<String> resources = module.getResources();
        for (String resource : resources) {
            String destinationPath = createResourceFileName(servletContext, resource);

            try {
                ZipEntry entry = jarFile.getEntry(resource);
                InputStream inputStream = jarFile.getInputStream(entry);
                File destinationFile = new File(destinationPath);

                deployFile(inputStream, destinationFile);

            } catch (Throwable t) {
                // ignore!
                // logger.log(Level.WARNING, "Error deploying {0} to {1}: {2}", new Object[] { entryName, destinationPath, t } );
            }
        }

    }

    /**
     * Processes the information from the given module and makes them available for the
     * service layer.
     *
     * @param module the module to process
     *
     * @throws IOException
     */
    private void unimportIndex(Module module) throws IOException {
        // 1.: iterate over resources and delete
        Set<String> resources = module.getResources();
        for (String resource : resources) {
            String destinationPath = createResourceFileName(servletContext, resource);

            undeployFile(resource, destinationPath);
        }

        // 2a.: iterate over properties and remove them from cache
        //     (maybe mark them as disabled)
        Set<String> properties = module.getProperties();
        for (String propertyName : properties) {
            String destinationPath = createResourceFileName(servletContext, propertyName);
            undeployFile(propertyName, destinationPath);
        }

        // 2b.: iterate over raw class names and remove them from cache
        //     (maybe mark them as disabled)
        Set<String> classes = module.getClasses();
        for (String className : classes) {
            try {
                Class clazz = Class.forName(className);
                if (StructrNode.class.isAssignableFrom(clazz)) {
                    String simpleName = clazz.getSimpleName();
                    String fullName = clazz.getName();

                    entityClassCache.remove(simpleName);
                    entityPackages.remove(fullName.substring(0, fullName.lastIndexOf(".")));

                } else if (structrPagePredicate != null && structrPagePredicate.evaluate(clazz)) {
                    String fullName = clazz.getName();

                    pagePackages.remove(fullName.substring(0, fullName.lastIndexOf(".")));
                }

            } catch (Throwable t) {
                // ignore
                logger.log(Level.WARNING, "error instantiating class {0}: {1}", new Object[]{className, t});
            }

        }
    }

    /**
     * Scans the JAR file for the given moduleName and creates an index file with
     * the information from it.
     *
     * @param moduleName the name of the module
     *
     * @return the index for the given module
     *
     * @throws IOException
     */
    private Module createModuleIndex(String moduleName) throws IOException {
        String modulePath = createModuleFileName(moduleName);
        String indexPath = createIndexFileName(moduleName);
        File moduleFile = new File(modulePath);
        File indexFile = new File(indexPath);

        // create module
        ClickModule ret = new ClickModule(modulePath);
        Set<String> classes = ret.getClasses();
        Set<String> properties = ret.getProperties();
        Set<String> resources = ret.getResources();

        JarFile jarFile = new JarFile(moduleFile);

        // conventions that might be useful here:
        // ignore entries beginning with meta-inf/
        // handle entries beginning with images/ as IMAGE
        // handle entries beginning with pages/ as PAGES
        // handle other entries as potential page and/or entity classes

        // .. to be extended

        // (entries that end with "/" are directories)

        for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            if (entryName.toLowerCase().startsWith("meta-inf/")) {
                continue;

            } else if (entryName.endsWith(".class")) {

                String fileEntry = entry.getName().replaceAll("[/]+", ".");

                // add class entry to Module
                classes.add(fileEntry.substring(0, fileEntry.length() - 6));

            } else if (entryName.endsWith(".properties")) {

                String fileEntry = entry.getName().replaceAll("[/]+", ".");

                // add property entry to Module
                properties.add(fileEntry.substring(0, fileEntry.length() - 11));

            } else {
                // add resources to module
                if (!entryName.endsWith("/")) {
                    resources.add(entryName);
                }
            }

        }

        // store index file
        try {
            indexFile.getParentFile().mkdirs();

            ObjectOutputStream ois = new ObjectOutputStream(new FileOutputStream(indexFile));
            ois.writeObject(ret);
            ois.flush();
            ois.close();

        } catch (Throwable t) {
            logger.log(Level.WARNING, "Unable to write module index from {0}: {1}", new Object[]{indexPath, t});
        }

        return (ret);
    }

    /**
     * Creates an absolute file name for the given module
     * @param moduleName the name of the module
     *
     * @return an absolute path for the given module inside the structr module directory
     */
    private String createModuleFileName(String moduleName) {
        return (Services.getFilePath(Path.Modules, moduleName));
    }

    /**
     * Creates an absolute index file name for the given module, located inside the structr
     * module directory.
     *
     * @param moduleName the name of the module
     *
     * @return an absolute index file path inside the structr module directory
     */
    private String createIndexFileName(String moduleName) {
        return (Services.getFilePath(Path.Modules, "index", "/", moduleName, ".index"));
    }

    /**
     * Creates an absolute file name for the given resource, located inside the root directory
     * of this web application.
     *
     * @param servletContext the servlet context
     * @param name the name of the resource
     *
     * @return an absolute path name for the given resource inside the web app root directory
     */
    private String createResourceFileName(ServletContext servletContext, String... pathParts) {
        StringBuilder ret = new StringBuilder();

        ret.append(servletContext.getRealPath("/"));

        for (int i = 0; i < pathParts.length; i++) {
            ret.append(File.separator);
            ret.append(pathParts[i]);
        }

        return (ret.toString());
    }

    private String createTargetDir(ServletContext servletContext, String targetDir) {
        if (servletContext == null) {
            throw new IllegalArgumentException("Null servletContext parameter");
        }

        String realTargetDir = servletContext.getRealPath("/") + File.separator;

        if (StringUtils.isNotBlank(targetDir)) {
            realTargetDir = realTargetDir + targetDir;
        }

        return (realTargetDir);
    }

    /**
     * Copies the data from the given InputStream to <code>destinationFile</code>
     *
     * @param inputStream the source stream
     * @param destinationFile the destination file
     *
     * @throws IOException
     */
    private void deployFile(InputStream inputStream, File destinationFile) throws IOException {
        if (destinationFile.exists()) {
            // logger.log(Level.INFO, "File {0} exists, not overwriting it", destinationFile.getAbsolutePath());

            return;
        }

        if (inputStream != null) {
            destinationFile.getParentFile().mkdirs();

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(destinationFile);
                byte[] buffer = new byte[1024];
                while (true) {
                    int length = inputStream.read(buffer);
                    if (length < 0) {
                        break;
                    }
                    fos.write(buffer, 0, length);
                }

            } finally {
                if (fos != null) {
                    fos.flush();
                    fos.close();
                }

                inputStream.close();
            }
        }
    }

    /**
     * Removes a previously deployed resource from the target directory.
     *
     * @param context the servlet context
     * @param resource the resource
     * @param targetDir the target directory
     * @throws IOException
     */
    private void undeployFile(String resource, String targetDir) throws IOException {
        String realTargetDir = createTargetDir(servletContext, targetDir);
        int index = resource.lastIndexOf('/');
        String destination = resource;

        if (index != -1) {
            destination = resource.substring(index + 1);
        }

        destination = realTargetDir + File.separator + destination;

        File toDelete = new File(destination);

        if (toDelete.exists()) {
            toDelete.delete();
        }
    }
    // </editor-fold>
}
