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
package org.structr.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.Diagnostic;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.web.common.RenderContext;
import org.structr.web.common.StringRenderBuffer;
import org.structr.web.entity.dom.DOMNode;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * The single place where a Structr page becomes PDF bytes.
 *
 * Both entry points of this module, the {@code pdf()} scripting function and the PdfServlet, render
 * through here so that they cannot drift apart in behaviour or in error handling. Nothing leaves the
 * JVM: the page engine produces the markup, and the PDF is written by a library on the classpath.
 */
public class PdfRenderer {

	private static final Logger logger = LoggerFactory.getLogger(PdfRenderer.class.getName());

	/**
	 * The base every relative reference in the document resolves against. It is deliberately not an
	 * http URL: a reference is a path into the Structr filesystem, not a request to a web server.
	 */
	private static final String BASE_URI = "structr:///";

	/**
	 * Renders a DOMNode to HTML in the calling thread and the calling transaction, using the page
	 * engine directly. No HTTP request is made, so the caller's SecurityContext governs visibility.
	 */
	public static String renderToHtml(final DOMNode root, final RenderContext renderContext) throws FrameworkException {

		final StringRenderBuffer buffer = new StringRenderBuffer();

		renderContext.setBuffer(buffer);
		root.render(renderContext, 0);

		return buffer.getBuffer().toString();
	}

	/**
	 * Converts rendered HTML into PDF bytes.
	 *
	 * The document is laid out for print, so an {@code @media print} block applies and CSS that the
	 * renderer does not support is reported rather than passed over in silence.
	 *
	 * @param securityContext governs which files of the Structr filesystem the document may read
	 * @param documentName    used for log output only
	 */
	public static byte[] toPdf(final String html, final SecurityContext securityContext, final String documentName) throws FrameworkException {

		final StructrResourceFactory resources = new StructrResourceFactory(securityContext);
		final List<String> unsupportedCss      = new ArrayList<>();
		final ByteArrayOutputStream out        = new ByteArrayOutputStream();

		try {

			final PdfRendererBuilder builder = new PdfRendererBuilder();

			builder.withDiagnosticConsumer(diagnostic -> collect(diagnostic, unsupportedCss));

			// every reference the document makes, whatever its scheme, is answered from the graph first
			builder.useProtocolsStreamImplementation(resources, "structr", "http", "https", "file");

			// jsoup repairs the HTML5 the page engine emits into the well formed document the renderer
			// wants, and hands it over as a DOM rather than as text that would have to be parsed again
			builder.withW3cDocument(new W3CDom().fromJsoup(Jsoup.parse(html, BASE_URI)), BASE_URI);
			builder.toStream(out);
			builder.run();

		} catch (final Throwable t) {

			throw new FrameworkException(500, "pdf(): could not convert " + documentName + " to PDF: " + t.getMessage());
		}

		report(documentName, unsupportedCss, resources.getUnresolvedReferences());

		final byte[] pdf = out.toByteArray();

		if (pdf.length == 0) {

			throw new FrameworkException(500, "pdf(): conversion of " + documentName + " produced an empty document.");
		}

		return pdf;
	}

	/**
	 * Turns a page name into a file name that is safe to put into a response header.
	 */
	public static String fileNameFor(final String pageName) {

		if (StringUtils.isBlank(pageName)) {

			return "document.pdf";
		}

		// keep the page name recognisable, but a header value must not carry quotes or line breaks
		return pageName.replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf";
	}

	private static void collect(final Diagnostic diagnostic, final List<String> unsupportedCss) {

		if (diagnostic.getLevel().intValue() >= Level.WARNING.intValue()) {

			unsupportedCss.add(diagnostic.getFormattedMessage());
		}
	}

	/**
	 * A PDF that renders but silently drops half its stylesheet is worse than one that fails, so both
	 * kinds of loss are logged with the document they came from.
	 */
	private static void report(final String documentName, final List<String> unsupportedCss, final Set<String> unresolved) {

		if (!unsupportedCss.isEmpty()) {

			logger.warn("{} declarations in {} are not supported by the PDF renderer and were skipped. The renderer implements CSS 2.1 plus paged media, so flexbox, grid and custom properties have no effect on paper.", unsupportedCss.size(), documentName);

			for (final String message : unsupportedCss) {

				logger.warn("  {}", message);
			}
		}

		if (!unresolved.isEmpty()) {

			logger.warn("{} references in {} could not be resolved to a file and were left empty: {}", unresolved.size(), documentName, unresolved);
		}
	}
}
