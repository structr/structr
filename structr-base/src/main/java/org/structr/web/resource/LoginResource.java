/*
 * Copyright (C) 2010-2023 Structr GmbH
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
package org.structr.web.resource;


import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.search.SortOrder;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.exception.*;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.rest.RestMethodResult;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.resource.FilterableResource;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.html.P;
import org.structr.web.function.BarcodeFunction;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Resource that handles user logins.
 */
public class LoginResource extends FilterableResource {

	protected static final Logger logger = LoggerFactory.getLogger(LoginResource.class.getName());
	protected HttpServletRequest request;

	public String getErrorMessage() {
		return AuthHelper.STANDARD_ERROR_MSG;
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		this.securityContext = securityContext;

		if (getUriPart().equals(part)) {
			this.request = request;
			return true;
		}

		return false;
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

		final String username       = (String) propertySet.get("name");
		final String email          = (String) propertySet.get("eMail");
		final String password       = (String) propertySet.get("password");
		final String twoFactorToken = (String) propertySet.get("twoFactorToken");
		final String twoFactorCode  = (String) propertySet.get("twoFactorCode");

		String emailOrUsername = StringUtils.isNotEmpty(email) ? email : username;

		if (StringUtils.contains(emailOrUsername, "@")) {
			emailOrUsername = emailOrUsername.trim().toLowerCase();
		}

		RestMethodResult returnedMethodResult = null;
		final SecurityContext ctx             = SecurityContext.getSuperUserInstance();
		final App app                         = StructrApp.getInstance(ctx);
		Principal user                        = null;

		if (Settings.CallbacksOnLogin.getValue() == false) {
			ctx.disableInnerCallbacks();
		}

		try (final Tx tx = app.tx(true, true, true)) {

			try {

				user = getUserForCredentials(securityContext, emailOrUsername, password, twoFactorToken, twoFactorCode, propertySet);
				returnedMethodResult = doLogin(securityContext, user);

			} catch (PasswordChangeRequiredException | TooManyFailedLoginAttemptsException | TwoFactorAuthenticationFailedException | TwoFactorAuthenticationTokenInvalidException ex) {

				logger.info("Unable to login {}: {}", emailOrUsername, ex.getMessage());
				returnedMethodResult = new RestMethodResult(401, ex.getMessage());
				returnedMethodResult.addHeader("reason", ex.getReason());

			} catch (TwoFactorAuthenticationRequiredException ex) {

				returnedMethodResult = new RestMethodResult(202);
				returnedMethodResult.addHeader("token", ex.getNextStepToken());
				returnedMethodResult.addHeader("twoFactorLoginPage", Settings.TwoFactorLoginPage.getValue());

				if (ex.showQrCode()) {

					try {

						user            = ex.getUser();
						final Map<String, Object> hints = new HashMap();

						hints.put("MARGIN", 0);
						hints.put("ERROR_CORRECTION", "M");

						returnedMethodResult.addHeader("qrdata", Base64.getUrlEncoder().encodeToString(BarcodeFunction.getQRCode(Principal.getTwoFactorUrl(user), "QR_CODE", 200, 200, hints).getBytes("ISO-8859-1")));

					} catch (UnsupportedEncodingException uee) {
						logger.warn("Charset ISO-8859-1 not supported!?", uee);
					}
				}

				securityContext.getAuthenticator().doLogout(securityContext.getRequest());

			} catch (AuthenticationException ae) {

				logger.info("Invalid credentials for {}", emailOrUsername);
				returnedMethodResult = new RestMethodResult(401, ae.getMessage());
			}

			tx.success();
		}

		if (returnedMethodResult == null) {
			// should not happen
			throw new AuthenticationException(getErrorMessage());
		}

		// broadcast login to cluster for the user
		Services.getInstance().broadcastLogin(user);

		return returnedMethodResult;
	}

	@Override
	public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {
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

	@Override
	public boolean createPostTransaction() {
		return false;
	}

	protected Principal getUserForCredentials(SecurityContext securityContext, String emailOrUsername, String password, String twoFactorToken, String twoFactorCode, Map<String, Object> propertySet) throws FrameworkException {

		final String superUserName = Settings.SuperUserName.getValue();
		if (StringUtils.equals(superUserName, emailOrUsername)) {
			throw new AuthenticationException("login with superuser not supported.");
		}

		Principal user = null;

		user = getUserForTwoFactorTokenOrEmailOrUsername(twoFactorToken, emailOrUsername, password);

		if (user != null) {

			final boolean twoFactorAuthenticationSuccessOrNotNecessary = AuthHelper.handleTwoFactorAuthentication(user, twoFactorCode, twoFactorToken, ActionContext.getRemoteAddr(securityContext.getRequest()));

			if (twoFactorAuthenticationSuccessOrNotNecessary) {
				return user;
			}
		}

		return null;
	}

	protected Principal getUserForTwoFactorTokenOrEmailOrUsername(String twoFactorToken, String emailOrUsername, String password) throws FrameworkException {

		Principal user = null;

		if (StringUtils.isNotEmpty(twoFactorToken)) {

			user = AuthHelper.getUserForTwoFactorToken(twoFactorToken);

		} else if (StringUtils.isNotEmpty(emailOrUsername) && StringUtils.isNotEmpty(password)) {

			user = securityContext.getAuthenticator().doLogin(securityContext.getRequest(), emailOrUsername, password);
		}

		return user;
	}

	protected RestMethodResult doLogin(SecurityContext securityContext, Principal user) throws FrameworkException {

		AuthHelper.doLogin(securityContext.getRequest(), user);

		logger.info("Login successful: {}", user);

		RuntimeEventLog.login("Login successful", Map.of("user", user.getUuid(), "name", user.getName()));

		user.setSecurityContext(securityContext);

		// make logged in user available to caller
		securityContext.setCachedUser(user);

		return createRestMethodResult(user);
	}

	protected RestMethodResult createRestMethodResult(Principal user) {

		RestMethodResult  returnedMethodResult = new RestMethodResult(200);
		returnedMethodResult.addContent(user);

		return returnedMethodResult;
	}
}
