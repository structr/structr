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

public class Base64EncodeFunction extends CoreFunction {

	@Override
	public String getName() {
		return "base64encode";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("text [, scheme, charset ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 3);

			final Charset charset = (sources.length == 3) ? Charset.forName(sources[2].toString()) : Charset.defaultCharset();
			
			byte[] input;
			if (sources[0] instanceof byte[]) {
				input = (byte[]) sources[0];
			} else {
				input = sources[0].toString().getBytes(charset);
			}

			String encodingScheme = "basic";
			if (sources.length >= 2) {
				encodingScheme = sources[1].toString();
			}

			final Base64.Encoder encoder;

			switch (encodingScheme) {
				case "url":
					encoder = Base64.getUrlEncoder();
					break;

				case "mime":
					encoder = Base64.getMimeEncoder();
					break;

				default:
					logger.warn("Unsupported base64 encoding scheme '{}' - using 'basic' scheme", encodingScheme);

				case "basic":
					encoder = Base64.getEncoder();
					break;
			}

			return encoder.encodeToString(input);

		} catch (UnsupportedCharsetException uce) {

			logger.warn("base64encode: Unsupported charset {}", sources[2].toString(), uce);

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
			Usage.structrScript("Usage: ${base64encode(text[, scheme[, charset]])}. Example: ${base64encode(\"Check out http://structr.com\")}"),
			Usage.javaScript("Usage: ${{Structr.base64encode(text[, scheme[, charset]])}}. Example: ${{Structr.base64encode(\"Check out http://structr.com\")}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Encodes the given string and returns a base64-encoded string.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
