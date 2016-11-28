/**
 * Copyright (C) 2010-2016 Structr GmbH
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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.Property;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------

/**
 *
 *
 */
public class LayoutsCommand extends AbstractCommand {

	private static final Logger logger                          = LoggerFactory.getLogger(LayoutsCommand.class.getName());
	private static final Property<List<String>> layoutsProperty = new GenericProperty<>("layouts");

	static {

		StructrWebSocket.addCommand(LayoutsCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		final Map<String, Object> data        = webSocketData.getNodeData();
		final String mode                     = (String)data.get("mode");
		final String name                     = (String)data.get("name");

		if (mode != null) {

			final List<GraphObject> result = new LinkedList<>();

			switch (mode) {

				case "list":

					final List<String> layouts = LayoutsCommand.listLayouts();

					if (layouts != null) {

						final GraphObjectMap layoutContainer = new GraphObjectMap();

						layoutContainer.put(layoutsProperty, layouts);
						result.add(layoutContainer);

						webSocketData.setResult(result);
						webSocketData.setRawResultCount(1);
						getWebSocket().send(webSocketData, true);
					}

					break;

				case "get":

					try {

						final String content = new String(Files.readAllBytes(locateFile(name, false).toPath()));

						getWebSocket().send(MessageBuilder.finished().callback(callback).data("schemaLayout", content).build(), true);

					} catch (IOException | FrameworkException ex) {

						logger.error("", ex);

					}

					break;

				case "add":

					final String positions = (String)data.get("schemaLayout");

					try {

						final File layoutFile = locateFile(name, false);

						if (layoutFile.exists()) {

							getWebSocket().send(MessageBuilder.status().code(422).message("Layout already exists!").build(), true);

						} else {

							createLayout(name, positions);

						}

						getWebSocket().send(MessageBuilder.finished().callback(callback).build(), true);

					} catch (FrameworkException ex) {

						logger.error("", ex);

					}

					break;

				case "delete":

					try {

						deleteLayout(name);

						getWebSocket().send(MessageBuilder.finished().callback(callback).build(), true);

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

			final File layoutFile = locateFile(name, false);

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

			final File layoutFile = locateFile(fileName, false);
			layoutFile.delete();

		} else {

			throw new FrameworkException(422, "Please supply schema name to import.");
		}
	}

	public static List<String> listLayouts() {

		final File baseDir       = new File(getBasePath());
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

	public static File locateFile(final String name, final boolean addTimestamp) throws FrameworkException {

		String fileName = name;
		if (StringUtils.isBlank(fileName)) {

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

		return "layouts/";
	}
}
