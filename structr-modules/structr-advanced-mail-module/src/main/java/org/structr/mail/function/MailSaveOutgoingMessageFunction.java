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
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.mail.AdvancedMailModule;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class MailSaveOutgoingMessageFunction extends AdvancedMailModuleFunction {

	public MailSaveOutgoingMessageFunction(final AdvancedMailModule parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "mailSaveOutgoingMessage";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("doSave");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			ctx.getAdvancedMailContainer().setSaveOutgoingMessage((boolean)sources[0]);

			return "";

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${mailSaveOutgoingMessage(doSave)}"),
			Usage.javaScript("Usage: ${{ $.mailSaveOutgoingMessage(doSave) }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Configures if the current mail should be saved or not.";
	}

	@Override
	public String getLongDescription() {
		return """
				Configures the Advanced Mail Module that the next invocation of `mailSend()` should save the outgoing email as an `EMailMessage` node.
				Configured attachments are *copied* and attached to the `EMailMessage` node. For attached dynamic files the evaluated result is saved as a static file.
				After the `mailSend()` command is finished, the outgoing message can be accessed via `mailGetLastOutgoingMessage()`.
				""";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("doSave", "boolean indicating if mail should be saved or not")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"By default, mails are not saved"
		);
	}
}
