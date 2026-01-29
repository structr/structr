/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.DynamicMailAttachment;
import org.structr.common.helper.MailHelper;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.entity.File;

import java.util.ArrayList;
import java.util.List;

public class SendHtmlMailFunction extends UiAdvancedFunction {

	private static final Logger logger = LoggerFactory.getLogger(SendHtmlMailFunction.class.getName());

	@Override
	public String getName() {
		return "sendHtmlMail";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("fromAddress, fromName, toAddress, toName, subject, htmlContent, textContent [, files]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 7, 8);

			final String from        = sources[0].toString();
			final String fromName    = sources[1].toString();
			final String to          = sources[2].toString();
			final String toName      = sources[3].toString();
			final String subject     = sources[4].toString();
			final String htmlContent = sources[5].toString();
			final String textContent = sources[6].toString();

			List<NodeInterface> fileNodes = null;
			List<DynamicMailAttachment> attachments = new ArrayList<>();

			try {

				if (sources.length == 8 && sources[7] instanceof List list && list.size() > 0 && list.get(0) instanceof NodeInterface n && n.is(StructrTraits.FILE)) {

					fileNodes = list;

					for (final NodeInterface fileNode : fileNodes) {

						final File file                        = fileNode.as(File.class);
						final DynamicMailAttachment attachment = new DynamicMailAttachment();

						attachment.setName(file.getName());
						attachment.setDisposition(EmailAttachment.ATTACHMENT);

						if (file.isTemplate()) {

							attachment.setDataSource(file);

						} else {

							attachment.setDataSource(StorageProviderFactory.getStorageProvider(file));
						}

						attachments.add(attachment);
					}
				}

				return MailHelper.sendHtmlMail(from, fromName, to, toName, null, null, from, subject, htmlContent, textContent,attachments);

			} catch (EmailException ex) {

				logException(caller, ex, sources);
			}

		} catch (ArgumentNullException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{ $.sendHtmlMail(fromAddress, fromName, toAddress, toName, subject, htmlContent, textContent [, files]) }}."),
			Usage.structrScript("Usage: ${sendHtmlMail(fromAddress, fromName, toAddress, toName, subject, htmlContent, textContent [, files])}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Sends an HTML email.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"Attachments must be provided as a list, even when only a single file is included.",
				"`htmlContent` and `textContent` are typically generated using the `template()` function.",
				"Emails are sent based on the SMTP configuration defined in structr.conf.",
				"For advanced scenarios, refer to the extended mail functions prefixed with `mail`, beginning with `mailBegin()`."
		);
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("fromAddress", "sender address"),
				Parameter.mandatory("fromName", "sender name"),
				Parameter.mandatory("toAddress", "recipient address"),
				Parameter.mandatory("toName", "recipient name"),
				Parameter.mandatory("subject", "subject"),
				Parameter.mandatory("htmlContent", "HTML content"),
				Parameter.mandatory("textContent", "text content"),
				Parameter.optional("files", "collection of file nodes to send as attachments")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${sendPlaintextMail('info@structr.com', 'Structr', 'user@domain.com', 'Test User', 'Welcome to Structr', 'Hi User, welcome to <b>Structr</b>!', 'Hi User, welcome to Structr!', find('File', 'name', 'welcome-to-structr.pdf')))}")
		);
	}
}