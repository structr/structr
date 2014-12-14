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

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;

//~--- JDK imports ------------------------------------------------------------

import org.structr.common.error.FrameworkException;
import org.structr.common.error.PropertiesNotFoundToken;
import org.structr.common.error.TypeToken;
import org.structr.core.JsonInput;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.RelationProperty;

//~--- classes ----------------------------------------------------------------

/**
 * Deserializes a {@link GraphObject} using a type and a set of property values.
 *
 * @author Christian Morgner
 */
public class TypeAndPropertySetDeserializationStrategy<S, T extends NodeInterface> implements DeserializationStrategy<S, T> {

	private static final Logger logger = Logger.getLogger(TypeAndPropertySetDeserializationStrategy.class.getName());

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
	public T deserialize(SecurityContext securityContext, Class<T> type, S source) throws FrameworkException {

		if (source instanceof JsonInput) {

			PropertyMap attributes = PropertyMap.inputTypeToJavaType(securityContext, type, ((JsonInput)source).getAttributes());
			return deserialize(securityContext, type, attributes);
		}

		if (source instanceof Map) {

			PropertyMap attributes = PropertyMap.inputTypeToJavaType(securityContext, type, (Map)source);
			return deserialize(securityContext, type, attributes);
		}

		return null;
	}

	private T deserialize(SecurityContext securityContext, Class<T> type, PropertyMap attributes) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);

		if (attributes != null) {

			Result<T> result = Result.EMPTY_RESULT;

			// Check if properties contain the UUID attribute
			if (attributes.containsKey(GraphObject.id)) {

				result = new Result(app.get(attributes.get(GraphObject.id)), false);

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

					break;

				case 1:
					return getTypedResult(result, type);

				default:

					logger.log(Level.SEVERE, "Found {0} nodes for given type and properties, property set is ambiguous!\n"
						+ "This is often due to wrong modeling, or you should consider creating a uniquness constraint for " + type.getName(), size);

					break;
			}

			throw new FrameworkException(type.getSimpleName(), new PropertiesNotFoundToken(AbstractNode.base, attributes));
		}

		return null;
	}

	private T getTypedResult(Result<T> result, Class<T> type) throws FrameworkException {

		GraphObject obj = result.get(0);

		if(!type.isAssignableFrom(obj.getClass())) {
			throw new FrameworkException(type.getSimpleName(), new TypeToken(AbstractNode.base, type.getSimpleName()));
		}

		return result.get(0);
	}


}
