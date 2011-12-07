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

import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.Services;
import org.structr.core.node.CreateNodeCommand;
import org.structr.websocket.StructrWebSocket;

/**
 *
 * @author Christian Morgner
 */
public class CreateCommand extends AbstractMessage {

	private static final Logger logger = Logger.getLogger(CreateCommand.class.getName());

	@Override
	public void processMessage() {

		// create node
		Services.command(SecurityContext.getSuperUserInstance(), CreateNodeCommand.class).execute(getParameters());

		// broadcast message
		StructrWebSocket.broadcast(getSource());
	}
}
