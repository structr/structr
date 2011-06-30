package org.structr.core.renderer;

import java.io.StringWriter;
import java.util.List;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public class ContentChildTemplateRenderer implements NodeRenderer {

	@Override
	public void renderNode(StructrOutputStream out, AbstractNode currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode) {

		String templateSource = getTemplateFromNode(currentNode);
		StringWriter content = new StringWriter(100);

		List<AbstractNode> children = currentNode.getSortedDirectChildNodes();
		for(AbstractNode child : children)
		{
			AbstractNode.staticReplaceByFreeMarker(templateSource, content, child, editUrl, editNodeId);
		}

		out.append(content.toString());
	}

	@Override
	public String getContentType(AbstractNode currentNode) {
		
		return("text/html");
	}

	// ----- private methods -----
	private String getTemplateFromNode(final AbstractNode node)
	{
		String ret = "";

		if(node.hasTemplate())
		{
			ret = node.getTemplate().getContent();
		}

		return (ret);
	}
}
