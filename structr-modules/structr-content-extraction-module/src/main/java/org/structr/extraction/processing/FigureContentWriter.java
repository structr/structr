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
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.extraction.ContentExtractionSettings;
import org.structr.extraction.ContentExtractionTraits;
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.api.ContentExtractionListener;
import org.structr.extraction.chunking.token.TokenCounter;
import org.structr.extraction.entity.ExtractedContentNode;
import org.structr.extraction.traits.definitions.DocumentFigureTraitDefinition;
import org.structr.extraction.traits.definitions.ExtractedContentNodeTraitDefinition;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Writes the chunk with the text of a DocumentFigure: one ExtractedContentNode of the figure's document,
 * whose text is the figure's text, so the searches over chunks find the figure by what it says (and,
 * with Structr Enterprise, by meaning). The chunk is not part of the document's text in reading order
 * (position -1, no neighbours). It follows the figure: a changed text changes the chunk and drops its
 * embedding, an empty text or a figure without document has no chunk.
 */
public final class FigureContentWriter {

	private static final Logger logger = LoggerFactory.getLogger(FigureContentWriter.class);

	/** The position of a chunk that is not part of the text in reading order. */
	public static final int NOT_IN_READING_ORDER = -1;

	private FigureContentWriter() {
	}

	/**
	 * Brings the chunk of a figure in line with the figure. Must run in a transaction; the listeners are
	 * told after it commits, if the chunk changed.
	 */
	public static void write(final NodeInterface figure) throws FrameworkException {

		final App app                       = StructrApp.getInstance(SecurityContext.getSuperUserInstance());
		final Traits figureTraits           = figure.getTraits();
		final Traits chunkTraits            = Traits.of(ContentExtractionTraits.EXTRACTED_CONTENT_NODE);
		final String text                   = figure.getProperty(figureTraits.key(DocumentFigureTraitDefinition.TEXT_PROPERTY));
		final NodeInterface document        = figure.getProperty(figureTraits.key(DocumentFigureTraitDefinition.DOCUMENT_PROPERTY));
		final NodeInterface current         = figure.getProperty(figureTraits.key(DocumentFigureTraitDefinition.CONTENT_PROPERTY));
		final ExtractedContentNode existing = current != null ? current.as(ExtractedContentNode.class) : null;

		if (text == null || text.isBlank() || document == null) {

			if (existing != null) {

				app.delete(existing);
			}

			return;
		}

		final Integer page   = figure.getProperty(figureTraits.key(DocumentFigureTraitDefinition.PAGE_PROPERTY));
		final String label   = figure.getProperty(figureTraits.key(DocumentFigureTraitDefinition.LABEL_PROPERTY));
		final String hash    = sha256(text);
		final boolean moved  = existing != null && (existing.getFile() == null || !existing.getFile().getUuid().equals(document.getUuid()));

		if (existing != null && !moved && hash.equals(existing.getContentHash()) && Objects.equals(page, existing.getPageStart()) && Objects.equals(headingPath(label), existing.getHeadingPath())) {

			return;
		}

		final PropertyMap properties = new PropertyMap();

		properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.FILE_PROPERTY), document);
		properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.TEXT_PROPERTY), text);
		properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.POSITION_PROPERTY), NOT_IN_READING_ORDER);
		properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.START_OFFSET_PROPERTY), 0);
		properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.END_OFFSET_PROPERTY), text.length());
		properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.CHAR_COUNT_PROPERTY), text.length());
		properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.TOKEN_COUNT_PROPERTY), TokenCounter.forEncoding(ContentExtractionSettings.TokenEncoding.getValue()).count(text));
		properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.PAGE_START_PROPERTY), page);
		properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.PAGE_END_PROPERTY), page);
		properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.HEADING_PATH_PROPERTY), headingPath(label).toArray(String[]::new));
		properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.LANGUAGE_PROPERTY), ContentExtraction.detectLanguage(text).language());
		properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.CONTENT_HASH_PROPERTY), hash);

		final NodeInterface chunk;

		if (existing != null) {

			// the embedding belongs to the old text; the module that writes embeddings computes a new one
			if (!hash.equals(existing.getContentHash())) {

				properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.EMBEDDING_PROPERTY), null);
				properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.EMBEDDING_MODEL_PROPERTY), null);
			}

			existing.setProperties(SecurityContext.getSuperUserInstance(), properties);

			chunk = existing;

		} else {

			properties.put(chunkTraits.key(ExtractedContentNodeTraitDefinition.FIGURE_PROPERTY), figure);

			chunk = app.create(ContentExtractionTraits.EXTRACTED_CONTENT_NODE, properties);
		}

		final String documentUuid = document.getUuid();
		final String chunkUuid    = chunk.getUuid();

		TransactionCommand.queuePostCommitProcedure(() -> notifyChanged(documentUuid, chunkUuid));
	}

	// ----- private methods -----
	private static void notifyChanged(final String documentUuid, final String chunkUuid) {

		for (final ContentExtractionListener listener : ContentExtraction.getListeners()) {

			try {

				listener.onContentAdded(documentUuid, List.of(chunkUuid));

			} catch (final RuntimeException ex) {

				logger.warn("Content extraction listener {} failed for the figure text of file {}", listener.getClass().getName(), documentUuid, ex);
			}
		}
	}

	private static List<String> headingPath(final String label) {

		return label != null && !label.isBlank() ? List.of(label) : List.of();
	}

	private static String sha256(final String value) {

		try {

			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));

		} catch (final NoSuchAlgorithmException ex) {

			// every Java runtime is required to provide SHA-256
			throw new IllegalStateException(ex);
		}
	}
}
