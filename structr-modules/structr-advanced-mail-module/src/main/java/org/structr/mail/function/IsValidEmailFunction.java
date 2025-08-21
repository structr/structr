/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.mail.function;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.structr.common.error.FrameworkException;
import org.structr.mail.AdvancedMailModule;
import org.structr.schema.action.ActionContext;

public class IsValidEmailFunction extends AdvancedMailModuleFunction {

	public final String ERROR_MESSAGE    = "Usage: ${is_valid_email(address)}";
	public final String ERROR_MESSAGE_JS = "Usage: ${{ $.is_valid_email(address) }}";

	public IsValidEmailFunction(final AdvancedMailModule parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "is_valid_email";
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

			try {

				final InternetAddress address = new InternetAddress(email);
				address.validate();

				// only accept if parsed address is equal to supplied address (even leading or trailing whitespace lets this validation fail)
				return (address.getAddress().equals(email));

			} catch (AddressException ex) {

				return false;
			}

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
		return "Checks if the given address conforms to the syntax rules of RFC 822. The current implementation checks many, but not all, syntax rules. Only email addresses without personal name are accepted and leading/trailing fails validation.";
	}
}