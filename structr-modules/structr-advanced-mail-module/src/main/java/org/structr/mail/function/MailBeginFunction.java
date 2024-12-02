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
import org.structr.web.entity.File;

import java.net.MalformedURLException;
import java.util.List;
import org.structr.common.helper.AdvancedMailContainer;

public class MailBeginFunction extends AdvancedMailModuleFunction {

	public final String ERROR_MESSAGE    = "Usage: ${mail_begin(fromAddress[, fromName[, subject[, htmlContent[, textContent[, files]]]]])}";
	public final String ERROR_MESSAGE_JS = "Usage: ${{ Structr.mail_begin(fromAddress[, fromName[, subject[, htmlContent[, textContent[, files]]]]]) }}";

	public MailBeginFunction(final AdvancedMailModule parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "mail_begin";
	}

	@Override
	public String getSignature() {
		return "fromAddress [, fromName[, subject[, htmlContent[, textContent[, files]]]]]";
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
					if (sources[5] instanceof List && ((List) sources[5]).size() > 0 && ((List) sources[5]).get(0) instanceof File) {

						for (File fileNode : (List<File>) sources[5]) {

							try {

								MailAddAttachmentFunction.addAttachment(amc, fileNode);

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
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_JS : ERROR_MESSAGE);
	}

	@Override
	public String shortDescription() {
		return "Begins a new mail configuration - previously started configurations are cleared.";
	}
}
