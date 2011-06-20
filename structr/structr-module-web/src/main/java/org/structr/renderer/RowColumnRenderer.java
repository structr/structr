package org.structr.renderer;

import java.util.List;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.web.Column;

/**
 *
 * @author Christian Morgner
 */
public class RowColumnRenderer implements NodeRenderer<Column>
{
	private String prefix = null;

	public RowColumnRenderer(String prefix)
	{
		this.prefix = prefix;
	}

	@Override
	public void renderNode(StructrOutputStream out, Column currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		List<AbstractNode> subnodes = currentNode.getSortedDirectChildAndLinkNodes();

		out.append("<div class=\"").append(prefix).append("\">");
		// render nodes in correct order
		for(AbstractNode s : subnodes)
		{

			out.append("<div class=\"").append(prefix).append("Item\">");

			if(editNodeId != null && s.getId() == editNodeId.longValue())
			{
				currentNode.renderEditFrame(out, editUrl);
			} else
			{
				s.renderNode(out, startNode, editUrl, editNodeId);
			}
			out.append("</div>");
		}

		out.append("</div>");
	}

	@Override
	public String getContentType(Column currentNode)
	{
		return("text/html");
	}
}
