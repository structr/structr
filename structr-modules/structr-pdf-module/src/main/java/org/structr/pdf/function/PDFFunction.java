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

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.pdf.PdfRenderer;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.web.common.FileHelper;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.Page;
import org.structr.websocket.DetachedHttpServletRequest;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PDFFunction extends Function<Object, Object> {

	@Override
	public String getName() {

		return "pdf";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("pagePath [, fileName [, parameters ]]");
	}

	@Override
	public String getRequiredModule() {

		return "pdf";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

		final SecurityContext securityContext = ctx.getSecurityContext();
		final String pagePath                 = sources[0].toString();

		if (pagePath.contains("?")) {

			throw new FrameworkException(422, "pdf(): the page path carries a query string. Request parameters are passed as the third parameter instead, for example pdf('invoice/<uuid>', 'invoice.pdf', { lang: 'de' }).");
		}

		final String[] parts = StringUtils.split(pagePath, "/");
		if (parts.length == 0 || parts.length > 2) {

			throw new FrameworkException(422, "pdf(): the page path is a page name, optionally followed by the id of the object the page renders, for example 'invoice/<uuid>'.");
		}

		final String pageName    = parts[0];
		final String detailsId   = parts.length == 2 ? parts[1] : null;
		final String fileName    = fileNameFrom(sources, pageName);
		final NodeInterface node = StructrApp.getInstance(securityContext).nodeQuery(StructrTraits.PAGE).name(pageName).getFirst();

		if (node == null) {

			throw new FrameworkException(422, "pdf(): no page named '" + pageName + "' exists, or it is not visible to the current user.");
		}

		final Page page                   = node.as(Page.class);
		final NodeInterface detailsObject = detailsId != null ? resolveDetailsObject(securityContext, detailsId) : null;

		// ${request.x} resolves through the SecurityContext, so the page gets a request of its own, restored afterwards
		final HttpServletRequest callersRequest = securityContext.getRequest();
		final HttpServletRequest pageRequest    = requestFor(callersRequest, parametersFrom(sources));

		securityContext.setRequest(pageRequest);

		try {

			// the page engine runs in this thread and transaction, so no request is made back to this server
			final RenderContext renderContext = new RenderContext(securityContext, pageRequest, null, RenderContext.EditMode.NONE);

			if (detailsObject != null) {

				renderContext.setDetailsDataObject(detailsObject);
			}

			final String html = PdfRenderer.renderToHtml(page, renderContext);
			final byte[] pdf  = PdfRenderer.toPdf(html, securityContext, pageName);

			return FileHelper.createFile(securityContext, pdf, "application/pdf", StructrTraits.FILE, fileName, true);

		} catch (final IOException ioex) {

			throw new FrameworkException(500, "pdf(): could not store the generated document: " + ioex.getMessage());

		} finally {

			securityContext.setRequest(callersRequest);
		}
	}

	/** The object the page renders as current, by id then by name; not found is an error, not a blank document. */
	private NodeInterface resolveDetailsObject(final SecurityContext securityContext, final String detailsId) throws FrameworkException {

		final NodeInterface byId = StructrApp.getInstance(securityContext).getNodeById(detailsId);
		if (byId != null) {

			return byId;
		}

		final NodeInterface byName = StructrApp.getInstance(securityContext).nodeQuery(StructrTraits.NODE_INTERFACE).name(detailsId).getFirst();
		if (byName != null) {

			return byName;
		}

		throw new FrameworkException(422, "pdf(): no object with id or name '" + detailsId + "' exists, or it is not visible to the current user.");
	}

	private Map<String, String[]> parametersFrom(final Object[] sources) throws FrameworkException {

		final Map<String, String[]> parameters = new LinkedHashMap<>();

		if (sources.length < 3 || sources[2] == null) {

			return parameters;
		}

		if (!(sources[2] instanceof Map)) {

			throw new FrameworkException(422, "pdf(): the third parameter is an object of request parameters, for example { lang: 'de', draft: true }.");
		}

		for (final Map.Entry<?, ?> entry : ((Map<?, ?>) sources[2]).entrySet()) {

			if (entry.getKey() != null && entry.getValue() != null) {

				parameters.put(entry.getKey().toString(), new String[] { entry.getValue().toString() });
			}
		}

		return parameters;
	}

	private HttpServletRequest requestFor(final HttpServletRequest callersRequest, final Map<String, String[]> parameters) {

		// keeps the caller's headers, cookies and locale, which a page may read, but never its parameters
		final DetachedHttpServletRequest request = callersRequest != null
			? new DetachedHttpServletRequest(callersRequest)
			: new DetachedHttpServletRequest();

		request.getParameterMap().clear();
		request.getParameterMap().putAll(parameters);

		return request;
	}

	/** An argument string is refused, not ignored: it would yield a plausible document missing whatever it was for. */
	private String fileNameFrom(final Object[] sources, final String pageName) throws FrameworkException {

		if (sources.length > 3) {

			throw new FrameworkException(422, "pdf(): takes a page path, an optional file name and an optional object of request parameters. The wkhtmltopdf parameters of earlier versions no longer exist, see the 7.x migration notes.");
		}

		if (sources.length == 2 && sources[1] != null) {

			final String second = sources[1].toString();
			if (second.trim().startsWith("-")) {

				throw new FrameworkException(422, "pdf(): the second parameter is the file name of the generated document. The wkhtmltopdf parameters of earlier versions no longer exist: headers, footers and page numbers are now written in the page's print stylesheet. See the 7.x migration notes.");
			}

			return second;
		}

		return PdfRenderer.fileNameFor(pageName);
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(Usage.structrScript("Usage: ${ pdf(pagePath [, fileName [, parameters ]]) }"), Usage.javaScript("Usage: ${{ $.pdf(pagePath [, fileName [, parameters ]]); }}"));
	}

	@Override
	public String getShortDescription() {

		return "Renders a page and stores the result as a PDF file.";
	}

	@Override
	public String getLongDescription() {

		return "Renders the given page with the Structr page engine, converts the result to PDF and returns the new File object. The page is rendered in the current user's context, so it sees exactly what that user is allowed to see, and no HTTP request is made back to this server.";
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
				Example.structrScript("${ pdf('statement') }", "Renders the page 'statement' and returns a File named statement.pdf"),
				Example.structrScript("${ pdf(concat('invoice/', current.id)) }", "Renders the page 'invoice' for a specific object, which the page reads as `current`"),
				Example.javaScript("""
						${{
						    // the page renders one order, in German, and the result is stored as a File
						    let order = $.first($.find('Order', { orderNumber: '2026-0042' }));

						    let file = $.pdf('invoice/' + order.id, 'invoice-2026-0042.pdf', { lang: 'de' });

						    // inside the page, current is the order and ${request.lang} is 'de'
						    return file;
						}}
						"""));
	}

	@Override
	public List<String> getNotes() {

		return List.of(
				"The PDF is produced inside the JVM. Unlike earlier versions there is no external binary to install, and the function works from a cron job or `doPrivileged` context as well as from a request.",
				"""
				The renderer implements **CSS 2.1 plus paged media**. Flexbox, grid, custom properties (`var(--x)`) and JavaScript have no effect on paper, so a PDF is a print document with its own stylesheet rather than a screenshot of the screen layout. Declarations the renderer cannot use are written to the server log rather than dropped silently.
				""",
				"""
				Headers, footers and page numbers are written in CSS instead of being separate pages:
				```css
				@page {
				    size: A4;
				    margin: 25mm 18mm;
				    @top-left     { content: element(docheader); }
				    @bottom-right { content: "Page " counter(page) " of " counter(pages); }
				}
				#docheader { position: running(docheader); }
				```
				""",
				"The page is rendered with a request of its own, carrying exactly the parameters passed to the function and no others, so the same call produces the same document from a page, a cron job or `doPrivileged`. The caller's headers, cookies and locale are still visible to the page.",
				"Images, stylesheets and fonts are read from the Structr filesystem by path, under the permissions of the user the page is rendered as. A `@font-face` rule pointing at a font file in the filesystem is embedded in the document, which is what non Latin-1 text needs. External URLs are not loaded unless `pdf.resources.external.allowed` is enabled.");
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("pagePath", "the name of the page to render, optionally followed by the id of the object it renders, for example `invoice/<uuid>`. That object is available in the page as `current`"),
				Parameter.optional("fileName", "the name of the generated file. Defaults to the page name with a .pdf extension"),
				Parameter.optional("parameters", "an object of request parameters, readable in the page as `${request.<name>}`. The page sees these and no others")
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.InputOutput;
	}
}
