/**
 * Copyright (C) 2010-2016 Structr GmbH
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.TypeToken;
import org.structr.core.GraphObject;
import org.structr.core.JsonInput;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.search.SearchCommand;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.RelationProperty;

/**
 * Deserializes a {@link GraphObject} using the UUID property.
 *
 *
 */
public class IdDeserializationStrategy<S, T extends NodeInterface> implements DeserializationStrategy<S, T> {

	private static final Logger logger = LoggerFactory.getLogger(IdDeserializationStrategy.class.getName());

	protected RelationProperty<S> relationProperty = null;

	public IdDeserializationStrategy() {
	}

	@Override
	public void setRelationProperty(final RelationProperty<S> parentProperty) {
		this.relationProperty = parentProperty;
	}

	@Override
	public T deserialize(final SecurityContext securityContext, final Class<T> type, final S source, final Object context) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);

		if (source != null) {

			if (source instanceof JsonInput) {

				final JsonInput properties = (JsonInput) source;
				Class<T> concreteType = type;

				if (concreteType != null && concreteType.isInterface()) {

					// try to identify concrete type from input set
					// (creation wouldn't work otherwise anyway)
					if (properties.containsKey(NodeInterface.type.jsonName())) {

						final String typeFromInput = properties.get(NodeInterface.type.jsonName()).toString();
						concreteType = StructrApp.getConfiguration().getNodeEntityClass(typeFromInput);

						// reset type on failed check
						if (concreteType == null) {
							concreteType = type;
						}
					}
				}

				final PropertyMap map      = PropertyMap.inputTypeToJavaType(securityContext, concreteType, properties.getAttributes());
				T relatedNode              = null;

				// If property map contains the uuid, search only for uuid
				if (map.containsKey(GraphObject.id)) {

					relatedNode = (T) app.getNodeById(map.get(GraphObject.id));

					if (relatedNode != null) {

						if ( !SearchCommand.isTypeAssignableFromOtherType(type, relatedNode.getClass()) ) {
							throw new FrameworkException(422, "Node type mismatch", new TypeToken(type.getSimpleName(), null, type.getSimpleName()));
						}
					}

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
								throw new FrameworkException(422, concat(
									"Unable to resolve related node of type ",
									type.getSimpleName(),
									", ambiguous result: found ",
									num,
									" nodes for the given property set."
								));
						}

					} else {

						// throw exception here?

					}
				}

				if (relatedNode == null) {

					// no related node found, should we create one?
					if (relationProperty != null) {

						final Relation relation = relationProperty.getRelation();

						if (relationProperty.doAutocreate()) {

							return app.create(type, map);

						} else {

							throw new FrameworkException(422, concat(
								"Cannot create ", relation.getOtherType(type).getSimpleName(),
								": no matching ", type.getSimpleName(),
								" found for the given property set",
								" and autoCreate has a value of ",
								relationProperty.getAutocreateFlagName()
							));

						}
					}

					// FIXME: when can the relationProperty be null at all?
					throw new FrameworkException(500, concat(
						"Unable to resolve related node of type ",
						type.getSimpleName(),
						", no relation defined."
					));

				} else {

					return relatedNode;
				}

			} else if (type.isAssignableFrom(source.getClass())) {

				return (T)source;

			} else {

				// interpret source as a raw ID string and fetch entity
				final GraphObject obj = app.getNodeById(source.toString());

				if (obj != null && !type.isAssignableFrom(obj.getClass())) {
					throw new FrameworkException(422, "Node type mismatch", new TypeToken(type.getSimpleName(), null, type.getSimpleName()));
				}

				return (T) obj;

			}
		}

		return null;
	}

	private String concat(final Object... values) {

		final StringBuilder buf = new StringBuilder(values.length * 20);

		for (Object value : values) {
			buf.append(value);
		}

		return buf.toString();
	}
}
