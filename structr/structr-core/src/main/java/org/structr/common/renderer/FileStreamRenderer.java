package org.structr.common.renderer;

import org.apache.commons.compress.utils.IOUtils;

import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.File;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;
import java.io.InputStream;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.CurrentRequest;
import org.structr.common.SecurityContext;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class FileStreamRenderer implements NodeRenderer<File> {

	private static final Logger logger = Logger.getLogger(FileStreamRenderer.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void renderNode(StructrOutputStream out, File currentNode, AbstractNode startNode, String editUrl,
			       Long editNodeId, RenderMode renderMode) {

		SecurityContext securityContext = CurrentRequest.getSecurityContext();
		if(securityContext.isVisible(currentNode)) {

			InputStream in = currentNode.getInputStream();

			try {

				if (in != null) {

					// just copy to output stream
					IOUtils.copy(in, out);
				}

			} catch (Throwable t) {
				logger.log(Level.SEVERE, "Error while rendering file", t);
			} finally {

				if (in != null) {

					try {
						in.close();
					} catch (IOException ignore) {}
				}
			}
		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getContentType(File node) {
		return (node.getContentType());
	}
}
