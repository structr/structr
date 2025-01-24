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
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.search.SearchCommand;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.RelationProperty;
import org.structr.core.traits.Traits;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Deserializes a {@link GraphObject} using the UUID property.
 */
public class IdDeserializationStrategy<S, T extends NodeInterface> extends DeserializationStrategy<S, T> {

	protected RelationProperty relationProperty = null;

	public IdDeserializationStrategy() {
	}

	@Override
	public void setRelationProperty(final RelationProperty parentProperty) {
		this.relationProperty = parentProperty;
	}

	@Override
	public T deserialize(final SecurityContext securityContext, final String type, final S source, final Object context) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);
		final PropertyKey<String> idProperty = Traits.idProperty();

		if (source != null) {

			if (source instanceof Map) {

				final Map<String, Object> properties = (Map<String, Object>) source;
				Traits actualType                    = Traits.of(type);
				T relatedNode                        = null;

				if (properties.containsKey("id")) {

					// fetch node by ID
					relatedNode = (T) app.getNodeById(properties.get("id").toString());
					if (relatedNode != null) {

						// fetch type from actual node
						actualType = relatedNode.getTraits();
					}

				} else if (properties.containsKey("type")) {

					final String typeFromInput = properties.get("type").toString();
					final Traits traits        = Traits.of(typeFromInput);

					if (traits != null) {

						actualType = traits;
					}
				}

				final Map<String, Object> foreignProps = new HashMap<>();

				// store "foreign" properties (those that are to be set on the newly created relationship
				for (final String key : properties.keySet()) {

					if (!actualType.hasKey(key)) {

						foreignProps.put(key, properties.get(key));

						// remove from property set
						properties.remove(key);
					}
				}

				// convert properties
				final PropertyMap convertedProperties  = PropertyMap.inputTypeToJavaType(securityContext, actualType.getName(), properties);

				// If property map contains the uuid, search only for uuid
				if (convertedProperties.containsKey(Traits.idProperty())) {

					// related node is already found
					if (relatedNode != null) {

						if ( !SearchCommand.isTypeAssignableFromOtherType(actualType, relatedNode.getTraits()) ) {
							throw new FrameworkException(422, "Node type mismatch", new TypeToken(type, null, type));
						}

						// node found, remove UUID
						convertedProperties.remove(idProperty);
					}

				} else {

					final PropertyMap uniqueKeyValues  = new PropertyMap();

					for (final PropertyKey key : convertedProperties.keySet()) {

						if (key.isUnique() || key.isCompound() || isIdentifying(actualType, key)) {

							uniqueKeyValues.put(key, convertedProperties.get(key));
						}
					}

					// try to find an entity for the given attributes, but only if they are unique
					//  (this is quite similar to the Cypher MERGE command),
					if (!uniqueKeyValues.isEmpty()) {

						final List<T> possibleResults = convert(app.nodeQuery(type).and(uniqueKeyValues).getResultStream());
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
									type,
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

							return (T)app.create(type, convertedProperties);

						} else {

							throw new FrameworkException(422, concat(
								"Cannot create ", relation.getOtherType(type),
								": no matching ", type,
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
						type,
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

			} else if (source instanceof GraphObject g && g.getTraits().contains(type)) {

				return (T)source;

			} else {

				// interpret source as a raw ID string and fetch entity
				final GraphObject obj = app.getNodeById(source.toString());

				if (obj != null && !obj.getTraits().contains(type)) {
					throw new FrameworkException(422, "Node type mismatch", new TypeToken(obj.getClass().getSimpleName(), null, type));
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

	private boolean isIdentifying(final Traits actualType, final PropertyKey key) {
		return (actualType.contains("Principal") && ("name".equals(key.jsonName()) || "eMail".equals(key.jsonName())));
	}

	private List<T> convert(final Iterable<NodeInterface> iterable) {

		final List<T> list = new LinkedList<>();

		for (final NodeInterface n : iterable) {
			list.add((T)n);
		}

		return list;
	}
}
