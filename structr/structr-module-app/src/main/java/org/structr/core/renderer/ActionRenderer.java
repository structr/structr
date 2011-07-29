package org.structr.core.renderer;

import java.util.List;
import org.neo4j.graphdb.Direction;
import org.structr.common.CurrentRequest;
import org.structr.common.RelType;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.app.ActionNode;

/**
 *
 * @author Christian Morgner
 */
public class ActionRenderer implements NodeRenderer<AbstractNode>
{
	@Override
	public void renderNode(StructrOutputStream out, AbstractNode currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		// do actions (iterate over children)
		List<AbstractNode> children = currentNode.getSortedDirectChildNodes();
		boolean executionSuccessful = true;
		for(AbstractNode n : children)
		{
			if(n instanceof ActionNode)
			{
				ActionNode actionNode = (ActionNode)n;
				actionNode.initialize();

				if(!actionNode.doAction(out, startNode, editUrl, editNodeId)) {
					
					executionSuccessful = false;
					break;
				}
			}
		}

		if(executionSuccessful)
		{
			// redirect to success page
			// saved session values can be reset!
			AbstractNode successTarget = getSuccessTarget(currentNode);
			if(successTarget != null)
			{
				CurrentRequest.redirect(currentNode, successTarget);
			}

		} else
		{
			// redirect to error page
			// saved session values must be kept
			AbstractNode failureTarget = getFailureTarget(currentNode);
			if(failureTarget != null)
			{
				CurrentRequest.redirect(currentNode, failureTarget);
			}
		}
	}

	@Override
	public String getContentType(AbstractNode node)
	{
		return("text/html");
	}

	// ----- private methods -----
	private AbstractNode getSuccessTarget(AbstractNode node)
	{
		List<StructrRelationship> rels = node.getRelationships(RelType.SUCCESS_DESTINATION, Direction.OUTGOING);
		AbstractNode ret = null;

		for(StructrRelationship rel : rels)
		{
			// first one wins
			ret = rel.getEndNode();
			break;
		}

		return (ret);
	}

	private AbstractNode getFailureTarget(AbstractNode node)
	{
		List<StructrRelationship> rels = node.getRelationships(RelType.ERROR_DESTINATION, Direction.OUTGOING);
		AbstractNode ret = null;

		for(StructrRelationship rel : rels)
		{
			// first one wins
			ret = rel.getEndNode();
			break;
		}

		return (ret);
	}
}
