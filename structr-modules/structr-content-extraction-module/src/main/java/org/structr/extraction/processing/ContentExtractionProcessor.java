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
package org.structr.extraction.processing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.extraction.ContentExtractionSettings;
import org.structr.extraction.ContentExtractionTraits;
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.api.ContentExtractionException;
import org.structr.extraction.api.ContentExtractionListener;
import org.structr.extraction.api.chunk.Chunk;
import org.structr.extraction.api.document.ExtractedDocument;
import org.structr.extraction.api.options.EffectiveContentOptions;
import org.structr.extraction.api.processing.ExtractionStatus;
import org.structr.extraction.api.processing.ProcessingResult;
import org.structr.extraction.entity.ExtractedContentNode;
import org.structr.extraction.options.ContentOptionsResolver;
import org.structr.extraction.traits.definitions.ExtractedContentNodeTraitDefinition;
import org.structr.extraction.traits.properties.ContentFileStateProperties;
import org.structr.web.entity.File;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Extracts, splits and stores the content of one file.
 *
 * Extraction can take minutes (OCR), so it does not run inside a transaction: a first short transaction
 * copies the file content to a temporary file and marks the file as running, the extraction works on
 * the copy, and a second transaction replaces the chunks. All of it runs as superuser, because the
 * chunks are system data whose visibility follows the file.
 */
public final class ContentExtractionProcessor {

	private static final Logger logger = LoggerFactory.getLogger(ContentExtractionProcessor.class);

	private ContentExtractionProcessor() {
	}

	/**
	 * Processes one file.
	 *
	 * @param fileUuid  the file
	 * @param overrides per-call options (see {@link ContentOptionsResolver#OPTIONS})
	 * @param force     whether to process the file even if content and options are unchanged
	 */
	public static ProcessingResult process(final String fileUuid, final Map<String, Object> overrides, final boolean force) {

		final SecurityContext superUser = SecurityContext.getSuperUserInstance();
		final App app                   = StructrApp.getInstance(superUser);

		final Prepared prepared;

		try {

			prepared = prepare(app, fileUuid, overrides, force);

		} catch (final FrameworkException | IOException | IllegalArgumentException ex) {

			markFailed(app, fileUuid, ex.getMessage());

			return ProcessingResult.failed(fileUuid, ex.getMessage());
		}

		if (prepared.result() != null) {

			return prepared.result();
		}

		try {

			// Tika reads the copy in place, instead of spooling a stream of it into another temporary file
			final ExtractedDocument document = ContentExtraction.extract(prepared.copy(), prepared.fileName(), prepared.options().extraction());
			final List<Chunk> chunks = ContentExtraction.split(document, prepared.options().split());

			return store(app, fileUuid, prepared, document, chunks);

		} catch (final ContentExtractionException | IllegalArgumentException | FrameworkException ex) {

			logger.warn("Content extraction of file {} failed: {}", fileUuid, ex.getMessage());
			markFailed(app, fileUuid, ex.getMessage());

			return ProcessingResult.failed(fileUuid, ex.getMessage());

		} finally {

			deleteQuietly(prepared.copy());
		}
	}

	/**
	 * Deletes the chunks of a file and resets its extraction state.
	 */
	public static void deleteContent(final File file) throws FrameworkException {

		final App app = StructrApp.getInstance(SecurityContext.getSuperUserInstance());

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.getNodeById(StructrTraits.FILE, file.getUuid());
			if (node != null) {

				final File current = node.as(File.class);

				replaceChunks(app, current, List.of(), null);

				final PropertyMap properties = new PropertyMap();
				final Traits traits          = Traits.of(StructrTraits.FILE);

				properties.put(traits.key(ContentFileStateProperties.STATUS), null);
				properties.put(traits.key(ContentFileStateProperties.ERROR), null);
				properties.put(traits.key(ContentFileStateProperties.DATE), null);
				properties.put(traits.key(ContentFileStateProperties.CHECKSUM), null);
				properties.put(traits.key(ContentFileStateProperties.OPTIONS_HASH), null);
				properties.put(traits.key(ContentFileStateProperties.OCR), null);

				current.setProperties(SecurityContext.getSuperUserInstance(), properties);
			}

			tx.success();
		}
	}

	/**
	 * Tells the listeners about new content; after the commit, outside of the transaction if possible.
	 */
	public static void notifyExtracted(final ProcessingResult result) {

		if (result.status() != ExtractionStatus.DONE) {

			return;
		}

		for (final ContentExtractionListener listener : ContentExtraction.getListeners()) {

			try {

				listener.onContentExtracted(result.fileUuid(), result.nodeUuids(), result.options());

			} catch (final RuntimeException ex) {

				logger.warn("Content extraction listener {} failed for file {}", listener.getClass().getName(), result.fileUuid(), ex);
			}
		}
	}

	// ----- private methods -----
	private static Prepared prepare(final App app, final String fileUuid, final Map<String, Object> overrides, final boolean force) throws FrameworkException, IOException {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.getNodeById(StructrTraits.FILE, fileUuid);
			if (node == null) {

				tx.success();

				return new Prepared(ProcessingResult.skipped(fileUuid, "The file no longer exists."), null, null, null, null);
			}

			final File file                        = node.as(File.class);
			final Traits traits                    = Traits.of(StructrTraits.FILE);
			final EffectiveContentOptions options  = ContentOptionsResolver.resolve(file, overrides);
			final Long checksum                    = file.getChecksum();
			final Long size                        = file.getSize();
			final long maxBytes                    = ContentExtractionSettings.MaxFileSize.getValue() * 1024L * 1024L;
			final String status                    = file.getProperty(traits.key(ContentFileStateProperties.STATUS));
			final Long previousChecksum            = file.getProperty(traits.key(ContentFileStateProperties.CHECKSUM));
			final String previousHash              = file.getProperty(traits.key(ContentFileStateProperties.OPTIONS_HASH));

			// an empty file has nothing to extract (Tika rejects it); chunks of earlier content are stale
			if (size == null || size == 0L) {

				replaceChunks(app, file, List.of(), null);
				setStatus(file, ExtractionStatus.SKIPPED, "The file is empty.");
				tx.success();

				return new Prepared(ProcessingResult.skipped(fileUuid, "empty"), null, null, null, null);
			}

			if (size > maxBytes) {

				setStatus(file, ExtractionStatus.SKIPPED, "The file is larger than the extraction limit of " + ContentExtractionSettings.MaxFileSize.getValue() + " MB.");
				tx.success();

				return new Prepared(ProcessingResult.skipped(fileUuid, "too large"), null, null, null, null);
			}

			final boolean unchanged = checksum != null && Objects.equals(checksum, previousChecksum) && options.hash().equals(previousHash) && ExtractionStatus.DONE.getValue().equals(status);
			if (unchanged && !force) {

				tx.success();

				return new Prepared(ProcessingResult.unchanged(fileUuid), null, null, null, null);
			}

			final Path copy = Files.createTempFile("structr-content-extraction-", ".tmp");

			try (final InputStream input = file.getInputStream()) {

				if (input == null) {

					throw new IOException("File " + file.getName() + " has no content.");
				}

				Files.copy(input, copy, StandardCopyOption.REPLACE_EXISTING);

			} catch (final IOException ex) {

				deleteQuietly(copy);
				throw ex;
			}

			setStatus(file, ExtractionStatus.RUNNING, null);
			tx.success();

			return new Prepared(null, copy, file.getName(), checksum, options);
		}
	}

	private static ProcessingResult store(final App app, final String fileUuid, final Prepared prepared, final ExtractedDocument document, final List<Chunk> chunks) throws FrameworkException {

		final List<String> nodeUuids;

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.getNodeById(StructrTraits.FILE, fileUuid);
			if (node == null) {

				tx.success();

				return ProcessingResult.skipped(fileUuid, "The file was deleted during extraction.");
			}

			final File file              = node.as(File.class);
			final PropertyMap properties = new PropertyMap();
			final Traits traits          = Traits.of(StructrTraits.FILE);
			final List<String> warnings  = new ArrayList<>(prepared.options().warnings());

			nodeUuids = replaceChunks(app, file, chunks, document.getMetadata().language());

			if (document.getMetadata().truncated()) {

				warnings.add("The text was cut at " + prepared.options().extraction().getMaxCharacters() + " characters.");
			}

			properties.put(traits.key(ContentFileStateProperties.STATUS), ExtractionStatus.DONE.getValue());
			properties.put(traits.key(ContentFileStateProperties.ERROR), warnings.isEmpty() ? null : String.join(" ", warnings));
			properties.put(traits.key(ContentFileStateProperties.DATE), new Date());
			properties.put(traits.key(ContentFileStateProperties.CHECKSUM), prepared.checksum());
			properties.put(traits.key(ContentFileStateProperties.OPTIONS_HASH), prepared.options().hash());
			properties.put(traits.key(ContentFileStateProperties.OCR), document.getMetadata().ocrApplied());

			file.setProperties(SecurityContext.getSuperUserInstance(), properties);

			tx.success();
		}

		return new ProcessingResult(fileUuid, ExtractionStatus.DONE, nodeUuids, prepared.options(), null);
	}

	/**
	 * Deletes the chunks of a file and creates the new ones, chained in reading order.
	 *
	 * @return the ids of the new chunks
	 */
	private static List<String> replaceChunks(final App app, final File file, final List<Chunk> chunks, final String language) throws FrameworkException {

		final Traits fileTraits                       = Traits.of(StructrTraits.FILE);
		final Traits chunkTraits                      = Traits.of(ContentExtractionTraits.EXTRACTED_CONTENT_NODE);
		final List<ExtractedContentNode> oldNodes     = new ArrayList<>();
		final Iterable<NodeInterface> existing        = file.getProperty(fileTraits.key(ContentFileStateProperties.EXTRACTED_CONTENT_NODES));

		for (final NodeInterface node : existing) {

			oldNodes.add(node.as(ExtractedContentNode.class));
		}

		for (final ContentExtractionListener listener : ContentExtraction.getListeners()) {

			try {

				listener.onBeforeContentReplaced(file, oldNodes, chunks);

			} catch (final RuntimeException ex) {

				logger.warn("Content extraction listener {} failed for file {}", listener.getClass().getName(), file.getUuid(), ex);
			}
		}

		final Map<String, ExtractedContentNode> embedded = embeddedNodesByHash(oldNodes);

		for (final ExtractedContentNode old : oldNodes) {

			app.delete(old);
		}

		final List<String> uuids = new ArrayList<>();
		NodeInterface previous   = null;

		for (final Chunk chunk : chunks) {

			final PropertyMap properties = new PropertyMap();

			properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.FILE_PROPERTY), file);
			properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.PREVIOUS_PROPERTY), previous);
			properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.TEXT_PROPERTY), chunk.text());
			properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.POSITION_PROPERTY), chunk.position());
			properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.START_OFFSET_PROPERTY), chunk.startOffset());
			properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.END_OFFSET_PROPERTY), chunk.endOffset());
			properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.CHAR_COUNT_PROPERTY), chunk.charCount());
			properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.TOKEN_COUNT_PROPERTY), chunk.tokenCount());
			properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.PAGE_START_PROPERTY), chunk.pageStart());
			properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.PAGE_END_PROPERTY), chunk.pageEnd());
			properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.HEADING_PATH_PROPERTY), chunk.headingPath().toArray(String[]::new));
			properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.LANGUAGE_PROPERTY), language);
			properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.CONTENT_HASH_PROPERTY), chunk.contentHash());

			// an unchanged chunk keeps the embedding of its old node, so only changed chunks need a new one
			final ExtractedContentNode unchanged = embedded.remove(chunk.contentHash());
			if (unchanged != null) {

				properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.EMBEDDING_PROPERTY), unchanged.getProperty(chunkTraits.key(ExtractedContentNodeTraitDefinition.EMBEDDING_PROPERTY)));
				properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.EMBEDDING_MODEL_PROPERTY), unchanged.getEmbeddingModel());
			}

			final NodeInterface node = app.create(ContentExtractionTraits.EXTRACTED_CONTENT_NODE, properties);

			uuids.add(node.getUuid());
			previous = node;
		}

		return uuids;
	}

	/**
	 * The old chunks that have an embedding, by content hash. A hash that occurs more than once maps to
	 * its first chunk; each entry is reused by at most one new chunk.
	 */
	private static Map<String, ExtractedContentNode> embeddedNodesByHash(final List<ExtractedContentNode> oldNodes) {

		final Map<String, ExtractedContentNode> embedded = new HashMap<>();

		for (final ExtractedContentNode old : oldNodes) {

			final String hash = old.getContentHash();
			if (hash != null && old.getEmbeddingModel() != null && old.getEmbedding() != null) {

				embedded.putIfAbsent(hash, old);
			}
		}

		return embedded;
	}

	private static void setStatus(final File file, final ExtractionStatus status, final String error) throws FrameworkException {

		final PropertyMap properties = new PropertyMap();
		final Traits traits          = Traits.of(StructrTraits.FILE);

		properties.put(traits.key(ContentFileStateProperties.STATUS), status.getValue());
		properties.put(traits.key(ContentFileStateProperties.ERROR), error);

		file.setProperties(SecurityContext.getSuperUserInstance(), properties);
	}

	private static void markFailed(final App app, final String fileUuid, final String message) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.getNodeById(StructrTraits.FILE, fileUuid);
			if (node != null) {

				setStatus(node.as(File.class), ExtractionStatus.FAILED, message);
			}

			tx.success();

		} catch (final FrameworkException ex) {

			logger.warn("Unable to record the failed content extraction of file {}: {}", fileUuid, ex.getMessage());
		}
	}

	private static void deleteQuietly(final Path path) {

		if (path != null) {

			try {

				Files.deleteIfExists(path);

			} catch (final IOException ex) {

				logger.warn("Unable to delete temporary file {}: {}", path, ex.getMessage());
			}
		}
	}

	/**
	 * State between the first transaction and the extraction: either a final result (skipped,
	 * unchanged), or the copy to extract and what is needed to store the result.
	 */
	private record Prepared(ProcessingResult result, Path copy, String fileName, Long checksum, EffectiveContentOptions options) {
	}
}
