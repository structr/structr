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
package org.structr.core.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Returns an aggregated Map of relationship counts for the given node.
 *
 */
public class NodeRelationshipStatisticsCommand extends NodeServiceCommand {

	private static final Logger logger = LoggerFactory.getLogger(NodeRelationshipStatisticsCommand.class.getName());

	public Map<String, Long> execute(final AbstractNode sNode) throws FrameworkException {
		return execute(sNode, null);
	}

	public Map<String, Long> execute(AbstractNode sNode, Direction dir) throws FrameworkException {

		final Map<String, Long> statistics = new LinkedHashMap<>();
		final Node node                    = sNode.getNode();
		Iterable<Relationship> rels        = null;

		if (dir != null) {

			rels = node.getRelationships(dir);

		} else {

			rels = node.getRelationships();
		}

		try {

			// use temporary map to avoid frequent construction of Long values when increasing..
			Map<String, LongValueHolder> values = new LinkedHashMap<>();
			for (Relationship r : rels) {

				final String relType = r.getType().name();
				LongValueHolder count = values.get(relType);
				if(count == null) {
					count = new LongValueHolder();
					values.put(relType, count);
				}
				count.inc();
			}

			// create results from temporary map
			for(Entry<String, LongValueHolder> entry : values.entrySet()) {
				final String key = entry.getKey();
				LongValueHolder value = entry.getValue();
				statistics.put(key, value.getValue());
			}

		} catch (RuntimeException e) {

			logger.warn("Exception occured.", e);
		}

		return statistics;
	}

	private static class LongValueHolder {

		private long value = 0;

		public long getValue() {
			return value;
		}

		public void setValue(long value) {
			this.value = value;
		}

		public void inc() {
			this.value++;
		}
	}
}
