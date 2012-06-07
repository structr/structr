/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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



package org.structr.core.module;

import org.apache.commons.lang.StringUtils;

import org.structr.common.Path;
import org.structr.core.Command;
import org.structr.core.Module;
import org.structr.core.Predicate;
import org.structr.core.Service;
import org.structr.core.Services;
import org.structr.core.SingletonService;
import org.structr.core.agent.Agent;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.GenericNode;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Modifier;

import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.structr.core.*;
import org.structr.core.entity.AbstractRelationship;

//~--- classes ----------------------------------------------------------------

/**
 * Modules need to be installed and uninstalled manually
 * This service keeps an index of installed / activated modules for efficient access
 * files:
 *  - $BASEDIR/modules/modules.conf             -> properties file
 *
 * The entity class cache needs to be initialized with the structr core entities even
 * when no modules exist.
 *
 *
 * @author Christian Morgner
 */
public class ModuleService implements SingletonService {

	private static final String MODULES_CONF			= "modules.conf";
	private static final Logger logger				= Logger.getLogger(ModuleService.class.getName());
	private static final Set<String> nodeEntityPackages		= new LinkedHashSet<String>();
	private static final Set<String> relationshipPackages		= new LinkedHashSet<String>();
	private static final Map<String, Class> nodeEntityClassCache	= new ConcurrentHashMap<String, Class>(100, 0.9f, 8);
	private static final Map<String, Class> relationshipClassCache	= new ConcurrentHashMap<String, Class>(10, 0.9f, 8);
	private static final Map<String, Class> agentClassCache		= new ConcurrentHashMap<String, Class>(10, 0.9f, 8);
	private static final Map<String, Set<Class>> interfaceCache	= new ConcurrentHashMap<String, Set<Class>>(10, 0.9f, 8);
	private static final Set<String> pagePackages			= new LinkedHashSet<String>();
	private static final Set<String> agentPackages			= new LinkedHashSet<String>();

	//~--- fields ---------------------------------------------------------

	private String servletContextRealRootPath	= null;
	private Predicate structrPagePredicate		= null;
	private boolean initialized			= false;

	//~--- methods --------------------------------------------------------

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

	public void reload() {

		logger.log(Level.INFO, "Reloading modules..");

		// reload everything..
		initialized = false;

		nodeEntityClassCache.clear();
		relationshipClassCache.clear();
		agentClassCache.clear();
		initializeModules();
	}

	/**
	 * Loads and activates the given module.
	 *
	 * @param moduleName the file name of the module to activate
	 * @param recreateIndex whether to recreate the index file
	 */
	public void activateModule(String moduleName) {

		boolean success = true;

		try {

			Module module = loadModule(moduleName);

			if (module != null) {

				importModule(module);

			} else {

				logger.log(Level.WARNING, "Module was null!");

			}

		} catch (IOException ioex) {

			logger.log(Level.WARNING, "Error loading module {0}: {1}", new Object[] { moduleName, ioex });

			success = false;
		}

		// save changes to modules.conf
		Properties modulesConf = getModulesConf();

		if (modulesConf != null) {

			if (success) {

				modulesConf.setProperty(moduleName, "active");

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
			Module module = loadModule(moduleName);

			if (module != null) {

				unimportIndex(module);

			}

		} catch (IOException ioex) {

			logger.log(Level.WARNING, "Error unloading module {0}: {1}", new Object[] { moduleName, ioex });

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

	// <editor-fold defaultstate="collapsed" desc="interface SingletonService">
	@Override
	public void injectArguments(Command command) {

		if (command != null) {

			command.setArgument("moduleService", this);

		}
	}

	@Override
	public void initialize(Map<String, String> context) {

		this.servletContextRealRootPath = context.get(Services.SERVLET_REAL_ROOT_PATH);

		initializeModules();
	}

	@Override
	public void shutdown() {

		nodeEntityClassCache.clear();
		relationshipClassCache.clear();
		agentClassCache.clear();
	}

	private void saveModulesConf(Properties modulesConf) {

		String modulesConfPath = Services.getFilePath(Path.Modules, MODULES_CONF);

		if (modulesConfPath != null) {

			File modulesConfFile = new File(modulesConfPath);

			try {

				FileOutputStream fos = new FileOutputStream(modulesConfFile);

				modulesConf.store(fos, "");
				fos.flush();
				fos.close();

			} catch (IOException ioex) {
				logger.log(Level.WARNING, "Could not write {0}: {1}", new Object[] { MODULES_CONF, ioex });
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

			activateModule(activeModuleName);

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
	private Module loadModule(String moduleName) throws IOException {

		return createModuleIndex(moduleName);
	}

	/**
	 * Processes the information from the given module and makes them available for the
	 * service layer.
	 *
	 * @param module the module to process
	 *
	 * @throws IOException
	 */
	private void importModule(Module module) throws IOException {

		String modulePath = module.getModulePath();
		ZipFile zipFile   = new ZipFile(modulePath);

		// 0.: iterate over libraries and deploy them
		Set<String> libraries = module.getLibraries();

		for (String library : libraries) {

			String destinationPath = createResourceFileName(servletContextRealRootPath, "WEB-INF/lib", library);

			try {

				ZipEntry entry          = zipFile.getEntry(library);
				InputStream inputStream = zipFile.getInputStream(entry);
				File destinationFile    = new File(destinationPath);

				deployFile(inputStream, destinationFile);
				logger.log(Level.INFO, "Deploying library {0} to {1}", new Object[] { entry.getName(), destinationFile });

			} catch (Throwable t) {

				// ignore!
				// logger.log(Level.WARNING, "Error deploying {0} to {1}: {2}", new Object[] { entryName, destinationPath, t } );
			}

		}

		// 1a.: iterate over raw class names and deploy them
		Set<String> classes = module.getClasses();

		for (final String className : classes) {

			logger.log(Level.FINE, "Deploying class {0} ", className);

			try {

				// deploy class file to WEB-INF/classes
				String sourcePath      = className.replaceAll("[\\.]+", "/").concat(".class");
				String destinationFile = createResourceFileName(servletContextRealRootPath, "WEB-INF/classes", sourcePath);

				logger.log(Level.FINE, "sourcePath: {0}, destinationFile: {1}", new Object[] { sourcePath, destinationFile });

				ZipEntry entry = zipFile.getEntry(sourcePath);

				if (entry != null) {

					InputStream inputStream = zipFile.getInputStream(entry);

					if (inputStream != null) {

						deployFile(inputStream, new File(destinationFile));

					} else {

						logger.log(Level.WARNING, "Invalid input stream: {0}", sourcePath);

					}

				} else {

					// do not log as this can occur when classes from WEB-INF/classes are examined here
					// logger.log(Level.WARNING, "Invalid entry: {0}", sourcePath);
				}
			} catch (Throwable t) {

				// ignore
				logger.log(Level.WARNING, "error deploying class {0}: {1}", new Object[] { className, t });
			}

		}

		// 1b.: iterate over properties files and deploy them
		Set<String> properties = module.getProperties();

		for (final String propertiesName : properties) {

			try {

				// deploy properties file to WEB-INF/classes
				String sourcePath      = propertiesName.replaceAll("[\\.]+", "/").concat(".properties");
				String destinationFile = createResourceFileName(servletContextRealRootPath, "WEB-INF/classes", sourcePath);
				ZipEntry entry         = zipFile.getEntry(sourcePath);

				if (entry != null) {

					InputStream inputStream = zipFile.getInputStream(entry);

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
				logger.log(Level.WARNING, "error deploying properties file {0}: {1}", new Object[] { propertiesName, t });
			}

		}

		// 2.: instantiate classes (this needs to be a distinct step because otherwise we wouldn't be able to instantiate classes with nested classes!)
		for (final String className : classes) {

			logger.log(Level.FINE, "Instantiating class {0} ", className);

			try {

				// instantiate class..
				Class clazz = Class.forName(className);

				logger.log(Level.FINE, "Class {0} instantiated: {1}", new Object[] { className, clazz });

				if (!Modifier.isAbstract(clazz.getModifiers())) {

					// register node entity classes
					if (AbstractNode.class.isAssignableFrom(clazz)) {

						EntityContext.init(clazz);

						String simpleName = clazz.getSimpleName();
						String fullName   = clazz.getName();

						nodeEntityClassCache.put(simpleName, clazz);
						nodeEntityPackages.add(fullName.substring(0, fullName.lastIndexOf(".")));


						for(Class interfaceClass : clazz.getInterfaces()) {

							String interfaceName = interfaceClass.getSimpleName();

							Set<Class> classesForInterface = interfaceCache.get(interfaceName);
							if(classesForInterface == null) {
								classesForInterface = new LinkedHashSet<Class>();
								interfaceCache.put(interfaceName, classesForInterface);
							}

							classesForInterface.add(clazz);
						}
						
					}

					// register entity classes
					if (AbstractRelationship.class.isAssignableFrom(clazz)) {

						String simpleName = clazz.getSimpleName();
						String fullName   = clazz.getName();

						relationshipClassCache.put(simpleName, clazz);
						relationshipPackages.add(fullName.substring(0, fullName.lastIndexOf(".")));

					}

					// register services
					if (Service.class.isAssignableFrom(clazz)) {

						Services.registerServiceClass(clazz);

					}

					// register agents
					if (Agent.class.isAssignableFrom(clazz)) {

						String simpleName = clazz.getSimpleName();
						String fullName   = clazz.getName();

						agentClassCache.put(simpleName, clazz);
						agentPackages.add(fullName.substring(0, fullName.lastIndexOf(".")));

					}

					// register page packages
					if (structrPagePredicate.evaluate(clazz)) {

						String fullName    = clazz.getName();
						String packageName = fullName.substring(0, fullName.lastIndexOf("."));

						pagePackages.add(packageName);

						// this did the trick!
						String parentPackage = packageName.substring(0, packageName.lastIndexOf("."));

						pagePackages.add(parentPackage);

					}
				}
			} catch (Throwable t) {

				// ignore
//                              logger.log(Level.WARNING, "error instantiating class {0}: {1}", new Object[]{className, t});
//                              t.printStackTrace(System.out);
			}

			// 3.: iterate over resources and deploy them
			Set<String> resources = module.getResources();

			for (String resource : resources) {

				String destinationPath = createResourceFileName(servletContextRealRootPath, resource);

				try {

					ZipEntry entry          = zipFile.getEntry(resource);
					InputStream inputStream = zipFile.getInputStream(entry);
					File destinationFile    = new File(destinationPath);

					deployFile(inputStream, destinationFile);

				} catch (Throwable t) {

					// ignore!
					// logger.log(Level.WARNING, "Error deploying {0} to {1}: {2}", new Object[] { entryName, destinationPath, t } );
				}

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

			String destinationPath = createResourceFileName(servletContextRealRootPath, resource);

			undeployFile(resource, destinationPath);

		}

		// 2a.: iterate over properties and remove them from cache
		// (maybe mark them as disabled)
		Set<String> properties = module.getProperties();

		for (String propertyName : properties) {

			String destinationPath = createResourceFileName(servletContextRealRootPath, propertyName);

			undeployFile(propertyName, destinationPath);

		}

		// 2b.: iterate over raw class names and remove them from cache
		// (maybe mark them as disabled)
		Set<String> classes = module.getClasses();

		for (String className : classes) {

			try {

				Class clazz       = Class.forName(className);
				String simpleName = clazz.getSimpleName();
				String fullName   = clazz.getName();

				if (AbstractNode.class.isAssignableFrom(clazz)) {

					nodeEntityClassCache.remove(simpleName);
					nodeEntityPackages.remove(fullName.substring(0, fullName.lastIndexOf(".")));

				} else if (AbstractRelationship.class.isAssignableFrom(clazz)) {

					relationshipClassCache.remove(simpleName);
					relationshipPackages.remove(fullName.substring(0, fullName.lastIndexOf(".")));

				} else if (Agent.class.isAssignableFrom(clazz)) {

					agentClassCache.remove(simpleName);
					agentPackages.remove(fullName.substring(0, fullName.lastIndexOf(".")));

				} else if ((structrPagePredicate != null) && structrPagePredicate.evaluate(clazz)) {

					pagePackages.remove(fullName.substring(0, fullName.lastIndexOf(".")));

				}

			} catch (Throwable t) {

				// ignore
				logger.log(Level.WARNING, "error instantiating class {0}: {1}", new Object[] { className, t });
			}

			// 3.: iterate over libraries and delete
			Set<String> libraries = module.getLibraries();

			for (String library : libraries) {

				String destinationPath = createResourceFileName(servletContextRealRootPath, "WEB-INF/lib", library);

				undeployFile(library, destinationPath);

			}

		}
	}

	/**
	 * Scans the Zip file for the given moduleName and creates an index file with
	 * the information from it.
	 *
	 * @param moduleName the name of the module
	 *
	 * @return the index for the given module
	 *
	 * @throws IOException
	 */
	private Module createModuleIndex(String moduleName) throws IOException {

		String libPath = servletContextRealRootPath.concat("/WEB-INF/lib");

		if (libPath == null) {

			return null;

		}

		String modulePath = libPath.concat("/").concat(moduleName);
		File moduleFile   = new File(modulePath);

		// create module
		ClickModule ret        = new ClickModule(modulePath);
		Set<String> classes    = ret.getClasses();
		Set<String> properties = ret.getProperties();
		Set<String> resources  = ret.getResources();
		Set<String> libraries  = ret.getLibraries();
		ZipFile zipFile        = new ZipFile(moduleFile);

		// conventions that might be useful here:
		// ignore entries beginning with meta-inf/
		// handle entries beginning with images/ as IMAGE
		// handle entries beginning with pages/ as PAGES
		// handle entries ending with .jar as libraries, to be deployed to WEB-INF/lib
		// handle other entries as potential page and/or entity classes
		// .. to be extended
		// (entries that end with "/" are directories)
		for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements(); ) {

			ZipEntry entry   = entries.nextElement();
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

			} else if (entryName.toLowerCase().endsWith(".jar")) {

				libraries.add(entryName);

			} else {

				// add resources to module
				if (!entryName.endsWith("/")) {

					resources.add(entryName);

				}
			}

		}

		// add entries from WEB-INF/classes
		File classesDir = new File(servletContextRealRootPath.concat("/WEB-INF/classes"));

		addClassesRecursively(classesDir, classes);

		return ret;
	}

	/**
	 * Creates an absolute file name for the given resource, located inside the root directory
	 * of this web application.
	 *
	 * @param realPath the real path
	 * @param name the name of the resource
	 *
	 * @return an absolute path name for the given resource inside the web app root directory
	 */
	private String createResourceFileName(String realPath, String... pathParts) {

		StringBuilder ret = new StringBuilder(100);

		ret.append(servletContextRealRootPath);

		for (int i = 0; i < pathParts.length; i++) {

			ret.append(File.separator);
			ret.append(pathParts[i]);

		}

		return (ret.toString());
	}

	private String createTargetDir(String servletRealRootPath, String targetDir) {

		if (servletRealRootPath == null) {

			throw new IllegalArgumentException("Null servlet real root path parameter");

		}

		String realTargetDir = servletRealRootPath + File.separator;

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

		String realTargetDir = createTargetDir(servletContextRealRootPath, targetDir);
		int index            = resource.lastIndexOf('/');
		String destination   = resource;

		if (index != -1) {

			destination = resource.substring(index + 1);

		}

		destination = realTargetDir + File.separator + destination;

		File toDelete = new File(destination);

		if (toDelete.exists()) {

			toDelete.delete();

		}
	}

	private void addClassesRecursively(File dir, Set<String> classes) {

		for (File file : dir.listFiles()) {

			if (file.isDirectory()) {

				addClassesRecursively(file, classes);

			} else {

				try {

					String fileEntry = file.getAbsolutePath();

					fileEntry = fileEntry.substring(0, fileEntry.length() - 6);
					fileEntry = fileEntry.substring(fileEntry.indexOf("/WEB-INF/classes") + 17);
					fileEntry = fileEntry.replaceAll("[/]+", ".");

					classes.add(fileEntry);

				} catch (Throwable t) {

					// ignore
				}

			}

		}
	}

	//~--- get methods ----------------------------------------------------

	public Set<String> getNodeEntityPackages() {
		return nodeEntityPackages;
	}

	public Set<String> getAgentPackages() {
		return agentPackages;
	}

	public Set<String> getCachedNodeEntityTypes() {
		return nodeEntityClassCache.keySet();
	}

	public Set<String> getCachedAgentTypes() {
		return agentClassCache.keySet();
	}

	public Set<Class> getClassesForInterface(String simpleName) {
		return interfaceCache.get(simpleName);
	}

	public Map<String, Class> getCachedNodeEntities() {
		return nodeEntityClassCache;
	}

	public Map<String, Class> getCachedAgents() {
		return agentClassCache;
	}

	public Set<String> getRelationshipPackages() {
		return relationshipPackages;
	}

	public Set<String> getCachedRelationshipTypes() {
		return relationshipClassCache.keySet();
	}

	public Map<String, Class> getCachedRelationships() {
		return relationshipClassCache;
	}

	public Class getNodeEntityClass(final String name) {

		Class ret = GenericNode.class;

		if ((name != null) && (!name.isEmpty())) {

			ret = nodeEntityClassCache.get(name);

			if (ret == null) {

				for (String possiblePath : nodeEntityPackages) {

					if (possiblePath != null) {

						try {

							Class nodeClass = Class.forName(possiblePath + "." + name);

							if (!Modifier.isAbstract(nodeClass.getModifiers())) {

								nodeEntityClassCache.put(name, nodeClass);
								
								// first match wins
								break;

							}

						} catch (ClassNotFoundException ex) {

							// ignore
						}

					}

				}

			}

		}

		return (ret);
	}

	public Class getRelationshipClass(final String name) {

		Class ret = AbstractNode.class;

		if ((name != null) && (name.length() > 0)) {

			ret = relationshipClassCache.get(name);

			if (ret == null) {

				for (String possiblePath : relationshipPackages) {

					if (possiblePath != null) {

						try {

							Class nodeClass = Class.forName(possiblePath + "." + name);

							if (!Modifier.isAbstract(nodeClass.getModifiers())) {

								relationshipClassCache.put(name, nodeClass);

								// first match wins
								break;

							}

						} catch (ClassNotFoundException ex) {

							// ignore
						}

					}

				}

			}

		}

		return (ret);
	}

	public Class getAgentClass(final String name) {

		Class ret = null;

		if ((name != null) && (name.length() > 0)) {

			ret = agentClassCache.get(name);

			if (ret == null) {

				for (String possiblePath : agentPackages) {

					if (possiblePath != null) {

						try {

							Class nodeClass = Class.forName(possiblePath + "." + name);

							agentClassCache.put(name, nodeClass);

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

	/**
	 * Scans the modules.conf file and returns a Set containing all modules that
	 * are active.
	 *
	 * @return a Set of active module names
	 */
	public Set<String> getActiveModuleNames() {

		Set<String> ret        = new LinkedHashSet<String>();
		Properties modulesConf = getModulesConf();

		if (modulesConf != null) {

			for (Entry entry : modulesConf.entrySet()) {

				String key   = entry.getKey().toString();
				String value = entry.getValue().toString();

				logger.log(Level.INFO, "Module: {0} ({1})", new Object[] { key, value });

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

		Set<String> ret        = new LinkedHashSet<String>();
		Properties modulesConf = getModulesConf();

		if (modulesConf != null) {

			for (Entry entry : modulesConf.entrySet()) {

				String key   = entry.getKey().toString();
				String value = entry.getValue().toString();

				logger.log(Level.INFO, "Module: {0} ({1})", new Object[] { key, value });

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

		Set<String> ret        = new LinkedHashSet<String>();
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

	@Override
	public String getName() {
		return (ModuleService.class.getSimpleName());
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="private methods">
	private Properties getModulesConf() {

		String modulesConfPath = Services.getFilePath(Path.Modules, MODULES_CONF);
		Properties ret         = new Properties();

		if (modulesConfPath != null) {

			File modulesConfFile = new File(modulesConfPath);

			if (modulesConfFile.exists()) {

				try {

					// config file exists, read existing, active modules
					FileInputStream fis = new FileInputStream(modulesConfFile);

					ret.load(fis);
					fis.close();
				} catch (IOException ioex) {
					logger.log(Level.WARNING, "Could not read {0}: {1}", new Object[] { MODULES_CONF, ioex });
				}

			} else {

				logger.log(Level.INFO, "{0} does not exist, creating it..", MODULES_CONF);

				try {
					modulesConfFile.createNewFile();
				} catch (IOException ioex) {
					logger.log(Level.WARNING, "Could not create {0}: {1}", new Object[] { MODULES_CONF, ioex });
				}

			}

		}

		return (ret);
	}

	@Override
	public boolean isRunning() {

		// we're always running :)
		return (true);
	}

	// </editor-fold>
}
