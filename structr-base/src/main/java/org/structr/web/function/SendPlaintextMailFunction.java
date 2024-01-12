/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.common.MailHelper;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

public class SendPlaintextMailFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_SEND_PLAINTEXT_MAIL = "Usage: ${send_plaintext_mail(fromAddress, fromName, toAddress, toName, subject, content)}.";

	@Override
	public String getName() {
		return "send_plaintext_mail";
	}

	@Override
	public String getSignature() {
		return "from, fromName, to, toName, subject, content";
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
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_SEND_PLAINTEXT_MAIL;
	}

	@Override
	public String shortDescription() {
		return "Sends a plaintext e-mail";
	}
}
