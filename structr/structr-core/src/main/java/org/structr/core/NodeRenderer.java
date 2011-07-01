/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.core;

import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.entity.AbstractNode;

/**
 * Defines the signature of a pluggable node renderer. Classes that implement
 * this interface can be used in the
 * {@see org.structr.core.entity.AbstractNode#initializeRenderers} method to
 * control the appearance of different node types.
 *
 * @author Christian Morgner
 */
public interface NodeRenderer<T extends AbstractNode>
{
	/**
	 * Renders the node <code>currentNode</code> to the output stream
	 * <code>out</code>.
	 *
	 * @param out the output stream that can be used for rendering
	 * @param currentNode the node to be rendered
	 * @param startNode the first node of the current rendering turn
	 * @param editUrl the edit url
	 * @param editNodeId the ID of the node that should be edited
	 * @param renderMode the render mode
	 */
	public void renderNode(final StructrOutputStream out, final T currentNode, final AbstractNode startNode, final String editUrl, final Long editNodeId, RenderMode renderMode);

	/**
	 * Returns the content type of this renderer.
	 *
	 * @param currentNode the node to be rendered
	 * @return the content type of the content this renderer produces
	 */
	public String getContentType(final T currentNode);
}
