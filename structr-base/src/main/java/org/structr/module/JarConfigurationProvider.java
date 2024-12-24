/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.module;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.agent.Agent;
import org.structr.api.service.LicenseManager;
import org.structr.api.service.Service;
import org.structr.core.Services;
import org.structr.schema.ConfigurationProvider;
import org.structr.web.common.UiModule;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The module service main class.
 */
public class JarConfigurationProvider implements ConfigurationProvider {

	private static final Logger logger = LoggerFactory.getLogger(JarConfigurationProvider.class.getName());

	public static final URI StaticSchemaRootURI      = URI.create("https://structr.org/v2.0/#");
	public static final String DYNAMIC_TYPES_PACKAGE = "org.structr.dynamic";

	private static final Set<String> coreModules                                                   = new HashSet<>(Arrays.asList("core", "rest", "ui"));
	private final Map<String, Class<? extends Agent>> agentClassCache                              = new ConcurrentHashMap<>(100);
	private final Map<String, StructrModule> modules                                               = new ConcurrentHashMap<>(100);
	private final Set<String> agentPackages                                                        = new LinkedHashSet<>();
	private final String fileSep                                                                   = System.getProperty("file.separator");
	private final String pathSep                                                                   = System.getProperty("path.separator");
	private final String fileSepEscaped                                                            = fileSep.replaceAll("\\\\", "\\\\\\\\");
	private final String testClassesDir                                                            = fileSep.concat("test-classes");
	private final String classesDir                                                                = fileSep.concat("classes");

	private LicenseManager licenseManager                                                          = null;

	// ----- interface ConfigurationProvider -----
	@Override
	public void initialize(final LicenseManager licenseManager) {

		this.licenseManager = licenseManager;

		final List<ClasspathResource> resources = scanResources();

		for (final ClasspathResource resource : resources) {

			try {

				importResource(resource);

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		loadModules(resolveModuleDependencies());
	}

	@Override
	public void shutdown() {
	}

	@Override
	public Map<String, Class<? extends Agent>> getAgents() {
		return agentClassCache;
	}

	public Class<? extends Agent> getAgentClass(final String name) {

		Class agentClass = null;

		if ((name != null) && (name.length() > 0)) {

			agentClass = agentClassCache.get(name);

			if (agentClass == null) {

				for (String possiblePath : agentPackages) {

					if (possiblePath != null) {

						try {

							Class nodeClass = Class.forName(possiblePath + "." + name);

							agentClassCache.put(name, nodeClass);

							// first match wins
							return nodeClass;

						} catch (ClassNotFoundException ex) {

							// ignore
						}

					}

				}

			}

		}

		return agentClass;

	}

	@Override
	public Map<String, StructrModule> getModules() {
		return modules;
	}

	// ----- private methods -----
	private List<ClasspathResource> scanResources() {
		
		final List<ClasspathResource> modules = new LinkedList<>();
		final Set<String> resourcePaths       = getResourcesToScan();

		for (String resourcePath : resourcePaths) {

			try {
				
				modules.add(loadResource(resourcePath));
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		logger.info("{} JARs scanned", resourcePaths.size());
		
		return modules;
	}

	private List<StructrModule> resolveModuleDependencies() {

		final List<StructrModule> sortedList         = new LinkedList<>();
		final Map<String, Set<String>> dependencyMap = new LinkedHashMap<>();

		for (final StructrModule module : modules.values()) {

			final Set<String> dependencies = module.getDependencies();
			final String moduleName        = module.getName();

			if (dependencies != null) {

				dependencyMap.computeIfAbsent(moduleName, k -> new LinkedHashSet<>()).addAll(dependencies);
			}

			sortedList.add(module);
		}

		Collections.sort(sortedList, (m1, m2) -> {

			final int level1 = getHierarchyLevel(dependencyMap, m1.getName());
			final int level2 = getHierarchyLevel(dependencyMap, m2.getName());

			return Integer.compare(level1, level2);
		});

		return sortedList;
	}

	private int getHierarchyLevel(final Map<String, Set<String>> dependencyMap, final String name) {

		final Set<String> dependencies = dependencyMap.get(name);
		if (dependencies == null) {

			return 0;

		}

		int level = 1;

		for (final String dependency : dependencies) {

			level += getHierarchyLevel(dependencyMap, dependency);
		}

		return level;
	}

	private void loadModules(final List<StructrModule> sortedModules) {

		for (final StructrModule structrModule : sortedModules) {

			final String moduleName = structrModule.getName();

			structrModule.registerModuleFunctions(licenseManager);

			if (coreModules.contains(moduleName) || licenseManager == null || licenseManager.isModuleLicensed(moduleName)) {

				modules.put(moduleName, structrModule);
				logger.info("Activating module {}", moduleName);

				structrModule.onLoad(licenseManager);
			}
		}
	}

	private void importResource(final ClasspathResource module) throws IOException {

		final Set<String> classes = module.getClasses();

		for (final String name : classes) {

			String className = StringUtils.removeStart(name, ".");

			try {

				// instantiate class..
				final Class clazz   = Class.forName(className);
				final int modifiers = clazz.getModifiers();

				// register services
				if (Service.class.isAssignableFrom(clazz) && !(Modifier.isAbstract(modifiers))) {

					Services.getInstance().registerServiceClass(clazz);
				}

				// register agents
				if (Agent.class.isAssignableFrom(clazz) && !(Modifier.isAbstract(modifiers))) {

					final String simpleName = clazz.getSimpleName();
					final String fullName   = clazz.getName();

					agentClassCache.put(simpleName, clazz);
					agentPackages.add(fullName.substring(0, fullName.lastIndexOf(".")));
				}

				// register modules
				if (StructrModule.class.isAssignableFrom(clazz) && !(Modifier.isAbstract(modifiers))) {

					try {

						// we need to make sure that a module is initialized exactly once
						final StructrModule structrModule = (StructrModule) clazz.getDeclaredConstructor().newInstance();
						final String moduleName = structrModule.getName();

						if (!modules.containsKey(moduleName)) {

							modules.put(moduleName, structrModule);
						}

					} catch (Throwable t) {

						t.printStackTrace();

						// log only errors from internal classes
						if (className.startsWith("org.structr.") && !UiModule.class.getName().equals(className)) {

							logger.warn("Unable to instantiate module " + clazz.getName(), t);
						}
					}
				}

			} catch (Throwable t) {
				t.printStackTrace();
				logger.warn("Error trying to load class {}: {}",  className, t.getMessage());
			}
		}
	}

	private ClasspathResource loadResource(String resource) throws IOException {

		// create module
		final ClasspathResource ret   = new ClasspathResource(resource);
		final Set<String> classes = ret.getClasses();

		if (resource.endsWith(".jar") || resource.endsWith(".war")) {

			try (final JarFile jarFile   = new JarFile(new File(resource), true)) {

				final Manifest manifest = jarFile.getManifest();
				if (manifest != null) {

					final Attributes attrs  = manifest.getAttributes("Structr");
					if (attrs != null) {

						final String name = attrs.getValue("Structr-Module-Name");

						// only scan and load modules that are licensed
						if (name != null) {

							if (licenseManager == null || licenseManager.isModuleLicensed(name)) {

								for (final Enumeration<? extends JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {

									final JarEntry entry = entries.nextElement();
									final String entryName = entry.getName();

									if (entryName.endsWith(".class")) {

										// cat entry > /dev/null (necessary to get signers below)
										IOUtils.copy(jarFile.getInputStream(entry), new ByteArrayOutputStream(65535));

										// verify module
										if (licenseManager == null || licenseManager.isValid(entry.getCodeSigners())) {

											final String fileEntry = entry.getName().replaceAll("[/]+", ".");
											final String fqcn      = fileEntry.substring(0, fileEntry.length() - 6);

											// add class entry to Module
											classes.add(fqcn);

											if (licenseManager != null) {
												// store licensing information
												licenseManager.addLicensedClass(fqcn);
											}
										}
									}
								}

							} else {

								// module is not licensed, only load functions as unlicensed

								for (final Enumeration<? extends JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {

									final JarEntry entry = entries.nextElement();
									final String entryName = entry.getName();

									if (entryName.endsWith(".class")) {

										// cat entry > /dev/null (necessary to get signers below)
										IOUtils.copy(jarFile.getInputStream(entry), new ByteArrayOutputStream(65535));

										// verify module
										if (licenseManager == null || licenseManager.isValid(entry.getCodeSigners())) {

											final String fileEntry = entry.getName().replaceAll("[/]+", ".");
											final String fqcn      = fileEntry.substring(0, fileEntry.length() - 6);

											try {

												final Class clazz   = Class.forName(fqcn);
												final int modifiers = clazz.getModifiers();

												// register entity classes
												if (StructrModule.class.isAssignableFrom(clazz) && !(Modifier.isAbstract(modifiers))) {

													// we need to make sure that a module is initialized exactly once
													final StructrModule structrModule = (StructrModule) clazz.getDeclaredConstructor().newInstance();

													structrModule.registerModuleFunctions(licenseManager);

												}

											} catch (Throwable t) {
												t.printStackTrace();
												logger.warn("Error trying to load class {}: {}",  fqcn, t.getMessage());
											}
										}
									}
								}
							}
						}
					}
				}
			}

		} else if (resource.endsWith(classesDir)) {

			// this is for testing only!
			addClassesRecursively(new File(resource), classesDir, classes);

		} else if (resource.endsWith(testClassesDir)) {

			// this is for testing only!
			addClassesRecursively(new File(resource), testClassesDir, classes);
		}

		return ret;
	}

	private void addClassesRecursively(final File dir, final String prefix, final Set<String> classes) {

		if (dir == null) {
			return;
		}

		int prefixLen = prefix.length();
		File[] files = dir.listFiles();

		if (files == null) {
			return;
		}

		for (final File file : files) {

			if (file.isDirectory()) {

				addClassesRecursively(file, prefix, classes);

			} else {

				try {

					String fileEntry = file.getAbsolutePath();

					if (fileEntry.endsWith(".class")) {

						fileEntry = fileEntry.substring(0, fileEntry.length() - 6);
						fileEntry = fileEntry.substring(fileEntry.indexOf(prefix) + prefixLen);
						fileEntry = fileEntry.replaceAll("[".concat(fileSepEscaped).concat("]+"), ".");

						if (fileEntry.startsWith(".")) {
							fileEntry = fileEntry.substring(1);
						}

						classes.add(fileEntry);
					}

				} catch (Throwable t) {
					// ignore
					logger.warn("", t);
				}

			}

		}

	}

	/**
	 * Scans the class path and returns a Set containing all structr
	 * modules.
	 *
	 * @return a Set of active module names
	 */
	private Set<String> getResourcesToScan() {

		final String classPath    = System.getProperty("java.class.path");
		final Set<String> modules = new TreeSet<>();
		final Pattern pattern     = Pattern.compile(".*(structr).*(war|jar)");
		final Matcher matcher     = pattern.matcher("");

		for (final String jarPath : classPath.split("[".concat(pathSep).concat("]+"))) {

			final String lowerPath = jarPath.toLowerCase();

			if (lowerPath.endsWith(classesDir) || lowerPath.endsWith(testClassesDir)) {

				modules.add(jarPath);

			} else {

				final String moduleName = lowerPath.substring(lowerPath.lastIndexOf(pathSep) + 1);

				matcher.reset(moduleName);

				if (matcher.matches()) {

					modules.add(jarPath);
				}
			}
		}

		for (final String resource : Services.getInstance().getResources()) {

			final String lowerResource = resource.toLowerCase();

			if (lowerResource.endsWith(".jar") || lowerResource.endsWith(".war")) {

				modules.add(resource);
			}
		}

		return modules;
	}
}
