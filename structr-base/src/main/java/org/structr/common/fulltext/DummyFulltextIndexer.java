/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common.fulltext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.graph.NodeInterface;

/**
 *
 */
public class DummyFulltextIndexer implements FulltextIndexer {

	private static final Logger logger = LoggerFactory.getLogger(DummyFulltextIndexer.class.getName());

	@Override
	public void addToFulltextIndex(final NodeInterface indexable) throws FrameworkException {
		logger.warn("No fulltext indexer installed, this is a dummy implementation that does nothing.");
	}

	@Override
	public GraphObjectMap getContextObject(String searchTerm, String text, int contextLength) {

		logger.warn("No fulltext indexer installed, this is a dummy implementation that does nothing.");

		return new GraphObjectMap();
	}
}
