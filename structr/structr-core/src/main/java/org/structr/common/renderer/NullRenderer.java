package org.structr.common.renderer;

import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public class NullRenderer implements NodeRenderer<AbstractNode>
{

	@Override
	public void renderNode(StructrOutputStream out, AbstractNode currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
	}

	@Override
	public String getContentType(AbstractNode node)
	{
		return("");
	}
}
