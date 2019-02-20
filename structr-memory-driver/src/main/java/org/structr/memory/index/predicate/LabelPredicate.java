/**
 * Copyright (C) 2010-2018 Structr GmbH
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
import org.structr.api.graph.Label;
import org.structr.api.graph.PropertyContainer;
import org.structr.memory.MemoryNode;
import org.structr.memory.MemoryRelationship;

/**
 */
public class LabelPredicate<T extends PropertyContainer> implements Predicate<T> {

	private Label label       = null;
	private String sourceType = null;
	private String targetType = null;

	public LabelPredicate(final Label label) {
		this(label, null, null);
	}

	public LabelPredicate(final Label label, final String sourceType, final String targetType) {

		this.sourceType = sourceType;
		this.targetType = targetType;
		this.label      = label;
	}

	@Override
	public boolean accept(final T entity) {

		if (entity instanceof MemoryNode) {

			final MemoryNode node = (MemoryNode)entity;

			return node.hasLabel(label);
		}

		if (entity instanceof MemoryRelationship) {

			final MemoryRelationship relationship = (MemoryRelationship)entity;

			final String relSourceType = relationship.getSourceNodeIdentity().getType();
			final String relTargetType = relationship.getTargetNodeIdentity().getType();
			final String relType       = relationship.getType().name();

			// step by step
			if (!label.name().equals(relType)) {
				return false;
			}

			if (sourceType != null && !sourceType.equals(relSourceType)) {
				return false;
			}

			if (targetType != null && !targetType.equals(relTargetType)) {
				return false;
			}

			return true;
		}

		return false;
	}
}
