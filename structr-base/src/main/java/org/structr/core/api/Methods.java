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
package org.structr.core.api;

import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */
public class Methods {

	private static final Map<String, CacheEntry> methodCache = new LinkedHashMap<>();

	public static Map<String, AbstractMethod> getAllMethods(final Traits traits) {

		final Map<String, AbstractMethod> allMethods = new LinkedHashMap<>();

		if (traits != null) {

			allMethods.putAll(traits.getDynamicMethods());

		} else {

			try {

				final PropertyKey<NodeInterface> schemaNodeKey = Traits.of("SchemaMethod").key("schemaNode");

				for (final NodeInterface globalMethod : StructrApp.getInstance().nodeQuery("SchemaMethod").and(schemaNodeKey, null).getResultStream()) {

					allMethods.put(globalMethod.getName(), new ScriptMethod(globalMethod.as(SchemaMethod.class)));
				}

			} catch (FrameworkException fex) {
				throw new RuntimeException(fex);
			}
		}

		return allMethods;
	}

	public static AbstractMethod resolveMethod(final Traits type, final String methodName) {

		// A method can either be a Java method, which we need to call with Method.invoke() via reflection,
		// OR a scripting method which will in turn call Actions.execute(), so we want do differentiate
		// between the two and use the appropriate calling method.

		if (methodName == null) {
			throw new RuntimeException(new FrameworkException(422, "Cannot resolve method without methodName!"));
		}

		// no type => global schema method!
		if (type == null) {

			CacheEntry cacheEntry = methodCache.get(methodName);
			if (cacheEntry == null) {

				cacheEntry = new CacheEntry();
				methodCache.put(methodName, cacheEntry);

				try (final Tx tx = StructrApp.getInstance().tx()) {

					final PropertyKey<NodeInterface> schemaNodeKey = Traits.of("SchemaMethod").key("schemaNode");
					final NodeInterface method = StructrApp.getInstance().nodeQuery("SchemaMethod").andName(methodName).and(schemaNodeKey, null).getFirst();

					if (method != null) {

						cacheEntry.method = new ScriptMethod(method.as(SchemaMethod.class));
					}

				} catch (FrameworkException fex) {
					throw new RuntimeException(fex);
				}
			}

			return cacheEntry.method;

		} else {

			final Map<String, AbstractMethod> methods = type.getDynamicMethods();

			return methods.get(methodName);
		}
	}

	public static void clearMethodCache() {
		methodCache.clear();
	}


	// ----- private static methods -----
	private static class CacheEntry {
		public AbstractMethod method = null;
	}
}
