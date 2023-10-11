/*
 * Copyright (C) 2010-2023 Structr GmbH
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
package org.structr.core.rest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.Value;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Controls serialization and deserialization of graph objects (nodes
 * and relationships).
 */
public class GraphObjectGSONAdapter {

	private static final Logger logger                   = LoggerFactory.getLogger(GraphObjectGSONAdapter.class.getName());
	private static final long MAX_SERIALIZATION_TIME     = TimeUnit.SECONDS.toMillis(30);
	private static final Set<PropertyKey> idTypeNameOnly = new LinkedHashSet<>();

	static {

		idTypeNameOnly.add(GraphObject.id);
		idTypeNameOnly.add(AbstractNode.type);
		idTypeNameOnly.add(AbstractNode.name);
	}

	private final Map<String, Serializer> serializerCache = new LinkedHashMap<>(100);
	private final Map<String, Serializer> serializers     = new LinkedHashMap<>();
	private final Set<String> nonSerializerClasses        = new LinkedHashSet<>();
	private final Serializer<GraphObject> root            = new RootSerializer();
	private int outputNestingDepth                        = 3;
	private SecurityContext securityContext               = null;
	private Value<String> propertyView                    = null;
	protected boolean compactNestedProperties             = true;

	public GraphObjectGSONAdapter(Value<String> propertyView, final int outputNestingDepth) {

		this.securityContext    = SecurityContext.getSuperUserInstance();
		this.propertyView       = propertyView;
		this.outputNestingDepth = outputNestingDepth;

		serializers.put(GraphObject.class.getName(), root);
		serializers.put(PropertyMap.class.getName(), new PropertyMapSerializer());
		serializers.put(Iterable.class.getName(),    new IterableSerializer());
		serializers.put(Map.class.getName(),         new MapSerializer());

		nonSerializerClasses.add(Object.class.getName());
		nonSerializerClasses.add(String.class.getName());
		nonSerializerClasses.add(Integer.class.getName());
		nonSerializerClasses.add(Long.class.getName());
		nonSerializerClasses.add(Double.class.getName());
		nonSerializerClasses.add(Float.class.getName());
		nonSerializerClasses.add(Byte.class.getName());
		nonSerializerClasses.add(Character.class.getName());
		nonSerializerClasses.add(StringBuffer.class.getName());
		nonSerializerClasses.add(Boolean.class.getName());
	}

	public JsonElement serialize(GraphObject src, long startTime) {

		String localPropertyView  = propertyView.get(null);

		// check for timeout
		if (System.currentTimeMillis() > startTime + MAX_SERIALIZATION_TIME) {

			logger.error("JSON serialization took more than {} ms, aborted. Please review output view size or adjust timeout.", MAX_SERIALIZATION_TIME);

			return null;
		}

		return root.serialize(src, localPropertyView, 0);
	}

	public JsonElement serializeObject(Object src, long startTime) {

		String localPropertyView  = propertyView.get(null);

		// check for timeout
		if (System.currentTimeMillis() > startTime + MAX_SERIALIZATION_TIME) {

			logger.error("JSON serialization took more than {} ms, aborted. Please review output view size or adjust timeout.", MAX_SERIALIZATION_TIME);

			return null;
		}

		return root.serializeRoot(src, localPropertyView, 0);
	}

	private static JsonElement toPrimitive(final Object value) {

		JsonElement p = null;

		if (value != null) {

			if (value instanceof Number) {

				p = new JsonPrimitive((Number) value);

			} else if (value instanceof Character) {

				p = new JsonPrimitive((Character) value);

			} else if (value instanceof String) {

				p = new JsonPrimitive((String) value);

			} else if (value instanceof Boolean) {

				p = new JsonPrimitive((Boolean) value);

			} else {

				p = new JsonPrimitive(value.toString());

			}
		}

		return p;
	}

	private Serializer getSerializerForType(Class type) {

		Class localType       = type;
		Serializer serializer = serializerCache.get(type.getName());

		if (serializer == null && !nonSerializerClasses.contains(type.getName())) {

			do {
				serializer = serializers.get(localType.getName());

				if (serializer == null) {

					Set<Class> interfaces = new LinkedHashSet<>();
					collectAllInterfaces(localType, interfaces);

					for (Class interfaceType : interfaces) {

						serializer = serializers.get(interfaceType.getName());

						if (serializer != null) {
							break;
						}
					}
				}

				localType = localType.getSuperclass();

			} while (serializer == null && !localType.equals(Object.class));


			// cache found serializer
			if (serializer != null) {
				serializerCache.put(type.getName(), serializer);
			}
		}

		return serializer;
	}

	private void collectAllInterfaces(Class type, Set<Class> interfaces) {

		if (interfaces.contains(type)) {
			return;
		}

		for (Class iface : type.getInterfaces()) {

			collectAllInterfaces(iface, interfaces);
			interfaces.add(iface);
		}
	}

	public abstract class Serializer<T> {

		public abstract JsonElement serialize(T value, String localPropertyView, int depth);

		public JsonElement serializeRoot(Object value, String localPropertyView, int depth) {

			if (value != null) {

				Serializer serializer = getSerializerForType(value.getClass());
				if (serializer != null) {

					return serializer.serialize(value, localPropertyView, depth+1);
				}
			}

			return toPrimitive(value);
		}

		public JsonElement serializeProperty(PropertyKey key, Object value, String localPropertyView, int depth) {

			try {
				PropertyConverter converter = key.inputConverter(securityContext);

				if (converter != null) {

					try {
						return serializeRoot(converter.revert(value), localPropertyView, depth);

					} catch (ClassCastException cce) {

						// try to fix the database property
						value = key.fixDatabaseProperty(value);

						return serializeRoot(converter.revert(value), localPropertyView, depth);

					}

				} else {

					return serializeRoot(value, localPropertyView, depth);
				}

			} catch (Throwable t) {

				logger.warn("Exception while serializing property {} ({}) of entity {} (value {}) : {}", new Object[] {
					key.jsonName(),
					key.getClass(),
					key.getClass().getDeclaringClass(),
					value.getClass().getName(),
					value,
					t.getMessage()
				});
			}

			return null;
		}
	}

	public class RootSerializer extends Serializer<GraphObject> {

		@Override
		public JsonElement serialize(GraphObject source, String localPropertyView, int depth) {

			JsonObject jsonObject = new JsonObject();

			final String uuid = source.getUuid();
			if (uuid != null) {
				jsonObject.add("id", new JsonPrimitive(uuid));

			} else {

				logger.debug("Found object without UUID: {}", source);
			}

			// prevent endless recursion by pruning at depth n
			if (depth <= outputNestingDepth) {

				// property keys
				Iterable<PropertyKey> keys = source.getPropertyKeys(localPropertyView);
				if (keys != null) {

					// speciality for the Ui view: limit recursive rendering to (id, name)
					if (compactNestedProperties && depth > 0 && ((PropertyView.Ui.equals(localPropertyView) && !securityContext.isSuperUserSecurityContext()) || PropertyView.All.equals(localPropertyView))) {
						keys = idTypeNameOnly;
					}

					for (PropertyKey key : keys) {

						Object value = source.getProperty(key);
						PropertyKey localKey = key;

						if (value != null) {

							jsonObject.add(localKey.jsonName(), serializeProperty(key, value, localPropertyView, depth+1));

						} else {

							jsonObject.add(localKey.jsonName(), null);
						}
					}
				}
			}

			return jsonObject;
		}
	}

	public class IterableSerializer extends Serializer<Iterable> {

		@Override
		public JsonElement serialize(Iterable value, String localPropertyView, int depth) {

			// prevent endless recursion by pruning at depth n
			if (depth > outputNestingDepth) {

				return null;
			}

			final JsonArray array = new JsonArray();
			for (Object o : value) {

				array.add(serializeRoot(o, localPropertyView, depth));
			}

			return array;
		}
	}

	public class MapSerializer extends Serializer {

		@Override
		public JsonElement serialize(Object source, String localPropertyView, int depth) {

			JsonObject object = new JsonObject();

			// prevent endless recursion by pruning at depth n
			if (depth > outputNestingDepth) {

				final Object value = ((Map<String, Object>)source).get("id");
				object.add("id", toPrimitive(value));

			} else {

				for (Entry<String, Object> entry : ((Map<String, Object>)source).entrySet()) {

					String key   = entry.getKey();
					Object value = entry.getValue();

					object.add(key, serializeRoot(value, localPropertyView, depth));
				}
			}

			return object;
		}
	}

	public class PropertyMapSerializer extends Serializer<PropertyMap> {

		public PropertyMapSerializer() {}

		@Override
		public JsonElement serialize(PropertyMap source, String localPropertyView, int depth) {

			JsonObject object = new JsonObject();

			// prevent endless recursion by pruning at depth n
			if (depth > outputNestingDepth) {

				final Object value = ((Map<String, Object>)source).get("id");
				object.add("id", toPrimitive(value));

			} else {

				for (Entry<PropertyKey, Object> entry : source.entrySet()) {

					PropertyKey key = entry.getKey();
					Object value    = entry.getValue();

					object.add(key.jsonName(), serializeProperty(key, value, localPropertyView, depth));
				}
			}

			return object;
		}
	}
}
