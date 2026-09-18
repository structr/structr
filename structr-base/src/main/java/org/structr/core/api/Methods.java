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
package org.structr.core.api;

import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class Methods {

	/* Concurrent, and filled before it is published. resolveMethod() runs on every request thread, and
	   clearMethodCache() runs from Actions.clearCache() after a schema compile, so a plain LinkedHashMap
	   here had two threads writing and one clearing the same linked list. The old shape also put the empty
	   CacheEntry into the map BEFORE the query filled it, so a second thread resolving the same name in
	   that window was told the method does not exist. */
	private static final Map<String, CacheEntry> methodCache = new ConcurrentHashMap<>();

	public static Map<String, AbstractMethod> getAllMethods(final Traits traits) {

		final Map<String, AbstractMethod> allMethods = new LinkedHashMap<>();

		if (traits != null) {

			allMethods.putAll(traits.getDynamicMethods());

		} else {

			try {

				final PropertyKey<NodeInterface> schemaNodeKey = Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY);

				for (final NodeInterface globalMethod : StructrApp.getInstance().nodeQuery(StructrTraits.SCHEMA_METHOD).key(schemaNodeKey, null).getResultStream()) {

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

				/* Resolved outside the map, then published. Not computeIfAbsent(): the lookup opens a
				   transaction and a global method may resolve another one, and a mapping function that
				   comes back to the same map is a recursive update ConcurrentHashMap refuses. Two threads
				   racing here both run the query and one of the two equal entries wins, which costs a
				   query and decides nothing. */
				cacheEntry = resolveGlobalMethod(methodName);

				final CacheEntry existing = methodCache.putIfAbsent(methodName, cacheEntry);
				if (existing != null) {

					cacheEntry = existing;
				}
			}

			return cacheEntry.method();

		} else {

			final Map<String, AbstractMethod> methods = type.getDynamicMethods();

			return methods.get(methodName);
		}
	}

	public static void clearMethodCache() {

		methodCache.clear();
	}

	// ----- private static methods -----

	/** Look up a global schema method by name. Returns an entry with a null method when there is none, because
	    the absence is worth caching too - that is what the cache did before and what keeps a script that calls
	    an undefined function from querying for it on every call. */
	private static CacheEntry resolveGlobalMethod(final String methodName) {

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final PropertyKey<NodeInterface> schemaNodeKey = Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY);
			final NodeInterface method                     = StructrApp.getInstance().nodeQuery(StructrTraits.SCHEMA_METHOD).name(methodName).key(schemaNodeKey, null).getFirst();
			final CacheEntry entry                         = new CacheEntry(method != null ? new ScriptMethod(method.as(SchemaMethod.class)) : null);

			tx.success();

			return entry;

		} catch (FrameworkException fex) {

			throw new RuntimeException(fex);
		}
	}

	/** final field, so an entry is safely published through the map without a volatile read on every hit. */
	private record CacheEntry(AbstractMethod method) {}
}
