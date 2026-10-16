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

import org.structr.extraction.api.chunk.Chunk;
import org.structr.extraction.api.options.EffectiveContentOptions;
import org.structr.extraction.entity.ExtractedContentNode;
import org.structr.web.entity.File;

import java.util.List;

/**
 * Notified when the stored content of a file changes. Register with
 * {@link ContentExtraction#addListener(ContentExtractionListener)}, e.g. in a module's onLoad().
 *
 * Exceptions thrown by a listener are logged and do not affect the extraction.
 */
public interface ContentExtractionListener {

	/**
	 * Called in the transaction that replaces the chunks of a file, before the old chunks are deleted.
	 * A listener can carry data over from old to new chunks here, e.g. reuse the embeddings of chunks
	 * whose content hash is unchanged. When content is deleted, {@code newChunks} is empty.
	 *
	 * @param file      the file
	 * @param oldNodes  the chunks about to be deleted
	 * @param newChunks the chunks that will be stored
	 */
	default void onBeforeContentReplaced(final File file, final List<ExtractedContentNode> oldNodes, final List<Chunk> newChunks) {
	}

	/**
	 * Called after the new chunks of a file were committed, outside of any transaction, e.g. to start
	 * an embedding job.
	 *
	 * @param fileUuid  the file's id
	 * @param nodeUuids the ids of the new ExtractedContentNodes, in reading order
	 * @param options   the options the content was extracted and split with
	 */
	default void onContentExtracted(final String fileUuid, final List<String> nodeUuids, final EffectiveContentOptions options) {
	}
}
