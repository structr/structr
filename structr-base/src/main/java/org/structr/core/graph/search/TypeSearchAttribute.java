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
import org.structr.api.search.Operation;
import org.structr.api.search.TypeQuery;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;

/**
 *
 *
 * @param <S>
 */
public class TypeSearchAttribute<S extends GraphObject> extends PropertySearchAttribute<String> implements TypeQuery {

	private static final Logger logger = LoggerFactory.getLogger(TypeSearchAttribute.class.getName());

	//private Set<String> types = null;
	private String sourceType = null;
	private String targetType = null;
	private String type       = null;

	public TypeSearchAttribute(final String type, final Operation operation, final boolean isExactMatch) {

		super(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.TYPE_PROPERTY), null, operation, isExactMatch);

		final Traits traits = Traits.of(type);

		if (traits.isRelationshipType()) {

			try {

				final Relation rel = traits.getRelation();
				setValue(rel.name());

				this.sourceType = rel.getSourceType();
				this.targetType = rel.getTargetType();

			} catch (Throwable t) {
				logger.warn("", t);
			}

		} else {

			// node types
			setValue(type);
		}

		//this.types = traits.getLabels();
		this.type = type;
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
	public boolean includeInResult(final GraphObject entity) {

		final Operation operation = getOperation();
		final boolean isOfType   = entity.getTraits().contains(type);

		if (operation.equals(Operation.NOT)) {

			return !isOfType;

		} else {

			return isOfType;
		}
	}

	@Override
	public String getSourceType() {
		return sourceType;
	}

	@Override
	public String getTargetType() {
		return targetType;
	}
}
