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



package org.structr.websocket.command;

import org.eclipse.jetty.websocket.WebSocket.Connection;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.FindRelationshipCommand;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.core.node.search.SearchRelationshipCommand;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.property.PropertyKey;
import org.structr.core.Result;

//~--- classes ----------------------------------------------------------------

/**
 * Base class for all WebSocket messages in structr.
 *
 * @author Christian Morgner
 */
public abstract class AbstractCommand {

	public static final String COMMAND_KEY = "command";
	public static final String ID_KEY      = "id";
	private static final Logger logger     = Logger.getLogger(AbstractCommand.class.getName());

	//~--- fields ---------------------------------------------------------

	private Connection connection          = null;
	private PropertyKey<String> idProperty = null;
	private StructrWebSocket webSocket     = null;

	//~--- methods --------------------------------------------------------

	public abstract void processMessage(final WebSocketMessage webSocketData);

	//~--- get methods ----------------------------------------------------

	public abstract String getCommand();

	public Connection getConnection() {
		return connection;
	}

	public PropertyKey<String> getIdProperty() {
		return idProperty;
	}

	public StructrWebSocket getWebSocket() {
		return webSocket;
	}

	/**
	 * Returns the node to which the uuid parameter
	 * of this command refers to.
	 *
	 * @return the node
	 */
	public AbstractNode getNode(final String id) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		try {

			if (idProperty != null) {

				List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();

				attrs.add(Search.andExactProperty(idProperty, id));

				Result results = Services.command(securityContext, SearchNodeCommand.class).execute(true, false, attrs);

				if (!results.isEmpty()) {

					return (AbstractNode) results.get(0);

				}

			} else {

				List<AbstractNode> results = (List<AbstractNode>) Services.command(securityContext, FindNodeCommand.class).execute(id);

				if (!results.isEmpty()) {

					return results.get(0);

				}

			}

		} catch (FrameworkException fex) {
			logger.log(Level.WARNING, "Unable to get node", fex);
		}

		return null;
	}

	/**
	 * Returns the relationship to which the uuid parameter
	 * of this command refers to.
	 *
	 * @return the node
	 */
	public AbstractRelationship getRelationship(final String id) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		try {

			if (idProperty != null) {

				List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();

				attrs.add(Search.andExactProperty(idProperty, id));

				List<AbstractRelationship> results = (List<AbstractRelationship>) Services.command(securityContext,
									     SearchRelationshipCommand.class).execute(attrs);

				if (!results.isEmpty()) {

					return results.get(0);

				}

			} else {

				List<AbstractRelationship> results = (List<AbstractRelationship>) Services.command(securityContext,
									     FindRelationshipCommand.class).execute(id);

				if (!results.isEmpty()) {

					return results.get(0);

				}

			}

		} catch (FrameworkException fex) {
			logger.log(Level.WARNING, "Unable to get relationship", fex);
		}

		return null;
	}

	// ----- protected methods -----
	protected String getIdFromNode(final AbstractNode node) {

		if (idProperty != null) {

			return node.getProperty(idProperty);

		} else {

			return node.getIdString();

		}
	}

	//~--- set methods ----------------------------------------------------

	public void setConnection(final Connection connection) {
		this.connection = connection;
	}

	public void setIdProperty(final PropertyKey<String> idProperty) {
		this.idProperty = idProperty;
	}

	public void setWebSocket(final StructrWebSocket webSocket) {
		this.webSocket = webSocket;
	}
}
