/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.JsonInput;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.RelationProperty;
import org.structr.core.traits.Traits;
import org.structr.module.StructrModule;
import org.structr.module.api.APIBuilder;
import org.structr.rest.common.CsvHelper;
import org.structr.web.entity.File;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

public class MixedCSVFileImportJob extends FileImportJob {

	private static final Logger logger = LoggerFactory.getLogger(MixedCSVFileImportJob.class.getName());

	public MixedCSVFileImportJob(File file, Principal user, Map<String, Object> configuration, final ContextStore ctxStore) throws FrameworkException {
		super(file, user, configuration, ctxStore);
	}

	@Override
	public boolean runInitialChecks () throws FrameworkException {

		final String delimiter  = getOrDefault(configuration.get("delimiter"), ";");
		final String quoteChar  = getOrDefault(configuration.get("quoteChar"), "\"");

		if (delimiter == null || quoteChar == null) {

			throw new FrameworkException(400, "Cannot import CSV, please specify at least delimiter and quote character.");

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

			final Map<String, Object> mixedMappings  = getOrDefault(configuration.get("mixedMappings"), Collections.EMPTY_MAP);
			final Map<String, String> importMappings = getOrDefault(configuration.get("mappings"), Collections.EMPTY_MAP);
			final Map<String, String> transforms     = getOrDefault(configuration.get("transforms"), Collections.EMPTY_MAP);
			final String delimiter                   = getOrDefault(configuration.get("delimiter"), ";");
			final String quoteChar                   = getOrDefault(configuration.get("quoteChar"), "\"");
			final String range                       = getOrDefault(configuration.get("range"), "");
			final boolean strictQuotes               = getOrDefault(configuration.get("strictQuotes"), false);
			final boolean distinct                   = getOrDefault(configuration.get("distinct"), false);
			final Integer commitInterval             = parseInt(configuration.get("commitInterval"), 1000);

			//logger.info("Importing CSV from {} ({}) to {} using {}", filePath, fileUuid, targetType, configuration);

			final SecurityContext threadContext = SecurityContext.getInstance(user, AccessMode.Backend);
			final SimpleDateFormat df           = new SimpleDateFormat("yyyyMMddHHMM");
			final App app                       = StructrApp.getInstance(threadContext);

			// disable transaction notifications
			threadContext.setContextStore(ctxStore);
			threadContext.disableModificationOfAccessTime();
			threadContext.setDoTransactionNotifications(false);
			threadContext.disablePreventDuplicateRelationships();

			try (final InputStream is = getFileInputStream(threadContext)) {

				if (is == null) {
					return;
				}

				final long startTime = System.currentTimeMillis();

				reportBegin();

				final Map<String, RelationProperty> relKeyCache = new LinkedHashMap<>();
				final Character fieldSeparator                  = delimiter.charAt(0);
				final Character quoteCharacter                  = StringUtils.isNotEmpty(quoteChar) ? quoteChar.charAt(0) : null;
				final Iterable<JsonInput> iterable              = CsvHelper.cleanAndParseCSV(threadContext, new InputStreamReader(is, StandardCharsets.UTF_8), fieldSeparator, quoteCharacter, range, reverse(importMappings), strictQuotes);
				final Iterator<JsonInput> iterator              = iterable.iterator();
				int chunks                                      = 0;
				int ignoreCount                                 = 0;
				int overallCount                                = 0;

				while (iterator.hasNext()) {

					int count = 0;

					try (final Tx tx = app.tx()) {

						final long chunkStartTime = System.currentTimeMillis();

						while (iterator.hasNext() && count++ < commitInterval) {

							final List<StringTuple> tuples              = new LinkedList<>();
							final Map<String, NodeInterface> rowObjects = new LinkedHashMap<>();
							final JsonInput row                         = iterator.next();

							for (final Entry<String, Object> entry : mixedMappings.entrySet()) {

								final Map<String, Object> data       = (Map<String, Object>)entry.getValue();
								final Map<String, String> properties = (Map<String, String>)data.get("properties");
								final List<String> relationships     = (List<String>)data.get("relationships");
								final Map<String, Object> inputData  = new LinkedHashMap<>();
								final String typeName                = (String)data.get("name");
								final PropertyMap searchAttributes   = new PropertyMap();
								final Traits type                    = Traits.of(typeName);
								NodeInterface newObject              = null;

								// select only mapped propertiers
								for (final String keyName : properties.values()) {
									inputData.put(keyName, row.get(keyName));
								}

								// transform properties using actual type and input converters etc.
								final PropertyMap transformedData = PropertyMap.inputTypeToJavaType(threadContext, typeName, inputData);

								// check if the transformed data contains keys with uniqueness constraints
								for (final PropertyKey key : transformedData.keySet()) {

									if (key.isUnique()) {

										searchAttributes.put(key, transformedData.get(key));
									}
								}

								// search for object before creating it again
								if (!searchAttributes.isEmpty()) {
									newObject = app.nodeQuery(typeName).key(searchAttributes).getFirst();
								}

								// create new object if it doesn't exist yet
								if (newObject == null) {

									newObject = app.create(typeName, transformedData);
									overallCount++;
								}

								// store object for later use (new or existing)
								rowObjects.put(typeName, newObject);

								// examine relationships between objects
								for (final String related : relationships) {

									tuples.add(new StringTuple(typeName, related));
								}
							}

							// link objects??!
							for (final StringTuple tuple : tuples) {

								RelationProperty relKey = relKeyCache.get(tuple.name());
								if (relKey == null) {

									relKey = findRelationshipKey(tuple.left, tuple.right);
									if (relKey != null) {

										relKeyCache.put(tuple.name(), relKey);
									}
								}

								if (relKey != null) {

									final NodeInterface obj1 = rowObjects.get(tuple.left);
									final NodeInterface obj2 = rowObjects.get(tuple.right);

									switch (relKey.getDirectionKey()) {

										case "in":
											relKey.addSingleElement(threadContext, obj1, obj2);
											break;

										case "out":
											relKey.addSingleElement(threadContext, obj2, obj1);
											break;
									}
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

	private RelationProperty findRelationshipKey(final String type1, final String type2) {

		final Traits class1 = Traits.of(type1);
		final Traits class2 = Traits.of(type2);
		PropertyKey relKey  = null;

		for (final PropertyKey key : class1.getAllPropertyKeys()) {

			if (class2.getName().equals(key.relatedType())) {

				relKey = key;
				break;
			}
		}

		if (relKey == null) {

			for (final PropertyKey key : class2.getAllPropertyKeys()) {

				if (class1.getName().equals(key.relatedType())) {

					relKey = key;
					break;
				}
			}
		}

		if (relKey instanceof RelationProperty) {

			return (RelationProperty)relKey;
		}

		return null;
	}

	private class StringTuple {

		public String left = null;
		public String right = null;

		public StringTuple(final String left, final String right) {
			this.right = right;
			this.left  = left;
		}

		public String name() {
			return left + "-" + right;
		}
	}
}