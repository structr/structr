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


public class Base64DecodeFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_BASE64DECODE = "Usage: ${base64decode(text[, scheme])}. Example: ${base64decode(\"Q2hlY2sgb3V0IGh0dHA6Ly9zdHJ1Y3RyLmNvbQ==\")}";
	public static final String ERROR_MESSAGE_BASE64DECODE_JS = "Usage: ${{Structr.base64decode(text[, scheme])}}. Example: ${{Structr.base64decode(\"Q2hlY2sgb3V0IGh0dHA6Ly9zdHJ1Y3RyLmNvbQ==\")}}";

	@Override
	public String getName() {
		return "base64decode()";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {
			if (arrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2)) {

				final String input = sources[0].toString();

				String decodingSchema = "basic";
				if (sources.length == 2) {
					decodingSchema = sources[1].toString();
				}

				final Base64.Decoder decoder;

				switch (decodingSchema) {
					case "url":
						decoder = Base64.getUrlDecoder();
						break;
					case "mime":
						decoder = Base64.getMimeDecoder();
						break;
					default:
						logger.warn("Unsupported base64 decoding scheme '{}' - using 'basic' scheme", decodingSchema);
					case "basic":
						decoder = Base64.getDecoder();
						break;
				}

				try {

					return new String(decoder.decode(input));

				} catch  (IllegalArgumentException iae) {

					logger.warn("Exception encountered while decoding '{}' with scheme '{}'", input, decodingSchema, iae);
					return iae.getMessage();
				}
			}

		} catch (IllegalArgumentException iae) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_BASE64DECODE_JS : ERROR_MESSAGE_BASE64DECODE);
	}

	@Override
	public String shortDescription() {
		return "Decodes the given base64-encoded value and returns a string";
	}
}
