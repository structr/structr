/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.apache.commons.mail.EmailException;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.AdvancedMailContainer;
import org.structr.mail.AdvancedMailModule;
import org.structr.schema.action.ActionContext;

public class MailSendFunction extends AdvancedMailModuleFunction {

	public final String ERROR_MESSAGE    = "Usage: ${mail_send()}";
	public final String ERROR_MESSAGE_JS = "Usage: ${{ Structr.mail_send() }}";

	public MailSendFunction(final AdvancedMailModule parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "mail_send";
	}

	@Override
	public String getSignature() {
		return null;
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		AdvancedMailContainer amc = ctx.getAdvancedMailContainer();

		try {

			amc.clearError();

			return amc.send(ctx.getSecurityContext());

		} catch (FrameworkException ex) {

			logger.warn(ex.getMessage());

			amc.setError(ex);

		} catch (EmailException ex) {

			logException(caller, ex, sources);

			amc.setError(ex);

		} catch (Throwable t) {

			logException(caller, t, sources);

			amc.setError(t);
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_JS : ERROR_MESSAGE);
	}

	@Override
	public String shortDescription() {
		return "Sends the current mail";
	}
}