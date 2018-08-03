/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.rest.auth;

import java.util.Date;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.Actions;


/**
 * Utility class for authentication
 *
 *
 */
public class AuthHelper {

	public static final String STANDARD_ERROR_MSG = "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";

	private static final Logger logger            = LoggerFactory.getLogger(AuthHelper.class.getName());

	//~--- get methods ----------------------------------------------------

	/**
	 * Find a {@link Principal} for the given credential
	 *
	 * @param key
	 * @param value
	 * @return principal
	 */
	public static <T> Principal getPrincipalForCredential(final PropertyKey<T> key, final T value) {

		return getPrincipalForCredential(key, value, false);

	}

	public static <T> Principal getPrincipalForCredential(final PropertyKey<T> key, final T value, final boolean isPing) {

		if (value != null) {

			try {

				return StructrApp.getInstance().nodeQuery(Principal.class).and(key, value).disableSorting().isPing(isPing).getFirst();

			} catch (FrameworkException fex) {

				logger.warn("Error while searching for principal: {}", fex.getMessage());
			}

		}

		return null;
	}

	/**
	 * Find a {@link Principal} with matching password and given key or name
	 *
	 * @param key
	 * @param value
	 * @param password
	 * @return principal
	 * @throws AuthenticationException
	 */
	public static Principal getPrincipalForPassword(final PropertyKey<String> key, final String value, final String password) throws AuthenticationException {

		String errorMsg = null;
		Principal principal  = null;

		final String superuserName = Settings.SuperUserName.getValue();
		final String superUserPwd  = Settings.SuperUserPassword.getValue();

		if (StringUtils.isEmpty(value)) {

			logger.info("Empty value for key {}", key);
			errorMsg = STANDARD_ERROR_MSG;

		}

		if (StringUtils.isEmpty(password)) {

			logger.info("Empty password");
			errorMsg = STANDARD_ERROR_MSG;

		}

		if (superuserName.equals(value) && superUserPwd.equals(password)) {

			// logger.info("############# Authenticated as superadmin! ############");

			principal = new SuperUser();

		} else if (errorMsg == null) { // no error so far

			try {

				principal = StructrApp.getInstance().nodeQuery(Principal.class).and().or(key, value).or(AbstractNode.name, value).disableSorting().getFirst();

				if (principal == null) {

					logger.info("No principal found for {} {}", new Object[]{ key.dbName(), value });

					errorMsg = STANDARD_ERROR_MSG;

				} else {

					if (principal.isBlocked()) {

						logger.info("Principal {} is blocked", principal);

						errorMsg = STANDARD_ERROR_MSG;

					}

					if (StringUtils.isEmpty(password)) {

						logger.info("Empty password for principal {}", principal);

						errorMsg = "Empty password, should never happen here!";

					} else {

						// let Principal decide how to check password
						if (!principal.isValidPassword(password)) {

							errorMsg = STANDARD_ERROR_MSG;
						}

					}

				}

			} catch (FrameworkException fex) {

				logger.warn("", fex);

			}

		}

		if (errorMsg != null) {

			throw new AuthenticationException(errorMsg);
		}

		return principal;

	}

	/**
	 * Find a {@link Principal} for the given session id
	 *
	 * @param sessionId
	 * @return principal
	 */
	public static Principal getPrincipalForSessionId(final String sessionId) {

		return getPrincipalForSessionId(sessionId, false);

	}

	public static Principal getPrincipalForSessionId(final String sessionId, final boolean isPing) {

		return getPrincipalForCredential(StructrApp.key(Principal.class, "sessionIds"), new String[]{ sessionId }, isPing);

	}

	public static void doLogin(final HttpServletRequest request, final Principal user) throws FrameworkException {

		if (request.getSession(false) == null) {
			SessionHelper.newSession(request);
		}

		SessionHelper.clearInvalidSessions(user);

		// We need a session to login a user
		if (request.getSession(false) != null) {

			final String sessionId = request.getSession(false).getId();

			SessionHelper.clearSession(sessionId);
			user.addSessionId(sessionId);

			try {

				Actions.call(Actions.NOTIFICATION_LOGIN, user);

			} catch (UnlicensedScriptException ex) {
				ex.log(logger);
			}
		}
	}

	public static void doLogout(final HttpServletRequest request, final Principal user) throws FrameworkException {

		final String sessionId = SessionHelper.getShortSessionId(request.getRequestedSessionId());

		if (sessionId == null) return;

		SessionHelper.clearSession(sessionId);
		SessionHelper.invalidateSession(sessionId);

		AuthHelper.sendLogoutNotification(user);
	}

	public static void sendLogoutNotification (final Principal user) throws FrameworkException {

		try {

			Actions.call(Actions.NOTIFICATION_LOGOUT, user);

		} catch (UnlicensedScriptException ex) {
				ex.log(logger);
		}
	}

	/**
	 * @return A confirmation key with the current timestamp
	 */
	public static String getConfirmationKey() {

		return UUID.randomUUID().toString() + "|" + new Date().getTime();
	}

	/**
	 * Determines if the key is valid or not. If the key has no timestamp the configuration setting for keys without timestamp is used
	 *
	 * @param confirmationKey The confirmation key to check
	 * @param validityPeriod The validity period for the key (in minutes)
	 * @return
	 */
	public static boolean isConfirmationKeyValid(final String confirmationKey, final Integer validityPeriod) {

		final String[] parts = confirmationKey.split("\\|");

		if (parts.length == 2) {

			final long timeStamp = Long.parseLong(parts[1]);
			final long endOfPeriod = new Date().getTime() + validityPeriod * 60 * 1000;

			return (timeStamp <= endOfPeriod);
		}

		return Settings.ConfirmationKeyValidWithoutTimestamp.getValue();
	}

}
