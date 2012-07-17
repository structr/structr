/*
 *  Copyright (C) 2012 Axel Morgner
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



package org.structr.web.converter;

import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.PropertyConverter;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.web.entity.Page;
import org.structr.web.entity.html.HtmlElement;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author axel
 */
public class PathsConverter extends PropertyConverter {

	@Override
	public Object convertForSetter(Object source, Value value) {

		// read only
		return null;
	}

	@Override
	public Object convertForGetter(Object source, Value value) {

		Command searchNode              = Services.command(securityContext, SearchNodeCommand.class);
		List<Page> containingPages      = new LinkedList<Page>();
		List<AbstractRelationship> rels = ((HtmlElement) currentObject).getIncomingRelationships(RelType.CONTAINS);

		for (AbstractRelationship rel : rels) {

			Map<String, Object> props = rel.getProperties();

			for (Entry<String, Object> entry : props.entrySet()) {

				String key = entry.getKey();

				// Check if key is a node id (UUID format)
				if (key.matches("[a-zA-Z0-9]{32}")) {

					List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();

					attrs.add(Search.andExactType(Page.class.getSimpleName()));
					attrs.add(Search.andExactUuid(key));

					List<AbstractNode> results = null;

					try {

						results = (List<AbstractNode>) searchNode.execute(null, false, false, attrs);

					} catch (Throwable ignore) {}

					if (results != null && !results.isEmpty()) {

						containingPages.add((Page) results.get(0));
					}

				}

			}

		}

		Set<String> paths = new HashSet<String>();

		// Create a path for each page
		for (Page page : containingPages) {

			String pageId     = page.getUuid();
			String path       = "";
			AbstractNode node = (AbstractNode) currentObject;

			// Stop at page node
			while (!(node instanceof Page)) {

				List<AbstractRelationship> containsRels = node.getIncomingRelationships(RelType.CONTAINS);

				for (AbstractRelationship r : containsRels) {

					Long pos = r.getLongProperty(pageId);

					if (pos != null) {

						path = "_" + pos + path;

						node = r.getStartNode();
						
						// A node should only have one position per pageId
						break;
					}

				}
				
				
				

			}

			paths.add(pageId + path);

		}

		return paths;

	}

}
