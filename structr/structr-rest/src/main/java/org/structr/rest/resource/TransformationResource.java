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
package org.structr.rest.resource;

import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Transformation;

/**
 *
 * @author Christian Morgner
 */
public class TransformationResource extends WrappingResource {

	private Transformation<List<? extends GraphObject>> transformation = null;
	
	public TransformationResource(SecurityContext securityContext, Transformation<List<? extends GraphObject>> transformation) {
		this.securityContext = securityContext;
		this.transformation  = transformation;
	}
	
	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {
		return false;	// no direct instantiation
	}

	@Override
	public Result doGet(String sortKey, boolean sortDescending, int pageSize, int page) throws FrameworkException {
		
		if(wrappedResource != null) {
			
			Result result = wrappedResource.doGet(sortKey, sortDescending, pageSize, page);
			
			if(transformation != null) {

				try {

					transformation.apply(securityContext, result.getResults());

				} catch(Throwable t) {
					t.printStackTrace();
				}
			}
				
			return result;
		}
		
		List emptyList = Collections.emptyList();
		return new Result(emptyList, isCollectionResource(), isPrimitiveArray());
	}

	@Override
	public String getResourceSignature() {
		if(wrappedResource != null) {
			return wrappedResource.getResourceSignature();
		}
		
		return "";
	}
}
