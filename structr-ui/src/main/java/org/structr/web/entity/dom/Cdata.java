package org.structr.web.entity.dom;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.web.common.RenderContext;
import org.w3c.dom.CDATASection;

/**
 *
 * @author Christian Morgner
 */

public class Cdata extends Content implements CDATASection {
	
	@Override
	public void render(SecurityContext securityContext, RenderContext renderContext, int depth) throws FrameworkException {

		renderContext.getBuffer().append(("<!CDATA["));
		
		super.render(securityContext, renderContext, depth);
		
		renderContext.getBuffer().append("]]>");
	}
}
