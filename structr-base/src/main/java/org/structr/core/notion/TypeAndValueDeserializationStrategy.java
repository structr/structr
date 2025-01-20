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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.PropertiesNotFoundToken;
import org.structr.common.error.TypeToken;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.RelationProperty;
import org.structr.core.traits.Traits;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Deserializes a {@link GraphObject} using a type and a property value.
 *
 *
 */
public class TypeAndValueDeserializationStrategy<S, T extends NodeInterface> extends DeserializationStrategy<S, T> {

	private static final Logger logger = LoggerFactory.getLogger(TypeAndValueDeserializationStrategy.class.getName());

	protected RelationProperty relationProperty = null;
	protected boolean createIfNotExisting       = false;
	protected String propertyKey                = null;

	public TypeAndValueDeserializationStrategy(final String propertyKey, final boolean createIfNotExisting) {

		this.createIfNotExisting = createIfNotExisting;
		this.propertyKey         = propertyKey;

		if (propertyKey == null) {
			throw new IllegalStateException("TypeAndValueDeserializationStrategy must contain at least one property.");
		}
	}

	@Override
	public void setRelationProperty(RelationProperty relationProperty) {
		this.relationProperty = relationProperty;
	}

	@Override
	public T deserialize(final SecurityContext securityContext, String type, S source, final Object context) throws FrameworkException {

		final App app        = StructrApp.getInstance(securityContext);
		final List<T> result = new LinkedList<>();

		// default to UUID
		if (propertyKey == null) {
			propertyKey = "id";
		}

		// create and fill input map with source object
		Map<String, Object> sourceMap = new LinkedHashMap<>();
		sourceMap.put(propertyKey, source);

		// try to convert input type to java type in order to create object correctly
		PropertyMap convertedSourceMap = PropertyMap.inputTypeToJavaType(securityContext, type, sourceMap);
		Object convertedSource         = convertedSourceMap.get(propertyKey);

		if (convertedSource != null) {

			// FIXME: use uuid only here?
			if (convertedSource instanceof Map) {

				Object value = ((Map<String, Object>)convertedSource).get(propertyKey);
				if (value != null) {

					result.addAll(app.nodeQuery(type).and(propertyKey, value.toString()).getAsList());
				}

			} else if (convertedSource instanceof GraphObject) {

				final GraphObject obj = (GraphObject)convertedSource;

				result.addAll(app.nodeQuery(type).and(propertyKey, obj.getProperty(propertyKey)).getAsList());

			} else {

				result.addAll(app.nodeQuery(type).and(propertyKey, convertedSource).getAsList());
			}
		}

		// just check for existance
		int resultCount = result.size();

		switch (resultCount) {

			case 0 :
				if ((convertedSource != null) && createIfNotExisting) {

					// create node and return it
					NodeInterface newNode = app.create(type);

					if (newNode != null) {
						newNode.setProperty(propertyKey, convertedSource);
						return (T)newNode;
					}

				} else {

					logger.debug("Unable to create node of type {} for property {}", type, propertyKey);
				}

				break;

			case 1 :

				T obj = result.get(0);

				if (!obj.getTraits().contains(type)) {
					throw new FrameworkException(422, "Node type mismatch", new TypeToken(obj.getClass().getSimpleName(), propertyKey, type));
				}

				if (!convertedSourceMap.isEmpty()) {

					// set properties on related node?
					setProperties(securityContext, obj, convertedSourceMap);
				}

				return obj;
		}

		if (convertedSource != null) {

			PropertyMap attributes = new PropertyMap();

			attributes.put(propertyKey,           convertedSource);
			attributes.put(Traits.typeProperty(), type);

			throw new FrameworkException(404, "No node found for given properties", new PropertiesNotFoundToken(type, null, attributes));
		}

		return null;
	}

}
