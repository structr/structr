/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.text;

import org.apache.commons.io.IOUtils;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.EmptyParser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.agent.Agent;
import org.structr.agent.ReturnValue;
import org.structr.agent.Task;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.entity.File;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.FileTraitDefinition;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 *
 */
public class FulltextIndexingAgent extends Agent<String> {

	private static final Logger logger = LoggerFactory.getLogger(FulltextIndexingAgent.class.getName());
	static final Map<String, Set<String>> languageStopwordMap = new LinkedHashMap<>();
	public static final String TASK_NAME                              = "FulltextIndexing";

	private final Detector detector;

	public FulltextIndexingAgent() {

		setName(TASK_NAME);
		setDaemon(true);

		detector = new DefaultDetector(MimeTypes.getDefaultMimeTypes());
	}


	@Override
	public ReturnValue processTask(final Task<String> task) throws Throwable {

		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
		final App app                         = StructrApp.getInstance(securityContext);

		securityContext.disablePreventDuplicateRelationships();

		if (TASK_NAME.equals(task.getType())) {

			for (final String indexableId : task.getWorkObjects()) {

				for (int i=0; i<3; i++) {

					try (final Tx tx = app.tx(true, false, false)) {

						//SearchCommand.prefetch(Indexable.class, indexableId);

						final NodeInterface indexable = app.nodeQuery(StructrTraits.FILE).key(Traits.of(StructrTraits.FILE).key(GraphObjectTraitDefinition.ID_PROPERTY), indexableId).getFirst();
						if (indexable != null) {

							if (doIndexing(app, indexable.as(File.class))) {

								return ReturnValue.Success;
							}
						}

						tx.success();

					} catch (FrameworkException fex) {}

					// wait for the transaction in a different thread to finish
					try { Thread.sleep(1000); } catch (InterruptedException ex) {}
				}
			}

			// retry
			return ReturnValue.Retry;
		}

		return ReturnValue.Abort;
	}

	@Override
	public Class getSupportedTaskType() {
		return FulltextIndexingTask.class;
	}

	@Override
	protected boolean canHandleMore() {
		return true;
	}

	// ----- private methods -----
	private boolean doIndexing(final App app, final File indexable) {

		boolean parsingSuccessful;
		InputStream inputStream;

		try {

			// load file by UUID to make sure that the transaction that created
			// the file is commited, do not use the actual file object because
			// each thread needs a separate AbstractNode object
			if (indexable != null && !indexable.isTemplate()) {

				// skip files that are larger than the indexing file size limit
				if (getFileSize(indexable) > Settings.IndexingMaxFileSize.getValue() * 1024 * 1024) {

					return true;
				}

				indexable.getSecurityContext().disableModificationOfAccessTime();
				inputStream = indexable.getInputStream();

				if (inputStream != null) {

					final Metadata metadata = new Metadata();

					try (final FulltextTokenizer tokenizer = new FulltextTokenizer()) {

						try (final InputStream is = inputStream) {

							final AutoDetectParser parser = new AutoDetectParser(detector);

							parser.parse(is, new BodyContentHandler(tokenizer), metadata);

							parsingSuccessful = !EmptyParser.class.getName().equals(metadata.get("X-Parsed-By"));
						}

						// only do indexing when parsing was successful
						if (parsingSuccessful) {

							// save raw extracted text
							indexable.setProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.EXTRACTED_CONTENT_PROPERTY), tokenizer.getRawText());
							return true;
						}
					}
				}

			} else {

				// File is not available yet because it was probably created
				// in a separate transaction that is not yet committed.
				return false;
			}

		} catch (final Throwable t) {

			logger.warn("Indexing of {} failed: {}", indexable.getProperty(Traits.of(StructrTraits.ABSTRACT_FILE).key(AbstractFileTraitDefinition.PATH_PROPERTY)), t.getMessage());

			return false;
		}

		return true;
	}

	private long getFileSize(final File file) {
		return StorageProviderFactory.getStorageProvider(file).size();
	}

	static {

		try (final ZipInputStream zis = new ZipInputStream(new BufferedInputStream(FulltextIndexingAgent.class.getResourceAsStream("/stopwords/stop-words.zip")))) {

			for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {

				if (!entry.isDirectory()) {

					final String entryName = entry.getName();
					if (entryName.contains("_") && entryName.endsWith(".txt")) {

						final int langPos     = entryName.lastIndexOf("_") + 1;
						final String language = entryName.substring(langPos, langPos + 2);

						Set<String> stopwordSet = languageStopwordMap.get(language);
						if (stopwordSet == null) {

							stopwordSet = new LinkedHashSet<>();
							languageStopwordMap.put(language, stopwordSet);
						}

						// read stopword set
						for (final String word : IOUtils.readLines(zis)) {
							stopwordSet.add(word.trim());
						}
					}
				}
			}

		} catch (IOException ioex) {

			logger.warn("", ioex);
		}
	}
}
