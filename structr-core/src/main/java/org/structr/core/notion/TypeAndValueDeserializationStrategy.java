/*
 *  Copyright (C) 2010-2013 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.core.notion;

import java.util.*;
import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.PropertiesNotFoundToken;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.TypeToken;
import org.structr.core.*;

//~--- classes ----------------------------------------------------------------

/**
 * Deserializes a {@link GraphObject} using a type and a property value.
 *
 * @author Christian Morgner
 */
public class TypeAndValueDeserializationStrategy implements DeserializationStrategy {

	private static final Logger logger = Logger.getLogger(TypeAndValueDeserializationStrategy.class.getName());

	//~--- fields ---------------------------------------------------------

	protected boolean createIfNotExisting     = false;
	protected PropertyKey<String> propertyKey = null;

	//~--- constructors ---------------------------------------------------

	public TypeAndValueDeserializationStrategy(PropertyKey<String> propertyKey, boolean createIfNotExisting) {

		this.createIfNotExisting = createIfNotExisting;
		this.propertyKey         = propertyKey;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public GraphObject deserialize(SecurityContext securityContext, Class type, Object source)
		throws FrameworkException {

		List<SearchAttribute> attrs = new LinkedList();

		attrs.add(Search.andExactTypeAndSubtypes(type.getSimpleName()));

		// TODO: check why this doesn't work for setProperty with plain uuid..
		
		if (source != null) {

			// FIXME: use uuid only here?
			if (source instanceof JsonInput) {

				Object value = ((JsonInput)source).getAttributes().get(propertyKey.jsonName());
				if (value != null) {
					
					String stringValue = value.toString();
					attrs.add(Search.andExactProperty(propertyKey, stringValue));
				}

			} else if (source instanceof GraphObject) {
				
				GraphObject obj = (GraphObject)source;
				if (propertyKey != null) {
					
					attrs.add(Search.andExactProperty(propertyKey, obj.getProperty(propertyKey)));
					
				} else {
					
					// fetch property key for "uuid", may be different for AbstractNode and AbstractRelationship!
					PropertyKey<String> idProperty = EntityContext.getPropertyKeyForDatabaseName(obj.getClass(), AbstractNode.uuid.dbName());
					attrs.add(Search.andExactUuid(obj.getProperty(idProperty)));
					
				}
				
				
			} else {

				attrs.add(Search.andExactProperty(propertyKey, source.toString()));

			}
		}


		// just check for existance
		Result result = Services.command(securityContext, SearchNodeCommand.class).execute(attrs);
		int resultCount = result.size();

		switch (resultCount) {

			case 0 :
				if ((source != null) && createIfNotExisting) {

					// create node and return it
					AbstractNode newNode = Services.command(securityContext, CreateNodeCommand.class).execute(
								   new NodeAttribute(AbstractNode.type, type.getSimpleName()),
								   new NodeAttribute(propertyKey, source.toString())
					                       );

					if (newNode != null) {
						
						return newNode;
						
					} else {

						logger.log(Level.WARNING,
							   "Unable to create node of type {0} for property {1}",
							   new Object[] { type.getSimpleName(),
									  propertyKey.jsonName() });
					}
				}

				break;

			case 1 :
				GraphObject obj = result.get(0);
				//if(!type.getSimpleName().equals(node.getType())) {
				if (!type.isAssignableFrom(obj.getClass())) {
					throw new FrameworkException("base", new TypeToken(propertyKey, type.getSimpleName()));
				}
				return obj;
		}

		if (source != null) {

			Map<PropertyKey, Object> attributes = new LinkedHashMap<PropertyKey, Object>();

			attributes.put(propertyKey,       source.toString());
			attributes.put(AbstractNode.type, type.getSimpleName());

			throw new FrameworkException(type.getSimpleName(), new PropertiesNotFoundToken(AbstractNode.base, attributes));
		}
		
		return null;
	}
}
