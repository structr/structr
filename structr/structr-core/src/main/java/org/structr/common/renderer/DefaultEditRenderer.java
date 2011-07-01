package org.structr.common.renderer;

import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public class DefaultEditRenderer implements NodeRenderer<AbstractNode>
{

	@Override
	public void renderNode(StructrOutputStream out, AbstractNode currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		// if this page is requested to be edited, render edit frame
		if(editNodeId != null && currentNode.getId() == editNodeId.longValue())
		{
			currentNode.renderEditFrame(out, editUrl);
		}
	}

	@Override
	public String getContentType(AbstractNode currentNode)
	{
		return("text/html");
	}
}
