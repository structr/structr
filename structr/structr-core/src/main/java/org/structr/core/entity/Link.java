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



package org.structr.core.entity;

import org.structr.common.PropertyView;
import org.structr.common.RenderMode;
import org.structr.common.renderer.LinkRenderer;
import org.structr.core.EntityContext;
import org.structr.core.NodeRenderer;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class Link extends AbstractNode {

	static {

		EntityContext.registerPropertySet(Link.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- fields ---------------------------------------------------------

	private String iconSrc;
	private AbstractNode structrNode;

	//~--- methods --------------------------------------------------------

	/**
	 * Special constructor for wrapper node
	 *
	 * @param node
	 */
	public void init(AbstractNode node) {

		super.init(node.dbNode);
		setStructrNode(node);
	}

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {

		renderers.put(RenderMode.Default,
			      new LinkRenderer());
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {

		return (iconSrc != null)
		       ? iconSrc
		       : "/images/linked.png";
	}

	/**
	 * Get structr node
	 *
	 * @return
	 */
	public AbstractNode getStructrNode() {
		return structrNode;
	}

	//~--- set methods ----------------------------------------------------

	/**
	 * Set structr node
	 */
	public void setStructrNode(final AbstractNode structrNode) {
		this.structrNode = structrNode;
	}

	public void setIconSrc(String iconSrc) {
		this.iconSrc = iconSrc;
	}
}
