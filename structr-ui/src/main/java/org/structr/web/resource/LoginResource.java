/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.web.resource;

import org.apache.commons.codec.binary.Base64;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.resource.Resource;

//~--- JDK imports ------------------------------------------------------------

import java.security.SecureRandom;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
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

	private static final int SessionIdLength = 128;
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
		String password      = properties.get(User.password);
		
		if (name != null && password != null) {
			
			User user = (User) securityContext.doLogin(name, password);

			if (user != null) {
				
				logger.log(Level.INFO, "Login successfull: {0}", new Object[]{ user });

				// login successful, create new session ID and store it in User entity
				//user.setProperty(User.sessionId, secureRandomString());

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
		
//		String email    = (String) securityContext.getRequest().getParameter(User.email.jsonName());
//		String password = (String) securityContext.getRequest().getParameter(User.password.jsonName());
//		
//		if (email == null || password == null) {
//			throw new IllegalMethodException();
//		}
//		
//		User user = getUserForEmailAndPassword(email, password);
//
//		if (user == null) {
//			throw new UnauthorizedException();
//		}
//		
//		List<User> result = new LinkedList();
//		result.add(user);
//		
//		return new Result(result, result.size(), true, false);

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
	public RestMethodResult doHead() throws FrameworkException {

		throw new IllegalMethodException();

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
	private String secureRandomString() {

		SecureRandom secureRandom = new SecureRandom();
		byte[] binaryData         = new byte[SessionIdLength];

		// create random data
		secureRandom.nextBytes(binaryData);

		// return random data encoded in Base64
		return Base64.encodeBase64URLSafeString(binaryData);

	}

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

	
//	private User getUserForEmailAndPassword(final String email, final String password) throws FrameworkException {
//
//		String encryptedPassword    = PasswordEncrypter.encryptPassword(password);
//		List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();
//
//		attrs.add(Search.andExactProperty(User.email, email));
//		attrs.add(Search.andExactType(User.class.getSimpleName()));
//
//		// we need to search with a super user security context here..
//		Result<User> results = Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(attrs);
//
//		if (!results.isEmpty()) {
//
//			int resultCount = results.size();
//
//			if (resultCount == 1) {
//
//				User user = (User) results.get(0);
//
//				logger.log(Level.INFO, "{0} == {1}?", new Object[] { encryptedPassword, user.getProperty(User.password) });
//
//				// check password
//				if (encryptedPassword.equals(user.getProperty(User.password))) {
//
//					return user;
//				} else {
//
//					logger.log(Level.WARNING, "Wrong password for user with email {0}.", email);
//				}
//
//			} else {
//
//				logger.log(Level.WARNING, "Found {0} users with email address {1}. This should not happen!", new Object[] { resultCount, email });
//			}
//
//		} else {
//
//			logger.log(Level.WARNING, "No user found for email {0}.", email);
//		}
//
//		return null;
//
//	}

}
