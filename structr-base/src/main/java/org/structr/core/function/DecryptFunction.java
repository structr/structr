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
package org.structr.core.function;

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

public class DecryptFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_DECRYPT    = "Usage: ${decrypt(value[, secret])}";
	public static final String ERROR_MESSAGE_DECRYPT_JS = "Usage: ${{Structr.decrypt(value[, secret])}}";

	@Override
	public String getName() {
		return "decrypt";
	}

	@Override
	public String getSignature() {
		return "value [, secret ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			String secret = null;
			String text   = null;

			switch (sources.length) {

				case 2:
					secret = sources[1].toString();
				case 1:
					text = sources[0].toString();
			}

			if (secret != null) {

				return CryptFunction.decrypt(text, secret);

			} else {

				return CryptFunction.decrypt(text);
			}

		} catch (ArgumentNullException pe) {

			if (sources[0] == null) {

				// silently ignore case which can happen for decrypt(current.propertyThatCanBeNull[, key])
				return "";

			} else if (sources.length <= 2) {

				logParameterError(caller, sources, ctx.isJavaScriptContext());

				return "";

			} else {

				logParameterError(caller, sources, ctx.isJavaScriptContext());

				// only show the error message for wrong parameter count
				return usage(ctx.isJavaScriptContext());
			}

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

			// only show the error message for wrong parameter count
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_DECRYPT_JS : ERROR_MESSAGE_DECRYPT);
	}

	@Override
	public String shortDescription() {
		return "Decrypts the given string with a secret key from structr.conf or argument 2";
	}
}
