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
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.mail.AdvancedMailModule;
import org.structr.mail.DynamicFileDataSource;
import org.structr.schema.action.ActionContext;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.entity.File;

import java.net.MalformedURLException;
import java.util.List;

public class MailAddAttachmentFunction extends AdvancedMailModuleFunction {

	public MailAddAttachmentFunction(final AdvancedMailModule parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "mail_add_attachment";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("file [, name ]");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			final AdvancedMailContainer amc = ctx.getAdvancedMailContainer();

			if (sources[0] instanceof NodeInterface n && n.is(StructrTraits.FILE)) {

				final File fileNode = n.as(File.class);
				final String attachmentName = (sources.length == 2) ? sources[1].toString() : fileNode.getName();

				try {

					addAttachment(amc, fileNode, attachmentName);

				} catch (MalformedURLException ex) {

					logException(caller, ex, sources);
				}
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
			Usage.structrScript("Usage: ${mail_add_attachment(file[, name])}"),
			Usage.javaScript("Usage: ${{ Structr.mailAddAttachment(file[, name]) }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Adds an attachment with an optional file name to the current mail.";
	}

	@Override
	public String getLongDescription() {
		return """
			Adds a file as an attachment to the mail. The `name` parameter can be used to send the file with a different name than the filename in the virtual filesystem.
			
			If the given file is a dynamic file, it will be evaluated at the time the mail is being sent.
			""";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("file", "file node from the virtual filesystem"),
				Parameter.optional("name", "file name of attachment (defaults to the actual file name if omitted)")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"can be called multiple times to add more attachments."
		);
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