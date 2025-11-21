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

import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.List;

public class ValidateEmailFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "validate_email";
	}


	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("string");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			final String email = sources[0].toString();

			return getEmailValidationErrorMessageOrNull(email);

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${validate_email(string)}"),
			Usage.javaScript("Usage: ${{ $.validate_email(string) }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Validates the given address against the syntax rules of RFC 822.";
	}

	@Override
	public String getLongDescription() {
		return """
			Returns the error text if validation fails. If it is a valid email according to RFC 822, nothing is returned.

			RFC 822 treats a lot of strings as valid email addresses, which (in a web context) might not be desirable.
			For more detailed information how this works, see javax.mail.internet.InternetAddress.validate().
			""";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("string", "Input string to be evaluated as a valid email address")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${validate_email('john@example.com')}", "Valid email address where the result would be empty"),
				Example.structrScript("${validate_email('John Doe <john@example.com>')}", "Valid email address where the result would be empty"),
				Example.structrScript("${validate_email('john@example')}", "Invalid email address where the result would be \"Missing final '@domain'\"."),
				Example.structrScript("${validate_email('john')}", "Invalid email address where the result would be \"Missing final '@domain'\"."),
				Example.javaScript("""
					${{
						let potentialEmail = $.request.email;

						let isValidEmail = $.empty($.validateEmail(potentialEmail));

						if (isValidEmail) {
							return 'Given email is valid.';
						} else {
							return 'Please supply a valid email address.';
						}
					}}
					""", "Script that checks if request parameter 'email' is a valid email address.")
		);
	}

	public static String getEmailValidationErrorMessageOrNull(final String email) {

		try {

			final InternetAddress address = new InternetAddress(email);
			address.validate();

			return null;

		} catch (AddressException ex) {

			return ex.getMessage();
		}
	}
}