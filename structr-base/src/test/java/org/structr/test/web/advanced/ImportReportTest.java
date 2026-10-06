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

import org.apache.commons.io.FileUtils;
import org.structr.core.graph.Tx;
import org.structr.web.entity.dom.Page;
import org.structr.web.maintenance.DeployCommand;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * What an import tells the caller that asked for it.
 *
 * <p>An import used to answer 200 with an empty body: everything it learned went to the server's own log
 * and to the websocket progress channel, and a REST caller can read neither. A deployment could report
 * success while the access rules it was meant to establish were absent, which is indistinguishable from
 * the inside of the application.</p>
 *
 * <p>The pre-deploy script matters most here, because it is the platform's own documented remedy for
 * missing principals: its transaction rolls back as a whole, so a script that fails on its last line
 * creates nothing, and until now it said so only in the log.</p>
 */
public class ImportReportTest extends DeploymentTestBase {

	@Test
	public void testTheImportAnswersWithWhatItDidAndWhatItDropped() {

		final Path tmp = Paths.get("/tmp/structr-import-report-test" + System.currentTimeMillis() + System.nanoTime());

		try {

			try (final Tx tx = app.tx()) {

				Page.createSimplePage(securityContext, "page1");

				tx.success();
			}

			export(tmp);

			// a script that throws on its last line: everything it did is rolled back with the transaction
			Files.writeString(tmp.resolve("pre-deploy.conf"),
				"{\n    $.log('pre-deploy ran');\n    throw new Error('deliberate failure');\n}", StandardCharsets.UTF_8);

			final DeployCommand command = app.command(DeployCommand.class);
			final Map<String, Object> params = new HashMap<>();

			params.put("mode", "import");
			params.put("source", tmp.toString());

			command.execute(params);

			final Object result = command.getCommandResult();

			assertTrue("The import has to answer with a report, not with the empty list that means 'nothing to say'",
				result instanceof Map);

			final Map<String, Object> report = (Map<String, Object>) result;

			assertEquals("a completed import reports ok", Boolean.TRUE, report.get("ok"));
			assertEquals("an app import says so", "app", report.get("mode"));

			assertNotNull("the report names the principals it could not resolve", report.get("missingPrincipals"));
			assertNotNull("the report names the ambiguous principals", report.get("ambiguousPrincipals"));
			assertNotNull("the report names the schema files it could not find", report.get("missingSchemaFiles"));

			// the point of the exercise: the documented remedy failed, and the caller is told
			final List<Map<String, Object>> scripts = (List<Map<String, Object>>) report.get("configScripts");

			assertNotNull("the report accounts for the deployment config scripts", scripts);

			// the export does not write a template post-deploy.conf - only our provided pre-deploy.conf is available
			assertEquals("both config scripts should be accounted for: " + scripts, 1, scripts.size());

			final Map<String, Object> script = scripts.stream()
				.filter(entry -> "pre-deploy.conf".equals(entry.get("file")))
				.findFirst()
				.orElse(null);

			assertNotNull("the failing script is accounted for by name: " + scripts, script);
			assertEquals("a script whose transaction rolled back did not apply", Boolean.FALSE, script.get("applied"));
			assertNotNull("the reason the script failed travels with it", script.get("error"));

			assertTrue("the error should name the failure rather than merely say that one happened: " + script.get("error"),
				script.get("error").toString().contains("deliberate failure"));

			// the file rolls back as a whole, so the line is what turns "it failed" into "fix this line"
			final Map<String, Object> at = (Map<String, Object>) script.get("at");

			assertNotNull("the report should say where the script failed: " + script, at);

			assertEquals("the throw is on the third line of the conf file", 3, ((Number) at.get("line")).intValue());
			assertNotNull("a column travels with the line", at.get("column"));

		} catch (final Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception: " + t.getMessage());

		} finally {

			try { FileUtils.deleteDirectory(tmp.toFile()); } catch (final Throwable ignore) {}
		}
	}

	@Test
	public void testAnImportWithoutProblemsSaysSoRatherThanSayingNothing() {

		final Path tmp = Paths.get("/tmp/structr-import-report-clean" + System.currentTimeMillis() + System.nanoTime());

		try {

			try (final Tx tx = app.tx()) {

				Page.createSimplePage(securityContext, "page1");

				tx.success();
			}

			export(tmp);

			final DeployCommand command       = app.command(DeployCommand.class);
			final Map<String, Object> params  = new HashMap<>();

			params.put("mode", "import");
			params.put("source", tmp.toString());

			command.execute(params);

			final Map<String, Object> report = (Map<String, Object>) command.getCommandResult();

			assertEquals("a clean import still answers", Boolean.TRUE, report.get("ok"));

			assertTrue("nothing was dropped, so the map is empty rather than absent",
				((Map<String, Object>) report.get("missingPrincipals")).isEmpty());

			// the export writes a post-deploy.conf, and a script that ran without throwing says applied
			for (final Map<String, Object> script : (List<Map<String, Object>>) report.get("configScripts")) {

				assertEquals("a script that did not throw should be reported as applied: " + script,
					Boolean.TRUE, script.get("applied"));

				assertEquals("a script that did not throw carries no error: " + script, null, script.get("error"));
			}

			assertFalse("the report carries how long the import took", report.get("duration") == null);

		} catch (final Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception: " + t.getMessage());

		} finally {

			try { FileUtils.deleteDirectory(tmp.toFile()); } catch (final Throwable ignore) {}
		}
	}

	private void export(final Path target) throws Exception {

		final Map<String, Object> params = new HashMap<>();

		params.put("mode", "export");
		params.put("target", target.toString());

		app.command(DeployCommand.class).execute(params);
	}
}
