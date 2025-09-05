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

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

public class ValidateEmailFunction extends UiAdvancedFunction {

	public final String ERROR_MESSAGE    = "Usage: ${validate_email(address)}";
	public final String ERROR_MESSAGE_JS = "Usage: ${{ $.validate_email(address) }}";

	@Override
	public String getName() {
		return "validate_email";
	}

	@Override
	public String getSignature() {
		return "address";
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
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_JS : ERROR_MESSAGE);
	}

	@Override
	public String shortDescription() {
		return "Validates the given address against the syntax rules of RFC 822. The current implementation checks many, but not all, syntax rules. If it is a valid email according to the RFC, nothing is returned. Otherwise the error text is returned.";
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