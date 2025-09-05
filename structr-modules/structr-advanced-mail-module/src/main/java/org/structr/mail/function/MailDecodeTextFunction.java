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
import org.structr.mail.AdvancedMailModule;
import org.structr.schema.action.ActionContext;

import javax.mail.internet.MimeUtility;
import java.io.UnsupportedEncodingException;

public class MailDecodeTextFunction extends AdvancedMailModuleFunction {

	public final String ERROR_MESSAGE    = "Usage: ${mail_decode_text(text)}";
	public final String ERROR_MESSAGE_JS = "Usage: ${{ Structr.mail_decode_text(text) }}";

	public MailDecodeTextFunction(final AdvancedMailModule parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "mail_decode_text";
	}

	@Override
	public String getSignature() {
		return "text";
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			try {

				return MimeUtility.decodeText(sources[0].toString());

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
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_JS : ERROR_MESSAGE);
	}

	@Override
	public String shortDescription() {
		return "Decodes RFC 822 \"text\" token from mail-safe form as per RFC 2047";
	}
}