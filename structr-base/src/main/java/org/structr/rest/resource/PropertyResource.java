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
package org.structr.rest.resource;


import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.rest.api.RESTCall;
import org.structr.core.property.*;
import org.structr.rest.api.RESTCallHandler;
import org.structr.api.Predicate;
import org.structr.api.search.SortOrder;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.CaseHelper;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.Notion;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.schema.SchemaHelper;

/**
 * A resource that calls getProperty() addressed by URL, like /TypeName/uuid/propertyName.
 *
 */
public class PropertyResource extends AbstractTypeIdLowercaseNameResource {

	@Override
	public RESTCallHandler handleTypeIdName(final RESTCall call, final String typeName, final String uuid, final String name) {

		final Class entityClass = SchemaHelper.getEntityClassForRawType(typeName);
		if (entityClass != null) {

			PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(entityClass, name, false);
			if (key == null) {

				// try to convert raw name into lower-case variable name
				key = StructrApp.getConfiguration().getPropertyKeyForJSONName(entityClass, CaseHelper.toLowerCamelCase(name), false);
			}

			if (key != null) {

				return new PropertyResourceHandler(call, entityClass, uuid, key);
			}
		}

		return null;
	}


	private class PropertyResourceHandler extends RESTCallHandler {

		private static final Logger logger = LoggerFactory.getLogger(PropertyResourceHandler.class);

		private PropertyKey propertyKey = null;
		private String typeName         = null;
		private String keyName          = null;
		private String uuid             = null;

		public PropertyResourceHandler(final RESTCall call, final String typeName, final String uuid, final PropertyKey propertyKey) {

			super(call);

			this.typeName    = typeName;
			this.uuid        = uuid;
			this.propertyKey = propertyKey;
			this.keyName     = propertyKey.jsonName();
		}

		@Override
		public ResultStream doGet(final SecurityContext securityContext, final SortOrder sortOrder, final int pageSize, final int page) throws FrameworkException {

			final Query query = StructrApp.getInstance(securityContext).nodeQuery();

			// use search context from type resource
			collectSearchAttributes(securityContext, typeName, query);

			final Predicate<GraphObject> predicate = query.toPredicate();

			final GraphObject sourceEntity = getEntity(securityContext, typeName, uuid);
			final Object value             = sourceEntity.getProperty(propertyKey, predicate);

			if (value != null) {

				if (value instanceof Iterable) {

					final Set<Object> propertyResults = new LinkedHashSet<>();
					Iterator<Object> iter = ((Iterable<Object>) value).iterator();
					boolean iterableContainsGraphObject = false;

					while (iter.hasNext()) {

						Object obj = iter.next();
						propertyResults.add(obj);

						if (obj != null && !iterableContainsGraphObject) {

							if (obj instanceof GraphObject) {

								iterableContainsGraphObject = true;
							}
						}
					}

					int rawResultCount = propertyResults.size();

					if (rawResultCount > 0 && !iterableContainsGraphObject) {

						GraphObjectMap gObject = new GraphObjectMap();

						gObject.setProperty(new ArrayProperty(typeName, Object.class), propertyResults.toArray());

						return new PagingIterable<>(getURL(), Arrays.asList(gObject));
					}

					final List<GraphObject> finalResult = new LinkedList<>();

					propertyResults.forEach(
						v -> finalResult.add((GraphObject) v)
					);

					applyDefaultSorting(finalResult, sortOrder);

					// return result
					return new PagingIterable<>(getURL(), finalResult, pageSize, page);

				} else if (value instanceof GraphObject) {

					return new PagingIterable<>(getURL(), Arrays.asList(value));

				} else {

					GraphObjectMap graphObjectMap = new GraphObjectMap();
					PropertyKey key;

					if (value instanceof String) {

						key = new StringProperty(keyName);

					} else if (value instanceof Integer) {

						key = new IntProperty(keyName);

					} else if (value instanceof Long) {

						key = new LongProperty(keyName);

					} else if (value instanceof Double) {

						key = new DoubleProperty(keyName);

					} else if (value instanceof Boolean) {

						key = new BooleanProperty(keyName);

					} else if (value instanceof Date) {

						key = new DateProperty(keyName);

					} else if (value instanceof String[]) {

						key = new ArrayProperty(keyName, String.class);

					} else {

						key = new GenericProperty(keyName);
					}

					graphObjectMap.setProperty(key, value);

					return new PagingIterable<>(getURL(), Arrays.asList(graphObjectMap));
				}
			}

			// check propertyKey to return the right variant of empty result
			if (!(propertyKey instanceof StartNode || propertyKey instanceof EndNode)) {

				return PagingIterable.EMPTY_ITERABLE;
			}

			return PagingIterable.EMPTY_ITERABLE;
		}

		@Override
		public RestMethodResult doPut(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {

			final GraphObject sourceEntity = getEntity(securityContext, entityClass, typeName, uuid);
			final App app                  = StructrApp.getInstance(securityContext);

			// fetch static relationship definition
			if (propertyKey instanceof RelationProperty) {

				if (propertyKey.isReadOnly()) {

					logger.info("Read-only property on {}: {}", sourceEntity.getClass(), typeName);
					return new RestMethodResult(HttpServletResponse.SC_FORBIDDEN);

				}

				final List<GraphObject> nodes = new LinkedList<>();

				// Now add new relationships for any new id: This should be the rest of the property set
				for (final Object obj : propertySet.values()) {

					nodes.add(app.getNodeById(obj.toString()));
				}

				// set property on source node
				sourceEntity.setProperty(propertyKey, nodes);
			}

			return new RestMethodResult(HttpServletResponse.SC_OK);
		}

		@Override
		public RestMethodResult doPost(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {

			final GraphObject sourceEntity = getEntity(securityContext, typeName, uuid);
			RestMethodResult result        = null;

			if (sourceEntity != null && propertyKey instanceof RelationProperty) {

				final RelationProperty relationProperty = (RelationProperty) propertyKey;
				final Class sourceNodeType              = sourceEntity.getClass();
				NodeInterface newNode                   = null;

				if (propertyKey.isReadOnly()) {

					logger.info("Read-only property on {}: {}", sourceNodeType, keyName);

					return null;
				}

				// fetch notion
				final Notion notion                  = relationProperty.getNotion();
				final PropertyKey primaryPropertyKey = notion.getPrimaryPropertyKey();
				final String relatedType             = relationProperty.getTargetType();

				// apply notion if the property set contains the ID property as the only element
				if (primaryPropertyKey != null && propertySet.containsKey(primaryPropertyKey.jsonName()) && propertySet.size() == 1) {

					logger.error("No implementation found");
					// FIXME: what happens here?
					Thread.dumpStack();

				} else {

					// the notion can not deserialize objects with a single key, or the POSTed propertySet did not contain a key to deserialize,
					// so we create a new node from the POSTed properties and link the source node to it. (this is the "old" implementation)
					newNode = createNode(securityContext, relatedType, propertySet);
					if (newNode != null) {

						relationProperty.addSingleElement(securityContext, sourceEntity, newNode);
					}
				}

				if (newNode != null) {

					result = new RestMethodResult(HttpServletResponse.SC_CREATED);
					result.addHeader("Location", buildLocationHeader(securityContext, newNode));

					return result;
				}
			}

			if (result != null) {
				return result;
			}

			throw new IllegalPathException("Illegal path");
		}

		@Override
		public RestMethodResult doDelete(final SecurityContext securityContext) throws FrameworkException {
			return genericDelete(securityContext);
		}

		@Override
		public Class getEntityClass(final SecurityContext securityContext) {

			if (entityClass == null && propertyKey != null) {

				return propertyKey.relatedType();
			}

			return entityClass;
		}

		@Override
		public boolean isCollection() {
			return propertyKey.isCollection();
		}

		@Override
		public String getResourceSignature() {
			return call.get("type") + "/_" + CaseHelper.toUpperCamelCase(propertyKey.jsonName());
		}

		@Override
		public Set<String> getAllowedHttpMethodsForOptionsCall() {
			return Set.of("DELETE", "GET", "OPTIONS", "PUT", "POST");
		}
	}

}
