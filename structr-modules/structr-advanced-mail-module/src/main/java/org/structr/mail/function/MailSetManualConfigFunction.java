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

import org.structr.common.error.FrameworkException;
import org.structr.mail.AdvancedMailModule;
import org.structr.schema.action.ActionContext;

public class MailSetManualConfigFunction extends AdvancedMailModuleFunction {

	public final String ERROR_MESSAGE    = "Usage: ${ mail_set_manual_config(smtpHost = 'localhost', smtpPort = 25, smtpUser = null, smtpPassword = null, smtpUseTLS = true, smtpRequireTLS = true) }";
	public final String ERROR_MESSAGE_JS = "Usage: ${{ Structr.mail_set_manual_config(smtpHost = 'localhost', smtpPort = 25, smtpUser = null, smtpPassword = null, smtpUseTLS = true, smtpRequireTLS = true) }}";

	public MailSetManualConfigFunction(final AdvancedMailModule parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "mail_set_manual_config";
	}

	@Override
	public String getSignature() {
			return "smtpHost = 'localhost', smtpPort = 25, smtpUser = null, smtpPassword = null, smtpUseTLS = true, smtpRequireTLS = true";
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			String smtpHost        = "localhost";
			int smtpPort           = 25;
			String smtpUser        = null;
			String smtpPassword    = null;
			boolean smtpUseTLS     = true;
			boolean smtpRequireTLS = true;

			switch (sources.length) {
				case 6:
					if (sources[5] != null && sources[5] instanceof Boolean) {
						smtpRequireTLS = (Boolean)sources[5];
					}
				case 5:
					if (sources[4] != null && sources[4] instanceof Boolean) {
						smtpUseTLS = (Boolean)sources[4];
					}
				case 4:
					if (sources[3] != null) {
						smtpPassword = sources[3].toString();
					}
				case 3:
					if (sources[2] != null) {
						smtpUser = sources[2].toString();
					}
				case 2:
					if (sources[1] != null && sources[1] instanceof Number) {
						smtpPort = ((Number)sources[1]).intValue();
					}
				case 1:
					if (sources[0] != null) {
						smtpHost = sources[0].toString();
					}
			}

			ctx.getAdvancedMailContainer().setManualConfiguration(smtpHost, smtpPort, smtpUser, smtpPassword, smtpUseTLS, smtpRequireTLS);

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
	public String shortDescription() {
		return "Sets a manual SMTP configuration for the current mail";
	}
}