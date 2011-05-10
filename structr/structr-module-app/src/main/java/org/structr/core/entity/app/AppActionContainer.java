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
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.structr.common.RelType;
import org.structr.common.CurrentRequest;

import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.User;

/**
 *
 * @author Christian Morgner
 */
public class AppActionContainer extends AbstractNode
{
	private static final Logger logger = Logger.getLogger(AppActionContainer.class.getName());

	@Override
	public String getIconSrc()
	{
		return("/images/brick_go.png");
	}

	// ----- public methods -----
	@Override
	public void renderView(StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user)
	{
		// only execute if path matches exactly
		String currentUrl = CurrentRequest.getCurrentNodePath();
		String myNodeUrl = getNodePath(user);

		if(currentUrl != null)
		{
			// remove slashes from end of string
			while(currentUrl.endsWith("/"))
			{
				currentUrl = currentUrl.substring(0, currentUrl.length() - 1);
			}

			// only execute method if path matches exactly
			if(!myNodeUrl.equals(currentUrl))
			{
				return;
			}

		} else
		{
			return;
		}

		// do actions (iterate over children)
		List<AbstractNode> children = getSortedDirectChildNodes(user);
		boolean executionSuccessful = true;
		for(AbstractNode node : children)
		{
			if(node instanceof ActionNode)
			{
				ActionNode actionNode = (ActionNode)node;
				actionNode.initialize();

				executionSuccessful &= actionNode.doAction(out, startNode, editUrl, editNodeId, user);
			}
		}

		if(executionSuccessful)
		{
			// redirect to success page
			// saved session values can be reset!
			AbstractNode successTarget = getSuccessTarget();
			if(successTarget != null)
			{
				CurrentRequest.redirect(user, successTarget);
			}

		} else
		{
			// redirect to error page
			// saved session values must be kept
			AbstractNode failureTarget = getFailureTarget();
			if(failureTarget != null)
			{
				CurrentRequest.redirect(user, failureTarget);
			}
		}
	}

	// ----- private methods -----
	private AbstractNode getSuccessTarget()
	{
		List<StructrRelationship> rels = getRelationships(RelType.SUCCESS_DESTINATION, Direction.OUTGOING);
		AbstractNode ret = null;

		for(StructrRelationship rel : rels)
		{
			// first one wins
			ret = rel.getEndNode();
			break;
		}

		return (ret);
	}

	private AbstractNode getFailureTarget()
	{
		List<StructrRelationship> rels = getRelationships(RelType.ERROR_DESTINATION, Direction.OUTGOING);
		AbstractNode ret = null;

		for(StructrRelationship rel : rels)
		{
			// first one wins
			ret = rel.getEndNode();
			break;
		}

		return (ret);
	}

	@Override
	public void onNodeCreation()
	{
	}

	@Override
	public void onNodeInstantiation()
	{
	}
}
