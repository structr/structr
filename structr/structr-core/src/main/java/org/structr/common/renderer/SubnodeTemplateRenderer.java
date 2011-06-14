package org.structr.common.renderer;

import java.io.StringWriter;
import org.apache.commons.lang.StringUtils;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.PlainText;

/**
 *
 * @author Christian Morgner
 */
public class SubnodeTemplateRenderer implements NodeRenderer<PlainText>
{
	@Override
	public void renderNode(StructrOutputStream out, PlainText currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		if(currentNode.isVisible())
		{
			String html = currentNode.getContent();
			if(StringUtils.isNotBlank(html))
			{

				StringWriter content = new StringWriter();

				// process content with Freemarker
				currentNode.replaceByFreeMarker(html, content, startNode, editUrl, editNodeId);

				StringBuilder content2 = new StringBuilder(content.toString());

				// finally, replace %{subnodeKey} by rendered content of subnodes with this name
				currentNode.replaceBySubnodes(content2, startNode, editUrl, editNodeId);
				out.append(content2.toString());

			}
		}
	}

	@Override
	public String getContentType(PlainText node)
	{
		return ("text/html");
	}
}
