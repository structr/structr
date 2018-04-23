/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.net.MalformedURLException;
import org.apache.commons.mail.EmailAttachment;
import org.structr.common.AdvancedMailContainer;
import org.structr.common.DynamicMailAttachment;
import org.structr.common.error.FrameworkException;
import org.structr.mail.DynamicFileDataSource;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.web.entity.File;


public class MailAddAttachmentFunction extends Function<Object, Object> {

	public final String ERROR_MESSAGE    = "Usage: ${mail_add_attachment(file[, name])}";
	public final String ERROR_MESSAGE_JS = "Usage: ${Structr.mail_add_attachment(file[, name])}";

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2)) {

			final AdvancedMailContainer amc = ctx.getAdvancedMailContainer();

			if (sources[0] instanceof File) {

				final File fileNode = (File)sources[0];
				final String attachmentName = (sources.length == 2) ? sources[1].toString() : fileNode.getName();

				try {

					addAttachment(amc, fileNode, attachmentName);

				} catch (MalformedURLException ex) {

					logException(caller, ex, sources);
				}
			}

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_JS : ERROR_MESSAGE);
	}

	@Override
	public String shortDescription() {
		return "";
	}

	@Override
	public String getName() {
		return "mail_add_attachment()";
	}

	public static void addAttachment(final AdvancedMailContainer amc, final File fileNode) throws MalformedURLException {
		addAttachment(amc, fileNode, fileNode.getName());
	}

	public static void addAttachment(final AdvancedMailContainer amc, final File fileNode, final String attachmentName) throws MalformedURLException {

		final DynamicMailAttachment attachment = new DynamicMailAttachment();
		attachment.setURL(fileNode.getFileOnDisk().toURI().toURL());
		attachment.setName(attachmentName);
		attachment.setDisposition(EmailAttachment.ATTACHMENT);

		if (fileNode.isTemplate()) {

			attachment.setIsDynamic(true);
			attachment.setDataSource(new DynamicFileDataSource(fileNode));

		} else {

			attachment.setIsDynamic(false);
			attachment.setURL(fileNode.getFileOnDisk().toURI().toURL());
		}

		amc.addAttachment(attachment);
	}
}