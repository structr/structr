/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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
package org.structr.core.node;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;

/**
 *
 * @author Christian Morgner
 */
public class GetNodeByIdCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(GetNodeByIdCommand.class.getName());
	
	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		if(parameters.length == 1 && parameters[0] instanceof String) {
		
			List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();
			String uuid = (String)parameters[0];

			attrs.add(Search.andExactUuid(uuid));

			List<AbstractNode> results = (List<AbstractNode>) Services.command(securityContext, SearchNodeCommand.class).execute(null, false, false, attrs);
			int size                   = results.size();

			switch (size) {

				case 0 :
					return null;

				case 1 :
					return results.get(0);

				default :
					logger.log(Level.WARNING, "Got more than one result for UUID {0}, this is very likely to be a UUID collision!", uuid);

					return results.get(0);

			}
		}

		throw new IllegalStateException("GetNodeByIdCommand takes exactly one String argument.");
	}
}
