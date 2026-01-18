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
package org.structr.web.function;

import org.structr.api.config.Settings;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.rest.auth.JWTHelper;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.User;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class CreateAccessTokenFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "createAccessToken";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("user, accessTokenTimeout");
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

			Calendar accessTokenExpirationDate = Calendar.getInstance();
			accessTokenExpirationDate.add(Calendar.MINUTE, accessTokenTimeout);

			Map<String, String> tokens = JWTHelper.createTokensForUser(user, accessTokenExpirationDate.getTime(), null);

			return tokens.get("accessToken");

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());

		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${createAccessToken(user [, accessTokenTimeout])}. Example: ${createAccessToken(me [, 15])}"),
			Usage.javaScript("Usage: ${{ $.createAccessToken(user [, accessTokenTimeout]); }}. Example: ${{ $.createAccessToken(Structr.me [, 15]); }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Creates a JWT access token for the given user entity that can be used for request authentication and authorization.";
	}

	@Override
	public String getLongDescription() {
		return "The return value of this function is a single string with the generated access token. This token can then be used in the `Authorization` HTTP header to authenticate requests against to Structr.";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("user", "user entity to create a token for"),
			Parameter.optional("accessTokenTimeout", "access token timeout in **minutes**, defaults to 1 hour (60 minutes)")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.javaScript("""
			{
				// create an access token that is valid for 30 minutes
				let accessToken = $.createAccessToken($.me, 30);
				
				// ... use the token
			}
			""", "Create a new access token with a validity of 30 minutes"),
			Example.javaScript("""
			fetch("http://localhost:8082/structr/rest/User", {
				method: "GET",
				headers: {
					"authorization": "Bearer eyJhbGciOiJIUzUxMiJ9.eyJ[...]VkIn0.fbwKEQ4dELHuXXmPiNtn8XNWh6ShesdlTZsXf-CojTmxQOWUxkbHcroj7gVz02twox82ChTuyxkyHeIoiidU4g"
				}
			});
			""", "Authenticate a request to the Structr backend with an existing access token")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"In order to use JWT in your application, you must configure `" + Settings.JWTSecretType.getKey() + "` and the corresponding settings your structr.conf.",
			"You can configure the timeouts for access and refresh tokens in your structr.conf by setting `" + Settings.JWTExpirationTimeout.getKey() + "` and `" + Settings.JWTRefreshTokenExpirationTimeout.getKey() + "` respectively.",
			"The refresh token is stored in the `refreshTokens` property in the given User entity."
		);
	}
}
