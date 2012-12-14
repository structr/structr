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

import org.structr.core.agent.Agent;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.GenericNode;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;

import java.lang.reflect.Modifier;

import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.lang.StringUtils;
import org.structr.core.*;

//~--- classes ----------------------------------------------------------------

/**
 * Modules need to be installed and uninstalled manually This service keeps an index of installed / activated modules for efficient access files: - $BASEDIR/modules/modules.conf -> properties file
 *
 * The entity class cache needs to be initialized with the structr core entities even when no modules exist.
 *
 *
 * @author Christian Morgner
 */
public class ModuleService implements SingletonService {

	private static final Logger logger                                       = Logger.getLogger(ModuleService.class.getName());
	private static final Map<String, Class<? extends Agent>> agentClassCache = new ConcurrentHashMap<String, Class<? extends Agent>>(10, 0.9f, 8);
	private static final Set<String> nodeEntityPackages                      = new LinkedHashSet<String>();
	private static final Set<String> relationshipPackages                    = new LinkedHashSet<String>();
	private static final Map<String, Class> relationshipClassCache           = new ConcurrentHashMap<String, Class>(10, 0.9f, 8);
	private static final Map<String, Class> nodeEntityClassCache             = new ConcurrentHashMap<String, Class>(100, 0.9f, 8);
	private static final Map<String, Set<Class>> interfaceCache              = new ConcurrentHashMap<String, Set<Class>>(10, 0.9f, 8);
	private static final Set<String> agentPackages                           = new LinkedHashSet<String>();
	private static final String fileSep                                      = System.getProperty("file.separator");
	private static final String fileSepEscaped                               = fileSep.replaceAll("\\\\", "\\\\\\\\");	// ....
	private static final String pathSep                                      = System.getProperty("path.separator");
	private static final String testClassesDir                               = fileSep.concat("test-classes");
	private static final String classesDir                                   = fileSep.concat("classes");

	//~--- methods --------------------------------------------------------

	/**
	 * Loads and activates the given module.
	 *
	 * @param resourceName the file name of the module to activate
	 * @param recreateIndex whether to recreate the index file
	 */
	public void scanResource(String resourceName) {

		try {

			Module module = loadResource(resourceName);

			if (module != null) {

				importResource(module);
			} else {

				logger.log(Level.WARNING, "Module was null!");
			}

		} catch (IOException ioex) {

			logger.log(Level.WARNING, "Error loading module {0}: {1}", new Object[] { resourceName, ioex });
			ioex.printStackTrace();

		}

	}

	@Override
	public void injectArguments(Command command) {

		if (command != null) {

			command.setArgument("moduleService", this);
		}

	}

	@Override
	public void initialize(Map<String, String> context) {

		scanResources();

	}

	@Override
	public void shutdown() {

		nodeEntityClassCache.clear();
		relationshipClassCache.clear();
		agentClassCache.clear();

	}

	private void scanResources() {

		Set<String> resourcePaths = getResourcesToScan();

		for (String resourcePath : resourcePaths) {

			scanResource(resourcePath);
		}

		logger.log(Level.INFO, "{0} JARs scanned", resourcePaths.size());

	}

	/**
	 * Processes the information from the given module and makes them available for the service layer.
	 *
	 * @param module the module to process
	 *
	 * @throws IOException
	 */
	private void importResource(Module module) throws IOException {

		Set<String> classes = module.getClasses();

		for (final String name : classes) {
			
			String className = StringUtils.removeStart(name, ".");

			logger.log(Level.FINE, "Instantiating class {0} ", className);

			try {

				// instantiate class..
				Class clazz = Class.forName(className);

				logger.log(Level.FINE, "Class {0} instantiated: {1}", new Object[] { className, clazz });

				if (!Modifier.isAbstract(clazz.getModifiers())) {

					// register node entity classes
					if (AbstractNode.class.isAssignableFrom(clazz)) {

						EntityContext.init(clazz);
						
						// try to instantiate class
						try { EntityContext.scanEntity(clazz.newInstance()); } catch(Throwable t) {}

						String simpleName = clazz.getSimpleName();
						String fullName   = clazz.getName();

						nodeEntityClassCache.put(simpleName, clazz);
						nodeEntityPackages.add(fullName.substring(0, fullName.lastIndexOf(".")));

						for (Class interfaceClass : clazz.getInterfaces()) {

							String interfaceName           = interfaceClass.getSimpleName();
							Set<Class> classesForInterface = interfaceCache.get(interfaceName);

							if (classesForInterface == null) {

								classesForInterface = new LinkedHashSet<Class>();

								interfaceCache.put(interfaceName, classesForInterface);

							}

							classesForInterface.add(clazz);

						}

					}

					// register entity classes
					if (AbstractRelationship.class.isAssignableFrom(clazz)) {

						EntityContext.init(clazz);

						// try to instantiate class
						try { EntityContext.scanEntity(clazz.newInstance()); } catch(Throwable t) {}

						String simpleName = clazz.getSimpleName();
						String fullName   = clazz.getName();

						relationshipClassCache.put(simpleName, clazz);
						relationshipPackages.add(fullName.substring(0, fullName.lastIndexOf(".")));

						for (Class interfaceClass : clazz.getInterfaces()) {

							String interfaceName           = interfaceClass.getSimpleName();
							Set<Class> classesForInterface = interfaceCache.get(interfaceName);

							if (classesForInterface == null) {

								classesForInterface = new LinkedHashSet<Class>();

								interfaceCache.put(interfaceName, classesForInterface);

							}

							classesForInterface.add(clazz);

						}
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
				}
				
			} catch (Throwable t) {}

		}

	}

	private Module loadResource(String resource) throws IOException {

		// create module
		DefaultModule ret   = new DefaultModule(resource);
		Set<String> classes = ret.getClasses();

		if (resource.endsWith(".jar") || resource.endsWith(".war")) {

			ZipFile zipFile = new ZipFile(new File(resource), ZipFile.OPEN_READ);

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

				if (entryName.endsWith(".class")) {

					String fileEntry = entry.getName().replaceAll("[/]+", ".");
					
					// add class entry to Module
					classes.add(fileEntry.substring(0, fileEntry.length() - 6));

				}

			}

			zipFile.close();

		} else if (resource.endsWith(classesDir)) {

			addClassesRecursively(new File(resource), classesDir, classes);

		} else if (resource.endsWith(testClassesDir)) {

			addClassesRecursively(new File(resource), testClassesDir, classes);
		}

		return ret;
	}

	private void addClassesRecursively(File dir, String prefix, Set<String> classes) {

		int prefixLen = prefix.length();

		for (File file : dir.listFiles()) {

			if (file.isDirectory()) {

				addClassesRecursively(file, prefix, classes);
				
			} else {

				try {

					String fileEntry = file.getAbsolutePath();

					fileEntry = fileEntry.substring(0, fileEntry.length() - 6);
					fileEntry = fileEntry.substring(fileEntry.indexOf(prefix) + prefixLen);
					fileEntry = fileEntry.replaceAll("[".concat(fileSepEscaped).concat("]+"), ".");
					
					if (fileEntry.startsWith(".")) {
						fileEntry = fileEntry.substring(1);
					}
					
					classes.add(fileEntry);

				} catch (Throwable t) {

					// ignore
					t.printStackTrace();
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

	public Map<String, Class<? extends Agent>> getCachedAgents() {

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

	public Class<? extends Agent> getAgentClass(final String name) {

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
	 * Scans the class path and returns a Set containing all structr modules.
	 *
	 * @return a Set of active module names
	 */
	public Set<String> getResourcesToScan() {

		String classPath = System.getProperty("java.class.path");
		Set<String> ret  = new LinkedHashSet<String>();
		Pattern pattern  = Pattern.compile(".*(structr).*(war|jar)");
		Matcher matcher  = pattern.matcher("");

		for (String jarPath : classPath.split("[".concat(pathSep).concat("]+"))) {

			String lowerPath = jarPath.toLowerCase();

			if (lowerPath.endsWith(classesDir) || lowerPath.endsWith(testClassesDir)) {

				ret.add(jarPath);
				
			} else {

				String moduleName = lowerPath.substring(lowerPath.lastIndexOf(pathSep) + 1);

				matcher.reset(moduleName);

				if (matcher.matches()) {

					ret.add(jarPath);
				}

			}

		}

		String resources = Services.getResources();

		if (resources != null) {

			for (String resource : resources.split("[".concat(pathSep).concat("]+"))) {

				String lowerResource = resource.toLowerCase();

				if (lowerResource.endsWith(".jar") || lowerResource.endsWith(".war")) {

					ret.add(resource);
				}

			}

		}
		
		// logger.log(Level.INFO, "resources: {0}", ret);

		return (ret);

	}

	@Override
	public String getName() {

		return (ModuleService.class.getSimpleName());

	}
	
	@Override
	public boolean isRunning() {

		// we're always running :)
		return (true);
	}
}
