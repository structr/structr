package org.structr.web.entity.dom;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.web.common.RenderContext;

/**
 *
 * @author Christian Morgner
 */

public class Comment extends Content implements org.w3c.dom.Comment {
	
	@Override
	public void render(SecurityContext securityContext, RenderContext renderContext, int depth) throws FrameworkException {

		renderContext.getBuffer().append(("<!--"));
		
		super.render(securityContext, renderContext, depth);
		
		renderContext.getBuffer().append("-->");
	}
	
}
