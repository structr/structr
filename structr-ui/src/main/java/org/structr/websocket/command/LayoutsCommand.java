/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.websocket.command;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

public class LayoutsCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(LayoutsCommand.class.getName());

	static {

		StructrWebSocket.addCommand(LayoutsCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final String mode                     = webSocketData.getNodeDataStringValue("mode");
		final String name                     = webSocketData.getNodeDataStringValue("name");

		if (mode != null) {

			switch (mode) {

				case "list":

					final List<String> layouts = LayoutsCommand.listLayouts();

					if (layouts != null) {

						getWebSocket().send(MessageBuilder.forName(getCommand()).callback(callback).data("layouts", layouts).build(), true);
					}

					break;

				case "get":

					try {

						final String content = new String(Files.readAllBytes(locateFile(name).toPath()));

						getWebSocket().send(MessageBuilder.forName(getCommand()).callback(callback).data("schemaLayout", content).build(), true);

					} catch (IOException | FrameworkException ex) {

						logger.error("", ex);
					}

					break;

				case "add":

					try {

						final File layoutFile = locateFile(name);

						if (layoutFile.exists()) {

							getWebSocket().send(MessageBuilder.forName(getCommand()).callback(callback).data("error", "Layout already exists").data("message", "To explicitly overwrite the layout, please use the overwrite function.").build(), true);

						} else {

							createLayout(name, webSocketData.getNodeDataStringValue("schemaLayout"));
							getWebSocket().send(MessageBuilder.forName(getCommand()).callback(callback).build(), true);

						}

					} catch (FrameworkException ex) {

						logger.error("", ex);
					}

					break;

				case "overwrite":

					try {

						final File layoutFile = locateFile(name);

						if (layoutFile.exists()) {

							layoutFile.delete();
						}

						createLayout(name, webSocketData.getNodeDataStringValue("schemaLayout"));
						getWebSocket().send(MessageBuilder.forName(getCommand()).callback(callback).build(), true);

					} catch (FrameworkException ex) {

						logger.error("", ex);
					}

					break;

				case "delete":

					try {

						deleteLayout(name);

						getWebSocket().send(MessageBuilder.forName(getCommand()).callback(callback).build(), true);

					} catch (FrameworkException ex) {

						logger.error("", ex);
					}

					break;

				default:

					getWebSocket().send(MessageBuilder.status().code(422).message("Mode must be one of list, get, add or delete.").build(), true);

			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(422).message("Mode must be one of list, get, add or delete.").build(), true);
		}
	}

	@Override
	public String getCommand() {

		return "LAYOUTS";
	}


	// ----- private methods -----
	private void createLayout(final String name, final String positions) throws FrameworkException {

		// we want to create a sorted, human-readble, diffable representation of the schema
		final App app = StructrApp.getInstance();

		// isolate write output
		try (final Tx tx = app.tx()) {

			final File layoutFile = locateFile(name);

			try (final Writer writer = new FileWriter(layoutFile)) {

				writer.append(positions);
				writer.append("\n");    // useful newline

				writer.flush();
			}

			tx.success();

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}

	private void deleteLayout(final String fileName) throws FrameworkException {

		if (fileName != null) {

			final File layoutFile = locateFile(fileName);
			layoutFile.delete();

		} else {

			throw new FrameworkException(422, "Please supply layout name to delete.");
		}
	}

	public static List<String> listLayouts() {

		final File baseDir       = new File(getLayoutsPath());
		final List<String> fileNames = new LinkedList<>();

		if (baseDir.exists()) {

			final String[] names = baseDir.list((File dir, String name) -> (name.endsWith(".json")));
			if (names != null) {

				fileNames.addAll(Arrays.asList(names));
			}
		}

		Collections.sort(fileNames, String.CASE_INSENSITIVE_ORDER);

		return fileNames;
	}

	public static File locateFile(final String name) throws FrameworkException {

		String fileName = name;
		if (StringUtils.isBlank(fileName)) {

			// create default value
			fileName = "layout.json";
		}

		if (
				(File.separator.equals("/") && fileName.contains(File.separator)) ||
				(File.separator.equals("\\") && (fileName.contains("/") || fileName.contains("\\")))   // because on Windows you can use both types to create sub-directories
			) {
			throw new FrameworkException(422, "Only relative file names are allowed, please use the " + Settings.SnapshotsPath.getKey() + " configuration setting to supply a custom path for snapshots.");
		}

		// append JSON extension
		if (!fileName.endsWith(".json")) {
			fileName = fileName + ".json";
		}

		// create
		final File path = new File(getLayoutsPath() + fileName);
		final File parent = path.getParentFile();
		if (!parent.exists()) {

			parent.mkdirs();
		}

		return path;
	}

	public static String getLayoutsPath() {

		return Settings.getFullSettingPath(Settings.LayoutsPath);

	}

}
