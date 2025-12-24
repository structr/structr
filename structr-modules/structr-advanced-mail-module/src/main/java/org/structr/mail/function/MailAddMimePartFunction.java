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
import org.structr.common.helper.AdvancedMailContainer;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.mail.AdvancedMailModule;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class MailAddMimePartFunction extends AdvancedMailModuleFunction {

	public MailAddMimePartFunction(final AdvancedMailModule parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "mailAddMimePart";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("content, contentType");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			final AdvancedMailContainer amc = ctx.getAdvancedMailContainer();

			amc.addMimePart(sources[0].toString(), sources[1].toString());

			return "";

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${mailAddMimePart(content, contentType)}"),
			Usage.javaScript("Usage: ${{ $.mailAddMimePart(content, contentType) }}")
		);
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("content", "content of the MIME part"),
				Parameter.mandatory("contentType", "content type of the MIME part")
		);
	}

	@Override
	public String getShortDescription() {
		return "Adds a MIME part to the current mail.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"see `mailClearMimeParts()` to remove added mime parts",
				"can be called multiple times to add more mime parts."
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.javaScript("""
						${{
						
							$.mailBegin('sender@example.com', 'VCard Collection Service', 'Your VCards');
							$.mailAddTo($.me.eMail);
							$.mailSetHtmlContent('<html><body><p>This are all the vcards you collected.</p></body></html>');

							for (let contact of $.me.contacts) {

								let vcardContent = $.template('VCARD', 'en', contact);

								$.mailAddMimePart(vcardContent, 'text/x-vcard; charset=utf-8; name="contact-' + contact.id + '.vcf"');
							}

							$.mailSend();
						}}""", "Mail containing all vcards a user collected")
		);
	}
}