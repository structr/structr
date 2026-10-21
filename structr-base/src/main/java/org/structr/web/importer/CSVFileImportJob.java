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
package org.structr.web.importer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessMode;
import org.structr.common.ContextStore;
import org.structr.common.PropertyMapping;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.JsonInput;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.RelationshipInterfaceTraitDefinition;
import org.structr.rest.common.CsvHelper;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.File;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CSVFileImportJob extends FileImportJob {

	private static final Logger logger = LoggerFactory.getLogger(CSVFileImportJob.class.getName());

	private enum IMPORT_TYPE {

		NODE, REL
	}

	public CSVFileImportJob(File file, Principal user, Map<String, Object> configuration, final ContextStore ctxStore) throws FrameworkException {

		super(file, user, configuration, ctxStore);
	}

	@Override
	public boolean runInitialChecks () throws FrameworkException {

		final String targetType   = getOrDefault(configuration.get("targetType"), null);
		final String delimiter    = getOrDefault(configuration.get("delimiter"), ";");
		final String quoteChar    = getOrDefault(configuration.get("quoteChar"), "\"");

		if (targetType == null || delimiter == null || quoteChar == null) {

			throw new FrameworkException(400, "Cannot import CSV, please specify target type, delimiter and quote character.");
		}

		return true;
	}

	@Override
	public Runnable getRunnable() {

		return () -> {

			final Map<String, String> importMappings = getOrDefault(configuration.get("mappings"), Collections.EMPTY_MAP);
			final Map<String, String> transforms     = getOrDefault(configuration.get("transforms"), Collections.EMPTY_MAP);
			final String targetType                  = getOrDefault(configuration.get("targetType"), null);
			final String delimiter                   = getOrDefault(configuration.get("delimiter"), ";");
			final String quoteChar                   = getOrDefault(configuration.get("quoteChar"), "\"");
			final String range                       = getOrDefault(configuration.get("range"), "");
			final boolean rfc4180Mode                = getOrDefault(configuration.get("rfc4180Mode"), false);
			final boolean strictQuotes               = getOrDefault(configuration.get("strictQuotes"), false);
			final boolean collectValues              = getOrDefault(configuration.get("collectValues"), false);
			final boolean distinct                   = getOrDefault(configuration.get("distinct"), false);
			final boolean ignoreInvalid              = getOrDefault(configuration.get("ignoreInvalid"), false);
			final Integer commitInterval             = parseInt(configuration.get("commitInterval"), 1000);

			logger.info("Importing CSV from {} ({}) to {} using {}", filePath, fileUuid, targetType, configuration);

			final SecurityContext threadContext = SecurityContext.getInstance(user, AccessMode.Backend);

			threadContext.setContextStore(ctxStore);
			final App app                       = StructrApp.getInstance(threadContext);

			// disable transaction notifications
			threadContext.disableModificationOfAccessTime();
			threadContext.setDoTransactionNotifications(false);
			threadContext.disablePreventDuplicateRelationships();

			try (final InputStream is = getFileInputStream(threadContext)) {

				if (is == null) {

					return;
				}

				final long startTime = System.currentTimeMillis();

				reportBegin();

				final PropertyMapping mapper  = createMapping(importMappings, transforms);
				IMPORT_TYPE currentImportType = IMPORT_TYPE.NODE;
				Traits targetEntityType       = Traits.of(targetType);

				if (targetEntityType == null) {

					throw new FrameworkException(422, "Could not find type for '" + targetType + "'!");
				}

				String relSourceType = null;
				String relTargetType = null;

				if (targetEntityType.isRelationshipType()) {

					currentImportType = IMPORT_TYPE.REL;

					final Relation template = targetEntityType.getRelation();

					relSourceType = template.getSourceType();
					relTargetType = template.getTargetType();
				}

				final Character fieldSeparator     = delimiter.charAt(0);
				final Character quoteCharacter     = StringUtils.isNotEmpty(quoteChar) ? quoteChar.charAt(0) : null;
				final Iterable<JsonInput> iterable = CsvHelper.cleanAndParseCSV(threadContext, new InputStreamReader(is, StandardCharsets.UTF_8), targetEntityType.getName(), fieldSeparator, quoteCharacter, range, reverse(importMappings), rfc4180Mode, strictQuotes);
				final Iterator<JsonInput> iterator = iterable.iterator();
				int chunks                         = 0;
				int ignoreCount                    = 0;
				int overallCount                   = 0;

				while (iterator.hasNext()) {

					int count = 0;

					try (final Tx tx = app.tx()) {

						final long chunkStartTime = System.currentTimeMillis();

						while (iterator.hasNext() && count++ < commitInterval) {

							final JsonInput input = iterator.next();
							if (input == null) {

								if (ignoreInvalid) {

									ignoreCount++;

								} else {

									throw new FrameworkException(422, "Error in CSV after " + (overallCount + ignoreCount) + " lines.");
								}

							} else {

								// a fresh context per line, so transform scripts cannot carry state from one line to the next
								mapper.transformInput(new ActionContext(threadContext), input);

								if (currentImportType.equals(IMPORT_TYPE.NODE)) {

									final PropertyMap properties = PropertyMap.inputTypeToJavaType(threadContext, targetEntityType.getName(), input);

									if (distinct) {

										// check for existing object and ignore import
										if (app.nodeQuery(targetEntityType.getName()).key(properties).getFirst() == null) {

											app.create(targetEntityType.getName(), properties);
											overallCount++;

										} else {

											ignoreCount++;
										}

									} else {

										app.create(targetEntityType.getName(), properties);
										overallCount++;
									}

								} else {

									final NodeInterface sourceNode = app.getNodeById(relSourceType, (String)input.get(RelationshipInterfaceTraitDefinition.SOURCE_ID_PROPERTY));
									final NodeInterface targetNode = app.getNodeById(relTargetType, (String)input.get(RelationshipInterfaceTraitDefinition.TARGET_ID_PROPERTY));

									app.create(sourceNode, targetNode, targetEntityType.getName(), PropertyMap.inputTypeToJavaType(threadContext, targetEntityType.getName(), input));
									overallCount++;
								}
							}
						}

						tx.success();

						chunks++;

						chunkFinished(chunkStartTime, chunks, commitInterval, overallCount, ignoreCount);
					}

					// do this outside of the transaction!
					shouldPause();

					if (shouldAbort()) {

						return;
					}
				}

				importFinished(startTime, overallCount, ignoreCount);

			} catch (Exception ex) {

				reportException(ex);

			} finally {

				jobFinished();
			}
		};

	}

	@Override
	public String getJobType() {

		return "CSV";
	}

	@Override
	public String getJobStatusType() {

		return "FILE_IMPORT_STATUS";
	}

	@Override
	public String getJobExceptionMessageType() {

		return "FILE_IMPORT_EXCEPTION";
	}

	// ----- private methods -----
	private PropertyMapping createMapping(final Map<String, String> importMappings, final Map<String, String> transforms) throws FrameworkException {

		final List<PropertyMapping.Entry> entries = new LinkedList<>();

		for (final Map.Entry<String, String> mapping : importMappings.entrySet()) {

			final String propertyName = mapping.getKey();

			entries.add(new PropertyMapping.Entry(propertyName, mapping.getValue(), transforms.get(propertyName), null));
		}

		return new PropertyMapping(entries);
	}
}
