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
package org.structr.core.notion;

import java.util.List;
import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.IdNotFoundToken;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.AmbiguousRelationToken;
import org.structr.core.GraphObject;
import org.structr.core.JsonInput;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.property.PropertyMap;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.RelationProperty;

/**
 * Deserializes a {@link GraphObject} using the UUID property.
 *
 * @author Christian Morgner
 */
public class IdDeserializationStrategy<S, T extends NodeInterface> implements DeserializationStrategy<S, T> {

	private static final Logger logger = Logger.getLogger(IdDeserializationStrategy.class.getName());

	protected RelationProperty<S> relationProperty = null;
	protected PropertyKey<String> propertyKey      = null;

	public IdDeserializationStrategy() {}

	public IdDeserializationStrategy(PropertyKey<String> propertyKey) {
		this.propertyKey = propertyKey;
	}

	@Override
	public void setRelationProperty(final RelationProperty<S> parentProperty) {
		this.relationProperty = parentProperty;
	}

	@Override
	public T deserialize(final SecurityContext securityContext, final Class<T> type, final S source) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);

		if (source != null) {

			if (source instanceof JsonInput) {

				final JsonInput properties = (JsonInput) source;
				final PropertyMap map      = PropertyMap.inputTypeToJavaType(securityContext, type, properties.getAttributes());
				T relatedNode              = null;

				// If property map contains the uuid, search only for uuid
				if (map.containsKey(GraphObject.id)) {

					relatedNode = (T) app.get(map.get(GraphObject.id));

				} else {

					final PropertyMap uniqueKeyValues = new PropertyMap();
					for (final PropertyKey key : map.keySet()) {

						if (key.isUnique()) {

							uniqueKeyValues.put(key, map.get(key));
						}
					}

					// try to find an entity for the given attributes, but only if they are unique
					//  (this is quite similar to the Cypher MERGE command),
					if (!uniqueKeyValues.isEmpty()) {

						final List<T> possibleResults = app.nodeQuery(type).and(uniqueKeyValues).getAsList();
						final int num                 = possibleResults.size();

						switch (num) {

							case 0:
								// not found => will be created
								break;

							case 1:
								relatedNode = possibleResults.get(0);
								break;

							default:
								// more than one => not unique??
								throw new FrameworkException(type.getSimpleName(), new AmbiguousRelationToken(propertyKey));
						}
					}
				}

				if (relatedNode == null) {

					// no related node found, should we create one?
					if (relationProperty != null) {

						final Relation relation = relationProperty.getRelation();
						if (relation != null) {

							switch (relation.getCascadingDeleteFlag()) {

								case Relation.ALWAYS:
								case Relation.CONSTRAINT_BASED:
								case Relation.SOURCE_TO_TARGET:
									return app.create(type, map);

								case Relation.TARGET_TO_SOURCE:
									logger.log(Level.INFO, "NOT creating related node for property {0} since cascading delete flag is TARGET_TO_SOURCE.", propertyKey);
									throw new FrameworkException(type.getSimpleName(), new IdNotFoundToken(source));

								case Relation.NONE:
									logger.log(Level.INFO, "NOT creating related node for property {0} since cascading delete flag is NONE.", propertyKey);
									throw new FrameworkException(type.getSimpleName(), new IdNotFoundToken(source));
							}
						}
					}

					logger.log(Level.INFO, "NOT creating related node for property {0} since no relation property was found.", propertyKey);
					throw new FrameworkException(type.getSimpleName(), new IdNotFoundToken(source));

				} else {

					return relatedNode;
				}

			} else if (source instanceof GraphObject) {

				// FIXME: does this happen at all??
				Thread.dumpStack();

				GraphObject obj = (GraphObject)source;
				if (propertyKey != null) {

					final Result<T> results = (Result<T>) app.nodeQuery(NodeInterface.class).and(propertyKey, obj.getProperty(propertyKey)).getResult();
					int size                = results.size();

					switch (size) {

						case 0 :
							throw new FrameworkException(type.getSimpleName(), new IdNotFoundToken(source));

						case 1 :
							return results.get(0);

						default :
							logger.log(Level.WARNING, "Got more than one result for UUID {0}. Either this is not an UUID or we have a collision.", source.toString());

					}


				} else {

					// fetch property key for "id", may be different for AbstractNode and AbstractRelationship!
					PropertyKey<String> idProperty = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(obj.getClass(), GraphObject.id.dbName());

					return (T) app.get(obj.getProperty(idProperty));

				}

			} else {

				// interpret source as a raw ID string and fetch entity
				return (T) app.get(source.toString());
			}
		}

		return null;
	}
}
