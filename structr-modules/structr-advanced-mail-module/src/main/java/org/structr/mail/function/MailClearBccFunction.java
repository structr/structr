/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.docs.Usage;
import org.structr.mail.AdvancedMailModule;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class MailClearBccFunction extends AdvancedMailModuleFunction {

	public MailClearBccFunction(final AdvancedMailModule parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "mailClearBcc";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		ctx.getAdvancedMailContainer().clearBcc();

		return "";
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${mailClearBcc()}"),
			Usage.javaScript("Usage: ${{ $.mailClearBcc() }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Clears the current list of `Bcc:` recipients.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}