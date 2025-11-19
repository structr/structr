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

public class MailAddCcFunction extends AdvancedMailModuleFunction {

	public MailAddCcFunction(final AdvancedMailModule parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "mail_add_cc";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("address [, name ]");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			final String address = sources[0].toString();
			final String name    = (sources.length == 2) ? sources[1].toString() : null;

			ctx.getAdvancedMailContainer().addCc(address, name);

			return "";

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${mail_add_cc(address [, name])}"),
			Usage.javaScript("Usage: ${{ $.mailAddCc(address [, name]) }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Adds a `Cc:` recipient to the current mail.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("address", "address of the recipient"),
				Parameter.optional("name", "name of the recipient")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"can be called multiple times to add more recipients."
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.javaScript("""
						${{
							let project = $.this;

							$.mailBegin('sender@example.com', 'Project Service', 'Project Status Report: ' + project.name);

							$.mailAddTo(project.manager.eMail, project.manager.name);

							for (let member of project.members) {

								$.mailAddCc(member.eMail, member.name);
							}

							let htmlContent = $.template('Project-Status-Template', 'en', project);
							$.mailSetHtmlContent(htmlContent);

							$.mailSend();
						}}""", "Project Status Report mail where all project members are \"Cc:\" recipients and project manager is main \"To:\" recipient.")
		);
	}
}