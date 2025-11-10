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
import org.structr.docs.Signature;
import org.structr.mail.AdvancedMailModule;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class MailSetInReplyTo extends AdvancedMailModuleFunction {

	public final String ERROR_MESSAGE    = "Usage: ${mail_set_in_reply_to(messageId)}";
	public final String ERROR_MESSAGE_JS = "Usage: ${{ Structr.mail_set_in_reply_to(messageId) }}";

	public static final String IN_REPLY_TO_HEADER = "In-reply-to";

	public MailSetInReplyTo(final AdvancedMailModule parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "mail_set_in_reply_to";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("messageId");
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
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_JS : ERROR_MESSAGE);
	}

	@Override
	public String getShortDescription() {
		return "Automatically sets the In-reply-to header for the outgoing mail so the recipient mail client knows to which message the mail is a reply.";
	}
}