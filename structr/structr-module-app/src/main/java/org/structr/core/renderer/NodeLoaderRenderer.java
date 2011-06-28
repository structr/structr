package org.structr.core.renderer;

import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.app.AppNodeLoader;

/**
 *
 * @author Christian Morgner
 */
public class NodeLoaderRenderer implements NodeRenderer<AppNodeLoader>
{
	@Override
	public void renderNode(StructrOutputStream out, AppNodeLoader currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		// FIXME: HTML code hard-coded..

		// maybe we can let the enclosing FORM instance decide how to pass request
		// parameters into the form.

		String loaderSourceParameter = currentNode.getStringProperty(AppNodeLoader.ID_SOURCE_KEY);
		if(loaderSourceParameter != null)
		{
			Object value = currentNode.getValue();
			if(value != null)
			{
				out.append("<input type='hidden' name='");
				out.append(loaderSourceParameter);
				out.append("' value='");
				out.append(value);
				out.append("' />");
			}
		}
	}

	@Override
	public String getContentType(AppNodeLoader node)
	{
		return ("text/html");
	}
}
