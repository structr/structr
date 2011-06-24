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
package org.structr.core.node;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Christian Morgner
 */
public class NodeConsoleCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(NodeConsoleCommand.class.getName());
	public static final Map<String, Class<? extends NodeServiceCommand>> commandMap;

	// serves as a static constructor
	static {
		commandMap = Collections.synchronizedMap(new LinkedHashMap<String, Class<? extends NodeServiceCommand>>());

		commandMap.put("create", CreateNodeCommand.class);
		commandMap.put("help", HelpCommand.class);
		commandMap.put("?", HelpCommand.class);
	}

	@Override
	public Object execute(Object... parameters) {

		StringBuilder ret = new StringBuilder(200);

		if(parameters.length > 0) {

			if(parameters[0] instanceof String) {

				String commandLine = (String)parameters[0];

				// split command line on whitespace characters
				String[] commands = commandLine.split("[\\s]+");
				if(commands.length > 0) {

					Class<? extends NodeServiceCommand> commandClass = commandMap.get(commands[0].toString());
					if(commandClass != null) {

						try
						{
							NodeServiceCommand command = commandClass.newInstance();
							Object[] params = new Object[parameters.length - 1];
							for(int i=1; i<parameters.length; i++) params[i-1] = parameters[i];

							ret.append(command.execute(params));

						} catch(Throwable t)
						{
							logger.log(Level.WARNING, "Error instantiating command {0}: {1}", new Object[] { commands[0].toString(), t.getMessage() } );
						}

					} else {

						ret.append("Command not found");
					}
				}

			}
		}


		return (ret.toString());
	}
}
