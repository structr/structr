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
package org.structr.web.resource;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.auth.JWTHelper;
import org.structr.schema.action.ActionContext;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.APICall;
import org.structr.api.APICallHandler;
import org.structr.api.APIEndpoint;
import org.structr.api.config.Settings;
import org.structr.api.parameter.APIParameter;
import org.structr.common.event.RuntimeEventLog;
import org.structr.rest.RestMethodResult;

public class TokenResource extends APIEndpoint {

	public TokenResource() {
		super(APIParameter.forStaticString("logout"));
	}

	@Override
	public APICallHandler accept(final SecurityContext securityContext, final APICall call) throws FrameworkException {
		return new TokenResourceHandler(securityContext, call.getURL());
	}

	private class TokenResourceHandler extends LoginResourceHandler {

		public TokenResourceHandler(final SecurityContext securityContext, final String url) {
			super(securityContext, url);
		}

		@Override
		public String getErrorMessage() {
			return JWTHelper.TOKEN_ERROR_MSG;
		}

		@Override
		protected Principal getUserForCredentials(final SecurityContext securityContext, final String emailOrUsername, final String password, final String twoFactorToken, final String twoFactorCode, final Map<String, Object> propertySet) throws FrameworkException {

			Principal user = getUserForTwoFactorTokenOrEmailOrUsername(twoFactorToken, emailOrUsername, password);

			boolean isFromRefreshToken = false;

			if (user == null) {

				String refreshToken = getRefreshToken(propertySet);

				if (refreshToken != null) {

					user = JWTHelper.getPrincipalForRefreshToken(refreshToken);
					isFromRefreshToken = true;

				}
			}

			if (user != null) {

				final boolean twoFactorAuthenticationSuccessOrNotNecessary = AuthHelper.handleTwoFactorAuthentication(user, twoFactorCode, twoFactorToken, ActionContext.getRemoteAddr(securityContext.getRequest()));

				if (twoFactorAuthenticationSuccessOrNotNecessary) {

					if (isFromRefreshToken == false) {

						AuthHelper.updateLastLoginDate(user);
						AuthHelper.sendLoginNotification(user, securityContext.getRequest());
					}

					return user;
				}
			}

			return null;
		}

		@Override
		protected RestMethodResult doLogin(final SecurityContext securityContext, final Principal user) throws FrameworkException {

			final Map<String, String> tokenMap = JWTHelper.createTokensForUser(user);

			logger.info("Token creation successful: {}", user);

			RuntimeEventLog.token("Token creation successful", Map.of("id", user.getUuid(), "name", user.getName()));

			user.setSecurityContext(securityContext);

			// make logged in user available to caller
			securityContext.setCachedUser(user);
			tokenMap.put("token_type", "Bearer");

			return createRestMethodResult(securityContext, tokenMap);
		}

		private String getRefreshToken(final Map<String, Object> propertySet) {

			String refreshToken = (String)propertySet.get("refresh_token");
			if (refreshToken != null) {

				return refreshToken;
			}

			if (this.request == null) {
				return null;
			}

			final Cookie[] cookies = request.getCookies();
			if (cookies != null) {

				for (Cookie cookie : request.getCookies()) {

					if (StringUtils.equals(cookie.getName(), "refresh_token")) {

						return cookie.getValue();
					}
				}
			}

			if (refreshToken == null) {
				return request.getHeader("refresh_token");
			}

			return refreshToken;
		}

		private RestMethodResult createRestMethodResult(final SecurityContext securityContext, final Map<String, String> tokenMap) {

			RestMethodResult returnedMethodResult = new RestMethodResult(200);
			returnedMethodResult.addContent(tokenMap);

			final HttpServletResponse response = securityContext.getResponse();
			if (response != null) {

				final int tokenMaxAge = Settings.JWTExpirationTimeout.getValue();
				final int refreshMaxAge = Settings.JWTRefreshTokenExpirationTimeout.getValue();

				final Cookie tokenCookie = new Cookie("access_token", tokenMap.get("access_token"));
				tokenCookie.setPath("/");
				tokenCookie.setHttpOnly(false);
				tokenCookie.setMaxAge(tokenMaxAge * 60);

				final Cookie refreshCookie = new Cookie("refresh_token", tokenMap.get("refresh_token"));
				refreshCookie.setHttpOnly(true);
				refreshCookie.setMaxAge(refreshMaxAge * 60);

				if (Settings.ForceHttps.getValue() || securityContext.getRequest().isSecure()) {

					tokenCookie.setSecure(true);
					refreshCookie.setSecure(true);
				}

				response.addCookie(tokenCookie);
				response.addCookie(refreshCookie);
			}

			return returnedMethodResult;
		}
	}
}
