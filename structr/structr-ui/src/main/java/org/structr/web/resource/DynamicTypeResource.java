/*
 *  Copyright (C) 2012 Axel Morgner
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
package org.structr.web.resource;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.dom4j.tree.AbstractNode;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.resource.TypeResource;

/**
 *
 * @author Christian Morgner
 */
public class DynamicTypeResource extends TypeResource {

	@Override
	public List<GraphObject> doGet() throws FrameworkException {

		// check for dynamic type, use super class otherwise
		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		AbstractNode topNode                   = null;
		boolean includeDeleted                 = false;
		boolean publicOnly                     = false;

		if (rawType != null) {

			searchAttributes.add(Search.andExactProperty("_html_data-class", EntityContext.normalizeEntityName(rawType)));
			
			// searchable attributes from EntityContext
			hasSearchableAttributes(rawType, request, searchAttributes);

			// do search
			List<GraphObject> results = (List<GraphObject>) Services.command(securityContext, SearchNodeCommand.class).execute(topNode, includeDeleted, publicOnly, searchAttributes);
			if (!results.isEmpty()) {
				return results;

			}
		}

			
		return super.doGet();
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {
		
		// TODO: implement POSTing of dynamic types
		
		throw new IllegalMethodException();
	}
}
