/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import java.util.List;
import org.neo4j.graphdb.Direction;
import org.structr.common.RelType;
import org.structr.common.CurrentRequest;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.User;

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
	public void doBeforeRendering(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		AppActionContainer submit = findSubmit(user);
		if(submit != null)
		{
			addAttribute("action", CurrentRequest.getAbsoluteNodePath(user, submit));
		}

		addAttribute("method", "post");
	}

	@Override
	public void renderContent(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		for(AbstractNode node : getSortedDirectChildNodes(user))
		{
			node.renderView(out, startNode, editUrl, editNodeId, user);
		}
	}

	@Override
	public boolean hasContent(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		return(hasChildren());
	}

	// ----- private methods -----
	private AppActionContainer findSubmit(User user)
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
			List<AbstractNode> children = getDirectChildNodes(user);
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
