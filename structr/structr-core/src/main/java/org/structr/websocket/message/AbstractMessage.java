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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.websocket.WebSocket.Connection;

/**
 * Base class for all WebSocket messages in structr.
 *
 * @author Christian Morgner
 */
public abstract class AbstractMessage implements Message {

	private static final Map<String, Class> commandSet = new LinkedHashMap<String, Class>();
	private static final Logger logger                 = Logger.getLogger(AbstractMessage.class.getName());
	private static final String COMMAND_KEY            = "command";
	private static final String UUID_KEY               = "uuid";
	private static final Gson gson;

	static {

		// initialize command set
		commandSet.put("UPDATE", UpdateCommand.class);

		// initialize JSON parser
		gson = new GsonBuilder().setPrettyPrinting().create();
	}

	private Map<String, String> parameters = null;
	private Connection connection = null;
	private String command = null;
	private String uuid = null;

	public abstract void processMessage();

	@Override
	public String getUuid() {
		return uuid;
	}

	@Override
	public Map<String, String> getParameters() {
		return parameters;
	}

	public Connection getConnection() {
		return connection;
	}

	// ----- private methods -----
	private void setConnection(Connection connection) {
		this.connection = connection;
	}

	private void setUuid(String uuid) {
		this.uuid = uuid;
	}

	// ----- public static methods -----
	public static AbstractMessage createMessage(Connection connection, String source) {

		try {
			Map<String, String> map = gson.fromJson(source, new TypeToken<Map<String, Map<String, String>>>() {}.getType());
			String command = map.get(COMMAND_KEY);
			String uuid = map.get(UUID_KEY);

			Class type = commandSet.get(command);
			if(type != null) {

				AbstractMessage msg = (AbstractMessage)type.newInstance();
				msg.setConnection(connection);
				msg.setUuid(uuid);

				// TODO: initialize?
				return msg;

			} else {
				logger.log(Level.WARNING, "Unknow command {0}", command);
			}



		} catch(Throwable t) {
			logger.log(Level.WARNING, "Unable to parse message.", t);
		}

		return null;
	}
}
