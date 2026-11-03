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
package org.structr.test.core.script;

import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.core.script.polyglot.config.ScriptConfig;
import org.structr.core.traits.StructrTraits;
import org.structr.schema.action.ActionContext;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.structr.test.core.script.ExecTestScripts.printed;
import static org.testng.AssertJUnit.assertArrayEquals;
import static org.testng.AssertJUnit.assertEquals;

/** exec() and execBinary() called from scripts, with files as input and output. */
public class ExecFunctionScriptingTest extends StructrUiTest {

	private final ExecTestScripts scripts = new ExecTestScripts("scripting");

	@BeforeClass(alwaysRun = true)
	public void createScripts() throws Exception {

		scripts.create();
	}

	@AfterClass(alwaysRun = true)
	public void removeScripts() throws Exception {

		scripts.remove();
	}

	@Test
	public void testJavaScriptPassesParametersInBothModes() throws Exception {

		final String expected = printed(List.of("a $(echo INJECTED)", "b"));

		assertEquals("collection mode", expected, evaluate("${{ return $.exec('" + scripts.printArgs + "', ['a $(echo INJECTED)', 'b']); }}"));
		assertEquals("argument mode", expected, evaluate("${{ return $.exec('" + scripts.printArgs + "', 'a $(echo INJECTED)', 'b'); }}"));
		assertEquals("masked parameter", expected, evaluate("${{ return $.exec('" + scripts.printArgs + "', ['a $(echo INJECTED)', { value: 'b', mask: true }], 2); }}"));
	}

	@Test
	public void testStructrScriptPassesParametersInBothModes() throws Exception {

		final String expected = printed(List.of("a $(echo INJECTED)", "b"));

		assertEquals("collection mode", expected, evaluate("${exec('" + scripts.printArgs + "', merge('a $(echo INJECTED)', 'b'))}"));
		assertEquals("argument mode", expected, evaluate("${exec('" + scripts.printArgs + "', 'a $(echo INJECTED)', 'b')}"));
	}

	@Test
	public void testJavaScriptPassesInputWithTheDefaultLogBehaviour() throws Exception {

		assertEquals("pa$$word\n", evaluate("${{ return $.exec('" + scripts.cat + "', [], null, 'pa$$word'); }}"));
	}

	@Test
	public void testAFileIsPassedAsInput() throws Exception {

		final String content = "line 1\nline 2 äöü";

		try (final Tx tx = app.tx()) {

			FileHelper.createFile(securityContext, content.getBytes(StandardCharsets.UTF_8), "text/plain", StructrTraits.FILE, "exec-input.txt", true);

			tx.success();
		}

		assertEquals(content + "\n", evaluate("${{ return $.exec('" + scripts.cat + "', [], 0, $.first($.find('File', { name: 'exec-input.txt' }))); }}"));
	}

	@Test
	public void testExecBinaryCopiesAFileIntoAFile() throws Exception {

		final byte[] bytes = new byte[256];

		for (int i = 0; i < bytes.length; i++) {

			bytes[i] = (byte) i;
		}

		try (final Tx tx = app.tx()) {

			FileHelper.createFile(securityContext, bytes, "application/octet-stream", StructrTraits.FILE, "exec-binary-input.bin", true);
			FileHelper.createFile(securityContext, new byte[0], "application/octet-stream", StructrTraits.FILE, "exec-binary-output.bin", true);

			tx.success();
		}

		evaluate("${{ $.execBinary($.first($.find('File', { name: 'exec-binary-output.bin' })), '" + scripts.cat + "', [], 0, $.first($.find('File', { name: 'exec-binary-input.bin' }))); }}");

		try (final Tx tx = app.tx()) {

			final NodeInterface output = app.nodeQuery(StructrTraits.FILE).name("exec-binary-output.bin").getFirst();

			try (final InputStream in = output.as(File.class).getInputStream()) {

				assertArrayEquals(bytes, in.readAllBytes());
			}

			tx.success();
		}
	}

	// ----- private methods -----
	private Object evaluate(final String script) throws Exception {

		try (final Tx tx = app.tx()) {

			final Object result = Scripting.evaluate(new ActionContext(securityContext), null, script, "test", null, ScriptConfig.builder().wrapJsInMain(true).build());

			tx.success();

			return result;
		}
	}
}
