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
package org.structr.core.notion;

import org.structr.common.EntityAndPropertiesContainer;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.TypeToken;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.search.SearchCommand;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.RelationProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deserializes a {@link GraphObject} using the UUID property.
 *
 *
 */
public class IdDeserializationStrategy<S, T extends NodeInterface> extends DeserializationStrategy<S, T> {

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

			if (source instanceof Map) {

				final Map<String, Object> properties = (Map<String, Object>) source;
				Class<T> actualType                  = type;
				T relatedNode                        = null;

				if (properties.containsKey(NodeInterface.id.jsonName())) {

					// fetch node by ID
					relatedNode = (T) app.getNodeById(properties.get(NodeInterface.id.jsonName()).toString());
					if (relatedNode != null) {

						// fetch type from actual node
						actualType = (Class)relatedNode.getClass();
					}

				} else if (properties.containsKey(NodeInterface.type.jsonName())) {

					final String typeFromInput = properties.get(NodeInterface.type.jsonName()).toString();
					actualType = StructrApp.getConfiguration().getNodeEntityClass(typeFromInput);

					// reset type on failed check
					if (actualType == null) {
						actualType = type;
					}
				}

				final PropertyMap convertedProperties  = PropertyMap.inputTypeToJavaType(securityContext, actualType, properties);
				final Set<PropertyKey> allProperties   = StructrApp.getConfiguration().getPropertySet(type, "all");
				final Map<String, Object> foreignProps = new HashMap<>();

				// If property map contains the uuid, search only for uuid
				if (convertedProperties.containsKey(GraphObject.id)) {

					// related node is already found
					if (relatedNode != null) {

						if ( !SearchCommand.isTypeAssignableFromOtherType(type, relatedNode.getClass()) ) {
							throw new FrameworkException(422, "Node type mismatch", new TypeToken(type.getSimpleName(), null, type.getSimpleName()));
						}

						for (final PropertyKey key : convertedProperties.keySet()) {

							if (!key.isUnique() && !key.isCompound() && !isIdentifying(actualType, key) && !allProperties.contains(key)) {

								// store "foreign" properties (those that are to be set on the newly created relationship
								foreignProps.put(key.jsonName(), properties.get(key.jsonName()));
							}
						}

						// node found, remove UUID
						convertedProperties.remove(GraphObject.id);
					}

				} else {

					final PropertyMap uniqueKeyValues  = new PropertyMap();

					for (final PropertyKey key : convertedProperties.keySet()) {

						if (key.isUnique() || key.isCompound() || isIdentifying(actualType, key)) {

							uniqueKeyValues.put(key, convertedProperties.get(key));

						} else if (!allProperties.contains(key)) {

							// store "foreign" properties (those that are to be set on the newly created relationship
							foreignProps.put(key.jsonName(), properties.get(key.jsonName()));
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

							return app.create(type, convertedProperties);

						} else {

							throw new FrameworkException(422, concat(
								"Cannot create ", relation.getOtherType(type).getSimpleName(),
								": no matching ", type.getSimpleName(),
								" found for the given property set ",
								convertedProperties,
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

					// set properties on related node?
					if (!convertedProperties.isEmpty()) {

						setProperties(securityContext, relatedNode, convertedProperties);
					}

					if (foreignProps.isEmpty()) {

						return relatedNode;

					} else {

						return (T)new EntityAndPropertiesContainer(relatedNode, foreignProps);

					}
				}

			} else if (type.isAssignableFrom(source.getClass())) {

				return (T)source;

			} else {

				// interpret source as a raw ID string and fetch entity
				final GraphObject obj = app.getNodeById(source.toString());

				if (obj != null && !type.isAssignableFrom(obj.getClass())) {
					throw new FrameworkException(422, "Node type mismatch", new TypeToken(obj.getClass().getSimpleName(), null, type.getSimpleName()));
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

	private boolean isIdentifying(final Class actualType, final PropertyKey key) {
		return (PrincipalInterface.class.isAssignableFrom(actualType) && ("name".equals(key.jsonName()) || "eMail".equals(key.jsonName())));
	}
}
