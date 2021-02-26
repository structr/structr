/*
 * Copyright (C) 2010-2021 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.entity.Principal;
import org.structr.rest.RestMethodResult;
import org.structr.rest.auth.AuthHelper;
import org.structr.schema.action.ActionContext;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class TokenResource extends LoginResource {


    @Override
    public String getErrorMessage() {
        return AuthHelper.TOKEN_ERROR_MSG;
    }

    @Override
    public String getUriPart() {
        return "token";
    }

    @Override
    public String getResourceSignature() {
        return "_token";
    }

    @Override
    protected RestMethodResult getUserForCredentials(SecurityContext securityContext, String emailOrUsername, String password, String twoFactorToken, String twoFactorCode) throws FrameworkException {
        Principal user = null;

        user = getUserForTwoFactorTokenOrEmailOrUsername(twoFactorToken, emailOrUsername, password);

        boolean sendLoginNotification = true;

        if (user == null) {
            String refreshToken = getRefreshToken();

            if (refreshToken != null) {

                user = AuthHelper.getPrincipalForRefreshToken(refreshToken);
                sendLoginNotification = false;

            }
        }

        if (user != null) {

            final boolean twoFactorAuthenticationSuccessOrNotNecessary = AuthHelper.handleTwoFactorAuthentication(user, twoFactorCode, twoFactorToken, ActionContext.getRemoteAddr(securityContext.getRequest()));

            if (twoFactorAuthenticationSuccessOrNotNecessary) {

                if (sendLoginNotification) {

                    AuthHelper.sendLoginNotification(user);
                }

                return doLogin(securityContext, user);
            }
        }

        return null;
    }

    @Override
    protected RestMethodResult doLogin(SecurityContext securityContext, Principal user) throws FrameworkException {
        Map<String, String> tokenMap = AuthHelper.createTokens(securityContext.getRequest(), user);

        logger.info("Token creation successful: {}", user);

        RuntimeEventLog.token("Token creation successful", Map.of("id", user.getUuid(), "name", user.getName()));

        user.setSecurityContext(securityContext);

        // make logged in user available to caller
        securityContext.setCachedUser(user);
        tokenMap.put("token_type", "Bearer");

        return createRestMethodResult(securityContext, tokenMap);
    }

    private String getRefreshToken() {

        final Cookie[] cookies = request.getCookies();

        // first check for token in cookie
        if (cookies != null) {

            for (Cookie cookie : request.getCookies()) {

                if (StringUtils.equals(cookie.getName(), "refresh_token")) {

                    return cookie.getValue();
                }
            }
        }

        if (this.request == null) {
            return null;
        }

        final String refreshToken = request.getParameter("refresh_token");
        if (refreshToken == null) {
            return request.getHeader("refresh_token");
        }
        return refreshToken;
    }

    private RestMethodResult createRestMethodResult(SecurityContext securityContext, Map<String, String> tokenMap) {
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

            if (Settings.ForceHttps.getValue()) {

                tokenCookie.setSecure(true);
                refreshCookie.setSecure(true);

            }

            response.addCookie(tokenCookie);
            response.addCookie(refreshCookie);
        }

        return returnedMethodResult;
    }
}
