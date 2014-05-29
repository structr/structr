/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.Permission;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.entity.Principal;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------
/**
 * Websocket command to grant or revoke a permission
 *
 * @author Axel Morgner
 */
public class SetPermissionCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(SetPermissionCommand.class.getName());

	static {

		StructrWebSocket.addCommand(SetPermissionCommand.class);

	}

	//~--- methods --------------------------------------------------------
	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		AbstractNode obj = getNode(webSocketData.getId());
		boolean rec = (Boolean) webSocketData.getNodeData().get("recursive");
		String principalId = (String) webSocketData.getNodeData().get("principalId");
		String permission = (String) webSocketData.getNodeData().get("permission");
		String action = (String) webSocketData.getNodeData().get("action");

		if (principalId == null) {

			logger.log(Level.SEVERE, "This command needs a principalId");
			getWebSocket().send(MessageBuilder.status().code(400).build(), true);

		}

		Principal principal = (Principal) getNode(principalId);

		if (principal == null) {

			logger.log(Level.SEVERE, "No principal found with id {0}", new Object[]{principalId});
			getWebSocket().send(MessageBuilder.status().code(400).build(), true);

		}

		webSocketData.getNodeData().remove("recursive");

		if (obj != null) {

			if (!getWebSocket().getSecurityContext().isAllowed(((AbstractNode) obj), Permission.accessControl)) {

				logger.log(Level.WARNING, "No access control permission for {0} on {1}", new Object[]{getWebSocket().getCurrentUser().toString(), obj.toString()});
				getWebSocket().send(MessageBuilder.status().message("No access control permission").code(400).build(), true);
				return;

			}

			try {
				setPermission(obj, principal, action, permission, rec);
				webSocketData.setResult(Arrays.asList(principal));

				// send only over local connection (no broadcast)
				getWebSocket().send(webSocketData, true);

			} catch (FrameworkException ex) {

				logger.log(Level.SEVERE, "Unable to set permissions: {0}", ((FrameworkException) ex).toString());
				getWebSocket().send(MessageBuilder.status().code(400).build(), true);

			}

		} else {

			logger.log(Level.WARNING, "Graph object with uuid {0} not found.", webSocketData.getId());
			getWebSocket().send(MessageBuilder.status().code(404).build(), true);

		}

	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {

		return "SET_PERMISSION";

	}

	//~--- set methods ----------------------------------------------------
	private void setPermission(final AbstractNode obj, final Principal principal, final String action, final String permission, final boolean rec) throws FrameworkException {

		switch (action) {
			case "grant":
				principal.grant(Permission.valueOf(permission), obj);
				break;
			case "revoke":
				principal.revoke(Permission.valueOf(permission), obj);
				break;
		}

		if (rec) {
			if (obj instanceof LinkedTreeNode) {
				LinkedTreeNode ltn = (LinkedTreeNode) obj;

				for (Object t : ltn.treeGetChildren()) {

					setPermission((AbstractNode) t, principal, action, permission, rec);

				}
			}
		}

	}

}
