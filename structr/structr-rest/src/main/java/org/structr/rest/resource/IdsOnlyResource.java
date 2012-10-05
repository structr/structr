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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Value;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;

/**
 *
 * @author Christian Morgner
 */
public class IdsOnlyResource extends ViewFilterResource {

	private static final Logger logger = Logger.getLogger(IdsOnlyResource.class.getName());

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {
		this.securityContext = securityContext;
		return true;
	}

	@Override
	public Result doGet(String sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		if(wrappedResource != null) {
			return wrappedResource.doGet(sortKey, sortDescending, pageSize, page, offsetId);
		}

		throw new IllegalPathException();
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {

		if(wrappedResource != null) {
			return wrappedResource.doPost(propertySet);
		}
		throw new IllegalPathException();
	}

	@Override
	public RestMethodResult doPut(Map<String, Object> propertySet) throws FrameworkException {
		if(wrappedResource != null) {
			return wrappedResource.doPut(propertySet);
		}
		throw new IllegalPathException();
	}

	@Override
	public RestMethodResult doDelete() throws FrameworkException {
		if(wrappedResource != null) {
			return wrappedResource.doDelete();
		}
		throw new IllegalPathException();
	}

	@Override
	public RestMethodResult doHead() throws FrameworkException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public RestMethodResult doOptions() throws FrameworkException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getUriPart() {
		return "ids";
	}

	@Override
	public boolean isCollectionResource() {
		return true;
	}

	@Override
	public boolean isPrimitiveArray() {
		return true;
	}

	@Override
	public void configurePropertyView(Value<String> propertyView) {
		
		try {
			propertyView.set(securityContext, "ids");
			
		} catch(FrameworkException fex) {
			
			logger.log(Level.WARNING, "Unable to configure property view", fex);
		}
	}

}
