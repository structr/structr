/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.websocket;


import org.structr.common.SecurityContext;
import org.structr.websocket.message.WebSocketMessage;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;

import static org.mockito.Mockito.mock;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.event.PropertyEntry;
import org.neo4j.graphdb.event.TransactionData;

import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.TransactionNotifier;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchAttributeGroup;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.graph.search.SearchOperator;
import org.structr.rest.ResourceProvider;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;


/**
 *
 * @author Axel Morgner
 */
public class PartialUpdateController implements TransactionNotifier {

	private static final Logger logger                              = Logger.getLogger(PartialUpdateController.class.getName());
	private List<WebSocketMessage> partials                         = new LinkedList();
	private SecurityContext securityContext                         = SecurityContext.getSuperUserInstance();
	private SynchronizationController syncController                = null;
	private ResourceProvider resourceProvider                       = null;

	public PartialUpdateController(SynchronizationController syncController) {
		this.syncController = syncController;
	}

	public List<WebSocketMessage> getPartials() {
		return partials;
	}
	
	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	public void setResourceProvider(final ResourceProvider resourceProvider) {
		this.resourceProvider = resourceProvider;
	}
	
	protected void sendPartial(SecurityContext securityContext, String type) {
		
		List<DOMElement> dynamicElements = null;
		List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();

		// Find all DOMElements which render data of the type of the obj
		attrs.add(Search.andExactTypeAndSubtypes(DOMElement.class.getSimpleName()));
		SearchAttributeGroup g = new SearchAttributeGroup(SearchOperator.AND);
		g.add(Search.orExactProperty(DOMElement.dataKey, EntityContext.denormalizeEntityName(type)));
		g.add(Search.orExactProperty(DOMElement.partialUpdateKey, EntityContext.denormalizeEntityName(type)));
		attrs.add(g);

		try {
			Result results = Services.command(securityContext, SearchNodeCommand.class).execute(attrs);
			
			dynamicElements = results.getResults();
			
		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, "Something went wrong while searching for dynamic elements of type " + type, ex);
		}
		
		HttpServletRequest request = mock(HttpServletRequest.class);
		RenderContext ctx = new RenderContext(request, null, false, Locale.GERMAN);
		ctx.setResourceProvider(resourceProvider);
		
		for (DOMElement el : dynamicElements) {
			
			logger.log(Level.FINE, "Found dynamic element for type {0}: {1}", new Object[]{type, el});
			
			try {
				
				Page page = el.getProperty(DOMNode.ownerDocument);
				
				// render only when contained in a page
				if (page != null) {
					
					DOMElement parent = (DOMElement) el.getParentNode();
				
					if (parent != null) {
						
						parent.render(securityContext, ctx, 0);
				
						String partialContent = ctx.getBuffer().toString();

						logger.log(Level.FINE, "Partial output:\n{0}", partialContent);

						WebSocketMessage message = new WebSocketMessage();

						message.setCommand("PARTIAL");

						message.setNodeData("pageId", page.getUuid());

						String pageName = page.getName();

						message.setNodeData("pagePath", "/" + pageName);
						message.setMessage(StringUtils.remove(partialContent, "\n"));
						message.setNodeData("parentPositionPath", parent.getPositionPath());

						syncController.broadcast(message);
					}
					
				}
				
				
			} catch (FrameworkException ex) {
				logger.log(Level.SEVERE, null, ex);
			}
			
			
		}
		
		
		
	}
	
	// ----- interface TransactionNotifier -----

	/**
	 * When notified after a transaction is committed, re-render a partial for
	 * each created node.
	 * 
	 * Deleted nodes are handled in the {@link SynchronizationController}.
	 * 
	 * @param data 
	 */
	@Override
	public void notify(SecurityContext securityContext, TransactionData data) {

		// Collect all nodes to be taken into account
		Set<Node> nodes = new HashSet();
		
		for (Node node : data.createdNodes()) {
			nodes.add(node);
		}
		
		for (PropertyEntry<Node> prop : data.assignedNodeProperties()) {
			nodes.add(prop.entity());
		}

		for (PropertyEntry<Node> prop : data.removedNodeProperties()) {
			nodes.add(prop.entity());
		}

		for (Node node : nodes) {
			String type = (String) node.getProperty("type");
			sendPartial(securityContext, type);
		}
	}


}
