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
package org.structr.rest.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.common.LogThrottle;
import org.structr.core.auth.HashHelper;
import org.structr.core.auth.exception.*;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.schema.action.Actions;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility class for authentication.
 */
public class AuthHelper {

	public static final String STANDARD_ERROR_MSG = "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";
	private static final Logger logger            = LoggerFactory.getLogger(AuthHelper.class.getName());

	/* Every failed sign-in is logged, and an attacker decides how many to attempt: unthrottled, a
	   credential-guessing run writes one line per attempt. Keyed by the REASON, so each distinct
	   cause is reported once per window rather than once per attempt - which also keeps attempted
	   user names out of the log in bulk. The per-event detail is still recorded in the runtime event
	   log, which is bounded and inspectable in the user interface. */
	private static final LogThrottle failedLoginLog = new LogThrottle("Failed login", 1);

	// Per-user lock objects for atomic failed login counter updates
	private static final ConcurrentHashMap<String, Object> userLocks = new ConcurrentHashMap<>();

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

		return getPrincipalForCredential(key, value, isPing, true);
	}

	public static <T> Principal getPrincipalForCredential(final PropertyKey<T> key, final T value, final boolean isPing, final boolean isExact) {

		if (value != null) {

			try {

				final NodeInterface node = StructrApp.getInstance().nodeQuery(StructrTraits.PRINCIPAL).key(key, value, isExact).disableSorting().isPing(isPing).getFirst();
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
	 * Find a {@link Principal} with matching password and one of the given keys or name
	 *
	 * @param keys
	 * @param value
	 * @param password
	 * @return
	 * @throws FrameworkException
	 */
	public static Principal getPrincipalForKeysAndPassword(final LinkedHashSet<PropertyKey<String>> keys, final String value, final String password) throws FrameworkException {

		return getPrincipalForPassword(keys, value, password);
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

		return getPrincipalForPassword(Set.of(key), value, password);
	}

	/**
	 * Finds the principal whose value for one of the keys, or whose name, equals the given value, and
	 * checks the password against that account. One lookup and one check, however many keys: trying the
	 * keys one after another looked the same account up by name every time, verified the same wrong
	 * password once per key and counted each of those as a failed attempt (ticket 1544).
	 */
	public static Principal getPrincipalForPassword(final Set<PropertyKey<String>> keys, final String value, final String password) throws AuthenticationException, TooManyFailedLoginAttemptsException, PasswordChangeRequiredException {

		Principal principal  = null;
		final String superuserName = Settings.SuperUserName.getValue();
		final String superUserPwd  = Settings.SuperUserPassword.getValue();

		if (StringUtils.isEmpty(value)) {

			if (failedLoginLog.allow("empty value")) {

				logger.info("Empty value for {}", describeLookupKeys(keys));
			}

			throw new AuthenticationException(STANDARD_ERROR_MSG);
		}

		if (StringUtils.isEmpty(password)) {

			if (failedLoginLog.allow("empty password")) {

				logger.info("Empty password");
			}

			throw new AuthenticationException(STANDARD_ERROR_MSG);
		}

		/* An unconfigured superuser password must not be usable, and it must not be distinguishable
		   from a wrong one. Without the emptiness check the comparison throws on a null password,
		   and the resulting error response tells an attacker that the name just guessed is the
		   superuser name, whatever it was renamed to. Treat "no password set" exactly as an empty
		   superuser.username is treated: superuser access is off, and the name falls through to the
		   normal principal lookup like any other unknown one. */
		if (StringUtils.isNotEmpty(superuserName) && StringUtils.isNotEmpty(superUserPwd)
			&& value.equals(superuserName)
			&& java.security.MessageDigest.isEqual(password.getBytes(java.nio.charset.StandardCharsets.UTF_8), superUserPwd.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {

			principal = new SuperUser();

			RuntimeEventLog.login("Authenticate", Map.of("id", principal.getUuid(), "name", principal.getName()));

		} else {

			try {

				/* One query per key, and not one OR query over all of them: a key adds the label of its
				   declaring type to the MATCH, and labels are ANDed, so a key from a subtype (Member.memberID)
				   restricted the whole lookup to that type and nobody else was found by name. Looking up per key
				   is harmless for ticket 1544, because the password is still checked once, below. */
				final List<PropertyKey<String>> lookupKeys = new LinkedList<>();

				lookupKeys.add(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY));

				for (final PropertyKey<String> key : keys) {

					if (!NodeInterfaceTraitDefinition.NAME_PROPERTY.equals(key.jsonName())) {

						lookupKeys.add(key);
					}
				}

				for (final PropertyKey<String> key : lookupKeys) {

					final NodeInterface node = StructrApp.getInstance().nodeQuery(StructrTraits.PRINCIPAL).key(key, value).disableSorting().getFirst();
					if (node != null) {

						principal = node.as(Principal.class);
						break;
					}
				}

			} catch (FrameworkException fex) {

				logger.warn("", fex);
			}

			if (principal == null) {

				final String keyMessage = describeLookupKeys(keys);

				/* Spend the same work a real password check would, so that "no such user" takes as
				   long as "wrong password". Both answer with the identical message already, but
				   without this the timing alone distinguishes them - an existing account pays for a
				   full Argon2 verification while a missing one returns at once - which is enough to
				   enumerate valid user names. */
				HashHelper.spendVerificationTime(password);

				if (failedLoginLog.allow("no principal")) {

					logger.info("No principal found for {} '{}'", keyMessage, value);
				}

				RuntimeEventLog.failedLogin("No principal found", Map.of("keyMessage", keyMessage, "value", value));

				throw new AuthenticationException(STANDARD_ERROR_MSG);

			} else {

				if (principal.isBlocked()) {

					/* Same reasoning as the missing-principal branch above: refusing a blocked account
					   must not be cheaper than checking a password, or the account is identifiable by
					   how quickly it is turned away. This branch answers before any verification, so
					   it has to spend that work itself. */
					HashHelper.spendVerificationTime(password);

					if (failedLoginLog.allow("blocked")) {

						logger.info("Principal {} is blocked", principal);
					}

					RuntimeEventLog.failedLogin("Principal is blocked", Map.of("id", principal.getUuid(), "name", principal.getName()));

					throw new AuthenticationException(STANDARD_ERROR_MSG);
				}

				try {

					// let Principal decide how to check password
					final boolean passwordValid = principal.isValidPassword(password);
					if (!passwordValid) {

						AuthHelper.incrementFailedLoginAttemptsCounter(principal);

						RuntimeEventLog.failedLogin("Wrong password", Map.of("id", principal.getUuid(), "name", principal.getName()));

						/* checkTooManyFailedLoginAttempts is deliberately NOT called here. It throws
						   TooManyFailedLoginAttemptsException, which the login endpoints report to the
						   caller with its own message and a "reason" header. Only an account that
						   EXISTS has a failed-attempt counter, so giving that answer to someone who did
						   not supply the right password confirms the account is real - precisely what
						   the shared error message exists to hide, and cheap to exploit because the
						   counter threshold is small and enabled by default. A caller who DOES supply
						   the right password is told, in the branch below: having proven they know the
						   credentials, the reason discloses nothing they could not already establish.
						   The lockout itself is unaffected, because that branch is the only way in. */
						throw new AuthenticationException(STANDARD_ERROR_MSG);

					} else {

						AuthHelper.checkTooManyFailedLoginAttempts(principal);
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

		return getPrincipalForCredential(Traits.of(StructrTraits.PRINCIPAL).key(PrincipalTraitDefinition.SESSION_IDS_PROPERTY), new String[]{ sessionId }, isPing, false);
	}

	public static void doLogin(final HttpServletRequest request, final Principal user) throws FrameworkException {

		if (request.getSession(false) == null) {

			SessionHelper.newSession(request);
		}

		SessionHelper.clearInvalidSessions(user);

		/* A new id for the authenticated session, and the old one is left bound to nobody: whatever id
		   the browser arrived with may have been chosen by somebody else - a cookie set from a sibling
		   subdomain, a ";jsessionid=" link, plain HTTP - and binding that one to the account is session
		   fixation (ticket 1594). Every way into Structr that ends in a session comes through here:
		   REST login, the websocket's HTTP counterpart, /confirm_registration, the password-reset
		   landing page and the OAuth callback. */
		final String previousSessionId = request.getSession(false) != null ? request.getSession(false).getId() : null;

		SessionHelper.rotateSessionId(request);

		// We need a session to login a user
		final HttpSession session = request.getSession(false);
		if (session != null) {

			final String sessionId = session.getId();
			if (previousSessionId != null && !previousSessionId.equals(sessionId)) {

				SessionHelper.clearSession(previousSessionId);
			}

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

		final Map<String, Object> params = new HashMap<>();
		params.put("user", user);

		Actions.callAsSuperUser(Actions.NOTIFICATION_ON_LOGIN, params, request);
	}

	public static void sendLogoutNotification (final Principal user, final HttpServletRequest request) throws FrameworkException {

		final Map<String, Object> params = new HashMap<>();
		params.put("user", user);

		Actions.callAsSuperUser(Actions.NOTIFICATION_ON_LOGOUT, params, request);
	}

	/**
	 * @return An opaque confirmation key with an HMAC-protected creation timestamp.
	 *
	 * Format: <random_hex>.<base64url_timestamp>.<hmac_base64url>
	 *
	 * The random portion provides uniqueness and unpredictability (128 bits from SecureRandom).
	 * The timestamp is Base64url-encoded (not plaintext) so it does not directly reveal server time.
	 * The HMAC-SHA256 signature prevents tampering with the timestamp and binds it to the random portion.
	 */
	public static String getConfirmationKey() {

		return generateOpaqueToken();
	}

	/**
	 * Determines if the key is valid or not. Supports both the new HMAC-signed format
	 * (random.timestamp_b64.hmac_b64) and the legacy format (uuid!timestamp) for
	 * backward compatibility during migration.
	 *
	 * @param confirmationKey The confirmation key to check
	 * @param validityPeriod The validity period for the key (in minutes)
	 * @return true if the key is within its validity period
	 */
	public static boolean isConfirmationKeyValid(final String confirmationKey, final Integer validityPeriod) {

		// Try new HMAC-signed format first
		final Long created = extractTimestampFromToken(confirmationKey);
		if (created != null) {

			final long maxValidity = created + validityPeriod * 60 * 1000L;

			return (maxValidity >= System.currentTimeMillis());
		}

		// Legacy format: uuid!timestamp
		final String[] parts = confirmationKey.split("!");
		if (parts.length == 2) {

			try {

				final long confirmationKeyCreated = Long.parseLong(parts[1]);
				final long maxValidity            = confirmationKeyCreated + validityPeriod * 60 * 1000L;

				return (maxValidity >= System.currentTimeMillis());

			} catch (NumberFormatException e) {

				logger.warn("Invalid legacy confirmation key format");

				return false;
			}
		}

		return Settings.ConfirmationKeyValidWithoutTimestamp.getValue();
	}

	/**
	 * @return "name", followed by every other key the lookup tried, joined with OR
	 */
	private static String describeLookupKeys(final Set<PropertyKey<String>> keys) {

		final Set<String> names = new LinkedHashSet<>();

		names.add(NodeInterfaceTraitDefinition.NAME_PROPERTY);

		for (final PropertyKey<String> key : keys) {

			names.add(key.jsonName());
		}

		return String.join(" OR ", names);
	}

	public static void incrementFailedLoginAttemptsCounter (final Principal principal) {

		final String uuid = principal.getUuid();
		final Object lock = userLocks.computeIfAbsent(uuid, k -> new Object());

		synchronized (lock) {

			/* Callers of getPrincipalForPassword() throw an AuthenticationException right after this
			   method returns, e.g. header authentication (X-User/X-Password), whose transaction is
			   opened and committed by servlet code far away from here and simply rolls back when that
			   exception propagates. Structr transactions are thread-local and reentrant (only the
			   outermost Tx actually commits/rolls back), so a nested Tx on this thread would roll back
			   with it. Committing on a separate thread - like the DeleteInvalidUserException handling -
			   gives the increment its own physical transaction, independent of whatever the
			   calling thread's transaction ends up doing. We join the thread so the counter is durably
			   persisted before returning, since the next login attempt's lockout check depends on it. */
			final Thread t = new Thread(() -> {
				final App app = StructrApp.getInstance();

				try (final Tx tx = app.tx()) {

					final NodeInterface node = app.getNodeById(uuid);
					if (node != null) {

						final Principal freshPrincipal = node.as(Principal.class);
						Integer failedAttempts = freshPrincipal.getPasswordAttempts();

						if (failedAttempts == null) {

							failedAttempts = 0;
						}

						freshPrincipal.setPasswordAttempts(failedAttempts + 1);

						/* The moment the lockout is measured from. Without it the counter only ever grows
						   and the block never lifts, so a handful of requests from anybody who knows a user
						   name take that account out (ticket 1598). What clears the counter otherwise is a
						   successful login - the thing being blocked - or a password reset, and that one
						   only where jsonrestservlet.user.autologin is on, which it is not by default. */
						freshPrincipal.setProperty(Traits.of(StructrTraits.PRINCIPAL).key(PrincipalTraitDefinition.LAST_FAILED_LOGIN_DATE_PROPERTY), new Date());
					}

					tx.success();

				} catch (FrameworkException fex) {

					logger.warn("Exception while incrementing failed login attempts counter", fex);
				}
			});

			t.start();

			try {

				t.join();

			} catch (InterruptedException iex) {

				Thread.currentThread().interrupt();
			}
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

				final long lockedForMinutes = lockoutMinutesFor(failedAttempts - maximumAllowedFailedAttempts);
				final Date lastFailure      = principal.getProperty(Traits.of(StructrTraits.PRINCIPAL).key(PrincipalTraitDefinition.LAST_FAILED_LOGIN_DATE_PROPERTY));

				/* No timestamp means the counter was run up before this instance recorded one, i.e. under
				   the scheme where the block never expired. Releasing those is the point of ticket 1598,
				   so the absence of a date is read as "long ago" rather than as "locked forever". */
				if (lastFailure != null && System.currentTimeMillis() - lastFailure.getTime() < TimeUnit.MINUTES.toMillis(lockedForMinutes)) {

					RuntimeEventLog.failedLogin("Too many login attempts", Map.of(
						"id", principal.getUuid(),
						"name", principal.getName(),
						"failedAttempts", failedAttempts,
						"maxAttempts", maximumAllowedFailedAttempts,
						"lockedForMinutes", lockedForMinutes
					));

					throw new TooManyFailedLoginAttemptsException();
				}

				/* The window has passed. Nothing is written here: the only caller reaches this after a
				   correct password and resets the counter itself a line later. */
			}
		}
	}

	/**
	 * How long an account stays locked, by how far it is past the allowed number of failed attempts.
	 *
	 * <p>Rising rather than flat, and expiring rather than permanent: a block that never lifts is a
	 * denial of service anybody can trigger with a user name and a handful of requests, while a flat
	 * short one is barely an obstacle to guessing. Five minutes covers a mistyped password, and an hour
	 * at the top is where the references sit - Keycloak's defaults rise to a fifteen-minute maximum, and
	 * the OWASP authentication guidance warns that a long lockout is itself the denial of service this
	 * exists to prevent, so the cap is deliberately short rather than punitive.
	 *
	 * <p>Deliberately not configurable: the threshold already is
	 * (security.passwordpolicy.maxfailedattempts), and a second dial here would mostly be a way to set it
	 * back to useless.
	 */
	private static long lockoutMinutesFor(final int attemptsOverLimit) {

		switch (attemptsOverLimit) {

			case 1:

				return 5;

			case 2:

				return 15;

			case 3:

				return 30;

			default:

				return 60;
		}
	}

	/**
	 * Records a wrong two-factor code and reports whether that used up what the account is allowed. The
	 * budget is the one a wrong password spends (security.passwordpolicy.maxfailedattempts), so guessing
	 * codes ends the same way guessing passwords does.
	 */
	public static boolean registerFailedTwoFactorAttempt (final Principal principal) {

		final String uuid                    = principal.getUuid();
		final Object lock                    = userLocks.computeIfAbsent(uuid, k -> new Object());
		final AtomicBoolean tokenInvalidated = new AtomicBoolean(false);

		synchronized (lock) {

			/* Its own thread, and with it its own transaction, for the same reason
			   incrementFailedLoginAttemptsCounter() needs one. Not every caller of
			   handleTwoFactorAuthentication() catches TwoFactorAuthenticationFailedException inside a
			   transaction it then commits, and where it does not, both writes below would roll back with
			   the calling thread - leaving the counter untouched and the token alive for the next guess,
			   which is the whole thing being fixed here. Joined, so the next attempt sees the result. */
			final Thread t = new Thread(() -> {
				final App app = StructrApp.getInstance();

				try (final Tx tx = app.tx()) {

					final NodeInterface node = app.getNodeById(uuid);
					if (node != null) {

						final Principal freshPrincipal = node.as(Principal.class);
						Integer failedAttempts = freshPrincipal.getPasswordAttempts();

						if (failedAttempts == null) {

							failedAttempts = 0;
						}

						failedAttempts++;

						freshPrincipal.setPasswordAttempts(failedAttempts);

						/* The same timestamp the password step records, because the two share this counter
						   (ticket 1583) and the lockout it produces expires from that moment (ticket 1598).
						   Left unwritten, a limit reached by guessing codes would carry no moment to measure
						   from and the block would read as one that had already run out. */
						freshPrincipal.setProperty(Traits.of(StructrTraits.PRINCIPAL).key(PrincipalTraitDefinition.LAST_FAILED_LOGIN_DATE_PROPERTY), new Date());

						final int maximumAllowedFailedAttempts = Settings.PasswordAttempts.getValue();
						if (maximumAllowedFailedAttempts > 0 && failedAttempts > maximumAllowedFailedAttempts) {

							/* The counter alone would not stop this: the token stays valid for
							   security.twofactorauthentication.logintimeout and the password step that issues
							   a new one RESETS the counter on its way through, so the budget would refill as
							   fast as it is spent. Dropping the token forces that password step, and it runs
							   checkTooManyFailedLoginAttempts() before the reset. */
							freshPrincipal.setTwoFactorToken(null);

							tokenInvalidated.set(true);
						}
					}

					tx.success();

				} catch (FrameworkException fex) {

					logger.warn("Exception while registering failed two factor attempt", fex);
				}
			});

			t.start();

			try {

				t.join();

			} catch (InterruptedException iex) {

				Thread.currentThread().interrupt();
			}
		}

		return tokenInvalidated.get();
	}

	public static void resetFailedLoginAttemptsCounter (final Principal principal) {

		final String uuid = principal.getUuid();
		final Object lock = userLocks.computeIfAbsent(uuid, k -> new Object());

		synchronized (lock) {

			/* Its own thread, and therefore its own transaction, for the same reason
			   incrementFailedLoginAttemptsCounter() needs one: header authentication (X-User/X-Password)
			   runs this inside a transaction the servlet code never commits, so a write on the calling
			   thread is simply lost - FailedLoginAttemptsCounterTest covers exactly that.

			   The consequence is that this must NOT be called from a transaction that is holding the
			   principal node, because the thread below would wait for a node the caller still has and the
			   caller then joins that thread. See HtmlServlet.checkResetPassword(). */
			final Thread t = new Thread(() -> {
				final App app = StructrApp.getInstance();

				try (final Tx tx = app.tx()) {

					final NodeInterface node = app.getNodeById(uuid);
					if (node != null) {

						node.as(Principal.class).setPasswordAttempts(0);
					}

					tx.success();

				} catch (FrameworkException fex) {

					logger.warn("Exception while resetting failed login attempts counter", fex);
				}
			});

			t.start();

			try {

				t.join();

			} catch (InterruptedException iex) {

				Thread.currentThread().interrupt();
			}
		}

		// Remove lock entry on successful login to prevent unbounded map growth
		userLocks.remove(uuid);
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
		final PropertyKey<String> twoFactorTokenKey = Traits.of(StructrTraits.PRINCIPAL).key(PrincipalTraitDefinition.TWO_FACTOR_TOKEN_PROPERTY);

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.nodeQuery(StructrTraits.PRINCIPAL).key(twoFactorTokenKey, twoFactorIdentificationToken).getFirst();
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

	/**
	 * Only the HMAC-signed format counts. The unsigned legacy shape "&lt;anything&gt;!&lt;timestamp&gt;"
	 * used to be accepted as a fallback, which made the token something an attacker could state rather
	 * than something the server issued - the timestamp was the only thing checked, and it is part of the
	 * value (ticket 1584). Dropping it costs nothing on upgrade: a token lives
	 * {@code security.twofactorauthentication.logintimeout} seconds, 300 by default, so the worst case is
	 * that someone between password and second factor at the moment of the restart enters their password
	 * again.
	 */
	public static boolean isTwoFactorTokenValid(final String twoFactorIdentificationToken) {

		final Long created = extractTimestampFromToken(twoFactorIdentificationToken);
		if (created == null) {

			return false;
		}

		final long maxTokenValidity = created + Settings.TwoFactorLoginTimeout.getValue() * 1000L;

		return (maxTokenValidity >= System.currentTimeMillis());
	}

	public static boolean isTwoFactorRequiredForUser(final Principal principal) {

		final int twoFactorLevel = Settings.TwoFactorLevel.getValue();

		final boolean twoFactorForced   = (twoFactorLevel == 2);
		final boolean twoFactorOptional = (twoFactorLevel == 1);

		return (twoFactorForced || (twoFactorOptional && principal.isTwoFactorUser()));
	}

	public enum TwoFactorAuthenticationResult {

		DISABLED, NOT_REQUIRED_FOR_USER, SUCCESS, TRUSTED, FAILURE
	}

	/**
	 * The reason this principal does not have to produce a code, or null when it does. One place decides
	 * it, because the answer is needed both here and on the login paths that cannot ask for a code
	 * themselves, and two copies of a rule like this drift apart in exactly the direction that hurts.
	 */
	private static TwoFactorAuthenticationResult getTwoFactorExemption(final Principal principal, final String userAgentString, final String trustToken) throws FrameworkException {

		final int twoFactorLevel = Settings.TwoFactorLevel.getValue();
		if (twoFactorLevel == 0) {

			return TwoFactorAuthenticationResult.DISABLED;
		}

		if (twoFactorLevel == 1 && !principal.isTwoFactorUser()) {

			return TwoFactorAuthenticationResult.NOT_REQUIRED_FOR_USER;
		}

		if (trustToken != null && principal.isDeviceTrustPossible() && DeviceTrustHelper.isValidDeviceTrustToken(trustToken, userAgentString, principal.getDeviceTrustSecret())) {

			return TwoFactorAuthenticationResult.TRUSTED;
		}

		return null;
	}

	public static boolean isTwoFactorStepRequired(final Principal principal, final String userAgentString, final String trustToken) throws FrameworkException {

		return getTwoFactorExemption(principal, userAgentString, trustToken) == null;
	}

	/**
	 * Starts the second factor for a login that has no way to ask for a code itself - an OAuth return or
	 * a confirmation link - and returns the address to send the browser to. Returns null when this
	 * principal needs no second factor, which is the caller's signal that it may log them in.
	 *
	 * <p>The caller must not create a session or issue tokens when this returns an address: those paths
	 * never reached handleTwoFactorAuthentication(), so a configuration that requires a second factor
	 * was satisfied by the redirect alone.
	 */
	public static String getTwoFactorRedirectForPrincipal(final HttpServletRequest request, final Principal principal) throws FrameworkException {

		if (!isTwoFactorStepRequired(principal, request.getHeader("User-Agent"), getDeviceTrustCookie(request))) {

			return null;
		}

		final String twoFactorToken = getIdentificationTokenForPrincipal();

		principal.setTwoFactorToken(twoFactorToken);

		/* No enrolment here, which is why buildData() is called with showQrCode false whatever the user's
		   state is. A QR code is a 200x200 image, and in a redirect it would have to travel as a query
		   parameter - tens of kilobytes of base64 in a Location header, which is past what a server will
		   emit and a browser will accept. Enrolment stays on the password login, where the same data goes
		   back in response headers and the application decides what to do with it. A user who has not
		   confirmed a second factor yet therefore cannot start here: they get the code prompt, cannot
		   answer it, and have to log in with their password - which is the flow that can enrol them. */
		final Map<String, String> data = TwoFactorAuthenticationRequiredException.buildData(principal, twoFactorToken, false);
		final StringBuilder redirect   = new StringBuilder(Settings.TwoFactorLoginPage.getValue());
		String separator               = "?";

		for (final Map.Entry<String, String> entry : data.entrySet()) {

			// twoFactorLoginPage is the address being built here, passing it along as a parameter says nothing
			if (!"twoFactorLoginPage".equals(entry.getKey())) {

				redirect.append(separator).append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
				separator = "&";
			}
		}

		return redirect.toString();
	}

	public static TwoFactorAuthenticationResult handleTwoFactorAuthentication (final Principal principal, final String twoFactorCode, final String twoFactorToken, final String userAgentString, final String trustToken) throws FrameworkException, TwoFactorAuthenticationRequiredException, TwoFactorAuthenticationFailedException {

		final TwoFactorAuthenticationResult exemption = getTwoFactorExemption(principal, userAgentString, trustToken);
		if (exemption != null) {

			return exemption;
		}

		if (twoFactorToken == null) {

			// user just logged in via username/password - no two factor identification token

			final boolean twoFactorConfirmed = principal.isTwoFactorConfirmed();
			if (!twoFactorConfirmed) {

				/* The exception below carries a QR code, and that QR code carries the TOTP secret, so a
				   password is all it takes to be handed the second factor of an account that has not
				   confirmed one yet. Two people who both know the password would walk away with the SAME
				   working secret, and whoever confirms first makes it permanent for both: the one who
				   should not have it keeps generating valid codes afterwards, and a password change does
				   not take that away. Rotating the secret on every handout means only the most recent
				   request can confirm, so an earlier copy stops working the moment the real user starts
				   their own enrolment. */
				principal.setTwoFactorSecret(TimeBasedOneTimePasswordHelper.generateBase32Secret());

				RuntimeEventLog.login("Two factor enrolment secret issued", Map.of("id", principal.getUuid(), "name", principal.getName()));
			}

			final String newTwoFactorToken = AuthHelper.getIdentificationTokenForPrincipal();
			principal.setTwoFactorToken(newTwoFactorToken);

			throw new TwoFactorAuthenticationRequiredException(principal, newTwoFactorToken, !twoFactorConfirmed);

		} else {

			try {

				final String currentKey = TimeBasedOneTimePasswordHelper.generateCurrentNumberString(principal.getTwoFactorSecret(), AuthHelper.getCryptoAlgorithm(), Settings.TwoFactorPeriod.getValue(), Settings.TwoFactorDigits.getValue());

				/* Not String.equals: it returns as soon as two digits differ, so how long the answer takes
				   says how much of the code was right, and a six-digit secret guessed digit by digit is a
				   few hundred attempts rather than a million. */
				if (twoFactorCode != null && java.security.MessageDigest.isEqual(currentKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), twoFactorCode.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {

					principal.setTwoFactorToken(null);   // reset token
					principal.setTwoFactorConfirmed(true);   // user has verified two factor use
					principal.setIsTwoFactorUser(true);

					logger.info("Successful two factor authentication ({})", principal.getName());

					RuntimeEventLog.login("Two factor authentication successful", Map.of("id", principal.getUuid(), "name", principal.getName()));

					return TwoFactorAuthenticationResult.SUCCESS;

				} else {

					/* Nothing used to count a wrong code. The token stayed valid for
					   security.twofactorauthentication.logintimeout no matter how many guesses were spent on
					   it, and a fresh one was always one password round away - a round that RESET the failed
					   attempt counter on its way through. Six digits with no limit is not a second factor,
					   only a delay. A wrong code now feeds the same counter a wrong password does, and once
					   that is used up the token dies with it, so the next guess has to go back through the
					   password step - where checkTooManyFailedLoginAttempts() runs before the counter is
					   reset and turns the account away. */
					if (AuthHelper.registerFailedTwoFactorAttempt(principal)) {

						logger.info("Two factor authentication failed too often, token invalidated ({})", principal.getName());
					}

					logger.info("Two factor authentication failed ({})", principal.getName());

					RuntimeEventLog.failedLogin("Two factor authentication failed", Map.of("id", principal.getUuid(), "name", principal.getName()));

					throw new TwoFactorAuthenticationFailedException();
				}

			} catch (GeneralSecurityException ex) {

				logger.warn("Two factor authentication key could not be generated - login not possible");

				return TwoFactorAuthenticationResult.FAILURE;
			}
		}
	}

	public static String getIdentificationTokenForPrincipal () {

		return generateOpaqueToken();
	}

	public static String getDeviceTrustCookie (final HttpServletRequest request) {

		final String trustCookieName = Settings.TwoFactorDeviceTrustCookieName.getValue();
		final Cookie[] cookies = request.getCookies();

		if (cookies != null) {

			for (Cookie cookie : cookies) {

				if (cookie.getName().equals(trustCookieName)) {

					return cookie.getValue();
				}
			}
		}

		return null;
	}

	public static void addDeviceTrustCookie (final SecurityContext securityContext, final String userAgentString, final String deviceTrustSecret) {

		final Cookie cookie = new Cookie(Settings.TwoFactorDeviceTrustCookieName.getValue(), DeviceTrustHelper.generateDeviceTrustToken(userAgentString, deviceTrustSecret));
		cookie.setHttpOnly(true);
		cookie.setSecure(securityContext.getRequest().isSecure());
		cookie.setPath("/");
		cookie.setMaxAge((int) Duration.ofDays(Settings.TwoFactorDeviceTrustDuration.getValue()).toSeconds());

		securityContext.getResponse().addCookie(cookie);
	}

	// --- HMAC-signed opaque token infrastructure ---

	// Ephemeral fallback key used when no JWT secret is configured.
	// Regenerated on each JVM start, which is acceptable because tokens
	// are short-lived and stored in the database.
	private static final byte[] EPHEMERAL_KEY = new byte[32];

	static {
		new SecureRandom().nextBytes(EPHEMERAL_KEY);
	}

	/**
	 * Generates an opaque, HMAC-signed token that embeds a creation timestamp
	 * without leaking it in plaintext.
	 *
	 * Format: <random_hex>.<base64url_timestamp>.<hmac_base64url>
	 *
	 * The HMAC covers both the random portion and the timestamp bytes,
	 * preventing tampering with either part.
	 */
	private static String generateOpaqueToken() {

		final byte[] randomBytes = new byte[16];
		new SecureRandom().nextBytes(randomBytes);
		final String randomHex = bytesToHex(randomBytes);
		final long now = System.currentTimeMillis();
		final byte[] timestampBytes = ByteBuffer.allocate(8).putLong(now).array();
		final String timestampB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(timestampBytes);
		final byte[] hmacKey = getTokenHmacKey();
		final byte[] hmacInput = new byte[randomBytes.length + timestampBytes.length];

		System.arraycopy(randomBytes, 0, hmacInput, 0, randomBytes.length);
		System.arraycopy(timestampBytes, 0, hmacInput, randomBytes.length, timestampBytes.length);

		try {

			final Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
			final byte[] hmacResult = mac.doFinal(hmacInput);
			final String hmacB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(hmacResult);

			return randomHex + "." + timestampB64 + "." + hmacB64;

		} catch (Exception e) {

			// Should never happen with HmacSHA256, but fall back to legacy format
			logger.warn("Failed to generate HMAC-signed token, falling back to legacy format: {}", e.getMessage());

			return UUID.randomUUID().toString() + "!" + now;
		}
	}

	/**
	 * Extracts and verifies the creation timestamp from an HMAC-signed token.
	 *
	 * @return the creation timestamp in milliseconds, or null if the token is
	 *         not in the new format or the HMAC verification fails
	 */
	private static Long extractTimestampFromToken(final String token) {

		if (token == null) {

			return null;
		}

		final String[] parts = token.split("\\.");
		if (parts.length != 3) {

			return null;
		}

		try {

			final byte[] randomBytes    = hexToBytes(parts[0]);
			final byte[] timestampBytes = Base64.getUrlDecoder().decode(parts[1]);
			final byte[] providedHmac   = Base64.getUrlDecoder().decode(parts[2]);

			if (timestampBytes.length != 8) {

				return null;
			}

			// Recompute HMAC and verify
			final byte[] hmacKey = getTokenHmacKey();
			final byte[] hmacInput = new byte[randomBytes.length + timestampBytes.length];

			System.arraycopy(randomBytes, 0, hmacInput, 0, randomBytes.length);
			System.arraycopy(timestampBytes, 0, hmacInput, randomBytes.length, timestampBytes.length);

			final Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
			final byte[] expectedHmac = mac.doFinal(hmacInput);

			// Constant-time comparison
			if (!java.security.MessageDigest.isEqual(expectedHmac, providedHmac)) {

				logger.warn("HMAC verification failed for token");

				return null;
			}

			return ByteBuffer.wrap(timestampBytes).getLong();

		} catch (Exception e) {

			// Not in new format, or corrupted

			return null;
		}
	}

	/**
	 * Returns the HMAC key for token signing/verification.
	 * Uses the JWT secret if configured (>= 32 chars), otherwise falls back
	 * to an ephemeral per-instance key.
	 */
	private static byte[] getTokenHmacKey() {

		final String jwtSecret = Settings.JWTSecret.getValue();
		if (jwtSecret != null && jwtSecret.length() >= 32) {

			return jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		}

		return EPHEMERAL_KEY;
	}

	private static String bytesToHex(final byte[] bytes) {

		final StringBuilder sb = new StringBuilder(bytes.length * 2);

		for (final byte b : bytes) {

			sb.append(String.format("%02x", b));
		}

		return sb.toString();
	}

	private static byte[] hexToBytes(final String hex) {

		final int len = hex.length();
		final byte[] data = new byte[len / 2];

		for (int i = 0; i < len; i += 2) {

			data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
				+ Character.digit(hex.charAt(i + 1), 16));
		}

		return data;
	}

	// The StandardName for the given SHA algorithm.
	// see https://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Mac
	private static String getCryptoAlgorithm() {

		return "Hmac" + Settings.TwoFactorAlgorithm.getValue();
	}
}
