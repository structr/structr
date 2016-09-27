/**
 * Copyright (C) 2010-2016 Structr GmbH
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.IOUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.structr.agent.Agent;
import org.structr.agent.ReturnValue;
import org.structr.agent.Task;
import org.structr.api.graph.Node;
import org.structr.api.index.Index;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import static org.structr.core.graph.NodeInterface.owner;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.Tx;
import org.structr.common.fulltext.Indexable;
import org.structr.core.entity.Person;

/**
 *
 *
 */
public class FulltextIndexingAgent extends Agent<Indexable> {

	private static final Logger logger = Logger.getLogger(FulltextIndexingAgent.class.getName());
	private static final Map<String, Set<String>> languageStopwordMap = new LinkedHashMap<>();
	public static final String TASK_NAME                              = "FulltextIndexing";

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

				final FulltextTokenizer tokenizer = new FulltextTokenizer(fileName);

				try (final InputStream is = inputStream) {

					Detector detector = new DefaultDetector(MimeTypes.getDefaultMimeTypes());
					final AutoDetectParser parser = new AutoDetectParser(detector);
					
					final Map<MediaType, Parser> customParsers = new HashMap<>();
					customParsers.put(MediaType.application("pdf"), new PDFParser());
					parser.setParsers(customParsers);

					parser.parse(is, new BodyContentHandler(tokenizer), new Metadata());
					parsingSuccessful = true;
				}

				// only do indexing when parsing was successful
				if (parsingSuccessful) {

					try (Tx tx = StructrApp.getInstance().tx()) {

						// don't modify access time when indexing is finished
						file.getSecurityContext().preventModificationOfAccessTime();

						// save raw extracted text
						file.setProperty(Indexable.extractedContent, tokenizer.getRawText());

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
					final NodeService nodeService       = Services.getInstance().getService(NodeService.class);
					final Index<Node> fulltextIndex     = nodeService.getNodeIndex();
					final Set<String> stopWords         = languageStopwordMap.get(tokenizer.getLanguage());
					final String indexKeyName           = Indexable.indexedWords.jsonName();
					final Iterator<String> wordIterator = tokenizer.getWords().iterator();
					final Node node                     = file.getNode();
					final Set<String> indexedWords      = new TreeSet<>();

					logger.log(Level.INFO, "Indexing {0}..", fileName);

					while (wordIterator.hasNext()) {

						try (Tx tx = StructrApp.getInstance().tx()) {

							// remove node from index (in case of previous indexing runs)
							fulltextIndex.remove(node, indexKeyName);

							while (wordIterator.hasNext()) {

								// strip double quotes
								final String word = StringUtils.strip(wordIterator.next(), "\"");

								if (!stopWords.contains(word)) {

									indexedWords.add(word);
									fulltextIndex.add(node, indexKeyName, word, String.class);

//									if (indexedWords > 1000) {
//										indexedWords = 0;
//										break;
//									}
								}
							}

							tx.success();
						}
					}

					// store indexed words separately
					try (Tx tx = StructrApp.getInstance().tx()) {

						// don't modify access time when indexing is finished
						file.getSecurityContext().preventModificationOfAccessTime();

						// store indexed words
						file.setProperty(Indexable.indexedWords, (String[]) indexedWords.toArray(new String[indexedWords.size()]));

						tx.success();
					}

					logger.log(Level.INFO, "Indexing of {0} finished, {1} words extracted", new Object[] { fileName, tokenizer.getWordCount() } );

				}
			}

		} catch (final Throwable t) {

			logger.log(Level.WARNING, "Indexing of {0} failed: {1}", new Object[] { fileName, t.getMessage() } );
			t.printStackTrace();
		}
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

			logger.log(Level.WARNING, "", ioex);
		}
	}
}
