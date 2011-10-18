package org.structr.common.renderer;

import org.structr.common.RenderMode;
import org.structr.common.SecurityContext;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Image;

/**
 *
 * @author Christian Morgner
 */
public class ImageSourceRenderer implements NodeRenderer<Image>
{
	@Override
	public void renderNode(StructrOutputStream out, Image currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		String imageUrl = null;
		if(currentNode.getUrl() == null)
		{
			imageUrl = currentNode.getNodePath(currentNode);
		} else
		{
			imageUrl = currentNode.getUrl();
		}

		// FIXME: title shoud be rendered dependent of locale
		SecurityContext securityContext = out.getSecurityContext();
		if(securityContext.isVisible(currentNode)) {

			String title = currentNode.getTitle();

			//out.append("<img src=\"").append(getNodeURL(renderMode, contextPath)).append("\" title=\"").append(getTitle()).append("\" alt=\"").append(getTitle()).append("\" width=\"").append(getWidth()).append("\" height=\"").append(getHeight()).append("\">");
			out.append("<img src=\"").append(imageUrl).append("\" title=\"").append(title).append("\" alt=\"").append(title).append("\" width=\"").append(currentNode.getWidth()).append("\" height=\"").append(currentNode.getHeight()).append("\">");
		}
	}

	@Override
	public String getContentType(Image node)
	{
		return ("text/html");


	}
}
