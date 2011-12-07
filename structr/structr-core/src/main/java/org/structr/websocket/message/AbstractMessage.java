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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.websocket.WebSocket.Connection;
import org.structr.common.SecurityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;

/**
 * Base class for all WebSocket messages in structr.
 *
 * @author Christian Morgner
 */
public abstract class AbstractMessage implements Message {

	public static final String COMMAND_KEY             = "command";
	public static final String UUID_KEY                = "uuid";

	private static final Map<String, Class> commandSet = new LinkedHashMap<String, Class>();
	private static final Logger logger                 = Logger.getLogger(AbstractMessage.class.getName());
	private static final Gson gson;

	static {

		// initialize command set
		addCommand(CreateCommand.class);
		addCommand(UpdateCommand.class);
		addCommand(DeleteCommand.class);

		// initialize JSON parser
		gson = new GsonBuilder().setPrettyPrinting().create();
	}

	private Map<String, String> parameters = new LinkedHashMap<String, String>();
	private Connection connection = null;
	private String source = null;
	private String uuid = null;

	public abstract Map<String, String> processMessage();
	public abstract String getCommand();

	@Override
	public String getUuid() {
		return uuid;
	}

	public String getSource() {
		return source;
	}

	@Override
	public Map<String, String> getParameters() {
		return parameters;
	}

	public Connection getConnection() {
		return connection;
	}

	// ----- protected methods -----
	/**
	 * Returns the node to which the uuid parameter
	 * of this command refers to.
	 *
	 * @return the node
	 */
	protected AbstractNode getNode() {

		List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();
		attrs.add(Search.andExactUuid(uuid));

		List<AbstractNode> results = (List<AbstractNode>)Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(null, false, false, attrs);
		if(!results.isEmpty()) {
			return results.get(0);
		}

		return null;
	}

	// ----- private methods -----
	private void setConnection(Connection connection) {
		this.connection = connection;
	}

	private void setSource(String source) {
		this.source = source;
	}

	private void setUuid(String uuid) {
		this.uuid = uuid;
	}

	private void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	// ----- public static methods -----
	public static AbstractMessage createMessage(Connection connection, String source) {

		try {
			Map<String, String> parsedSource = gson.fromJson(source, new TypeToken<Map<String, String>>() {}.getType());
			String command = parsedSource.get(COMMAND_KEY);
			String uuid = parsedSource.get(UUID_KEY);

			// prepare parsed source map to be used for setProperty (remove meta-information)
			parsedSource.remove(COMMAND_KEY);
			parsedSource.remove(UUID_KEY);

			Class type = commandSet.get(command);
			if(type != null) {

				AbstractMessage msg = (AbstractMessage)type.newInstance();
				msg.setParameters(parsedSource);
				msg.setConnection(connection);
				msg.setSource(source);
				msg.setUuid(uuid);

				return msg;

			} else {
				logger.log(Level.WARNING, "Unknow command {0}", command);
			}



		} catch(Throwable t) {
			logger.log(Level.WARNING, "Unable to parse message.", t);
		}

		return null;
	}

	// ----- private static methods -----
	private static final void addCommand(Class command) {

		try {
			AbstractMessage msg = (AbstractMessage)command.newInstance();
			commandSet.put(msg.getCommand(), command);

		} catch(Throwable t) {
			
			logger.log(Level.SEVERE, "Unable to add command {0}", command.getName());
		}
	}
}
