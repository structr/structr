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

import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.mail.AdvancedMailModule;
import org.structr.schema.action.ActionContext;

import javax.mail.internet.MimeUtility;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class MailEncodeTextFunction extends AdvancedMailModuleFunction {

	public MailEncodeTextFunction(final AdvancedMailModule parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "mail_encode_text";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("text");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			try {

				return MimeUtility.encodeText(sources[0].toString());

			} catch (UnsupportedEncodingException ex) {

				logger.warn("Unable to encode text '{}', returning original value", sources[0].toString());
				return sources[0].toString();
			}

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${mail_encode_text(text)}"),
			Usage.javaScript("Usage: ${{ $.mailEncodeText(text) }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Encodes RFC 822 \"text\" token into mail-safe form as per RFC 2047.";
	}

	@Override
	public String getLongDescription() {
		return """
				The given Unicode string is examined for non US-ASCII characters. If the string contains only US-ASCII characters, it is returned as-is. If the string contains non US-ASCII characters, it is first character-encoded using the platform's default charset, then transfer-encoded using either the B or Q encoding. The resulting bytes are then returned as a Unicode string containing only ASCII characters.
				Note that this method should be used to encode only "unstructured" RFC 822 headers.
				""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${mail_add_header('X-Mailer', mail_encode_text('Umlaut Mail Dämon'))}", "Encoded header with non US-ASCII characters")
		);
	}
}