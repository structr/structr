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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import graphql.ExecutionResult;
import graphql.GraphQL;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graphql.GraphQLQuery;
import org.structr.core.graphql.GraphQLQueryConfiguration;
import org.structr.core.graphql.GraphQLRequest;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.SchemaService;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 *
 */
public class GraphQLWriter {

	private static final Logger logger                   = LoggerFactory.getLogger(GraphQLWriter.class.getName());

	private final Map<String, Serializer> serializerCache = new LinkedHashMap<>();
	private final Map<String, Serializer> serializers     = new LinkedHashMap<>();
	private final Serializer<GraphObject> root            = new RootSerializer();
	private final Set<String> nonSerializerClasses        = new LinkedHashSet<>();
	private final Set<Integer> visitedObjects             = ConcurrentHashMap.newKeySet();
	protected boolean indent                              = true;

	public GraphQLWriter(final boolean indent) {

		this.indent = indent;

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

	public void stream(final SecurityContext securityContext, final Writer output, final GraphQLRequest request) throws IOException, FrameworkException {

		final RestWriter writer = new StructrJsonWriter(securityContext, output);

		if (indent) {
			writer.setIndent("	");
		}

		if (request.hasSchemaQuery()) {

			// schema query is serialized from GraphQL execution result, doesn't need enclosing JSON object
			for (final GraphQLQuery query : request.getQueries()) {

				if (query.isSchemaQuery()) {

					// use graphql-java schema response
					final String originalQuery   = request.getOriginalQuery();
					final GraphQL graphQL        = GraphQL.newGraphQL(SchemaService.getGraphQLSchema()).build();
					final ExecutionResult result = graphQL.execute(originalQuery);
					final Gson gson              = new GsonBuilder().setPrettyPrinting().create();

					if (result != null) {

						final Map<String, Object> data = result.getData();
						if (data != null) {

							gson.toJson(data, output);
						}
					}
				}
			}

		} else {

			writer.beginDocument(null, null);
			writer.beginObject();

			for (final GraphQLQuery query : request.getQueries()) {

				writer.name(query.getFieldName());
				writer.beginArray();

				final int softLimit = securityContext.getSoftLimit(0);
				int count           = 0;

				for (final GraphObject object : query.getEntities(securityContext)) {

					root.serialize(writer, null, object, query, query.getRootPath());

					if (count++ > softLimit) {
						break;
					}
				}

				writer.endArray();
			}

			// finished
			writer.endObject();
			writer.endDocument();
		}
	}

	private Serializer getSerializerForType(final Class type) {

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

	private void collectAllInterfaces(final Class type, final Set<Class> interfaces) {

		if (interfaces.contains(type)) {
			return;
		}

		for (Class iface : type.getInterfaces()) {

			collectAllInterfaces(iface, interfaces);
			interfaces.add(iface);
		}
	}

	private void serializePrimitive(final RestWriter writer, final Object value) throws IOException {

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

		public abstract void serialize(final RestWriter writer, final PropertyKey key, final T value, final GraphQLQuery graphQLQuery, final String path) throws IOException;

		public void serializeRoot(final RestWriter writer, final PropertyKey key, final Object value, final GraphQLQuery graphQLQuery, final String path) throws IOException {

			if (value != null) {

				Serializer serializer = getSerializerForType(value.getClass());
				if (serializer != null) {

					serializer.serialize(writer, key, value, graphQLQuery, path);

					return;
				}
			}

			serializePrimitive(writer, value);
		}

		public void serializeProperty(final RestWriter writer, final PropertyKey key, final Object value, final GraphQLQuery graphQLQuery, final String path) {

			final SecurityContext securityContext = writer.getSecurityContext();

			try {
				final PropertyConverter converter = key.inputConverter(securityContext);
				if (converter != null) {

					Object convertedValue = null;

					// ignore conversion errors
					try { convertedValue = converter.revert(value); } catch (Throwable t) {}

					serializeRoot(writer, key, convertedValue, graphQLQuery, path);

				} else {

					serializeRoot(writer, key, value, graphQLQuery, path);
				}

			} catch(Throwable t) {

				logger.warn("Exception while serializing property {} ({}) of entity {} (value {}) : {}", new Object[] {
					key.jsonName(),
					key.getClass(),
					key.getClass().getDeclaringClass(),
					value.getClass().getName(),
					value,
					t.getMessage()
				});

				logger.warn(ExceptionUtils.getStackTrace(t));
			}
		}

		protected Predicate<GraphObject> getPredicate(final GraphQLQuery query, final String path, final PropertyKey key) {

			if (query != null) {

				final GraphQLQueryConfiguration subkeyConfig = query.getQueryConfiguration(path);
				if (subkeyConfig != null) {

					return subkeyConfig.getPredicateForPropertyKey(key);
				}
			}

			return null;
		}
	}

	public class RootSerializer extends Serializer<GraphObject> {

		@Override
		public void serialize(final RestWriter writer, final PropertyKey parentKey, final GraphObject source, final GraphQLQuery graphQLQuery, final String path) throws IOException {

			int hashCode = -1;

			// mark object as visited
			if (source != null) {

				final GraphQLQueryConfiguration propertyConfig = graphQLQuery.getQueryConfiguration(path);

				hashCode = source.hashCode();
				visitedObjects.add(hashCode);

				writer.beginObject(source, true);

				// property keys
				for (final PropertyKey key : propertyConfig.getPropertyKeys()) {

					final String name                      = key.jsonName();
					final Predicate<GraphObject> predicate = getPredicate(graphQLQuery, path + "/" + name, key);
					final Object value                     = source.getProperty(key, predicate);

					if (value != null) {

						writer.name(name);
						serializeProperty(writer, key, value, graphQLQuery, path + "/" + name);

					} else {

						writer.name(name).nullValue();
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
		public void serialize(final RestWriter writer, final PropertyKey key, final Iterable value, final GraphQLQuery graphQLQuery, final String path) throws IOException {

			final GraphQLQueryConfiguration config = graphQLQuery.getQueryConfiguration(path);
			final Predicate predicate              = config.getPredicateForPropertyKey(key);
			final SecurityContext securityContext  = writer.getSecurityContext();
			int pageSize                           = Integer.MAX_VALUE;
			int start                              = 0;
			int count                              = 0;
			int page                               = 1;
			int pos                                = 0;

			if (config != null) {

				pageSize = config.getPageSize();
				page     = config.getPage();
				start    = (page - 1) * pageSize;
			}

			final int softLimit = securityContext.getSoftLimit(config.getPageSize());

			writer.beginArray();

			for (Object o : value) {

				if (predicate == null || predicate.accept(o)) {

					if (pos++ >= start) {

						serializeRoot(writer, null, o, graphQLQuery, path);
						count++;
					}

					// stop serialization if page size is reached
					if (count >= pageSize) {
						break;
					}

					if (count > softLimit) {
						break;
					}
				}
			}

			writer.endArray();
		}
	}

	public class MapSerializer extends Serializer {

		@Override
		public void serialize(final RestWriter writer, final PropertyKey parentKey, final Object source, final GraphQLQuery graphQLQuery, final String path) throws IOException {

			final Set<String> requestedKeys = graphQLQuery.getPropertyKeys(path).stream().map(PropertyKey::jsonName).collect(Collectors.toSet());

			writer.beginObject();

			for (Map.Entry<String, Object> entry : ((Map<String, Object>)source).entrySet()) {

				final String key = entry.getKey();
				if (requestedKeys.contains(key)) {

					final Object value = entry.getValue();

					writer.name(key);
					serializeRoot(writer, null, value, graphQLQuery, path + "/" + key);
				}
			}

			writer.endObject();
		}
	}

	public class PropertyMapSerializer extends Serializer<PropertyMap> {

		public PropertyMapSerializer() {}

		@Override
		public void serialize(final RestWriter writer, final PropertyKey parentKey, final PropertyMap source, final GraphQLQuery graphQLQuery, final String path) throws IOException {

			final Set<PropertyKey> requestedKeys = graphQLQuery.getPropertyKeys(path);

			writer.beginObject();

			for (Map.Entry<PropertyKey, Object> entry : source.entrySet()) {

				final PropertyKey key = entry.getKey();
				if (requestedKeys.contains(key)) {

					final Object value = entry.getValue();
					final String name  = key.jsonName();

					writer.name(name);
					serializeProperty(writer, key, value, graphQLQuery, path + "/" + name);
				}
			}

			writer.endObject();
		}
	}
}
