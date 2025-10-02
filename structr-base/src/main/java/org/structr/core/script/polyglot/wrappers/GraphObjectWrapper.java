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
package org.structr.core.script.polyglot.wrappers;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessControllable;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Methods;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.core.script.polyglot.function.GrantFunction;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GraphObjectWrapper<T extends GraphObject> implements ProxyObject {

	private static final Logger logger = LoggerFactory.getLogger(GraphObjectWrapper.class);
	private final ActionContext actionContext;
	private final T node;

	public GraphObjectWrapper(final ActionContext actionContext, final T node) {

		this.actionContext = actionContext;
		this.node          = node;
	}

	public T getOriginalObject() {
		return node;
	}

	@Override
	public Object getMember(final String key) {

		if (node instanceof NodeInterface nodeInterface) {

			switch (key) {

				case "id":
					return this.node.getUuid();

				case "owner":
					final AccessControllable ac = nodeInterface.as(AccessControllable.class);
					return PolyglotWrapper.wrap(actionContext, ac.getOwnerNode());

				case "_path":
					return PolyglotWrapper.wrap(actionContext, nodeInterface.getPath(actionContext.getSecurityContext()));

				case "createdDate":
					return PolyglotWrapper.wrap(actionContext, this.node.getCreatedDate());

				case "lastModifiedDate":
					return PolyglotWrapper.wrap(actionContext, this.node.getLastModifiedDate());

				case "visibleToPublicUsers":
					return this.node.isVisibleToPublicUsers();

				case "visibleToAuthenticatedUsers":
					return this.node.isVisibleToAuthenticatedUsers();
			}
		}

		if (node instanceof GraphObjectMap) {

			return PolyglotWrapper.wrap(actionContext, ((GraphObjectMap)node).get(new GenericProperty<>(key)));

		} else {

			// Lookup method, if it's not in cache
			final AbstractMethod method = Methods.resolveMethod(node.getTraits(), key);
			if (method != null) {

				// dont call static methods here, but warn instead
				if (method.isStatic()) {

					// At this point method is guaranteed to be static since earlier isStatic check was true
					logger.warn("Tried calling a static type method in a non-static way on a type instance.");
					return null;
				}

				return method.getProxyExecutable(actionContext, node);

			} else if (key.equals("grant")) {

				// grant() on GraphObject needs special handling
				return new GrantFunction(actionContext, node);
			}

			final Traits traits = node.getTraits();

			if (traits == null) {
				return null;
			}

			final PropertyKey propKey = traits.key(key);
			if (propKey != null) {

				if (propKey instanceof EndNodes || propKey instanceof StartNodes || propKey instanceof ArrayProperty || (propKey instanceof AbstractPrimitiveProperty && propKey.isArray())) {

					// RelationshipProperty needs special binding
					// ArrayProperty values need synchronized ProxyArrays as well
					return new PolyglotProxyArray(actionContext, node, propKey);

				} else if (propKey instanceof EndNode || propKey instanceof StartNode) {

					GraphObject graphObject = (GraphObject)node.getProperty(propKey);
					if (graphObject != null) {

						return new GraphObjectWrapper<>(actionContext, graphObject);
					}

					return null;

				} else if (propKey instanceof EnumProperty) {

					Object propValue = node.getProperty(propKey);
					if (propValue != null && propValue instanceof Enum) {

						return ((Enum)propValue).toString();
					}

					return PolyglotWrapper.wrap(actionContext, propValue);

				}

				return PolyglotWrapper.wrap(actionContext, node.getProperty(propKey));
			}

			//return PolyglotWrapper.wrap(actionContext, node.getProperty(key));
			return null;
		}
	}

	@Override
	public Object getMemberKeys() {

		if (node instanceof GraphObjectMap) {

			return ((GraphObjectMap) node).toMap().keySet().toArray();

		} else if (node != null) {

			final List<String> members = new LinkedList<>();

			final Set<PropertyKey> keys = node.getPropertyKeys("all");
			if (keys != null) {

				members.addAll(keys.stream().map(k -> k.jsonName()).collect(Collectors.toList()));
			}

			for (final Map.Entry<String, AbstractMethod> entry : Methods.getAllMethods(node.getTraits()).entrySet()) {

				final AbstractMethod method = entry.getValue();
				if (method != null && !method.isPrivate()) {

					members.add(entry.getKey());
				}
			}

			return members.toArray();
		}

		return null;
	}

	@Override
	public boolean hasMember(final String key) {

		if (node instanceof GraphObjectMap) {

			return ((GraphObjectMap) node).containsKey(new GenericProperty<>(key));

		} else {

			if (node != null) {

				// Special handling for grant-Function
				if (key.equals("grant")) {
					return true;
				}

				final Traits traits       = node.getTraits();
				final boolean hasProperty = traits != null && traits.hasKey(key);

				return hasProperty || Methods.resolveMethod(traits, key) != null;

			} else {

				return false;
			}
		}
	}

	@Override
	public void putMember(final String key, final Value value) {

		Object unwrappedValue = PolyglotWrapper.unwrap(actionContext, value);

		if (node instanceof GraphObjectMap) {

			((GraphObjectMap) node).put(new GenericProperty<>(key), unwrappedValue);

		} else {

			try {

				final PropertyKey propKey = node.getTraits().key(key);

				// fixme: how to compare types here?
				if (propKey != null && unwrappedValue != null && !propKey.valueType().equals(unwrappedValue.getClass().getSimpleName())) {

					final PropertyConverter inputConverter = propKey.inputConverter(actionContext.getSecurityContext(), false);

					if (inputConverter != null) {

						try {

							unwrappedValue = inputConverter.convert(unwrappedValue);

						} catch (ClassCastException ex) {

							throw new FrameworkException(422, "Invalid input for key " + propKey.jsonName() + ", expected a " + propKey.typeName() + ".");
						}
					}
				}

				node.setProperty(propKey, unwrappedValue);

			} catch (FrameworkException ex) {

				throw new RuntimeException(ex);
			}
		}
	}

	@Override
	public boolean removeMember(String key) {

		final PropertyKey propKey = node.getTraits().key(key);

		if (node instanceof GraphObjectMap) {

			if (((GraphObjectMap) node).containsKey(propKey)) {

				((GraphObjectMap) node).remove(propKey);
				return true;
			} else {

				return false;
			}
		} else {

			try {

				node.removeProperty(propKey);
			} catch (FrameworkException ex) {

				throw new RuntimeException(ex);
			}
		}

		return false;
	}

	@Override
	public String toString() {

		if (node instanceof GraphObjectMap) {

			return super.toString();
		}

		return node.getClass().getSimpleName() + "(" + node.getUuid() + ")";
	}
}
