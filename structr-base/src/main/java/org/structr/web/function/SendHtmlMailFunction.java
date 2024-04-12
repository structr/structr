/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.storage.StorageProviderFactory;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.File;

import java.util.ArrayList;
import java.util.List;
import org.structr.common.helper.DynamicMailAttachment;
import org.structr.common.helper.MailHelper;

public class SendHtmlMailFunction extends UiAdvancedFunction {

	private static final Logger logger = LoggerFactory.getLogger(SendHtmlMailFunction.class.getName());

	public static final String ERROR_MESSAGE_SEND_HTML_MAIL = "Usage: ${send_html_mail(fromAddress, fromName, toAddress, toName, subject, htmlContent, textContent [, files])}.";

	@Override
	public String getName() {
		return "send_html_mail";
	}

	@Override
	public String getSignature() {
		return "fromAddress, fromName, toAddress, toName, subject, htmlContent, textContent [, files]";
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

			List<File> fileNodes = null;
			List<DynamicMailAttachment> attachments = new ArrayList<>();

			try {

				if (sources.length == 8 && sources[7] instanceof List && ((List) sources[7]).size() > 0 && ((List) sources[7]).get(0) instanceof File) {

					fileNodes = (List<File>) sources[7];

					for (File fileNode : fileNodes) {

						final DynamicMailAttachment attachment = new DynamicMailAttachment();
						attachment.setName(fileNode.getProperty(File.name));
						attachment.setDisposition(EmailAttachment.ATTACHMENT);

						if (fileNode.isTemplate()) {

							attachment.setDataSource(fileNode);

						} else {

							attachment.setDataSource(StorageProviderFactory.getStorageProvider(fileNode));
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
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_SEND_HTML_MAIL;
	}

	@Override
	public String shortDescription() {
		return "Sends an HTML e-mail";
	}
}