package org.structr.renderer;

import java.util.List;
import org.structr.common.CurrentRequest;
import org.structr.common.RenderMode;
import org.structr.common.SecurityContext;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.web.Breadcrumb;

/**
 *
 * @author Christian Morgner
 */
public class BreadcrumbRenderer implements NodeRenderer<Breadcrumb>
{
	@Override
	public void renderNode(StructrOutputStream out, Breadcrumb currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		SecurityContext securityContext = CurrentRequest.getSecurityContext();
		if(securityContext.isVisible(currentNode)) {

			renderBreadcrumbItems(out, startNode, currentNode);

		}
	}

	/**
	 * Render breadcrumb items
	 *
	 * @param out
	 * @param startNode
	 */
	private void renderBreadcrumbItems(final StructrOutputStream out, final AbstractNode startNode, final AbstractNode currentNode)
	{
		SecurityContext securityContext = CurrentRequest.getSecurityContext();
		List<AbstractNode> ancestors = startNode.getAncestorNodes();
		String cssClass = "";
		int currentPos = 0;

		out.append("<ul>");

		for(AbstractNode breadcrumbItem : ancestors)
		{
			if(currentPos == 0)
			{
				cssClass = " first";
			}

			if(currentPos == ancestors.size() - 1)
			{
				cssClass = " last";
			}

			if(breadcrumbItem.equals(startNode))
			{
				cssClass += " current";
			}

			if(securityContext.isVisible(breadcrumbItem)) {

				String relativeNodePath = breadcrumbItem.getNodePath(startNode).replace("&", "%26");

				if(!(cssClass.isEmpty()))
				{
					cssClass = " class=\"" + cssClass + "\"";
				}

				out.append("<li").append(cssClass).append(">");
				out.append("<span>" + "<a href=\"").append(relativeNodePath).append("\">");
				out.append(breadcrumbItem.getName());
				out.append("</a>").append("</span>\n");
				out.append("</li>");

			}
		}

		out.append("</ul>");

	}

	@Override
	public String getContentType(Breadcrumb currentNode)
	{
		return("text/html");
	}
}
