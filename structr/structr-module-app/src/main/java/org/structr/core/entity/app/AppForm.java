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

import java.util.List;
import org.neo4j.graphdb.Direction;
import org.structr.common.RelType;
import org.structr.common.CurrentRequest;
import org.structr.common.StructrOutputStream;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;

/**
 *
 *
 * @author Christian Morgner
 */
public class AppForm extends HtmlNode
{
	private static final String ICON_SRC = "/images/form.png";

	public AppForm()
	{
		super("form");
	}

	@Override
	public String getIconSrc()
	{
		return (ICON_SRC);
	}


	@Override
	public void doBeforeRendering(final StructrOutputStream out, final AbstractNode startNode, final String editUrl, final Long editNodeId)
	{
		AppActionContainer submit = findSubmit();
		if(submit != null)
		{
			addAttribute("action", CurrentRequest.getAbsoluteNodePath(submit));
		}

		addAttribute("method", "post");
	}

	@Override
	public void renderContent(final StructrOutputStream out, final AbstractNode startNode, final String editUrl, final Long editNodeId)
	{
		for(AbstractNode node : getSortedDirectChildNodes())
		{
			node.renderNode(out, startNode, editUrl, editNodeId);
		}
	}

	@Override
	public boolean hasContent(final StructrOutputStream out, final AbstractNode startNode, final String editUrl, final Long editNodeId)
	{
		return(hasChildren());
	}

	// ----- private methods -----
	private AppActionContainer findSubmit()
	{
		AppActionContainer ret = null;

		List<StructrRelationship> rels = getRelationships(RelType.SUBMIT, Direction.OUTGOING);
		if(rels != null && rels.size() > 0)
		{
			StructrRelationship rel = rels.get(0);
			AbstractNode node = rel.getEndNode();

			if(node != null && node instanceof AppActionContainer)
			{
				ret = (AppActionContainer)node;
			}
		}

		// not found, try children
		if(ret == null)
		{
			// try direct children
			List<AbstractNode> children = getDirectChildNodes();
			for(AbstractNode child : children)
			{
				if(child instanceof AppActionContainer)
				{
					ret = (AppActionContainer)child;
					break;
				}

			}
		}

		return(ret);
	}
}
