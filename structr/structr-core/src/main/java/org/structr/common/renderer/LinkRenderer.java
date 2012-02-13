package org.structr.common.renderer;

import org.structr.common.RenderMode;
import org.structr.common.SecurityContext;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.NodeLink;

/**
 *
 * @author Christian Morgner
 */
public class LinkRenderer implements NodeRenderer<NodeLink>
{
	@Override
	public void renderNode(StructrOutputStream out, NodeLink currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		AbstractNode structrNode = currentNode.getStructrNode();
		structrNode.setTemplate(currentNode.getTemplate());

		// if this page is requested to be edited, render edit frame
		if(editNodeId != null && currentNode.getId() == editNodeId.longValue())
		{

			structrNode.renderNode(out, currentNode, editUrl, editNodeId);

		} else
		{
			SecurityContext securityContext = out.getSecurityContext();
			if(securityContext.isVisible(currentNode)) {

				structrNode.renderNode(out, currentNode, editUrl, editNodeId);
			}
		}
	}

	@Override
	public String getContentType(NodeLink node)
	{
		return("text/html");
	}
}
