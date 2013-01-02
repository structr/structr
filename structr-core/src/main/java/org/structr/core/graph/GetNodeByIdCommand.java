/*
 *  Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
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
package org.structr.core.graph;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;

/**
 * Fetches a node by its UUID. This is just a convenience wrapper around the
 * {@link SearchNodeCommand}.
 * 
 * @author Christian Morgner
 */
public class GetNodeByIdCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(GetNodeByIdCommand.class.getName());
	
	public AbstractNode execute(String uuid) throws FrameworkException {

		List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();
		attrs.add(Search.andExactUuid(uuid));

		Result results = Services.command(securityContext, SearchNodeCommand.class).execute(attrs);
		int size       = results.size();

		if (size == 0) {
			return null;
		}

		if (size > 1) {
		
			logger.log(Level.WARNING, "Got more than one result for UUID {0}, this is very likely to be a UUID collision!", uuid);
		}

		// finally return node
		GraphObject result = results.get(0);
		if (result instanceof AbstractNode) {
				
			return (AbstractNode)result;
				
		} else {
		
			logger.log(Level.WARNING, "Node with UUID {0} is not of type AbstractNode!", uuid);
		}
		
		return null;
	}
}
