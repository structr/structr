package org.structr.common.renderer;

import java.util.List;
import org.structr.common.CurrentRequest;
import org.structr.common.RenderMode;
import org.structr.common.SecurityContext;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Template;

/**
 *
 * @author Christian Morgner
 */
public class ExternalTemplateRenderer implements NodeRenderer<AbstractNode>
{
	private boolean renderSubnodesIfNoTemplate = false;

	public ExternalTemplateRenderer(boolean renderSubnodesIfNoTemplate)
	{
		this.renderSubnodesIfNoTemplate = renderSubnodesIfNoTemplate;
	}

	@Override
	public void renderNode(StructrOutputStream out, AbstractNode currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		SecurityContext securityContext = CurrentRequest.getSecurityContext();
		if(securityContext.isVisible(currentNode)) {

			if(currentNode.hasTemplate())
			{
				Template template = currentNode.getTemplate();

				template.setCallingNode(currentNode);
				template.renderNode(out, startNode, editUrl, editNodeId);

			} else
			if(renderSubnodesIfNoTemplate)
			{
				List<AbstractNode> subnodes = currentNode.getSortedDirectChildAndLinkNodes();

				// render subnodes in correct order
				for(AbstractNode s : subnodes)
				{
					s.renderNode(out, startNode, editUrl, editNodeId);
				}
			}
		}
	}

	@Override
	public String getContentType(AbstractNode node)
	{
		return ("text/html");
	}
}
