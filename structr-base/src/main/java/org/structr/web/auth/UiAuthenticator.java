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
package org.structr.web.auth;

import com.github.scribejava.core.model.OAuth2AccessToken;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.AccessMode;
import org.structr.common.PathHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.auth.exception.UnauthorizedException;
import org.structr.core.entity.*;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.auth.JWTHelper;
import org.structr.rest.auth.SessionHelper;
import org.structr.web.auth.provider.*;
import org.structr.web.entity.User;
import org.structr.web.resource.RegistrationResource;
import org.structr.web.servlet.HtmlServlet;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
public class UiAuthenticator implements Authenticator {

	private static final Logger logger = LoggerFactory.getLogger(UiAuthenticator.class.getName());

	protected boolean examined = false;

	private enum Method { GET, PUT, POST, DELETE, HEAD, OPTIONS, PATCH }
	private static final Map<String, Method> methods = new LinkedHashMap();
	private static final Map<String, Map<String,String[]>> stateParameters = new LinkedHashMap<>();

	// HTTP methods
	static {

		methods.put("GET", Method.GET);
		methods.put("PUT", Method.PUT);
		methods.put("POST", Method.POST);
		methods.put("HEAD", Method.HEAD);
		methods.put("PATCH", Method.PATCH);
		methods.put("DELETE", Method.DELETE);
		methods.put("OPTIONS", Method.OPTIONS);

	}

	// access flags
	public static final long FORBIDDEN		= 0;
	public static final long AUTH_USER_GET		= 1;
	public static final long AUTH_USER_PUT		= 2;
	public static final long AUTH_USER_POST		= 4;
	public static final long AUTH_USER_DELETE	= 8;

	public static final long NON_AUTH_USER_GET	= 16;
	public static final long NON_AUTH_USER_PUT	= 32;
	public static final long NON_AUTH_USER_POST	= 64;
	public static final long NON_AUTH_USER_DELETE	= 128;

	public static final long AUTH_USER_OPTIONS	= 256;
	public static final long NON_AUTH_USER_OPTIONS	= 512;

	public static final long AUTH_USER_HEAD		= 1024;
	public static final long NON_AUTH_USER_HEAD	= 2048;

	public static final long AUTH_USER_PATCH	= 4096;
	public static final long NON_AUTH_USER_PATCH	= 8192;

	/**
	 * Examine request and try to find a user.
	 *
	 * First, check session id, then try external (OAuth) authentication,
	 * finally, check standard login by credentials.
	 *
	 * @param request
	 * @param response
	 * @return security context
	 * @throws FrameworkException
	 */
	@Override
	public SecurityContext initializeAndExamineRequest(final HttpServletRequest request, final HttpServletResponse response) throws FrameworkException {

		Principal user = checkExternalAuthentication(request, response);
		SecurityContext securityContext;

		String authorizationToken = getAuthorizationToken(request);

		if (user == null && StringUtils.isBlank(authorizationToken) && !response.isCommitted()) {

			user = SessionHelper.checkSessionAuthentication(request);
		}

		if (user == null && StringUtils.isNotBlank(authorizationToken)) {

			final PropertyKey<String> eMailKey = StructrApp.key(User.class, "eMail");
			user = JWTHelper.getPrincipalForAccessToken(authorizationToken, eMailKey);
		}

		if (user == null) {

			user = getUser(request, true);
		}

		if (user == null) {

			// If no user could be determined, assume frontend access
			securityContext = SecurityContext.getInstance(user, request, AccessMode.Frontend);

		} else {


			if (user instanceof SuperUser) {

				securityContext = SecurityContext.getSuperUserInstance(request);

			} else {

				securityContext = SecurityContext.getInstance(user, request, AccessMode.Backend);
			}
		}

		securityContext.setAuthenticator(this);

		if (StringUtils.isNotBlank(request.getHeader("Origin"))) {

			checkCORS(securityContext, request, response);
		}

		examined = true;

		// store a reference of the response object in SecurityContext
		// to be able to stream data directly from builtin functions
		securityContext.setResponse(response);

		// expose Structr edition
		response.setHeader("X-Structr-Edition", Services.getInstance().getEdition());

		// expose cluster node replica number
		if (Settings.ClusterModeEnabled.getValue(false) == true) {

			response.setHeader("X-Structr-Cluster-Node", Services.getInstance().getNodeName());
		}

		return securityContext;
	}

	@Override
	public boolean hasExaminedRequest() {
		return examined;
	}

	public void checkCORS(final SecurityContext securityContext, final HttpServletRequest request, final HttpServletResponse response) throws FrameworkException {

		// Check CORS settings (Cross-origin resource sharing, see http://en.wikipedia.org/wiki/Cross-origin_resource_sharing)
		final String origin           = request.getHeader("Origin");
		final String requestedHeaders = request.getHeader("Access-Control-Request-Headers");
		final String requestedMethod  = request.getHeader("Access-Control-Request-Method");
		final String requestUri       = request.getRequestURI();

		String acceptedOriginsString  = (String)  getEffectiveCorsSettingValue(request, Settings.AccessControlAcceptedOrigins.getKey(),  CorsSetting.acceptedOrigins);
		final Integer maxAge          = (Integer) getEffectiveCorsSettingValue(request, Settings.AccessControlMaxAge.getKey(),           CorsSetting.maxAge);
		final String allowMethods     = (String)  getEffectiveCorsSettingValue(request, Settings.AccessControlAllowMethods.getKey(),     CorsSetting.allowMethods);
		final String allowHeaders     = (String)  getEffectiveCorsSettingValue(request, Settings.AccessControlAllowHeaders.getKey(),     CorsSetting.allowHeaders);
		final String allowCredentials = (String)  getEffectiveCorsSettingValue(request, Settings.AccessControlAllowCredentials.getKey(), CorsSetting.allowCredentials);
		final String exposeHeaders    = (String)  getEffectiveCorsSettingValue(request, Settings.AccessControlExposeHeaders.getKey(),    CorsSetting.exposeHeaders);

		final List<String> acceptedOrigins = Arrays.stream(acceptedOriginsString.split(",")).map(String::trim).collect(Collectors.toList());
		final boolean wildcardAllowed = acceptedOrigins.contains("*");

		if (acceptedOrigins.contains(origin) || wildcardAllowed) {

			// Respond with wildcard "*" only for non-credentialed requests (user == null)
			if (wildcardAllowed && securityContext.getUser(false) == null && !StringUtils.equalsIgnoreCase(allowCredentials, "true")) {
				response.setHeader("Access-Control-Allow-Origin",  "*");
			} else {
				response.setHeader("Access-Control-Allow-Origin",  origin);
				response.addHeader("Vary", "Origin");
			}

			if (maxAge != null) {
				response.setHeader("Access-Control-Max-Age", maxAge.toString());
			}

			if (StringUtils.isNotBlank(requestedHeaders) && StringUtils.isNotBlank(requestedMethod)) {

				// CORS-preflight request, see https://fetch.spec.whatwg.org/#cors-preflight-request

				if (StringUtils.isNotBlank(allowMethods)) {
					response.setHeader("Access-Control-Allow-Methods", allowMethods);
				}

				if (StringUtils.isNotBlank(allowHeaders)) {
					response.setHeader("Access-Control-Allow-Headers", allowHeaders);
				}
			}

			if (StringUtils.isNotBlank(allowCredentials)) {
				response.setHeader("Access-Control-Allow-Credentials", allowCredentials);
			}

			if (StringUtils.isNotBlank(exposeHeaders)) {
				response.setHeader("Access-Control-Expose-Headers", exposeHeaders);
			}

		}
	}


	@Override
	public void checkResourceAccess(final SecurityContext securityContext, final HttpServletRequest request, final String rawResourceSignature, final String propertyView) throws FrameworkException {

		final Principal user    = securityContext.getUser(false);
		final boolean validUser = (user != null);

		// super user is always authenticated
		if (validUser && (user instanceof SuperUser || user.isAdmin())) {
			return;
		}

		// only necessary for non-admin users!
		final List<ResourceAccess> grants = ResourceAccess.findGrants(securityContext, rawResourceSignature);
		final Method method               = methods.get(request.getMethod());

		// flatten grants
		long combinedFlags = 0;
		int grantsFound    = 0;

		if (grants != null) {

			// combine allowed flags for grants user is allowed to see
			for (final ResourceAccess grant : grants) {

				if (securityContext.isReadable(grant, false, false)) {

					grantsFound++;
					combinedFlags = combinedFlags | grant.getFlags();
				}
			}
		}

		// no grants => no access rights
		if (grantsFound == 0) {

			final String userInfo     = (validUser ? "user '" + user.getName() + "'" : "anonymous users");
			final String errorMessage = "Found no resource access grant for " + userInfo + " with signature '" + rawResourceSignature + "' and method '" + method + "' (URI: " + securityContext.getCompoundRequestURI() + ").";
			final Map eventLogMap     = (validUser ? Map.of("raw", rawResourceSignature, "method", method, "validUser", validUser, "userName", user.getName()) : Map.of("raw", rawResourceSignature, "method", method, "validUser", validUser));

			logger.info(errorMessage);
			RuntimeEventLog.resourceAccess("No grant", eventLogMap);

			TransactionCommand.simpleBroadcastGenericMessage(Map.of(
				"type", "RESOURCE_ACCESS",
				"message", errorMessage,
				"uri", securityContext.getCompoundRequestURI(),
				"signature", rawResourceSignature,
				"method", method,
				"validUser", validUser
			));

			throw new UnauthorizedException("Access denied");

		} else if (method != null) {

			switch (method) {

				case GET :

					if (!validUser && ResourceAccess.hasFlag(NON_AUTH_USER_GET, combinedFlags)) {
						return;
					}

					if (validUser && ResourceAccess.hasFlag(AUTH_USER_GET, combinedFlags)) {
						return;
					}

					break;

				case PUT :

					if (!validUser && ResourceAccess.hasFlag(NON_AUTH_USER_PUT, combinedFlags)) {
						return;
					}

					if (validUser && ResourceAccess.hasFlag(AUTH_USER_PUT, combinedFlags)) {
						return;
					}

					break;

				case POST :

					if (!validUser && ResourceAccess.hasFlag(NON_AUTH_USER_POST, combinedFlags)) {
						return;
					}

					if (validUser && ResourceAccess.hasFlag(AUTH_USER_POST, combinedFlags)) {
						return;
					}

					break;

				case DELETE :

					if (!validUser && ResourceAccess.hasFlag(NON_AUTH_USER_DELETE, combinedFlags)) {
						return;
					}

					if (validUser && ResourceAccess.hasFlag(AUTH_USER_DELETE, combinedFlags)) {
						return;
					}

					break;

				case OPTIONS :

					if (!validUser && ResourceAccess.hasFlag(NON_AUTH_USER_OPTIONS, combinedFlags)) {
						return;
					}

					if (validUser && ResourceAccess.hasFlag(AUTH_USER_OPTIONS, combinedFlags)) {
						return;
					}

					break;

				case HEAD :

					if (!validUser && ResourceAccess.hasFlag(NON_AUTH_USER_HEAD, combinedFlags)) {
						return;
					}

					if (validUser && ResourceAccess.hasFlag(AUTH_USER_HEAD, combinedFlags)) {
						return;
					}

					break;

				case PATCH :

					if (!validUser && ResourceAccess.hasFlag(NON_AUTH_USER_PATCH, combinedFlags)) {
						return;
					}

					if (validUser && ResourceAccess.hasFlag(AUTH_USER_PATCH, combinedFlags)) {
						return;
					}

					break;
			}

		} else {

			logger.warn("Unknown method {}, cannot determine resource access.", request.getMethod());
		}

		final String userInfo     = (validUser ? "user '" + user.getName() + "'" : "anonymous users");
		final Map eventLogMap     = (validUser ? Map.of("raw", rawResourceSignature, "method", method, "validUser", validUser, "userName", user.getName()) : Map.of("raw", rawResourceSignature, "method", method, "validUser", validUser));
		final String errorMessage = "Found " + grantsFound + " resource access grant" + (grantsFound > 1 ? "s" : "") + " for " + userInfo + " and signature '" + rawResourceSignature + "' (URI: " + securityContext.getCompoundRequestURI() + "), but method '" + method + "' not allowed in any of them.";

		logger.info(errorMessage);

		RuntimeEventLog.resourceAccess("Method not allowed", eventLogMap);

		TransactionCommand.simpleBroadcastGenericMessage(Map.of(
			"type", "RESOURCE_ACCESS",
			"message", errorMessage,
			"uri", securityContext.getCompoundRequestURI(),
			"signature", rawResourceSignature,
			"method", method,
			"validUser", validUser
		));

		throw new UnauthorizedException("Access denied");
	}

	@Override
	public Principal doLogin(final HttpServletRequest request, final String emailOrUsername, final String password) throws AuthenticationException, FrameworkException {

		final PropertyKey<String> confKey  = StructrApp.key(User.class, "confirmationKey");
		final PropertyKey<String> eMailKey = StructrApp.key(User.class, "eMail");
		final Principal user               = AuthHelper.getPrincipalForPassword(eMailKey, emailOrUsername, password);

		if  (user != null) {

			Services.getInstance().broadcastLogin(user);

			final boolean allowLoginBeforeConfirmation = Settings.RegistrationAllowLoginBeforeConfirmation.getValue();
			if (user.getProperty(confKey) != null && !allowLoginBeforeConfirmation) {

				logger.warn("Login as '{}' not allowed before confirmation.", user.getName());
				RuntimeEventLog.failedLogin("Login attempt before confirmation", Map.of("id", user.getUuid(), "name", user.getName()));
				throw new AuthenticationException(AuthHelper.STANDARD_ERROR_MSG);
			}
		}

		return user;
	}

	@Override
	public void doLogout(final HttpServletRequest request) {

		try {
			final Principal user = getUser(request, false);
			if (user != null) {

				Services.getInstance().broadcastLogout(user);

				AuthHelper.doLogout(request, user);
			}

			final String sessionId = request.getRequestedSessionId();
			if (sessionId != null) {

				SessionHelper.invalidateSession(sessionId);
			}

		} catch (IllegalStateException | FrameworkException ex) {

			logger.warn("Error while logging out user", ex);
		}
	}

	private String getAuthorizationToken(HttpServletRequest request) {

		final Cookie[] cookies = request.getCookies();

		// first check for token in cookie
		if (cookies != null) {

			for (Cookie cookie : request.getCookies()) {

				if (StringUtils.equals(cookie.getName(), "access_token")) {

					return cookie.getValue();
				}
			}
		}

		final String authorizationHeader = request.getHeader("Authorization");

		if (authorizationHeader == null) {
			return null;
		}

		String[] headerParts = authorizationHeader.split(" ");
		if (StringUtils.equals(headerParts[0], "Bearer") && headerParts.length > 1) {

			return headerParts[1];

		} else {

			return null;
		}
	}

	/**
	 * Get effective CORS setting
	 */
	private <T> Object getEffectiveCorsSettingValue(final HttpServletRequest request, final String corsAttributeKey, final PropertyKey<T> corsSettingPropertyKey) throws FrameworkException {

		// Default is value from Settings
		Object effectiveCorsSettingValue = Settings.getSetting(corsAttributeKey).getValue();

		logger.debug("Settings value for {} from config: {}", corsAttributeKey, effectiveCorsSettingValue);

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final String requestUri       = request.getRequestURI();
			CorsSetting corsSettingObjectFromDatabase = StructrApp.getInstance().nodeQuery(CorsSetting.class).and(CorsSetting.requestUri, requestUri).getFirst();

			if (corsSettingObjectFromDatabase != null) {

				final Object corsSettingValueFromDatabase = corsSettingObjectFromDatabase.getProperty(corsSettingPropertyKey);

				if (corsSettingValueFromDatabase != null) {
					// Overwrite config setting
					effectiveCorsSettingValue = corsSettingValueFromDatabase;
				}
				logger.debug("Settings value for {} from database: {}", corsAttributeKey, effectiveCorsSettingValue);
			}

			tx.success();
			return effectiveCorsSettingValue;

		}  catch (FrameworkException t) {

			logger.error("Exception while processing request", t);
		}

		return null;
	}

	/**
	 * This method checks all configured external authentication services.
	 *
	 * @param request
	 * @param response
	 * @return user
	 */
	protected Principal checkExternalAuthentication(final HttpServletRequest request, final HttpServletResponse response) throws FrameworkException {

		final String path = PathHelper.clean(request.getPathInfo());
		final String[] uriParts = PathHelper.getParts(path);

		logger.debug("Checking external authentication ...");

		if (uriParts == null || uriParts.length != 3 || !("oauth".equals(uriParts[0]))) {

			logger.debug("No OAuth keywords in URI, ignoring. (needs /oauth/<name>/<action>)");
			return null;
		}

		final String name   = uriParts[1];
		final String action = uriParts[2];

		OAuth2Client oAuth2Client = null;

		switch (name) {
			case "auth0":
				oAuth2Client = new Auth0AuthClient(request);
				break;
			case "facebook":
				oAuth2Client = new FacebookAuthClient(request);
				break;
			case "github":
				oAuth2Client = new GithubAuthClient(request);
				break;
			case "google":
				oAuth2Client = new GoogleAuthClient(request);
				break;
			case "linkedin":
				oAuth2Client = new LinkedInAuthClient(request);
				break;
			case "azure":
				oAuth2Client = new AzureAuthClient(request);
				break;
			default:
				logger.error("Could not initialize oAuth2Client for provider {}", name);
				return null;
		}

		if ("login".equals(action)) {

			try {

				final String state = NodeServiceCommand.getNextUuid();
				stateParameters.put(state, request.getParameterMap());
				response.sendRedirect(oAuth2Client.getAuthorizationURL(state));

				return null;

			} catch (Exception ex) {

				logger.error("Could not send redirect to authorization server.", ex);
			}

		} else if ("logout".equals(action)) {

			try {

				final String logoutURI = oAuth2Client.getLogoutURI();
				if (logoutURI != null && logoutURI.length() > 0) {

					response.sendRedirect(logoutURI);
				}

				return null;

			} catch (Exception ex) {

				logger.error("Could not send redirect to logout endpoint", ex);
			}

		} else if ("auth".equals(action)) {

			final String[] codes = request.getParameterMap().get("code");
			final String code = codes != null && codes.length == 1 ? codes[0] : null;
			final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();

			final OAuth2AccessToken accessToken = oAuth2Client.getAccessToken(code);
			if (accessToken != null) {

				logger.debug("Got access token {}", accessToken);

				String value = oAuth2Client.getClientCredentials(accessToken);

				logger.debug("Got credential value: {}", value);

				if (value != null) {

					final PropertyKey credentialKey = StructrApp.key(User.class, "eMail");

					logger.debug("Fetching user with {} {}", credentialKey, value);

					// first try: literal, unchanged value from oauth provider
					Principal user = AuthHelper.getPrincipalForCredential(credentialKey, value);
					if (user == null) {

						// since e-mail addresses are stored in lower case, we need
						// to search for users with lower-case e-mail address value..
						logger.debug("2nd try: fetching user with lowercase {} {}", credentialKey, value.toLowerCase());

						// second try: lowercase value
						user = AuthHelper.getPrincipalForCredential(credentialKey, value.toLowerCase());
					}

					if (user == null) {

						if (Settings.RestUserAutocreate.getValue()) {

							logger.debug("No user found, creating new user for {} {}.", credentialKey, value);

							user = RegistrationResource.createUser(superUserContext, credentialKey, value, true, getUserClass(), null);

							// let oauth implementation augment user info
							oAuth2Client.initializeAutoCreatedUser(user);

							RuntimeEventLog.registration("OAuth user created", Map.of("id", user.getUuid(), "name", user.getName()));

						} else {

							logger.debug("No user found, but jsonrestservlet.user.autocreate is false, so I'm not allowed to create a new user for {} {}.", credentialKey, value);
						}
					}

					if (user != null) {

						try {

							// get the original request state and add the parameters to the redirect page
							final String originalRequestState = request.getParameter("state");
							Map<String, String[]> originalRequestParameters = stateParameters.get(originalRequestState);
							stateParameters.remove(originalRequestState);

							Boolean isTokenLogin = false;

							URIBuilder uriBuilder = new URIBuilder();
							if (originalRequestParameters != null) {
								for (Map.Entry<String, String[]> entry : originalRequestParameters.entrySet()) {
									for (String parameterEntry : entry.getValue()) {

										final String entryKey = entry.getKey();

										if (StringUtils.equals(entryKey, "createTokens") && StringUtils.equals(parameterEntry, "true")) {
											isTokenLogin = true;

											Map<String, String> tokenMap = JWTHelper.createTokensForUser(user);

											uriBuilder.addParameter("access_token", tokenMap.get("access_token"));
											uriBuilder.addParameter("refresh_token", tokenMap.get("refresh_token"));
										} else {

											uriBuilder.addParameter(entry.getKey(), parameterEntry);
										}
									}
								}
							}

							logger.debug("Logging in user {}", user);

							if(!isTokenLogin) {
								AuthHelper.doLogin(request, user);
							}
							HtmlServlet.setNoCacheHeaders(response);

							oAuth2Client.invokeOnLoginMethod(user);

							logger.debug("HttpServletResponse status: {}", response.getStatus());

							final String configuredReturnUri = oAuth2Client.getReturnURI();
							if (StringUtils.startsWith(configuredReturnUri, "http")) {

								URI redirectUri = new URI(configuredReturnUri);
								uriBuilder.setHost(redirectUri.getHost());
								uriBuilder.setPath(redirectUri.getPath());
								uriBuilder.setPort(redirectUri.getPort());
								uriBuilder.setScheme(redirectUri.getScheme());

							} else {
								uriBuilder.setPath(configuredReturnUri);
							}

							response.resetBuffer();
							response.setHeader("Location", Settings.ApplicationRootPath.getValue() + uriBuilder.build().toString());
							response.setStatus(HttpServletResponse.SC_FOUND);
							response.flushBuffer();

						} catch (IOException | URISyntaxException ex) {

							logger.error("Could not redirect", ex);
						}

						return user;

					} else {

						logger.debug("Still no valid user found, no oauth authentication possible.");
					}
				}
			}
		}

		try {

			response.sendRedirect(oAuth2Client.getErrorURI());

		} catch (IOException ex) {

			logger.error("Could not redirect to {}: {}", new Object[]{ oAuth2Client.getErrorURI(), ex });
		}


		return null;
	}

	public static void writeUnauthorized(final HttpServletResponse response) throws IOException {

		response.setHeader("WWW-Authenticate", "BASIC realm=\"Restricted Access\"");
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
	}

	public static void writeNotFound(final HttpServletResponse response) throws IOException {
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	public static void writeInternalServerError(final HttpServletResponse response) {
		try {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		} catch (IOException ioex) {
			logger.warn("Unable to send error response: {}", ioex.getMessage());
		}
	}

	public static void writeFrameworkException(final HttpServletResponse response, final FrameworkException fex) {
		try {
			response.sendError(fex.getStatus());
		} catch (IOException ioex) {
			logger.warn("Unable to send error response: {}", ioex.getMessage());
		}
	}

	@Override
	public Principal getUser(final HttpServletRequest request, final boolean tryLogin) throws FrameworkException {

		Principal user = null;

		String authorizationToken = getAuthorizationToken(request);

		if ((authorizationToken == null || StringUtils.equals(authorizationToken, "")) && request.getAttribute(SessionHelper.SESSION_IS_NEW) == null) {

			// First, check session (JSESSIONID cookie)
			if (request.getSession(false) != null) {

				user = AuthHelper.getPrincipalForSessionId(request.getSession(false).getId());
			}

		} else if (authorizationToken != null) {

			final PropertyKey<String> eMailKey = StructrApp.key(User.class, "eMail");
			user = JWTHelper.getPrincipalForAccessToken(authorizationToken, eMailKey);
		}

		if (user == null) {

			// Second, check X-Headers
			String userName = request.getHeader("X-User");
			String password = request.getHeader("X-Password");
			String token    = request.getHeader("X-StructrSessionToken");

			// Try to authorize with a session token first
			if (token != null) {

				user = AuthHelper.getPrincipalForSessionId(token);

			} else if ((userName != null) && (password != null)) {

				if (tryLogin) {

					try {

						user = AuthHelper.getPrincipalForPassword(AbstractNode.name, userName, password);

					} catch (AuthenticationException ex) {

						final PropertyKey<String> eMailKey = StructrApp.key(User.class, "eMail");
						user = AuthHelper.getPrincipalForPassword(eMailKey, userName, password);
					}
				}
			}
		}

		return user;
	}

	@Override
	public Class getUserClass() {

		String configuredCustomClassName = Settings.RegistrationCustomUserClass.getValue();
		if (StringUtils.isEmpty(configuredCustomClassName)) {

			configuredCustomClassName = User.class.getSimpleName();
		}

		return StructrApp.getConfiguration().getNodeEntityClass(configuredCustomClassName);
	}
}
