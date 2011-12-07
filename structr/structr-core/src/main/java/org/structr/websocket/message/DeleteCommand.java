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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.DeleteNodeCommand;
import org.structr.websocket.StructrWebSocket;

/**
 *
 * @author Christian Morgner
 */
public class DeleteCommand extends AbstractMessage {

	private static final Logger logger = Logger.getLogger(DeleteCommand.class.getName());

	@Override
	public void processMessage() {

		AbstractNode node = getNode();
		if(node != null) {

			Services.command(SecurityContext.getSuperUserInstance(), DeleteNodeCommand.class).execute(node);

			// broadcast message
			StructrWebSocket.broadcast(getSource());

		} else {

			logger.log(Level.WARNING, "Node with uuid {0} not found.", getUuid());

		}
	}
}
