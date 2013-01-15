/*
 *  Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.common;

import org.structr.core.graph.GetNodeByIdCommand;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.PropertyKey;
import org.structr.web.entity.dom.Page;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class PageHelper {
	
	private static final Logger logger = Logger.getLogger(PageHelper.class.getName());

	public static AbstractNode getNodeById(SecurityContext securityContext, String id) {

		if (id == null) {

			return null;
		}

		try {

			return (AbstractNode) Services.command(securityContext, GetNodeByIdCommand.class).execute(id);

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Unable to load node with id {0}, {1}", new java.lang.Object[] { id, t.getMessage() });

		}

		return null;

	}

	/**
	 * Find all pages which contain the given html element node
	 *
	 * @param securityContext
	 * @param node
	 * @return
	 */
	public static List<Page> getPages(SecurityContext securityContext, final AbstractNode node) throws FrameworkException {

		List<Page> pages = new LinkedList<Page>();
		AbstractNode pageNode;
		List<AbstractRelationship> rels = node.getIncomingRelationships(RelType.CONTAINS);

		for (AbstractRelationship rel : rels) {

			for (PropertyKey key : rel.getProperties().keySet()) {

				pageNode = getNodeById(securityContext, key.dbName());

				if (pageNode != null && pageNode instanceof Page) {

					pages.add((Page) pageNode);
				}

			}

		}

		return pages;

	}

}
