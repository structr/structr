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
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.mail.AdvancedMailModule;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class MailSetInReplyTo extends AdvancedMailModuleFunction {

	public static final String IN_REPLY_TO_HEADER = "In-Reply-To";

	public MailSetInReplyTo(final AdvancedMailModule parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "mailSetInReplyTo";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("messageId");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			final String inReplyTo = sources[0].toString();

			ctx.getAdvancedMailContainer().setInReplyTo(inReplyTo);
			ctx.getAdvancedMailContainer().addCustomHeader(IN_REPLY_TO_HEADER, inReplyTo);

			return "";

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${mailSetInReplyTo(messageId)}"),
			Usage.javaScript("Usage: ${{ $.mailSetInReplyTo(messageId) }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Sets the `In-Reply-To` header for the outgoing mail.";
	}

	@Override
	public String getLongDescription() {
		return """
				Indicates that the mail is a reply to the message with the given `messageId`. This function automatically sets the `In-Reply-To` header of the mail so that the receiving mail client can handle it correctly.
				This function is especially interesting in combination with the mail service and automatically ingested mails from configured mailboxes.
				""";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("messageId", "message id of the mail to respond to")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${mailSetInReplyTo('<1910177794.5.1555059600315.JavaMail.username@machine.local>')}")
		);
	}
}