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
import org.structr.common.helper.AdvancedMailContainer;
import org.structr.core.graph.NodeInterface;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.mail.AdvancedMailModule;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.File;

import java.net.MalformedURLException;
import java.util.List;

public class MailBeginFunction extends AdvancedMailModuleFunction {

	public MailBeginFunction(final AdvancedMailModule parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "mail_begin";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("fromAddress [, fromName[, subject[, htmlContent[, textContent[, files]]]]]");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 6);

			final AdvancedMailContainer amc = ctx.getAdvancedMailContainer();

			// clear current mail container in case it has been used before in the same scripting environment
			amc.clearMailContainer();

			switch (sources.length) {
				case 6: {
					if (sources[5] instanceof List && ((List) sources[5]).size() > 0 && ((List) sources[5]).get(0) instanceof NodeInterface) {

						for (NodeInterface fileNode : (List<NodeInterface>) sources[5]) {

							final File file = fileNode.as(File.class);

							try {

								MailAddAttachmentFunction.addAttachment(amc, file);

							} catch (MalformedURLException ex) {

								logException(caller, ex, sources);
							}
						}
					}
				}
				case 5: amc.setTextContent(sources[4].toString());
				case 4: amc.setHtmlContent(sources[3].toString());
				case 3: amc.setSubject(sources[2].toString());
				case 2: amc.setFromName(sources[1].toString());
				case 1: amc.setFromAddress(sources[0].toString());
			}

			return "";

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${mail_begin(fromAddress[, fromName[, subject[, htmlContent[, textContent[, files]]]]])}"),
			Usage.javaScript("Usage: ${{ $.mailBegin(fromAddress[, fromName[, subject[, htmlContent[, textContent[, files]]]]]) }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Begins a new mail configuration.";
	}

	@Override
	public String getLongDescription() {
		return """
				Begins an HTML email with the basic given configuration. Previously started configurations are cleared.

				The optional parameters can be omitted and selectively be set via `mail_set_from()`, `mail_set_subject()`, `mail_set_html_content()`, `mail_set_text_content()` and `mail_add_attachment()`.

				Recipients are added via the `mail_add_to()`, `mail_add_cc()` and `mail_add_bcc()` functions. This is separated out to allow for emails with only CC and/or BCC addresses.
				""";
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"`htmlContent` and `textContent` are typically generated using the `template()` function.",
				"By default, emails are sent based on the SMTP configuration defined in structr.conf. This can be changed using `mail_set_manual_config()` and `mail_reset_manual_config()`"
		);
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("fromAddress", "sender address"),
				Parameter.optional("fromName", "sender name"),
				Parameter.optional("subject", "subject"),
				Parameter.optional("htmlContent", "HTML content"),
				Parameter.optional("textContent", "text content"),
				Parameter.optional("files", "collection of file nodes to send as attachments")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.javaScript("""
						${{
							let projectTask  = $.this;
							let project      = projectTask.project;
							let assignee     = projectTask.assignee;
							let manager      = project.manager;
							let stakeHolders = projectTask.getStakeHolders();

							$.mailBegin('sender@example.com', 'Project Service', 'Project Task Update: ' + projectTask.name);

							$.mailAddTo(assignee.eMail, assignee.name);

							for (let stakeHolder of stakeHolders) {

								$.mailAddCc(stakeHolder.eMail, stakeHolder.name);
							}

							if (project.archiveMailbox) {
								$.mailAddBcc(project.archiveMailbox);
							}

							let htmlContent = $.template('Project-Task-Update-HTML', 'en', projectTask);
							$.mailSetHtmlContent(htmlContent);

							let textContent = $.template('Project-Task-Update-TEXT', 'en', projectTask);
							$.mailSetTextContent(textContent);

							$.mailSend();
						}}""", "Project task update with multiple different recipients")
		);
	}
}
