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
import org.structr.help.Container;
import org.structr.help.Content;
import org.structr.help.Paragraph;

/**
 * Trash folder
 * 
 * @author amorgner
 * 
 */
public class Trash extends Folder {

    private final static String ICON_SRC = "/images/bin.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

	@Override
	public AbstractComponent getHelpContent() {

		AbstractComponent root = new Container();

		root.add(new Paragraph().add(new Content(
		    "This is a Trash node. You can drag and drop other nodes into it,",
		    "and recover them later, or empty the trash to delete ",
		    "the nodes permanently."
		   )));

		return(root);
	}
}
