/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.schema.InvalidSchemaException;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;

import java.io.*;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 *
 */
public class SnapshotCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(SnapshotCommand.class.getName());

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		final String       mode  = (String) attributes.get("mode");
		final String       name  = (String) attributes.get("name");
		final List<String> types = (List<String>) attributes.get("types");

		execute(mode, name, types);
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return false;
	}

	public void execute(final String mode, final String name) throws FrameworkException {
		execute(mode, name, null);
	}

	public void execute(final String mode, final String name, final List<String> types) throws FrameworkException {

		if (mode != null) {

			switch (mode) {

				case "export":
					createSnapshot(name, types);
					break;

				case "restore":
					restoreSnapshot(name);
					break;

				case "add":
					addSnapshot(name);
					break;

				case "delete":
					deleteSnapshot(name);
					break;

				case "purge":
					purgeLocalSchema();
					break;

				default:
					throw new FrameworkException(422, "Invalid mode supplied, valid values are export, restore, add, delete or delete.");
			}

		} else {

			throw new FrameworkException(500, "No snapshot mode supplied, aborting.");
		}

	}

	// ----- private methods -----
	private void createSnapshot(final String name, final List<String> types) throws FrameworkException {

		// we want to create a sorted, human-readble, diffable representation of the schema
		final App app = StructrApp.getInstance();

		// isolate write output
		try (final Tx tx = app.tx()) {

			final File snapshotFile = locateFile(name, true);
			try (final Writer writer = new FileWriter(snapshotFile)) {

				final JsonSchema schema = StructrSchema.createFromDatabase(app, types);

				writer.append(schema.toString());
				writer.append("\n");    // useful newline

				writer.flush();
			}

			tx.success();

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}

	private void restoreSnapshot(final String fileName) throws FrameworkException {

		final App app = StructrApp.getInstance();

		// isolate write output
		try (final Tx tx = app.tx()) {

			if (fileName != null) {

				final File snapshotFile = locateFile(fileName, false);
				try (final Reader reader = new FileReader(snapshotFile)) {

					StructrSchema.replaceDatabaseSchema(app, StructrSchema.createFromSource(reader));

				} catch (InvalidSchemaException | URISyntaxException ex) {

					throw new FrameworkException(422, ex.getMessage());
				}

			} else {

				throw new FrameworkException(422, "Please supply schema snapshot name to restore from.");
			}

			tx.success();

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}

	private void purgeLocalSchema() throws FrameworkException {

		final App app = StructrApp.getInstance();

		// isolate write output
		try (final Tx tx = app.tx()) {

			StructrSchema.replaceDatabaseSchema(app, StructrSchema.createEmptySchema());

			tx.success();

		} catch (InvalidSchemaException | URISyntaxException ex) {

			throw new FrameworkException(422, ex.getMessage());
		}
	}

	private void addSnapshot(final String fileName) throws FrameworkException {

		final App app = StructrApp.getInstance();

		// isolate write output
		try (final Tx tx = app.tx()) {

			if (fileName != null) {

				final File snapshotFile = locateFile(fileName, false);
				try (final Reader reader = new FileReader(snapshotFile)) {

					StructrSchema.extendDatabaseSchema(app, StructrSchema.createFromSource(reader));

				} catch (InvalidSchemaException | URISyntaxException ex) {

					throw new FrameworkException(422, ex.getMessage());
				}

			} else {

				throw new FrameworkException(422, "Please supply schema snapshot name to add to database.");
			}

			tx.success();

		} catch (IOException  ioex) {
			logger.warn("", ioex);
		}
	}

	private void deleteSnapshot(final String fileName) throws FrameworkException {

		if (fileName != null) {

			final File snapshotFile = locateFile(fileName, false);
			snapshotFile.delete();

		} else {

			throw new FrameworkException(422, "Please supply schema snapshot name to delete.");
		}
	}

	public static List<String> listSnapshots() {

		final File baseDir       = new File(getSnapshotsPath());
		final List<String> fileNames = new ArrayList<>();

		if (baseDir.exists()) {

			final String[] names = baseDir.list();
			if (names != null) {

				for (final String name : names) {
					if (name.endsWith(".json")) {
						fileNames.add(name);
					}
				}

			}
		}

		Collections.sort(fileNames);

		return fileNames;
	}

	public static File locateFile(final String name, final boolean addTimestamp) throws FrameworkException {

		String fileName = name;
		if (StringUtils.isBlank(fileName)) {

			// create default value
			fileName = "schema.json";
		}

		if (
				(File.separator.equals("/") && fileName.contains(File.separator)) ||
				(File.separator.equals("\\") && (fileName.contains("/") || fileName.contains("\\")))   // because on Windows you can use both types to create sub-directories
			) {
			throw new FrameworkException(422, "Only relative file names are allowed, please use the " + Settings.SnapshotsPath.getKey() + " configuration setting to supply a custom path for snapshots.");
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
		final File path = new File(getSnapshotsPath() + fileName);
		final File parent = path.getParentFile();
		if (!parent.exists()) {

			parent.mkdirs();
		}

		return path;
	}

	public static String getSnapshotsPath() {

		return Settings.getFullSettingPath(Settings.SnapshotsPath);

	}

}
