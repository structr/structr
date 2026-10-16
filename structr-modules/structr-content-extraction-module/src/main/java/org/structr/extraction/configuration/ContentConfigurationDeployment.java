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
package org.structr.extraction.configuration;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.extraction.ContentExtractionTraits;
import org.structr.extraction.entity.ContentExtractionConfiguration;
import org.structr.extraction.traits.definitions.ContentExtractionConfigurationTraitDefinition;
import org.structr.web.entity.AbstractFile;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Deployment export and import of content extraction configurations.
 *
 * Configurations are ordinary nodes, not files, so they are written to a file of their own in the
 * module's deployment folder. Module data is imported after the files, and files keep their UUIDs, so
 * each configuration is attached to its File or Folder again by UUID. Like the storage configurations,
 * the import replaces all existing configurations.
 */
public final class ContentConfigurationDeployment {

	private static final Logger logger = LoggerFactory.getLogger(ContentConfigurationDeployment.class);

	public static final String CONFIGURATIONS_FILE = "content-extraction-configurations.json";

	private static final String KEY_ID             = GraphObjectTraitDefinition.ID_PROPERTY;
	private static final String KEY_NAME           = NodeInterfaceTraitDefinition.NAME_PROPERTY;
	private static final String KEY_TARGET         = ContentExtractionConfigurationTraitDefinition.TARGET_PROPERTY;
	private static final String KEY_TARGET_PATH    = "targetPath";
	private static final String KEY_ENABLED        = ContentExtractionConfigurationTraitDefinition.ENABLED_PROPERTY;
	private static final String KEY_OPTIONS        = "options";

	private ContentConfigurationDeployment() {
	}

	public static void exportConfigurations(final Path target, final Gson gson) throws FrameworkException {

		final Path file                                = target.resolve(CONFIGURATIONS_FILE);
		final List<Map<String, Object>> configurations = new ArrayList<>();
		final App app                                  = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final Traits traits = Traits.of(ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION);

			for (final NodeInterface node : app.nodeQuery(ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION).sort(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY)).getAsList()) {

				final ContentExtractionConfiguration configuration = node.as(ContentExtractionConfiguration.class);
				final AbstractFile configured                      = configuration.getTarget();

				if (configured == null) {

					continue;
				}

				final Map<String, Object> entry = new TreeMap<>();

				entry.put(KEY_ID,          node.getUuid());
				entry.put(KEY_NAME,        node.getName());
				entry.put(KEY_TARGET,      configured.getUuid());
				entry.put(KEY_TARGET_PATH, configured.getPath());
				entry.put(KEY_ENABLED,     configuration.isEnabled());
				entry.put(KEY_OPTIONS,     new TreeMap<>(configuration.getOptionValues()));

				configurations.add(entry);
			}

			tx.success();
		}

		try (final Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {

			gson.toJson(configurations, writer);

		} catch (final IOException ex) {

			logger.warn("Unable to write {}", file, ex);
		}
	}

	public static void importConfigurations(final Path source, final Gson gson) throws FrameworkException {

		final Path file = source.resolve(CONFIGURATIONS_FILE);
		if (!Files.exists(file)) {

			return;
		}

		final List<Map<String, Object>> configurations;

		try (final Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {

			configurations = gson.fromJson(reader, List.class);

		} catch (final IOException ex) {

			logger.warn("Unable to read {}", file, ex);

			return;
		}

		final SecurityContext context = SecurityContext.getSuperUserInstance();
		context.setDoTransactionNotifications(false);

		final App app = StructrApp.getInstance(context);

		try (final Tx tx = app.tx()) {

			for (final NodeInterface existing : app.nodeQuery(ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION).getAsList()) {

				app.delete(existing);
			}

			if (configurations != null) {

				for (final Map<String, Object> data : configurations) {

					importConfiguration(context, app, data);
				}
			}

			tx.success();
		}
	}

	// ----- private methods -----
	private static void importConfiguration(final SecurityContext context, final App app, final Map<String, Object> data) throws FrameworkException {

		final String targetId          = (String) data.get(KEY_TARGET);
		final NodeInterface configured = targetId != null ? app.getNodeById(StructrTraits.ABSTRACT_FILE, targetId) : null;

		if (configured == null) {

			logger.warn("Content extraction configuration '{}': file or folder {} ({}) not found, configuration skipped.", data.get(KEY_NAME), targetId, data.get(KEY_TARGET_PATH));

			return;
		}

		final Map<String, Object> input = new LinkedHashMap<>();
		final Object options            = data.get(KEY_OPTIONS);

		if (options instanceof Map<?, ?> map) {

			for (final Map.Entry<?, ?> option : map.entrySet()) {

				input.put(String.valueOf(option.getKey()), option.getValue());
			}
		}

		input.put(KEY_ID,      data.get(KEY_ID));
		input.put(KEY_NAME,    data.get(KEY_NAME));
		input.put(KEY_ENABLED, !Boolean.FALSE.equals(data.get(KEY_ENABLED)));

		// converts the numbers Gson reads as Double, and lists to arrays
		final PropertyMap properties = PropertyMap.inputTypeToJavaType(context, ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION, input);

		properties.put(Traits.of(ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION).key(KEY_TARGET), configured);

		app.create(ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION, properties);
	}
}
