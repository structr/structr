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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.diff.compare.Delta;
import org.structr.diff.compare.Matcher;
import org.structr.diff.compare.Signal;
import org.structr.diff.export.ExportSource;
import org.structr.diff.export.ExportSources;
import org.structr.diff.model.Entity;
import org.structr.diff.parse.ExportParser;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Runs the differ over two real exports and checks the result against an expectation supplied on
 * the command line.
 *
 * Guarded by system properties and skipped without them, because the exports worth asserting
 * against are several MB of real applications and can contain a live API key, so they cannot live
 * in the repository. That is the trade: this proves the differ against messy real input, and a
 * committed synthetic fixture will later cover the individual identity rules on every build.
 *
 * <p>The expectation is a parameter rather than a constant so that any pair can be pinned, not
 * just the one that happened to be at hand when this was written. A pair whose expected breakdown
 * is known is a regression fixture; without one there is nothing to assert, so the test skips
 * rather than passing vacuously.
 *
 * <p>Two exports of ONE instance either side of an upgrade are the most valuable shape: same
 * application, same data, same uuids, and only the deliberate changes in between. Anything beyond
 * those is an invented delta, which is what this catches. For example:
 *
 * <pre>
 * mvn verify -pl structr-modules/structr-diff-module -DskipDockerTestDB=true \
 *   -Dstructr.export.left=/path/webapp_20260825_083928 \
 *   -Dstructr.export.right=/path/webapp_20260826_223536 \
 *   -Dstructr.export.expected="REMOVED LLMTool=20, REMOVED LLMAgent=1, CHANGED SchemaSource=5, \
 *      ADDED Scratchpad=3, REMOVED Scratchpad=1, CHANGED ConfigFile=1"
 * </pre>
 *
 * Either path may be a directory or a zip.
 */
public class ExportDiffTest {

	private static final Logger logger = LoggerFactory.getLogger(ExportDiffTest.class);

	@Test
	public void pairMatchesTheExpectedBreakdown() throws Exception {

		final Path left                     = requiredPath("structr.export.left");
		final Path right                    = requiredPath("structr.export.right");
		final Map<String, Integer> expected = requiredBreakdown("structr.export.expected");
		final List<Entity> a                = parse(left);
		final List<Entity> b                = parse(right);

		// Non-vacuity first: an expectation of nothing is also what two unread exports produce.
		assertTrue("The left export parsed to nothing", a.size() > 1000);

		final List<Delta> deltas = new Matcher(a, b).getDeltas();

		report(left + " -> " + right, a, b, deltas, describe(deltas));

		final Map<String, Integer> actual = describe(deltas);

		assertEquals("Unexpected deltas: " + actual, expected, actual);
	}

	/**
	 * The same export read as a zip and as a directory must compare identically.
	 *
	 * Worth more than a pinned breakdown in one respect: it needs no knowledge of which application
	 * was exported, so it stays valid when the fixtures are replaced. It is also the only check
	 * that ZipExportSource and PathExportSource agree, and they read an archive by entirely
	 * different routes.
	 */
	@Test
	public void zipAndDirectoryOfOneExportAgree() throws Exception {

		final Path directory             = requiredPath("structr.export.dir");
		final Path zip                   = requiredPath("structr.export.zip");
		final List<Entity> fromDirectory = parse(directory);
		final List<Entity> fromZip       = parse(zip);

		assertTrue("The directory parsed to nothing", fromDirectory.size() > 1000);
		assertEquals("The two readings disagree on how many entities the export holds",
			fromDirectory.size(), fromZip.size());

		final List<Delta> deltas = new Matcher(fromDirectory, fromZip).getDeltas();

		report(directory + " -> " + zip, fromDirectory, fromZip, deltas, describe(deltas));

		assertEquals("A zip and a directory of one export must not differ, but: " + describe(deltas),
			0, deltas.size());
	}

	// ----- helpers -----

	private static List<Entity> parse(final Path path) throws Exception {

		final ExportSource source = ExportSources.open(path, path.getFileName().toString());

		return new ExportParser(source).getEntities();
	}

	/**
	 * Logs the comparison on every run, passing or failing.
	 *
	 * A green test says only that the differ agreed with an expectation someone wrote down. Seeing
	 * the breakdown is what lets a reader notice that the expectation itself became wrong, and it is
	 * printed in the same shape structr.export.expected accepts, so a new pair can be pinned by
	 * reading this and pasting it back.
	 */
	private static void report(final String pair, final List<Entity> left, final List<Entity> right,
			final List<Delta> deltas, final Map<String, Integer> counts) {

		final StringBuilder buf = new StringBuilder();
		int total               = 0;

		for (final int count : counts.values()) {

			total += count;
		}

		buf.append("\n  ").append(pair);
		buf.append("\n  entities: ").append(left.size()).append(" -> ").append(right.size());
		int low = 0;

		for (final Delta delta : deltas) {

			if (Signal.LOW.equals(delta.getSignal())) {

				low++;
			}
		}

		buf.append("\n  deltas:   ").append(total)
			.append(" (").append(total - low).append(" normal, ").append(low).append(" low signal)");

		for (final Map.Entry<String, Integer> entry : counts.entrySet()) {

			buf.append(String.format("%n    %5d  %s", entry.getValue(), entry.getKey()));
		}

		logger.info("{}", buf);
	}

	/** Deltas counted by operation and kind, which is what a failure needs to be readable. */
	private static Map<String, Integer> describe(final List<Delta> deltas) {

		final Map<String, Integer> counts = new TreeMap<>();

		for (final Delta delta : deltas) {

			counts.merge(delta.getOperation() + " " + delta.getKind(), 1, Integer::sum);
		}

		return counts;
	}

	/**
	 * The expected breakdown, as {@code "OPERATION Kind=count, OPERATION Kind=count"}.
	 *
	 * Deliberately the same shape the failure message prints, so a first run against a new pair
	 * can be turned into an expectation by copying what it reported, once it has been read and
	 * agreed with.
	 */
	private static Map<String, Integer> requiredBreakdown(final String property) {

		final String value                = required(property);
		final Map<String, Integer> result = new TreeMap<>();

		for (final String entry : value.split(",")) {

			final String trimmed = entry.trim();

			if (trimmed.isEmpty()) {

				continue;
			}

			final int equals = trimmed.lastIndexOf('=');

			if (equals < 0) {

				throw new IllegalArgumentException("Expected \"OPERATION Kind=count\", got: " + trimmed);
			}

			result.put(trimmed.substring(0, equals).trim(),
				Integer.valueOf(trimmed.substring(equals + 1).trim()));
		}

		return result;
	}

	private static Path requiredPath(final String property) {

		return Path.of(required(property));
	}

	private static String required(final String property) {

		final String value = System.getProperty(property);

		if (value == null || value.isBlank()) {

			throw new SkipException("Set -D" + property + " to run this test.");
		}

		return value;
	}
}
