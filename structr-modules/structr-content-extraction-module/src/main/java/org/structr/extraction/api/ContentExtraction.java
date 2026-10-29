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
package org.structr.extraction.api;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.scheduler.JobQueueManager;
import org.structr.extraction.ContentExtractionSettings;
import org.structr.extraction.ContentExtractionTraits;
import org.structr.extraction.api.chunk.Chunk;
import org.structr.extraction.api.chunk.ChunkContext;
import org.structr.extraction.api.document.DetectedLanguage;
import org.structr.extraction.api.document.DocumentMetadata;
import org.structr.extraction.api.document.EmbeddedDocument;
import org.structr.extraction.api.document.ExtractedDocument;
import org.structr.extraction.api.document.ExtractedFigureData;
import org.structr.extraction.api.document.ExtractedLink;
import org.structr.extraction.api.document.ExtractedTable;
import org.structr.extraction.api.document.FigureContext;
import org.structr.extraction.api.document.OcrInfo;
import org.structr.extraction.api.options.EffectiveContentOptions;
import org.structr.extraction.api.options.ExtractionOptions;
import org.structr.extraction.api.options.FigureOptions;
import org.structr.extraction.api.options.NormalizeOptions;
import org.structr.extraction.api.options.OcrMode;
import org.structr.extraction.api.options.SplitOptions;
import org.structr.extraction.api.processing.AutoExtractionDecision;
import org.structr.extraction.api.processing.ProcessingResult;
import org.structr.extraction.api.search.ContentSearchHit;
import org.structr.extraction.api.search.ContentSearchOptions;
import org.structr.extraction.api.search.FileSearchHit;
import org.structr.extraction.chunking.ContentChunker;
import org.structr.extraction.chunking.token.TokenCounter;
import org.structr.extraction.entity.ExtractedContentNode;
import org.structr.extraction.options.ContentOptionsResolver;
import org.structr.extraction.parser.figures.FigureExtractor;
import org.structr.extraction.parser.tika.TikaDocumentExtractor;
import org.structr.extraction.processing.ContentExtractionJob;
import org.structr.extraction.processing.ContentExtractionProcessor;
import org.structr.extraction.query.ContentChunkReader;
import org.structr.extraction.query.ContentKeywordSearch;
import org.structr.extraction.query.FigureContextReader;
import org.structr.extraction.text.StopWords;
import org.structr.extraction.text.TextLanguageDetector;
import org.structr.extraction.text.TextNormalizer;
import org.structr.web.entity.File;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Java API of the content extraction module, for other modules (e.g. the Enterprise AI module) and for
 * the module's own script functions.
 *
 * Unless their description says otherwise, the methods in this class do not write to the graph.
 */
public final class ContentExtraction {

	private static final CopyOnWriteArrayList<ContentExtractionListener> listeners = new CopyOnWriteArrayList<>();

	/** the size of the embedding vectors, provided by the module that writes them (the Enterprise AI module) */
	private static volatile Supplier<Integer> embeddingDimensions = null;

	private ContentExtraction() {
	}

	/**
	 * Extracts text, structure, links and metadata of a document.
	 *
	 * @param input    the document; closed when done
	 * @param fileName the file name, used as a hint for type detection; may be null
	 */
	public static ExtractedDocument extract(final InputStream input, final String fileName, final ExtractionOptions options) throws ContentExtractionException {

		return TikaDocumentExtractor.extract(input, fileName, options);
	}

	/**
	 * Extracts text, structure, links and metadata of a document in a local file. Tika reads the file in
	 * place, so formats that need random access (PDF, ZIP, Office) are not copied to a temporary file first.
	 *
	 * @param path     the document
	 * @param fileName the file name, used as a hint for type detection; may be null
	 */
	public static ExtractedDocument extract(final Path path, final String fileName, final ExtractionOptions options) throws ContentExtractionException {

		return TikaDocumentExtractor.extract(path, fileName, options);
	}

	/**
	 * Extracts text, structure, links and metadata of a file.
	 */
	public static ExtractedDocument extract(final File file, final ExtractionOptions options) throws ContentExtractionException {

		try (final InputStream input = open(file)) {

			return TikaDocumentExtractor.extract(input, file.getName(), options);

		} catch (final IOException ex) {

			throw new ContentExtractionException("Unable to read file " + file.getName() + ": " + ex.getMessage(), ex);
		}
	}

	/**
	 * Reads the metadata of a file without extracting its text.
	 */
	public static DocumentMetadata extractMetadata(final File file) throws ContentExtractionException {

		try (final InputStream input = open(file)) {

			return TikaDocumentExtractor.extractMetadata(input, file.getName());

		} catch (final IOException ex) {

			throw new ContentExtractionException("Unable to read file " + file.getName() + ": " + ex.getMessage(), ex);
		}
	}

	/**
	 * The tables of a file, with their cells. Tables come from the document structure, so no OCR runs.
	 */
	public static List<ExtractedTable> extractTables(final File file, final ExtractionOptions options) throws ContentExtractionException {

		return extract(file, copyWithoutOcr(options)).getTables();
	}

	/**
	 * The hyperlinks of a file, with their anchor text. Links come from the document structure, so no OCR runs.
	 */
	public static List<ExtractedLink> extractLinks(final File file, final ExtractionOptions options) throws ContentExtractionException {

		return extract(file, copyWithoutOcr(options)).getLinks();
	}

	/**
	 * The documents embedded in a file: mail attachments, archive entries, embedded office documents.
	 *
	 * @param includeText whether to extract the text of each embedded document
	 */
	public static List<EmbeddedDocument> extractEmbedded(final File file, final ExtractionOptions options, final boolean includeText) throws ContentExtractionException {

		try (final InputStream input = open(file)) {

			return TikaDocumentExtractor.extractEmbedded(input, file.getName(), options, includeText);

		} catch (final IOException ex) {

			throw new ContentExtractionException("Unable to read file " + file.getName() + ": " + ex.getMessage(), ex);
		}
	}

	/**
	 * The figures of a file (charts, diagrams, tables with a caption, pictures) with their rendered images,
	 * in reading order. Nothing is stored.
	 */
	public static List<ExtractedFigureData> extractFigures(final File file, final FigureOptions options) throws ContentExtractionException {

		Path copy = null;

		try (final InputStream input = open(file)) {

			// PDFBox and Tika need random access, so the content is read from a local copy
			copy = Files.createTempFile("structr-content-figures-", ".tmp");

			Files.copy(input, copy, StandardCopyOption.REPLACE_EXISTING);

			return FigureExtractor.extract(copy, file.getName(), options);

		} catch (final IOException ex) {

			throw new ContentExtractionException("Unable to read file " + file.getName() + ": " + ex.getMessage(), ex);

		} finally {

			deleteQuietly(copy);
		}
	}

	/**
	 * The figures of a document in a local file, in reading order. Nothing is stored.
	 *
	 * @param path     the document
	 * @param fileName the file name, used as a hint for type detection; may be null
	 */
	public static List<ExtractedFigureData> extractFigures(final Path path, final String fileName, final FigureOptions options) throws ContentExtractionException {

		return FigureExtractor.extract(path, fileName, options);
	}

	/**
	 * Splits an extracted document into chunks. Chunk offsets refer to the document's plain text.
	 *
	 * @throws IllegalArgumentException if the options are invalid, e.g. an overlap not smaller than the chunk size
	 */
	public static List<Chunk> split(final ExtractedDocument document, final SplitOptions options) {

		return ContentChunker.split(document, options);
	}

	/**
	 * Splits a text into chunks. Chunk offsets refer to the text; the structure splitter reads the text
	 * as Markdown, and its offsets refer to the resulting plain text instead.
	 *
	 * @throws IllegalArgumentException if the options are invalid
	 */
	public static List<Chunk> split(final String text, final SplitOptions options) {

		return ContentChunker.split(text, options);
	}

	/**
	 * Number of tokens of a text in an encoding such as "cl100k_base" or "o200k_base".
	 *
	 * @throws IllegalArgumentException if the encoding is unknown
	 */
	public static int countTokens(final String text, final String encoding) {

		return TokenCounter.forEncoding(encoding).count(text);
	}

	/**
	 * Cuts a text to at most the given number of tokens, after a sentence or word where possible.
	 *
	 * @throws IllegalArgumentException if the encoding is unknown
	 */
	public static String truncateTokens(final String text, final int maxTokens, final String encoding) {

		return TokenCounter.forEncoding(encoding).truncate(text, maxTokens);
	}

	/**
	 * Cleans up text: Unicode normalization, control characters, hyphenation at line breaks, whitespace.
	 */
	public static String normalize(final String text, final NormalizeOptions options) {

		return TextNormalizer.normalize(text, options);
	}

	/**
	 * Detects the language of a text.
	 */
	public static DetectedLanguage detectLanguage(final String text) {

		return TextLanguageDetector.detect(text);
	}

	/**
	 * The stop words of a language, or an empty set for a language without a list.
	 *
	 * @param language a two-letter language code, e.g. "en"
	 */
	public static Set<String> stopWords(final String language) {

		return StopWords.forLanguage(language);
	}

	/**
	 * Whether Tesseract OCR is available, and with which languages.
	 */
	public static OcrInfo getOcrInfo() throws ContentExtractionException {

		return TikaDocumentExtractor.getOcrInfo();
	}

	// ----- stored content -----
	/**
	 * Whether a file is extracted automatically, and which setting decided it.
	 */
	public static AutoExtractionDecision shouldExtract(final File file) {

		return ContentOptionsResolver.shouldExtract(file);
	}

	/**
	 * The options a file is extracted and split with, each with its source.
	 *
	 * @param overrides per-call options by name, as accepted by processContent(); may be empty
	 * @throws IllegalArgumentException if an override is unknown or invalid
	 */
	public static EffectiveContentOptions resolveOptions(final File file, final Map<String, Object> overrides) {

		return ContentOptionsResolver.resolve(file, overrides);
	}

	/**
	 * Extracts files in a background job, in the given job queue.
	 *
	 * @param user      the user the job is listed under, or null
	 * @param fileUuids the files
	 * @param overrides per-call options by name, as accepted by processContent(); may be empty
	 * @param force     whether to extract files whose content and options are unchanged
	 * @param queue     the job queue; null for the setting contentextraction.extraction.queue
	 * @return the job id
	 */
	public static Long schedule(final Principal user, final List<String> fileUuids, final Map<String, Object> overrides, final boolean force, final String queue) throws FrameworkException {

		final ContentExtractionJob job = new ContentExtractionJob(user, fileUuids, overrides, force, queue != null ? queue : ContentExtractionSettings.ExtractionQueue.getValue());

		JobQueueManager.getInstance().addJob(job);

		return job.jobId();
	}

	/**
	 * Extracts a file in the calling thread. Listeners are notified after the surrounding transaction
	 * commits, or right away if there is none.
	 */
	public static ProcessingResult processNow(final File file, final Map<String, Object> overrides, final boolean force) {

		final ProcessingResult result = ContentExtractionProcessor.process(file.getUuid(), overrides, force);

		if (TransactionCommand.inTransaction()) {

			TransactionCommand.queuePostCommitProcedure(() -> ContentExtractionProcessor.notifyExtracted(result));

		} else {

			ContentExtractionProcessor.notifyExtracted(result);
		}

		return result;
	}

	/**
	 * The stored chunks of a file, in reading order.
	 */
	public static List<ExtractedContentNode> getExtractedContent(final File file) {

		return ContentChunkReader.chunks(file);
	}

	/**
	 * All stored chunks of a file: the chunks of the text in reading order, then the chunks with the text
	 * of its figures. For a module that processes every chunk, e.g. to embed it.
	 */
	public static List<ExtractedContentNode> getAllContent(final File file) {

		return ContentChunkReader.allChunks(file);
	}

	/**
	 * The stored text of a file, reassembled from its chunks with overlapping parts included once.
	 */
	public static String getExtractedText(final File file) {

		return ContentChunkReader.mergedText(ContentChunkReader.chunks(file));
	}

	/**
	 * A chunk with up to {@code before} chunks before and {@code after} chunks after it, as one text.
	 */
	public static ChunkContext getContext(final ExtractedContentNode node, final int before, final int after) {

		return ContentChunkReader.context(node, before, after);
	}

	/**
	 * What a document says about one of its figures, for a prompt that interprets the figure in its
	 * context: the caption and the sentences that refer to the figure, the text of its page, and other
	 * passages with the words of its caption, assembled into one text within a token budget. Uses the
	 * stored content of the document, so it must have been extracted. Must run in a transaction.
	 *
	 * @param label     the figure's label, e.g. "Figure 3"; may be null
	 * @param caption   the figure's caption; may be null
	 * @param page      the page the figure is on; may be null
	 * @param mentions  the sentences of the document that refer to the figure
	 * @param maxTokens the token budget of the assembled text
	 * @param passages  how many other passages to find; 0 for none
	 */
	public static FigureContext getFigureContext(final SecurityContext securityContext, final File document, final String label, final String caption, final Integer page, final List<String> mentions,
		final int maxTokens, final int passages) throws FrameworkException {

		return FigureContextReader.read(securityContext, document, label, caption, page, mentions, maxTokens, passages);
	}

	/**
	 * Deletes the stored chunks of a file, those with the text of its figures included, and resets its
	 * extraction state. The figures themselves are kept.
	 */
	public static void deleteExtractedContent(final File file) throws FrameworkException {

		ContentExtractionProcessor.deleteContent(file);
	}

	/**
	 * Keyword search over stored chunks (Lucene query syntax). Only chunks of files the security context
	 * can read are found.
	 */
	public static List<ContentSearchHit> search(final SecurityContext securityContext, final String query, final ContentSearchOptions options) throws FrameworkException {

		return ContentKeywordSearch.search(securityContext, query, options);
	}

	/**
	 * Searches files by their extracted content, as a search box does: every word of the input must
	 * occur, as a prefix or with small typos, ignoring case. Hits are grouped by file, best file first,
	 * and come with the matching places in context. Only files the security context may read are found.
	 *
	 * @param options      limit (number of files), minScore, fileUuids and folder are used
	 * @param maxSnippets  at most this many snippets per file
	 * @param contextChars at most this many characters of context on each side of a match
	 */
	public static List<FileSearchHit> searchFiles(final SecurityContext securityContext, final String input, final ContentSearchOptions options, final int maxSnippets, final int contextChars) throws FrameworkException {

		return ContentKeywordSearch.searchFiles(securityContext, input, options, maxSnippets, contextChars);
	}

	/**
	 * The search hit for a chunk found by other means, e.g. by a vector index, or null if the security
	 * context may not read it or the options filter it out. Uses minScore, fileUuids, folder and
	 * contextChunks of the options, so other searches filter exactly like the keyword search.
	 */
	public static ContentSearchHit toSearchHit(final SecurityContext securityContext, final NodeInterface node, final double score, final ContentSearchOptions options) {

		return ContentKeywordSearch.searchHit(securityContext, node, score, options);
	}

	// ----- embeddings -----
	/**
	 * Sets where the size of the embedding vectors comes from. ExtractedContentNode.embedding is vector-indexed
	 * with this size; null or less than 1 means no index. Set by the module that writes the embeddings.
	 */
	public static void setEmbeddingDimensions(final Supplier<Integer> dimensions) {

		embeddingDimensions = dimensions;
	}

	/**
	 * The size of the vector index of ExtractedContentNode.embedding, asked for by Structr's index management.
	 *
	 * Without a module that writes embeddings (e.g. after a change from Structr Enterprise to the Community
	 * Edition) this is the size of the index that exists, so the index and the stored vectors are kept as
	 * they are, ready for a return to Enterprise.
	 */
	public static Integer embeddingDimensions() {

		final Supplier<Integer> supplier = embeddingDimensions;
		if (supplier != null) {

			return supplier.get();
		}

		final App app = StructrApp.getInstance(SecurityContext.getSuperUserInstance());

		try (final Tx tx = app.tx()) {

			final Object dimensions = app.getDatabaseService().getIndexInfo(ContentExtractionTraits.EMBEDDING_VECTOR_INDEX).get("dimensions");

			tx.success();

			return dimensions instanceof Number number ? number.intValue() : null;

		} catch (FrameworkException | RuntimeException ex) {

			// a database without index information (e.g. the in-memory database) has no vector index to keep
			return null;
		}
	}

	// ----- listeners -----
	public static void addListener(final ContentExtractionListener listener) {

		listeners.addIfAbsent(listener);
	}

	public static void removeListener(final ContentExtractionListener listener) {

		listeners.remove(listener);
	}

	public static List<ContentExtractionListener> getListeners() {

		return List.copyOf(listeners);
	}

	// ----- private methods -----
	private static InputStream open(final File file) throws ContentExtractionException {

		final Long size      = file.getSize();
		final long maxSizeMB = ContentExtractionSettings.MaxFileSize.getValue();

		if (size != null && size > maxSizeMB * 1024 * 1024) {

			throw new ContentExtractionException("File " + file.getName() + " is larger than the extraction limit of " + maxSizeMB + " MB (contentextraction.extraction.maxfilesize).");
		}

		// the stored content: getInputStream would run the scripts of a template file and return its output
		final InputStream input = file.getRawInputStream();
		if (input == null) {

			throw new ContentExtractionException("File " + file.getName() + " has no content.");
		}

		return input;
	}

	private static void deleteQuietly(final Path path) {

		if (path != null) {

			try {

				Files.deleteIfExists(path);

			} catch (final IOException ex) {

				// a temporary file the system cleans up eventually
			}
		}
	}

	private static ExtractionOptions copyWithoutOcr(final ExtractionOptions options) {

		return new ExtractionOptions()
			.setOcrMode(OcrMode.OFF)
			.setMaxCharacters(options.getMaxCharacters())
			.setIncludeEmbedded(options.isIncludeEmbedded())
			.setMaxEmbeddedDepth(options.getMaxEmbeddedDepth());
	}
}
