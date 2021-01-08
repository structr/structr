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
package org.structr.core.script.polyglot.wrappers;

import org.apache.commons.lang3.ArrayUtils;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.Converter;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.Tx;
import org.structr.core.property.*;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.core.script.polyglot.cache.ExecutableTypeMethodCache;
import org.structr.core.script.polyglot.function.GrantFunction;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Actions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphObjectWrapper<T extends GraphObject> implements ProxyObject {
	private static final Logger logger = LoggerFactory.getLogger(GraphObjectWrapper.class);
	private final T node;
	private ActionContext actionContext;

	public GraphObjectWrapper(ActionContext actionContext, final T node) {
		this.node = node;
		this.actionContext = actionContext;
	}

	public T getOriginalObject() {

		return node;
	}

	@Override
	public Object getMember(String key) {

		if (getOriginalObject() instanceof GraphObjectMap) {

			return PolyglotWrapper.wrap(actionContext, ((GraphObjectMap) getOriginalObject()).get(new GenericProperty<>(key)));
		} else {

			// Check cache for already initialized executables
			ExecutableTypeMethodCache methodCache = actionContext.getExecutableTypeMethodCache();

			ProxyExecutable cachedExecutable = methodCache.getExecutable(node.getClass().getSimpleName(), key);

			if (cachedExecutable != null) {

				return cachedExecutable;
			}


			App app = StructrApp.getInstance();

			try (final Tx tx = app.tx()) {

				SchemaNode schemaNode = app.nodeQuery(SchemaNode.class).andName(node.getClass().getSimpleName()).getFirst();
				if (schemaNode != null) {

					for (SchemaMethod m : Iterables.toList(schemaNode.getSchemaMethodsIncludingInheritance())) {

						if (m.getName().equals(key) && !m.isStaticMethod()) {

							ProxyExecutable executable =  (ProxyExecutable) arguments -> {

								try {
									Map<String, Object> parameters = new HashMap<>();

									if (arguments.length == 1) {

										Object parameter = PolyglotWrapper.unwrap(actionContext, arguments[0]);
										if (parameter instanceof Map) {

											parameters = (Map<String, Object>) parameter;
										}
									}

									return PolyglotWrapper.wrap(actionContext, Actions.execute(actionContext.getSecurityContext(), node, SchemaMethod.getCachedSourceCode(m.getUuid()), parameters, node.getClass().getSimpleName() + "." + key, m.getUuid()));

								} catch (FrameworkException ex) {

									logger.error("Unexpected exception while trying to call static schema type method.", ex);
								}

								return null;
							};

							methodCache.cacheExecutable(node.getClass().getSimpleName(), key, executable);

							return executable;
						}

					}
				}

			} catch (FrameworkException ex) {

				logger.error("Unexpected exception while trying to retrieve member method of graph object.", ex);
			}

			if (key.equals("grant")) {

				// grant() on GraphObject needs special handling
				return new GrantFunction(actionContext, node);
			}

			PropertyKey propKey = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(node.getClass(), key);

			if (propKey != null) {

				if (propKey instanceof EndNodes || propKey instanceof StartNodes || propKey instanceof ArrayProperty || (propKey instanceof AbstractPrimitiveProperty && propKey.valueType().isArray())) {
					// RelationshipProperty needs special binding
					// ArrayProperty values need synchronized ProxyArrays as well
					return new PolyglotProxyArray(actionContext, node, propKey);
				} else if (propKey instanceof EndNode || propKey instanceof StartNode) {

					GraphObject graphObject = (GraphObject)node.getProperty(propKey);
					return new GraphObjectWrapper<>(actionContext, graphObject);
				} else if (propKey instanceof EnumProperty) {

					Object propValue = node.getProperty(propKey);
					if (propValue != null && propValue instanceof Enum) {
						return ((Enum)propValue).toString();
					}
					return propValue;
				} else {

					return PolyglotWrapper.wrap(actionContext, node.getProperty(propKey));
				}
			}

			return PolyglotWrapper.wrap(actionContext, node.getProperty(key));
		}
	}

	@Override
	public Object getMemberKeys() {
		if (getOriginalObject() instanceof GraphObjectMap) {

			return ((GraphObjectMap) getOriginalObject()).toMap().keySet().toArray();
		} else if (getOriginalObject() != null){

			return getOriginalObject().getPropertyKeys("all").stream().map(PropertyKey::dbName).toArray();
		}
		return null;
	}

	@Override
	public boolean hasMember(String key) {
		return true;
	}

	@Override
	public void putMember(String key, Value value) {
		Object unwrappedValue = PolyglotWrapper.unwrap(actionContext, value);

		if (getOriginalObject() instanceof GraphObjectMap) {

			((GraphObjectMap) getOriginalObject()).put(new GenericProperty<>(key), unwrappedValue);
		} else {

			try {

				final PropertyKey propKey = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(node.getClass(), key);

				if (propKey != null && unwrappedValue != null && !propKey.valueType().isAssignableFrom(unwrappedValue.getClass())) {

					PropertyConverter inputConverter = propKey.inputConverter(actionContext.getSecurityContext());

					if (inputConverter != null) {

						unwrappedValue = inputConverter.convert(unwrappedValue);
					}
				}
				node.setProperty(propKey, unwrappedValue);
			} catch (FrameworkException ex) {

				throw new RuntimeException(ex);
			}
		}
	}
}
