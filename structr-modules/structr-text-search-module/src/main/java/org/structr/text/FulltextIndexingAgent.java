/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.IOUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.bouncycastle.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.agent.Agent;
import org.structr.agent.ReturnValue;
import org.structr.agent.Task;
import org.structr.common.fulltext.Indexable;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Person;
import org.structr.core.entity.Principal;
import static org.structr.core.graph.NodeInterface.owner;
import org.structr.core.graph.Tx;

/**
 *
 *
 */
public class FulltextIndexingAgent extends Agent<Indexable> {

	private static final Logger logger = LoggerFactory.getLogger(FulltextIndexingAgent.class.getName());
	private static final Map<String, Set<String>> languageStopwordMap = new LinkedHashMap<>();
	public static final String TASK_NAME                              = "FulltextIndexing";

	private static final int maxStringLength = 32700;
	private static final int maxTopWords     = 10000;

	@Override
	public ReturnValue processTask(final Task<Indexable> task) throws Throwable {

		if (TASK_NAME.equals(task.getType())) {

			for (final Indexable file : task.getNodes()) {

				doIndexing(file);
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
	public boolean createEnclosingTransaction() {
		return false;
	}

	// ----- private methods -----
	private void doIndexing(final Indexable file) {

		boolean parsingSuccessful         = false;
		InputStream inputStream           = null;
		String fileName                   = "unknown file";

		try {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				inputStream = file.getInputStream();
				fileName = file.getName();

				tx.success();
			}

			if (inputStream != null) {

				try (final FulltextTokenizer tokenizer = new FulltextTokenizer(fileName)) {

					try (final InputStream is = inputStream) {

						Detector detector             = new DefaultDetector(MimeTypes.getDefaultMimeTypes());
						final AutoDetectParser parser = new AutoDetectParser(detector);
						final Metadata metadata       = new Metadata();

						parser.parse(is, new BodyContentHandler(tokenizer), metadata);
						parsingSuccessful = true;

						logger.debug(String.join(", ", metadata.names()));
					}

					// only do indexing when parsing was successful
					if (parsingSuccessful) {

						try (Tx tx = StructrApp.getInstance().tx()) {

							// don't modify access time when indexing is finished
							file.getSecurityContext().disableModificationOfAccessTime();

							// save raw extracted text
							file.setProperty(Indexable.extractedContent, trimToLength(tokenizer.getRawText(), maxStringLength));

							// tokenize name
							tokenizer.write(getName());

							// tokenize owner name
							final Principal _owner = file.getProperty(owner);
							if (_owner != null) {

								final String ownerName = _owner.getName();
								if (ownerName != null) {

									tokenizer.write(ownerName);
								}

								final String eMail = _owner.getProperty(Person.eMail);
								if (eMail != null) {

									tokenizer.write(eMail);
								}

								final String twitterName = _owner.getProperty(Person.twitterName);
								if (twitterName != null) {

									tokenizer.write(twitterName);
								}
							}

							tx.success();
						}

						// index document excluding stop words
						final Set<String> stopWords             = languageStopwordMap.get(tokenizer.getLanguage());
						final Iterator<String> wordIterator     = tokenizer.getWords().iterator();
						final Map<String, Integer> indexedWords = new LinkedHashMap<>();

						while (wordIterator.hasNext()) {

							try (Tx tx = StructrApp.getInstance().tx()) {

								while (wordIterator.hasNext()) {

									// strip double quotes
									final String word = StringUtils.strip(wordIterator.next(), "\"");
									if (!stopWords.contains(word)) {

										add(indexedWords, word);
									}
								}

								tx.success();
							}
						}

						// store indexed words separately
						try (Tx tx = StructrApp.getInstance().tx()) {

							// don't modify access time when indexing is finished
							file.getSecurityContext().disableModificationOfAccessTime();

							// store indexed words
							file.setProperty(Indexable.indexedWords, getFrequencySortedTopWords(indexedWords, maxTopWords));

							tx.success();
						}

						logger.debug("Indexing of {} finished, {} words extracted", new Object[] { fileName, tokenizer.getWordCount() } );
					}
				}
			}

		} catch (final Throwable t) {

			logger.warn("Indexing of {} failed", fileName, t.getMessage());
		}
	}

	private void add(final Map<String, Integer> frequencyMap, final String word) {

		Integer count = frequencyMap.get(word);
		if (count == null) {

			frequencyMap.put(word, 1);

		} else {

			frequencyMap.put(word, count + 1);
		}
	}

	private String[] getFrequencySortedTopWords(final Map<String, Integer> frequency, int maxWords) {

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

				if (resultList.size() == maxWords) {
					break;
				}
			}
		}

		return resultList.toArray(new String[0]);
	}

	private String trimToLength(final String source, final int maxLength) {

		final Charset utf = Charset.forName("utf-8");
		final byte[] data = source.getBytes(utf);

		if (data.length > maxLength) {

			// this might corrupt the last character of the string if
			// it is a multi-byte value..
			return new String(Arrays.copyOfRange(data, 0, maxLength), utf);
		}

		return source;
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
