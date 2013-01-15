package org.structr.web.entity;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.web.common.RenderContext;

/**
 *
 * @author Christian Morgner
 */
public interface Renderable {

	public void render(SecurityContext securityContext, RenderContext renderContext, int depth) throws FrameworkException;
}
