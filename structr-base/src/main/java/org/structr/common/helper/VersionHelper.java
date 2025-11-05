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

import org.structr.api.config.Settings;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.module.StructrModule;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class to gather and provide information about the running Structr version.
 */
public class VersionHelper {

	private static final String classPath;
	private static final Map<String, Map<String, Object>> modules    = new HashMap<>();
	private static final Map<String, Map<String, String>> components = new HashMap<>();
	private static boolean modulesUpdatedAfterSystemInitComplete = false;

	static {

		classPath                            = System.getProperty("java.class.path");
		final Pattern outerPattern           = Pattern.compile("(structr-[^:;]*\\.jar)");
		final Pattern innerPattern           = Pattern.compile("(structr-base|structr-app|structr)-([^-]*(?:-SNAPSHOT|-(?:rc|RC)\\d){0,1})-{0,1}(?:([0-9]{0,12})\\.{0,1}([0-9a-f]{0,32}))\\.jar");

		final Matcher outerMatcher           = outerPattern.matcher(classPath);

		while (outerMatcher.find()) {

			final String group               = outerMatcher.group();
			final Matcher innerMatcher       = innerPattern.matcher(group);
			final Map<String, String> module = new HashMap<>();

			if (innerMatcher.matches()) {

				module.put("version", innerMatcher.group(2));
				module.put("date", innerMatcher.group(3));
				module.put("build", innerMatcher.group(4));

				components.put(innerMatcher.group(1), module);
			}
		}
	}

	public static String getFullVersionInfo() {

		Map<String, String> structrModule = components.get("structr");

		if (structrModule != null) {
			return VersionHelper.getFullVersionInfoFromModule(structrModule);
		}

		Map<String, String> structrBaseModule = getComponents().get("structr-app");

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
