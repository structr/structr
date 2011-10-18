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

import org.structr.common.AbstractComponent;
import org.structr.common.PropertyView;
import org.structr.common.renderer.RenderContext;
import org.structr.core.EntityContext;
import org.structr.help.Container;
import org.structr.help.Content;
import org.structr.help.Paragraph;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class Folder extends AbstractNode {

	static {

		EntityContext.registerPropertySet(Folder.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- methods --------------------------------------------------------

	@Override
	public boolean renderingAllowed(final RenderContext context) {
		return false;
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/folder.png";
	}

	@Override
	public AbstractComponent getHelpContent() {

		AbstractComponent root = new Container();

		root.add(new Paragraph().add(new Content("This is a Folder node.")));

		return (root);
	}
}
