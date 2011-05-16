/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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

package org.structr.core.entity.app;

import java.io.StringWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.CurrentRequest;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.node.FindNodeCommand;

/**
 * AppNodeView loads the node with the ID found in the request parameter specified
 * by the ID_SOURCE_KEY property of this node.
 *
 * @author Christian Morgner
 */
public class AppNodeView extends AbstractNode
{
	private static final Logger logger = Logger.getLogger(AppNodeView.class.getName());

	private static final String FOLLOW_RELATIONSHIP_KEY =	"followRelationship";
	private static final String ID_SOURCE_KEY =		"idSource";

	@Override
	public String getIconSrc()
	{
		return("/images/magnifier.png");
	}

	@Override
	public void renderView(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		AbstractNode sourceNode = loadNode();
		if(sourceNode != null)
		{
			doRendering(out, this, sourceNode, editUrl, editNodeId, user);

		} else
		{
			logger.log(Level.WARNING, "sourceNode was null");
		}
	}

	@Override
	public void onNodeCreation()
	{
	}

	@Override
	public void onNodeInstantiation()
	{
	}

	// ----- protected methods -----
	protected void doRendering(StringBuilder out, AbstractNode viewNode, AbstractNode dataNode, String editUrl, Long editNodeId, User user)
	{
		String templateSource = getTemplateFromNode(viewNode, user);
		StringWriter content = new StringWriter(100);

		AbstractNode.staticReplaceByFreeMarker(templateSource, content, dataNode, editUrl, editNodeId, user);
		out.append(content.toString());

		List<AbstractNode> viewChildren = viewNode.getSortedDirectChildNodes(user);
		for(AbstractNode viewChild : viewChildren)
		{
			// 1. get desired display relationship from view node
			// 2. find requested child nodes from dataNode
			// 3. render nodes with doRendering (recursion)

			String followRel = viewChild.getStringProperty(FOLLOW_RELATIONSHIP_KEY);
			if(followRel != null)
			{
				RelationshipType relType = DynamicRelationshipType.withName(followRel);
				List<AbstractNode> dataChildren = dataNode.getDirectChildren(relType, user);

				for(AbstractNode dataChild : dataChildren)
				{
					doRendering(out, viewChild, dataChild, editUrl, editNodeId, user);
				}
			}

		}
	}

	// ----- private methods -----
	private AbstractNode loadNode()
	{
		String idSourceParameter = (String)getProperty(ID_SOURCE_KEY);
		String idSource = CurrentRequest.getRequest().getParameter(idSourceParameter);

		return((AbstractNode)Services.command(FindNodeCommand.class).execute(null, this, idSource));
	}
	
	private String getTemplateFromNode(AbstractNode node, User user)
	{
		String ret = "";
		
		if(node.hasTemplate(user))
		{
			ret = node.getTemplate(user).getContent();
		}
		
		return(ret);
	}
}
