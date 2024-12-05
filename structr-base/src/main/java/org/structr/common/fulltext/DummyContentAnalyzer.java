/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.core.graph.NodeInterface;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class DummyContentAnalyzer implements ContentAnalyzer {

	private static final Logger logger = LoggerFactory.getLogger(DummyContentAnalyzer.class.getName());

	@Override
	public Map<String, Object> analyzeContent(final NodeInterface indexable) throws FrameworkException {
		
		logger.warn("No content analyzer installed, this is a dummy implementation that does nothing.");
		return Map.of();
	}

	@Override
	public Set<String> getStopWords(final String language) {
		return Collections.EMPTY_SET;
	}
}
