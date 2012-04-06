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
import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.search.*;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.resource.Resource;


/**
 *
 * @author Christian Morgner
 */
public class HtmlResource extends Resource {

	private String resourceName = null;
	
	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

		this.securityContext = securityContext;
		this.resourceName = part;
		
		// FIXME: look for resources and accept only existing ones!
		return part.toLowerCase().endsWith(".html");
	}

	@Override
	public List<? extends GraphObject> doGet() throws FrameworkException {
		throw new IllegalMethodException();
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
		throw new IllegalMethodException();
	}

	@Override
	public RestMethodResult doPut(Map<String, Object> propertySet) throws FrameworkException {
		throw new IllegalMethodException();
	}

	@Override
	public RestMethodResult doDelete() throws FrameworkException {
		throw new IllegalMethodException();
	}

	@Override
	public RestMethodResult doHead() throws FrameworkException {
		throw new IllegalMethodException();
	}

	@Override
	public RestMethodResult doOptions() throws FrameworkException {
		throw new IllegalMethodException();
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {
		
		if(next instanceof DynamicTypeResource) {

			DynamicTypeResource dynamicTypeResource = (DynamicTypeResource)next;
			
			// create search group
			SearchAttributeGroup group = new SearchAttributeGroup(SearchOperator.AND);
			group.add(Search.orExactType(org.structr.web.entity.Resource.class.getSimpleName()));
			
			// create search attributes
			List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();
			searchAttrs.add(Search.andExactName(resourceName));
			searchAttrs.add(group);

			// Searching for resources needs super user context anyway
			List<AbstractNode> results = (List<AbstractNode>) Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(null, false, false, searchAttrs);

			if (!results.isEmpty()) {

				AbstractNode node = results.get(0);
				String uuid = node.getStringProperty(AbstractNode.Key.uuid);
				
				dynamicTypeResource.setResourceId(uuid);
				
				return dynamicTypeResource;
			}			
		}
		
		return null;
	}

	@Override
	public String getUriPart() {
		return resourceName;
	}

	@Override
	public boolean isCollectionResource() throws FrameworkException {
		return true;
	}
}
