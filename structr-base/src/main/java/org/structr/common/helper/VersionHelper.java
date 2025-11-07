/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
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

		final Pattern structrJarFilePattern = Pattern.compile("([^:;]*structr-[^:;]*\\.jar)");

		final Matcher structrJarFileMatcher = structrJarFilePattern.matcher(classPath);

		while (structrJarFileMatcher.find()) {
			final String structrJarPath = structrJarFileMatcher.group(1);

			try {
				File jarFile = new File(structrJarPath);

				if (jarFile.exists()) {
					// Read manifest from the JAR file
					try (JarFile jar = new JarFile(jarFile)) {
						Manifest manifest = jar.getManifest();

						if (manifest != null) {
							Attributes attrs = manifest.getMainAttributes();

							Map<String, String> module = new HashMap<>();
							module.put("version", attrs.getValue("Implementation-Version"));
							module.put("date", attrs.getValue("Build-Timestamp"));
							module.put("build", attrs.getValue("Build-Number"));

							String moduleName = attrs.getValue("Implementation-Title");
							if ("structr-app-enterprise".equals(moduleName)) {
								components.put("structr", module);
							} else if ("structr-app".equals(moduleName)) {
								components.putIfAbsent("structr", module);
							} else if (StringUtils.isNotBlank(moduleName)) {
								components.put(moduleName, module);
							}
						}
					}
				}
			} catch (Exception e) {
				logger.error("Error parsing module manifest  \"{}\".", e.getMessage());
			}
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
