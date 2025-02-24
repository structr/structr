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
package org.structr.rest.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.exception.*;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.schema.action.Actions;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class for authentication.
 */
public class AuthHelper {

	public static final String STANDARD_ERROR_MSG = "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";
	private static final Logger logger            = LoggerFactory.getLogger(AuthHelper.class.getName());


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

				final NodeInterface node = StructrApp.getInstance().nodeQuery(StructrTraits.PRINCIPAL).and(key, value).disableSorting().isPing(isPing).getFirst();
				if (node != null) {

					return node.as(Principal.class);
				}

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
	public static Principal getPrincipalForPassword(final PropertyKey<String> key, final String value, final String password) throws AuthenticationException, TooManyFailedLoginAttemptsException, PasswordChangeRequiredException {

		Principal principal  = null;

		final String superuserName = Settings.SuperUserName.getValue();
		final String superUserPwd  = Settings.SuperUserPassword.getValue();

		if (StringUtils.isEmpty(value)) {

			logger.info("Empty value for key {}", key.dbName());
			throw new AuthenticationException(STANDARD_ERROR_MSG);
		}

		if (StringUtils.isEmpty(password)) {

			logger.info("Empty password");
			throw new AuthenticationException(STANDARD_ERROR_MSG);
		}

		if (value.equals(superuserName) && password.equals(superUserPwd)) {

			principal = new SuperUser();

			RuntimeEventLog.login("Authenticate", Map.of("id", principal.getUuid(), "name", principal.getName()));

		} else {

			try {

				final NodeInterface node = StructrApp.getInstance().nodeQuery(StructrTraits.PRINCIPAL).and().or(key, value).or(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), value).disableSorting().getFirst();
				if (node != null) {

					principal = node.as(Principal.class);
				}

			} catch (FrameworkException fex) {

				logger.warn("", fex);
			}

			if (principal == null) {

				final String keyMessage = ("name".equals(key.dbName())) ? "name" : "name OR " + key.dbName();

				logger.info("No principal found for {} '{}'", keyMessage, value);

				RuntimeEventLog.failedLogin("No principal found", Map.of("keyMessage", keyMessage, "value", value));

				throw new AuthenticationException(STANDARD_ERROR_MSG);

			} else {

				if (principal.isBlocked()) {

					logger.info("Principal {} is blocked", principal);

					RuntimeEventLog.failedLogin("Principal is blocked", Map.of("id", principal.getUuid(), "name", principal.getName()));

					throw new AuthenticationException(STANDARD_ERROR_MSG);
				}

				try {

					// let Principal decide how to check password
					final boolean passwordValid = principal.isValidPassword(password);

					if (!passwordValid) {

						AuthHelper.incrementFailedLoginAttemptsCounter(principal);
					}

					AuthHelper.checkTooManyFailedLoginAttempts(principal);

					if (!passwordValid) {

						RuntimeEventLog.failedLogin("Wrong password", Map.of("id", principal.getUuid(), "name", principal.getName()));

						throw new AuthenticationException(STANDARD_ERROR_MSG);

					} else {

						AuthHelper.handleForcePasswordChange(principal);
						AuthHelper.resetFailedLoginAttemptsCounter(principal);

						// allow external users (LDAP etc.) to update group membership
						principal.onAuthenticate();

						RuntimeEventLog.login("Authenticate", Map.of("id", principal.getUuid(), "name", principal.getName()));
					}

				} catch (DeleteInvalidUserException iuex) {

					// we need to delete the user in a separate transaction
					new Thread(() -> {

						final App app     = StructrApp.getInstance();
						final String uuid = iuex.getUuid();

						// delete user, return null
						if (uuid != null) {

							try (final Tx tx = app.tx()) {

								try {
									final NodeInterface toDelete = app.getNodeById(uuid);
									if (toDelete != null) {

										app.delete(toDelete);
									}

								} catch (FrameworkException fex) {
									fex.printStackTrace();
								}

								tx.success();

							} catch (FrameworkException fex) {
								logger.warn("Unable to delete user {}: {}", uuid, fex.getMessage());
							}

						} else {

							logger.warn("Unable to delete user {}, not found", uuid);
						}

					}).start();

					return null;
				}
			}
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

		return getPrincipalForCredential(Traits.of(StructrTraits.PRINCIPAL).key("sessionIds"), new String[]{ sessionId }, isPing);

	}

	public static void doLogin(final HttpServletRequest request, final Principal user) throws FrameworkException {

		if (request.getSession(false) == null) {
			SessionHelper.newSession(request);
		}

		SessionHelper.clearInvalidSessions(user);

		// We need a session to login a user
		final HttpSession session = request.getSession(false);
		if (session != null) {

			final String sessionId = session.getId();

			SessionHelper.clearSession(sessionId);

			if (user.addSessionId(sessionId)) {

				AuthHelper.updateLastLoginDate(user);
				AuthHelper.sendLoginNotification(user, request);

			} else {

				SessionHelper.clearSession(sessionId);
				SessionHelper.invalidateSession(sessionId);

				RuntimeEventLog.failedLogin("Max. number of sessions exceeded", Map.of("id", user.getUuid(), "name", user.getName()));
				throw new SessionLimitExceededException();
			}
		}
	}

	public static void doLogout(final HttpServletRequest request, final Principal user) throws FrameworkException {

		final String sessionId = SessionHelper.getShortSessionId(request.getRequestedSessionId());

		if (sessionId == null) return;

		SessionHelper.clearSession(sessionId);
		SessionHelper.invalidateSession(sessionId);

		// clear all refreshTokens on logout
		user.clearTokens();

		RuntimeEventLog.logout("Logout", Map.of("id", user.getUuid(), "name", user.getName()));

		AuthHelper.sendLogoutNotification(user, request);
	}

	public static void updateLastLoginDate(final Principal user) throws FrameworkException {

		try {

			user.setLastLoginDate(new Date());

		} catch (FrameworkException fex) {

			logger.warn("Exception while updating last login date", fex);
		}
	}

	public static void sendLoginNotification (final Principal user, final HttpServletRequest request) throws FrameworkException {

		try {

			final Map<String, Object> params = new HashMap<>();
			params.put("user", user);

			Actions.callAsSuperUser(Actions.NOTIFICATION_LOGIN, params, request);

		} catch (UnlicensedScriptException ex) {
			ex.log(logger);
		}
	}

	public static void sendLogoutNotification (final Principal user, final HttpServletRequest request) throws FrameworkException {

		try {

			final Map<String, Object> params = new HashMap<>();
			params.put("user", user);

			Actions.callAsSuperUser(Actions.NOTIFICATION_LOGOUT, params, request);

		} catch (UnlicensedScriptException ex) {
				ex.log(logger);
		}
	}

	/**
	 * @return A confirmation key with the current timestamp
	 */
	public static String getConfirmationKey() {

		return UUID.randomUUID().toString() + "!" + new Date().getTime();
	}

	/**
	 * Determines if the key is valid or not. If the key has no timestamp the configuration setting for keys without timestamp is used
	 *
	 * @param confirmationKey The confirmation key to check
	 * @param validityPeriod The validity period for the key (in minutes)
	 * @return
	 */
	public static boolean isConfirmationKeyValid(final String confirmationKey, final Integer validityPeriod) {

		final String[] parts = confirmationKey.split("!");

		if (parts.length == 2) {

			final long confirmationKeyCreated = Long.parseLong(parts[1]);
			final long maxValidity            = confirmationKeyCreated + validityPeriod * 60 * 1000;

			return (maxValidity >= new Date().getTime());
		}

		return Settings.ConfirmationKeyValidWithoutTimestamp.getValue();
	}

	public static void incrementFailedLoginAttemptsCounter (final Principal principal) {

		try {

			Integer failedAttempts = principal.getPasswordAttempts();

			if (failedAttempts == null) {
				failedAttempts = 0;
			}

			failedAttempts++;

			principal.setPasswordAttempts(failedAttempts);

		} catch (FrameworkException fex) {

			logger.warn("Exception while incrementing failed login attempts counter", fex);
		}
	}

	public static void checkTooManyFailedLoginAttempts (final Principal principal) throws TooManyFailedLoginAttemptsException {

		final int maximumAllowedFailedAttempts = Settings.PasswordAttempts.getValue();

		if (maximumAllowedFailedAttempts > 0) {

			Integer failedAttempts = principal.getPasswordAttempts();

			if (failedAttempts == null) {
				failedAttempts = 0;
			}

			if (failedAttempts > maximumAllowedFailedAttempts) {

				RuntimeEventLog.failedLogin("Too many login attempts", Map.of(
					"id", principal.getUuid(),
					"name", principal.getName(),
					"failedAttempts", failedAttempts,
					"maxAttempts", maximumAllowedFailedAttempts
				));

				throw new TooManyFailedLoginAttemptsException();
			}
		}
	}

	public static void resetFailedLoginAttemptsCounter (final Principal principal) {

		try {

			principal.setPasswordAttempts(0);

		} catch (FrameworkException fex) {

			logger.warn("Exception while resetting failed login attempts counter", fex);
		}
	}

	public static void handleForcePasswordChange (final Principal principal) throws PasswordChangeRequiredException {

		final boolean forcePasswordChange = Settings.PasswordForceChange.getValue();

		if (forcePasswordChange) {

			final int passwordDays = Settings.PasswordForceChangeDays.getValue();

			final Date now                = new Date();
			final Date passwordChangeDate = (principal.getPasswordChangeDate() != null) ? principal.getPasswordChangeDate() : new Date (0); // setting date in past if not yet set
			final int daysApart           = (int) ((now.getTime() - passwordChangeDate.getTime()) / (1000 * 60 * 60 * 24l));

			if (daysApart > passwordDays) {

				throw new PasswordChangeRequiredException();
			}
		}
	}

	public static Principal getUserForTwoFactorToken (final String twoFactorIdentificationToken) throws TwoFactorAuthenticationTokenInvalidException, FrameworkException {

		final App app = StructrApp.getInstance();

		Principal principal = null;

		final PropertyKey<String> twoFactorTokenKey = Traits.of(StructrTraits.PRINCIPAL).key("twoFactorToken");

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.nodeQuery(StructrTraits.PRINCIPAL).and(twoFactorTokenKey, twoFactorIdentificationToken).getFirst();
			if (node != null) {

				principal = node.as(Principal.class);
			}

			tx.success();
		}

		if (principal != null) {

			if (!AuthHelper.isTwoFactorTokenValid(twoFactorIdentificationToken)) {

				principal.setTwoFactorToken(null);

				RuntimeEventLog.failedLogin("Two factor authentication token not valid anymore", Map.of("id", principal.getUuid(), "name", principal.getName()));

				throw new TwoFactorAuthenticationTokenInvalidException();
			}
		}

		return principal;
	}

	public static boolean isTwoFactorTokenValid(final String twoFactorIdentificationToken) {

		final String[] parts = twoFactorIdentificationToken.split("!");

		if (parts.length == 2) {

			final long tokenCreatedTimestamp = Long.parseLong(parts[1]);
			final long maxTokenValidity      = tokenCreatedTimestamp + Settings.TwoFactorLoginTimeout.getValue() * 1000;

			return (maxTokenValidity >= new Date().getTime());
		}

		return false;
	}

	public static boolean isRequestingIPWhitelistedForTwoFactorAuthentication(final String requestIP, final String whitelistEntries) {

		if (!StringUtils.isEmpty(requestIP) && !StringUtils.isEmpty(whitelistEntries)) {

			try {

				final InetAddress requestAddress     = InetAddress.getByName(requestIP);
				final String[] splitWhitelistEntries = whitelistEntries.split("[ ,]+");

				final BigInteger maxIP;
				final int maxPrefix;
				boolean isIPv6 = false;
				boolean isIPv4 = false;

				if (requestAddress instanceof Inet6Address) {

					isIPv6    = true;
					maxIP     = new BigInteger("ffffffffffffffffffffffffffffffff", 16);
					maxPrefix = 128;

				} else if (requestAddress instanceof Inet4Address) {

					isIPv4    = true;
					maxIP     = new BigInteger("ffffffff", 16);
					maxPrefix = 32;

				} else {

					return false;
				}

				for (final String wlEntry : splitWhitelistEntries) {

					final String[] wlEntryParts = wlEntry.split("/");

					try {

						final InetAddress wlAddress = InetAddress.getByName(wlEntryParts[0]);

						if (wlAddress instanceof Inet4Address && isIPv4 || wlAddress instanceof Inet6Address && isIPv6) {

							int prefixLength = maxPrefix;
							if (wlEntryParts.length == 2) {
								try {
									final int definedPrefix = Integer.parseInt(wlEntryParts[1]);

									if (definedPrefix > 0) {
										prefixLength = definedPrefix;
									} else {
										logger.warn("Prefix length for '{}' is invalid, using most restrictive value {}", wlEntry, maxPrefix);
									}

								} catch (NumberFormatException nfe) {
									logger.warn("Unable to parse numeric prefix length for '{}', using most restrictive value {}", wlEntry, maxPrefix);
								}
							}

							final BigInteger prefixMask          = maxIP.shiftLeft(maxPrefix - prefixLength).and(maxIP);
							final BigInteger wildcard            = prefixMask.xor(maxIP);
							final BigInteger requestIPAsBigInt   = new BigInteger(1, requestAddress.getAddress());
							final BigInteger whiteListIpAsBigInt = new BigInteger(1, wlAddress.getAddress());
							final BigInteger lowerBound          = whiteListIpAsBigInt.and(prefixMask);
							final BigInteger upperBound          = lowerBound.or(wildcard);

							final boolean myIp_GTE_lowerBound    = (-1 != requestIPAsBigInt.compareTo(lowerBound));
							final boolean myIp_LTE_upperBound    = (-1 != upperBound.compareTo(requestIPAsBigInt));

							if (myIp_GTE_lowerBound && myIp_LTE_upperBound) {
								return true;
							}
						}

					} catch (UnknownHostException uhe) {

						logger.warn("Unable to parse whitelist entry '{}': {}", wlEntryParts[0], uhe.getMessage());
					}
				}

			} catch (UnknownHostException uhe) {

				logger.warn("Unable to parse request IP address '{}': {}", requestIP, uhe.getMessage());
			}
		}

		return false;
	}

	public static boolean handleTwoFactorAuthentication (final Principal principal, final String twoFactorCode, final String twoFactorToken, final String requestIP) throws FrameworkException, TwoFactorAuthenticationRequiredException, TwoFactorAuthenticationFailedException {

		if (!AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication(requestIP, Settings.TwoFactorWhitelistedIPs.getValue())) {

			final int twoFactorLevel   = Settings.TwoFactorLevel.getValue();
			boolean isTwoFactorUser    = principal.isTwoFactorUser();
			boolean twoFactorConfirmed = principal.isTwoFactorConfirmed();

			boolean userNeedsTwoFactor = twoFactorLevel == 2 || (twoFactorLevel == 1 && isTwoFactorUser == true);

			if (userNeedsTwoFactor) {

				if (twoFactorToken == null) {

					// user just logged in via username/password - no two factor identification token

					final String newTwoFactorToken = AuthHelper.getIdentificationTokenForPrincipal();
					principal.setTwoFactorToken(newTwoFactorToken);

					throw new TwoFactorAuthenticationRequiredException(principal, newTwoFactorToken, !twoFactorConfirmed);

				} else {

					try {

						final String currentKey = TimeBasedOneTimePasswordHelper.generateCurrentNumberString(principal.getTwoFactorSecret(), AuthHelper.getCryptoAlgorithm(), Settings.TwoFactorPeriod.getValue(), Settings.TwoFactorDigits.getValue());

						// check two factor authentication
						if (currentKey.equals(twoFactorCode)) {

							principal.setTwoFactorToken(null);   // reset token
							principal.setTwoFactorConfirmed(true);   // user has verified two factor use
							principal.setIsTwoFactorUser(true);

							logger.info("Successful two factor authentication ({})", principal.getName());

							RuntimeEventLog.login("Two factor authentication successful", Map.of("id", principal.getUuid(), "name", principal.getName()));

							return true;

						} else {

							// two factor authentication not successful
						   logger.info("Two factor authentication failed ({})", principal.getName());

						   RuntimeEventLog.failedLogin("Two factor authentication failed", Map.of("id", principal.getUuid(), "name", principal.getName()));

						   throw new TwoFactorAuthenticationFailedException();
						}

					} catch (GeneralSecurityException ex) {

						logger.warn("Two factor authentication key could not be generated - login not possible");

						return false;
					}
				}
			}
		}

		return true;
	}

	public static String getIdentificationTokenForPrincipal () {
		return UUID.randomUUID().toString() + "!" + new Date().getTime();
	}

	// The StandardName for the given SHA algorithm.
	// see https://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Mac
	private static String getCryptoAlgorithm() {
		return "Hmac" + Settings.TwoFactorAlgorithm.getValue();
	}
}
