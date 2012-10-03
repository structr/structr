/*
 *  Copyright (C) 2010-2012 Axel Morgner
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
import org.structr.common.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.PropertiesNotFoundToken;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.TypeToken;
import org.structr.core.Result;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class TypeAndValueDeserializationStrategy implements DeserializationStrategy {

	private static final Logger logger = Logger.getLogger(TypeAndValueDeserializationStrategy.class.getName());

	//~--- fields ---------------------------------------------------------

	protected boolean createIfNotExisting = false;
	protected PropertyKey propertyKey     = null;

	//~--- constructors ---------------------------------------------------

	public TypeAndValueDeserializationStrategy(PropertyKey propertyKey, boolean createIfNotExisting) {

		this.createIfNotExisting = createIfNotExisting;
		this.propertyKey         = propertyKey;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public GraphObject deserialize(SecurityContext securityContext, Class type, Object source)
		throws FrameworkException {

		List<SearchAttribute> attrs = new LinkedList();

		attrs.add(Search.andExactTypeAndSubtypes(type.getSimpleName()));

		if (source != null) {
			attrs.add(Search.andExactProperty(propertyKey, source.toString()));
		}


		// just check for existance
		Result result = (Result) Services.command(securityContext,
						   SearchNodeCommand.class).execute(null, false, false, attrs);
		int resultCount = result.size();

		switch (resultCount) {

			case 0 :
				if ((source != null) && createIfNotExisting) {

					// create node and return it
					AbstractNode newNode = (AbstractNode) Services.command(
								   securityContext, CreateNodeCommand.class).execute(
								   new NodeAttribute(
								       AbstractNode.Key.type.name(),
								       type.getSimpleName()), new NodeAttribute(
									   propertyKey.name(), source.toString()));

					if (newNode != null) {
						return newNode;
					} else {

						logger.log(Level.WARNING,
							   "Unable to create node of type {0} for property {1}",
							   new Object[] { type.getSimpleName(),
									  propertyKey.name() });
					}
				}

				break;

			case 1 :
				GraphObject obj = result.get(0);
				//if(!type.getSimpleName().equals(node.getType())) {
				if (!type.isAssignableFrom(obj.getClass())) {
					throw new FrameworkException("base", new TypeToken(propertyKey.name(), type.getSimpleName()));
				}
				return obj;
		}

		if (source != null) {

			Map<String, Object> attributes = new LinkedHashMap<String, Object>();

			attributes.put(propertyKey.name(), source.toString());
			attributes.put("type", type.getSimpleName());

			throw new FrameworkException(type.getSimpleName(),
						     new PropertiesNotFoundToken("base", attributes));
		}
		
		return null;
	}
}
