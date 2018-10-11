/**
 * Copyright (C) 2010-2018 Structr GmbH
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
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.PropertyView;
import org.structr.common.QueryRange;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Value;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.function.Functions;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

/**
 *
 *
 */
public abstract class StreamingWriter {

	private static final Logger logger                   = LoggerFactory.getLogger(StreamingWriter.class.getName());
	private static final Set<PropertyKey> idTypeNameOnly = new LinkedHashSet<>();

	static {

		idTypeNameOnly.add(GraphObject.id);
		idTypeNameOnly.add(AbstractNode.type);
		idTypeNameOnly.add(AbstractNode.name);
	}

	private final ExecutorService threadPool              = Executors.newWorkStealingPool();
	private final Map<String, Serializer> serializerCache = new LinkedHashMap<>();
	private final Map<String, Serializer> serializers     = new LinkedHashMap<>();
	private final Serializer<GraphObject> root            = new RootSerializer();
	private final Set<String> nonSerializerClasses        = new LinkedHashSet<>();
	private final DecimalFormat decimalFormat             = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	private String resultKeyName                          = "result";
	private boolean renderSerializationTime               = true;
	private boolean renderResultCount                     = true;
	private boolean reduceRedundancy                      = false;
	private int outputNestingDepth                        = 3;
	private int parallelizationThreshold                  = 100;
	private Value<String> propertyView                    = null;
	protected boolean indent                              = true;
	protected boolean compactNestedProperties             = true;

	public abstract RestWriter getRestWriter(final SecurityContext securityContext, final Writer writer);

	public StreamingWriter(final Value<String> propertyView, final boolean indent, final int outputNestingDepth) {

		this.parallelizationThreshold = Settings.JsonParallelizationThreshold.getValue(100);
		this.reduceRedundancy         = Settings.JsonRedundancyReduction.getValue(true);
		this.outputNestingDepth       = outputNestingDepth;
		this.propertyView             = propertyView;
		this.indent                   = indent;

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


		//this.writer = new StructrWriter(writer);
		//this.writer.setIndent("   ");
	}

	public void streamSingle(final SecurityContext securityContext, final Writer output, final GraphObject obj) throws IOException {

		final Set<Integer> visitedObjects = new LinkedHashSet<>();
		final RestWriter writer           = getRestWriter(securityContext, output);
		final String view                 = propertyView.get(securityContext);

		configureWriter(writer);

		writer.beginDocument(null, view);
		root.serialize(writer, obj, view, 0, visitedObjects);
		writer.endDocument();

	}

	public void stream(final SecurityContext securityContext, final Writer output, final Result result, final String baseUrl) throws IOException {

		long t0 = System.nanoTime();

		final RestWriter rootWriter = getRestWriter(securityContext, output);

		configureWriter(rootWriter);

		// result fields in alphabetical order
		final List<? extends GraphObject> results = result.getResults();
		final Set<Integer> visitedObjects         = new LinkedHashSet<>();
		final Integer outputNestingDepth          = result.getOutputNestingDepth();
		final Integer page                        = result.getPage();
		final Integer pageCount                   = result.getPageCount();
		final Integer pageSize                    = result.getPageSize();
		final String queryTime                    = result.getQueryTime();
		final Integer resultCount                 = result.getRawResultCount();
		final String searchString                 = result.getSearchString();
		final String sortKey                      = result.getSortKey();
		final String sortOrder                    = result.getSortOrder();
		final GraphObject metaData                = result.getMetaData();

		rootWriter.beginDocument(baseUrl, propertyView.get(securityContext));

		// open result set
		rootWriter.beginObject();

		if (outputNestingDepth != null) {
			rootWriter.name("output_nesting_depth").value(outputNestingDepth);
		}

		if (page != null) {
			rootWriter.name("page").value(page);
		}

		if (pageCount != null) {
			rootWriter.name("page_count").value(pageCount);
		}

		if (pageSize != null) {
			rootWriter.name("page_size").value(pageSize);
		}

		if (queryTime != null) {
			rootWriter.name("query_time").value(queryTime);
		}

		if (resultCount != null && renderResultCount) {
			rootWriter.name("result_count").value(resultCount);
		}

		if (results != null) {

			if (results.isEmpty() && result.isPrimitiveArray()) {

				rootWriter.name(resultKeyName).nullValue();

			} else if (results.isEmpty() && !result.isPrimitiveArray()) {

				rootWriter.name(resultKeyName).beginArray().endArray();

			} else if (result.isPrimitiveArray()) {

				rootWriter.name(resultKeyName);

				if (results.size() > 1) {
					rootWriter.beginArray();
				}

				if (securityContext.doMultiThreadedJsonOutput() && results.size() > parallelizationThreshold) {

					doParallel(results, rootWriter, visitedObjects, (writer, o, nestedObjects) -> {

						writePrimitive(o, writer, nestedObjects);
					});

				} else {

					for (final Object object : results) {

						if (object != null) {

							writePrimitive(object, rootWriter, visitedObjects);
						}
					}
				}

				if (results.size() > 1) {

					rootWriter.endArray();

				}

			} else {

				final String localPropertyView  = propertyView.get(null);

				// result is an attribute called via REST API
				if (results.size() > 1 && !result.isCollection()) {

					throw new IllegalStateException(result.getClass().getSimpleName() + " is not a collection resource, but result set has size " + results.size());
				}

				if (result.isCollection()) {

					rootWriter.name(resultKeyName).beginArray();

					if (securityContext.doMultiThreadedJsonOutput() && results.size() > parallelizationThreshold) {

						doParallel(results, rootWriter, visitedObjects, (writer, o, nestedObjects) -> {

							root.serialize(writer, (GraphObject)o, localPropertyView, 0, nestedObjects);
						});

					} else {

						// serialize list of results
						for (GraphObject graphObject : results) {

							root.serialize(rootWriter, graphObject, localPropertyView, 0, visitedObjects);
						}
					}

					rootWriter.endArray();

				} else {

					rootWriter.name(resultKeyName);
					root.serialize(rootWriter, results.get(0), localPropertyView, 0, visitedObjects);
				}
			}
		}

		if (searchString != null) {
			rootWriter.name("search_string").value(searchString);
		}

		if (sortKey != null) {
			rootWriter.name("sort_key").value(sortKey);
		}

		if (sortOrder != null) {
			rootWriter.name("sort_order").value(sortOrder);
		}

		if (metaData != null) {

			String localPropertyView  = propertyView.get(null);

			rootWriter.name("meta_data");
			root.serialize(rootWriter, metaData, localPropertyView, 0, visitedObjects);
		}

		if (renderSerializationTime) {
			rootWriter.name("serialization_time").value(decimalFormat.format((System.nanoTime() - t0) / 1000000000.0));
		}

		// finished
		rootWriter.endObject();
		rootWriter.endDocument();

		threadPool.shutdown();
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

		if (value != null && !Functions.NULL_STRING.equals(value)) {

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

	private void configureWriter(final RestWriter writer) {

		if (indent && !writer.getSecurityContext().doMultiThreadedJsonOutput()) {
			writer.setIndent("	");
		}
	}

	// ----- nested classes -----
	public abstract class Serializer<T> {

		public abstract void serialize(final RestWriter writer, final T value, final String localPropertyView, final int depth, final Set<Integer> visitedObjects) throws IOException;

		public void serializeRoot(final RestWriter writer, final Object value, final String localPropertyView, final int depth, final Set<Integer> visitedObjects) throws IOException {

			if (value != null) {

				Serializer serializer = getSerializerForType(value.getClass());
				if (serializer != null) {

					serializer.serialize(writer, value, localPropertyView, depth, visitedObjects);

					return;
				}
			}

			serializePrimitive(writer, value);
		}

		public void serializeProperty(final RestWriter writer, final PropertyKey key, final Object value, final String localPropertyView, final int depth, final Set<Integer> visitedObjects) {

			final SecurityContext securityContext = writer.getSecurityContext();

			try {
				final PropertyConverter converter = key.inputConverter(securityContext);
				if (converter != null) {

					Object convertedValue = null;

					// ignore conversion errors
					try { convertedValue = converter.revert(value); } catch (Throwable t) {}

					serializeRoot(writer, convertedValue, localPropertyView, depth, visitedObjects);

				} else {

					serializeRoot(writer, value, localPropertyView, depth, visitedObjects);
				}

			} catch(Throwable t) {

				logger.warn("Exception while serializing property {} ({}) declared in {} with valuetype {} (value = {}) : {}", new Object[] {
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
		public void serialize(final RestWriter writer, final GraphObject source, final String localPropertyView, final int depth, final Set<Integer> visitedObjects) throws IOException {

			int hashCode = -1;

			// mark object as visited
			if (source != null) {

				hashCode = source.hashCode();
				visitedObjects.add(hashCode);

				writer.beginObject(source);

				// prevent endless recursion by pruning at depth n
				if (depth <= outputNestingDepth) {

					// property keys
					Iterable<PropertyKey> keys = source.getPropertyKeys(localPropertyView);
					if (keys != null) {

						// speciality for the Ui view: limit recursive rendering to (id, name)
						if (compactNestedProperties && depth > 0 && (PropertyView.Ui.equals(localPropertyView) || PropertyView.All.equals(localPropertyView))) {
							keys = idTypeNameOnly;
						}

						for (final PropertyKey key : keys) {

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
									localKey = StructrApp.key(source.getClass(), AbstractNode.name.jsonName());
								}
							}

							final Object value = source.getProperty(localKey, range);
							if (value != null) {

								if (!(reduceRedundancy && value instanceof GraphObject && visitedObjects.contains(value.hashCode()))) {

									writer.name(key.jsonName());
									serializeProperty(writer, localKey, value, localPropertyView, depth+1, visitedObjects);
								}

							} else {

								writer.name(localKey.jsonName()).nullValue();
							}
						}
					}
				}

				writer.endObject(source);

				// unmark (visiting only counts for children)
				visitedObjects.remove(hashCode);
			}
		}
	}

	public class IterableSerializer extends Serializer<Iterable> {

		@Override
		public void serialize(final RestWriter parentWriter, final Iterable value, final String localPropertyView, final int depth, final Set<Integer> visitedObjects) throws IOException {

			final SecurityContext securityContext = parentWriter.getSecurityContext();

			parentWriter.beginArray();

			// prevent endless recursion by pruning at depth n
			if (depth <= outputNestingDepth) {

				if (securityContext.doMultiThreadedJsonOutput() && value instanceof List && ((List)value).size() > parallelizationThreshold) {

					doParallel((List)value, parentWriter, visitedObjects, (writer, o, nestedObjects) -> {

						serializeRoot(writer, o, localPropertyView, depth, nestedObjects);

					});

				} else {

					for (Object o : value) {

						serializeRoot(parentWriter, o, localPropertyView, depth, visitedObjects);
					}
				}
			}

			parentWriter.endArray();
		}
	}

	public class MapSerializer extends Serializer<Map<String, Object>> {

		@Override
		public void serialize(final RestWriter writer, final Map<String, Object> source, final String localPropertyView, final int depth, final Set<Integer> visitedObjects) throws IOException {

			writer.beginObject();

			// prevent endless recursion by pruning at depth n
			if (depth <= outputNestingDepth) {

				for (Map.Entry<String, Object> entry : source.entrySet()) {

					String key = entry.getKey();
					Object value = entry.getValue();

					writer.name(key);
					serializeRoot(writer, value, localPropertyView, depth+1, visitedObjects);
				}
			}

			writer.endObject();
		}
	}

	public class PropertyMapSerializer extends Serializer<PropertyMap> {

		public PropertyMapSerializer() {}

		@Override
		public void serialize(final RestWriter writer, final PropertyMap source, final String localPropertyView, final int depth, final Set<Integer> visitedObjects) throws IOException {

			writer.beginObject();

			// prevent endless recursion by pruning at depth n
			if (depth <= outputNestingDepth) {

				for (Map.Entry<PropertyKey, Object> entry : source.entrySet()) {

					final PropertyKey key   = entry.getKey();
					final Object value      = entry.getValue();

					writer.name(key.jsonName());
					serializeProperty(writer, key, value, localPropertyView, depth+1, visitedObjects);
				}
			}

			writer.endObject();
		}
	}

	// ----- private methods -----
	private void doParallel(final List list, final RestWriter parentWriter, final Set<Integer> visitedObjects, final Operation op) {

		final SecurityContext securityContext = parentWriter.getSecurityContext();
		final int numberOfPartitions          = (int)Math.rint(Math.log(list.size())) + 1;
		final List<List> partitions           = ListUtils.partition(list, numberOfPartitions);
		final List<Future<String>> futures    = new LinkedList<>();

		for (final List partition : partitions) {

			futures.add(threadPool.submit(() -> {

				final StringWriter buffer = new StringWriter();

				// avoid deadlocks by preventing writes in this transaction
				securityContext.setReadOnlyTransaction();

				try (final Tx tx = StructrApp.getInstance(securityContext).tx(false, false, false)) {

					final RestWriter bufferingRestWriter = getRestWriter(securityContext, buffer);
					final Set<Integer> nestedObjects     = new LinkedHashSet<>(visitedObjects);
					configureWriter(bufferingRestWriter);

					bufferingRestWriter.beginArray();

					for (final Object o : partition) {

						op.run(bufferingRestWriter, o, nestedObjects);
					}

					bufferingRestWriter.endArray();
					bufferingRestWriter.flush();

					tx.success();
				}

				final String data = buffer.toString();
				final String sub  = data.substring(1, data.length() - 1);

				return sub;

			}));
		}

		for (final Iterator<Future<String>> it = futures.iterator(); it.hasNext();) {

			try {

				final Future<String> future = it.next();
				final String raw            = future.get();

				parentWriter.raw(raw);

				if (it.hasNext()) {
					parentWriter.raw(",");
				}

			} catch (Throwable t) {

				t.printStackTrace();
			}
		}
	}

	private void writePrimitive(final Object o, final RestWriter writer, final Set<Integer> visitedObjects) throws IOException {

		if (o instanceof GraphObject) {

			final String localPropertyView    = propertyView.get(null);
			final GraphObject obj             = (GraphObject)o;
			final Iterator<PropertyKey> keyIt = obj.getPropertyKeys(localPropertyView).iterator();

			while (keyIt.hasNext()) {

				PropertyKey k = keyIt.next();
				Object value  = obj.getProperty(k);

				root.serializeProperty(writer, k, value, localPropertyView, 0, visitedObjects);

			}

		} else {

			writer.value(o.toString());
		}
	}

	private interface Operation {

		public void run(final RestWriter writer, final Object o, final Set<Integer> visitedObjects) throws IOException;
	}
}
