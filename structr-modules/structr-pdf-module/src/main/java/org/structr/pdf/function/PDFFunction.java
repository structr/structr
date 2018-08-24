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
package org.structr.pdf.function;

import com.github.jhonnymertz.wkhtmltopdf.wrapper.Pdf;
import com.github.jhonnymertz.wkhtmltopdf.wrapper.params.Param;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PDFFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_PDF = "Usage: ${ pdf(page [, wkhtmltopdfParameter, baseUrl]) }";
	public static final String ERROR_MESSAGE_PDF_JS = "Usage: ${{ Structr.pdf(page [, wkhtmltopdfParameter, baseUrl]); }}";


	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

		String baseUrl = null;
		String params = null;

		final String page = sources[0].toString();

		if (sources.length == 2) {

			params = sources[1].toString();
		}

		if (sources.length == 3) {

			 baseUrl = sources[2].toString();
		}

		if (baseUrl == null) {

			baseUrl = ctx.getBaseUrl() + "/";
		}

		logger.warn("Converting page {}{} to pdf.", baseUrl, page);

		Pdf pdf = new Pdf();
		pdf.addPageFromUrl(baseUrl + page);
		Principal currentUser = ctx.getSecurityContext().getUser(false);

		if (currentUser instanceof SuperUser) {

			pdf.addParam(new Param("--custom-header X-User superadmin --custom-header X-Password " + Settings.SuperUserPassword.getValue()), new Param("--custom-header-propagation"));

		} else {

			pdf.addParam(new Param("--cookie JSESSIONID " + ctx.getSecurityContext().getSessionId()));

		}

		if (params != null) {
			pdf.addParam(new Param(params));
		}

		try {

			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(pdf.getPDF());

			return baos.toString("ISO-8859-1");

		} catch (IOException e) {

			logger.warn("pdf(): IOException", e);

		} catch (InterruptedException e) {

			logger.warn("pdf(): InterruptedException", e);

		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {

		return (inJavaScriptContext ? ERROR_MESSAGE_PDF_JS : ERROR_MESSAGE_PDF);
	}

	@Override
	public String shortDescription() {

		return "Creates the PDF representation of a given page.";
	}

	@Override
	public String getName() {
		return "pdf()";
	}
}
