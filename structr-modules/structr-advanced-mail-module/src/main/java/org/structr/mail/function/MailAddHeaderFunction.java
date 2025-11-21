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

import java.util.List;

public class MailAddHeaderFunction extends AdvancedMailModuleFunction {

	public MailAddHeaderFunction(final AdvancedMailModule parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "mail_add_header";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("name, value");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 2);

			final String name  = sources[0].toString();
			final String value = sources[1].toString();

			ctx.getAdvancedMailContainer().addCustomHeader(name, value);

			return "";

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${mail_add_header(name, value)}"),
			Usage.javaScript("Usage: ${{ $.mailAddHeader(name, value) }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Adds a custom header to the current mail.";
	}

	@Override
	public String getLongDescription() {
		return """
			Email headers (according to RFC 822) must contain only US-ASCII characters. A header that contains non US-ASCII characters must be encoded as per the rules of RFC 2047 (see `mail_encode_text()`).

			Adding a non-compliant header will result in an error upon calling the `mail_send()` function.
			""";
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"can be called multiple times to add more headers."
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${mail_add_header('X-Mailer', 'Structr')}", "US-ASCII only header"),
				Example.structrScript("${mail_add_header('X-Mailer', mail_encode_text('Umlaut Mail Dämon'))}", "Encoded header with non US-ASCII characters")
		);
	}
}