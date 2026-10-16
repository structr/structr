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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.PropertyMap;
import org.structr.core.scheduler.JobQueueManager;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.extraction.ContentExtractionSettings;
import org.structr.extraction.ContentExtractionTraits;
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.api.ContentExtractionException;
import org.structr.extraction.api.document.OcrInfo;
import org.structr.extraction.api.options.EffectiveContentOptions;
import org.structr.extraction.api.processing.AutoExtractionDecision;
import org.structr.extraction.entity.ContentExtractionConfiguration;
import org.structr.extraction.options.ContentOptionsResolver;
import org.structr.extraction.processing.ContentExtractionJob;
import org.structr.extraction.traits.definitions.ContentExtractionConfigurationTraitDefinition;
import org.structr.extraction.traits.properties.ContentFileStateProperties;
import org.structr.extraction.function.ResultMaps;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Backs the content extraction dialog of the Files area: the static methods describe, save and remove
 * of ContentExtractionConfiguration.
 */
public final class ContentConfigurationDialog {

	private static final Logger logger = LoggerFactory.getLogger(ContentConfigurationDialog.class);

	private static final String ENABLED = ContentExtractionConfigurationTraitDefinition.ENABLED_PROPERTY;
	private static final String TARGET  = ContentExtractionConfigurationTraitDefinition.TARGET_PROPERTY;

	private ContentConfigurationDialog() {
	}

	/**
	 * Everything the dialog shows for a File or Folder: its own configuration, the one it inherits, the
	 * effective options with their sources, the defaults from the settings, the allowed values, and for
	 * files the extraction state.
	 */
	public static Object describe(final SecurityContext securityContext, final Object targetArgument) throws FrameworkException {

		final AbstractFile target                      = target(securityContext, targetArgument);
		final ContentExtractionConfiguration own       = ContentConfigurations.own(target);
		final ContentConfigurations.Found inherited    = ContentConfigurations.inherited(target);
		final AutoExtractionDecision decision          = ContentOptionsResolver.shouldExtract(target);
		final Map<String, Object> result               = new LinkedHashMap<>();
		final Map<String, Object> decisionMap          = new LinkedHashMap<>();

		result.put("target",        targetMap(target));
		result.put("canEdit",       securityContext.isSuperUser());
		result.put("configuration", own != null ? configurationMap(own) : null);
		result.put("inherited",     inherited != null ? inheritedMap(inherited) : null);

		decisionMap.put("extract",       decision.extract());
		decisionMap.put("source",        decision.source());
		decisionMap.put("configuration", decision.configurationId());
		decisionMap.put("target",        decision.targetPath());
		result.put("decision", decisionMap);

		try {

			final EffectiveContentOptions options = ContentOptionsResolver.resolve(target, Map.of());

			result.put("effective", options.toMap());
			result.put("warnings",  options.warnings());

		} catch (final IllegalArgumentException ex) {

			result.put("effective", null);
			result.put("warnings",  List.of(ex.getMessage()));
		}

		result.put("defaults", ContentOptionsResolver.defaults());
		result.put("choices",  choices());
		result.put("limits",   Map.of(ContentOptionsResolver.CHUNK_SIZE, Map.of("min", ContentExtractionConfigurationTraitDefinition.MIN_CHUNK_SIZE, "max", ContentExtractionConfigurationTraitDefinition.MAX_CHUNK_SIZE)));
		result.put("ocr",      ocr());

		if (target.is(StructrTraits.FILE)) {

			result.put("state", state(target.as(File.class)));
		}

		return ResultMaps.toGraphObjectMap(result);
	}

	/**
	 * Creates or updates the configuration of a File or Folder. Empty values clear an option, so it uses
	 * the setting again. With process, the files the configuration covers are extracted after the commit
	 * (unchanged files are skipped).
	 */
	public static Object save(final SecurityContext securityContext, final Object targetArgument, final Object valuesArgument, final boolean process) throws FrameworkException {

		ContentExtractionConfigurationTraitDefinition.requireAdmin(securityContext);

		final AbstractFile target                = target(securityContext, targetArgument);
		final App app                            = StructrApp.getInstance(securityContext);
		final ContentExtractionConfiguration own = ContentConfigurations.own(target);
		final PropertyMap properties             = PropertyMap.inputTypeToJavaType(securityContext, ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION, values(valuesArgument));

		if (own != null) {

			own.setProperties(securityContext, properties);

		} else {

			final Traits traits = Traits.of(ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION);

			// set here as well as on creation, so the answer below already shows it
			properties.putIfAbsent(traits.key("name"), "Content extraction for " + target.getPath());
			properties.put(traits.key(TARGET), target);

			app.create(ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION, properties);
		}

		if (process) {

			final ContentExtractionJob job = ContentExtractionJob.automatic(List.of(target.getUuid()), ContentExtractionSettings.ExtractionQueue.getValue());

			// the job runs in its own thread and must see the committed configuration
			TransactionCommand.queuePostCommitProcedure(() -> startJob(job));
		}

		return describe(securityContext, target);
	}

	/**
	 * Deletes the configuration of a File or Folder, if it has one.
	 */
	public static Object remove(final SecurityContext securityContext, final Object targetArgument) throws FrameworkException {

		ContentExtractionConfigurationTraitDefinition.requireAdmin(securityContext);

		final AbstractFile target                = target(securityContext, targetArgument);
		final ContentExtractionConfiguration own = ContentConfigurations.own(target);

		if (own != null) {

			StructrApp.getInstance(securityContext).delete(own);
		}

		return describe(securityContext, target);
	}

	// ----- private methods -----
	private static void startJob(final ContentExtractionJob job) {

		try {

			JobQueueManager.getInstance().addJob(job);

		} catch (final FrameworkException ex) {

			logger.warn("Unable to start content extraction after saving a configuration: {}", ex.getMessage(), ex);
		}
	}

	private static AbstractFile target(final SecurityContext securityContext, final Object argument) throws FrameworkException {

		final NodeInterface node;

		if (argument instanceof NodeInterface given) {

			node = given;

		} else if (argument instanceof String uuid && !uuid.isBlank()) {

			node = StructrApp.getInstance(securityContext).getNodeById(StructrTraits.ABSTRACT_FILE, uuid);

		} else {

			throw new FrameworkException(422, "Expected the File or Folder (or its UUID) as target.");
		}

		if (node == null || !node.is(StructrTraits.ABSTRACT_FILE)) {

			throw new FrameworkException(404, "File or Folder " + argument + " not found.");
		}

		return node.as(AbstractFile.class);
	}

	/**
	 * The values sent by the dialog, with empty texts and lists as null.
	 */
	private static Map<String, Object> values(final Object argument) throws FrameworkException {

		final Map<String, Object> values = new LinkedHashMap<>();
		final Set<String> accepted       = acceptedValues();

		if (argument == null) {

			return values;
		}

		if (!(argument instanceof Map<?, ?> map)) {

			throw new FrameworkException(422, "Expected the configuration values as an object.");
		}

		for (final Map.Entry<?, ?> entry : map.entrySet()) {

			final String name = String.valueOf(entry.getKey());
			if (!accepted.contains(name)) {

				throw new FrameworkException(422, "Unknown configuration value '" + name + "'.");
			}

			values.put(name, empty(entry.getValue()) ? null : entry.getValue());
		}

		// a configuration is enabled unless switched off explicitly
		if (values.containsKey(ENABLED) && values.get(ENABLED) == null) {

			values.put(ENABLED, true);
		}

		return values;
	}

	private static Set<String> acceptedValues() {

		final Set<String> accepted = new HashSet<>(ContentExtractionConfigurationTraitDefinition.OPTION_PROPERTIES);

		accepted.add(ENABLED);
		accepted.add("name");

		return accepted;
	}

	private static boolean empty(final Object value) {

		return value == null || (value instanceof String text && text.isBlank()) || (value instanceof Collection<?> collection && collection.isEmpty());
	}

	private static Map<String, Object> targetMap(final AbstractFile target) {

		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("id",       target.getUuid());
		map.put("type",     target.getType());
		map.put("name",     target.getName());
		map.put("path",     target.getPath());
		map.put("isFolder", target.is(StructrTraits.FOLDER));

		return map;
	}

	private static Map<String, Object> configurationMap(final ContentExtractionConfiguration configuration) {

		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("id",      configuration.getUuid());
		map.put("name",    configuration.getName());
		map.put(ENABLED,   configuration.isEnabled());
		map.put("options", configuration.getOptionValues());

		return map;
	}

	private static Map<String, Object> inheritedMap(final ContentConfigurations.Found found) {

		final Map<String, Object> map = configurationMap(found.configuration());

		map.put("path", found.targetPath());

		return map;
	}

	private static Map<String, Object> choices() {

		final Map<String, Object> choices = new LinkedHashMap<>();

		choices.put(ContentOptionsResolver.SPLITTER,       List.of("structure", "recursive", "sentence", "token", "page"));
		choices.put(ContentOptionsResolver.SIZE_UNIT,      List.of("tokens", "characters"));
		choices.put(ContentOptionsResolver.TOKEN_ENCODING, List.of("cl100k_base", "o200k_base", "p50k_base", "r50k_base"));
		choices.put(ContentOptionsResolver.OCR,            List.of("auto", "always", "off"));

		return choices;
	}

	private static Map<String, Object> ocr() {

		final Map<String, Object> map = new LinkedHashMap<>();

		try {

			final OcrInfo ocr = ContentExtraction.getOcrInfo();

			map.put("available", ocr.available());
			map.put("languages", ocr.languages().stream().sorted().toList());

		} catch (final ContentExtractionException ex) {

			map.put("available", false);
			map.put("languages", List.of());
		}

		return map;
	}

	private static Map<String, Object> state(final File file) {

		final Traits traits           = Traits.of(StructrTraits.FILE);
		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("status",     file.getProperty(traits.key(ContentFileStateProperties.STATUS)));
		map.put("error",      file.getProperty(traits.key(ContentFileStateProperties.ERROR)));
		map.put("date",       file.getProperty(traits.key(ContentFileStateProperties.DATE)));
		map.put("chunkCount", ContentExtraction.getExtractedContent(file).size());
		map.put("ocrUsed",    file.getProperty(traits.key(ContentFileStateProperties.OCR)));

		return map;
	}
}
