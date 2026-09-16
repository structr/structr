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

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.ServicePrincipal;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

public class JWTHelper {

	public static final String TOKEN_ERROR_MSG = "The given access_token or refresh_token is invalid";

	/**
	 * The claim marking a token as belonging to the MCP endpoint, and its only accepted value.
	 *
	 * Mirrors org.structr.ai.mcp.oauth.TokenService in structr-ai-module. Duplicated as a literal
	 * rather than imported
	 * because structr.base cannot depend on a feature module - and the value is part of a token
	 * format, so it is frozen either way.
	 */
	private static final String MCP_TOKEN_USE_CLAIM = "token_use";
	private static final String MCP_TOKEN_USE_VALUE = "mcp";
	private static final Logger logger = LoggerFactory.getLogger(JWTHelper.class.getName());

	public static Principal getPrincipalForAccessToken(final String token, final PropertyKey<String> eMailKey) throws FrameworkException {

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

				final String provider = Settings.JWKSProvider.getValue();
				final String issuer = Settings.JWTIssuer.getValue();

				if (provider != null) {

					try {

						DecodedJWT jwt = JWT.decode(token);
						final String kid = jwt.getKeyId();

						if (kid != null) {

							// if no issuer is specified, we can assume that issuer url = provider url.
							JwkProvider jwkProvider;

							if (!StringUtils.isEmpty(issuer) && !StringUtils.equals("structr", issuer)) {

								jwkProvider = new UrlJwkProvider(new URL(provider));

							} else {

								// loads jwks from .well-known resource of provider
								jwkProvider = new UrlJwkProvider(provider);
							}

							Jwk jwk = jwkProvider.get(kid);
							Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
							JWTVerifier verifier = buildJWKSVerifier(algorithm, issuer);

							if (verifier != null) {

								jwt = verifier.verify(jwt);

								/* Not self-issued. Everything this instance knows about the token is that the
								   configured provider signed it for the configured audience - the claims inside
								   are the provider's, not Structr's. */
								user = getPrincipalForTokenClaims(jwt.getClaims(), eMailKey, false);
							}
						}

					} catch (JWTVerificationException ex) {

						throw new FrameworkException(422, ex.getMessage());

					} catch (Exception ex) {

						logger.warn("Error while trying to process JWKS.\n {}", ex.getMessage());
						throw new FrameworkException(422, "Error while trying to process JWKS.");
					}
				}

				break;
		}

		return user;
	}

	public static Principal getPrincipalForRefreshToken(final String refreshToken) throws FrameworkException {

		final Traits traits        = Traits.of(StructrTraits.PRINCIPAL);
		final String jwtSecretType = Settings.JWTSecretType.getValue();
		Map<String, Claim> claims  = null;

		switch (jwtSecretType) {
			default:
			case "secret":

				final String secret = Settings.JWTSecret.getValue();
				claims = validateTokenWithSecret(refreshToken, secret);
				break;

			case "keypair":

				final RSAPublicKey publicKey = getPublicKeyForToken();
				final RSAPrivateKey privateKey = getPrivateKeyForToken();

				if (publicKey == null || privateKey == null) {

					break;
				}

				claims = validateTokenWithKeystore(refreshToken, Algorithm.RSA256(publicKey, privateKey));
				break;

			case "jwks":

				throw new FrameworkException(400, "will not validate refresh_token because authentication is not handled by this instance");

		}

		if (claims == null) {

			return null;
		}

		final String tokenId = claims.get("tokenId").asString();
		final String tokenType = claims.get("tokenType").asString();

		if (tokenId == null || tokenType == null || !StringUtils.equals(tokenType, "refresh_token")) {

			return null;

		}

		final Principal user = AuthHelper.getPrincipalForCredential(traits.key(PrincipalTraitDefinition.REFRESH_TOKENS_PROPERTY), new String[]{ tokenId }, false, false);
		if (user == null) {

			return null;
		}

		user.removeRefreshToken(tokenId);

		return user;
	}

	public static Map<String, String> createTokensForUser(final Principal user) throws FrameworkException {

		if (user == null) {

			throw new FrameworkException(500, "Can't create token if no user is given");
		}

		Calendar accessTokenExpirationDate = Calendar.getInstance();
		accessTokenExpirationDate.add(Calendar.MINUTE, Settings.JWTExpirationTimeout.getValue());

		Calendar refreshTokenExpirationDate = Calendar.getInstance();
		refreshTokenExpirationDate.add(Calendar.MINUTE, Settings.JWTRefreshTokenExpirationTimeout.getValue());

		return createTokensForUser(user, accessTokenExpirationDate.getTime(), refreshTokenExpirationDate.getTime());
	}

	public static Map<String, String> createTokensForUser(final Principal user, final Date accessTokenLifetime, final Date refreshTokenLifetime) throws FrameworkException {

		final String jwtSecretType = Settings.JWTSecretType.getValue();
		Map<String, String> tokens = null;

		if (user == null) {

			throw new FrameworkException(500, "Can't create token if no user is given");
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

	private static boolean validateTokenForUser(final String tokenId, final Principal user) {

		// if tokenId is empty, token was created without refresh_token
		if (StringUtils.isEmpty(tokenId)) {

			return true;
		}

		final String[] refreshTokens = user.getRefreshTokens();

		return Arrays.asList(refreshTokens).contains(tokenId);
	}

	/**
	 * Resolves the principal a set of verified token claims refers to.
	 *
	 * @param selfIssued whether the token was issued by this instance, i.e. signed with the secret or the
	 *                   key pair configured here. Only such a token may use the instance/uuid shortcut
	 *                   below - see the comment there.
	 */
	private static Principal getPrincipalForTokenClaims(final Map<String, Claim> claims, final PropertyKey<String> eMailKey, final boolean selfIssued) throws FrameworkException {

		// An MCP access token is a capability for one endpoint, issued to an external service by the
		// MCP authorization server in structr-ai-module. It must never authenticate anything else,
		// and this path is reached before any resource check, so refusing it here is what keeps
		// "MCP token" from meaning "REST token".
		//
		// Those tokens are signed with their own secret (mcp.oauth.jwt.secret), so one normally
		// fails verification long before this. This is the second lock: it still holds if an
		// instance is ever configured with the same secret for both.
		final Claim tokenUse = claims.get(MCP_TOKEN_USE_CLAIM);
		if (tokenUse != null && !tokenUse.isNull() && MCP_TOKEN_USE_VALUE.equals(tokenUse.asString())) {

			logger.warn("An MCP access token was presented to the REST API and refused. MCP tokens are only "
				+ "valid at the MCP endpoint.");

			return null;
		}

		final String instanceName = Settings.InstanceName.getValue();
		NodeInterface userNode    = null;
		Principal user            = null;
		String instance = claimAsString(claims, "instance");
		String uuid     = claimAsString(claims, "uuid");
		String eMail    = claimAsString(claims, "eMail");

		if (StringUtils.isEmpty(eMail)) {

			eMail = claimAsString(claims, "email");
		}

		/* The instance/uuid shortcut, and why it is guarded by selfIssued (ticket 1593): "the instance
		   name matches, so take the uuid claim and log in that node" is only an identity statement when
		   the token came from this instance. In jwks mode it did not - it came from an external identity
		   provider, whose claims an administrator there controls. Neither value protects anything:
		   application.instance.name is empty by default and is displayed in the UI when it is set, and
		   the uuid of an admin user is not a secret either. A token carrying those two claims would
		   otherwise be a login as that admin, with no e-mail address involved and nothing else checked. */
		if (selfIssued && StringUtils.equals(instance, instanceName)) {

			userNode = StructrApp.getInstance().nodeQuery(StructrTraits.PRINCIPAL).key(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY), uuid).disableSorting().getFirst();

			if (userNode != null) {

				user = userNode.as(Principal.class);
			}

		} else if (eMail != null && StringUtils.isNotEmpty(eMail)) {

			userNode = StructrApp.getInstance().nodeQuery(StructrTraits.PRINCIPAL).key(eMailKey, eMail).disableSorting().getFirst();

			if (userNode != null) {

				user = userNode.as(Principal.class);
			}

		} else {

			final String adminClaimKey     = Settings.JWKSAdminClaimKey.getValue("");
			final String adminClaimValue   = Settings.JWKSAdminClaimValue.getValue("");
			final String groupReferenceKey = Settings.JWKSGroupClaimKey.getValue("");
			final String objectNameKey     = Settings.JWKSObjectNameClaimKey.getValue("");
			final String objectIdKey       = Settings.JWKSObjectIdClaimKey.getValue("");

			if (StringUtils.isEmpty(objectIdKey)) {

				logger.warn("Invalid JWKS configuration: missing value for {} which is used to identify the temporary principal for m2m requests authenticated with JWKS.", Settings.JWKSObjectIdClaimKey.getKey());

			} else {

				// create a virtual user with an optional temporary group association via groupReferenceKey
				if (claims.containsKey(objectIdKey)) {

					final String id                 = claims.get(objectIdKey).asString();
					final List<String> referenceIds = new LinkedList<>();
					boolean isAdmin                 = false;
					String name                     = id;

					// group reference is optional
					if (claims.containsKey(groupReferenceKey)) {

						final Claim claim               = claims.get(groupReferenceKey);
						final List<String> values       = claim.asList(String.class);

						if (values != null) {

							// if values is non-null, the claim contained a list of strings
							referenceIds.addAll(values);

						} else {

							// otherwise it was a single string
							referenceIds.add(claim.asString());
						}
					}

					// explicit admin setting?
					if (claims.containsKey(adminClaimKey)) {

						if (StringUtils.isBlank(adminClaimValue)) {

							logger.warn("Invalid JWKS configuration: missing configuration value for {} while {} is configured.", Settings.JWKSAdminClaimValue.getKey(), Settings.JWKSAdminClaimKey.getKey());

						} else {

							final Set<String> adminClaims = new LinkedHashSet<>();
							final Claim claim             = claims.get(adminClaimKey);
							final List<String> values     = claim.asList(String.class);

							if (values != null) {

								// if values is non-null, the claim contained a list of strings
								adminClaims.addAll(values);

							} else {

								// otherwise it was a single string
								adminClaims.add(claim.asString());
							}

							if (adminClaims.contains(adminClaimValue)) {

								isAdmin = true;
							}
						}

					} else {

						logger.warn("Invalid JWKS configuration: JWKS claims response contains no value for {}, configured in {}, which is needed to set admin privileges." , adminClaimKey, Settings.JWKSAdminClaimKey.getKey());
					}

					// name is optional
					if (claims.containsKey(objectNameKey)) {

						name = claims.get(objectNameKey).asString();
					}

					user = new ServicePrincipal(id, name, referenceIds, isAdmin);

				} else {

					logger.warn("Invalid JWKS configuration: JWKS claims response contains no value for {} which is needed to identify the principal. Please set the correct key in {}.", objectIdKey, Settings.JWKSObjectIdClaimKey.getKey());
				}
			}
		}

		return user;
	}

	private static Principal getPrincipalForAccessTokenWithKeystore(String token, PropertyKey<String> eMailKey) throws FrameworkException {

		Key publicKey = getPublicKeyForToken();
		final Algorithm alg = parseAlgorithm(publicKey.getAlgorithm());
		Map<String, Claim> claims = validateTokenWithKeystore(token, alg);

		if (claims == null) {

			return null;
		}

		Principal user = getPrincipalForTokenClaims(claims, eMailKey, true);
		if (user == null) {

			return null;
		}

		// Check if the access_token is still valid.
		// If access_token isn't valid anymore, then either it timed out, or the user logged out.
		String tokenReference = claimAsString(claims, "tokenId");
		if (validateTokenForUser(tokenReference, user)) {

			return user;
		}

		return null;
	}

	private static Principal getUserForAccessTokenWithSecret(String token, PropertyKey<String> eMailKey) throws FrameworkException {

		final String secret = Settings.JWTSecret.getValue();
		Map<String, Claim> claims = validateTokenWithSecret(token, secret);

		if (claims == null) {

			return null;
		}

		Principal user = getPrincipalForTokenClaims(claims, eMailKey, true);
		if (user == null) {

			return null;
		}

		// Check if the access_token is still valid.
		// If access_token isn't valid anymore, then either it timed out, or the user logged out.
		String tokenReference = claimAsString(claims, "tokenId");
		if (validateTokenForUser(tokenReference, user)) {

			return user;
		}

		return null;
	}

	private static void clearTimedoutRefreshTokens(final Principal user) {

		final String[] refreshTokens = user.getRefreshTokens();
		if (refreshTokens != null) {

			try {

				for (final String refreshToken : refreshTokens) {

					if (refreshTokenTimedOut(refreshToken)) {

						logger.debug("RefreshToken {} timed out", refreshToken);

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

	private static Map<String, String> createTokensForUserWithSecret(Principal user, Date accessTokenExpirationDate, Date refreshTokenExpirationDate, String instanceName) throws FrameworkException {

		// generate and persist a strong secret on first use if none is configured (a present-but-weak
		// secret is left untouched so the strength check below still rejects it)
		final String secret = Settings.getOrGenerateJWTSecret();
		final String jwtIssuer = Settings.JWTIssuer.getValue();

		if (secret.length() < 32) {

			throw new FrameworkException(500, "The configured secret is too weak (must be at least 32 characters) - see " + Settings.JWTSecret.getKey());
		}

		// Check for minimum entropy: reject secrets with fewer than 10 distinct characters
		final long distinctChars = secret.chars().distinct().count();
		if (distinctChars < 10) {

			throw new FrameworkException(500, "The configured secret has insufficient entropy (only " + distinctChars + " distinct characters). Please use a more random secret - see " + Settings.JWTSecret.getKey());
		}

		try {

			final Algorithm alg = Algorithm.HMAC256(secret.getBytes(StandardCharsets.UTF_8));

			return createTokens(user, alg, accessTokenExpirationDate, refreshTokenExpirationDate, instanceName, jwtIssuer);

		} catch (JWTCreationException ex) {

			throw new FrameworkException(500, ex.getMessage(), ex);
		}
	}

	private static Map<String, String> createTokensForUserWithKeystore(Principal user, Date accessTokenExpirationDate, Date refreshTokenExpirationDate, String instanceName) throws FrameworkException {

		RSAPrivateKey privateKey = getPrivateKeyForToken();
		RSAPublicKey publicKey = getPublicKeyForToken();

		if (privateKey == null) {

			throw new FrameworkException(500, "Cannot read private key file");
		}

		final String jwtIssuer = Settings.JWTIssuer.getValue();
		final Algorithm alg = Algorithm.RSA256(publicKey, privateKey);

		return createTokens(user, alg, accessTokenExpirationDate, refreshTokenExpirationDate, instanceName, jwtIssuer);
	}

	private static Map<String, String> createTokens(Principal user, Algorithm alg, Date accessTokenExpirationDate, Date refreshTokenExpirationDate, String instanceName, String jwtIssuer) throws FrameworkException {

		final Map<String, String> tokens = new HashMap<>();
		String tokenId = null;

		if (refreshTokenExpirationDate != null) {

			// create a unique uuid for refresh_token with expiration
			final String newTokenUUID = NodeServiceCommand.getNextUuid();
			StringBuilder tokenStringBuilder = new StringBuilder();

			tokenStringBuilder.append(newTokenUUID).append("_").append(refreshTokenExpirationDate.getTime());

			tokenId = tokenStringBuilder.toString();

			final JWTCreator.Builder refreshBuilder = JWT.create()
				.withSubject(user.getName())
				.withExpiresAt(refreshTokenExpirationDate)
				.withIssuer(jwtIssuer)
				.withClaim("tokenId", tokenId)
				.withClaim("tokenType", "refresh_token");
			applyAudience(refreshBuilder);
			String refreshToken = refreshBuilder.sign(alg);

			user.addRefreshToken(tokenId);
			tokens.put("refresh_token", refreshToken);
		}

		final JWTCreator.Builder accessBuilder = JWT.create()
			.withSubject(user.getName())
			.withExpiresAt(accessTokenExpirationDate)
			.withIssuer(jwtIssuer)
			.withClaim("instance", instanceName)
			.withClaim("uuid", user.getUuid())
			.withClaim("eMail", user.getEMail())
			.withClaim("tokenType", "access_token")
			.withClaim("tokenId", tokenId);
		applyAudience(accessBuilder);
		String accessToken = accessBuilder.sign(alg);

		tokens.put("access_token", accessToken);
		tokens.put("expiration_date", Long.toString(accessTokenExpirationDate.getTime()));

		return tokens;
	}

	private static RSAPrivateKey getPrivateKeyForToken() {

		final String keyStorePath = Settings.JWTKeyStore.getValue();
		final String keyStorePassword = Settings.JWTKeyStorePassword.getValue();
		final String keyAlias = Settings.JWTKeyAlias.getValue();

		try {

			File keyStoreFile = new File(keyStorePath);
			KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());

			keystore.load(new FileInputStream(keyStoreFile), keyStorePassword.toCharArray());

			final RSAPrivateKey privateKey = (RSAPrivateKey) keystore.getKey(keyAlias, keyStorePassword.toCharArray());

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

	private static RSAPublicKey getPublicKeyForToken() {

		final String keyStorePath = Settings.JWTKeyStore.getValue();
		final String keyStorePassword = Settings.JWTKeyStorePassword.getValue();
		final String keyAlias = Settings.JWTKeyAlias.getValue();

		try {

			File keyStoreFile = new File(keyStorePath);
			KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());

			keystore.load(new FileInputStream(keyStoreFile), keyStorePassword.toCharArray());

			Certificate cert = keystore.getCertificate(keyAlias);
			RSAPublicKey publicKey = (RSAPublicKey) cert.getPublicKey();

			return publicKey;

		} catch (Exception e) {

			logger.warn("Error while reading jwt keystore", e);
		}

		return null;
	}

	private static Map<String, Claim> validateTokenWithSecret(String token, String secret) {

		try {

			Algorithm alg = Algorithm.HMAC256(secret.getBytes(StandardCharsets.UTF_8));
			JWTVerifier verifier = buildVerifier(alg);
			DecodedJWT decodedJWT = verifier.verify(token);

			return decodedJWT.getClaims();

		} catch (JWTVerificationException e) {

			logger.debug("Invalid token", e);
		}

		return null;
	}

	private static Map<String, Claim> validateTokenWithKeystore(String token, Algorithm alg) {

		try {

			JWTVerifier verifier = buildVerifier(alg);
			DecodedJWT decodedJWT = verifier.verify(token);

			return decodedJWT.getClaims();

		} catch (JWTVerificationException e) {

			logger.debug("Invalid token", e);
		}

		return null;
	}

	/**
	 * Builds a verifier that enforces the configured JWT issuer (and, when
	 * configured, audience) in addition to the signature and standard
	 * time-based claims checked by auth0-java. Without these checks, any
	 * token signed with the same secret or key would be accepted regardless
	 * of which service issued it or whom it was intended for.
	 */
	public static JWTVerifier buildVerifier(final Algorithm alg) {

		Verification verification = JWT.require(alg);
		final String jwtIssuer = Settings.JWTIssuer.getValue();

		if (StringUtils.isNotBlank(jwtIssuer)) {

			verification = verification.withIssuer(jwtIssuer);
		}

		final String[] audiences = configuredAudiences();
		if (audiences.length > 0) {

			// "any of" semantics: the token is accepted if its aud claim
			// contains at least one of the configured values. Default
			// withAudience requires all of them, which is rarely what a
			// recipient actually wants.
			verification = verification.withAnyOfAudience(audiences);
		}

		return verification.build();
	}

	/**
	 * Returns the configured audience values as a trimmed, empty-filtered
	 * array. An empty array means audience binding is disabled: no claim
	 * is emitted on new tokens and no check is applied on verification.
	 */
	public static String[] configuredAudiences() {

		return audienceList(Settings.JWTAudience.getValue(""));
	}

	/**
	 * Builds the verifier for a token signed by the external identity provider configured in jwks mode,
	 * or null if the instance is not configured to accept such tokens at all.
	 *
	 * <p>Unlike {@link #buildVerifier(Algorithm)}, the audience is not optional here (ticket 1593). A
	 * verifier that checks only the signature and the issuer accepts <b>every</b> token that provider
	 * ever issued - including the ones it issued to a completely different application that happens to
	 * use the same tenant, and, where the provider allows self-registration, the ones it issues to
	 * anybody who signs up there. The
	 * {@code aud} claim is what says "this token was meant for this installation", so without a value to
	 * check it against there is no honest way to accept the token, and Structr refuses instead of
	 * pretending.
	 */
	public static JWTVerifier buildJWKSVerifier(final Algorithm algorithm, final String issuer) {

		final String[] audiences = configuredJWKSAudiences();
		if (audiences.length == 0) {

			logger.error("Refusing a token from the configured JWKS provider because {} is not set. Set it to the audience the provider "
				+ "issues tokens for this installation with - usually the client id of this application at the provider.", Settings.JWKSAudience.getKey());

			return null;
		}

		// "any of": a token is accepted when its aud claim carries at least one of the configured values,
		// which is what a recipient of a multi-audience token actually wants (see buildVerifier)
		Verification verification = JWT.require(algorithm).withAnyOfAudience(audiences);

		if (StringUtils.isNotBlank(issuer)) {

			verification = verification.withIssuer(issuer);

		} else {

			logger.warn("No value for {} while verifying a token from the configured JWKS provider: the issuer of the token is not checked.", Settings.JWTIssuer.getKey());
		}

		return verification.build();
	}

	/**
	 * Returns the audience values accepted for tokens from the external JWKS provider. Kept apart from
	 * {@link #configuredAudiences()} because the two mean different things: that one is the audience this
	 * instance puts into the tokens it issues and checks again when one comes back, this one is the
	 * audience an external provider puts into the tokens it issues for this instance.
	 */
	public static String[] configuredJWKSAudiences() {

		return audienceList(Settings.JWKSAudience.getValue(""));
	}

	/**
	 * Splits a configured, comma-separated audience list. An empty array means the setting is not in use.
	 */
	private static String[] audienceList(final String raw) {

		if (StringUtils.isBlank(raw)) {

			return new String[0];
		}

		return Arrays.stream(raw.split(","))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.toArray(String[]::new);
	}

	/**
	 * Reads a string claim, answering null when the token does not carry it at all.
	 *
	 * <p>DecodedJWT.getClaim() answers a NullClaim for a claim that is not in the token, but the map
	 * from getClaims() is built from the keys the payload actually has, so a missing claim is simply not
	 * in it and Map.get() answers null. Calling isNull() on that is an NPE - and an externally issued
	 * token is under no obligation to carry the claims this instance writes.
	 */
	private static String claimAsString(final Map<String, Claim> claims, final String name) {

		final Claim claim = claims.get(name);

		return claim == null || claim.isNull() ? null : claim.asString();
	}

	/**
	 * Attaches the configured audience list to a token builder when set.
	 * Mutates and returns the same builder (auth0-jwt builders are mutable
	 * fluent builders).
	 */
	public static JWTCreator.Builder applyAudience(final JWTCreator.Builder builder) {

		final String[] audiences = configuredAudiences();
		if (audiences.length > 0) {

			builder.withAudience(audiences);
		}

		return builder;
	}

	private static Algorithm parseAlgorithm(final String algString) {

		switch (algString) {
			case "RSA":

				return Algorithm.RSA256(getPublicKeyForToken(), getPrivateKeyForToken());
		}

		return null;
	}
}
