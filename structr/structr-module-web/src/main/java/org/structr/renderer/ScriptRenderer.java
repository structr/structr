package org.structr.renderer;

import org.structr.common.CurrentRequest;
import org.structr.common.RenderMode;
import org.structr.common.SecurityContext;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.web.Script;

/**
 *
 * @author Christian Morgner
 */
public class ScriptRenderer implements NodeRenderer<Script>
{
	@Override
	public void renderNode(StructrOutputStream out, Script currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		SecurityContext securityContext = CurrentRequest.getSecurityContext();
		if(securityContext.isVisible(currentNode)) {

			out.append(currentNode.evaluate());
		}
	}

	@Override
	public String getContentType(Script currentNode)
	{
		return ("text/html");
	}
}
