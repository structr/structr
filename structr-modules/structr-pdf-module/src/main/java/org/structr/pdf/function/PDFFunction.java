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

import java.io.IOException;
import java.util.List;

public class PDFFunction extends Function<Object, Object> {

	@Override
	public String getName() {

		return "pdf";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("pageName [, fileName ]");
	}

	@Override
	public String getRequiredModule() {

		return "pdf";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

		final SecurityContext securityContext = ctx.getSecurityContext();
		final String pageName                 = sources[0].toString();

		final String fileName = fileNameFrom(sources, pageName);

		final NodeInterface node = StructrApp.getInstance(securityContext).nodeQuery(StructrTraits.PAGE).name(pageName).getFirst();

		if (node == null) {

			throw new FrameworkException(422, "pdf(): no page named '" + pageName + "' exists, or it is not visible to the current user.");
		}

		final Page page = node.as(Page.class);

		// the page engine runs here, in this thread and this transaction, so no request is made back to
		// this server and the current user's permissions apply to the page and to everything it reads
		final RenderContext renderContext = new RenderContext(securityContext, securityContext.getRequest(), null, RenderContext.EditMode.NONE);
		final String html                 = PdfRenderer.renderToHtml(page, renderContext);
		final byte[] pdf                  = PdfRenderer.toPdf(html, securityContext, pageName);

		try {

			return FileHelper.createFile(securityContext, pdf, "application/pdf", StructrTraits.FILE, fileName, true);

		} catch (final IOException ioex) {

			throw new FrameworkException(500, "pdf(): could not store the generated document: " + ioex.getMessage());
		}
	}

	/**
	 * The second parameter used to carry wkhtmltopdf arguments and is now a file name. An argument
	 * string is refused rather than quietly ignored: it would have produced a document that looks
	 * plausible and is missing whatever those arguments were for.
	 */
	private String fileNameFrom(final Object[] sources, final String pageName) throws FrameworkException {

		if (sources.length > 2) {

			throw new FrameworkException(422, "pdf(): takes a page name and an optional file name. The wkhtmltopdf parameters of earlier versions no longer exist, see the 7.x migration notes.");
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

		return List.of(
				Usage.structrScript("Usage: ${ pdf(pageName [, fileName ]) }"),
				Usage.javaScript("Usage: ${{ $.pdf(pageName [, fileName ]); }}")
		);
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
				Example.structrScript("${ pdf('statement', concat('statement-', me.name, '.pdf')) }", "Renders the page and names the file after the current user"),
				Example.javaScript("""
						${{
						    // render a page to PDF and offer it to the user as a download
						    let file = $.pdf('invoice', 'invoice-2026-0042.pdf');

						    $.setResponseHeader('Content-Disposition', 'attachment; filename="' + file.name + '"');

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
				"Images, stylesheets and fonts are read from the Structr filesystem by path, under the permissions of the user the page is rendered as. A `@font-face` rule pointing at a font file in the filesystem is embedded in the document, which is what non Latin-1 text needs. External URLs are not loaded unless `pdf.resources.external.allowed` is enabled.");
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("pageName", "the name of the page to render"),
				Parameter.optional("fileName", "the name of the generated file. Defaults to the page name with a .pdf extension")
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.InputOutput;
	}
}
