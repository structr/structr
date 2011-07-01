package org.structr.common.renderer;

import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Link;

/**
 *
 * @author Christian Morgner
 */
public class LinkRenderer implements NodeRenderer<Link>
{
	@Override
	public void renderNode(StructrOutputStream out, Link currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		AbstractNode structrNode = currentNode.getStructrNode();
		structrNode.setTemplate(currentNode.getTemplate());

		// if this page is requested to be edited, render edit frame
		if(editNodeId != null && currentNode.getId() == editNodeId.longValue())
		{

			structrNode.renderNode(out, currentNode, editUrl, editNodeId);

		} else
		{
			if(currentNode.isVisible())
			{
				structrNode.renderNode(out, currentNode, editUrl, editNodeId);
			}
		}
	}

	@Override
	public String getContentType(Link node)
	{
		return("text/html");
	}
}
