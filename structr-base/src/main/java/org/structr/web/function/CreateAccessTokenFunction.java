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
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.rest.auth.JWTHelper;
import org.structr.schema.action.ActionContext;

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

		return Signature.forAllScriptingLanguages("user [, accessTokenTimeout ]");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof NodeInterface p1 && p1.is(StructrTraits.USER)) {

				final Principal user = p1.as(Principal.class);
				int accessTokenTimeout = Settings.JWTExpirationTimeout.getValue();

				if (sources.length > 1) {

					if (sources[1] instanceof Number timeout) {

						accessTokenTimeout = timeout.intValue();

					} else {

						logger.warn("{}: parameter 'accessTokenTimeout' must be a number, was '{}' - using default. {}", getDisplayName(false), sources[1], usage());
					}
				}

				final Calendar accessTokenExpirationDate = Calendar.getInstance();
				accessTokenExpirationDate.add(Calendar.MINUTE, accessTokenTimeout);

				final Map<String, String> tokens = JWTHelper.createTokensForUser(user, accessTokenExpirationDate.getTime(), null);

				return tokens.get(JWTHelper.ACCESS_TOKEN_KEY);

			} else {

				return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, getDisplayName(false) + ": Node of type User required as first argument!");
			}

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage());

			return null;
		}
	}

	@Override
	public String getShortDescription() {

		return "Creates a JWT access token for the given user entity that can be used for request authentication and authorization.";
	}

	@Override
	public String getLongDescription() {

		return "The return value of this function is a single string with the generated access token. This token can then be used in the `Authorization` HTTP header to authenticate requests against Structr.";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("user", "user entity to create a token for"),
			Parameter.optional("accessTokenTimeout",  "access token timeout in **minutes**, defaults to setting `" + Settings.JWTExpirationTimeout.getKey() + "`")
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
						"authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJzdHJ1Y3RyIiwic3ViIjoiYWRtaW4iLCJleHAiOjE1OTc5MjMzNjh9..."
					}
				});
				""", "Authenticate a request to the Structr backend with an existing access token")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"In order to use JWT in your application, you must configure `" + Settings.JWTSecretType.getKey() + "` and the corresponding settings in your structr.conf.",
			"The default timeout is configured in structr.conf with `" + Settings.JWTExpirationTimeout.getKey() + "` (default: " + Settings.JWTExpirationTimeout.getDefaultValue() + " minutes).",
			"The timeout must be a number. Decimal values are cut down to whole minutes (`45.7` becomes 45 minutes). Any other value is ignored with a warning in the server log, and the default timeout is used instead.",
			"The first argument must be a node of type `User` (or of a type that extends `User`). For any other value, the function throws an error in JavaScript, and in StructrScript it logs a warning and returns `null`."
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Security;
	}
}
