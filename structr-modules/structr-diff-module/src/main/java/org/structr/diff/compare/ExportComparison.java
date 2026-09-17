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
package org.structr.diff.compare;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.diff.export.ExportSource;
import org.structr.diff.export.ZipExportSource;
import org.structr.diff.model.Entity;
import org.structr.diff.parse.ExportParser;
import org.structr.web.entity.File;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Compares two exports held in the virtual filesystem and returns the report.
 *
 * The single producer behind every caller: the websocket command the admin UI uses and the
 * {@code compareExports()} function everything else uses. Both hand in two File uuids and get the same
 * map back, so the two surfaces cannot drift apart.
 *
 * <p>Takes uuids rather than paths, because an export is a File node once it is ingested and a uuid is
 * what a caller already has in hand. Reading one needs nothing but {@code getRawInputStream()}, so this
 * works on any storage backend.</p>
 *
 * <p>The whole comparison happens inline. It is affordable: parsing a large real export measured around
 * 180ms, and the two parses plus the match come in under half a second, which is why there is no job, no
 * progress channel and no partial result to reason about.</p>
 */
public final class ExportComparison {

	private ExportComparison() {}

	/**
	 * Parses both exports and returns {@code {summary, congruence, profiles, deltas}}.
	 *
	 * Failures are answers rather than server faults, and both callers present them as such: 422 for a
	 * missing uuid or an archive that will not open, 404 for a uuid that names no readable File.
	 */
	public static Map<String, Object> of(final SecurityContext securityContext, final String leftId, final String rightId) throws FrameworkException {

		if (StringUtils.isBlank(leftId) || StringUtils.isBlank(rightId)) {

			throw new FrameworkException(422, "compareExports() needs leftId and rightId, the uuids of two export files.");
		}

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final File left  = file(app, leftId);
			final File right = file(app, rightId);

			if (left == null || right == null) {

				throw new FrameworkException(404, "No such export file: " + (left == null ? leftId : rightId));
			}

			final Parsed parsedLeft  = parse(left);
			final Parsed parsedRight = parse(right);
			final List<Entity> a     = parsedLeft.entities();
			final List<Entity> b     = parsedRight.entities();
			final List<Delta> deltas = new Matcher(a, b).getDeltas();

			final Map<String, Object> congruence = Congruence.of(a, b, parsedLeft.structrVersion(), parsedRight.structrVersion(), taken(left), taken(right));
			final Map<String, Object> report     = DiffReport.of(left.getName(), right.getName(), a, b, deltas, congruence);

			tx.success();

			return report;

		} catch (final IOException ioex) {

			// An unreadable archive is a user-facing answer, not a server fault: the wrong file was picked,
			// or it is not an export at all.
			throw new FrameworkException(422, "Could not read an export: " + ioex.getMessage());
		}
	}

	private static File file(final App app, final String uuid) throws FrameworkException {

		final NodeInterface node = app.getNodeById(StructrTraits.FILE, uuid);

		return node != null ? node.as(File.class) : null;
	}

	/** Entities plus the build the export came from, read while the source is still open. */
	private record Parsed(List<Entity> entities, String structrVersion) {}

	private static Parsed parse(final File file) throws IOException, FrameworkException {

		try (final InputStream in = file.getRawInputStream()) {

			final ExportSource source = ZipExportSource.read(in, file.getName());

			return new Parsed(new ExportParser(source).getEntities(), structrVersion(source));
		}
	}

	private static String structrVersion(final ExportSource source) throws IOException {

		if (!source.exists("deployment.conf")) {

			return null;
		}

		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(source.open("deployment.conf"), StandardCharsets.UTF_8))) {

			return reader.lines()
				.filter(line -> line.startsWith("structr-version"))
				.map(line -> line.substring(line.indexOf('=') + 1).trim())
				.findFirst()
				.orElse(null);
		}
	}

	private static Congruence.Taken taken(final File file) {

		return new Congruence.Taken(file.getCreatedDate(), file.getLastModifiedDate());
	}
}
