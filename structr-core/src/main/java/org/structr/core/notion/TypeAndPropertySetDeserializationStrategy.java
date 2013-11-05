/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.core.notion;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;

//~--- JDK imports ------------------------------------------------------------

import org.structr.common.error.FrameworkException;
import org.structr.common.error.PropertiesNotFoundToken;
import org.structr.common.error.TypeToken;
import org.structr.core.JsonInput;
import org.structr.core.Result;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.NodeInterface;

//~--- classes ----------------------------------------------------------------

/**
 * Deserializes a {@link GraphObject} using a type and a set of property values.
 *
 * @author Christian Morgner
 */
public class TypeAndPropertySetDeserializationStrategy<S, T extends NodeInterface> implements DeserializationStrategy<S, T> {

	private static final Logger logger = Logger.getLogger(TypeAndPropertySetDeserializationStrategy.class.getName());
	
	protected PropertyKey[] propertyKeys  = null;
	protected boolean createIfNotExisting = false;

	//~--- constructors ---------------------------------------------------

	public TypeAndPropertySetDeserializationStrategy(PropertyKey... propertyKeys) {
		this(false, propertyKeys);
	}
	
	public TypeAndPropertySetDeserializationStrategy(boolean createIfNotExisting, PropertyKey... propertyKeys) {
		this.createIfNotExisting = createIfNotExisting;
		this.propertyKeys = propertyKeys;
	}

	//~--- methods --------------------------------------------------------

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

		if (attributes != null) {
			
			List<SearchAttribute> attrs = new LinkedList<>();

			// Check if properties contain the UUID attribute
			if (attributes.containsKey(GraphObject.uuid)) {
				
				attrs.add(Search.andExactUuid(attributes.get(GraphObject.uuid)));
				
			} else {

				
				boolean attributesComplete = true;
				
				// Check if all property keys of the PropertySetNotion are present
				for (PropertyKey key : propertyKeys) {
					attributesComplete &= attributes.containsKey(key);
				}
				
				if (attributesComplete) {

					attrs.add(Search.andExactTypeAndSubtypes(type));

					for (Entry<PropertyKey, Object> entry : attributes.entrySet()) {
						attrs.add(Search.andExactProperty(securityContext, entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null));
					}
					
				}
				
			
			}

			// just check for existance
			Result<T> result = Services.command(securityContext, SearchNodeCommand.class).execute(attrs);
			int size         = result.size();
			
			switch (size) {
				
				case 0:
					
					if (createIfNotExisting) {

						attributes.put(AbstractNode.type, type.getSimpleName());
						
						// create node and return it
						T newNode = (T)Services.command(securityContext, CreateNodeCommand.class).execute(attributes);
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
