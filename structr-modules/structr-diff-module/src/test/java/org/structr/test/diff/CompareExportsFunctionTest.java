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
import static org.testng.AssertJUnit.assertFalse;
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

	@Test
	public void testAFreshInstanceIsNotTheSameLineageAsAnApplication() {

		final String freshId;
		final String appId;

		try (final Tx tx = app.tx()) {

			// what a brand new instance exports: the platform's own boilerplate, and one page of its own
			freshId = createExport("fresh.zip", Map.of(
				"widgets.json", widgets(PLATFORM_WIDGETS),
				"pages.json",   pages(Map.of("index", "f0000000000000000000000000000001"))));

			// an application, exported from an instance carrying the same bundled widgets
			appId = createExport("app.zip", Map.of(
				"widgets.json", widgets(PLATFORM_WIDGETS),
				"pages.json",   pages(Map.of(
					"orders",    "a0000000000000000000000000000001",
					"customers", "a0000000000000000000000000000002",
					"invoices",  "a0000000000000000000000000000003"))));

			tx.success();

		} catch (final Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception while creating the exports: " + ex.getMessage());

			return;
		}

		try (final Tx tx = app.tx()) {

			final Map<String, Object> report     = (Map<String, Object>) apply(freshId, appId);
			final Map<String, Object> congruence = (Map<String, Object>) report.get("congruence");

			final double raw         = ((Number) congruence.get("identityOverlapAllEntities")).doubleValue();
			final double application = ((Number) congruence.get("identityOverlap")).doubleValue();

			// the symptom: counted over everything, the shared boilerplate alone carries identity past the
			// SAME_LINEAGE threshold, which is what made a fresh instance read as a version of the app
			assertTrue("The fixture has to reproduce the boilerplate overlap, otherwise it proves nothing about"
				+ " excluding it. Raw identity was " + raw, raw >= 0.5);

			// the fix: the two share no application entity at all
			assertEquals("A fresh instance shares no application entity with an app it has never seen",
				0.0, application, 0.0001);

			assertTrue("A fresh instance must not be reported as a version of an application it has never held,"
				+ " because a caller refuses or permits a deployment on this verdict: " + congruence.get("verdict"),
				!"SAME_LINEAGE".equals(congruence.get("verdict")));

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testAPageIsNamedByItsNameNotByItsExportFileName() {

		final String leftId;
		final String rightId;

		try (final Tx tx = app.tx()) {

			// one page called "orders"
			final Map<String, String[]> left = new LinkedHashMap<>();

			left.put("orders", new String[] { "orders", "b0000000000000000000000000000001" });

			// the same page, plus a second one that also calls itself "orders": the export has to put the
			// second one in a file of its own, so its manifest key carries the uuid while its name does not
			final Map<String, String[]> right = new LinkedHashMap<>();

			right.put("orders", new String[] { "orders", "b0000000000000000000000000000001" });
			right.put("orders-b0000000000000000000000000000002", new String[] { "orders", "b0000000000000000000000000000002" });

			leftId  = createExport("left.zip",  Map.of("pages.json", pagesManifest(left)));
			rightId = createExport("right.zip", Map.of("pages.json", pagesManifest(right)));

			tx.success();

		} catch (final Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception while creating the exports: " + ex.getMessage());

			return;
		}

		try (final Tx tx = app.tx()) {

			final Map<String, Object> report     = (Map<String, Object>) apply(leftId, rightId);
			final Map<String, Object> congruence = (Map<String, Object>) report.get("congruence");
			final Map<String, Object> signals    = (Map<String, Object>) congruence.get("signals");

			// both sides call every page they have "orders", so the page names agree completely. Reading the
			// manifest key instead would see "orders-<uuid>" as a page nobody else has and score this 0.5
			assertEquals("A page is named by its name, not by the file the export happened to write it to",
				1.0, ((Number) signals.get("pageNames")).doubleValue(), 0.0001);

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testADataExportIsReadRatherThanSilentlyAgreedWith() {

		final String leftId;
		final String rightId;

		try (final Tx tx = app.tx()) {

			// one record changed, one only on the left, and a link between two records
			leftId = createExport("left.zip", Map.of(
				"nodes/Project.json", records(
					"{\"id\":\"c0000000000000000000000000000001\",\"name\":\"Apollo\",\"status\":\"open\"}",
					"{\"id\":\"c0000000000000000000000000000002\",\"name\":\"Gemini\",\"status\":\"open\"}"),
				"relationships/OWNS.json", records(
					"{\"id\":\"e0000000000000000000000000000001\",\"sourceId\":\"c0000000000000000000000000000001\",\"targetId\":\"c0000000000000000000000000000002\",\"relType\":\"OWNS\"}")));

			rightId = createExport("right.zip", Map.of(
				"nodes/Project.json", records(
					"{\"id\":\"c0000000000000000000000000000001\",\"name\":\"Apollo\",\"status\":\"closed\"}"),
				// the same link, with a uuid of its own that an import would have minted fresh
				"relationships/OWNS.json", records(
					"{\"id\":\"e9999999999999999999999999999999\",\"sourceId\":\"c0000000000000000000000000000001\",\"targetId\":\"c0000000000000000000000000000002\",\"relType\":\"OWNS\"}")));

			tx.success();

		} catch (final Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception while creating the exports: " + ex.getMessage());

			return;
		}

		try (final Tx tx = app.tx()) {

			final Map<String, Object> report      = (Map<String, Object>) apply(leftId, rightId);
			final List<Map<String, Object>> deltas = (List<Map<String, Object>>) report.get("deltas");

			final Map<String, Object> changed = deltaFor(deltas, "c0000000000000000000000000000001");
			final Map<String, Object> removed = deltaFor(deltas, "c0000000000000000000000000000002");

			assertNotNull("a record whose attributes differ has to be reported: " + deltas, changed);
			assertEquals("Record", changed.get("kind"));
			assertEquals("CHANGED", changed.get("operation"));

			assertNotNull("a record only the left side has, has to be reported: " + deltas, removed);
			assertEquals("REMOVED", removed.get("operation"));

			// the link is the same link on both sides, so its own uuid changing must not make it a difference
			for (final Map<String, Object> delta : deltas) {

				assertFalse("the link was matched by its uuid rather than by what it connects, so a deployment"
					+ " that recreates every edge would report every link as a change: " + delta,
					"RecordLink".equals(delta.get("kind")));
			}

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testAChangedRecordCarriesTheOldAndTheNewValue() {

		final String leftId;
		final String rightId;
		final String longValue = "x".repeat(900);

		try (final Tx tx = app.tx()) {

			leftId = createExport("left.zip", Map.of("nodes/Project.json", records(
				"{\"id\":\"c0000000000000000000000000000001\",\"name\":\"Apollo\",\"status\":\"open\",\"notes\":\"short\"}")));

			rightId = createExport("right.zip", Map.of("nodes/Project.json", records(
				"{\"id\":\"c0000000000000000000000000000001\",\"name\":\"Apollo\",\"status\":\"closed\",\"notes\":\"" + longValue + "\"}")));

			tx.success();

		} catch (final Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception while creating the exports: " + ex.getMessage());

			return;
		}

		try (final Tx tx = app.tx()) {

			final Map<String, Object> report       = (Map<String, Object>) apply(leftId, rightId);
			final List<Map<String, Object>> deltas = (List<Map<String, Object>>) report.get("deltas");
			final Map<String, Object> delta        = deltaFor(deltas, "c0000000000000000000000000000001");

			assertNotNull("the changed record has to be reported: " + deltas, delta);

			final List<Map<String, Object>> changes = (List<Map<String, Object>>) delta.get("changes");

			assertNotNull("a delta that names an attribute has to say what it became: " + delta, changes);

			final Map<String, Object> status = changeFor(changes, "status");

			assertNotNull("the changed attribute is accounted for: " + changes, status);
			assertTrue("the old value travels with the delta: " + status, status.get("from").toString().contains("open"));
			assertTrue("the new value travels with the delta: " + status, status.get("to").toString().contains("closed"));
			assertEquals("a short value is not marked as cut", null, status.get("truncated"));

			final Map<String, Object> notes = changeFor(changes, "notes");

			assertNotNull("the long attribute is accounted for too: " + changes, notes);
			assertEquals("a cut value says that it was cut", Boolean.TRUE, notes.get("truncated"));
			assertTrue("the real length survives the cut", ((Number) notes.get("toLength")).intValue() > 900);
			assertTrue("the value itself is bounded", notes.get("to").toString().length() <= 400);

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	private Map<String, Object> deltaFor(final List<Map<String, Object>> deltas, final String key) {

		return deltas.stream().filter(d -> key.equals(d.get("key"))).findFirst().orElse(null);
	}

	private Map<String, Object> changeFor(final List<Map<String, Object>> changes, final String attribute) {

		return changes.stream().filter(c -> attribute.equals(c.get("attribute"))).findFirst().orElse(null);
	}

	private String records(final String... entries) {

		return "[" + String.join(",", entries) + "]";
	}

	private Object apply(final String leftId, final String rightId) throws FrameworkException {

		// exactly what a server-side caller writes: no import from this module, no reflection
		return Functions.get("compareExports").apply(new ActionContext(securityContext), null, new Object[] { leftId, rightId });
	}

	/** Widget uuids that ship with the platform, so every instance exports them under the same keys. */
	private static final List<String> PLATFORM_WIDGETS = List.of(
		"126e7efc2ddc4e449d7a554ee9ed2ecb", "226e7efc2ddc4e449d7a554ee9ed2ecb", "326e7efc2ddc4e449d7a554ee9ed2ecb");

	private String widgets(final List<String> ids) {

		final StringBuilder json = new StringBuilder("[");

		for (int i = 0; i < ids.size(); i++) {

			json.append(i > 0 ? "," : "")
				.append("{\"id\":\"").append(ids.get(i)).append("\",\"name\":\"widget-").append(i).append("\"}");
		}

		return json.append("]").toString();
	}

	/** pages.json is a manifest keyed by the export FILE name, with the page's real name and uuid inside. */
	private String pages(final Map<String, String> idsByName) {

		final Map<String, String[]> entries = new LinkedHashMap<>();

		for (final Map.Entry<String, String> entry : idsByName.entrySet()) {

			entries.put(entry.getKey(), new String[] { entry.getKey(), entry.getValue() });
		}

		return pagesManifest(entries);
	}

	/** The same manifest, but with the export file name stated separately from the page name. */
	private String pagesManifest(final Map<String, String[]> nameAndIdByFileName) {

		final StringBuilder json = new StringBuilder("{");
		boolean first            = true;

		for (final Map.Entry<String, String[]> entry : nameAndIdByFileName.entrySet()) {

			json.append(first ? "" : ",")
				.append("\"").append(entry.getKey()).append("\":{\"id\":\"").append(entry.getValue()[1])
				.append("\",\"name\":\"").append(entry.getValue()[0]).append("\",\"visibleToPublicUsers\":true}");

			first = false;
		}

		return json.append("}").toString();
	}

	/** A minimal but real export: the parser reads what it finds and ignores what it does not. */
	private String createExport(final String name, final String localizations) throws Exception {

		return createExport(name, Map.of("localizations.json", localizations));
	}

	private String createExport(final String name, final Map<String, String> files) throws Exception {

		final Map<String, String> entries = new LinkedHashMap<>();

		entries.put("deployment.conf", "structr-version = 7.0-SNAPSHOT\n");
		entries.putAll(files);

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
