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

/** The single place where a Structr page becomes PDF bytes, without leaving the JVM. */
public class PdfRenderer {

	private static final Logger logger = LoggerFactory.getLogger(PdfRenderer.class.getName());

	// not an http URL: a reference is a path into the Structr filesystem, not a request to a server
	private static final String BASE_URI = "structr:///";

	/** Renders a DOMNode in the calling thread and transaction, so the caller's SecurityContext governs visibility. */
	public static String renderToHtml(final DOMNode root, final RenderContext renderContext) throws FrameworkException {

		final StringRenderBuffer buffer = new StringRenderBuffer();

		renderContext.setBuffer(buffer);
		root.render(renderContext, 0);

		return buffer.getBuffer().toString();
	}

	/** Converts rendered HTML into PDF bytes, laid out for print; documentName is used for log output only. */
	public static byte[] toPdf(final String html, final SecurityContext securityContext, final String documentName) throws FrameworkException {

		final StructrResourceFactory resources = new StructrResourceFactory(securityContext);
		final List<String> unsupportedCss      = new ArrayList<>();
		final ByteArrayOutputStream out        = new ByteArrayOutputStream();

		try {

			final PdfRendererBuilder builder = new PdfRendererBuilder();

			builder.withDiagnosticConsumer(diagnostic -> collect(diagnostic, unsupportedCss));

			// every reference the document makes, whatever its scheme, is answered from the graph first
			builder.useProtocolsStreamImplementation(resources, "structr", "http", "https", "file");

			// jsoup repairs the page engine's HTML5 into a well formed DOM, so it is not parsed twice
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

	/** Turns a page name into a file name safe to put into a response header. */
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

	/** A PDF that silently drops half its stylesheet is worse than one that fails, so both losses are logged. */
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
