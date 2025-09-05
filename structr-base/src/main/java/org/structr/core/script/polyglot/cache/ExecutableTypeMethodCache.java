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
package org.structr.core.script.polyglot.cache;

import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.structr.api.util.FixedSizeCache;
import org.structr.core.GraphObject;

import java.util.HashMap;
import java.util.Map;

public class ExecutableTypeMethodCache {

	private final FixedSizeCache<GraphObject, Map<String, ProxyExecutable>> typeMethodCache = new FixedSizeCache<>("executableTypeMethodCache", 100);

	public ProxyExecutable getExecutable(final GraphObject instance, final String methodName) {

		if (typeMethodCache.containsKey(instance)) {

			return typeMethodCache.get(instance).get(methodName);
		}

		return null;
	}

	public void cacheExecutable(final GraphObject instance, final String methodName, final ProxyExecutable executable) {

		if (typeMethodCache.containsKey(instance)) {

			typeMethodCache.get(instance).put(methodName, executable);

		} else {

			final Map<String, ProxyExecutable> methodMap = new HashMap<>();
			methodMap.put(methodName, executable);
			typeMethodCache.put(instance, methodMap);
		}
	}

	public void clearCacheForType(final GraphObject instance) {

		typeMethodCache.remove(instance);
	}

	public void clearCache() {

		typeMethodCache.clear();
	}
}
