/*
 * Copyright (C) 2010-2021 Structr GmbH
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

import java.util.HashMap;
import java.util.Map;

public class ExecutableTypeMethodCache {
	private final Map<String, Map<String, ProxyExecutable>> typeMethodCache = new HashMap<>();

	public ProxyExecutable getExecutable(String typeName, String methodName) {

		if (typeMethodCache.containsKey(typeName)) {

			return typeMethodCache.get(typeName).get(methodName);
		}

		return null;
	}

	public void cacheExecutable(String typeName, String methodName, final ProxyExecutable executable) {

		if (typeMethodCache.containsKey(typeName)) {

			typeMethodCache.get(typeName).put(methodName, executable);
		} else {

			Map<String, ProxyExecutable> methodMap = new HashMap<>();
			methodMap.put(methodName, executable);
			typeMethodCache.put(typeName, methodMap);
		}
	}

	public void clearCacheForType(final String typeName) {

		typeMethodCache.remove(typeName);
	}

	public void clearCache() {

		typeMethodCache.clear();
	}

}
