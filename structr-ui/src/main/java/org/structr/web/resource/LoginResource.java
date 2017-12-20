/**
 * Copyright (C) 2010-2017 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.resource;


import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.resource.Resource;
import org.structr.schema.ConfigurationProvider;
import org.structr.web.entity.User;

/**
 * Resource that handles user logins.
 */
public class LoginResource extends Resource {

	private static final Logger logger       = LoggerFactory.getLogger(LoginResource.class.getName());

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

		final ConfigurationProvider config = StructrApp.getConfiguration();
		final PropertyMap properties       = PropertyMap.inputTypeToJavaType(securityContext, User.class, propertySet);
		final PropertyKey<String> nameKey  = StructrApp.key(User.class, "name");
		final PropertyKey<String> eMailKey = StructrApp.key(User.class, "eMail");
		final PropertyKey<String> pwdKey   = StructrApp.key(User.class, "password");

		final String name                  = properties.get(nameKey);
		final String email                 = properties.get(eMailKey);
		final String password              = properties.get(pwdKey);

		String emailOrUsername = StringUtils.isNotEmpty(email) ? email : name;

		if (StringUtils.isNotEmpty(emailOrUsername) && StringUtils.isNotEmpty(password)) {

			Principal user = securityContext.getAuthenticator().doLogin(securityContext.getRequest(), emailOrUsername, password);

			if (user != null) {

				logger.info("Login successful: {}", new Object[]{ user });

				// make logged in user available to caller
				securityContext.setCachedUser(user);

				RestMethodResult methodResult = new RestMethodResult(200);
				methodResult.addContent(user);

				return methodResult;
			}

		}

		logger.info("Invalid credentials (name, email, password): {}, {}, {}", new Object[]{ name, email, password });

		return new RestMethodResult(401);
	}

	@Override
	public Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page) throws FrameworkException {
		throw new NotAllowedException("GET not allowed on " + getResourceSignature());
	}

	@Override
	public RestMethodResult doPut(Map<String, Object> propertySet) throws FrameworkException {
		throw new NotAllowedException("PUT not allowed on " + getResourceSignature());
	}

	@Override
	public RestMethodResult doDelete() throws FrameworkException {
		throw new NotAllowedException("DELETE not allowed on " + getResourceSignature());
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {
		return null;
	}

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
