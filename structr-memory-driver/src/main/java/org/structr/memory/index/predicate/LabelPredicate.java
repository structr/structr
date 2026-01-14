/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.memory.index.predicate;

import org.structr.api.Predicate;
import org.structr.api.graph.PropertyContainer;
import org.structr.memory.MemoryNode;
import org.structr.memory.MemoryRelationship;

/**
 */
public class LabelPredicate<T extends PropertyContainer> implements Predicate<T> {

	private String label       = null;
	private String sourceLabel = null;
	private String targetLabel = null;

	public LabelPredicate(final String label) {
		this(label, null, null);
	}

	public LabelPredicate(final String label, final String sourceLabel, final String targetLabel) {

		this.sourceLabel = sourceLabel;
		this.targetLabel = targetLabel;
		this.label       = label;
	}

	@Override
	public String toString() {
		return "LABEL(" + label + ")";
	}

	@Override
	public boolean accept(final T entity) {

		if (entity instanceof MemoryNode) {

			final MemoryNode node = (MemoryNode)entity;

			return node.hasLabel(label);
		}

		if (entity instanceof MemoryRelationship) {

			final MemoryRelationship relationship = (MemoryRelationship)entity;
			final MemoryNode sourceNode           = (MemoryNode)relationship.getStartNode();
			final MemoryNode targetNode           = (MemoryNode)relationship.getEndNode();
			final String relType                  = relationship.getType().name();

			if (!label.equals(relType)) {
				return false;
			}

			if (sourceLabel != null && !sourceNode.hasLabel(sourceLabel)) {
				return false;
			}

			if (targetLabel != null && !targetNode.hasLabel(targetLabel)) {
				return false;
			}

			return true;
		}

		return false;
	}
}
