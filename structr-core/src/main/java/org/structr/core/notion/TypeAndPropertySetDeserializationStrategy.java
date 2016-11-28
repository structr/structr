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

import java.util.*;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.PropertiesNotFoundToken;
import org.structr.common.error.TypeToken;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.RelationProperty;

//~--- classes ----------------------------------------------------------------

/**
 * Deserializes a {@link GraphObject} using a type and a set of property values.
 *
 *
 */
public class TypeAndPropertySetDeserializationStrategy<S, T extends NodeInterface> implements DeserializationStrategy<S, T> {

	private static final Logger logger = LoggerFactory.getLogger(TypeAndPropertySetDeserializationStrategy.class.getName());

	protected RelationProperty relationProperty = null;
	protected PropertyKey[] propertyKeys        = null;
	protected boolean createIfNotExisting       = false;

	//~--- constructors ---------------------------------------------------

	public TypeAndPropertySetDeserializationStrategy(PropertyKey... propertyKeys) {
		this(false, propertyKeys);
	}

	public TypeAndPropertySetDeserializationStrategy(boolean createIfNotExisting, PropertyKey... propertyKeys) {
		this.createIfNotExisting = createIfNotExisting;
		this.propertyKeys = propertyKeys;
	}

	@Override
	public void setRelationProperty(RelationProperty<S> relationProperty) {
		this.relationProperty = relationProperty;
	}

	@Override
	public T deserialize(final SecurityContext securityContext, final Class<T> type, final S source, final Object context) throws FrameworkException {

		if (source instanceof Map) {

			PropertyMap attributes = PropertyMap.inputTypeToJavaType(securityContext, type, (Map)source);
			return deserialize(securityContext, type, attributes);
		}

		if (source != null && type.isAssignableFrom(source.getClass())) {
			return (T) source;
		}

		if (source != null && source instanceof String && Pattern.matches("[a-fA-F0-9]{32}", (String) source)) {

			return (T) getTypedResult(new Result(StructrApp.getInstance(securityContext).getNodeById((String) source), false), type);

		}

		return null;
	}

	private T deserialize(final SecurityContext securityContext, Class<T> type, final PropertyMap attributes) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);

		if (attributes != null) {

			Result<T> result = Result.EMPTY_RESULT;

			// Check if properties contain the UUID attribute
			if (attributes.containsKey(GraphObject.id)) {

				result = new Result(app.getNodeById(attributes.get(GraphObject.id)), false);

			} else {


				boolean attributesComplete = true;

				// Check if all property keys of the PropertySetNotion are present
				for (PropertyKey key : propertyKeys) {
					attributesComplete &= attributes.containsKey(key);
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

					result = app.nodeQuery(type).and(searchAttributes).getResult();

				}
			}

			// just check for existance
			String errorMessage = null;
			final int size = result.size();
			switch (size) {

				case 0:

					if (createIfNotExisting) {

						// create node and return it
						T newNode = app.create(type, attributes);
						if (newNode != null) {

							return newNode;
						}
					}

					errorMessage = "No node found for the given properties and auto-creation not enabled";

					break;

				case 1:
					return getTypedResult(result, type);

				default:

					errorMessage = "Found " + size + " nodes for given type and properties, property set is ambiguous";
					logger.error(""
						+ "This is often due to wrong modeling, or you should consider creating a uniquness constraint for " + type.getName(), size);

					break;
			}

			throw new FrameworkException(404, errorMessage, new PropertiesNotFoundToken(type.getSimpleName(), null, attributes));
		}

		return null;
	}

	private T getTypedResult(Result<T> result, Class<T> type) throws FrameworkException {

		final GraphObject obj = result.get(0);

		if (!type.isAssignableFrom(obj.getClass())) {
			throw new FrameworkException(422, "Node type mismatch", new TypeToken(type.getSimpleName(), null, type.getSimpleName()));
		}

		return result.get(0);
	}


}
