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
import org.apache.commons.lang3.StringUtils;
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
import org.structr.common.fulltext.Indexable;
import org.structr.common.fulltext.IndexedWord;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Person;
import org.structr.core.entity.Principal;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.graph.search.SearchCommand;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.entity.File;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;
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

						SearchCommand.prefetch(Indexable.class, indexableId);

						final Indexable indexable = app.nodeQuery(Indexable.class).and(GraphObject.id, indexableId).getFirst();
						if (indexable != null) {

							if (doIndexing(app, indexable)) {

								return ReturnValue.Success;
							}
						}

						tx.success();

					} catch (FrameworkException fex) {}

					// wait for the transaction in a different thread to finish
					try { Thread.sleep(1000); } catch (InterruptedException ex) {}
				}
			}

			return ReturnValue.Success;
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
	private boolean doIndexing(final App app, final Indexable indexable) {

		boolean parsingSuccessful         = false;
		InputStream inputStream           = null;
		String fileName                   = "unknown file";

		try {

			// load file by UUID to make sure that the transaction that created
			// the file is commited, do not use the actual file object because
			// each thread needs a separate AbstractNode object
			if (indexable != null && !(indexable instanceof File && ((File)indexable).isTemplate())) {

				// skip files that are larger than the indexing file size limit
				if (getFileSize(indexable) > Settings.IndexingMaxFileSize.getValue() * 1024 * 1024) {

					return true;
				}

				indexable.getSecurityContext().disableModificationOfAccessTime();
				inputStream = indexable.getInputStream();
				fileName    = indexable.getName();

				if (inputStream != null) {

					final Metadata metadata = new Metadata();

					try (final FulltextTokenizer tokenizer = new FulltextTokenizer(indexable.indexedWordMinLength(), indexable.indexedWordMaxLength())) {

						try (final InputStream is = inputStream) {

							final AutoDetectParser parser = new AutoDetectParser(detector);

							parser.parse(is, new BodyContentHandler(tokenizer), metadata);

							parsingSuccessful = !EmptyParser.class.getName().equals(metadata.get("X-Parsed-By"));
						}

						// only do indexing when parsing was successful
						if (parsingSuccessful) {

							// save raw extracted text
							indexable.setProperty(StructrApp.key(File.class, "extractedContent"), tokenizer.getRawText());

							// tokenize name
							tokenizer.write(getName());

							// tokenize owner name
							final Principal _owner = indexable.getOwnerNode();
							if (_owner != null) {

								final String ownerName = _owner.getName();
								if (ownerName != null) {

									tokenizer.write(ownerName);
								}

								final String eMail = _owner.getEMail();
								if (eMail != null) {

									tokenizer.write(eMail);
								}
							}

							// index document excluding stop words
							final Set<String> stopWords             = languageStopwordMap.get(tokenizer.getLanguage());
							final Iterator<String> wordIterator     = tokenizer.getWords().iterator();
							final Map<String, Integer> indexedWords = new LinkedHashMap<>();

							while (wordIterator.hasNext()) {

								// strip quotes
								final String word = StringUtils.strip(wordIterator.next(), "\"\'");
								if (!stopWords.contains(word)) {

									add(indexedWords, word);
								}
							}

							final List<String> topWords       = getFrequencySortedTopWords(indexedWords, indexable.maximumIndexedWords());
							final List<IndexedWord> wordNodes = new LinkedList<>();

							try {

								// create words first
								for (final String word : topWords) {

									IndexedWord wordNode = app.nodeQuery(IndexedWord.class).andName(word).getFirst();
									if (wordNode == null) {

										wordNode = app.create(IndexedWord.class, word);
									}

									wordNodes.add(wordNode);
								}

								// store indexed words
								indexable.setProperty(StructrApp.key(File.class, "words"), wordNodes);

							} catch (Throwable t) {

								logger.info("Unable to store fulltext indexing result for {}, retrying after 1000ms..", fileName);

								return false;
							}
						}
					}
				}

			} else {

				// File is not available yet because it was probably created
				// in a separate transaction that is not yet committed.
				return false;
			}

		} catch (final Throwable t) {

			logger.warn("Indexing of {} failed: {}", indexable.getProperty(StructrApp.key(File.class, "path")), t.getMessage());

			return false;
		}

		return true;
	}

	private void add(final Map<String, Integer> frequencyMap, final String word) {

		Integer count = frequencyMap.get(word);
		if (count == null) {

			frequencyMap.put(word, 1);

		} else {

			frequencyMap.put(word, count + 1);
		}
	}

	private List<String> getFrequencySortedTopWords(final Map<String, Integer> frequency, final Integer maxEntries) {

		final Map<Integer, Set<String>> words = new TreeMap<>(Collections.reverseOrder());
		final ArrayList<String> resultList    = new ArrayList<>();

		for (final Entry<String, Integer> frequencyEntry : frequency.entrySet()) {

			final String word   = frequencyEntry.getKey();
			final Integer count = frequencyEntry.getValue();

			Set<String> wordSet = words.get(count);
			if (wordSet == null) {

				wordSet = new TreeSet<>();
				words.put(count, wordSet);
			}

			wordSet.add(word);
		}

		for (final Set<String> set : words.values()) {

			for (final String word : set) {

				resultList.add(word);

				if (resultList.size() >= maxEntries) {
					break;
				}
			}

			if (resultList.size() >= maxEntries) {
				break;
			}
		}

		return resultList;
	}

	private long getFileSize(final Indexable indexable) {

		if (indexable instanceof File) {

			final File file     = (File)indexable;
			final Long fileSize = StorageProviderFactory.getStorageProvider(file).size();

			if (fileSize != null) {

				return fileSize;
			}

		}

		return -1L;
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
