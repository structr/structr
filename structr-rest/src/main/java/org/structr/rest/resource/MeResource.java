/**
 * Copyright (C) 2010-2015 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.resource;

import org.structr.core.Result;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.Principal;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.exception.NotAllowedException;

/**
 *
 *
 */
public class MeResource extends TypedIdResource {

	private static final Logger logger = Logger.getLogger(MeResource.class.getName());
	
	public MeResource() {
		super(null);
	}

	public MeResource(SecurityContext securityContext) {
		super(securityContext);
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

		this.securityContext = securityContext;

		if ("me".equalsIgnoreCase(part)) {
			
			this.typeResource = new TypeResource();
			this.typeResource.setSecurityContext(securityContext);
			this.typeResource.checkAndConfigure("user", securityContext, request);

			Principal user = securityContext.getUser(true);
			if (user != null) {

				// we need to create synthetic nested constraints
				this.idResource = new UuidResource();
				this.idResource.setSecurityContext(securityContext);
				this.idResource.checkAndConfigure(user.getProperty(GraphObject.id), securityContext, request);

			}
		}

		return true;
	}

	@Override
	public Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		Principal user = securityContext.getUser(true);
		if (user != null) {

			List<GraphObject> resultList = new LinkedList<>();
			resultList.add(user);

			return new Result(resultList, null,  isCollectionResource(), isPrimitiveArray());

		} else {

			throw new NotAllowedException();
		}
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
		
		if (typeResource != null) {
			return typeResource.doPost(propertySet);
		}
		
		throw new IllegalMethodException();
	}

	@Override
	public String getUriPart() {
		return "me";
	}

}
