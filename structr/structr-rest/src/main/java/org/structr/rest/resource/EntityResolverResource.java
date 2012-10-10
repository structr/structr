/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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
package org.structr.rest.resource;

import org.structr.core.Result;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.GetNodeByIdCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;

/**
 *
 * @author Christian Morgner
 */
public class EntityResolverResource extends SortableResource {

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {
		
		return true;
	}

	@Override
	public Result doGet(String sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {
		throw new IllegalMethodException();
	}
	
	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {
		
		Command searchCommand = Services.command(securityContext, GetNodeByIdCommand.class);
		RestMethodResult result = new RestMethodResult(200);

		for(Object o : propertySet.values()) {
			
			if(o instanceof String) {
				
				String id = (String)o;
				
				AbstractNode node = (AbstractNode)searchCommand.execute(id);
				if(node != null) {
					result.addContent(node);
				}
			}
		}
		
		return result;
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
	public RestMethodResult doDelete() throws FrameworkException {
		throw new IllegalMethodException();
	}

	@Override
	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {
		throw new IllegalMethodException();
	}

        @Override
        public String getResourceSignature() {
                return getUriPart();
        }
	
	@Override
	public String getUriPart() {
		return "resolver";
	}
}
