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

import java.util.List;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.PropertyKey;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.schema.SchemaHelper;
import org.structr.web.datasource.RestDataSource;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;

//~--- classes ----------------------------------------------------------------
/**
 *
 * @author Axel Morgner
 */
public class SearchCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(SearchCommand.class.getName());

	static {

		StructrWebSocket.addCommand(SearchCommand.class);

	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final String searchString = (String) webSocketData.getNodeData().get("searchString");
		final String restQuery = (String) webSocketData.getNodeData().get("restQuery");
		final String cypherQuery = (String) webSocketData.getNodeData().get("cypherQuery");
		final String typeString = (String) webSocketData.getNodeData().get("type");

		Class type = null;
		if (typeString != null) {
			type = SchemaHelper.getEntityClassForRawType(typeString);
		}

		if (searchString == null) {

			if (cypherQuery != null) {

				try {
					final List<GraphObject> result = StructrApp.getInstance(securityContext).cypher(cypherQuery, null);

					webSocketData.setResult(result);
					getWebSocket().send(webSocketData, true);

					return;

				} catch (Exception ex) {

					logger.log(Level.WARNING, "Exception occured", ex);
					getWebSocket().send(MessageBuilder.status().code(400).message(ex.getMessage()).build(), true);

				}

			}

			if (restQuery != null) {

				final RestDataSource restDataSource = new RestDataSource();
				try {
					securityContext.setRequest(getWebSocket().getRequest());
					
					webSocketData.setResult(restDataSource.getData(securityContext, null, restQuery));
					getWebSocket().send(webSocketData, true);
					
					return;

				} catch (FrameworkException ex) {
					Logger.getLogger(SearchCommand.class.getName()).log(Level.SEVERE, null, ex);
				}

			}

		}

		final String sortOrder = webSocketData.getSortOrder();
		final String sortKey = webSocketData.getSortKey();
		final PropertyKey sortProperty = StructrApp.getConfiguration().getPropertyKeyForJSONName(AbstractNode.class, sortKey);
		final Query query = StructrApp.getInstance(securityContext).nodeQuery().includeDeletedAndHidden().sort(sortProperty).order("desc".equals(sortOrder));

		query.andName(searchString);

		if (type != null) {
			query.andType(type);
		}

		try {

			// do search
			final Result result = query.getResult();

			// set full result list
			webSocketData.setResult(result.getResults());

			// send only over local connection
			getWebSocket().send(webSocketData, true);

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Exception occured", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);

		}

	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {

		return "SEARCH";

	}

}
