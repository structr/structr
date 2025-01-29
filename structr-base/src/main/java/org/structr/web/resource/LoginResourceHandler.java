/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.exception.*;
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.graph.Tx;
import org.structr.rest.RestMethodResult;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.auth.AuthHelper;
import org.structr.schema.action.ActionContext;
import org.structr.web.function.BarcodeFunction;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 */
public class LoginResourceHandler extends RESTCallHandler {

	protected static final Logger logger = LoggerFactory.getLogger(LoginResourceHandler.class.getName());

	public LoginResourceHandler(final RESTCall call) {
		super(call);
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	public String getErrorMessage() {
		return AuthHelper.STANDARD_ERROR_MSG;
	}

	@Override
	public RestMethodResult doPost(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {

		RestMethodResult returnedMethodResult = null;
		long userId = -1;

		try {

			final String username = (String) propertySet.get("name");
			final String email = (String) propertySet.get("eMail");
			final String password = (String) propertySet.get("password");
			final String twoFactorToken = (String) propertySet.get("twoFactorToken");
			final String twoFactorCode = (String) propertySet.get("twoFactorCode");

			String emailOrUsername = StringUtils.isNotEmpty(email) ? email : username;

			if (StringUtils.contains(emailOrUsername, "@")) {
				emailOrUsername = emailOrUsername.trim().toLowerCase();
			}

			final SecurityContext ctx = SecurityContext.getSuperUserInstance();
			final App app = StructrApp.getInstance(ctx);
			PrincipalInterface user = null;

			if (Settings.CallbacksOnLogin.getValue() == false) {
				ctx.disableInnerCallbacks();
			}

			try (final Tx tx = app.tx(true, true, true)) {

				try {

					user = getUserForCredentials(securityContext, emailOrUsername, password, twoFactorToken, twoFactorCode, propertySet);
					returnedMethodResult = doLogin(securityContext, user);

					userId = user.getNode().getId().getId();

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

							user = ex.getUser();
							final Map<String, Object> hints = new HashMap();

							hints.put("MARGIN", 0);
							hints.put("ERROR_CORRECTION", "M");

							returnedMethodResult.addHeader("qrdata", Base64.getUrlEncoder().encodeToString(BarcodeFunction.getQRCode(user.getTwoFactorUrl(), "QR_CODE", 200, 200, hints).getBytes("ISO-8859-1")));

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

		} catch (ClassCastException cce) {

			logger.info("Unable to process login data. All attributes must be or type String.");
			returnedMethodResult = new RestMethodResult(401, "Unable to process login data. All attributes must be of type String.");
		}

		if (returnedMethodResult == null) {
			// should not happen
			throw new AuthenticationException(getErrorMessage());
		}

		if (userId != -1) {
			// broadcast login to cluster for the user
			Services.getInstance().broadcastLogin(userId);
		}

		return returnedMethodResult;
	}

	@Override
	public Class getEntityClass(final SecurityContext securityContext) {
		return null;
	}

	@Override
	public boolean createPostTransaction() {
		return false;
	}

	@Override
	public Set<String> getAllowedHttpMethodsForOptionsCall() {
		return Set.of("OPTIONS", "POST");
	}

	// ----- protected methods -----
	protected PrincipalInterface getUserForCredentials(final SecurityContext securityContext, final String emailOrUsername, final String password, final String twoFactorToken, final String twoFactorCode, final Map<String, Object> propertySet) throws FrameworkException {

		final String superUserName = Settings.SuperUserName.getValue();
		if (StringUtils.equals(superUserName, emailOrUsername)) {
			throw new AuthenticationException("login with superuser not supported.");
		}

		PrincipalInterface user = null;

		user = getUserForTwoFactorTokenOrEmailOrUsername(securityContext, twoFactorToken, emailOrUsername, password);

		if (user != null) {

			final boolean twoFactorAuthenticationSuccessOrNotNecessary = AuthHelper.handleTwoFactorAuthentication(user, twoFactorCode, twoFactorToken, ActionContext.getRemoteAddr(securityContext.getRequest()));

			if (twoFactorAuthenticationSuccessOrNotNecessary) {
				return user;
			}
		}

		return null;
	}

	protected PrincipalInterface getUserForTwoFactorTokenOrEmailOrUsername(final SecurityContext securityContext, final String twoFactorToken, final String emailOrUsername, final String password) throws FrameworkException {

		PrincipalInterface user = null;

		if (StringUtils.isNotEmpty(twoFactorToken)) {

			user = AuthHelper.getUserForTwoFactorToken(twoFactorToken);

		} else {

			user = securityContext.getAuthenticator().doLogin(securityContext.getRequest(), emailOrUsername, password);
		}

		return user;
	}

	protected RestMethodResult doLogin(final SecurityContext securityContext, final PrincipalInterface user) throws FrameworkException {

		AuthHelper.doLogin(securityContext.getRequest(), user);

		logger.info("Login successful: {}", user);

		RuntimeEventLog.login("Login successful", Map.of("user", user.getUuid(), "name", user.getName()));

		user.setSecurityContext(securityContext);

		// make logged in user available to caller
		securityContext.setCachedUser(user);

		return createRestMethodResult(user);
	}

	protected RestMethodResult createRestMethodResult(final PrincipalInterface user) {

		RestMethodResult  returnedMethodResult = new RestMethodResult(200);
		returnedMethodResult.addContent(user);

		return returnedMethodResult;
	}
}
