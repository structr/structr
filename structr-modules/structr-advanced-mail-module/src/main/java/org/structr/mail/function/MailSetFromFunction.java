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

public class MailSetFromFunction extends AdvancedMailModuleFunction {

	public final String ERROR_MESSAGE    = "Usage: ${mail_set_from(fromAddress[, fromName])}";
	public final String ERROR_MESSAGE_JS = "Usage: ${{ Structr.mail_set_from(fromAddress[, fromName]) }}";

	public MailSetFromFunction(final AdvancedMailModule parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "mail_set_from";
	}

	@Override
	public String getSignature() {
		return "fromAddress [, fromName ]";
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			final String address = sources[0].toString();
			final String name    = (sources.length == 2) ? sources[1].toString() : null;

			ctx.getAdvancedMailContainer().setFrom(address, name);

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_JS : ERROR_MESSAGE);
	}

	@Override
	public String shortDescription() {
		return "Sets fromAddress and optional fromName of the current mail";
	}
}