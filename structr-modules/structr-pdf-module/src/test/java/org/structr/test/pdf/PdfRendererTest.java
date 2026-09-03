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
package org.structr.test.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.pdf.PdfRenderer;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.Page;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

public class PdfRendererTest extends StructrUiTest {

	@Test
	public void testRenderToHtmlUsesThePageEngine() {

		try (final Tx tx = app.tx()) {

			final Page page   = Page.createSimplePage(securityContext, "report");
			final String html = PdfRenderer.renderToHtml(page, new RenderContext(securityContext));

			assertTrue("Rendered output is not an HTML document: " + html, html.contains("<html"));
			assertTrue("Page content is missing from the rendered output: " + html, html.contains("Initial body text"));

			// the simple page titles itself with ${capitalize(page.name)}, so a rendered "Report" proves
			// that expressions were evaluated rather than emitted verbatim
			assertTrue("Scripting was not evaluated during the render: " + html, html.contains("Report"));

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	/**
	 * The whole pipeline, asserted by reading the result back rather than by looking at it. This is only
	 * possible because the renderer runs in the JVM: the document it produces can be inspected with the
	 * PDFBox that produced it.
	 */
	@Test
	public void testPageBecomesAReadablePdf() {

		try (final Tx tx = app.tx()) {

			final Page page   = Page.createSimplePage(securityContext, "statement");
			final String html = PdfRenderer.renderToHtml(page, new RenderContext(securityContext));

			final byte[] pdf = PdfRenderer.toPdf(html, securityContext, "statement");

			assertTrue("Conversion produced no bytes", pdf.length > 0);
			assertTrue("Result is not a PDF document", pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F');

			try (final PDDocument document = Loader.loadPDF(pdf)) {

				assertEquals("A one page document was expected", 1, document.getNumberOfPages());

				final String text = new PDFTextStripper().getText(document);

				assertTrue("The page content did not survive into the PDF: " + text, text.contains("Initial body text"));
			}

			tx.success();

		} catch (final Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception: " + t.getMessage());
		}
	}

	/**
	 * Headers, footers and page numbers are the capability that replaced wkhtmltopdf's separate header
	 * and footer pages, so the counters are asserted per page rather than assumed.
	 */
	@Test
	public void testPagedMediaProducesRunningHeadersAndPageNumbers() {

		final String html = """
			<html><head><style>
			  @page {
			    size: A4; margin: 2cm;
			    @top-center    { content: element(runhead); }
			    @bottom-center { content: "Page " counter(page) " of " counter(pages); }
			  }
			  #runhead { position: running(runhead); }
			  .sheet { page-break-after: always; }
			</style></head><body>
			  <div id="runhead">STATEMENT SR-1</div>
			  <div class="sheet">First</div>
			  <div>Second</div>
			</body></html>""";

		try {

			final byte[] pdf = PdfRenderer.toPdf(html, securityContext, "paged");

			try (final PDDocument document = Loader.loadPDF(pdf)) {

				assertEquals("page-break-after did not start a new page", 2, document.getNumberOfPages());

				assertTrue("The running header is missing from page 2: " + textOf(document, 2), textOf(document, 2).contains("STATEMENT SR-1"));
				assertTrue("Page numbering is wrong on page 1: " + textOf(document, 1), textOf(document, 1).contains("Page 1 of 2"));
				assertTrue("Page numbering is wrong on page 2: " + textOf(document, 2), textOf(document, 2).contains("Page 2 of 2"));
			}

		} catch (final Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception: " + t.getMessage());
		}
	}

	/**
	 * Unsupported CSS must not abort a document. The content still has to arrive, laid out as blocks.
	 */
	@Test
	public void testUnsupportedCssDoesNotFailTheDocument() {

		final String html = "<html><head><style>.row { display: flex; gap: 1rem; }</style></head>"
			+ "<body><div class=\"row\"><div>LEFT</div><div>RIGHT</div></div></body></html>";

		try {

			final byte[] pdf = PdfRenderer.toPdf(html, securityContext, "flex");

			try (final PDDocument document = Loader.loadPDF(pdf)) {

				final String text = new PDFTextStripper().getText(document);

				assertTrue("Content was lost along with the unsupported layout: " + text, text.contains("LEFT") && text.contains("RIGHT"));
			}

		} catch (final Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception: " + t.getMessage());
		}
	}

	@Test
	public void testFileNameForSanitisesThePageName() {

		assertEquals("report.pdf", PdfRenderer.fileNameFor("report"));
		assertEquals("quarterly-report_2026.pdf", PdfRenderer.fileNameFor("quarterly-report 2026"));

		// a page name reaches a response header, so quotes and line breaks must not survive
		assertEquals("evil__name.pdf", PdfRenderer.fileNameFor("evil\"\nname"));

		assertEquals("document.pdf", PdfRenderer.fileNameFor(null));
		assertEquals("document.pdf", PdfRenderer.fileNameFor("   "));
	}

	private static String textOf(final PDDocument document, final int page) throws Exception {

		final PDFTextStripper stripper = new PDFTextStripper();

		stripper.setStartPage(page);
		stripper.setEndPage(page);

		return stripper.getText(document).replaceAll("\\s+", " ").trim();
	}
}
