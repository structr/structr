/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.exception.PasswordChangeRequiredException;
import org.structr.core.auth.exception.TooManyFailedLoginAttemptsException;
import org.structr.core.auth.exception.TwoFactorAuthenticationFailedException;
import org.structr.core.auth.exception.TwoFactorAuthenticationNextStepException;
import org.structr.core.auth.exception.TwoFactorAuthenticationRequiredException;
import org.structr.core.entity.Principal;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.rest.RestMethodResult;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.resource.Resource;

/**
 * Resource that handles user logins.
 */
public class LoginResource extends Resource {

	private static final Logger logger = LoggerFactory.getLogger(LoginResource.class.getName());

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

		final PropertyMap properties       = PropertyMap.inputTypeToJavaType(securityContext, Principal.class, propertySet);
		final PropertyKey<String> eMailKey = StructrApp.key(Principal.class, "eMail");
		final PropertyKey<String> pwdKey   = StructrApp.key(Principal.class, "password");

		final String name     = properties.get(Principal.name);
		final String email    = properties.get(eMailKey);
		final String password = properties.get(pwdKey);

		final String emailOrUsername = StringUtils.isNotEmpty(email) ? email : name;

		final String twoFactorToken     = properties.get(StructrApp.key(Principal.class, "twoFactorToken"));
		final String twoFactorCode      = properties.get(StructrApp.key(Principal.class, "twoFactorCode"));

		Principal user = null;

		try {

			if (StringUtils.isNotEmpty(twoFactorToken)) {

				user = AuthHelper.getUserForTwoFactorToken(twoFactorToken);

			} else  if (StringUtils.isNotEmpty(emailOrUsername) && StringUtils.isNotEmpty(password)) {

				user = securityContext.getAuthenticator().doLogin(securityContext.getRequest(), emailOrUsername, password);
			}

			if (user != null) {

				final boolean twoFactorAuthenticationSuccessOrNotNecessary = AuthHelper.handleTwoFactorAuthentication(user, twoFactorCode, twoFactorToken, securityContext.getRequest().getRemoteAddr());

				if (twoFactorAuthenticationSuccessOrNotNecessary) {

					AuthHelper.doLogin(securityContext.getRequest(), user);

					logger.info("Login successful: {}", user);

					// make logged in user available to caller
					securityContext.setCachedUser(user);

					RestMethodResult methodResult = new RestMethodResult(200);
					methodResult.addContent(user);

					return methodResult;
				}
			}

		} catch (PasswordChangeRequiredException ex) {

			RestMethodResult methodResult = new RestMethodResult(401);
			methodResult.addHeader("reason", "changed");

			return methodResult;

		} catch (TooManyFailedLoginAttemptsException ex) {

			RestMethodResult methodResult = new RestMethodResult(401);
			methodResult.addHeader("reason", "attempts");

			return methodResult;

		} catch (TwoFactorAuthenticationFailedException ex) {

			RestMethodResult methodResult = new RestMethodResult(401);
			methodResult.addHeader("reason", "twofactor");

			return methodResult;

		} catch (TwoFactorAuthenticationRequiredException ex) {

			RestMethodResult methodResult = new RestMethodResult(204);
			methodResult.addHeader("forceRegistrationPage", Settings.TwoFactorForceRegistrationPage.getValue());
			methodResult.addHeader("qrdata",                Base64.getUrlEncoder().encodeToString(Principal.getTwoFactorUrl(user).getBytes(Charset.forName("ISO-8859-1"))));

			securityContext.getAuthenticator().doLogout(securityContext.getRequest());

			return methodResult;

		} catch (TwoFactorAuthenticationNextStepException ex) {

			RestMethodResult methodResult = new RestMethodResult(202);
			methodResult.addHeader("token", ex.getNextStepToken());
			methodResult.addHeader("twoFactorLoginPage", Settings.TwoFactorLoginPage.getValue());

			securityContext.getAuthenticator().doLogout(securityContext.getRequest());

			return methodResult;
		}

		logger.info("Invalid credentials for {}", emailOrUsername);

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
