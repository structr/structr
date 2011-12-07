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

import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public class UpdateCommand extends AbstractMessage {

	private static final Logger logger = Logger.getLogger(UpdateCommand.class.getName());

	@Override
	public Map<String, String> processMessage() {

		AbstractNode node = getNode();
		if(node != null) {

			for(Entry<String, String> entry : getParameters().entrySet()) {
				node.setProperty(entry.getKey(), entry.getValue());
			}

			// add uuid to parameter set
			getParameters().put(AbstractMessage.ID_KEY, node.getStringProperty(getIdProperty()));

			return getParameters();

		} else {

			logger.log(Level.WARNING, "Node with uuid {0} not found.", getUuid());

		}

		return null;
	}

	@Override
	public String getCommand() {
		return "UPDATE";
	}
}
