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
import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractRelationship;

/**
 * A factory for structr relationships. This class exists because we need a fast
 * way to instantiate and initialize structr relationships, as this is the most-
 * used operation.
 *
 * @param <T>
 */
public class RelationshipFactory extends Factory<Relationship, RelationshipInterface> {

	private static final Logger logger = LoggerFactory.getLogger(RelationshipFactory.class.getName());

	public RelationshipFactory(final SecurityContext securityContext) {
		super(securityContext);
	}

	public RelationshipFactory(final SecurityContext securityContext, final boolean includeHidden, final boolean publicOnly) {
		super(securityContext, includeHidden, publicOnly);
	}

	public RelationshipFactory(final SecurityContext securityContext, final int pageSize, final int page) {
		super(securityContext, pageSize, page);
	}

	public RelationshipFactory(final SecurityContext securityContext, final boolean includeHidden, final boolean publicOnly, final int pageSize, final int page) {
		super(securityContext, includeHidden, publicOnly, pageSize, page);
	}

	@Override
	public RelationshipInterface instantiateWithType(final Relationship relationship, final String relClass, final Identity pathSegmentId, final boolean isCreation) {

		// cannot instantiate relationship without type
		if (relClass == null) {
			return null;
		}

		logger.debug("Instantiate relationship with type {}", relClass);

		return new AbstractRelationship(securityContext, relationship, relClass, TransactionCommand.getCurrentTransactionId());
	}

	// ----- protected methods -----
	@Override
	protected String determineActualType(final Relationship relationship) {

		if (relationship.hasProperty("type")) {

			final Object obj =  relationship.getProperty("type");
			if (obj != null) {

				return obj.toString();
			}
		}

		return null;
	}
}
