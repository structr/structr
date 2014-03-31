package org.structr.web.common;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.web.Importer;
import org.structr.web.diff.InvertibleModificationOperation;
import org.structr.web.entity.dom.Page;

/**
 *
 * @author Christian Morgner
 */
public class DiffTest extends StructrUiTest {

//	public void testReplaceContent() {
//
//		final String result1 = testDiff("<html><head><title>Title</title></head><body>Test</body></html>", new org.neo4j.helpers.Function<String, String>() {
//
//			@Override
//			public String apply(String from) {
//				return from.replace("Test", "Wurst");
//			}
//		});
//
//		assertEquals(
//			"<!DOCTYPE html>\n" +
//			"<html>\n" +
//			"  <head>\n" +
//			"    <title>Title</title>\n" +
//			"  </head>\n" +
//			"  <body>Wurst</body>\n" +
//			"</html>",
//			result1
//		);
//	}
//
//	public void testInsertHeading() {
//
//		final String result1 = testDiff("<html><head><title>Title</title></head><body>Test</body></html>", new org.neo4j.helpers.Function<String, String>() {
//
//			@Override
//			public String apply(String from) {
//				return from.replace("Test", "<h1>Title text</h1>");
//			}
//		});
//
//		assertEquals(
//			"<!DOCTYPE html>\n" +
//			"<html>\n" +
//			"  <head>\n" +
//			"    <title>Title</title>\n" +
//			"  </head>\n" +
//			"  <body>\n" +
//			"    <h1>Title text</h1>\n" +
//			"  </body>\n" +
//			"</html>",
//			result1
//		);
//	}
//
//	public void testInsertDivBranch() {
//
//		final String result1 = testDiff("<html><head><title>Title</title></head><body>Test</body></html>", new org.neo4j.helpers.Function<String, String>() {
//
//			@Override
//			public String apply(String from) {
//				return from.replace("Test", "<div><h1>Title text</h1></div>");
//			}
//		});
//
//		assertEquals(
//			"<!DOCTYPE html>\n" +
//			"<html>\n" +
//			"  <head>\n" +
//			"    <title>Title</title>\n" +
//			"  </head>\n" +
//			"  <body>\n" +
//			"    <div>\n" +
//			"      <h1>Title text</h1>\n" +
//			"    </div>\n" +
//			"  </body>\n" +
//			"</html>",
//			result1
//		);
//	}
//
//	public void testInsertDivBranch2() {
//
//		final String result1 = testDiff("<html><head><title>Title</title></head><body>Test</body></html>", new org.neo4j.helpers.Function<String, String>() {
//
//			@Override
//			public String apply(String from) {
//				return from.replace("Test", "<div><div><h1>Title text</h1><p>paragraph</p></div></div>");
//			}
//		});
//
//		assertEquals(
//			"<!DOCTYPE html>\n" +
//			"<html>\n" +
//			"  <head>\n" +
//			"    <title>Title</title>\n" +
//			"  </head>\n" +
//			"  <body>\n" +
//			"    <div>\n" +
//			"      <div>\n" +
//			"        <h1>Title text</h1>\n" +
//			"        <p>paragraph</p>\n" +
//			"      </div>\n" +
//			"    </div>\n" +
//			"  </body>\n" +
//			"</html>",
//			result1
//		);
//	}
//
//	public void testInsertMultipleTextNodes() {
//
//		final String result1 = testDiff("<html><head><title>Title</title></head><body>Test</body></html>", new org.neo4j.helpers.Function<String, String>() {
//
//			@Override
//			public String apply(String from) {
//				return from.replace("Test", "Test<b>bold</b>between<i>italic</i>Text");
//			}
//		});
//
//		System.out.println(result1);
//
//		assertEquals(
//			"<!DOCTYPE html>\n" +
//			"<html>\n" +
//			"  <head>\n" +
//			"    <title>Title</title>\n" +
//			"  </head>\n" +
//			"  <body>Test<b>bold</b>between<i>italic</i>Text</body>\n" +
//			"</html>",
//			result1
//		);
//	}
//
//	public void testModifyMultipleTextNodes2() {
//
//		final String result1 = testDiff("<html><head><title>Title</title></head><body>Test<b>bold</b>between<i>italic</i>Text</body></html>", new org.neo4j.helpers.Function<String, String>() {
//
//			@Override
//			public String apply(String from) {
//
//				String mod = from;
//
//				mod = mod.replace("bold", "BOLD");
//				mod = mod.replace("between", "BETWEEN");
//				mod = mod.replace("italic", "ITALIC");
//				mod = mod.replace("Text", "abcdef");
//
//				return mod;
//			}
//		});
//
//		System.out.println(result1);
//
//		assertEquals(
//			"<!DOCTYPE html>\n" +
//			"<html>\n" +
//			"  <head>\n" +
//			"    <title>Title</title>\n" +
//			"  </head>\n" +
//			"  <body>Test<b>BOLD</b>BETWEEN<i>ITALIC</i>abcdef</body>\n" +
//			"</html>",
//			result1
//		);
//	}
//
//	public void testReparentOneLevel() {
//
//		final String result1 = testDiff("<html><head><title>Title</title></head><body><h1>Title text</h1></body></html>", new org.neo4j.helpers.Function<String, String>() {
//
//			@Override
//			public String apply(String from) {
//
//				final StringBuilder buf = new StringBuilder(from);
//
//				int startPos = buf.indexOf("<h1");
//				int endPos   = buf.indexOf("</h1>") + 5;
//
//				// insert from back to front, otherwise insert position changes
//				buf.insert(endPos, "</div>");
//				buf.insert(startPos, "<div>");
//
//				return buf.toString();
//			}
//		});
//
//		System.out.println(result1);
//
//		assertEquals(
//			"<!DOCTYPE html>\n" +
//			"<html>\n" +
//			"  <head>\n" +
//			"    <title>Title</title>\n" +
//			"  </head>\n" +
//			"  <body>\n" +
//			"    <div>\n" +
//			"      <h1>Title text</h1>\n" +
//			"    </div>\n" +
//			"  </body>\n" +
//			"</html>",
//			result1
//		);
//	}
//
//	public void testReparentTwoLevels() {
//
//		final String result1 = testDiff("<html><head><title>Title</title></head><body><h1>Title text</h1></body></html>", new org.neo4j.helpers.Function<String, String>() {
//
//			@Override
//			public String apply(String from) {
//
//				final StringBuilder buf = new StringBuilder(from);
//
//				int startPos = buf.indexOf("<h1");
//				int endPos   = buf.indexOf("</h1>") + 5;
//
//				// insert from back to front, otherwise insert position changes
//				buf.insert(endPos, "</div></div>");
//				buf.insert(startPos, "<div><div>");
//
//				return buf.toString();
//			}
//		});
//
//		System.out.println(result1);
//
//		assertEquals(
//			"<!DOCTYPE html>\n" +
//			"<html>\n" +
//			"  <head>\n" +
//			"    <title>Title</title>\n" +
//			"  </head>\n" +
//			"  <body>\n" +
//			"    <div>\n" +
//			"      <div>\n" +
//			"        <h1>Title text</h1>\n" +
//			"      </div>\n" +
//			"    </div>\n" +
//			"  </body>\n" +
//			"</html>",
//			result1
//		);
//	}
//
//	public void testReparentThreeLevels() {
//
//		final String result1 = testDiff("<html><head><title>Title</title></head><body><h1>Title text</h1></body></html>", new org.neo4j.helpers.Function<String, String>() {
//
//			@Override
//			public String apply(String from) {
//
//				final StringBuilder buf = new StringBuilder(from);
//
//				int startPos = buf.indexOf("<h1");
//				int endPos   = buf.indexOf("</h1>") + 5;
//
//				// insert from back to front, otherwise insert position changes
//				buf.insert(endPos, "</div></div></div>");
//				buf.insert(startPos, "<div><div><div>");
//
//				return buf.toString();
//			}
//		});
//
//		System.out.println(result1);
//
//		assertEquals(
//			"<!DOCTYPE html>\n" +
//			"<html>\n" +
//			"  <head>\n" +
//			"    <title>Title</title>\n" +
//			"  </head>\n" +
//			"  <body>\n" +
//			"    <div>\n" +
//			"      <div>\n" +
//			"        <div>\n" +
//			"          <h1>Title text</h1>\n" +
//			"        </div>\n" +
//			"      </div>\n" +
//			"    </div>\n" +
//			"  </body>\n" +
//			"</html>",
//			result1
//		);
//	}
//
//	public void testMove() {
//
//		final String result1 = testDiff("<html><head><title>Title</title></head><body><h1>Title text</h1><div><h2>subtitle</h2></div></body></html>", new org.neo4j.helpers.Function<String, String>() {
//
//			@Override
//			public String apply(String from) {
//
//				final StringBuilder buf = new StringBuilder(from);
//
//				int startPos = buf.indexOf("<h1");
//				int endPos   = buf.indexOf("</h1>") + 5;
//
//				// cut out <h1> block
//				final String toMove = buf.substring(startPos, endPos);
//				buf.replace(startPos, endPos, "");
//
//				// insert after <h2>
//				int insertPos = buf.indexOf("</h2>") + 5;
//
//				// insert from back to front, otherwise insert position changes
//				buf.insert(insertPos, toMove);
//
//				return buf.toString();
//			}
//		});
//
//		System.out.println(result1);
//
//		assertEquals(
//			"<!DOCTYPE html>\n" +
//			"<html>\n" +
//			"  <head>\n" +
//			"    <title>Title</title>\n" +
//			"  </head>\n" +
//			"  <body>\n" +
//			"    <div>\n" +
//			"      <h2>subtitle</h2>\n" +
//			"      <h1>Title text</h1>\n" +
//			"    </div>\n" +
//			"  </body>\n" +
//			"</html>",
//			result1
//		);
//	}
//
//	public void testCutAndPaste() {
//
//		final StringBuilder clipboard = new StringBuilder();
//
//		final String result1 = testDiff("<html><head><title>Title</title></head><body><div><h2>one</h2></div><div><h2>two</h2></div><div><h2>three</h2></div><div><h2>four</h2></div></body></html>", new org.neo4j.helpers.Function<String, String>() {
//
//			@Override
//			public String apply(String from) {
//
//				final StringBuilder buf = new StringBuilder(from);
//
//				// cut the first two div/h2 blocks and store them for later
//				for (int i=0; i<2; i++) {
//
//					int startPos = buf.indexOf("<div");
//					int endPos   = buf.indexOf("</div>") + 6;
//
//					// cut out <h1> block
//					clipboard.append(buf.substring(startPos, endPos));
//					buf.replace(startPos, endPos, "");
//				}
//
//				return buf.toString();
//			}
//		});
//
//		assertEquals(
//			"<!DOCTYPE html>\n" +
//			"<html>\n" +
//			"  <head>\n" +
//			"    <title>Title</title>\n" +
//			"  </head>\n" +
//			"  <body>\n" +
//			"    <div>\n" +
//			"      <h2>three</h2>\n" +
//			"    </div>\n" +
//			"    <div>\n" +
//			"      <h2>four</h2>\n" +
//			"    </div>\n" +
//			"  </body>\n" +
//			"</html>",
//			result1
//		);
//
//		// remove data-hash=... from clipboard buffer
//		int pos = -1;
//		do {
//			pos = clipboard.indexOf(" data-hash");
//			if (pos != -1) {
//
//				clipboard.replace(pos, pos+21, "");
//			}
//
//
//		} while (pos != -1);
//
//		final String result2 = testDiff(result1, new org.neo4j.helpers.Function<String, String>() {
//
//			@Override
//			public String apply(String from) {
//
//				final StringBuilder buf = new StringBuilder(from);
//
//				final int insertPos = buf.indexOf("<div");
//				buf.insert(insertPos, clipboard.toString());
//
//				return buf.toString();
//			}
//		});
//
//		assertEquals(
//			"<!DOCTYPE html>\n" +
//			"<html>\n" +
//			"  <head>\n" +
//			"    <title>Title</title>\n" +
//			"  </head>\n" +
//			"  <body>\n" +
//			"    <div>\n" +
//			"      <h2>one</h2>\n" +
//			"    </div>\n" +
//			"    <div>\n" +
//			"      <h2>two</h2>\n" +
//			"    </div>\n" +
//			"    <div>\n" +
//			"      <h2>three</h2>\n" +
//			"    </div>\n" +
//			"    <div>\n" +
//			"      <h2>four</h2>\n" +
//			"    </div>\n" +
//			"  </body>\n" +
//			"</html>",
//			result2
//		);
//	}

	public void testSwap() {

		final StringBuilder clipboard = new StringBuilder();

		final String result1 = testDiff("<html><head><title>Title</title></head><body><div><h2>one</h2></div><div><h2>two</h2></div><div><h2>three</h2></div><div><h2>four</h2></div></body></html>", new org.neo4j.helpers.Function<String, String>() {

			@Override
			public String apply(String from) {

				final StringBuilder buf = new StringBuilder(from);

				int cutStart = buf.indexOf("<h2") - 33;
				int cutEnd = buf.indexOf("</h2>") + 21;

				// cut out <h1> block
				clipboard.append(buf.substring(cutStart, cutEnd));
				buf.replace(cutStart, cutEnd, "");

				int insert = buf.indexOf("</h2>") + 20;

				buf.insert(insert, clipboard.toString());

				System.out.println(buf.toString());

				return buf.toString();
			}
		});

		assertEquals(
			"<!DOCTYPE html>\n"
			+ "<html>\n"
			+ "  <head>\n"
			+ "    <title>Title</title>\n"
			+ "  </head>\n"
			+ "  <body>\n"
			+ "    <div>\n"
			+ "      <h2>two</h2>\n"
			+ "    </div>\n"
			+ "    <div>\n"
			+ "      <h2>one</h2>\n"
			+ "    </div>\n"
			+ "    <div>\n"
			+ "      <h2>three</h2>\n"
			+ "    </div>\n"
			+ "    <div>\n"
			+ "      <h2>four</h2>\n"
			+ "    </div>\n"
			+ "  </body>\n"
			+ "</html>",
			result1
		);
	}












	private String testDiff(final String source, final org.neo4j.helpers.Function<String, String> modifier) {

		StringBuilder buf = new StringBuilder();

		try (final Tx tx = app.tx()) {

			final Page sourcePage     = Importer.parsePageFromSource(securityContext, source, "test");
			final String sourceHtml   = renderPage(sourcePage, RenderContext.EditMode.RAW);
			final String modifiedHtml = modifier.apply(sourceHtml);

			// parse page from modified source
			final Page modifiedPage = Importer.parsePageFromSource(securityContext, modifiedHtml, "Test");

			final List<InvertibleModificationOperation> changeSet = Importer.diffPages(sourcePage, modifiedPage);

			for (final InvertibleModificationOperation op : changeSet) {

				System.out.println(op);

				// execute operation
				op.apply(app, sourcePage, modifiedPage);

				System.out.println("############################################################################################");
				System.out.println(renderPage(sourcePage, RenderContext.EditMode.NONE));
			}

			buf.append(renderPage(sourcePage, RenderContext.EditMode.NONE));

		} catch (Throwable t) {

			t.printStackTrace();
		}

		return buf.toString();
	}

	private String renderPage(final Page page, final RenderContext.EditMode editMode) throws FrameworkException {

		final RenderContext ctx = new RenderContext(null, null, editMode, Locale.GERMAN);
		final TestBuffer buffer = new TestBuffer();
		ctx.setBuffer(buffer);
		page.render(securityContext, ctx, 0);

		// extract source
		return buffer.getBuffer().toString();
	}

	private static class TestBuffer extends AsyncBuffer {

		private StringBuilder buf = new StringBuilder();

		@Override
		public AsyncBuffer append(final String s) {
			buf.append(s);
			return this;
		}

		public StringBuilder getBuffer() {
			return buf;
		}

		@Override
		public void flush() {
		}

		@Override
		public void onWritePossible() throws IOException {
		}
	}
}
