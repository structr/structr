/**
 * Copyright (C) 2010-2015 Structr GmbH
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
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.structr.common.PermissionResolutionMask;
import org.structr.common.PropertyView;
import org.structr.common.QueryRange;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

/**
 *
 *
 */
public abstract class StreamingWriter {

	private static final Logger logger                   = Logger.getLogger(StreamingWriter.class.getName());
	private static final long MAX_SERIALIZATION_TIME     = TimeUnit.SECONDS.toMillis(300);
	private static final Set<PropertyKey> idNameOnly     = new LinkedHashSet<>();
	private static final Set<PropertyKey> structrGraph   = new LinkedHashSet<>();

	static {

		idNameOnly.add(GraphObject.id);
		idNameOnly.add(AbstractNode.name);

		structrGraph.add(GraphObject.id);
		structrGraph.add(AbstractNode.type);
		structrGraph.add(AbstractNode.name);
	}

	private final Map<String, Serializer> serializerCache = new LinkedHashMap<>();
	private final Map<String, Serializer> serializers     = new LinkedHashMap<>();
	private final Serializer<GraphObject> root            = new RootSerializer();
	private final Set<String> nonSerializerClasses        = new LinkedHashSet<>();
	private final Set<Integer> visitedObjects             = new ConcurrentHashSet<>();
	private final DecimalFormat decimalFormat             = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	private String resultKeyName                          = "result";
	private boolean renderSerializationTime               = true;
	private boolean renderResultCount                     = true;
	private boolean reduceRedundancy                      = false;
	private int outputNestingDepth                        = 3;
	private Value<String> propertyView                    = null;
	protected boolean indent                              = true;
	protected boolean compactNestedProperties             = true;

	public abstract RestWriter getRestWriter(final SecurityContext securityContext, final Writer writer);

	public StreamingWriter(final Value<String> propertyView, final boolean indent, final int outputNestingDepth) {

		this.outputNestingDepth = outputNestingDepth;
		this.propertyView       = propertyView;
		this.indent             = indent;

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

		try {
			this.reduceRedundancy = Boolean.valueOf(Services.getInstance().getConfigurationValue(Services.JSON_REDUNDANCY_REDUCTION, "false"));
		} catch (Throwable t) {
			logger.log(Level.WARNING, "Unable to parse value for {0} from configuration file, invalid value.", Services.JSON_REDUNDANCY_REDUCTION);
		}

		//this.writer = new StructrWriter(writer);
		//this.writer.setIndent("   ");
	}

	public void streamSingle(final SecurityContext securityContext, final Writer output, final GraphObject obj) throws IOException {

		final RestWriter writer = getRestWriter(securityContext, output);
		final String view       = propertyView.get(securityContext);

		if (indent) {
			writer.setIndent("   ");
		}

		writer.beginDocument(null, view);
		root.serialize(writer, obj, view, 0);
		writer.endDocument();

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

		if (resultCount != null && renderResultCount) {
			writer.name("result_count").value(resultCount);
		}

		if (results != null) {

			if (results.isEmpty()) {

				writer.name(resultKeyName).beginArray().endArray();

			} else if (result.isPrimitiveArray()) {

				writer.name(resultKeyName).beginArray();

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

					writer.name(resultKeyName).beginArray();

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

					writer.name(resultKeyName);
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

		if (renderSerializationTime) {
			writer.name("serialization_time").value(decimalFormat.format((System.nanoTime() - t0) / 1000000000.0));
		}

		// finished
		writer.endObject();
		writer.endDocument();
	}

	public void setResultKeyName(final String resultKeyName) {
		this.resultKeyName = resultKeyName;
	}

	public void setRenderSerializationTime(final boolean doRender) {
		this.renderSerializationTime = doRender;
	}

	public void setRenderResultCount(final boolean doRender) {
		this.renderResultCount = doRender;
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

			int hashCode = -1;

			// mark object as visited
			if (source != null) {

				hashCode = source.hashCode();
				visitedObjects.add(hashCode);
			}

			writer.beginObject(source);

			// prevent endless recursion by pruning at depth n
			if (depth <= outputNestingDepth) {

				// property keys
				Iterable<PropertyKey> keys = source.getPropertyKeys(localPropertyView);
				if (keys != null) {

					final PermissionResolutionMask permissionResolutionMask = source.getPermissionResolutionMask();

					// speciality for the Ui view: limit recursive rendering to (id, name)
					if (compactNestedProperties && depth > 0 && PropertyView.Ui.equals(localPropertyView)) {
						keys = idNameOnly;
					}

					for (final PropertyKey key : keys) {

						if (permissionResolutionMask == null || permissionResolutionMask.allowsProperty(key)) {

							final QueryRange range = writer.getSecurityContext().getRange(key.jsonName());
							if (range != null) {
								// Reset count for each key
								range.resetCount();
							}

							// special handling for the internal _graph view: replace name with
							// the name property from the ui view, in case it was overwritten
							PropertyKey localKey = key;

							if (View.INTERNAL_GRAPH_VIEW.equals(localPropertyView)) {

								if (AbstractNode.name.equals(localKey)) {

									// replace key
									localKey = StructrApp.getConfiguration().getPropertyKeyForJSONName(source.getClass(), AbstractNode.name.jsonName(), false);
								}
							}


							final Object value = source.getProperty(localKey, range);
							if (value != null) {

								if (!(reduceRedundancy && visitedObjects.contains(value.hashCode()))) {

									writer.name(key.jsonName());
									serializeProperty(writer, localKey, value, localPropertyView, depth+1);
								}

							} else {

								writer.name(localKey.jsonName()).nullValue();
							}
						}
					}
				}
			}

			writer.endObject(source);

			// unmark (visiting only counts for children)
			visitedObjects.remove(hashCode);
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

					final PropertyKey key   = entry.getKey();
					final Object value      = entry.getValue();

					writer.name(key.jsonName());
					serializeProperty(writer, key, value, localPropertyView, depth+1);
				}
			}

			writer.endObject();
		}
	}
}
