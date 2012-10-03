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

import org.structr.common.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.IdNotFoundToken;
import org.structr.core.Command;
import org.structr.core.GraphObject;
import org.structr.core.PropertySet;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.*;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.search.*;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.Result;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class IdDeserializationStrategy implements DeserializationStrategy {

	private static final Logger logger = Logger.getLogger(IdDeserializationStrategy.class.getName());

	//~--- fields ---------------------------------------------------------

	protected boolean createIfNotExisting = false;
	protected PropertyKey propertyKey     = null;

	//~--- constructors ---------------------------------------------------

	public IdDeserializationStrategy() {}

	public IdDeserializationStrategy(PropertyKey propertyKey, boolean createIfNotExisting) {

		this.propertyKey         = propertyKey;
		this.createIfNotExisting = createIfNotExisting;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public GraphObject deserialize(final SecurityContext securityContext, final Class type, Object source) throws FrameworkException {

		if (source != null) {

			List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();

			// FIXME: use uuid only here?
			if (source instanceof PropertySet) {

				PropertySet properties = (PropertySet) source;

				for (NodeAttribute attr : properties.getAttributes()) {

					attrs.add(Search.andExactProperty(attr.getKey(), attr.getValue().toString()));

				}

			} else {

				attrs.add(Search.andExactUuid(source.toString()));

			}

			Result results = (Result) Services.command(securityContext, SearchNodeCommand.class).execute(null, false, false, attrs);
			int size                   = results.size();

			switch (size) {

				case 0 :
					GraphObject idResult = (GraphObject) Services.command(securityContext, FindNodeCommand.class).execute(source);

					if (idResult == null) {

						throw new FrameworkException(type.getSimpleName(), new IdNotFoundToken(source));

					} else {

						return idResult;

					}
				case 1 :
					return results.get(0);

				default :
					logger.log(Level.WARNING, "Got more than one result for UUID {0}. Either this is not an UUID or we have a collision.", source.toString());

			}

		} else if (createIfNotExisting) {

			Command transactionCommand = Services.command(securityContext, TransactionCommand.class);

			return (AbstractNode) transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					// create node and return it
					AbstractNode newNode = (AbstractNode) Services.command(securityContext,
								       CreateNodeCommand.class).execute(new NodeAttribute(AbstractNode.Key.type.name(), type.getSimpleName()));

					if (newNode == null) {

						logger.log(Level.WARNING, "Unable to create node of type {0} for property {1}", new Object[] { type.getSimpleName(), propertyKey.name() });

					}

					return newNode;
				}

			});

		}

		return null;
	}
}
