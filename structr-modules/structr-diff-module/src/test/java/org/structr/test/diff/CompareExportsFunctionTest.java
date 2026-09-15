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
package org.structr.test.diff;

import org.structr.common.error.FrameworkException;
import org.structr.core.function.Functions;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Covers the platform entry to the comparison: the function, and the registry seam a server-side caller
 * reaches it through.
 *
 * The comparison itself is covered by {@link ExportDiffTest}, which needs two real exports handed to it on
 * the command line and skips without them. So this is the only test in the module that runs on every build,
 * and what it has to prove is the seam rather than the diff: that the module registers the function under
 * the name callers use, that {@code Functions.get} finds it without any dependency on this module, that it
 * answers with the report map rather than a JSON string, and that a bad uuid comes back as an answer a
 * caller can act on rather than as a server fault.
 */
public class CompareExportsFunctionTest extends StructrUiTest {

	@Test
	public void testTheFunctionIsReachableThroughTheRegistry() {

		final Function<Object, Object> function = Functions.get("compareExports");

		assertNotNull("compareExports is not registered; a server-side caller reaches this module only through"
			+ " Functions.get(), so an unregistered function makes the capability unreachable", function);

		assertEquals("The function must name the module it needs, so an instance without it can say so",
			"diff", function.getRequiredModule());

		assertEquals("The function belongs in the Deployment category of the documentation",
			FunctionCategory.Deployment, function.getCategory());

		assertNotNull("Every function needs a short description; it is what the reference and any tool listing shows",
			function.getShortDescription());
	}

	@Test
	public void testComparingTwoExportsReturnsTheReport() {

		final String leftId;
		final String rightId;

		try (final Tx tx = app.tx()) {

			leftId  = createExport("left.zip",  localizations("aaa", "bbb"));
			rightId = createExport("right.zip", localizations("aaa", "ccc"));

			tx.success();

		} catch (final Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception while creating the exports: " + ex.getMessage());

			return;
		}

		try (final Tx tx = app.tx()) {

			final Object result = apply(leftId, rightId);

			assertTrue("The function has to answer with the report itself, so a script gets an object rather than"
				+ " a string it would have to parse", result instanceof Map);

			final Map<String, Object> report = (Map<String, Object>) result;

			// the four keys DiffReport produces; the websocket command sends this very map
			assertTrue("report is missing 'summary': " + report.keySet(),    report.containsKey("summary"));
			assertTrue("report is missing 'congruence': " + report.keySet(), report.containsKey("congruence"));
			assertTrue("report is missing 'profiles': " + report.keySet(),   report.containsKey("profiles"));
			assertTrue("report is missing 'deltas': " + report.keySet(),     report.containsKey("deltas"));

			// an export that parsed to nothing would still produce a verdict, so the fixture has to be shown to carry entities
			final Map<String, Object> summary = (Map<String, Object>) report.get("summary");

			// two localizations plus deployment.conf, which the parser reads as a ConfigFile entity of its own
			assertEquals("The left fixture has to parse to three entities, otherwise the verdict below means nothing",
				3, ((Number) summary.get("leftEntities")).intValue());

			assertEquals("The right fixture has to parse to three entities", 3, ((Number) summary.get("rightEntities")).intValue());

			assertTrue("One localization differs between the two fixtures, so the comparison has to report at least one delta",
				((Number) summary.get("deltas")).intValue() > 0);

			final Map<String, Object> congruence = (Map<String, Object>) report.get("congruence");
			final Object verdict                 = congruence.get("verdict");

			assertTrue("The verdict has to be one of the three the documentation names, because a caller branches"
				+ " on it: " + verdict, List.of("SAME_LINEAGE", "SAME_APP_DIFFERENT_LINEAGE", "DIFFERENT_APPS").contains(verdict));

			assertNotNull("The verdict comes with the sentence that explains it", congruence.get("explanation"));

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testAnUnknownFileIsAnAnswerNotAServerFault() {

		final String leftId;

		try (final Tx tx = app.tx()) {

			leftId = createExport("left.zip", localizations("aaa"));

			tx.success();

		} catch (final Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception while creating the export: " + ex.getMessage());

			return;
		}

		try (final Tx tx = app.tx()) {

			apply(leftId, "0123456789abcdef0123456789abcdef");

			fail("Comparing against a uuid that names no file has to fail");

			tx.success();

		} catch (final FrameworkException fex) {

			assertEquals("A uuid that names no readable file is a 404, so both the websocket and a script caller"
				+ " can tell it apart from a broken archive", 404, fex.getStatus());
		}
	}

	private Object apply(final String leftId, final String rightId) throws FrameworkException {

		// exactly what a server-side caller writes: no import from this module, no reflection
		return Functions.get("compareExports").apply(new ActionContext(securityContext), null, new Object[] { leftId, rightId });
	}

	/** A minimal but real export: the parser reads what it finds and ignores what it does not. */
	private String createExport(final String name, final String localizations) throws Exception {

		final Map<String, String> entries = new LinkedHashMap<>();

		entries.put("deployment.conf", "structr-version = 7.0-SNAPSHOT\n");
		entries.put("localizations.json", localizations);

		final File file = FileHelper.createFile(securityContext, zip(entries), "application/zip", StructrTraits.FILE, name, true).as(File.class);

		return file.getUuid();
	}

	private String localizations(final String... ids) {

		final StringBuilder json = new StringBuilder("[");

		for (int i = 0; i < ids.length; i++) {

			json.append(i > 0 ? "," : "")
				.append("{\"id\":\"").append(ids[i]).append("\",\"name\":\"key-").append(ids[i])
				.append("\",\"localizedName\":\"value-").append(ids[i]).append("\",\"locale\":\"en\"}");
		}

		return json.append("]").toString();
	}

	private byte[] zip(final Map<String, String> entries) throws Exception {

		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		try (final ZipOutputStream zip = new ZipOutputStream(out)) {

			for (final Map.Entry<String, String> entry : entries.entrySet()) {

				zip.putNextEntry(new ZipEntry(entry.getKey()));
				zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
				zip.closeEntry();
			}
		}

		return out.toByteArray();
	}
}
