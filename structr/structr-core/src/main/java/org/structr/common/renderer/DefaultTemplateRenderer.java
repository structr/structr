package org.structr.common.renderer;

import java.util.List;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Template;

/**
 *
 * @author Christian Morgner
 */
public class DefaultTemplateRenderer implements NodeRenderer<AbstractNode>
{
	private boolean renderSubnodes = true;

	public DefaultTemplateRenderer()
	{
	}

	public DefaultTemplateRenderer(boolean renderSubnodes)
	{
		this.renderSubnodes = renderSubnodes;
	}

	@Override
	public void renderNode(StructrOutputStream out, AbstractNode currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		if(currentNode.hasTemplate())
		{
			Template template = currentNode.getTemplate();
			template.setCallingNode(currentNode);

			template.renderNode(out, currentNode, editUrl, editNodeId);

		} else
		if(renderSubnodes)
		{

			List<AbstractNode> subnodes = currentNode.getSortedDirectChildAndLinkNodes();

			// render subnodes in correct order
			for(AbstractNode s : subnodes)
			{
				s.renderNode(out, currentNode, editUrl, editNodeId);
			}
		}
	}

	@Override
	public String getContentType(AbstractNode node)
	{
		return ("text/html");
	}
}
