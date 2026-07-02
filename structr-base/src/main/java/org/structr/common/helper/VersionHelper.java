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
package org.structr.common.helper;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.module.StructrModule;

import java.io.File;
import java.lang.module.ResolvedModule;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

/**
 * Helper class to gather and provide information about the running Structr version.
 */
public class VersionHelper {
	private static final Logger logger = LoggerFactory.getLogger(VersionHelper.class);

	private static final String classPath;
	private static final Map<String, Map<String, Object>> modules    = new HashMap<>();
	private static final Map<String, Map<String, String>> components = new HashMap<>();
	private static boolean modulesUpdatedAfterSystemInitComplete = false;

	static {

		classPath = System.getProperty("java.class.path");

		// (1) Structr jars on the class path: the resources-only application jar
		//     (structr-app.jar / the enterprise app jar) and any class-path islands.
		//     Split on the path separator and match on the jar's *file name* only. Matching
		//     the raw class-path string with a regex is wrong: a parent directory containing
		//     "structr-" (e.g. a CI build dir like /builds/structr/structr-enterprise/.m2/...)
		//     makes every third-party jar look like a Structr jar and floods the log with
		//     "Missing build information in manifest" warnings.
		if (classPath != null) {

			for (final String classPathEntry : classPath.split(Pattern.quote(File.pathSeparator))) {

				final File jarFile   = new File(classPathEntry);
				final String jarName = jarFile.getName();

				if (jarName.startsWith("structr-") && jarName.endsWith(".jar")) {
					readModuleManifest(jarFile);
				}
			}
		}

		// (2) Structr jars on the module path. Since the JPMS migration, Structr's own
		//     modules live on --module-path (lib/), not the class path, so enumerate the
		//     boot module layer and read the manifest of every structr-*.jar it resolved.
		try {

			for (final ResolvedModule resolvedModule : ModuleLayer.boot().configuration().modules()) {

				final Optional<URI> location = resolvedModule.reference().location();
				if (location.isPresent()) {

					final Path path       = Paths.get(location.get());
					final Path fileName   = path.getFileName();
					final String jarName  = fileName != null ? fileName.toString() : "";

					if (jarName.startsWith("structr-") && jarName.endsWith(".jar")) {
						readModuleManifest(path.toFile());
					}
				}
			}

		} catch (Exception e) {
			logger.error("Error enumerating Structr modules from the module path: {}", e.getMessage());
		}
	}

	private static void readModuleManifest(final File jarFile) {

		try {

			// exploded class directories (Maven dev/reactor layout) carry no manifest; skip them
			if (jarFile.exists() && jarFile.isFile()) {

				try (JarFile jar = new JarFile(jarFile)) {

					final Manifest manifest = jar.getManifest();
					if (manifest != null) {

						final Attributes attrs = manifest.getMainAttributes();

						final Map<String, String> module = new HashMap<>();
						module.put("version", attrs.getValue("Implementation-Version"));
						module.put("date", attrs.getValue("Build-Timestamp"));
						module.put("build", attrs.getValue("Build-Number"));

						final String moduleName = attrs.getValue("Implementation-Title");
						if ("structr-app-enterprise".equals(moduleName)) {
							components.put("structr", module);
						} else if ("structr-app".equals(moduleName)) {
							components.putIfAbsent("structr", module);
						} else if (StringUtils.isNotBlank(moduleName)) {
							components.put(moduleName, module);
						} else {
							logger.warn("Missing build information in manifest for {}", jarFile.getName());
						}
					}
				}
			}

		} catch (Exception e) {
			logger.error("Error parsing module manifest \"{}\".", e.getMessage());
		}
	}


	public static String getFullVersionInfo() {

		Map<String, String> structrModule = components.get("structr");

		if (structrModule != null) {
			return VersionHelper.getFullVersionInfoFromModule(structrModule);
		}

		Map<String, String> structrBaseModule = components.get("structr-base");

		if (structrBaseModule != null) {
			return VersionHelper.getFullVersionInfoFromModule(structrBaseModule);
		}

		return "Could not determine version string";
	}

	private static String getFullVersionInfoFromModule(final Map<String, String> module) {

		return module.get("version") + " " + module.get("build") + " " + module.get("date");

	}

	public static String getClassPath() {
		return classPath;
	}

	public static String getVersion() {

		final Map<String, String> structrModule = components.get("structr");
		if (structrModule != null) {

			return structrModule.get("version");
		}

		return "unknown version";
	}

	public static String getInstanceName() {
		return Settings.InstanceName.getValue();
	}

	public static String getInstanceStage() {
		return Settings.InstanceStage.getValue();
	}

	public static void updateModuleList () {

		modules.clear();

		// collect StructrModules
		for (final StructrModule module : StructrApp.getConfiguration().getModules().values())  {

			final Map<String, Object> map = new LinkedHashMap<>();

			map.put("source", module.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());

			if (module.getDependencies() != null) {
				map.put("dependencies", module.getDependencies());
			}

			if (module.getFeatures() != null) {
				map.put("features", module.getFeatures());
			}

			modules.put(module.getName(), map);
		}
	}

	public static Map<String, Map<String, Object>> getModules() {

		if (!modulesUpdatedAfterSystemInitComplete) {
			updateModuleList();
		}

		modulesUpdatedAfterSystemInitComplete = Services.getInstance().isInitialized();

		return modules;
	}

	public static Map<String, Map<String, String>> getComponents() {
		return components;
	}

}
