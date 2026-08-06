/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.web.maintenance.deploy;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.web.entity.StorageConfiguration;
import org.structr.web.entity.StorageConfigurationEntry;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.StorageConfigurationEntryTraitDefinition;
import org.structr.web.traits.definitions.StorageConfigurationTraitDefinition;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Deployment export/import of {@link StorageConfiguration} nodes, their
 * key/value entries and the linkage to the files/folders they configure.
 *
 * StorageConfigurations are ordinary nodes (not files), so they are not part of
 * the files.json export; they are serialized to a dedicated
 * {@value #STORAGE_CONFIGURATIONS_FILE} inside the ui module's deployment
 * folder. Configuration UUIDs are preserved so the file/folder linkage - which
 * is imported after the files themselves - can be restored by UUID.
 *
 * Entry values are declared as EncryptedStringProperty: the accessor decrypts
 * them, so they are written to the export in clear text (like all other
 * deployment configuration) and re-encrypted on import.
 */
public abstract class StorageConfigurationDeploymentHandler {

	private static final Logger logger = LoggerFactory.getLogger(StorageConfigurationDeploymentHandler.class);

	public static final String STORAGE_CONFIGURATIONS_FILE = "storage-configurations.json";

	private static final String KEY_ID              = GraphObjectTraitDefinition.ID_PROPERTY;
	private static final String KEY_NAME            = NodeInterfaceTraitDefinition.NAME_PROPERTY;
	private static final String KEY_PROVIDER        = StorageConfigurationTraitDefinition.PROVIDER_PROPERTY;
	private static final String KEY_VISIBLE_PUBLIC  = GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY;
	private static final String KEY_VISIBLE_AUTH    = GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY;
	private static final String KEY_ENTRIES         = StorageConfigurationTraitDefinition.ENTRIES_PROPERTY;
	private static final String KEY_FOLDERS         = StorageConfigurationTraitDefinition.FOLDERS_PROPERTY;

	public static void exportDeploymentData(final Path target, final Gson gson) throws FrameworkException {

		final Path confFile                             = target.resolve(STORAGE_CONFIGURATIONS_FILE);
		final List<Map<String, Object>> configurations = new LinkedList<>();
		final App app                                   = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final Traits traits                                   = Traits.of(StructrTraits.STORAGE_CONFIGURATION);
			final PropertyKey<Iterable<NodeInterface>> foldersKey = traits.key(StorageConfigurationTraitDefinition.FOLDERS_PROPERTY);

			for (final NodeInterface node : app.nodeQuery(StructrTraits.STORAGE_CONFIGURATION).sort(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY)).getAsList()) {

				final StorageConfiguration config = node.as(StorageConfiguration.class);
				final Map<String, Object> entry   = new TreeMap<>();

				entry.put(KEY_ID,             node.getUuid());
				entry.put(KEY_NAME,           config.getName());
				entry.put(KEY_PROVIDER,       config.getProvider());
				entry.put(KEY_VISIBLE_PUBLIC, node.isVisibleToPublicUsers());
				entry.put(KEY_VISIBLE_AUTH,   node.isVisibleToAuthenticatedUsers());

				// key/value entries (name -> decrypted value), preserving order
				final Map<String, String> entries = new LinkedHashMap<>();

				for (final NodeInterface entryNode : config.getEntries()) {

					final StorageConfigurationEntry configEntry = entryNode.as(StorageConfigurationEntry.class);
					entries.put(configEntry.getName(), configEntry.getValue());
				}

				entry.put(KEY_ENTRIES, entries);

				// linkage: uuids of the files/folders this configuration is attached to
				final List<String> fileIds            = new LinkedList<>();
				final Iterable<NodeInterface> folders = node.getProperty(foldersKey);

				if (folders != null) {

					for (final NodeInterface file : folders) {

						fileIds.add(file.getUuid());
					}
				}

				entry.put(KEY_FOLDERS, fileIds);

				configurations.add(entry);
			}

			tx.success();
		}

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(confFile.toFile()), StandardCharsets.UTF_8)) {

			gson.toJson(configurations, fos);

		} catch (IOException ioex) {

			logger.warn("Unable to write {}", confFile, ioex);
		}
	}

	public static void importDeploymentData(final Path source, final Gson gson) throws FrameworkException {

		final Path confFile = source.resolve(STORAGE_CONFIGURATIONS_FILE);
		if (!Files.exists(confFile)) {

			return;
		}

		logger.info("Reading {}..", confFile);

		final List<Map<String, Object>> configurations;

		try (final Reader reader = Files.newBufferedReader(confFile, StandardCharsets.UTF_8)) {

			configurations = gson.fromJson(reader, List.class);

		} catch (IOException ioex) {

			logger.warn("Unable to read {}", confFile, ioex);

			return;
		}

		if (configurations == null) {

			return;
		}

		final SecurityContext context = SecurityContext.getSuperUserInstance();
		context.setDoTransactionNotifications(false);

		final App app = StructrApp.getInstance(context);

		try (final Tx tx = app.tx()) {

			final Traits cfgTraits   = Traits.of(StructrTraits.STORAGE_CONFIGURATION);
			final Traits entryTraits = Traits.of(StructrTraits.STORAGE_CONFIGURATION_ENTRY);
			final Traits fileTraits  = Traits.of(StructrTraits.ABSTRACT_FILE);
			final PropertyKey<NodeInterface> storageConfigKey = fileTraits.key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY);

			// clean slate: entries first (they reference the configuration), then configurations
			for (final NodeInterface toDelete : app.nodeQuery(StructrTraits.STORAGE_CONFIGURATION_ENTRY).getAsList()) {

				app.delete(toDelete);
			}

			for (final NodeInterface toDelete : app.nodeQuery(StructrTraits.STORAGE_CONFIGURATION).getAsList()) {

				app.delete(toDelete);
			}

			for (final Map<String, Object> data : configurations) {

				final String id       = (String) data.get(KEY_ID);
				final String name     = (String) data.get(KEY_NAME);
				final String provider = (String) data.get(KEY_PROVIDER);
				final PropertyMap props = new PropertyMap();

				props.put(cfgTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),                name);
				props.put(cfgTraits.key(StorageConfigurationTraitDefinition.PROVIDER_PROPERTY),     provider);

				// preserve the uuid so the file/folder linkage can be restored by id
				if (id != null) {

					props.put(cfgTraits.key(GraphObjectTraitDefinition.ID_PROPERTY), id);
				}

				if (Boolean.TRUE.equals(data.get(KEY_VISIBLE_PUBLIC))) {

					props.put(cfgTraits.key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true);
				}

				if (Boolean.TRUE.equals(data.get(KEY_VISIBLE_AUTH))) {

					props.put(cfgTraits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
				}

				final NodeInterface configNode = app.create(StructrTraits.STORAGE_CONFIGURATION, props);

				// entries
				final Map<String, Object> entries = (Map<String, Object>) data.get(KEY_ENTRIES);
				if (entries != null) {

					for (final Map.Entry<String, Object> configEntry : entries.entrySet()) {

						final PropertyMap entryProps = new PropertyMap();

						entryProps.put(entryTraits.key(StorageConfigurationEntryTraitDefinition.CONFIGURATION_PROPERTY), configNode);
						entryProps.put(entryTraits.key(StorageConfigurationEntryTraitDefinition.NAME_PROPERTY),          configEntry.getKey());
						entryProps.put(entryTraits.key(StorageConfigurationEntryTraitDefinition.VALUE_PROPERTY),         (String) configEntry.getValue());

						app.create(StructrTraits.STORAGE_CONFIGURATION_ENTRY, entryProps);
					}
				}

				// restore the linkage to the files/folders (imported before module data)
				final List<String> fileIds = (List<String>) data.get(KEY_FOLDERS);
				if (fileIds != null) {

					for (final String fileId : fileIds) {

						final NodeInterface file = app.getNodeById(StructrTraits.ABSTRACT_FILE, fileId);
						if (file != null) {

							file.setProperty(storageConfigKey, configNode);

						} else {

							logger.warn("Storage configuration '{}': linked file/folder {} not found on import, skipping linkage", name, fileId);
						}
					}
				}
			}

			tx.success();
		}
	}
}
