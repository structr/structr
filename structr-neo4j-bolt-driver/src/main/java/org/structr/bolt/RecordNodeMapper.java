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
package org.structr.bolt;

import org.neo4j.driver.Record;
import org.neo4j.driver.exceptions.value.Uncoercible;
import org.neo4j.driver.types.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 *
 */
class RecordNodeMapper implements Function<Record, Node> {

	private static final Logger logger = LoggerFactory.getLogger(RecordNodeMapper.class);

	@Override
	public Node apply(final Record t) {

		try {
			return t.get(0).asNode();

		} catch (Uncoercible ex) {

			logger.warn("Unable to map Neo4j Record {} to Structr Node: {}", t.asMap(), ex.getMessage());
		}

		return null;
	}
}