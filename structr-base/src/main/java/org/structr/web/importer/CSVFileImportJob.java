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
package org.structr.web.importer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessMode;
import org.structr.common.ContextStore;
import org.structr.common.ResultTransformer;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.JsonInput;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.module.StructrModule;
import org.structr.module.api.APIBuilder;
import org.structr.rest.common.CsvHelper;
import org.structr.web.entity.File;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Iterator;
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

		} else {

			final StructrModule module = StructrApp.getConfiguration().getModules().get("api-builder");

			if (module == null || !(module instanceof APIBuilder) ) {

				throw new FrameworkException(400, "Cannot import CSV, API builder module is not available.");

			}
		}

		return true;
	}

	@Override
	public boolean canRunMultiThreaded() {
		// think about this, maybe we can add parallelism here?
		return false;
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

			final APIBuilder builder       = (APIBuilder) StructrApp.getConfiguration().getModules().get("api-builder");
			final SimpleDateFormat df      = new SimpleDateFormat("yyyyMMddHHMM");
			final String importTypeName    = "ImportFromCsv" + df.format(System.currentTimeMillis());

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

				final ResultTransformer mapper     = builder.createMapping(app, targetType, importTypeName, importMappings, transforms);

				IMPORT_TYPE currentImportType = IMPORT_TYPE.NODE;
				Class targetEntityType        = StructrApp.getConfiguration().getNodeEntityClass(targetType);

				Class relSourceType = null;
				Class relTargetType = null;
				if (targetEntityType == null) {
					targetEntityType  = StructrApp.getConfiguration().getRelationshipEntityClass(targetType);
					currentImportType = IMPORT_TYPE.REL;

					final Relation template = (Relation)targetEntityType.getDeclaredConstructor().newInstance();
					relSourceType = template.getSourceType();
					relTargetType = template.getTargetType();

					if (targetEntityType == null) {
						throw new FrameworkException(422, "Could not find type for '" + targetType + "'!");
					}
				}

				final Character fieldSeparator     = delimiter.charAt(0);
				final Character quoteCharacter     = StringUtils.isNotEmpty(quoteChar) ? quoteChar.charAt(0) : null;
				final Iterable<JsonInput> iterable = CsvHelper.cleanAndParseCSV(threadContext, new InputStreamReader(is, "utf-8"), targetEntityType, fieldSeparator, quoteCharacter, range, reverse(importMappings), rfc4180Mode, strictQuotes);
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

								mapper.transformInput(threadContext, targetEntityType, input);

								if (currentImportType.equals(IMPORT_TYPE.NODE)) {

									final PropertyMap properties = PropertyMap.inputTypeToJavaType(threadContext, targetEntityType, input);

									if (distinct) {

										// check for existing object and ignore import
										if (app.nodeQuery(targetEntityType).and(properties).getFirst() == null) {

											app.create(targetEntityType, properties);
											overallCount++;

										} else {

											ignoreCount++;
										}

									} else {

										app.create(targetEntityType, properties);
										overallCount++;
									}

								} else {

									final AbstractNode sourceNode = (AbstractNode)app.get(relSourceType, (String)input.get("sourceId"));
									final AbstractNode targetNode = (AbstractNode)app.get(relTargetType, (String)input.get("targetId"));

									app.create(sourceNode, targetNode, targetEntityType, PropertyMap.inputTypeToJavaType(threadContext, targetEntityType, input));
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

				try {

					builder.removeMapping(app, targetType, importTypeName);

				} catch (FrameworkException ex) {
					logger.warn("Exception while cleaning up CSV Import Mapping '{}'", targetType);
				}

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
}
