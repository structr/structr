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
import java.util.List;
import org.structr.common.AdvancedMailContainer;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.web.entity.File;


public class MailBeginFunction extends Function<Object, Object> {

	public final String ERROR_MESSAGE    = "Usage: ${mail_begin(fromAddress, fromName, subject, htmlContent[, textContent][, files])}";
	public final String ERROR_MESSAGE_JS = "Usage: ${Structr.mail_begin(type[, view])}";


	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 4, 6)) {

			final AdvancedMailContainer amc = ctx.getAdvancedMailContainer();

			final String from = sources[0].toString();
			final String fromName = sources[1].toString();
			final String subject = sources[2].toString();
			final String htmlContent = sources[3].toString();
			String textContent = "";

			if (sources.length >= 5) {
				textContent = sources[4].toString();
			}

			amc.init(from, fromName, subject, htmlContent, textContent);

			if (sources.length == 6 && sources[5] instanceof List && ((List) sources[5]).size() > 0 && ((List) sources[5]).get(0) instanceof File) {

				for (File fileNode : (List<File>) sources[5]) {

					try {

						MailAddAttachmentFunction.addAttachment(amc, fileNode);

					} catch (MalformedURLException ex) {

						logException(caller, ex, sources);
					}
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
		return "mail_begin()";
	}
}
