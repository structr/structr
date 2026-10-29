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
package org.structr.extraction.entity;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.web.entity.File;

import java.util.List;

/**
 * A chunk of a file's content, stored in the graph.
 *
 * The embedding properties are written by the Enterprise AI module, never computed by this module.
 * When a file is extracted again, a new chunk with the same content hash as an old one keeps the
 * old chunk's embedding and model.
 */
public interface ExtractedContentNode extends NodeInterface {

	File getFile();
	ExtractedContentNode getNext();
	ExtractedContentNode getPrevious();

	String getText();
	int getPosition();
	int getStartOffset();
	int getEndOffset();
	int getCharCount();
	int getTokenCount();
	Integer getPageStart();
	Integer getPageEnd();
	List<String> getHeadingPath();
	String getEmbeddedPath();
	String getLanguage();
	String getContentHash();

	/**
	 * The embedding vector, or null if none was computed.
	 */
	double[] getEmbedding();

	/**
	 * Stores an embedding vector and the model that produced it.
	 */
	void setEmbedding(final double[] embedding, final String model) throws FrameworkException;

	String getEmbeddingModel();


	/**
	 * The DocumentFigure whose text this chunk holds, or null. A figure's chunk is not part of the text in
	 * reading order; it is written from the figure's text whenever that changes.
	 */
	NodeInterface getFigure();
}
