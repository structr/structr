/*
 *  Copyright (C) 2010-2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.core;


//~--- JDK imports ------------------------------------------------------------

import com.google.gson.*;
import java.lang.reflect.Type;

import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.Property;
import org.structr.common.PropertyKey;

//~--- classes ----------------------------------------------------------------

/**
 * Controls serialization and deserialization of graph objects (nodes
 * and relationships).
 *
 * @author Christian Morgner
 */
public class GraphObjectGSONAdapter implements JsonSerializer<GraphObject>, JsonDeserializer<GraphObject> {

	private static final Logger logger       = Logger.getLogger(GraphObjectGSONAdapter.class.getName());
	private static final Property<String> id = new Property<String>("id");
	
	private int outputNestingDepth        = Services.getOutputNestingDepth();
	private PropertyKey idProperty        = null;
	private Value<String> propertyView    = null;
 	
	//~--- constructors ---------------------------------------------------

	public GraphObjectGSONAdapter(Value<String> propertyView, PropertyKey idProperty) {

		this.propertyView   = propertyView;
		this.idProperty     = idProperty;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public JsonElement serialize(GraphObject src, Type typeOfSrc, JsonSerializationContext context) {

		String localPropertyView     = propertyView.get(null);

		return serializeFlatNameValue(src, typeOfSrc, context, localPropertyView, 0);
	}

	@Override
	public GraphObject deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

		String localPropertyView       = propertyView.get(null);

		return deserializeFlatNameValue(json, typeOfT, context, localPropertyView, 0);
	}
	
	
	// ----- private methods -----
	private JsonElement serializeFlatNameValue(GraphObject src, Type typeOfSrc, JsonSerializationContext context, String localPropertyView, int depth) {

		// prevent endless recursion by pruning at depth 2
		if (depth > outputNestingDepth) {

			return null;

		}

		JsonObject jsonObject = new JsonObject();

		// id (only if idProperty is not set)
		if (idProperty == null) {

			jsonObject.add("id", new JsonPrimitive(src.getId()));

		} else {

			Object idPropertyValue = src.getProperty(idProperty);

			if (idPropertyValue != null) {

				String idString = idPropertyValue.toString();

				jsonObject.add("id", new JsonPrimitive(idString));

			}

		}

		// property keys
		Iterable<PropertyKey> keys = src.getPropertyKeys(localPropertyView);
		if(keys != null) {
			for (PropertyKey key : keys) {

				Object value = src.getProperty(key);

				if (value != null) {

					// id property mapping
					if (key.equals(idProperty)) {

						key = id;

					}

					if (value instanceof Iterable) {

						jsonObject.add(key.name(), serializeIterable((Iterable)value, typeOfSrc, context, localPropertyView, depth));

					} else if (value instanceof GraphObject) {

						GraphObject graphObject = (GraphObject) value;

						jsonObject.add(key.name(), this.serializeFlatNameValue(graphObject, typeOfSrc, context, localPropertyView, depth + 1));

					} else if (value instanceof Map) {

						jsonObject.add(key.name(), serializeMap((Map) value, typeOfSrc, context, localPropertyView, false, depth));

					} else {

	//                                      jsonObject.add(key, new JsonPrimitive(value.toString()));
						jsonObject.add(key.name(), primitive(value));
					}
				} else {

					jsonObject.add(key.name(), new JsonNull());

				}

			}
		}

		return jsonObject;
	}
	
	private GraphObject deserializeFlatNameValue(JsonElement json, Type typeOfT, JsonDeserializationContext context, String localPropertyView, int depth) throws JsonParseException {
		logger.log(Level.WARNING, "Deserialization of nested (key,value,type) objects not supported yet.");
		return null;
	}
	
	private JsonArray serializeIterable(Iterable value, Type typeOfSrc, JsonSerializationContext context, String localPropertyView, int depth) {
		
		JsonArray property = new JsonArray();

		for (Object o : value) {

			// non-null check in case a lazy evaluator returns null
			if (o != null) {

				if (o instanceof GraphObject) {

					GraphObject obj                      = (GraphObject) o;
					JsonElement recursiveSerializedValue = this.serializeFlatNameValue(obj, typeOfSrc, context, localPropertyView, depth + 1);

					if (recursiveSerializedValue != null) {

						property.add(recursiveSerializedValue);

					}

				} else if (o instanceof Map) {

					property.add(serializeMap((Map) o, typeOfSrc, context, localPropertyView, false, depth));

				} else if (o instanceof Iterable) {

					property.add(serializeIterable((Iterable) o, typeOfSrc, context, localPropertyView, depth));

				} else {

					// serialize primitive, this is for PropertyNotion
					// property.add(new JsonPrimitive(o.toString()));
					property.add(primitive(o));
				}

			}
		}

		return property;
	}
	
	
	private JsonObject serializePrimitive(PropertyKey key, Object value, boolean includeTypeInOutput) {

		JsonObject property = new JsonObject();

		// id property mapping
		if (key.equals(idProperty)) {

			key = id;

		}

		property.add("key", new JsonPrimitive(key.name()));

		if (value != null) {

			property.add("value", primitive(value));

			// include type?
			if (includeTypeInOutput) {

				String valueType = value.getClass().getSimpleName();

				property.add("type", new JsonPrimitive(valueType));

			}

		} else {

			property.add("value", new JsonNull());

			// include type?
			if (includeTypeInOutput) {

				property.add("type", new JsonNull());

			}

		}

		return property;
	}

	private JsonObject serializeMap(Map<String, Object> map, Type typeOfT, JsonSerializationContext context, String localPropertyView, boolean includeType, int depth) {

		JsonObject object = new JsonObject();

		for (Entry<String, Object> entry : map.entrySet()) {

			String key   = entry.getKey();
			Object value = entry.getValue();

			if (key != null) {

				// id property mapping
				if (key.equals(idProperty.name())) {

					key = "id";

				}

				if (value != null) {

					// serialize graph objects that are nested in the map..
					if (value instanceof GraphObject) {

						object.add(key, serializeFlatNameValue((GraphObject) value, typeOfT, context, localPropertyView, depth + 1));

					} else if(value instanceof Map) {
						
						object.add(key, serializeMap((Map)value, typeOfT, context, localPropertyView, false, depth + 1));

					} else if(value instanceof Iterable) {
						
						object.add(key, serializeIterable((Iterable)value, typeOfT, context, localPropertyView, depth));
						
					} else {

						object.add(key, primitive(value));

					}
				} else {

					object.add(key, new JsonNull());

				}
			}

		}

		return object;
	}

	private static JsonPrimitive primitive(final Object value) {

		JsonPrimitive p;

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

		return p;
	}
}
