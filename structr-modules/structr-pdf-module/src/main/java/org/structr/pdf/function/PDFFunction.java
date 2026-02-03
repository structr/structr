/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import com.github.jhonnymertz.wkhtmltopdf.wrapper.configurations.WrapperConfig;
import com.github.jhonnymertz.wkhtmltopdf.wrapper.configurations.XvfbConfig;
import com.github.jhonnymertz.wkhtmltopdf.wrapper.params.Param;
import jakarta.servlet.http.HttpSession;
import org.eclipse.jetty.server.Session;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.rest.auth.JWTHelper;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PDFFunction extends Function<Object, Object> {

	@Override
	public String getName() {
		return "pdf";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("pageName [, wkthtmlParams, baseUrl, runWithX, xSettings ]");
	}

	@Override
	public String getRequiredModule() {
		return "pdf";
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

		String baseUrl         = null;
		String userParameter   = null;
		Boolean runWithXserver = false;
		String xServerSettings = null;
		final String page      = sources[0].toString();

		if (sources.length >= 2) {

			userParameter = sources[1].toString();
		}

		if (sources.length >= 3) {

			 baseUrl = sources[2].toString();
		}

		if (sources.length >= 4) {

			runWithXserver = (Boolean) sources[3];
		}

		if (sources.length >= 5) {

			xServerSettings = sources[4].toString();
		}

		if (baseUrl == null || baseUrl.length() == 0) {

			baseUrl = ActionContext.getBaseUrl(ctx.getSecurityContext().getRequest()) + "/";
		}

		Principal currentUser = ctx.getSecurityContext().getUser(false);

		final List<Param> parameterList = new ArrayList<>();

		if (currentUser instanceof SuperUser) {

			throw new FrameworkException(422, "Error: Using the pdf() function without a user context (e.g. cron job or $.doPrivileged) is deprecated. This can be easily remedied by using the $.doAs() function to create the pdf as a given user.");

		} else {

			final HttpSession session = ctx.getSecurityContext().getSession();

			if (session != null) {

				final String sessionId = (session instanceof Session) ? ((Session) session).getExtendedId() : session.getId();

				parameterList.add(new Param("--cookie", "JSESSIONID", sessionId));

			} else {

				try {

					// Fallback: Create token for user with minimal lifetime and no refresh token
					final Calendar accessTokenExpirationDate = Calendar.getInstance();
					accessTokenExpirationDate.add(Calendar.MINUTE, 1);

					final Map<String, String> tokens = JWTHelper.createTokensForUser(currentUser, accessTokenExpirationDate.getTime(), null);

					parameterList.add(new Param("--cookie", "access_token", tokens.get("access_token")));

				} catch (Throwable t) {

					// only log in error case to reduce verbosity
					logger.info("pdf(): No session information available and fallback method of creating a JWT for user also failed. Please see log output.");

					// simply re-throw
					throw t;
				}
			}
		}

		if (userParameter != null) {

			// use regular expression to extract quoted parts and single terms to be able to convert them to params
			final Map<String, List<String>> map = new HashMap<>();
			final Matcher matcher = Pattern.compile("(\"[^\"]*\")|(\\S+)").matcher(userParameter);
			String lastParam = "";
			while (matcher.find()) {

				final String val = (matcher.group(1) != null) ? matcher.group(1) : matcher.group(2);

				if (val.length() > 0) {
					if (val.startsWith("-")) {
						lastParam = val;
						map.put(val, new ArrayList());
					} else {
						map.get(lastParam).add(val);
					}
				}
			}

			// now convert Map to Param objects
			map.entrySet().stream().forEach(e -> {
				final String paramName         = e.getKey();
				final List<String> paramValues = e.getValue();

				if (paramValues.size() == 0) {
					parameterList.add(new Param(paramName));
				} else {
					parameterList.add(new Param(paramName, paramValues.toArray(new String[0])));
				}
			});
		}

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {

			if (!runWithXserver) {

				return convertPageToPdfWithoutXServer(baseUrl, page, parameterList, baos);

			} else {

				return convertPageToPdfWithXServer(baseUrl, page, parameterList, baos, xServerSettings);
			}

		} catch (final Throwable t) {

			logger.warn("Could not convert page {}{} to pdf... retrying with xvfb...", baseUrl, page);

			return convertPageToPdfWithXServer(baseUrl, page, parameterList, baos, xServerSettings);
		}
	}

	private  String convertPageToPdfWithoutXServer (String baseUrl, String page, List<Param> parameterList, ByteArrayOutputStream baos) {

		Pdf pdf = new Pdf();
		pdf.addPageFromUrl(baseUrl + page);
		addParametersToPdf(pdf, parameterList);

		return convertPageToPdf(pdf, baos);
	}

	private String convertPageToPdfWithXServer (String baseUrl, String page, List<Param> parameterList, ByteArrayOutputStream baos, String xServerSettings) {

		XvfbConfig xc = new XvfbConfig();

		if (xServerSettings == null || xServerSettings.length() == 0) {

			xc.addParams(new Param("--auto-servernum"), new Param("--server-num=1"));

		} else {

			xc.addParams(new Param(xServerSettings));
		}

		WrapperConfig wc = new WrapperConfig();
		wc.setXvfbConfig(xc);

		Pdf pdf = new Pdf(wc);
		pdf.addPageFromUrl(baseUrl + page);
		addParametersToPdf(pdf, parameterList);

		return convertPageToPdf(pdf, baos);
	}

	private String convertPageToPdf (Pdf pdf, ByteArrayOutputStream baos) {

		try {

			baos.write(pdf.getPDF());
			return baos.toString("ISO-8859-1");

		} catch (IOException e) {

			logger.warn("pdf(): IOException", e);

		} catch (InterruptedException e) {

			logger.warn("pdf(): InterruptedException", e);
		}

		return "";
	}

	private void addParametersToPdf (Pdf pdf, List<Param> paramList) {

		for (Param param : paramList) {
			pdf.addParam(param);
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
				Usage.structrScript("Usage: ${ pdf(page [, wkhtmltopdfParameter, baseUrl, runWithXServer, xServerSettings]) }"),
				Usage.javaScript("Usage: ${{ $.pdf(page [, wkhtmltopdfParameter, baseUrl, runWithXServer, xServerSettings]); }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Creates the PDF representation of a given page.";
	}

	@Override
	public String getLongDescription() {
		return "Returns a PDF string representation of the given page.";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${set_content(create('File', 'name', 'new_document.pdf'), pdf('pdf-export-page'), 'ISO-8859-1')}", "Creates a new file for each run of the script"),
				Example.javaScript("""
						${{
						    // download pdf file as "my-downloaded-file.pdf"
						    $.setResponseHeader('Content-Disposition', 'attachment; filename="my-downloaded-file.pdf"');
						    $.set_response_header('Cache-Control', 'no-cache');
						    // These variables reference local pages in the structr installation
						    let main   = 'pdf-export-main-page/';
						    let header = '--header-html ' + $.get('base_url') + '/pdf-export-header-page/';
						    let footer   = '--footer-html ' + $.get('base_url') + '/pdf-export-footer-page/';
						    let wkhtmlArgs   = header + ' ' + footer + ' --disable-smart-shrinking';
						    let pdf = $.pdf(main, wkhtmlArgs);
						    $.print(pdf);
						}}
						"""));
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"The PDF functionality relies on other software: wkhtmltopdf. This needs to be installed on the server. It is recommended to install a [wkhtmltopdf release](https://github.com/wkhtmltopdf/wkhtmltopdf/releases) from github to ensure that a version with patched qt is installed. See the [autogenerated documentation](https://wkhtmltopdf.org/usage/wkhtmltopdf.txt) for wkhtmltopdf.",
				"**IMPORTANT**: If you are creating a PDF document from a **dynamic file**, make sure that there are no extraneous whitespaces after the dynamic script content. This will lead to corrupt PDFs and is very hard to detect! The dynamic file should have the charset `ISO-8859-1` specified in its contentType (e.g. `application/octet-stream; charset=ISO-8859-1`). If you experience caching issues, make sure that the `dontCache` flag of the file is set to `true`",
				"""
				When using page-based HTML headers and/or footers the following keys are appended to the request URL so they can be used directly in the page:
				`${request.page}`       Replaced by the number of the pages currently being printed
				`${request.frompage}`   Replaced by the number of the first page to be printed
				`${request.topage}`     Replaced by the number of the last page to be printed
				`${request.webpage}`    Replaced by the URL of the page being printed
				`${request.section}`    Replaced by the name of the current section
				`${request.subsection}` Replaced by the name of the current subsection
				`${request.date}`       Replaced by the current date in system local format
				`${request.isodate}`    Replaced by the current date in ISO 8601 extended format
				`${request.time}`       Replaced by the current time in system local format
				`${request.title}`      Replaced by the title of the of the current page object
				`${request.doctitle}`   Replaced by the title of the output document
				`${request.sitepage}`   Replaced by the number of the page in the current site being converted
				`${request.sitepages}`  Replaced by the number of pages in the current site being converted
				""");
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("page", "the page that should be rendered as a PDF"),
				Parameter.optional("wkhtmltopdfParameter", "this string is passed to wkhtmltopdf and may contain all parameters that wkhtmltopdf accepts. A useful parameter is `--disable-smart-shrinking`"),
				Parameter.optional("baseUrl", "the baseUrl for the main page (the header and footer page need the baseUrl explicitly as they are currently provided as a string). Defaults to the value of the keyword `base_url`"),
				Parameter.optional("runWithXServer", "forces the usage of xvfb"),
				Parameter.optional("xServerSettings", "parameters for xvfb")
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.InputOutput;
	}
}
