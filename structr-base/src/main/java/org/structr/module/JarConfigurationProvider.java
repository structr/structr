/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.agent.Agent;
import org.structr.api.service.LicenseManager;
import org.structr.api.service.Service;
import org.structr.core.Services;
import org.structr.docs.Documentation;
import org.structr.docs.Documentations;
import org.structr.schema.ConfigurationProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The module service main class.
 *
 * All Structr SPIs - {@link StructrModule}, {@link Service}, {@link Agent} and the
 * database driver - are discovered via {@link ServiceLoader}, and the set of
 * {@code @Documentation} annotated types is read from a per-module index built at
 * compile time (see structr-annotation-processor). Neither mechanism scans the class
 * path, so both work unchanged when Structr runs on the Java module path.
 */
public class JarConfigurationProvider implements ConfigurationProvider {

	private static final Logger logger = LoggerFactory.getLogger(JarConfigurationProvider.class.getName());

	/** compile-time generated index of @Documentation annotated types, one FQCN per line */
	private static final String DOCUMENTED_CLASSES_INDEX = "META-INF/structr/documented-classes";

	private final Map<String, Class<? extends Agent>> agentClassCache      = new ConcurrentHashMap<>(100);
	private final Map<Class, List<Documentation>> documentationAnnotations = new LinkedHashMap<>();
	private final Map<String, StructrModule> modules                       = new ConcurrentHashMap<>(100);
	private final Set<String> classNames                                   = new LinkedHashSet<>();
	private final Set<String> agentPackages                                = new LinkedHashSet<>();

	private LicenseManager licenseManager                                  = null;

	// ----- interface ConfigurationProvider -----
	@Override
	public void initialize(final LicenseManager licenseManager) {

		this.licenseManager = licenseManager;

		// All Structr SPIs are discovered via ServiceLoader, which works identically on
		// the class path (META-INF/services) and on the module path ('provides ... with').
		loadModulesFromServiceLoader();
		loadServicesFromServiceLoader();
		loadAgentsFromServiceLoader();

		// @Documentation annotated types are read from the per-module index built at
		// compile time by structr-annotation-processor; this avoids a class-path scan
		// and works on the module path.
		loadDocumentationAnnotationsFromIndex();

		loadModules(resolveModuleDependencies());
	}

	@Override
	public void shutdown() {
	}

	@Override
	public Map<String, Class<? extends Agent>> getAgents() {

		return agentClassCache;
	}

	@Override
	public Map<String, StructrModule> getModules() {

		return modules;
	}

	@Override
	public Set<String> getClassNames() {

		return classNames;
	}

	@Override
	public Map<Class, List<Documentation>> getDocumentationAnnotations() {

		return documentationAnnotations;
	}

	// ----- private methods -----
	private void loadModulesFromServiceLoader() {

		final Iterator<StructrModule> iterator = ServiceLoader.load(StructrModule.class).iterator();

		while (iterator.hasNext()) {

			try {

				final StructrModule structrModule = iterator.next();
				final String moduleName           = structrModule.getName();

				// a module is initialized exactly once; the first provider for a
				// given name wins.
				modules.putIfAbsent(moduleName, structrModule);

			} catch (Throwable t) {

				logger.warn("Unable to load StructrModule service provider", t);
			}
		}

		logger.info("{} modules discovered via ServiceLoader", modules.size());
	}

	private void loadServicesFromServiceLoader() {

		int count = 0;

		for (final ServiceLoader.Provider<Service> provider : ServiceLoader.load(Service.class).stream().toList()) {

			try {

				// register the class only; instantiation and lifecycle stay with Services
				Services.getInstance().registerServiceClass(provider.type());
				count++;

			} catch (Throwable t) {

				logger.warn("Unable to register Service service provider", t);
			}
		}

		logger.info("{} services discovered via ServiceLoader", count);
	}

	private void loadAgentsFromServiceLoader() {

		for (final ServiceLoader.Provider<Agent> provider : ServiceLoader.load(Agent.class).stream().toList()) {

			try {

				final Class<? extends Agent> clazz = provider.type();
				final String fullName              = clazz.getName();

				agentClassCache.put(clazz.getSimpleName(), clazz);
				agentPackages.add(fullName.substring(0, fullName.lastIndexOf(".")));

			} catch (Throwable t) {

				logger.warn("Unable to register Agent service provider", t);
			}
		}
	}

	private void loadDocumentationAnnotationsFromIndex() {

		final Set<String> documentedClassNames = new LinkedHashSet<>();

		try {

			final Enumeration<URL> indexResources = classLoader().getResources(DOCUMENTED_CLASSES_INDEX);

			while (indexResources.hasMoreElements()) {

				final URL indexResource = indexResources.nextElement();

				try (final BufferedReader reader = new BufferedReader(new InputStreamReader(indexResource.openStream(), StandardCharsets.UTF_8))) {

					String line;

					while ((line = reader.readLine()) != null) {

						final String className = line.trim();
						if (!className.isEmpty()) {

							documentedClassNames.add(className);
						}
					}

				} catch (IOException ioex) {

					logger.warn("Unable to read documentation index {}", indexResource, ioex);
				}
			}

		} catch (IOException ioex) {

			logger.warn("Unable to enumerate documentation indexes", ioex);
		}

		for (final String className : documentedClassNames) {

			try {

				final Class clazz = Class.forName(className);

				classNames.add(className);

				// repeatable / multiple annotations on the same type
				final Documentations documentations = (Documentations) clazz.getAnnotation(Documentations.class);
				if (documentations != null) {

					documentationAnnotations.computeIfAbsent(clazz, k -> new LinkedList<>()).addAll(Arrays.asList(documentations.value()));
				}

				// single annotation
				final Documentation documentation = (Documentation) clazz.getAnnotation(Documentation.class);
				if (documentation != null) {

					documentationAnnotations.computeIfAbsent(clazz, k -> new LinkedList<>()).add(documentation);
				}

			} catch (Throwable t) {

				logger.warn("Unable to load documented class {}", className, t);
			}
		}

		logger.info("{} documented classes loaded from index", documentationAnnotations.size());
	}

	private ClassLoader classLoader() {

		final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

		return contextClassLoader != null ? contextClassLoader : JarConfigurationProvider.class.getClassLoader();
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

			logger.info("Activating module {}", moduleName);

			try {

				structrModule.registerModuleFunctions(licenseManager);

			} catch (Throwable t) {

				logger.error("Error loading module '{}'", moduleName);
				logger.error("", t);

				System.exit(1);
			}

			modules.put(moduleName, structrModule);

			structrModule.onLoad();
		}
	}
}
