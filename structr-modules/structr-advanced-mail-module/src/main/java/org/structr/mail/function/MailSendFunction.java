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

import org.apache.commons.mail.EmailException;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.AdvancedMailContainer;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.mail.AdvancedMailModule;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class MailSendFunction extends AdvancedMailModuleFunction {

	public MailSendFunction(final AdvancedMailModule parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "mailSend";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("");
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
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${mailSend()}"),
			Usage.javaScript("Usage: ${{ $.mailSend() }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Sends the currently configured mail.";
	}

	@Override
	public String getLongDescription() {
		return """
				The message-id of the created mail is being returned.

				If not all pre-conditions are met or the sending of the mail fails, an empty string will be returned and an error message is logged.
				
				A possible error message can be retrieved via `mailGetError()` and the presence of an error can be checked via `mailHasError()`.
				
				Before attempting to send the mail, the last error (if any) is cleared automatically.
				""";
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"Will result in an error if no `To:`, `Cc:` or `Bcc:` addresses are configured.",
				"Will result in an error if `mailBegin()` was not called"
		);
	}
}