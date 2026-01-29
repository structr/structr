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
import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class IsValidEmailFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "isValidEmail";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("address");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			final String email = sources[0].toString();

			return Settings.isValidEmail(email);

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${isValidEmail(address)}"),
			Usage.javaScript("Usage: ${{ $.isValidEmail(address) }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Checks if the given address is a valid email address.";
	}

	@Override
	public String getLongDescription() {
		return "The validation uses the email validation regular expression configured in `%s`".formatted(Settings.EmailValidationRegex.getKey());
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("address", "address to validate")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${validateEmail('john@example.com')}", "Valid email"),
				Example.structrScript("${validateEmail('John Doe <john@example.com>')}", "Invalid email"),
				Example.structrScript("${validateEmail('john@example')}", "Invalid email"),
				Example.javaScript("""
					${{
						let potentialEmail = $.request.email;

						if ($.isValidEmail(potentialEmail)) {

							return 'Given email is valid.';

						} else {

							return 'Please supply a valid email address.';
						}
					}}
					""", "Script that checks if request parameter 'email' is a valid email address.")
		);
	}
}