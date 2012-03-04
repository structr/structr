/*
 *  Copyright (C) 2011 Axel Morgner
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

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.IdNotFoundToken;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;

/**
 *
 * @author Christian Morgner
 */
public class IdDeserializationStrategy implements DeserializationStrategy {

	private static final Logger logger = Logger.getLogger(IdDeserializationStrategy.class.getName());

	@Override
	public GraphObject deserialize(SecurityContext securityContext, Class type, Object source) throws FrameworkException {

		if(source != null) {

			// try uuid first
			List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();
			attrs.add(Search.andExactUuid(source.toString()));

			List<AbstractNode> results = (List<AbstractNode>)Services.command(securityContext, SearchNodeCommand.class).execute(
				null, false, false, attrs
			);

			int size = results.size();

			switch(size) {

				case 0:
					GraphObject idResult = (GraphObject)Services.command(securityContext, FindNodeCommand.class).execute(source);
					if(idResult == null) {

						throw new FrameworkException(type.getSimpleName(), new IdNotFoundToken(source));

					} else {

						return idResult;
					}

				case 1:
					return results.get(0);

				default:
					logger.log(Level.WARNING, "Got more than one result for UUID {0}. Either this is not an UUID or we have a collision.", source.toString());
			}

		} else {

			logger.log(Level.WARNING, "Source was null!");
		}

		return (GraphObject)Services.command(securityContext, FindNodeCommand.class).execute(source);
	}
}
