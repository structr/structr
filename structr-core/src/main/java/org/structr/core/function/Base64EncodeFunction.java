/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.util.Base64;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;


public class Base64EncodeFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_BASE64ENCODE = "Usage: ${base64encode(text[, scheme])}. Example: ${base64encode(\"Check out http://structr.com\", \"url\")}";
	public static final String ERROR_MESSAGE_BASE64ENCODE_JS = "Usage: ${{Structr.base64encode(text[, scheme])}}. Example: ${{Structr.base64encode(\"Check out http://structr.com\")}}";

	@Override
	public String getName() {
		return "base64encode()";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {
			if (arrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2)) {

				final byte[] input = sources[0].toString().getBytes();

				String encodingSchema = "basic";
				if (sources.length == 2) {
					encodingSchema = sources[1].toString();
				}

				final Base64.Encoder encoder;

				switch (encodingSchema) {
					case "url":
						encoder = Base64.getUrlEncoder();
						break;
					case "mime":
						encoder = Base64.getMimeEncoder();
						break;
					default:
						logger.warn("Unsupported base64 encoding scheme '{}' - using 'basic' scheme", encodingSchema);
					case "basic":
						encoder = Base64.getEncoder();
						break;
				}

				return encoder.encodeToString(input);
			}

		} catch (IllegalArgumentException iae) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_BASE64ENCODE_JS : ERROR_MESSAGE_BASE64ENCODE);
	}

	@Override
	public String shortDescription() {
		return "Encodes the given string and returns a base64-encoded string";
	}
}
