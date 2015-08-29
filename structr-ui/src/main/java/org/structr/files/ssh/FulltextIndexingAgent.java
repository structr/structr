package org.structr.files.ssh;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.tika.io.IOUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.structr.agent.Agent;
import org.structr.agent.ReturnValue;
import org.structr.agent.Task;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import static org.structr.core.graph.NodeInterface.owner;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.Tx;
import org.structr.files.text.FulltextTokenizer;
import org.structr.web.entity.FileBase;
import static org.structr.web.entity.FileBase.extractedContent;
import org.structr.web.entity.User;

/**
 *
 * @author Christian Morgner
 */
public class FulltextIndexingAgent extends Agent<FileBase> {

	private static final Map<String, Set<String>> languageStopwordMap = new LinkedHashMap<>();
	public static final String TASK_NAME                              = "FulltextIndexing";

	@Override
	public ReturnValue processTask(final Task<FileBase> task) throws Throwable {

		if (TASK_NAME.equals(task.getType())) {

			for (final FileBase file : task.getNodes()) {

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
	private void doIndexing(final FileBase file) {

		boolean parsingSuccessful         = false;
		InputStream inputStream           = null;

		try (final Tx tx = StructrApp.getInstance().tx()) {

			System.out.println("Extracting content from " + file.getName() + "...");
			inputStream = file.getInputStream();

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		if (inputStream != null) {

			final FulltextTokenizer tokenizer = new FulltextTokenizer();

			try (final InputStream is = inputStream) {

				new AutoDetectParser().parse(is, new BodyContentHandler(tokenizer), new Metadata());
				parsingSuccessful = true;

			} catch (Throwable t) {
				t.printStackTrace();
			}

			// only do indexing when parsing was successful
			if (parsingSuccessful) {

				try (final Tx tx = StructrApp.getInstance().tx()) {

					System.out.println("Indexing " + file.getName() + "...");

					final NodeService nodeService     = Services.getInstance().getService(NodeService.class);
					final Index<Node> fulltextIndex   = nodeService.getNodeIndex(NodeService.NodeIndex.fulltext);
					final String indexKeyName         = FileBase.indexedContent.jsonName();
					final Node node                   = file.getNode();

					// save raw extracted text
					file.setProperty(extractedContent, tokenizer.getRawText());

					// tokenize name
					tokenizer.write(getName());

					// tokenize owner name
					final Principal _owner = file.getProperty(owner);
					if (_owner != null) {

						final String ownerName = _owner.getName();
						if (ownerName != null) {

							tokenizer.write(ownerName);
						}

						final String eMail = _owner.getProperty(User.eMail);
						if (eMail != null) {

							tokenizer.write(eMail);
						}

						final String twitterName = _owner.getProperty(User.twitterName);
						if (twitterName != null) {

							tokenizer.write(twitterName);
						}
					}

					// remove node from index (in case of previous indexing runs)
					fulltextIndex.remove(node, indexKeyName);

					// index document excluding stop words
					final Set<String> stopWords = languageStopwordMap.get(tokenizer.getLanguage());
					final StringBuilder buf     = new StringBuilder();

					for (final String word : tokenizer.getWords()) {

						if (!stopWords.contains(word)) {

							buf.append(word + " ");
							fulltextIndex.add(node, indexKeyName, word);
						}
					}

					file.setProperty(FileBase.indexedWords, buf.toString());

					tx.success();

				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
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

			ioex.printStackTrace();
		}
	}
}
