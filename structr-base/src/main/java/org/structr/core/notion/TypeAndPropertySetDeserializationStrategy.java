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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.PropertiesNotFoundToken;
import org.structr.common.error.TypeToken;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.RelationProperty;

import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;

/**
 * Deserializes a {@link GraphObject} using a type and a set of property values.
 *
 *
 */
public class TypeAndPropertySetDeserializationStrategy<S, T extends NodeInterface> extends DeserializationStrategy<S, T> {

	private static final Logger logger = LoggerFactory.getLogger(TypeAndPropertySetDeserializationStrategy.class.getName());

	protected RelationProperty relationProperty = null;
	protected final Set<String> propertyKeys;
	protected final boolean createIfNotExisting;

	public TypeAndPropertySetDeserializationStrategy(final Set<String> propertyKeys) {
		this(false, propertyKeys);
	}

	public TypeAndPropertySetDeserializationStrategy(boolean createIfNotExisting, Set<String> propertyKeys) {

		this.createIfNotExisting = createIfNotExisting;
		this.propertyKeys        = propertyKeys;

		if (propertyKeys == null || propertyKeys.isEmpty()) {
			throw new IllegalStateException("TypeAndPropertySetDeserializationStrategy must contain at least one property.");
		}
	}

	@Override
	public void setRelationProperty(final RelationProperty relationProperty) {
		this.relationProperty = relationProperty;
	}

	@Override
	public T deserialize(final SecurityContext securityContext, final String type, final S source, final Object context) throws FrameworkException {

		if (source instanceof Map) {

			PropertyMap attributes = PropertyMap.inputTypeToJavaType(securityContext, type, (Map)source);
			return deserialize(securityContext, type, attributes);
		}

		if (source instanceof NodeInterface) {
			return (T) source;
		}

		if (source instanceof String && Settings.isValidUuid((String) source)) {

			return getTypedResult((T)StructrApp.getInstance(securityContext).getNodeById((String) source), type);

		}

		return null;
	}

	private T deserialize(final SecurityContext securityContext, String type, final PropertyMap attributes) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);

		if (attributes != null) {

			final List<T> result = new LinkedList<>();

			// Check if properties contain the UUID attribute
			if (attributes.containsKey(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY))) {

				result.add((T)app.getNodeById(attributes.get(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY))));

			} else {


				boolean attributesComplete = true;

				// Check if all property keys of the PropertySetNotion are present
				for (String key : propertyKeys) {
					attributesComplete &= attributes.containsKey(Traits.of(type).key(key));
				}

				if (attributesComplete) {

					final PropertyMap searchAttributes = new PropertyMap();
					for (final PropertyKey key : attributes.keySet()) {

						// only use attribute for searching if it is NOT
						// a related node property
						if (key.relatedType() == null) {

							searchAttributes.put(key, attributes.get(key));
						}
					}

					for (final NodeInterface n : app.nodeQuery(type).and(searchAttributes).getResultStream()) {
						result.add((T)n);
					}

				}
			}

			// just check for existance
			String errorMessage = null;
			final int size = result.size();
			switch (size) {

				case 0:

					if (createIfNotExisting) {

						// create node and return it
						NodeInterface newNode = app.create(type, attributes);
						if (newNode != null) {

							return (T)newNode;
						}
					}

					errorMessage = "No node found for the given properties and auto-creation not enabled";

					break;

				case 1:

					final T relatedNode = getTypedResult(result.get(0), type);
					if (!attributes.isEmpty()) {

						// set properties on related node?
						setProperties(securityContext, relatedNode, attributes);
					}

					return relatedNode;

				default:

					errorMessage = "Found " + size + " nodes for given type and properties, property set is ambiguous";
					logger.error(errorMessage +
						". This is often due to wrong modeling, or you should consider creating a uniquness constraint for " + type, size);

					break;
			}

			throw new FrameworkException(404, errorMessage, new PropertiesNotFoundToken(type, null, attributes));
		}

		return null;
	}

	private T getTypedResult(final T obj, String type) throws FrameworkException {

		if (!obj.getTraits().contains(type)) {
			throw new FrameworkException(422, "Node type mismatch", new TypeToken(type, null, type));
		}

		return obj;
	}


}
