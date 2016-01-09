/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.rest.maintenance;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;
import org.structr.schema.json.InvalidSchemaException;
import org.structr.schema.json.JsonSchema;

/**
 *
 *
 */
public class SnapshotCommand extends NodeServiceCommand implements MaintenanceCommand {

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		final String mode = (String)attributes.get("mode");
		final String name = (String)attributes.get("name");

		execute(mode, name);
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	public void execute(final String mode, final String name) throws FrameworkException {

		if (mode != null) {

			switch (mode) {

				case "export":
					createSnapshot(name);
					break;

				case "restore":
					restoreSnapshot(name);
					break;

				case "delete":
					deleteSnapshot(name);
					break;

				default:
					throw new FrameworkException(422, "Invalid mode supplied, valid values are export and restore.");
			}

		} else {

			throw new FrameworkException(500, "No snapshot mode supplied, aborting.");
		}

	}

	// ----- private methods -----
	private void createSnapshot(final String name) throws FrameworkException {

		// we want to create a sorted, human-readble, diffable representation of the schema
		final App app = StructrApp.getInstance();

		// isolate write output
		try (final Tx tx = app.tx()) {

			final File snapshotFile = locateFile(name, true);
			try (final Writer writer = new FileWriter(snapshotFile)) {

				final JsonSchema schema = StructrSchema.createFromDatabase(app);

				writer.append(schema.toString());
				writer.append("\n");    // useful newline

				writer.flush();
			}

			tx.success();

		} catch (IOException | URISyntaxException ioex) {
			ioex.printStackTrace();
		}
	}

	private void restoreSnapshot(final String fileName) throws FrameworkException {

		// we want to create a sorted, human-readble, diffable representation of the schema
		final App app = StructrApp.getInstance();

		// isolate write output
		try (final Tx tx = app.tx()) {

			if (fileName != null) {

				final File snapshotFile = locateFile(fileName, false);
				try (final Reader reader = new FileReader(snapshotFile)) {

					final JsonSchema schema = StructrSchema.createFromSource(reader);
					StructrSchema.replaceDatabaseSchema(app, schema);

				} catch (InvalidSchemaException iex) {

					throw new FrameworkException(422, iex.getMessage());
				}

			} else {

				throw new FrameworkException(422, "Please supply schema name to import.");
			}

			tx.success();

		} catch (IOException | URISyntaxException ioex) {
			ioex.printStackTrace();
		}
	}

	private void deleteSnapshot(final String fileName) throws FrameworkException {

		if (fileName != null) {

			final File snapshotFile = locateFile(fileName, false);
			snapshotFile.delete();

		} else {

			throw new FrameworkException(422, "Please supply schema name to import.");
		}
	}

	public static List<String> listSnapshots() {

		final File baseDir       = new File(getBasePath());
		final List<String> fileNames = new LinkedList<>();

		if (baseDir.exists()) {

			final String[] names = baseDir.list();
			if (names != null) {

				fileNames.addAll(Arrays.asList(names));
			}
		}

		Collections.sort(fileNames);

		return fileNames;
	}

	public static File locateFile(final String name, final boolean addTimestamp) throws FrameworkException {

		String fileName = name;
		if (fileName == null) {

			// create default value
			fileName = "schema.json";
		}

		if (fileName.contains(System.getProperty("dir.separator", "/"))) {
			throw new FrameworkException(422, "Only relative file names are allowed, please use the snapshot.path configuration setting to supply a custom path for snapshots.");
		}

		if (addTimestamp) {
			final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");
			fileName = format.format(System.currentTimeMillis()) + "-" + fileName;
		}

		// append JSON extension
		if (!fileName.endsWith(".json")) {
			fileName = fileName + ".json";
		}

		// create
		final File path = new File(getBasePath() + fileName);
		final File parent = path.getParentFile();
		if (!parent.exists()) {

			parent.mkdirs();
		}

		return path;
	}

	public static String getBasePath() {

		String basePath = StructrApp.getConfigurationValue(Services.SNAPSHOT_PATH, "snapshots/");
		if (!basePath.endsWith("/")) {

			basePath = basePath + "/";
		}

		return basePath;
	}
}
