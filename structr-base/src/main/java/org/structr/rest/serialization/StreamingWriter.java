/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.io.QuietException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.util.ProgressWatcher;
import org.structr.api.util.ResultStream;
import org.structr.common.QueryRange;
import org.structr.common.RequestKeywords;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.Schema;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 *
 */
public abstract class StreamingWriter {

	private static final Logger logger                   = LoggerFactory.getLogger(StreamingWriter.class.getName());
	private static final Set<PropertyKey> idTypeNameOnly = new LinkedHashSet<>(Arrays.asList(GraphObject.id, AbstractNode.type, AbstractNode.name));

	private final ExecutorService threadPool              = Executors.newWorkStealingPool();
	private final Map<String, Serializer> serializerCache = new LinkedHashMap<>();
	private final Map<String, Serializer> serializers     = new LinkedHashMap<>();
	private final Serializer<GraphObject> root            = new RootSerializer();
	private final Set<String> nonSerializerClasses        = new LinkedHashSet<>();
	private final DecimalFormat decimalFormat             = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	private String resultKeyName                          = "result";
	private boolean serializeNulls                        = true;
	private boolean renderSerializationTime               = true;
	private boolean reduceRedundancy                      = false;
	private int outputNestingDepth                        = 3;
	private Value<String> propertyView                    = null;
	protected boolean indent                              = true;
	protected boolean wrapSingleResultInArray             = false;
	private int skippedDeletedObjects                     = 0;
	private Integer overriddenResultCount                 = null;

	private boolean reduceNestedObjectsForRestrictedViews = true;
	private int reduceNestedObjectsInRestrictedViewsDepth = Settings.JsonReduceNestedObjectsDepth.getValue();

	public abstract RestWriter getRestWriter(final SecurityContext securityContext, final Writer writer);

	public StreamingWriter(final Value<String> propertyView, final boolean indent, final int outputNestingDepth, final boolean wrapSingleResultInArray, final boolean serializeNulls) {

		this.wrapSingleResultInArray   = wrapSingleResultInArray;
		this.serializeNulls            = serializeNulls;
		this.reduceRedundancy          = Settings.JsonRedundancyReduction.getValue(true);
		this.outputNestingDepth        = outputNestingDepth;
		this.propertyView              = propertyView;
		this.indent                    = indent;

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

	public void streamSingle(final SecurityContext securityContext, final Writer output, final GraphObject obj) throws IOException {

		final Set<Integer> visitedObjects = new LinkedHashSet<>();
		final RestWriter writer           = getRestWriter(securityContext, output);
		final String view                 = propertyView.get(securityContext);

		configureWriter(writer);

		setReduceNestedObjectsInRestrictedViewsDepth(securityContext);

		writer.beginDocument(null, view);
		root.serialize(writer, obj, view, 0, visitedObjects);
		writer.endDocument();
	}

	public void stream(final SecurityContext securityContext, final Writer output, final ResultStream result, final String baseUrl) throws IOException {
		stream(securityContext, output, result, baseUrl, true);
	}

	public void stream(final SecurityContext securityContext, final Writer output, final ResultStream result, final String baseUrl, final boolean includeMetadata) throws IOException {

		long t0 = System.nanoTime();

		final RestWriter rootWriter = getRestWriter(securityContext, output);

		configureWriter(rootWriter);

		setReduceNestedObjectsInRestrictedViewsDepth(securityContext);

		// result fields in alphabetical order
		final Set<Integer> visitedObjects = new LinkedHashSet<>();
		final String queryTime            = result.getQueryTime();
		final Integer page                = result.getPage();
		final Integer pageSize            = result.getPageSize();
		final int softLimit               = securityContext.getSoftLimit(pageSize);
		long actualResultCount            = 0L;

		// make pageSize and page available to nested serializers
		rootWriter.setPageSize(pageSize);
		rootWriter.setPage(page);

		rootWriter.beginDocument(baseUrl, propertyView.get(securityContext));
		rootWriter.beginObject();

		if (result != null) {

			rootWriter.name(resultKeyName);

			actualResultCount = root.serializeRoot(rootWriter, result, propertyView.get(securityContext), 0, visitedObjects);

			rootWriter.flush();
		}

		if (includeMetadata) {

			long t1         = System.nanoTime(); // time delta for serialization
			int resultCount = -1;
			int pageCount   = -1;

			if (pageSize != null && !pageSize.equals(Integer.MAX_VALUE)) {

				if (page != null) {

					rootWriter.name("page").value(page);
				}

				rootWriter.name("page_size").value(pageSize);
			}

			if (queryTime != null) {
				rootWriter.name("query_time").value(queryTime);
			}

			if (actualResultCount == Settings.ResultCountSoftLimit.getValue()) {

				rootWriter.name("info").beginObject();
				rootWriter.name("message").value("Result size limited");
				rootWriter.name("limit").value(Settings.ResultCountSoftLimit.getValue());
				rootWriter.name("result_size").value(actualResultCount);
				rootWriter.name("hint").value("Use pageSize parameter to avoid automatic response size limit");
				rootWriter.endObject();
			}

			// in the future more conditions could be added to show different warnings
			final boolean hasWarnings = (skippedDeletedObjects > 0);

			if (hasWarnings) {

				rootWriter.name("warnings").beginArray();

				if (skippedDeletedObjects > 0) {
					rootWriter.beginObject();
					rootWriter.name("token").value("SKIPPED_OBJECTS");
					rootWriter.name("message").value("Skipped serializing " + skippedDeletedObjects + " object(s) because they were deleted between the creation and the serialization of the result. The result_count will differ from the number of returned results");
					rootWriter.name("skipped").value(skippedDeletedObjects);
					rootWriter.endObject();
				}

				rootWriter.endArray();
			}

			// make output available immediately
			rootWriter.flush();

			try (final JsonProgressWatcher watcher = new JsonProgressWatcher(rootWriter, 5000L)) {

				final int countLimit = securityContext.forceResultCount() ? Integer.MAX_VALUE : softLimit;

				resultCount = result.calculateTotalResultCount(watcher, countLimit);
				pageCount   = result.calculatePageCount(watcher, countLimit);
			}

			if (resultCount >= 0 && pageCount >= 0) {

				rootWriter.name("result_count").value(overriddenResultCount != null ? overriddenResultCount : resultCount);
				rootWriter.name("page_count").value(pageCount);
				rootWriter.name("result_count_time").value(decimalFormat.format((System.nanoTime() - t1) / 1000000000.0));

			}

			if (renderSerializationTime) {
				rootWriter.name("serialization_time").value(decimalFormat.format((System.nanoTime() - t0) / 1000000000.0));
			}
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

	public void setOverriddenResultCount(final int resultCount) {
		this.overriddenResultCount = resultCount;
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

	private long serializePrimitive(RestWriter writer, final Object value) throws IOException {

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

		return 1;
	}

	private void configureWriter(final RestWriter writer) {

		if (indent && !writer.getSecurityContext().doMultiThreadedJsonOutput()) {
			writer.setIndent("	");
		}

	}

	private void setReduceNestedObjectsInRestrictedViewsDepth (SecurityContext securityContext) {
		this.reduceNestedObjectsInRestrictedViewsDepth = Settings.JsonReduceNestedObjectsDepth.getValue();
		HttpServletRequest request = securityContext.getRequest();
		if (request != null) {
			this.reduceNestedObjectsInRestrictedViewsDepth = Services.parseInt(request.getParameter(RequestKeywords.OutputReductionDepth.keyword()), this.reduceNestedObjectsInRestrictedViewsDepth);
		}
	}

	// ----- nested classes -----
	public abstract class Serializer<T> {

		public abstract long serialize(final RestWriter writer, final T value, final String localPropertyView, final int depth, final Set<Integer> visitedObjects) throws IOException;

		public long serializeRoot(final RestWriter writer, final Object value, final String localPropertyView, final int depth, final Set<Integer> visitedObjects) throws IOException {

			if (value != null) {

				Serializer serializer = getSerializerForType(value.getClass());
				if (serializer != null) {

					return serializer.serialize(writer, value, localPropertyView, depth, visitedObjects);
				}
			}

			return serializePrimitive(writer, value);
		}

		public long serializeProperty(final RestWriter writer, final PropertyKey key, final Object value, final String localPropertyView, final int depth, final Set<Integer> visitedObjects) {

			final SecurityContext securityContext = writer.getSecurityContext();

			try {
				final PropertyConverter converter = key.inputConverter(securityContext);
				if (converter != null) {

					Object convertedValue = value;

					// ignore conversion errors
					try { convertedValue = converter.revert(value); } catch (Throwable t) {}

					return serializeRoot(writer, convertedValue, localPropertyView, depth, visitedObjects);

				} else {

					return serializeRoot(writer, value, localPropertyView, depth, visitedObjects);
				}

			} catch(Throwable t) {

				if (t instanceof QuietException || t.getCause() instanceof QuietException) {
					// ignore exceptions which (by jettys standards) should be handled less verbosely
				} else if (t instanceof IllegalStateException && t.getCause() == null && (t.getMessage() == null || t.getMessage().equals("Nesting problem."))) {
					// ignore exception. it is probably caused by a canceled request/closed connection which caused the JsonWriter to tilt
				} else {
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

			return 0L;
		}
	}

	public class RootSerializer extends Serializer<GraphObject> {

		@Override
		public long serialize(final RestWriter writer, final GraphObject source, final String localPropertyView, final int depth, final Set<Integer> visitedObjects) throws IOException {

			int hashCode = -1;

			// mark object as visited
			if (source != null) {

				hashCode = source.hashCode();
				final boolean notVisitedBefore = visitedObjects.add(hashCode);

				if (source.getPropertyContainer() != null && source.getPropertyContainer().isDeleted()) {
					skippedDeletedObjects++;
					return 1;
				}

				writer.beginObject(source);

				// prevent endless recursion by pruning at depth n
				if (depth <= outputNestingDepth) {

					// property keys (for nested objects check if view exists on type)
					Set<PropertyKey> keys = source.getPropertyKeys(localPropertyView);

					if ((keys == null || keys.isEmpty()) && depth > 0 && !StructrApp.getConfiguration().hasView(source.getClass(), localPropertyView)) {
						keys = idTypeNameOnly;
					}

					if (keys != null) {

						// speciality for all, custom and ui view: limit recursive rendering to (id, type, name)
						if (reduceNestedObjectsForRestrictedViews && depth > reduceNestedObjectsInRestrictedViewsDepth && Schema.RestrictedViews.contains(localPropertyView)) {
							keys = idTypeNameOnly;
						}

						// speciality nested nodes which were already rendered: limit recursive rendering (id, type, name)
						if (reduceRedundancy && !notVisitedBefore && depth > 0) {
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

							final Object value = source.getProperty(localKey, range);
							if (value != null) {

								writer.name(key.jsonName());
								serializeProperty(writer, localKey, value, localPropertyView, depth+1, visitedObjects);

							} else {

								if (localKey.isCollection()) {

									writer.name(localKey.jsonName()).beginArray().endArray();
								} else if (serializeNulls) {
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

			return 1;
		}
	}

	public class IterableSerializer extends Serializer<Iterable> {

		@Override
		public long serialize(final RestWriter parentWriter, final Iterable value, final String localPropertyView, final int depth, final Set<Integer> visitedObjects) throws IOException {

			final SecurityContext securityContext = parentWriter.getSecurityContext();
			final int pageSize                    = parentWriter.getPageSize();
			final int softLimit                   = securityContext.getSoftLimit(pageSize);
			final Iterator iterator               = value.iterator();
			final Object firstValue               = iterator.hasNext() ? iterator.next() : null;
			final Object secondValue              = iterator.hasNext() ? iterator.next() : null;
			long actualResultCount                = 0;

			if (!wrapSingleResultInArray && depth == 0 && firstValue != null && secondValue == null && !(value instanceof Collection) && !Settings.ForceArrays.getValue()) {

				// prevent endless recursion by pruning at depth n
				if (depth <= outputNestingDepth) {

					serializeRoot(parentWriter, firstValue, localPropertyView, depth, visitedObjects);
				}

			} else {

				parentWriter.beginArray();

				// prevent endless recursion by pruning at depth n
				if (depth <= outputNestingDepth) {

					// first value?
					if (firstValue != null) {
						serializeRoot(parentWriter, firstValue, localPropertyView, depth, visitedObjects);
						actualResultCount++;
					}

					// second value?
					if (secondValue != null) {

						serializeRoot(parentWriter, secondValue, localPropertyView, depth, visitedObjects);
						actualResultCount++;

						// more values?
						while (iterator.hasNext()) {

							serializeRoot(parentWriter, iterator.next(), localPropertyView, depth, visitedObjects);

							actualResultCount++;

 							if (actualResultCount == softLimit) {
								break;
							}
						}
					}
				}

				parentWriter.endArray();
			}

			return actualResultCount;
		}
	}

	public class MapSerializer extends Serializer<Map<String, Object>> {

		@Override
		public long serialize(final RestWriter writer, final Map<String, Object> source, final String localPropertyView, final int depth, final Set<Integer> visitedObjects) throws IOException {

			long count = 0L;

			writer.beginObject();

			// prevent endless recursion by pruning at depth n
			if (depth <= outputNestingDepth) {

				for (Map.Entry<String, Object> entry : source.entrySet()) {

					String key   = getString(entry.getKey());
					Object value = entry.getValue();

					writer.name(key);
					count = serializeRoot(writer, value, localPropertyView, depth+1, visitedObjects);
				}
			}

			writer.endObject();

			return count;
		}
	}

	public class PropertyMapSerializer extends Serializer<PropertyMap> {

		public PropertyMapSerializer() {}

		@Override
		public long serialize(final RestWriter writer, final PropertyMap source, final String localPropertyView, final int depth, final Set<Integer> visitedObjects) throws IOException {

			long count = 0;

			writer.beginObject();

			// prevent endless recursion by pruning at depth n
			if (depth <= outputNestingDepth) {

				for (Map.Entry<PropertyKey, Object> entry : source.entrySet()) {

					final PropertyKey key   = entry.getKey();
					final Object value      = entry.getValue();

					if (serializeNulls || value != null) {

						writer.name(key.jsonName());
						count = serializeProperty(writer, key, value, localPropertyView, depth+1, visitedObjects);
					}
				}
			}

			writer.endObject();

			return count;
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
				logger.error(ExceptionUtils.getStackTrace(t));
			}
		}
	}

	private String getString(final Object value) {

		if (value != null) {
			return value.toString();
		}

		throw new NullPointerException();
	}

	private interface Operation {

		public void run(final RestWriter writer, final Object o, final Set<Integer> visitedObjects) throws IOException;
	}

	private static class JsonProgressWatcher implements ProgressWatcher, AutoCloseable {

		private final SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss");
		private long lastUpdate           = System.currentTimeMillis();
		private boolean hasWritten        = false;
		private long interval             = 5000L;
		private RestWriter writer         = null;

		public JsonProgressWatcher(final RestWriter writer, final long interval) {

			this.interval = interval;
			this.writer   = writer;
		}

		@Override
		public boolean okToContinue(final int progress) {

			final long now = System.currentTimeMillis();

			if (now > lastUpdate + interval) {

				lastUpdate = now;

				try {

					// start array when writing the first entry (might not write anything)
					if (!hasWritten) {

						writer.name("status").value("Calculating overall result count");
						writer.name("progress");
						writer.beginArray();
						hasWritten = true;
					}

					writer.beginObject();
					writer.name("timestamp").value(df.format(System.currentTimeMillis()));
					writer.name("result_count").value(progress);
					writer.endObject();

					writer.flush();

				} catch (IOException ioex) {
					return false;
				}
			}

			return true;
		}

		@Override
		public void close() throws IOException {

			if (hasWritten) {
				writer.endArray();
			}
		}
	}
}
