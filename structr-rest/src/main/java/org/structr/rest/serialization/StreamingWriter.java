/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.serialization;

import java.io.IOException;
import java.util.List;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Value;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */
public abstract class StreamingWriter {

	private static final Logger logger                   = Logger.getLogger(StreamingWriter.class.getName());
	private static final long MAX_SERIALIZATION_TIME     = TimeUnit.SECONDS.toMillis(30);

	private final Map<Class, Serializer> serializerCache = new LinkedHashMap<>();
	private final Map<Class, Serializer> serializers     = new LinkedHashMap<>();
	private final Serializer<GraphObject> root           = new RootSerializer();
	private final Set<Class> nonSerializerClasses        = new LinkedHashSet<>();
	private final Property<String> id                    = new StringProperty("id");
	private final DecimalFormat decimalFormat            = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	private final PropertyKey idProperty                 = GraphObject.id;
	private int outputNestingDepth                       = 3;
	private Value<String> propertyView                   = null;
	protected boolean indent                             = true;

	public abstract RestWriter getRestWriter(final SecurityContext securityContext, final Writer writer);

	public StreamingWriter(Value<String> propertyView, boolean indent, final int outputNestingDepth) {

		this.outputNestingDepth = outputNestingDepth;
		this.propertyView       = propertyView;
		this.indent             = indent;

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

		//this.writer = new StructrWriter(writer);
		//this.writer.setIndent("   ");
	}

	public void stream(final SecurityContext securityContext, final Writer output, final Result result, final String baseUrl) throws IOException {

		long t0 = System.nanoTime();

		RestWriter writer = getRestWriter(securityContext, output);

		if (indent) {
			writer.setIndent("   ");
		}

		// result fields in alphabetical order
		List<? extends GraphObject> results = result.getResults();
		Integer page = result.getPage();
		Integer pageCount = result.getPageCount();
		Integer pageSize = result.getPageSize();
		String queryTime = result.getQueryTime();
		Integer resultCount = result.getRawResultCount();
		String searchString = result.getSearchString();
		String sortKey = result.getSortKey();
		String sortOrder = result.getSortOrder();
		GraphObject metaData = result.getMetaData();

		writer.beginDocument(baseUrl, propertyView.get(securityContext));

		// open result set
		writer.beginObject();

		if (page != null) {
			writer.name("page").value(page);
		}

		if (pageCount != null) {
			writer.name("page_count").value(pageCount);
		}

		if (pageSize != null) {
			writer.name("page_size").value(pageSize);
		}

		if (queryTime != null) {
			writer.name("query_time").value(queryTime);
		}

		if (resultCount != null) {
			writer.name("result_count").value(resultCount);
		}

		if (results != null) {

			if (results.isEmpty()) {

				writer.name("result").beginArray().endArray();

			} else if (result.isPrimitiveArray()) {

				writer.name("result").beginArray();

				for (GraphObject graphObject : results) {

					Object value = graphObject.getProperty(GraphObject.id);	// FIXME: UUID key hard-coded, use variable in Result here!
					if (value != null) {

						writer.value(value.toString());
					}
				}

				writer.endArray();


			} else {

				if (results.size() > 1 && !result.isCollection()){
					throw new IllegalStateException(result.getClass().getSimpleName() + " is not a collection resource, but result set has size " + results.size());
				}

				// keep track of serialization time
				long startTime            = System.currentTimeMillis();
				String localPropertyView  = propertyView.get(null);

				if (result.isCollection()) {

					writer.name("result").beginArray();

					// serialize list of results
					for (GraphObject graphObject : results) {

						root.serialize(writer, graphObject, localPropertyView, 0);

						// check for timeout
						if (System.currentTimeMillis() > startTime + MAX_SERIALIZATION_TIME) {

							logger.log(Level.SEVERE, "JSON serialization took more than {0} ms, aborted. Please review output view size or adjust timeout.", MAX_SERIALIZATION_TIME);

							// TODO: create some output indicating that streaming was interrupted
							break;
						}
					}

					writer.endArray();

				} else {

					writer.name("result");
					root.serialize(writer, results.get(0), localPropertyView, 0);
				}
			}
		}

		if (searchString != null) {
			writer.name("search_string").value(searchString);
		}

		if (sortKey != null) {
			writer.name("sort_key").value(sortKey);
		}

		if (sortOrder != null) {
			writer.name("sort_order").value(sortOrder);
		}

		if (metaData != null) {

			String localPropertyView  = propertyView.get(null);

			writer.name("meta_data");
			root.serialize(writer, metaData, localPropertyView, 0);
		}

		writer.name("serialization_time").value(decimalFormat.format((System.nanoTime() - t0) / 1000000000.0));

		// finished
		writer.endObject();
		writer.endDocument();
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

	private void serializePrimitive(RestWriter writer, final Object value) throws IOException {

		if (value != null) {

			if (value instanceof Number) {

				writer.value((Number)value);

			} else if (value instanceof Boolean) {

				writer.value((Boolean)value);

			} else {

				writer.value(value.toString());
			}

		} else {

			writer.nullValue();
		}
	}

	public abstract class Serializer<T> {

		public abstract void serialize(RestWriter writer, T value, String localPropertyView, int depth) throws IOException;

		public void serializeRoot(RestWriter writer, Object value, String localPropertyView, int depth) throws IOException {

			if (value != null) {

				Serializer serializer = getSerializerForType(value.getClass());
				if (serializer != null) {

					serializer.serialize(writer, value, localPropertyView, depth);

					return;
				}
			}

			serializePrimitive(writer, value);
		}

		public void serializeProperty(RestWriter writer, PropertyKey key, Object value, String localPropertyView, int depth) {

			try {
				PropertyConverter converter = key.inputConverter(writer.getSecurityContext());

				if (converter != null) {

					Object convertedValue = null;

					// ignore conversion errors
					try { convertedValue = converter.revert(value); } catch (Throwable t) {}

					serializeRoot(writer, convertedValue, localPropertyView, depth);

				} else {

					serializeRoot(writer, value, localPropertyView, depth);
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
		}
	}

	public class RootSerializer extends Serializer<GraphObject> {

		@Override
		public void serialize(RestWriter writer, GraphObject source, String localPropertyView, int depth) throws IOException {

			writer.beginObject(source);

			// prevent endless recursion by pruning at depth n
			if (depth <= outputNestingDepth) {


				/*
				// id (only if idProperty is not set)
				if (idProperty == null) {

					writer.name("id").value(source.getId());

				} else {

					Object idPropertyValue = source.getProperty(idProperty);
					if (idPropertyValue != null) {

						writer.name("id").value(idPropertyValue.toString());
					}

				}
				*/

				// property keys
				Iterable<PropertyKey> keys = source.getPropertyKeys(localPropertyView);
				if (keys != null) {

					for (PropertyKey key : keys) {

						Object value = source.getProperty(key);
						PropertyKey localKey = key;

						if (localKey.equals(idProperty)) {
							localKey = id;
						}

						if (value != null) {

							writer.name(localKey.jsonName());
							serializeProperty(writer, key, value, localPropertyView, depth+1);

						} else {

							writer.name(localKey.jsonName()).nullValue();
						}
					}
				}
			}

			writer.endObject(source);
		}
	}

	public class IterableSerializer extends Serializer<Iterable> {

		@Override
		public void serialize(RestWriter writer, Iterable value, String localPropertyView, int depth) throws IOException {

			writer.beginArray();

			// prevent endless recursion by pruning at depth n
			if (depth <= outputNestingDepth) {

				for (Object o : value) {

					serializeRoot(writer, o, localPropertyView, depth);
				}
			}

			writer.endArray();
		}
	}

	public class MapSerializer extends Serializer {

		@Override
		public void serialize(RestWriter writer, Object source, String localPropertyView, int depth) throws IOException {

			writer.beginObject();

			// prevent endless recursion by pruning at depth n
			if (depth <= outputNestingDepth) {

				for (Map.Entry<String, Object> entry : ((Map<String, Object>)source).entrySet()) {

					String key = entry.getKey();
					Object value = entry.getValue();

					// id property mapping again..
					if (idProperty.jsonName().equals(key)) {
						key = "id";
					}

					writer.name(key);
					serializeRoot(writer, value, localPropertyView, depth+1);
				}
			}

			writer.endObject();
		}
	}

	public class PropertyMapSerializer extends Serializer<PropertyMap> {

		public PropertyMapSerializer() {}

		@Override
		public void serialize(RestWriter writer, PropertyMap source, String localPropertyView, int depth) throws IOException {

			writer.beginObject();

			// prevent endless recursion by pruning at depth n
			if (depth <= outputNestingDepth) {

				for (Map.Entry<PropertyKey, Object> entry : source.entrySet()) {

					PropertyKey key = entry.getKey();
					if (key.equals(idProperty)) {

						key = id;
					}

					writer.name(key.jsonName());
					serializeProperty(writer, key, entry.getValue(), localPropertyView, depth+1);
				}
			}

			writer.endObject();
		}
	}
}
