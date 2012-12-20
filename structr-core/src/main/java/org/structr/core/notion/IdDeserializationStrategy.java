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

import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.IdNotFoundToken;
import org.structr.core.GraphObject;
import org.structr.core.JsonInput;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.NodeAttribute;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.property.PropertyMap;
import org.structr.core.*;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class IdDeserializationStrategy<S, T extends GraphObject> implements DeserializationStrategy<S, T> {

	private static final Logger logger = Logger.getLogger(IdDeserializationStrategy.class.getName());

	//~--- fields ---------------------------------------------------------

	protected boolean createIfNotExisting     = false;
	protected PropertyKey<String> propertyKey = null;

	//~--- constructors ---------------------------------------------------

	public IdDeserializationStrategy() {}

	public IdDeserializationStrategy(PropertyKey<String> propertyKey, boolean createIfNotExisting) {

		this.propertyKey         = propertyKey;
		this.createIfNotExisting = createIfNotExisting;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public T deserialize(final SecurityContext securityContext, final Class<T> type, final S source) throws FrameworkException {

		if (source != null) {

			List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();

			// FIXME: use uuid only here?
			if (source instanceof JsonInput) {

				JsonInput properties = (JsonInput) source;
				PropertyMap map      = PropertyMap.inputTypeToJavaType(securityContext, type, properties.getAttributes());
				
				for (Entry<PropertyKey, Object> entry : map.entrySet()) {

					attrs.add(Search.andExactProperty(entry.getKey(), entry.getValue().toString()));

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

				attrs.add(Search.andExactUuid(source.toString()));

			}

			Result<T> results = Services.command(securityContext, SearchNodeCommand.class).execute(attrs);
			int size       = results.size();

			switch (size) {

				case 0 :
					throw new FrameworkException(type.getSimpleName(), new IdNotFoundToken(source));

				case 1 :
					return results.get(0);

				default :
					logger.log(Level.WARNING, "Got more than one result for UUID {0}. Either this is not an UUID or we have a collision.", source.toString());

			}

		} else if (createIfNotExisting) {

			return (T)Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<AbstractNode>() {

				@Override
				public AbstractNode execute() throws FrameworkException {

					// create node and return it
					AbstractNode newNode = Services.command(securityContext, CreateNodeCommand.class).execute(new NodeAttribute(AbstractNode.type, type.getSimpleName()));
					if (newNode == null) {

						logger.log(Level.WARNING, "Unable to create node of type {0} for property {1}", new Object[] { type.getSimpleName(), propertyKey.dbName() });

					}

					return newNode;
				}

			});

		}

		return null;
	}
}
