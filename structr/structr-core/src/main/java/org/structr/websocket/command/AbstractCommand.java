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

package org.structr.websocket.command;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import org.eclipse.jetty.websocket.WebSocket.Connection;
import org.structr.common.SecurityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.WebSocketMessage;

/**
 * Base class for all WebSocket messages in structr.
 *
 * @author Christian Morgner
 */
public abstract class AbstractCommand {

	private static final Logger logger                 = Logger.getLogger(AbstractCommand.class.getName());
	
	public static final String COMMAND_KEY             = "command";
	public static final String ID_KEY                  = "id";

	private StructrWebSocket parent = null;
	private Connection connection = null;
	private String idProperty = null;
	private String token = null;

	public abstract boolean processMessage(final WebSocketMessage webSocketData);
	public abstract String getCommand();

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	public String getIdProperty() {
		return idProperty;
	}

	public void setIdProperty(String idProperty) {
		this.idProperty = idProperty;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public StructrWebSocket getParent() {
		return parent;
	}

	public void setParent(StructrWebSocket parent) {
		this.parent = parent;
	}

	/**
	 * Returns the node to which the uuid parameter
	 * of this command refers to.
	 *
	 * @return the node
	 */
	public AbstractNode getNode(String id) {

		if(idProperty != null) {

			List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();
			attrs.add(Search.andExactProperty(idProperty, id));

			List<AbstractNode> results = (List<AbstractNode>)Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(null, false, false, attrs);
			if(!results.isEmpty()) {
				return results.get(0);
			}

		} else {

			List<AbstractNode> results = (List<AbstractNode>)Services.command(SecurityContext.getSuperUserInstance(), FindNodeCommand.class).execute(id);
			if(!results.isEmpty()) {
				return results.get(0);
			}

		}

		return null;
	}

	// ----- protected methods -----
	protected String getIdFromNode(AbstractNode node) {
		if(idProperty != null) {
			return node.getStringProperty(idProperty);
		} else {
			return node.getIdString();
		}
	}
}
