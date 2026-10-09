/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.web.advanced;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.schema.action.ActionContext;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.common.Minifier;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

public class MinifyTest extends StructrUiTest {

	@Test
	public void testMinifierKinds() throws FrameworkException {

		assertEquals("var answer=42;", Minifier.minify(Minifier.Kind.JavaScript, "// comment\nvar answer = 40 + 2;\n", "app.js"));
		assertEquals("a{color:red;margin:0}", Minifier.minify(Minifier.Kind.Css, "a {\n  color: #ff0000;\n  margin: 0px 0px 0px 0px;\n}\n/* note */\n", "styles.css"));
		assertEquals("{\"a\":[1,2],\"b\":\"x\"}", Minifier.minify(Minifier.Kind.Json, "{\n  \"a\" : [ 1, 2 ],\n  \"b\": \"x\"\n}\n", "data.json"));

		assertEquals(
			"<div> <p>Hello world</p> </div> <pre>  keep\n  this </pre><script>var answer=42;</script><style>a{color:red}</style>",
			Minifier.minify(Minifier.Kind.Html, "<div>\n  <!-- comment -->\n  <p>Hello   world</p>\n</div>\n<pre>  keep\n  this </pre><script>\nvar answer = 40 + 2;\n</script><style>\na { color: #ff0000 }\n</style>", "page.html")
		);

		assertEquals(
			"<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 10 10\"><rect width=\"10\" height=\"10\" /><text x=\"0\" y=\"5\"><tspan>a</tspan> <tspan>b</tspan></text></svg>",
			Minifier.minify(Minifier.Kind.Svg, "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 10 10\">\n  <!-- comment -->\n  <rect width=\"10\" height=\"10\"/>\n  <text x=\"0\" y=\"5\"><tspan>a</tspan> <tspan>b</tspan></text>\n</svg>\n", "icon.svg")
		);
	}

	@Test
	public void testDetection() {

		assertEquals(Minifier.Kind.JavaScript, Minifier.detect("text/javascript; charset=utf-8", "app.js"));
		assertEquals(Minifier.Kind.Json,       Minifier.detect("text/plain", "data.json"));
		assertEquals(Minifier.Kind.Json,       Minifier.detect("application/manifest+json", "site.webmanifest"));
		assertEquals(Minifier.Kind.Svg,        Minifier.detect(null, "icon.svg"));
		assertNull(Minifier.detect("image/png", "image.png"));
	}

	@Test
	public void testMinifyFunctionDefaultAndRelativeOutputPath() {

		try (final Tx tx = app.tx()) {

			final Folder folder = FileHelper.createFolderPath(securityContext, "/js").as(Folder.class);

			FileHelper.createFile(securityContext, stream("var answer = 40 + 2;\n"), "application/javascript", StructrTraits.FILE, "app.js", folder);

			final File minified = run("${{ $.minify('/js/app.js') }}");

			assertEquals("/js/app.min.js", minified.getPath());
			assertEquals("application/javascript", minified.getContentType());
			assertEquals("var answer=42;", content(minified));

			final File relative = run("${{ $.minify($.first($.find('File', 'name', 'app.js')), 'dist/app.js') }}");

			assertEquals("/js/dist/app.js", relative.getPath());
			assertEquals("var answer=42;", content(relative));

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testMinifyFunctionOverwritesExistingOutput() {

		try (final Tx tx = app.tx()) {

			final File source  = FileHelper.createFile(securityContext, stream("a { color: #ff0000 }"), "text/css", StructrTraits.FILE, "styles.css", null).as(File.class);
			final File first   = run("${minify('/styles.css')}");

			assertEquals("a{color:red}", content(first));

			FileHelper.setFileData(source, "a { color: #0000ff }".getBytes(StandardCharsets.UTF_8), "text/css");

			final File second  = run("${minify('/styles.css')}");

			assertEquals(first.getUuid(), second.getUuid());
			assertEquals("a{color:#00f}", content(second));

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testMinifyFunctionErrors() {

		try (final Tx tx = app.tx()) {

			FileHelper.createFile(securityContext, stream("var x = ;"), "application/javascript", StructrTraits.FILE, "broken.js", null);
			FileHelper.createFile(securityContext, stream("not an image"), "image/png", StructrTraits.FILE, "image.png", null);
			FileHelper.createFile(securityContext, stream("var answer = 42;"), "application/javascript", StructrTraits.FILE, "ok.js", null);

			assertMinifyFails("${{ $.minify('/broken.js') }}", "Unable to minify JavaScript");
			assertMinifyFails("${{ $.minify('/image.png') }}", "unsupported content type image/png");
			assertMinifyFails("${{ $.minify('/ok.js', '/ok.js') }}", "the output path must differ from the source path");
			assertMinifyFails("${{ $.minify('/missing.js') }}", "not a file");

			assertNull(FileHelper.getFileByAbsolutePath(securityContext, "/broken.min.js"));

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	// ----- private methods -----
	private void assertMinifyFails(final String script, final String expectedMessage) {

		try {

			Scripting.evaluate(new ActionContext(securityContext), null, script, "test");

			fail("minify() should have failed for " + script);

		} catch (FrameworkException fex) {

			assertEquals(422, fex.getStatus());
			assertTrue("Unexpected message: " + fex.getMessage(), fex.getMessage().contains(expectedMessage));
		}
	}

	private File run(final String script) throws FrameworkException {

		return ((NodeInterface) Scripting.evaluate(new ActionContext(securityContext), null, script, "test")).as(File.class);
	}

	private InputStream stream(final String content) {

		return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
	}

	private String content(final File file) throws Exception {

		try (final InputStream is = file.getInputStream()) {

			return new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
