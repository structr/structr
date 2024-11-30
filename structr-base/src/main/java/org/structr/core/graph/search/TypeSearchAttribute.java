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
package org.structr.core.graph.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.search.Occurrence;
import org.structr.api.search.TypeQuery;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Relation;
import org.structr.core.traits.GraphObject;
import org.structr.core.traits.Trait;

import java.util.Set;

/**
 *
 *
 * @param <S>
 */
public class TypeSearchAttribute<S extends GraphObject> extends PropertySearchAttribute<String> implements TypeQuery {

	private static final Logger logger = LoggerFactory.getLogger(TypeSearchAttribute.class.getName());

	private Set<String> types = null;
	private Class sourceType  = null;
	private Class targetType  = null;

	public TypeSearchAttribute(final Trait<S> type, final Occurrence occur, final boolean isExactMatch) {

		super(AbstractNode.typeHandler, null, occur, isExactMatch);

		if (Relation.class.isAssignableFrom(type)) {

			try {

				final Relation rel = (Relation)type.getDeclaredConstructor().newInstance();
				setValue(rel.name());

				this.sourceType = rel.getSourceType();
				this.targetType = rel.getTargetType();

			} catch (Throwable t) {
				logger.warn("", t);
			}

		} else {

			// node types
			setValue(type.getName());
		}

		this.types  = SearchCommand.getAllSubtypesAsStringSet(type.getName());
	}

	@Override
	public String toString() {
		return "TypeSearchAttribute(" + super.toString() + ")";
	}

	@Override
	public Class getQueryType() {
		return TypeQuery.class;
	}

	@Override
	public boolean includeInResult(final GraphObjectTraits entity) {

		final String nodeValue   = entity.getProperty(getKey());
		final Occurrence occur   = getOccurrence();
		final boolean isOfType   = types.contains(nodeValue);

		if (occur.equals(Occurrence.FORBIDDEN)) {

			return !isOfType;

		} else {

			return isOfType;
		}
	}

	@Override
	public Class getSourceType() {
		return sourceType;
	}

	@Override
	public Class getTargetType() {
		return targetType;
	}
}
