package org.structr.common.renderer;

import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.structr.common.RenderMode;
import org.structr.common.SecurityContext;
import org.structr.common.StructrOutputStream;
import org.structr.common.error.FrameworkException;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.PlainText;

/**
 *
 * @author Christian Morgner
 */
public class ContentTemplateRenderer implements NodeRenderer<PlainText>
{
	private static final Logger logger = Logger.getLogger(ContentTemplateRenderer.class.getName());

	@Override
	public void renderNode(StructrOutputStream out, PlainText currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		SecurityContext securityContext = out.getSecurityContext();
		if(securityContext.isVisible(currentNode)) {

			String html = currentNode.getContent();
			if(StringUtils.isNotBlank(html))
			{
				StringWriter content = new StringWriter();

				// process content with Freemarker
				currentNode.replaceByFreeMarker(out.getRequest(), html, content, startNode, editUrl, editNodeId);

				StringBuilder content2 = new StringBuilder(content.toString());

				try {
					// finally, replace %{subnodeKey} by rendered content of subnodes with this name
					currentNode.replaceBySubnodes(out.getRequest(), content2, startNode, editUrl, editNodeId);
					out.append(content2.toString());
				} catch(FrameworkException fex) {
					logger.log(Level.WARNING, "Unable to replace content", fex);
				}
			}
		}
	}

	@Override
	public String getContentType(PlainText node)
	{
		return (node.getContentType());
	}
}
