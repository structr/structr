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
import org.apache.commons.codec.digest.DigestUtils;
import org.structr.common.SecurityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.WebSocketMessage;

/**
 *
 * @author Christian Morgner
 */
public class LoginCommand extends AbstractCommand {

	@Override
	public boolean processMessage(WebSocketMessage webSocketData) {

		String username = webSocketData.getData().get("username");
		String password = webSocketData.getData().get("password");

		if(username != null && password != null) {

			List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();
			attrs.add(Search.andExactProperty(AbstractNode.Key.name.name(), username));
			attrs.add(Search.andExactType("User"));

			// we need to search with a super user security context here..
			List<AbstractNode> results = (List<AbstractNode>)Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(
			    null, false, false, attrs);

			if(!results.isEmpty()) {

				int resultCount = results.size();
				if(resultCount == 1) {

					User user = (User)results.get(0);

					// check password
					if(DigestUtils.sha512Hex(password).equals(user.getProperty(User.Key.password))) {

						String token = StructrWebSocket.secureRandomString();

						// store token in user
						user.setProperty(User.Key.sessionId, token);

						// store token in response data
						webSocketData.getData().clear();
						webSocketData.setToken(token);

						// authenticate socket
						this.getWebSocket().setAuthenticated(token);

						// send data..
						this.getWebSocket().send(getConnection(), webSocketData, false);
					}
				}
			}

		}
		// do NOT broadcast
		return false;
	}

	@Override
	public String getCommand() {
		return "LOGIN";
	}
}
