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

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.auth.exception.PasswordChangeRequiredException;
import org.structr.core.auth.exception.TooManyFailedLoginAttemptsException;
import org.structr.core.auth.exception.TwoFactorAuthenticationFailedException;
import org.structr.core.auth.exception.TwoFactorAuthenticationRequiredException;
import org.structr.core.auth.exception.TwoFactorAuthenticationTokenInvalidException;
import org.structr.core.entity.Principal;
import org.structr.core.property.PropertyKey;
import org.structr.rest.RestMethodResult;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.resource.FilterableResource;
import org.structr.web.function.BarcodeFunction;

/**
 * Resource that handles user logins.
 */
public class LoginResource extends FilterableResource {

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

		final String username       = (String) propertySet.get("name");
		final String email          = (String) propertySet.get("eMail");
		final String password       = (String) propertySet.get("password");
		final String twoFactorToken = (String) propertySet.get("twoFactorToken");
		final String twoFactorCode  = (String) propertySet.get("twoFactorCode");

		final String emailOrUsername = StringUtils.isNotEmpty(email) ? email : username;

		Principal user = null;

		try {

			if (StringUtils.isNotEmpty(twoFactorToken)) {

				user = AuthHelper.getUserForTwoFactorToken(twoFactorToken);

			} else if (StringUtils.isNotEmpty(emailOrUsername) && StringUtils.isNotEmpty(password)) {

				user = securityContext.getAuthenticator().doLogin(securityContext.getRequest(), emailOrUsername, password);
			}

			if (user != null) {

				final boolean twoFactorAuthenticationSuccessOrNotNecessary = AuthHelper.handleTwoFactorAuthentication(user, twoFactorCode, twoFactorToken, securityContext.getRequest().getRemoteAddr());

				if (twoFactorAuthenticationSuccessOrNotNecessary) {

					AuthHelper.doLogin(securityContext.getRequest(), user);

					logger.info("Login successful: {}", user);

					user.setSecurityContext(securityContext);

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

		} catch (TwoFactorAuthenticationTokenInvalidException ex) {

			RestMethodResult methodResult = new RestMethodResult(401);
			methodResult.addHeader("reason", "twofactortoken");

			return methodResult;

		} catch (TwoFactorAuthenticationRequiredException ex) {

			RestMethodResult methodResult = new RestMethodResult(202);
			methodResult.addHeader("token", ex.getNextStepToken());
			methodResult.addHeader("twoFactorLoginPage", Settings.TwoFactorLoginPage.getValue());

			if (ex.showQrCode()) {

				try {

					final Map<String, Object> hints = new HashMap();
					hints.put("MARGIN", 0);
					hints.put("ERROR_CORRECTION", "M");

					methodResult.addHeader("qrdata", Base64.getUrlEncoder().encodeToString(BarcodeFunction.getQRCode(Principal.getTwoFactorUrl(user), "QR_CODE", 200, 200, hints).getBytes("ISO-8859-1")));

				} catch (UnsupportedEncodingException uee) {
					logger.warn("Charset ISO-8859-1 not supported!?", uee);
				}
			}

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
