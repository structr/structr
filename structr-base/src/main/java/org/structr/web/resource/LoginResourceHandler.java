/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.graph.Identity;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.exception.*;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.rest.RestMethodResult;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.auth.DeviceTrustHelper;
import org.structr.schema.action.ActionContext;

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
		Identity<?>      userId               = null;

		try {

			final String username       = (String) propertySet.get(NodeInterfaceTraitDefinition.NAME_PROPERTY);
			final String email          = (String) propertySet.get(PrincipalTraitDefinition.EMAIL_PROPERTY);
			final String password       = (String) propertySet.get(PrincipalTraitDefinition.PASSWORD_PROPERTY);
			final String twoFactorToken = (String) propertySet.get(PrincipalTraitDefinition.TWO_FACTOR_TOKEN_PROPERTY);
			final String twoFactorCode  = (String) propertySet.get("twoFactorCode");
			String emailOrUsername = StringUtils.isNotEmpty(email) ? email : username;

			if (StringUtils.contains(emailOrUsername, "@")) {

				emailOrUsername = emailOrUsername.trim().toLowerCase();
			}

			final SecurityContext ctx = SecurityContext.getSuperUserInstance();
			final App app = StructrApp.getInstance(ctx);
			Principal user = null;

			if (!Settings.CallbacksOnLogin.getValue()) {

				ctx.disableInnerCallbacks();
			}

			try (final Tx tx = app.tx(true, true, true)) {

				try {

					user = getUserForCredentials(securityContext, emailOrUsername, password, twoFactorToken, twoFactorCode, propertySet);
					returnedMethodResult = doLogin(securityContext, user);

					userId = user.getNode().getId();

				} catch (PasswordChangeRequiredException | TooManyFailedLoginAttemptsException | TwoFactorAuthenticationFailedException | TwoFactorAuthenticationTokenInvalidException ex) {

					logger.info("Unable to login {}: {}", emailOrUsername, ex.getMessage());
					returnedMethodResult = new RestMethodResult(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
					returnedMethodResult.addHeader("reason", ex.getReason());

				} catch (final TwoFactorAuthenticationRequiredException ex) {

					returnedMethodResult = new RestMethodResult(HttpServletResponse.SC_ACCEPTED);
					returnedMethodResult.addHeaders(ex.getData());

					securityContext.getAuthenticator().doLogout(securityContext.getRequest());

				} catch (AuthenticationException ae) {

					logger.info("Invalid credentials for {}", emailOrUsername);
					returnedMethodResult = new RestMethodResult(HttpServletResponse.SC_UNAUTHORIZED, ae.getMessage());
				}

				tx.success();
			}

		} catch (ClassCastException cce) {

			logger.info("Unable to process login data. All attributes must be or type String.");
			returnedMethodResult = new RestMethodResult(HttpServletResponse.SC_UNAUTHORIZED, "Unable to process login data. All attributes must be of type String.");
		}

		if (returnedMethodResult == null) {

			// should not happen
			throw new AuthenticationException(getErrorMessage());
		}

		if (userId != null) {

			// broadcast login to cluster for the user
			Services.getInstance().broadcastLogin(userId.hash());
		}

		return returnedMethodResult;
	}

	@Override
	public String getTypeName(final SecurityContext securityContext) {

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
	protected Principal getUserForCredentials(final SecurityContext securityContext, final String emailOrUsername, final String password, final String twoFactorToken, final String twoFactorCode, final Map<String, Object> propertySet) throws FrameworkException {

		final String superUserName = Settings.SuperUserName.getValue();
		if (StringUtils.equals(superUserName, emailOrUsername)) {

			throw new AuthenticationException("login with superuser not supported.");
		}

		final Principal user = getUserForTwoFactorTokenOrEmailOrUsername(securityContext, twoFactorToken, emailOrUsername, password);

		if (user != null) {

			final HttpServletRequest request = securityContext.getRequest();
			final boolean userRequestedTrust = propertySet.containsKey(DeviceTrustHelper.DEVICE_TRUST_REQUESTED_STRING) && (boolean) propertySet.get(DeviceTrustHelper.DEVICE_TRUST_REQUESTED_STRING);
			final String userAgentString     = request.getHeader(HttpHeader.USER_AGENT.asString());

			final AuthHelper.TwoFactorAuthenticationResult result = AuthHelper.handleTwoFactorAuthentication(user, twoFactorCode, twoFactorToken, ActionContext.getRemoteAddr(request), userAgentString, AuthHelper.getDeviceTrustCookie(request));

			if (result != AuthHelper.TwoFactorAuthenticationResult.FAILURE) {

				// only set trust cookie if actual two-factor authentication was used
				if (result == AuthHelper.TwoFactorAuthenticationResult.SUCCESS && userRequestedTrust) {

					if (Settings.TwoFactorDeviceTrustEnabled.getValue()) {

						logger.info("Two factor authentication: User '{}' requested trust, setting trust cookie", user.getName());

						AuthHelper.addDeviceTrustCookie(securityContext, userAgentString, user.getDeviceTrustSecret());

					} else {

						logger.info("Two factor authentication: User '{}' requested trust, but feature is disabled ({})", user.getName(), Settings.TwoFactorDeviceTrustEnabled.getKey());
					}
				}

				return user;
			}
		}

		return null;
	}

	protected Principal getUserForTwoFactorTokenOrEmailOrUsername(final SecurityContext securityContext, final String twoFactorToken, final String emailOrUsername, final String password) throws FrameworkException {

		Principal user = null;

		if (StringUtils.isNotEmpty(twoFactorToken)) {

			user = AuthHelper.getUserForTwoFactorToken(twoFactorToken);

		} else {

			user = securityContext.getAuthenticator().doLogin(securityContext.getRequest(), emailOrUsername, password);
		}

		return user;
	}

	protected RestMethodResult doLogin(final SecurityContext securityContext, final Principal user) throws FrameworkException {

		AuthHelper.doLogin(securityContext.getRequest(), user);

		logger.info("Login successful: {}", user);

		RuntimeEventLog.login("Login successful", Map.of("user", user.getUuid(), "name", user.getName()));

		user.setSecurityContext(securityContext);

		// make logged in user available to caller
		securityContext.setCachedUser(user);

		return createRestMethodResult(user);
	}

	protected RestMethodResult createRestMethodResult(final Principal user) {

		RestMethodResult  returnedMethodResult = new RestMethodResult(200);
		returnedMethodResult.addContent(user);

		return returnedMethodResult;
	}
}
