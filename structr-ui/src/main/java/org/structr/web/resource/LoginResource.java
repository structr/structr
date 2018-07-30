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

import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.rest.RestMethodResult;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.resource.Resource;
import org.structr.web.entity.User;

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

		final PropertyMap properties       = PropertyMap.inputTypeToJavaType(securityContext, User.class, propertySet);
		final PropertyKey<String> eMailKey = StructrApp.key(User.class, "eMail");
		final PropertyKey<String> pwdKey   = StructrApp.key(User.class, "password");

		final PropertyKey<String> twoFactorTokenKey   = StructrApp.key(User.class, "twoFactorToken");
		final PropertyKey<String> twoFactorCodeKey    = StructrApp.key(User.class, "twoFactorCode");
		final PropertyKey<Boolean> isTwoFactorUserKey = StructrApp.key(User.class, "isTwoFactorUser");

		final PropertyKey<Integer> passwordAttemptsKey = StructrApp.key(User.class, "passwordAttempts");
		final PropertyKey<Date> passwordChangeDateKey  = StructrApp.key(User.class, "passwordChangeDate");

		final String name     = properties.get(User.name);
		final String email    = properties.get(eMailKey);
		final String password = properties.get(pwdKey);

		final String twoFactorCode             = (properties.get(twoFactorCodeKey) == null) ? null : properties.get(twoFactorCodeKey).replaceAll("\\s+","");
		final int twoFactorLevel               = Settings.TwoFactorLevel.getValue();
		final String twoFactorTokenFromRequest = properties.get(twoFactorTokenKey);

		final int maximumAttempts         = Settings.PasswordAttempts.getValue();
		final int passwordDays            = Settings.PasswordForceChangeDays.getValue();
		final boolean forcePasswordChange = Settings.PasswordForceChange.getValue();

		final App app = StructrApp.getInstance();

		final String emailOrUsername = StringUtils.isNotEmpty(email) ? email : name;

		if ((StringUtils.isNotEmpty(emailOrUsername) && StringUtils.isNotEmpty(password)) || StringUtils.isNotEmpty(twoFactorTokenFromRequest)) {

			Principal user = null;

			// If there is no token get user by username/ pw, else get user by token
			if (twoFactorTokenFromRequest != null) {

				Result<Principal> results;
				try (final Tx tx = app.tx()) {
					results = app.nodeQuery(Principal.class).and(twoFactorTokenKey, twoFactorTokenFromRequest).getResult();
					tx.success();
				}

				if (!results.isEmpty()) {

					user = results.get(0);
					try (final Tx tx = app.tx()) {
						tx.success();
					}
				}

			} else {

				try {
					user = securityContext.getAuthenticator().doLogin(securityContext.getRequest(), emailOrUsername, password);
				} catch (Exception e) {
					user = null;
				}
			}

			if (user != null) {

				int attempts = user.getProperty(passwordAttemptsKey) == null ? 0 : user.getProperty(passwordAttemptsKey);

				if (attempts > maximumAttempts && maximumAttempts > 0) {

					securityContext.getAuthenticator().doLogout(securityContext.getRequest());
					logger.info("Too many login-attempts for user: {}", emailOrUsername);

					RestMethodResult methodResult = new RestMethodResult(401);
					methodResult.addHeader("reason", "attempts");

					return methodResult;
				}

				// reset password attempts
				user.setProperty(passwordAttemptsKey, 0);

				if (forcePasswordChange) {

					final Date now = new Date();
					final Date passwordChangeDate = (user.getProperty(passwordChangeDateKey) != null) ? user.getProperty(passwordChangeDateKey) : new Date (0); // setting date in past if not yet set
					final int daysApart = (int) ((now.getTime() - passwordChangeDate.getTime()) / (1000 * 60 * 60 * 24l));

					if (daysApart > passwordDays) {
						// User has not changed password for too long
						securityContext.getAuthenticator().doLogout(securityContext.getRequest());
						logger.info("The password has not been changed by user: {}", emailOrUsername);

						RestMethodResult methodResult = new RestMethodResult(401);
						methodResult.addHeader("reason", "changed");

						return methodResult;
					}
				}

				boolean isTwoFactorUser = user.getProperty(isTwoFactorUserKey);
				// If System and user are two factor authentication
				boolean isTwoFactor = (isTwoFactorUser == true && twoFactorLevel > 0);

				// If user is not two factor authentication, but system expects him to be
				if (isTwoFactorUser == false && twoFactorLevel == 2) {

					logger.info("User needs to use two factor authentication to login");
					user.setProperty(isTwoFactorUserKey, true);

					RestMethodResult methodResult = new RestMethodResult(204);
					methodResult.addHeader("forceRegistrationPage", Settings.TwoFactorForceRegistrationPage.getValue());
					methodResult.addHeader("qrdata",                Base64.getUrlEncoder().encodeToString(User.getTwoFactorUrl((User) user).getBytes(Charset.forName("ISO-8859-1"))));

					securityContext.getAuthenticator().doLogout(securityContext.getRequest());

					return methodResult;
				}


				if (isTwoFactor) {

					if (twoFactorTokenFromRequest == null) {

						// set token to identify user by it
						final String newTwoFactorToken = UUID.randomUUID().toString();
						user.setProperty(twoFactorTokenKey, newTwoFactorToken);

						RestMethodResult methodResult = new RestMethodResult(202);
						methodResult.addHeader("token", newTwoFactorToken);
						methodResult.addHeader("twoFactorLoginPage", Settings.TwoFactorLoginPage.getValue());

						securityContext.getAuthenticator().doLogout(securityContext.getRequest());

						return methodResult;

					} else {

						final String twoFactorSecret = user.getProperty(StructrApp.key(User.class, "twoFactorSecret"));
						String currentKey = "";

						try {
							currentKey = TimeBasedOneTimePasswordUtil.generateCurrentNumberString(twoFactorSecret);
						} catch (GeneralSecurityException ex) {
							logger.info("Two factor authentication key could not be generated");
						}

						// check two factor authentication
						if (currentKey.equals(twoFactorCode)) {

							user.setProperty(twoFactorTokenKey, null); // reset token
							AuthHelper.doLogin(securityContext.getRequest(), user);
							logger.info ("Succesful two factor authentication");

						} else {

							// two factor authentication not successful
						   logger.info("Two factor authentication failed");

						   RestMethodResult methodResult = new RestMethodResult(401);
						   methodResult.addHeader("reason", "twofactor");

						   return methodResult;

						}
					}
				}

				logger.info("Login successful: {}", new Object[]{user});

				// make logged in user available to caller
				securityContext.setCachedUser(user);

				RestMethodResult methodResult = new RestMethodResult(200);
				methodResult.addContent(user);

				return methodResult;

			} else {

				if (maximumAttempts > 0) {

					Result<Principal> results;
					try (final Tx tx = app.tx()) {
						results = app.nodeQuery(Principal.class).and(StructrApp.key(User.class, "name"), emailOrUsername).getResult();
						tx.success();
					}

					if (!results.isEmpty()) {

						user = results.get(0);
						try (final Tx tx = app.tx()) {
							tx.success();
						}

					} else {

						try (final Tx tx = app.tx()) {
							results = app.nodeQuery(Principal.class).and(StructrApp.key(User.class, "eMail"), emailOrUsername).getResult();
							tx.success();
						}

						if (!results.isEmpty()) {

							user = results.get(0);
							try (final Tx tx = app.tx()) {
								tx.success();
							}
						}
					}

					if (user != null) {
						int attempts = user.getProperty(passwordAttemptsKey) == null ? 0 : user.getProperty(passwordAttemptsKey);

						if (attempts <= maximumAttempts) {

							user.setProperty(passwordAttemptsKey, ++attempts);

						} else {

							//user.setProperty(StructrApp.key(User.class, "blocked"), true);
							logger.info("Too many login-attempts for user: {}", emailOrUsername);

							RestMethodResult methodResult = new RestMethodResult(401);
							methodResult.addHeader("reason", "attempts");

							return methodResult;
						}
					}
				}
			}
		}

		logger.info("Invalid credentials (name, email, password): {}, {}, {}", new Object[]{name, email, password});

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
