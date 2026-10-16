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
package org.structr.extraction.traits.wrappers;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.extraction.entity.ExtractedContentNode;
import org.structr.web.entity.File;

import java.util.Arrays;
import java.util.List;

import static org.structr.extraction.traits.definitions.ExtractedContentNodeTraitDefinition.*;

public class ExtractedContentNodeTraitWrapper extends AbstractNodeTraitWrapper implements ExtractedContentNode {

	public ExtractedContentNodeTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {

		super(traits, wrappedObject);
	}

	@Override
	public File getFile() {

		final NodeInterface file = wrappedObject.getProperty(traits.key(FILE_PROPERTY));

		return file != null ? file.as(File.class) : null;
	}

	@Override
	public ExtractedContentNode getNext() {

		return neighbour(NEXT_PROPERTY);
	}

	@Override
	public ExtractedContentNode getPrevious() {

		return neighbour(PREVIOUS_PROPERTY);
	}

	@Override
	public String getText() {

		return wrappedObject.getProperty(traits.key(TEXT_PROPERTY));
	}

	@Override
	public int getPosition() {

		return intValue(POSITION_PROPERTY);
	}

	@Override
	public int getStartOffset() {

		return intValue(START_OFFSET_PROPERTY);
	}

	@Override
	public int getEndOffset() {

		return intValue(END_OFFSET_PROPERTY);
	}

	@Override
	public int getCharCount() {

		return intValue(CHAR_COUNT_PROPERTY);
	}

	@Override
	public int getTokenCount() {

		return intValue(TOKEN_COUNT_PROPERTY);
	}

	@Override
	public Integer getPageStart() {

		return wrappedObject.getProperty(traits.key(PAGE_START_PROPERTY));
	}

	@Override
	public Integer getPageEnd() {

		return wrappedObject.getProperty(traits.key(PAGE_END_PROPERTY));
	}

	@Override
	public List<String> getHeadingPath() {

		final String[] path = wrappedObject.getProperty(traits.key(HEADING_PATH_PROPERTY));

		return path != null ? List.of(path) : List.of();
	}

	@Override
	public String getLanguage() {

		return wrappedObject.getProperty(traits.key(LANGUAGE_PROPERTY));
	}

	@Override
	public String getContentHash() {

		return wrappedObject.getProperty(traits.key(CONTENT_HASH_PROPERTY));
	}

	@Override
	public double[] getEmbedding() {

		final Double[] embedding = wrappedObject.getProperty(traits.key(EMBEDDING_PROPERTY));

		return embedding != null ? Arrays.stream(embedding).mapToDouble(Double::doubleValue).toArray() : null;
	}

	@Override
	public void setEmbedding(final double[] embedding, final String model) throws FrameworkException {

		final PropertyMap properties = new PropertyMap();

		properties.put(traits.key(EMBEDDING_PROPERTY), embedding != null ? Arrays.stream(embedding).boxed().toArray(Double[]::new) : null);
		properties.put(traits.key(EMBEDDING_MODEL_PROPERTY), model);

		wrappedObject.setProperties(wrappedObject.getSecurityContext(), properties);
	}

	@Override
	public String getEmbeddingModel() {

		return wrappedObject.getProperty(traits.key(EMBEDDING_MODEL_PROPERTY));
	}

	// ----- private methods -----
	private ExtractedContentNode neighbour(final String key) {

		final NodeInterface node = wrappedObject.getProperty(traits.key(key));

		return node != null ? node.as(ExtractedContentNode.class) : null;
	}

	private int intValue(final String key) {

		final Integer value = wrappedObject.getProperty(traits.key(key));

		return value != null ? value : 0;
	}
}
