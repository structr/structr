/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.web.function;

import org.structr.api.config.Settings;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.rest.auth.JWTHelper;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.User;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class CreateAccessAndRefreshTokenFunction extends UiAdvancedFunction {

    public static final String ERROR_MESSAGE    = "Usage: ${create_access_and_refresh_token(user [, accessTokenTimeout, refreshTokenTimeout])}. Example: ${create_access_and_refresh_token(find('User', '<id>') [, 15, 60])}";
    public static final String ERROR_MESSAGE_JS = "Usage: ${{Structr.create_access_and_refresh_token(user [, accessTokenTimeout, refreshTokenTimeout])}}. Example: ${{Structr.create_access_and_refresh_token(Structr.find('User', '<id>') [, 15, 60])}";

    @Override
    public String getName() {
        return "create_access_and_refresh_token";
    }

    @Override
    public List<Signature> getSignatures() {
        return Signature.forAllLanguages("user, accessTokenTimeout, refreshTokenTimeout");
    }

    @Override
    public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

        try {
            assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);
            final User user = (User) sources[0];
            int accessTokenTimeout = Settings.JWTExpirationTimeout.getValue();
            int refreshTokenTimeout = Settings.JWTRefreshTokenExpirationTimeout.getValue();

            if (sources.length > 1) {
                accessTokenTimeout = (int) sources[1];
            }

            if (sources.length > 2) {
                refreshTokenTimeout = (int) sources[2];
            }

            Calendar accessTokenExpirationDate = Calendar.getInstance();
            accessTokenExpirationDate.add(Calendar.MINUTE, accessTokenTimeout);

            Calendar refreshTokenExpirationDate = Calendar.getInstance();
            refreshTokenExpirationDate.add(Calendar.MINUTE, refreshTokenTimeout);

            Map<String, String> tokens = JWTHelper.createTokensForUser(user, accessTokenExpirationDate.getTime(), refreshTokenExpirationDate.getTime());

            return UiFunction.toGraphObject(tokens, 1);

        } catch (ArgumentCountException pe) {

            logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
            return usage(ctx.isJavaScriptContext());

        }
    }

    @Override
    public String usage(boolean inJavaScriptContext) {
        return (inJavaScriptContext ? ERROR_MESSAGE_JS : ERROR_MESSAGE);
    }

    @Override
    public String getShortDescription() {
        return "Creates an access token and an refresh token for the given user";
    }
}
