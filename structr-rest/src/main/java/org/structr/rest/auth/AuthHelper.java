/*
 * Copyright (C) 2010-2021 Structr GmbH
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

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.*;
import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
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
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.Actions;

/**
 * Utility class for authentication.
 */
public class AuthHelper {

	public static final String STANDARD_ERROR_MSG = "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";
	public static final String TOKEN_ERROR_MSG = "The given access_token or refresh_token is invalid";
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

		if (superuserName.equals(value) && superUserPwd.equals(password)) {

			// logger.info("############# Authenticated as superadmin! ############");

			principal = new SuperUser();

			RuntimeEventLog.login("Authenticate", Map.of("id", principal.getUuid(), "name", principal.getName()));

		} else {

			try {

				principal = StructrApp.getInstance().nodeQuery(Principal.class).and().or(key, value).or(AbstractNode.name, value).disableSorting().getFirst();

			} catch (FrameworkException fex) {

				logger.warn("", fex);
			}

			if (principal == null) {

				final String keyMessage = ("name".equals(key.dbName())) ? "name" : "name OR " + key.dbName();

				logger.info("No principal found for {} {}", keyMessage, value);

				RuntimeEventLog.failedLogin("No principal found", Map.of("keyMessage", keyMessage, "value", value));

				throw new AuthenticationException(STANDARD_ERROR_MSG);

			} else {

				if (principal.isBlocked()) {

					logger.info("Principal {} is blocked", principal);

					RuntimeEventLog.failedLogin("Principal is blocked", Map.of("id", principal.getUuid(), "name", principal.getName()));

					throw new AuthenticationException(STANDARD_ERROR_MSG);
				}

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

		return getPrincipalForCredential(StructrApp.key(Principal.class, "sessionIds"), new String[]{ sessionId }, isPing);

	}

	public static Principal getPrincipalForAccessToken(String token, PropertyKey<String> eMailKey) throws FrameworkException {

		final String jwtSecretType = Settings.JWTSecretType.getValue();
		Principal user = null;

		switch (jwtSecretType) {
			default:
			case "secret":
				user = getUserForAccessTokenWithSecret(token, eMailKey);
				break;

			case "keypair":
				user = getPrincipalForAccessTokenWithKeystore(token, eMailKey);
				break;

			case "jwks":

				final String provider = Settings.JWTSProvider.getValue();

				break;
		}

		return user;
	}

	private static Principal getPrincipalForTokenClaims(Claims claims, PropertyKey<String> eMailKey) throws FrameworkException {

		final String instanceName = Settings.InstanceName.getValue();
		Principal user = null;

		String instance = claims.get("instance", String.class);
		String uuid = claims.get("uuid", String.class);
		String eMail = claims.get("eMail", String.class);

		// if the instance is the same that issued the token, we can lookup the user with uuid claim
		if (StringUtils.equals(instance, instanceName)) {

			user  = StructrApp.getInstance().nodeQuery(Principal.class).and().or(NodeInterface.id, uuid).disableSorting().getFirst();

		} else if (eMail != null && StringUtils.equals(eMail, "")) {

			user  = StructrApp.getInstance().nodeQuery(Principal.class).and().or(eMailKey, eMail).disableSorting().getFirst();

		}

		return user;
	}

	private static Principal getPrincipalForAccessTokenWithKeystore(String token, PropertyKey<String> eMailKey) throws FrameworkException  {

		Key publicKey = getPublicKeyForToken();
		Jws<Claims> jws = validateTokenWithKeystore(token, publicKey);

		if (jws == null) {
			throw new FrameworkException(404, AuthHelper.TOKEN_ERROR_MSG);
		}

		return getPrincipalForTokenClaims(jws.getBody(), eMailKey);
	}

	private static Principal getUserForAccessTokenWithSecret(String token, PropertyKey<String> eMailKey) throws FrameworkException {

		final String secret = Settings.JWTSecret.getValue();

		Jws<Claims> jws = validateTokenWithSecret(token, secret);

		if (jws == null) {
			throw new FrameworkException(404, AuthHelper.TOKEN_ERROR_MSG);
		}

		return getPrincipalForTokenClaims(jws.getBody(), eMailKey);
	}

	public static Map<String, String> createTokensForUser(Principal user, Date accessTokenLifetime, Date refreshTokenLifetime) throws FrameworkException {

		final String jwtSecretType = Settings.JWTSecretType.getValue();
		Map<String, String> tokens = null;

		if (user == null) {
			throw new FrameworkException(400, "Can't create token if no user is given");
		}

		final String instanceName = Settings.InstanceName.getValue();

		switch (jwtSecretType) {

			default:
			case "secret":
				tokens = createTokensForUserWithSecret(user, accessTokenLifetime, refreshTokenLifetime, instanceName);
				break;

			case "keypair":
				tokens = createTokensForUserWithKeystore(user, accessTokenLifetime, refreshTokenLifetime, instanceName);
				break;
		}

		clearTimedoutRefreshTokens(user);
		return tokens;
	}

	private static void clearTimedoutRefreshTokens(Principal user) {

		final PropertyKey<String[]> key = StructrApp.key(Principal.class, "refreshTokens");
		final String[] refreshTokens    = user.getProperty(key);

		if (refreshTokens != null) {

			try {

				for (final String refreshToken : refreshTokens) {

					if (refreshTokenTimedOut(refreshToken)) {

						logger.debug("RefreshToken {} timed out", new Object[]{refreshToken});

						user.removeRefreshToken(refreshToken);
					}
				}

			} catch (Exception fex) {

				logger.warn("Error while removing refreshToken of user " + user.getUuid(), fex);
			}
		}
	}

	private static boolean refreshTokenTimedOut(String refreshToken) {

		final String[] splittedToken = refreshToken.split("_");

		if (splittedToken.length > 1 && splittedToken[1] != null) {

			if (Calendar.getInstance().getTimeInMillis() > Long.parseLong(splittedToken[1])) {
				return true;
			}
		}

		return false;
	}

	public static Map<String, String> createTokensForUser(final Principal user) throws FrameworkException {

		if (user == null) {
			throw new FrameworkException(400, "Can't create token if no user is given");
		}

		Calendar accessTokenExpirationDate = Calendar.getInstance();
		accessTokenExpirationDate.add(Calendar.MINUTE, Settings.JWTExpirationTimeout.getValue());

		Calendar refreshTokenExpirationDate = Calendar.getInstance();
		refreshTokenExpirationDate.add(Calendar.MINUTE, Settings.JWTRefreshTokenExpirationTimeout.getValue());

		return createTokensForUser(user, accessTokenExpirationDate.getTime(), refreshTokenExpirationDate.getTime());
	}

	public static Map<String, String> createTokens (Principal user, Key key, Date accessTokenExpirationDate, Date refreshTokenExpirationDate, String instanceName, String jwtIssuer) {
		final  Map<String, String> tokens = new HashMap<>();

		String accessToken = Jwts.builder()
				.setSubject(user.getName())
				.setExpiration(accessTokenExpirationDate)
				.setIssuer(jwtIssuer)
				.claim("instance", instanceName)
				.claim("uuid", user.getUuid())
				.claim("eMail", user.getEMail())
				.claim("tokenType", "access_token")
				.signWith(key)
				.compact();

		tokens.put("access_token", accessToken);
		tokens.put("expiration_date", Long.toString(accessTokenExpirationDate.getTime()));

		if (refreshTokenExpirationDate != null) {

			final String newTokenUUID = NodeServiceCommand.getNextUuid();
			StringBuilder tokenStringBuilder = new StringBuilder();
			tokenStringBuilder.append(newTokenUUID).append("_").append(refreshTokenExpirationDate.getTime());

			final String tokenId = tokenStringBuilder.toString();

			String refreshToken = Jwts.builder()
					.setSubject(user.getName())
					.setExpiration(refreshTokenExpirationDate)
					.setIssuer(jwtIssuer)
					.claim("tokenId", tokenId)
					.claim("tokenType", "refresh_token")
					.signWith(key)
					.compact();

			Principal.addRefreshToken(user, tokenId);
			tokens.put("refresh_token", refreshToken);
		}

		return tokens;
	}

	public static Map<String, String> createTokensForUserWithSecret(Principal user, Date accessTokenExpirationDate, Date refreshTokenExpirationDate, String instanceName) throws FrameworkException {

		final String secret = Settings.JWTSecret.getValue();
		final String jwtIssuer = Settings.JWTIssuer.getValue();

		try {

			SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
			return createTokens(user, key, accessTokenExpirationDate, refreshTokenExpirationDate, instanceName, jwtIssuer);

		} catch (WeakKeyException ex) {

			throw new FrameworkException(500, "The provided secret is too weak (must be at least 32 characters)");
		}
	}

	public static Map<String, String> createTokensForUserWithKeystore(Principal user, Date accessTokenExpirationDate, Date refreshTokenExpirationDate, String instanceName) throws FrameworkException {

		Key privateKey = getPrivateKeyForToken();

		if (privateKey == null) {

			throw new FrameworkException(400, "Cannot read private key file");
		}

		final String jwtIssuer = Settings.JWTIssuer.getValue();

		return createTokens(user, privateKey, accessTokenExpirationDate, refreshTokenExpirationDate, instanceName, jwtIssuer);
	}

	public static Principal getPrincipalForRefreshToken(String refreshToken) throws FrameworkException {

		final String jwtSecretType = Settings.JWTSecretType.getValue();
		Jws<Claims> claims = null;

		switch (jwtSecretType) {
			default:
			case "secret":

				final String secret = Settings.JWTSecret.getValue();
				claims = validateTokenWithSecret(refreshToken, secret);
				break;

			case "keypair":

				final Key publicKey = getPublicKeyForToken();
				claims = validateTokenWithKeystore(refreshToken, publicKey);
				break;

			case "jwks":

				throw new FrameworkException(400, "will not validate refresh_token because authentication is not handled by this instance");

		}

		if (claims == null) {
			throw new FrameworkException(401, AuthHelper.TOKEN_ERROR_MSG);
		}

		final String tokenId = (String) claims.getBody().get("tokenId");
		final String tokenType = (String) claims.getBody().get("tokenType");
		if (tokenId == null || tokenType == null || !StringUtils.equals(tokenType, "refresh_token")) {
			throw new FrameworkException(401, AuthHelper.TOKEN_ERROR_MSG);
		}

		Principal user = getPrincipalForCredential(StructrApp.key(Principal.class, "refreshTokens"), new String[]{ tokenId }, false);

		if (user == null) {
			return null;
		}

		Principal.removeRefreshToken(user, tokenId);
		return user;
	}

	public static Map<String, String> createTokens(final HttpServletRequest request, final Principal user) throws FrameworkException {

		final Map<String, String>  tokens = createTokensForUser(user);
		return tokens;
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

			if (user.addSessionId(sessionId)) {

				AuthHelper.sendLoginNotification(user);

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

		RuntimeEventLog.logout("Logout", Map.of("id", user.getUuid(), "name", user.getName()));

		AuthHelper.sendLogoutNotification(user);
	}

	public static void sendLoginNotification (final Principal user) throws FrameworkException {

		try {

			final Map<String, Object> params = new HashMap<>();
			params.put("user", user);

			Actions.callAsSuperUser(Actions.NOTIFICATION_LOGIN, params);

		} catch (UnlicensedScriptException ex) {
			ex.log(logger);
		}
	}

	public static void sendLogoutNotification (final Principal user) throws FrameworkException {

		try {

			final Map<String, Object> params = new HashMap<>();
			params.put("user", user);

			Actions.callAsSuperUser(Actions.NOTIFICATION_LOGOUT, params);

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

			final PropertyKey<Integer> passwordAttemptsKey = StructrApp.key(Principal.class, "passwordAttempts");

			Integer failedAttempts = principal.getProperty(passwordAttemptsKey);

			if (failedAttempts == null) {
				failedAttempts = 0;
			}

			failedAttempts++;

			principal.setProperty(passwordAttemptsKey, failedAttempts);

		} catch (FrameworkException fex) {

			logger.warn("Exception while incrementing failed login attempts counter", fex);
		}
	}

	public static void checkTooManyFailedLoginAttempts (final Principal principal) throws TooManyFailedLoginAttemptsException {

		final PropertyKey<Integer> passwordAttemptsKey = StructrApp.key(Principal.class, "passwordAttempts");
		final int maximumAllowedFailedAttempts = Settings.PasswordAttempts.getValue();

		if (maximumAllowedFailedAttempts > 0) {

			Integer failedAttempts = principal.getProperty(passwordAttemptsKey);

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

			principal.setProperty(StructrApp.key(Principal.class, "passwordAttempts"), 0);

		} catch (FrameworkException fex) {

			logger.warn("Exception while resetting failed login attempts counter", fex);
		}
	}

	public static void handleForcePasswordChange (final Principal principal) throws PasswordChangeRequiredException {

		final boolean forcePasswordChange = Settings.PasswordForceChange.getValue();

		if (forcePasswordChange) {

			final PropertyKey<Date> passwordChangeDateKey  = StructrApp.key(Principal.class, "passwordChangeDate");
			final int passwordDays = Settings.PasswordForceChangeDays.getValue();

			final Date now = new Date();
			final Date passwordChangeDate = (principal.getProperty(passwordChangeDateKey) != null) ? principal.getProperty(passwordChangeDateKey) : new Date (0); // setting date in past if not yet set
			final int daysApart = (int) ((now.getTime() - passwordChangeDate.getTime()) / (1000 * 60 * 60 * 24l));

			if (daysApart > passwordDays) {

				throw new PasswordChangeRequiredException();
			}
		}
	}

	public static Principal getUserForTwoFactorToken (final String twoFactorIdentificationToken) throws TwoFactorAuthenticationTokenInvalidException, FrameworkException {

		final App app = StructrApp.getInstance();

		Principal principal = null;

		final PropertyKey<String> twoFactorTokenKey   = StructrApp.key(Principal.class, "twoFactorToken");

		try (final Tx tx = app.tx()) {
			principal = app.nodeQuery(Principal.class).and(twoFactorTokenKey, twoFactorIdentificationToken).getFirst();
			tx.success();
		}

		if (principal != null) {

			if (!AuthHelper.isTwoFactorTokenValid(twoFactorIdentificationToken)) {

				principal.setProperty(twoFactorTokenKey, null);

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

	public static boolean isRequestingIPWhitelistedForTwoFactorAuthentication(final String requestIP) {

		final String whitelistedIPs = Settings.TwoFactorWhitelistedIPs.getValue();

		if (!StringUtils.isEmpty(whitelistedIPs) && !StringUtils.isEmpty(requestIP)) {
			for (final String whitelistedIP : whitelistedIPs.split(",")) {
				if (whitelistedIP.trim().equals(requestIP.split(":")[0])) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean handleTwoFactorAuthentication (final Principal principal, final String twoFactorCode, final String twoFactorToken, final String requestIP) throws FrameworkException, TwoFactorAuthenticationRequiredException, TwoFactorAuthenticationFailedException {

		if (!AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication(requestIP)) {

			final PropertyKey<String> twoFactorTokenKey      = StructrApp.key(Principal.class, "twoFactorToken");
			final PropertyKey<Boolean> isTwoFactorUserKey    = StructrApp.key(Principal.class, "isTwoFactorUser");
			final PropertyKey<Boolean> twoFactorConfirmedKey = StructrApp.key(Principal.class, "twoFactorConfirmed");

			final int twoFactorLevel   = Settings.TwoFactorLevel.getValue();
			boolean isTwoFactorUser    = principal.getProperty(isTwoFactorUserKey);
			boolean twoFactorConfirmed = principal.getProperty(twoFactorConfirmedKey);

			boolean userNeedsTwoFactor = twoFactorLevel == 2 || (twoFactorLevel == 1 && isTwoFactorUser == true);

			if (userNeedsTwoFactor) {

				if (twoFactorToken == null) {

					// user just logged in via username/password - no two factor identification token

					final String newTwoFactorToken = AuthHelper.getIdentificationTokenForPrincipal();
					principal.setProperty(twoFactorTokenKey, newTwoFactorToken);

					throw new TwoFactorAuthenticationRequiredException(principal, newTwoFactorToken, !twoFactorConfirmed);

				} else {

					try {

						final String currentKey = TimeBasedOneTimePasswordHelper.generateCurrentNumberString(Principal.getTwoFactorSecret(principal), AuthHelper.getCryptoAlgorithm(), Settings.TwoFactorPeriod.getValue(), Settings.TwoFactorDigits.getValue());

						// check two factor authentication
						if (currentKey.equals(twoFactorCode)) {

							principal.setProperty(twoFactorTokenKey,     null);   // reset token
							principal.setProperty(twoFactorConfirmedKey, true);   // user has verified two factor use
							principal.setProperty(isTwoFactorUserKey,    true);

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

	private static Key getPrivateKeyForToken() {

		final String keyStorePath = Settings.JWTKeyStore.getValue();
		final String keyStorePassword = Settings.JWTKeyStorePassword.getValue();
		final String keyAlias = Settings.JWTKeyAlias.getValue();

		try {

			File keyStoreFile = new File(keyStorePath);
			KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			keystore.load(new FileInputStream(keyStoreFile), keyStorePassword.toCharArray());

			Key privateKey = keystore.getKey(keyAlias, keyStorePassword.toCharArray());

			return privateKey;
		} catch (IOException e) {

			logger.warn("Cannot read private key file", e);

		} catch (CertificateException e) {

			logger.warn("Error while reading jwt keystore", e);
		} catch (UnrecoverableKeyException e) {

			logger.warn("Error while reading jwt keystore, probably wrong password", e);
		} catch (Exception e) {

			logger.warn("Error while reading jwt keystore", e);
		}

		return null;
	}

	private static Key getPublicKeyForToken() {

		final String keyStorePath = Settings.JWTKeyStore.getValue();
		final String keyStorePassword = Settings.JWTKeyStorePassword.getValue();
		final String keyAlias = Settings.JWTKeyAlias.getValue();

		try {
			File keyStoreFile = new File(keyStorePath);
			KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			keystore.load(new FileInputStream(keyStoreFile), keyStorePassword.toCharArray());

			Certificate cert = keystore.getCertificate(keyAlias);
			PublicKey publicKey = cert.getPublicKey();

			return publicKey;

		} catch (Exception e) {
			logger.warn("Error while reading jwt keystore", e);
		}

		return null;
	}

	private static Jws<Claims> validateTokenWithSecret(String token, String secret) {
		try {

			SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

			return Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token);

		} catch (Exception e) {
			logger.debug("Invalid token", e);
		}

		return null;
	}

	private static Jws<Claims> validateTokenWithKeystore(String token, Key key) {
		try {

			return Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token);

		} catch (Exception e) {
			logger.debug("Invalid token", e);
		}

		return null;
	}


}
