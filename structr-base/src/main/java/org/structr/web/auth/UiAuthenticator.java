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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.common.helper.PathHelper;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.ServicePrincipal;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.auth.exception.OAuthException;
import org.structr.core.auth.exception.UnauthorizedException;
import org.structr.core.entity.Principal;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.CorsSettingTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.traits.wrappers.ResourceAccessTraitWrapper;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.auth.JWTHelper;
import org.structr.rest.auth.SessionHelper;
import org.structr.web.auth.provider.*;
import org.structr.web.entity.User;
import org.structr.web.resource.RegistrationResourceHandler;
import org.structr.web.servlet.HtmlServlet;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class UiAuthenticator implements Authenticator {

	private static final Logger logger = LoggerFactory.getLogger(UiAuthenticator.class.getName());

	private static final Map<String, Map<String,String[]>> stateParameters = new HashMap<>();
	private static final Map<String, Method> methods                       = new HashMap();

	protected boolean examined = false;

	private enum Method { GET, PUT, POST, DELETE, HEAD, OPTIONS, PATCH }

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

	private static final String BACKEND_SSO_LOGIN_INDICATOR = "isBackendOAuthLogin";

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

		// prefetch
		/*
		TransactionCommand.getCurrentTransaction().prefetch(
			"(p1:PrincipalInterface)-[r:CONTAINS*0..1]-(p2:PrincipalInterface)",
			Set.of("PrincipalInterface/all/OUTGOING/CONTAINS", "Group/all/OUTGOING/CONTAINS"),
			Set.of("PrincipalInterface/all/INCOMING/CONTAINS", "Group/all/INCOMING/CONTAINS")
		);

		 */

		Principal user = checkExternalAuthentication(request, response);
		SecurityContext securityContext;

		String authorizationToken = getAuthorizationToken(request);

		if (user == null && StringUtils.isBlank(authorizationToken) && !response.isCommitted()) {

			user = SessionHelper.checkSessionAuthentication(request);
		}

		if (user == null && StringUtils.isNotBlank(authorizationToken)) {

			final PropertyKey<String> eMailKey = Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.EMAIL_PROPERTY);
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

				// overwrite superuser context in user
				user.setSecurityContext(securityContext);
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
		if (Settings.ClusterModeEnabled.getValue(false)) {

			response.setHeader("X-Structr-Cluster-Node", Services.getInstance().getNodeName());
		}

		return securityContext;
	}

	@Override
	public boolean hasExaminedRequest() {
		return examined;
	}

	public void checkCORS(final SecurityContext securityContext, final HttpServletRequest request, final HttpServletResponse response) throws FrameworkException {

		final Traits traits = Traits.of(StructrTraits.CORS_SETTING);

		// Check CORS settings (Cross-origin resource sharing, see http://en.wikipedia.org/wiki/Cross-origin_resource_sharing)
		final String origin           = request.getHeader("Origin");
		final String requestedHeaders = request.getHeader("Access-Control-Request-Headers");
		final String requestedMethod  = request.getHeader("Access-Control-Request-Method");
		final String requestUri       = request.getRequestURI();

		String acceptedOriginsString  = Settings.AccessControlAcceptedOrigins.getValue();
		Integer maxAge                = Settings.AccessControlMaxAge.getValue();
		String allowMethods           = Settings.AccessControlAllowMethods.getValue();
		String allowHeaders           = Settings.AccessControlAllowHeaders.getValue();
		String allowCredentials       = Settings.AccessControlAllowCredentials.getValue();
		String exposeHeaders          = Settings.AccessControlExposeHeaders.getValue();

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final NodeInterface corsSettingObjectFromDatabase = StructrApp.getInstance().nodeQuery(StructrTraits.CORS_SETTING).key(traits.key(CorsSettingTraitDefinition.REQUEST_URI_PROPERTY), requestUri).getFirst();
			if (corsSettingObjectFromDatabase != null) {

				acceptedOriginsString = (String)  getEffectiveCorsSettingValue(corsSettingObjectFromDatabase, CorsSettingTraitDefinition.ACCEPTED_ORIGINS_PROPERTY,  acceptedOriginsString);
				maxAge                = (Integer) getEffectiveCorsSettingValue(corsSettingObjectFromDatabase, CorsSettingTraitDefinition.MAX_AGE_PROPERTY,           maxAge);
				allowMethods          = (String)  getEffectiveCorsSettingValue(corsSettingObjectFromDatabase, CorsSettingTraitDefinition.ALLOW_METHODS_PROPERTY,     allowMethods);
				allowHeaders          = (String)  getEffectiveCorsSettingValue(corsSettingObjectFromDatabase, CorsSettingTraitDefinition.ALLOW_HEADERS_PROPERTY,     allowHeaders);
				allowCredentials      = (String)  getEffectiveCorsSettingValue(corsSettingObjectFromDatabase, CorsSettingTraitDefinition.ALLOW_CREDENTIALS_PROPERTY, allowCredentials);
				exposeHeaders         = (String)  getEffectiveCorsSettingValue(corsSettingObjectFromDatabase, CorsSettingTraitDefinition.EXPOSE_HEADERS_PROPERTY,    exposeHeaders);
			}

			tx.success();

		}  catch (FrameworkException t) {

			logger.error("Exception while processing request", t);
		}

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

		final Principal user             = securityContext.getUser(false);
		final boolean validUser          = (user != null);

		// super user is always authenticated
		if (validUser && (user instanceof SuperUser || user.isAdmin())) {
			return;
		}

		// only necessary for non-admin users!
		final List<ResourceAccess> permissions = ResourceAccessTraitWrapper.findPermissions(securityContext, rawResourceSignature);
		final Method method                    = methods.get(request.getMethod());

		// flatten permissons
		long combinedFlags   = 0;
		int permissionsFound = 0;

		if (permissions != null) {

			// combine allowed flags for permissions user is allowed to see
			for (final ResourceAccess permission : permissions) {

				if (securityContext.isReadable(permission, false, false)) {

					permissionsFound++;
					combinedFlags = combinedFlags | permission.getFlags();
				}
			}
		}

		// no permissions => no access rights
		if (permissionsFound == 0) {

			final boolean isServicePrincipal = validUser && (user instanceof ServicePrincipal);

			final String userInfo     = (validUser ? (isServicePrincipal ? "service principal '" + user.getName() + "'" : "user '" + user.getName() + "'") : "anonymous users");
			final String errorMessage = "Found no resource access permission for " + userInfo + " with signature '" + rawResourceSignature + "' and method '" + method + "' (URI: " + securityContext.getCompoundRequestURI() + ").";
			final Map eventLogMap     = new HashMap(Map.of("raw", rawResourceSignature, "method", method, "validUser", validUser, "isServicePrincipal", isServicePrincipal));
			if (validUser) {
				eventLogMap.put("userName", user.getName());
			}

			logger.info(errorMessage);
			RuntimeEventLog.resourceAccess("No permission", eventLogMap);

			TransactionCommand.simpleBroadcastGenericMessage(Map.of(
				"type",           "RESOURCE_ACCESS",
				"message",            errorMessage,
				"uri",                securityContext.getCompoundRequestURI(),
				"signature",          rawResourceSignature,
				"method",             method,
				"validUser",          validUser,
				"isServicePrincipal", isServicePrincipal,
				"userid",             (validUser ? user.getUuid() : ""),
				"username",           (validUser ? user.getName() : "")
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
		final String errorMessage = "Found " + permissionsFound + " resource access permission" + (permissionsFound > 1 ? "s" : "") + " for " + userInfo + " and signature '" + rawResourceSignature + "' (URI: " + securityContext.getCompoundRequestURI() + "), but method '" + method + "' not allowed in any of them.";

		logger.info(errorMessage);

		RuntimeEventLog.resourceAccess("Method not allowed", eventLogMap);

		TransactionCommand.simpleBroadcastGenericMessage(Map.of(
			"type",  "RESOURCE_ACCESS",
			"message",   errorMessage,
			"uri",       securityContext.getCompoundRequestURI(),
			"signature", rawResourceSignature,
			"method",    method,
			"validUser", validUser,
			"userid",    (validUser ? user.getUuid() : ""),
			"username",  (validUser ? user.getName() : "")
		));

		throw new UnauthorizedException("Access denied");
	}

	@Override
	public Principal doLogin(final HttpServletRequest reqt, final String userProvidedValueForAuthenticationKey, final String password) throws FrameworkException {

		// Default is eMail
		final PropertyKey<String> defaultAuthenticationPropertyKey = Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.EMAIL_PROPERTY);

		final Set<PropertyKey<String>> authenticationPropertyKeySet = new HashSet<>();
		authenticationPropertyKeySet.add(defaultAuthenticationPropertyKey);

		final String authenticationPropertyKeysSetting = Settings.AuthenticationPropertyKeys.getValue();

		if (StringUtils.isNotBlank(authenticationPropertyKeysSetting)) {

			final List<String> authenticationPropertyKeys = Arrays.asList(StringUtils.split(authenticationPropertyKeysSetting, " "));

			for (final String key : authenticationPropertyKeys) {

				final String[] typeAndKey = StringUtils.split(key, ".");

				if (typeAndKey.length == 2 && Traits.exists(typeAndKey[0])) {

					final PropertyKey<String> authenticationPropertyKey = Traits.of(typeAndKey[0]).key(typeAndKey[1]);
					authenticationPropertyKeySet.add(authenticationPropertyKey);
				}
			}
		}

		final Principal user = AuthHelper.getPrincipalForKeysAndPassword(authenticationPropertyKeySet, userProvidedValueForAuthenticationKey, password);

		if  (user != null) {

			final boolean allowLoginBeforeConfirmation = Settings.RegistrationAllowLoginBeforeConfirmation.getValue();
			if (user.is(StructrTraits.USER) && user.as(User.class).getConfirmationKey() != null && !allowLoginBeforeConfirmation) {

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

				Services.getInstance().broadcastLogout(user.getNode().getId().getId());

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
	private <T> Object getEffectiveCorsSettingValue(final NodeInterface corsSettingObjectFromDatabase, final String corsSettingPropertyKey, final T defaultValue) throws FrameworkException {

		if (corsSettingObjectFromDatabase != null) {

			final Traits traits                       = Traits.of(StructrTraits.CORS_SETTING);
			final Object corsSettingValueFromDatabase = corsSettingObjectFromDatabase.getProperty(traits.key(corsSettingPropertyKey));

			if (corsSettingValueFromDatabase != null) {

				// Overwrite config setting
				return corsSettingValueFromDatabase;
			}
		}

		return defaultValue;
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

		try {

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

					logger.error("Unable to initialize oAuth2Client for provider {}", name);
					return null;
			}

		} catch (IllegalArgumentException iae) {

			// always throw exception, but handle it silently for backend SSO probes
			final boolean treatSilently = (request.getParameter(BACKEND_SSO_LOGIN_INDICATOR) != null);

			throw new OAuthException("Unable to initialize OAuth client '" + name + "': " + iae.getMessage(), treatSilently);
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

					final PropertyKey credentialKey = Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.EMAIL_PROPERTY);

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

							user = RegistrationResourceHandler.createUser(superUserContext, credentialKey, value, true, getUserClass(), null);

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

								for (final Map.Entry<String, String[]> entry : originalRequestParameters.entrySet()) {

									for (final String parameterEntry : entry.getValue()) {

										final String entryKey = entry.getKey();

										if (StringUtils.equals(entryKey, "createTokens") && StringUtils.equals(parameterEntry, "true")) {

											isTokenLogin = true;

											final Map<String, String> tokenMap = JWTHelper.createTokensForUser(user);

											uriBuilder.addParameter("access_token", tokenMap.get("access_token"));
											uriBuilder.addParameter("refresh_token", tokenMap.get("refresh_token"));

										} else if (!BACKEND_SSO_LOGIN_INDICATOR.equals(entry.getKey())) {

											uriBuilder.addParameter(entry.getKey(), parameterEntry);
										}
									}
								}
							}

							logger.debug("Logging in user {}", user);

							if (!isTokenLogin) {

								AuthHelper.doLogin(request, user);
							}

							HtmlServlet.setNoCacheHeaders(response);

							oAuth2Client.invokeOnLoginMethod(user);

							logger.debug("HttpServletResponse status: {}", response.getStatus());

							boolean isBackendSSOLogin = (originalRequestParameters.get(BACKEND_SSO_LOGIN_INDICATOR) != null);
							if (!isBackendSSOLogin) {

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

							} else {

								uriBuilder.setPath("/structr/");
							}

							response.resetBuffer();
							response.setHeader(StructrTraits.LOCATION, Settings.ApplicationRootPath.getValue() + uriBuilder.build().toString());
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

			logger.error("Could not redirect to {}: {}", oAuth2Client.getErrorURI(), ex);
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

		Traits userTraits = Traits.of(StructrTraits.USER);
		Principal user    = null;

		String authorizationToken = getAuthorizationToken(request);

		if ((authorizationToken == null || StringUtils.equals(authorizationToken, "")) && request.getAttribute(SessionHelper.SESSION_IS_NEW) == null) {

			// First, check session (JSESSIONID cookie)
			if (request.getSession(false) != null) {

				user = AuthHelper.getPrincipalForSessionId(request.getSession(false).getId());
			}

		} else if (authorizationToken != null) {

			final PropertyKey<String> eMailKey = Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.EMAIL_PROPERTY);
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

						user = AuthHelper.getPrincipalForPassword(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), userName, password);

					} catch (AuthenticationException ex) {

						final PropertyKey<String> eMailKey = Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.EMAIL_PROPERTY);
						user = AuthHelper.getPrincipalForPassword(eMailKey, userName, password);
					}
				}
			}
		}

		return user;
	}

	@Override
	public String getUserClass() {

		String configuredCustomClassName = Settings.RegistrationCustomUserClass.getValue();
		if (StringUtils.isEmpty(configuredCustomClassName)) {

			configuredCustomClassName = StructrTraits.USER;
		}

		return configuredCustomClassName;
	}
}
