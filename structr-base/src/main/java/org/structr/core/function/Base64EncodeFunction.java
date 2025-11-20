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
import org.structr.docs.Example;
import org.structr.docs.Parameter;
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
	public String getShortDescription() {
		return "Encodes the given string and returns a base64-encoded string.";
	}

	@Override
	public String getLongDescription() {
		return """
		Valid values for `scheme` are `basic` (default), `url` and `mime`. The following explanation of the encoding schemes is taken directly from https://docs.oracle.com/javase/8/docs/api/java/util/Base64.html
		
		**Basic**
		Uses "The Base64 Alphabet" as specified in Table 1 of RFC 4648 and RFC 2045 for encoding and decoding operation. The encoder does not add any line feed (line separator) character. The decoder rejects data that contains characters outside the base64 alphabet.
		
		**URL and Filename safe**
		Uses the "URL and Filename safe Base64 Alphabet" as specified in Table 2 of RFC 4648 for encoding and decoding. The encoder does not add any line feed (line separator) character. The decoder rejects data that contains characters outside the base64 alphabet.
		
		**MIME**
		Uses the "The Base64 Alphabet" as specified in Table 1 of RFC 2045 for encoding and decoding operation. The encoded output must be represented in lines of no more than 76 characters each and uses a carriage return '\\r' followed immediately by a linefeed '\\n' as the line separator. No line separator is added to the end of the encoded output. All line separators or other characters not found in the base64 alphabet table are ignored in decoding operation.
		""";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("text [, scheme, charset ]");
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${base64encode(text[, scheme[, charset]])}. Example: ${base64encode(\"Check out http://structr.com\")}"),
			Usage.javaScript("Usage: ${{Structr.base64encode(text[, scheme[, charset]])}}. Example: ${{Structr.base64encode(\"Check out http://structr.com\")}}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("base64Text", "text to encode"),
			Parameter.optional("scheme", "encoding scheme, `url`, `mime` or `basic`, defaults to `basic`"),
			Parameter.optional("charset", "charset to use, defaults to UTF-8")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${base64encode(\"Check out https://structr.com\")}", "Encode a string")
		);
	}
}
