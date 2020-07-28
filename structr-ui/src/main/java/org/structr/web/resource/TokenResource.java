/*
 * Copyright (C) 2010-2020 Structr GmbH
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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.entity.Principal;
import org.structr.rest.RestMethodResult;
import org.structr.rest.auth.AuthHelper;
import org.structr.schema.action.ActionContext;

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

        if (user == null) {
            String refreshToken = getRefreshToken();

            if (refreshToken != null) {

                user = AuthHelper.getPrincipalForRefreshToken(refreshToken);

            }
        }

        if (user != null) {

            final boolean twoFactorAuthenticationSuccessOrNotNecessary = AuthHelper.handleTwoFactorAuthentication(user, twoFactorCode, twoFactorToken, ActionContext.getRemoteAddr(securityContext.getRequest()));

            if (twoFactorAuthenticationSuccessOrNotNecessary) {

                return doLogin(securityContext, user);
            }
        }

        return null;
    }

    @Override
    protected RestMethodResult doLogin(SecurityContext securityContext, Principal user) throws FrameworkException {
        Map<String, String> tokenMap = AuthHelper.createTokens(securityContext.getRequest(), user);

        logger.info("Token creation successful: {}", user);

        RuntimeEventLog.login("Token creation successful", user.getUuid(), user.getName());

        user.setSecurityContext(securityContext);

        // make logged in user available to caller
        securityContext.setCachedUser(user);
        tokenMap.put("token_type", "Bearer");

        return createRestMethodResult(tokenMap);
    }

    private String getRefreshToken() {

        if (this.request == null) {
            return null;
        }

        final String refreshToken = request.getParameter("refresh_token");
        if (refreshToken == null) {
            return request.getHeader("refresh_token");
        }
        return refreshToken;
    }

    private RestMethodResult createRestMethodResult(Map<String, String> tokenMap) {
        RestMethodResult returnedMethodResult = new RestMethodResult(200);
        returnedMethodResult.addContent(tokenMap);

        returnedMethodResult.addHeader("access_token", tokenMap.get("access_token"));
        returnedMethodResult.addHeader("refresh_token", tokenMap.get("refresh_token"));

        return returnedMethodResult;
    }
}
