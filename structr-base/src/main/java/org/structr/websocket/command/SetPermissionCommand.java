/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.Permission;
import org.structr.common.Permissions;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.StaticValue;
import org.structr.core.Value;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Websocket command to grant or revoke a permission.
 */
public class SetPermissionCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(SetPermissionCommand.class.getName());

	private static final String RECURSIVE_KEY    = "recursive";
	private static final String PRINCIPAL_ID_KEY = "principalId";
	private static final String PERMISSIONS_KEY  = "permissions";
	private static final String ACTION_KEY       = "action";

	static {

		StructrWebSocket.addCommand(SetPermissionCommand.class);
	}

	private int sum   = 0;
	private int count = 0;

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		AbstractNode obj   = getNode(webSocketData.getId());
		boolean rec        = webSocketData.getNodeDataBooleanValue(RECURSIVE_KEY);
		String principalId = webSocketData.getNodeDataStringValue(PRINCIPAL_ID_KEY);
		String permissions = webSocketData.getNodeDataStringValue(PERMISSIONS_KEY);
		String action      = webSocketData.getNodeDataStringValue(ACTION_KEY);

		if (principalId == null) {

			logger.error("This command needs a principalId");
			getWebSocket().send(MessageBuilder.status().code(400).build(), true);
		}

		Principal principal = (Principal) getNode(principalId);
		if (principal == null) {

			logger.error("No principal found with id {}", new Object[]{principalId});
			getWebSocket().send(MessageBuilder.status().code(400).build(), true);
		}

		webSocketData.getNodeData().remove("recursive");

		if (obj != null) {

			final SecurityContext securityContext = getWebSocket().getSecurityContext();

			securityContext.setDoTransactionNotifications(false);
			securityContext.disablePreventDuplicateRelationships();

			final App app = StructrApp.getInstance(securityContext);
			try (final Tx nestedTx = app.tx()) {

				if (!((AbstractNode)obj).isGranted(Permission.accessControl, securityContext)) {

					logger.warn("No access control permission for {} on {}", new Object[]{getWebSocket().getCurrentUser().toString(), obj.toString()});
					getWebSocket().send(MessageBuilder.status().message("No access control permission").code(400).build(), true);
					nestedTx.success();

					return;

				}

				nestedTx.success();

			} catch (FrameworkException ex) {
				logger.warn("", ex);
			}

			try {

				final Value<Tx> value = new StaticValue<>(null);

				final Set<Permission> permissionSet = new HashSet();
				final String[] parts = permissions.split("[,]+");

				for (final String part : parts) {

					final String trimmedPart = part.trim();
					if (trimmedPart.length() > 0) {

						permissionSet.add(Permissions.valueOf(trimmedPart));
					}
				}


				setPermission(value, app, obj, principal, action, permissionSet, rec);

				// commit and close transaction
				final Tx tx = value.get(null);
				if (tx != null) {

					try {
						tx.success();
					} finally {
						tx.close();
					}

					value.set(null, null);
				}
				webSocketData.setResult(Arrays.asList(principal));

				// send only over local connection (no broadcast)
				getWebSocket().send(webSocketData, true);

			} catch (FrameworkException ex) {

				logger.error("Unable to set permissions: {}", ((FrameworkException) ex).toString());
				getWebSocket().send(MessageBuilder.status().code(ex.getStatus()).jsonErrorObject(ex.toJSON()).build(), true);
			}

		} else {

			logger.warn("Graph object with uuid {} not found.", webSocketData.getId());
			getWebSocket().send(MessageBuilder.status().code(404).build(), true);
		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	@Override
	public String getCommand() {
		return "SET_PERMISSION";
	}

	private void setPermission(final Value<Tx> transaction, final App app, final AbstractNode obj, final Principal principal, final String action, final Set<Permission> permissions, final boolean rec) throws FrameworkException {

		// create new transaction if not already present
		Tx tx = transaction.get(null);
		if (tx == null) {

			tx = app.tx();
			transaction.set(null, tx);
		}

		switch (action) {

			case "grant":
				obj.grant(permissions, principal);
				break;

			case "revoke":
				obj.revoke(permissions, principal);
				break;

			case "setAllowed":
				obj.setAllowed(permissions, principal);
				break;
		}

		sum++;

		// commit transaction after 100 nodes
		if (++count == 100) {

			logger.info("Committing transaction after {} objects..", sum);

			count = 0;

			// commit and close old transaction
			try {
				tx.success();
			} finally {
				tx.close();
			}

			// create new transaction, do not notify Ui
			tx = app.tx(true, true, false);
		}

		if (rec && obj instanceof LinkedTreeNode) {

			for (final Object t : ((LinkedTreeNode) obj).treeGetChildren()) {

				setPermission(transaction, app, (AbstractNode) t, principal, action, permissions, rec);
			}
		}
	}
}
