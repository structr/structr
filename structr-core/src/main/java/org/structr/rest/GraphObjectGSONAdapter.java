/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
package org.structr.rest;


//~--- JDK imports ------------------------------------------------------------

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.Value;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.converter.PropertyConverter;

//~--- classes ----------------------------------------------------------------

/**
 * Controls serialization and deserialization of graph objects (nodes
 * and relationships).
 *
 * @author Christian Morgner
 */
public class GraphObjectGSONAdapter {

	private static final Logger logger                   = Logger.getLogger(GraphObjectGSONAdapter.class.getName());
	private static final long MAX_SERIALIZATION_TIME     = TimeUnit.SECONDS.toMillis(30);

	private final Map<Class, Serializer> serializerCache = new LinkedHashMap<>();
	private final Map<Class, Serializer> serializers     = new LinkedHashMap<>();
	private final Set<Class> nonSerializerClasses        = new LinkedHashSet<>();
	private final Serializer<GraphObject> root           = new RootSerializer();
	private int outputNestingDepth                       = 3;
	private SecurityContext securityContext              = null;
	private Value<String> propertyView                   = null;
	private JsonWriter writer                            = null;

	//~--- constructors ---------------------------------------------------

	public GraphObjectGSONAdapter(Value<String> propertyView, final int outputNestingDepth) {

		this.securityContext    = SecurityContext.getSuperUserInstance();
		this.propertyView       = propertyView;
		this.outputNestingDepth = outputNestingDepth;

		serializers.put(GraphObject.class, root);
		serializers.put(PropertyMap.class, new PropertyMapSerializer());
		serializers.put(Iterable.class,    new IterableSerializer());
		serializers.put(Map.class,         new MapSerializer());

		nonSerializerClasses.add(Object.class);
		nonSerializerClasses.add(String.class);
		nonSerializerClasses.add(Integer.class);
		nonSerializerClasses.add(Long.class);
		nonSerializerClasses.add(Double.class);
		nonSerializerClasses.add(Float.class);
		nonSerializerClasses.add(Byte.class);
		nonSerializerClasses.add(Character.class);
		nonSerializerClasses.add(StringBuffer.class);
		nonSerializerClasses.add(Boolean.class);
	}

	//~--- methods --------------------------------------------------------

	public JsonElement serialize(GraphObject src, long startTime) {

		String localPropertyView  = propertyView.get(null);

		// check for timeout
		if (System.currentTimeMillis() > startTime + MAX_SERIALIZATION_TIME) {

			logger.log(Level.SEVERE, "JSON serialization took more than {0} ms, aborted. Please review output view size or adjust timeout.", MAX_SERIALIZATION_TIME);

			return null;
		}

		return root.serialize(src, localPropertyView, 0);
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
		Serializer serializer = serializerCache.get(type);

		if (serializer == null && !nonSerializerClasses.contains(type)) {

			do {
				serializer = serializers.get(localType);

				if (serializer == null) {

					Set<Class> interfaces = new LinkedHashSet<>();
					collectAllInterfaces(localType, interfaces);

					for (Class interfaceType : interfaces) {

						serializer = serializers.get(interfaceType);

						if (serializer != null) {
							break;
						}
					}
				}

				localType = localType.getSuperclass();

			} while (serializer == null && !localType.equals(Object.class));


			// cache found serializer
			if (serializer != null) {
				serializerCache.put(type, serializer);
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

					return serializeRoot(converter.revert(value), localPropertyView, depth);

				} else {

					return serializeRoot(value, localPropertyView, depth);
				}

			} catch(Throwable t) {

				// CHM: remove debug code later
				t.printStackTrace();

				logger.log(Level.WARNING, "Exception while serializing property {0} ({1}) of entity {2} (value {3}) : {4}", new Object[] {
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

			jsonObject.add("id", new JsonPrimitive(source.getUuid()));

			// prevent endless recursion by pruning at depth n
			if (depth <= outputNestingDepth) {

				// property keys
				Iterable<PropertyKey> keys = source.getPropertyKeys(localPropertyView);
				if (keys != null) {
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

			JsonArray array = new JsonArray();

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
