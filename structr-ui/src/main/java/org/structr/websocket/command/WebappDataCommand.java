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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
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
public class WebappDataCommand extends AbstractCommand {

	private static final Logger logger                        = LoggerFactory.getLogger(WebappDataCommand.class.getName());
	private static final Property<List<String>> namesProperty = new GenericProperty<>("names");

	static {

		StructrWebSocket.addCommand(WebappDataCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final Map<String, Object> data        = webSocketData.getNodeData();
		final String category                 = (String)data.get("category");
		final String mode                     = (String)data.get("mode");
		final String name                     = (String)data.get("name");

		if (mode != null) {

			final List<GraphObject> result = new LinkedList<>();

			switch (mode) {

				case "list":

					final List<String> values = WebappDataCommand.listValues(category);

					if (values != null) {

						final GraphObjectMap container = new GraphObjectMap();

						container.put(namesProperty, values);
						result.add(container);

						webSocketData.setResult(result);
						webSocketData.setRawResultCount(1);
						getWebSocket().send(webSocketData, true);
					}

					break;

				case "get":

					try {

						final String content = new String(Files.readAllBytes(locateFile(category, name).toPath()));

						getWebSocket().send(MessageBuilder.finished().callback(callback).data("value", content).build(), true);

					} catch (IOException | FrameworkException ex) {

						logger.error("", ex);

					}

					break;

				case "add":

					final String positions = (String)data.get("value");

					try {

						final File layoutFile = locateFile(category, name);

						if (layoutFile.exists()) {

							getWebSocket().send(MessageBuilder.status().code(422).message("Category/name combination already exists!").build(), true);

						} else {

							createValue(category, name, positions);

						}

						getWebSocket().send(MessageBuilder.finished().callback(callback).build(), true);

					} catch (FrameworkException ex) {

						logger.error("", ex);

					}

					break;

				case "delete":

					try {

						deleteValue(category, name);

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
		return "WEBAPPDATA";
	}

	// ----- private methods -----
	private void createValue(final String category, final String name, final String value) throws FrameworkException {

		// we want to create a sorted, human-readble, diffable representation of the schema
		final App app = StructrApp.getInstance();

		// isolate write output
		try (final Tx tx = app.tx()) {

			final File layoutFile = locateFile(category, name);

			try (final Writer writer = new FileWriter(layoutFile)) {

				writer.append(value);
				writer.append("\n");

				writer.flush();
			}

			tx.success();

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}

	private void deleteValue(final String category, final String fileName) throws FrameworkException {

		if (fileName != null) {

			final File layoutFile = locateFile(category, fileName);
			layoutFile.delete();

		} else {

			throw new FrameworkException(422, "Please supply category/name to delete.");
		}
	}

	public static List<String> listValues(final String category) {

		final File baseDir           = new File(Paths.get(getValuesPath(), category).toString());
		final List<String> fileNames = new LinkedList<>();

		if (baseDir.exists()) {

			final String[] names = baseDir.list();
			if (names != null) {

				fileNames.addAll(Arrays.asList(names));
			}
		}

		Collections.sort(fileNames, String.CASE_INSENSITIVE_ORDER);

		return fileNames;
	}

	public static File locateFile(final String category, final String name) throws FrameworkException {

		final String finalCategory = StringUtils.isBlank(category) ? "defaultCategory" : category;
		final String finalName     = StringUtils.isBlank(name) ? "defaultName" : name;
		final Path path            = Paths.get(getValuesPath(), category, name);

		assertValidFileName(finalCategory, finalName);

		// create
		final File file   = new File(path.toString());
		final File parent = file.getParentFile();

		if (!parent.exists()) {

			parent.mkdirs();
		}

		return file;
	}

	public static void assertValidFileName(final String category, final String name) throws FrameworkException {

		boolean invalid = false;

		invalid |= (File.separator.equals("/") && (category.contains(File.separator) || name.contains(File.separator)));
		invalid |= (File.separator.equals("\\") && (category.contains("/") || category.contains("\\") || name.contains("/") || name.contains("\\")));

		if (invalid) {
			throw new FrameworkException(422, "Only relative file names are allowed, please use the " + Settings.WebDataPath.getKey() + " configuration setting to supply a custom path for web app data items.");
		}
	}

	public static String getValuesPath() {
		return Settings.getFullSettingPath(Settings.WebDataPath);
	}
}
