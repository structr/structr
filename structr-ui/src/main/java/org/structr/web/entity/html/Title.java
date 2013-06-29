/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.entity.html;

import org.structr.web.entity.dom.DOMElement;
import org.neo4j.graphdb.Direction;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;

import org.structr.web.common.RelType;
import org.structr.core.property.CollectionProperty;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.Content;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Title extends DOMElement {

	public static final CollectionProperty<Head>    heads    = new CollectionProperty<Head>("heads", Head.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Content> contents = new CollectionProperty<Content>("contents", Content.class, RelType.CONTAINS, Direction.OUTGOING, false);

	@Override
	public void render(SecurityContext securityContext, RenderContext renderContext, int depth) throws FrameworkException {
		
		super.render(securityContext, renderContext, depth);

		if (renderContext.getEdit()) {
			
			renderContext.getBuffer()
				.append("\n    <script type=\"text/javascript\" src=\"/structr/js/lib/jquery-1.9.1.js\"></script>")
				.append("\n    <script type=\"text/javascript\" src=\"/structr/js/structr-edit.js\"></script>")
				.append("\n    <link rel=\"stylesheet\" type=\"text/css\" media=\"screen\" href=\"/structr/css/edit.css\"></link>");
			
		}
	
	}
	

}
