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

import org.structr.core.Result;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.ViewTransformation;
import org.structr.core.node.NodeFactory;

/**
 *
 * @author Christian Morgner
 */
public class TransformationResource extends WrappingResource {

	private ViewTransformation transformation = null;
	
	public TransformationResource(SecurityContext securityContext, ViewTransformation transformation) {
		this.securityContext = securityContext;
		this.transformation  = transformation;
	}
	
	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {
		return false;	// no direct instantiation
	}

	@Override
	public Result doGet(String sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {
		
		if(wrappedResource != null && transformation != null) {

			// allow view transformation to avoid evaluation of wrapped resource
			if (transformation.evaluateWrappedResource()) {
				
				Result result = wrappedResource.doGet(sortKey, sortDescending, NodeFactory.DEFAULT_PAGE_SIZE, NodeFactory.DEFAULT_PAGE, null);
			
				try {

					transformation.apply(securityContext, result.getResults());
					result.setRawResultCount(result.size());

				} catch(Throwable t) {
					t.printStackTrace();
				}

				// apply paging later
				return PagingHelper.subResult(result, pageSize, page, offsetId);
				
			} else {
				
				List<? extends GraphObject> listToTransform = new LinkedList<GraphObject>();
				transformation.apply(securityContext, listToTransform);

				Result result = new Result(listToTransform, listToTransform.size(), wrappedResource.isCollectionResource(), wrappedResource.isPrimitiveArray());

				// apply paging later
				return PagingHelper.subResult(result, pageSize, page, offsetId);
				
			}
		}
		
		List emptyList = Collections.emptyList();
		return new Result(emptyList, null, isCollectionResource(), isPrimitiveArray());
	}

	@Override
	public String getResourceSignature() {
		if(wrappedResource != null) {
			return wrappedResource.getResourceSignature();
		}
		
		return "";
	}
}
