package org.structr.renderer;

import org.structr.common.CurrentRequest;
import org.structr.common.RenderMode;
import org.structr.common.SecurityContext;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.web.Menu;

/**
 *
 * @author Christian Morgner
 */
public class MenuRenderer extends MenuItemRenderer implements NodeRenderer<Menu>
{
	@Override
	public void renderNode(StructrOutputStream out, Menu currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		SecurityContext securityContext = CurrentRequest.getSecurityContext();
		if(securityContext.isVisible(currentNode)) {

			renderMenuItems(out, currentNode, startNode, currentNode, 0, 0, 0, currentNode.getMaxDepth());

		}
	}

	@Override
	public String getContentType(Menu currentNode)
	{
		return ("text/html");
	}
}
