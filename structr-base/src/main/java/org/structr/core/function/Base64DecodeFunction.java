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
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Base64;
import java.util.List;

public class Base64DecodeFunction extends CoreFunction {

	@Override
	public String getName() {
		return "base64decode";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("text [, scheme, charset ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 3);

			final String input    = sources[0].toString();
			final Charset charset = (sources.length == 3) ? Charset.forName(sources[2].toString()) : Charset.defaultCharset();

			String decodingScheme = "basic";
			if (sources.length >= 2) {
				decodingScheme = sources[1].toString();
			}

			final Base64.Decoder decoder;

			switch (decodingScheme) {
				case "url":
					decoder = Base64.getUrlDecoder();
					break;

				case "mime":
					decoder = Base64.getMimeDecoder();
					break;

				default:
					logger.warn("Unsupported base64 decoding scheme '{}' - using 'basic' scheme", decodingScheme);

				case "basic":
					decoder = Base64.getDecoder();
					break;
			}

			try {

				return new String(decoder.decode(input), charset);

			} catch (UnsupportedCharsetException uce) {

				logger.warn("base64decode: Unsupported charset {}", sources[2].toString(), uce);

			} catch  (IllegalArgumentException iae) {

				logger.warn("Exception encountered while decoding '{}' with scheme '{}'", input, decodingScheme, iae);
				return iae.getMessage();
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${base64decode(text[, scheme[, charset]])}. Example: ${base64decode(\"Q2hlY2sgb3V0IGh0dHA6Ly9zdHJ1Y3RyLmNvbQ==\")}"),
			Usage.structrScript("Usage: ${{Structr.base64decode(text[, scheme[, charset]])}}. Example: ${{Structr.base64decode(\"Q2hlY2sgb3V0IGh0dHA6Ly9zdHJ1Y3RyLmNvbQ==\")}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Decodes the given base64-encoded value and returns a string";
	}
}
