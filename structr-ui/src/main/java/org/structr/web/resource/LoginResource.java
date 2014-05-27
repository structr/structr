/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.resource;


import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.resource.Resource;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.structr.core.entity.Principal;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.User;

//~--- classes ----------------------------------------------------------------

/**
 * Resource that handles user logins
 *
 * @author Axel Morgner
 */
public class LoginResource extends Resource {

	private static final Logger logger       = Logger.getLogger(LoginResource.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		this.securityContext = securityContext;

		if (getUriPart().equals(part)) {

			return true;
		}

		return false;

	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {

		PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, User.class, propertySet);

		String name          = properties.get(User.name);
		String email         = properties.get(User.eMail);
		String password      = properties.get(User.password);

		String emailOrUsername = StringUtils.isNotEmpty(email) ? email : name;

		if (StringUtils.isNotEmpty(emailOrUsername) && StringUtils.isNotEmpty(password)) {

			Principal user = (Principal) securityContext.getAuthenticator().doLogin(securityContext.getRequest(), emailOrUsername, password);

			if (user != null) {

				logger.log(Level.INFO, "Login successfull: {0}", new Object[]{ user });

				RestMethodResult methodResult = new RestMethodResult(200);
				methodResult.addContent(user);

				return methodResult;
			}

		}

		logger.log(Level.INFO, "Invalid credentials (name, password): {0}, {1}, {2}", new Object[]{ name, password });

		return new RestMethodResult(401);

	}

	@Override
	public Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		throw new NotAllowedException();

	}

	@Override
	public RestMethodResult doPut(Map<String, Object> propertySet) throws FrameworkException {

		throw new NotAllowedException();

	}

	@Override
	public RestMethodResult doDelete() throws FrameworkException {

		throw new NotAllowedException();

	}

	@Override
	public RestMethodResult doOptions() throws FrameworkException {

		throw new IllegalMethodException();

	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		return null;

	}

	// ----- private methods -----

	//~--- get methods ----------------------------------------------------

	@Override
	public Class getEntityClass() {

		return null;

	}

	@Override
	public String getUriPart() {

		return "login";

	}

	@Override
	public String getResourceSignature() {

		return "_login";

	}

	@Override
	public boolean isCollectionResource() {

		return false;

	}

}
