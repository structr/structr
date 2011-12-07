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

package org.structr.websocket.message;

import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.websocket.StructrWebSocket;

/**
 *
 * @author Christian Morgner
 */
public class UpdateCommand extends AbstractMessage {

	private static final Logger logger = Logger.getLogger(UpdateCommand.class.getName());

	@Override
	public void processMessage() {

		String uuid = getUuid();

		List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();
		attrs.add(Search.andExactUuid(uuid));

		List<AbstractNode> results = (List<AbstractNode>)Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(null, false, false, attrs);
		if(!results.isEmpty()) {

			logger.log(Level.INFO, "Updating node with uuid {0}", uuid);

			AbstractNode node = results.get(0);

			for(Entry<String, String> entry : getParameters().entrySet()) {
				node.setProperty(entry.getKey(), entry.getValue());
			}

			// broadcast message
			StructrWebSocket.broadcast(getSource());

		} else {

			logger.log(Level.WARNING, "Node with uuid {0} not found.", uuid);

		}
	}
}
