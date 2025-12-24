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
package org.structr.web.function;

import org.apache.commons.mail.EmailException;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.MailHelper;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class SendPlaintextMailFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "sendPlaintextMail";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("fromAddress, fromName, toAddress, toName, subject, content");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 6);

			final String from        = sources[0].toString();
			final String fromName    = sources[1].toString();
			final String to          = sources[2].toString();
			final String toName      = sources[3].toString();
			final String subject     = sources[4].toString();
			final String textContent = sources[5].toString();

			try {

				return MailHelper.sendSimpleMail(from, fromName, to, toName, null, null, from, subject, textContent);

			} catch (EmailException eex) {

				logException(caller, eex, sources);
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{ $.sendPlaintextMail(fromAddress, fromName, toAddress, toName, subject, content) }}."),
			Usage.structrScript("Usage: ${sendPlaintextMail(fromAddress, fromName, toAddress, toName, subject, content)}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Sends a plaintext email.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"`textContent` is typically generated using the `template()` function.",
				"Emails are sent based on the SMTP configuration defined in structr.conf.",
				"For advanced scenarios, refer to the extended mail functions prefixed with `mail`, beginning with `mailBegin()`."
		);
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("fromAddress", "sender address"),
				Parameter.mandatory("fromName", "sender name"),
				Parameter.mandatory("toAddress", "recipient address"),
				Parameter.mandatory("toName", "recipient name"),
				Parameter.mandatory("subject", "subject"),
				Parameter.mandatory("textContent", "text content")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${sendPlaintextMail('info@structr.com', 'Structr', 'user@domain.com', 'Test User', 'Welcome to Structr', 'Hi User, welcome to Structr!')}")
		);
	}
}
