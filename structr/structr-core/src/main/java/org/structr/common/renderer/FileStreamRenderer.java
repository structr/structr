package org.structr.common.renderer;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.compress.utils.IOUtils;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.File;

/**
 *
 * @author Christian Morgner
 */
public class FileStreamRenderer implements NodeRenderer<File>
{
	private static final Logger logger = Logger.getLogger(FileStreamRenderer.class.getName());

	@Override
	public void renderNode(StructrOutputStream out, File currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		if(currentNode.isVisible())
		{
			try
			{
				InputStream in = currentNode.getInputStream();
				if(in != null)
				{
					// just copy to output stream
					IOUtils.copy(in, out);
				}

			} catch(Throwable t)
			{
				logger.log(Level.SEVERE, "Error while rendering file", t);
			}
		}
	}

	@Override
	public String getContentType(File node)
	{
		return(node.getContentType());
	}
}
