/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.PropertyKey;
import org.structr.websocket.message.WebSocketMessage;
import org.structr.common.PagingHelper;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;

//~--- JDK imports ------------------------------------------------------------
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.ShadowDocument;
import org.structr.web.entity.dom.Template;
import org.structr.web.entity.dom.relationship.DOMChildren;

//~--- classes ----------------------------------------------------------------
/**
 * Websocket command to retrieve DOM nodes which are not attached to a parent
 * element
 *
 *
 */
public class ListUnattachedNodesCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(ListUnattachedNodesCommand.class.getName());

	static {

		StructrWebSocket.addCommand(ListUnattachedNodesCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final String sortOrder = webSocketData.getSortOrder();
		final String sortKey = webSocketData.getSortKey();
		final int pageSize = webSocketData.getPageSize();
		final int page = webSocketData.getPage();
		final PropertyKey sortProperty = StructrApp.getConfiguration().getPropertyKeyForJSONName(DOMNode.class, sortKey);
		final Query query = StructrApp.getInstance(securityContext).nodeQuery().includeDeletedAndHidden().sort(sortProperty).order("desc".equals(sortOrder));

		query.orTypes(DOMElement.class);
		query.orType(Content.class);
		query.orType(Template.class);

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			// do search
			List<AbstractNode> filteredResults = getUnattachedNodes(app, securityContext, webSocketData);

			// save raw result count
			int resultCountBeforePaging = filteredResults.size();

			// set full result list
			webSocketData.setResult(PagingHelper.subList(filteredResults, pageSize, page, null));
			webSocketData.setRawResultCount(resultCountBeforePaging);

			// send only over local connection
			getWebSocket().send(webSocketData, true);

			tx.success();

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Exception occured", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);

		}

	}

	/**
	 * Return list of nodes which are not attached to a page and have no
	 * parent element (no incoming CONTAINS rel)
	 *
	 * @param app
	 * @param securityContext
	 * @param webSocketData
	 * @return
	 * @throws FrameworkException
	 */
	protected static List<AbstractNode> getUnattachedNodes(final App app, final SecurityContext securityContext, final WebSocketMessage webSocketData) throws FrameworkException {

		final String sortOrder = webSocketData.getSortOrder();
		final String sortKey = webSocketData.getSortKey();
		final PropertyKey sortProperty = StructrApp.getConfiguration().getPropertyKeyForJSONName(DOMNode.class, sortKey);
		final Query query = StructrApp.getInstance(securityContext).nodeQuery().includeDeletedAndHidden().sort(sortProperty).order("desc".equals(sortOrder));

		query.orTypes(DOMElement.class);
		query.orType(Content.class);
		query.orType(Template.class);

		// do search
		List<AbstractNode> filteredResults = new LinkedList();
		List<? extends GraphObject> resultList = null;
		
		try (final Tx tx = app.tx()) {
			
			resultList = query.getAsList();
			
		} catch (FrameworkException fex) {
			logger.log(Level.WARNING, "Exception occured", fex);
		}

		// determine which of the nodes have no incoming CONTAINS relationships and no page id
		for (GraphObject obj : resultList) {

			if (obj instanceof AbstractNode) {

				AbstractNode node = (AbstractNode) obj;

				if (!node.hasIncomingRelationships(DOMChildren.class) && node.getProperty(DOMNode.ownerDocument) == null && !(node instanceof ShadowDocument)) {

					filteredResults.add(node);
				}

			}

		}

		return filteredResults;

	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {

		return "LIST_UNATTACHED_NODES";

	}

}
