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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.property.PropertyKey;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.*;

public class JWTHelper {

    public static final String TOKEN_ERROR_MSG = "The given access_token or refresh_token is invalid";
    private static final Logger logger         = LoggerFactory.getLogger(AuthHelper.class.getName());

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

                final String provider = Settings.JWTSProvider.getValue();

                break;
        }

        return user;
    }

    public static Principal getPrincipalForRefreshToken(final String refreshToken) throws FrameworkException {

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
            return null;
        }

        final String tokenId = (String) claims.getBody().get("tokenId");
        final String tokenType = (String) claims.getBody().get("tokenType");
        if (tokenId == null || tokenType == null || !StringUtils.equals(tokenType, "refresh_token")) {
            return null;

        }

        Principal user = AuthHelper.getPrincipalForCredential(StructrApp.key(Principal.class, "refreshTokens"), new String[]{ tokenId }, false);

        if (user == null) {
            return null;
        }

        Principal.removeRefreshToken(user, tokenId);
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

    private static boolean validateTokenForUser(String tokenId, Principal user) {

        // if tokenId is empty, token was created without refresh_token
        if (StringUtils.isEmpty(tokenId)) {
            return true;
        }

        final PropertyKey<String[]> key = StructrApp.key(Principal.class, "refreshTokens");
        final String[] refreshTokens = user.getProperty(key);

        return Arrays.asList(refreshTokens).contains(tokenId);
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
            return null;
        }

        Principal user = getPrincipalForTokenClaims(jws.getBody(), eMailKey);

        // Check if the access_token is still valid.
        // If access_token isn't valid anymore, then either it timed out, or the user logged out.
        String tokenReference = jws.getBody().get("tokenId", String.class);
        if (validateTokenForUser(tokenReference, user)) {

            return user;
        }

        return null;
    }

    private static Principal getUserForAccessTokenWithSecret(String token, PropertyKey<String> eMailKey) throws FrameworkException {

        final String secret = Settings.JWTSecret.getValue();

        Jws<Claims> jws = validateTokenWithSecret(token, secret);

        if (jws == null) {
            return null;
        }

        Principal user =  getPrincipalForTokenClaims(jws.getBody(), eMailKey);

        // Check if the access_token is still valid.
        // If access_token isn't valid anymore, then either it timed out, or the user logged out.
        String tokenReference = jws.getBody().get("tokenId", String.class);
        if (validateTokenForUser(tokenReference, user)) {

            return user;
        }

        return null;
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

    private static Map<String, String> createTokensForUserWithSecret(Principal user, Date accessTokenExpirationDate, Date refreshTokenExpirationDate, String instanceName) throws FrameworkException {

        final String secret = Settings.JWTSecret.getValue();
        final String jwtIssuer = Settings.JWTIssuer.getValue();

        try {

            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            return createTokens(user, key, accessTokenExpirationDate, refreshTokenExpirationDate, instanceName, jwtIssuer);

        } catch (WeakKeyException ex) {

            throw new FrameworkException(500, "The configured secret is too weak (must be at least 32 characters)");
        }
    }

    private static Map<String, String> createTokensForUserWithKeystore(Principal user, Date accessTokenExpirationDate, Date refreshTokenExpirationDate, String instanceName) throws FrameworkException {

        Key privateKey = getPrivateKeyForToken();

        if (privateKey == null) {

            throw new FrameworkException(500, "Cannot read private key file");
        }

        final String jwtIssuer = Settings.JWTIssuer.getValue();

        return createTokens(user, privateKey, accessTokenExpirationDate, refreshTokenExpirationDate, instanceName, jwtIssuer);
    }

    private static Map<String, String> createTokens (Principal user, Key key, Date accessTokenExpirationDate, Date refreshTokenExpirationDate, String instanceName, String jwtIssuer) throws FrameworkException {
        final  Map<String, String> tokens = new HashMap<>();

        // create a unique uuid for refresh_token with expiration
        final String newTokenUUID = NodeServiceCommand.getNextUuid();
        StringBuilder tokenStringBuilder = new StringBuilder();
        tokenStringBuilder.append(newTokenUUID).append("_").append(refreshTokenExpirationDate.getTime());
        final String tokenId = tokenStringBuilder.toString();

        if (refreshTokenExpirationDate != null) {
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

        String accessToken = Jwts.builder()
                .setSubject(user.getName())
                .setExpiration(accessTokenExpirationDate)
                .setIssuer(jwtIssuer)
                .claim("instance", instanceName)
                .claim("uuid", user.getUuid())
                .claim("eMail", user.getEMail())
                .claim("tokenType", "access_token")
                .claim("tokenId", tokenId)
                .signWith(key)
                .compact();

        tokens.put("access_token", accessToken);
        tokens.put("expiration_date", Long.toString(accessTokenExpirationDate.getTime()));

        return tokens;
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
