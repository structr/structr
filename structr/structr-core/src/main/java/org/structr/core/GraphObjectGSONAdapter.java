/*
 *  Copyright (C) 2011 Axel Morgner
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

import com.google.gson.*;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;

import org.structr.core.PropertySet.PropertyFormat;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

//~--- JDK imports ------------------------------------------------------------

import java.lang.reflect.Type;

import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Controls serialization and deserialization of graph objects (nodes
 * and relationships).
 *
 * @author Christian Morgner
 */
public class GraphObjectGSONAdapter implements JsonSerializer<GraphObject>, JsonDeserializer<GraphObject> {

	private static final Logger logger    = Logger.getLogger(GraphObjectGSONAdapter.class.getName());
	private String idProperty             = null;
	private int outputNestingDepth        = 1;
	private PropertyFormat propertyFormat = null;
	private Value<String> propertyView    = null;

	//~--- constructors ---------------------------------------------------

	public GraphObjectGSONAdapter(PropertyFormat propertyFormat, Value<String> propertyView, String idProperty) {

		this.propertyFormat = propertyFormat;
		this.propertyView   = propertyView;
		this.idProperty     = idProperty;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public JsonElement serialize(GraphObject src, Type typeOfSrc, JsonSerializationContext context) {

		String localPropertyView     = propertyView.get();
		JsonElement serializedOutput = null;

		switch (propertyFormat) {

			case NestedKeyValueType :
				serializedOutput = serializeNestedKeyValueType(src, typeOfSrc, context, true, localPropertyView, 0);

				break;

			case NestedKeyValue :
				serializedOutput = serializeNestedKeyValueType(src, typeOfSrc, context, false, localPropertyView, 0);

				break;

			case FlatNameValue :
				serializedOutput = serializeFlatNameValue(src, typeOfSrc, context, localPropertyView, 0);

				break;

		}

		return serializedOutput;
	}

	@Override
	public GraphObject deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

		String localPropertyView       = propertyView.get();
		GraphObject deserializedOutput = null;

		switch (propertyFormat) {

			case NestedKeyValueType :
				deserializedOutput = deserializeNestedKeyValueType(json, typeOfT, context, true, localPropertyView, 0);

				break;

			case NestedKeyValue :
				deserializedOutput = deserializeNestedKeyValueType(json, typeOfT, context, false, localPropertyView, 0);

				break;

			case FlatNameValue :
				deserializedOutput = deserializeFlatNameValue(json, typeOfT, context, localPropertyView, 0);

				break;

		}

		return deserializedOutput;
	}
	
	
	// ----- private methods -----
	private JsonElement serializeNestedKeyValueType(GraphObject src, Type typeOfSrc, JsonSerializationContext context, boolean includeTypeInOutput, String localPropertyView, int depth) {

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

		/*
		 * String type = src.getType();
		 * if (type != null) {
		 *
		 *       jsonObject.add("type", new JsonPrimitive(type));
		 *
		 * }
		 */

		// property keys
		JsonArray properties = new JsonArray();

		for (String key : src.getPropertyKeys(localPropertyView)) {

			Object value = src.getProperty(key);

			if (value instanceof Iterable) {

				JsonArray property = new JsonArray();

				for (Object o : (Iterable) value) {

					if (o instanceof GraphObject) {

						GraphObject obj                      = (GraphObject) o;
						JsonElement recursiveSerializedValue = this.serializeNestedKeyValueType(obj, typeOfSrc, context, includeTypeInOutput, localPropertyView, depth + 1);

						if (recursiveSerializedValue != null) {

							property.add(recursiveSerializedValue);

						}

					} else if (o instanceof Map) {

						properties.add(serializeMap((Map) o, typeOfSrc, context, localPropertyView, includeTypeInOutput, true, depth));

					} else {

						// serialize primitive, this is for PropertyNotion
						properties.add(serializePrimitive(key, o, includeTypeInOutput));
					}

					// TODO: Unterstützung von Notions mit mehr als einem Property bei der Ausgabe!
					// => neuer Typ?

				}

				properties.add(property);

			} else if (value instanceof GraphObject) {

				GraphObject graphObject = (GraphObject) value;

				properties.add(this.serializeNestedKeyValueType(graphObject, typeOfSrc, context, includeTypeInOutput, localPropertyView, depth + 1));

			} else if (value instanceof Map) {

				properties.add(serializeMap((Map) value, typeOfSrc, context, localPropertyView, includeTypeInOutput, true, depth));

			} else {

				properties.add(serializePrimitive(key, value, includeTypeInOutput));

			}

		}

		jsonObject.add("properties", properties);

		if (src instanceof AbstractNode) {

			// outgoing relationships
			Map<RelationshipType, Long> outRelStatistics = ((AbstractNode) src).getRelationshipInfo(Direction.OUTGOING);

			if (outRelStatistics != null) {

				JsonArray outRels = new JsonArray();

				for (Entry<RelationshipType, Long> entry : outRelStatistics.entrySet()) {

					RelationshipType relType = entry.getKey();
					Long count               = entry.getValue();
					JsonObject outRelEntry   = new JsonObject();

					outRelEntry.add("type", new JsonPrimitive(relType.name()));
					outRelEntry.add("count", new JsonPrimitive(count));
					outRels.add(outRelEntry);

				}

				jsonObject.add("out", outRels);

			}

			// incoming relationships
			Map<RelationshipType, Long> inRelStatistics = ((AbstractNode) src).getRelationshipInfo(Direction.INCOMING);

			if (inRelStatistics != null) {

				JsonArray inRels = new JsonArray();

				for (Entry<RelationshipType, Long> entry : inRelStatistics.entrySet()) {

					RelationshipType relType = entry.getKey();
					Long count               = entry.getValue();
					JsonObject inRelEntry    = new JsonObject();

					inRelEntry.add("type", new JsonPrimitive(relType.name()));
					inRelEntry.add("count", new JsonPrimitive(count));
					inRels.add(inRelEntry);

				}

				jsonObject.add("in", inRels);

			}
		} else if (src instanceof AbstractRelationship) {

			// start node id (for relationships)
			String startNodeId = ((AbstractRelationship) src).getStartNodeId();

			if (startNodeId != null) {

				jsonObject.add("startNodeId", new JsonPrimitive(startNodeId));

			}

			// end node id (for relationships)
			String endNodeId = ((AbstractRelationship) src).getEndNodeId();

			if (endNodeId != null) {

				jsonObject.add("endNodeId", new JsonPrimitive(endNodeId));

			}
		}

		return jsonObject;
	}

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
		for (String key : src.getPropertyKeys(localPropertyView)) {

			Object value = src.getProperty(key);

			if (value != null) {

				// id property mapping
				if (key.equals(idProperty)) {

					key = "id";

				}

				if (value instanceof Iterable) {

					JsonArray property = new JsonArray();

					for (Object o : (Iterable) value) {

						// non-null check in case a lazy evaluator returns null
						if (o != null) {

							if (o instanceof GraphObject) {

								GraphObject obj                      = (GraphObject) o;
								JsonElement recursiveSerializedValue = this.serializeFlatNameValue(obj, typeOfSrc, context, localPropertyView, depth + 1);

								if (recursiveSerializedValue != null) {

									property.add(recursiveSerializedValue);

								}

							} else if (o instanceof Map) {

								property.add(serializeMap((Map) o, typeOfSrc, context, localPropertyView, false, false, depth));

							} else {

								// serialize primitive, this is for PropertyNotion
								// property.add(new JsonPrimitive(o.toString()));
								property.add(primitive(o));
							}

						}
					}

					jsonObject.add(key, property);

				} else if (value instanceof GraphObject) {

					GraphObject graphObject = (GraphObject) value;

					jsonObject.add(key, this.serializeFlatNameValue(graphObject, typeOfSrc, context, localPropertyView, depth + 1));

				} else if (value instanceof Map) {

					jsonObject.add(key, serializeMap((Map) value, typeOfSrc, context, localPropertyView, false, false, depth));

				} else {

//                                      jsonObject.add(key, new JsonPrimitive(value.toString()));
					jsonObject.add(key, primitive(value));
				}
			} else {

				jsonObject.add(key, new JsonNull());

			}

		}

		return jsonObject;
	}
	
	private GraphObject deserializeFlatNameValue(JsonElement json, Type typeOfT, JsonDeserializationContext context, String localPropertyView, int depth) throws JsonParseException {
		logger.log(Level.WARNING, "Deserialization of nested (key,value,type) objects not supported yet.");
		return null;
	}
	
	private GraphObject deserializeNestedKeyValueType(JsonElement json, Type typeOfT, JsonDeserializationContext context, boolean includeTypeInOutput, String localPropertyView, int depth) {
		logger.log(Level.WARNING, "Deserialization of nested (key,value,type) objects not supported yet.");
		return null;
	}
	

	private JsonObject serializePrimitive(String key, Object value, boolean includeTypeInOutput) {

		JsonObject property = new JsonObject();

		// id property mapping
		if (key.equals(idProperty)) {

			key = "id";

		}

		property.add("key", new JsonPrimitive(key));

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

	private JsonObject serializeMap(Map<String, Object> map, Type typeOfT, JsonSerializationContext context, String localPropertyView, boolean includeType, boolean nested, int depth) {

		JsonObject object = new JsonObject();

		for (Entry<String, Object> entry : map.entrySet()) {

			String key   = entry.getKey();
			Object value = entry.getValue();

			if (key != null) {

				// id property mapping
				if (key.equals(idProperty)) {

					key = "id";

				}

				if (value != null) {

					// serialize graph objects that are nested in the map..
					if (value instanceof GraphObject) {

						if (nested) {

							object.add(key, serializeNestedKeyValueType((GraphObject) value, typeOfT, context, includeType, localPropertyView, depth + 1));

						} else {

							object.add(key, serializeFlatNameValue((GraphObject) value, typeOfT, context, localPropertyView, depth + 1));

						}

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
