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
import org.structr.docs.ontology.FunctionCategory;
import org.structr.rest.auth.JWTHelper;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.User;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class CreateAccessAndRefreshTokenFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "createAccessAndRefreshToken";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("user, accessTokenTimeout, refreshTokenTimeout");
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
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${createAccessAndRefreshToken(user [, accessTokenTimeout, refreshTokenTimeout])}. Example: ${createAccessAndRefreshToken(find('User', '<id>') [, 15, 60])}"),
			Usage.javaScript("Usage: ${{Structr.createAccessAndRefreshToken(user [, accessTokenTimeout, refreshTokenTimeout])}}. Example: ${{Structr.createAccessAndRefreshToken(Structr.find('User', '<id>') [, 15, 60])}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Creates both JWT access token and refresh token for the given User entity that can be used for request authentication and authorization.";
	}

	@Override
	public String getLongDescription() {
		return """
		The return value of this function is a map with the following structure:

		```
		{
		    "accessToken":"eyJhbGciOiJIUzUxMiJ9.eyJ[...]VkIn0.fbwKEQ4dELHuXXmPiNtn8XNWh6ShesdlTZsXf-CojTmxQOWUxkbHcroj7gVz02twox82ChTuyxkyHeIoiidU4g",
		    "refreshToken":"eyJhbGciOiJIUzUxMiJ9.eyJ[...]lbiJ9.GANUkPH09pBimd5EkJmrEbsYQhDw6hXULZGSldHSZYqq1FNjM_g6wfxt1217TlGZcjKyXEL_lktcPzjOeEU3A",
		    "expirationDate":"1616692902820"
		}
		```
		""";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("user", "user entity to create tokens for"),
			Parameter.optional("accessTokenTimeout", "access token timeout in **minutes**, defaults to 1 hour (60 minutes)"),
			Parameter.optional("refreshTokenTimeout", "refresh token timeout in **minutes**, defaults to 1 day (1440 minutes)")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.javaScript("""
		{
			// create an access token that is valid for 30 minutes
			// and a refresh token that is valid for 2 hours
			let tokens       = $.createAccessAndRefreshToken($.me, 30, 120);
			let accessToken  = tokens.accessToken;
			let refreshToken = tokens.refreshToken;
			
			// ... use the tokens
		}
		""", "Create a new tokens with non-default validity periods"),
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

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Security;
	}
}
