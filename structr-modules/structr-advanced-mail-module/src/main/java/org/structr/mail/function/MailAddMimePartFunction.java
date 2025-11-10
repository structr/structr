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

import org.apache.commons.mail.EmailAttachment;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.AdvancedMailContainer;
import org.structr.common.helper.DynamicMailAttachment;
import org.structr.docs.Signature;
import org.structr.mail.AdvancedMailModule;
import org.structr.mail.DynamicFileDataSource;
import org.structr.schema.action.ActionContext;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.entity.File;

import java.net.MalformedURLException;
import java.util.List;

public class MailAddMimePartFunction extends AdvancedMailModuleFunction {

	public final String ERROR_MESSAGE    = "Usage: ${mail_add_mime_part(content, contentType)}";
	public final String ERROR_MESSAGE_JS = "Usage: ${{ Structr.mailAddMimePart(content, contentType) }}";

	public MailAddMimePartFunction(final AdvancedMailModule parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "mail_add_mime_part";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("content, contentType");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			final AdvancedMailContainer amc = ctx.getAdvancedMailContainer();

			amc.addMimePart(sources[0].toString(), sources[1].toString());

			return "";

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_JS : ERROR_MESSAGE);
	}

	@Override
	public String getShortDescription() {
		return "Adds an attachment file and an optional file name to the current mail";
	}

	public static void addAttachment(final AdvancedMailContainer amc, final File fileNode) throws MalformedURLException {
		addAttachment(amc, fileNode, fileNode.getName());
	}

	public static void addAttachment(final AdvancedMailContainer amc, final File fileNode, final String attachmentName) throws MalformedURLException {

		final DynamicMailAttachment attachment = new DynamicMailAttachment();
		attachment.setName(attachmentName);
		attachment.setDisposition(EmailAttachment.ATTACHMENT);

		if (fileNode.isTemplate()) {

			attachment.setDataSource(new DynamicFileDataSource(fileNode));

		} else {

			attachment.setDataSource(StorageProviderFactory.getStorageProvider(fileNode));

		}

		amc.addAttachment(attachment);
	}
}