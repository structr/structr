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
import org.structr.api.graph.Relationship;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;

/**
 * A factory for structr relationships. This class exists because we need a fast
 * way to instantiate and initialize structr relationships, as this is the most-
 * used operation.
 *
 * @param <T>
 */
public class RelationshipFactory<T extends RelationshipInterface> extends Factory<Relationship, T> {

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
	public T instantiate(final Relationship relationship) {
		return instantiate(relationship, null);
	}

	@Override
	public T instantiate(final Relationship relationship, final Identity pathSegmentId) {

		if (relationship == null || TransactionCommand.isDeleted(relationship) || relationship.isDeleted()) {
			return null;
		}

		final Class relationshipType = factoryDefinition.determineRelationshipType(relationship);
		if (relationshipType == null) {
			return null;
		}

		return (T) instantiateWithType(relationship, relationshipType, pathSegmentId, false);
	}

	@Override
	public T instantiateWithType(final Relationship relationship, final Class<T> relClass, final Identity pathSegmentId, final boolean isCreation) {

		// cannot instantiate relationship without type
		if (relClass == null) {
			return null;
		}

		logger.debug("Instantiate relationship with type {}", relClass.getName());

		SecurityContext securityContext = factoryProfile.getSecurityContext();
		T newRel          = null;

		try {

			newRel = relClass.getDeclaredConstructor().newInstance();

		} catch (Throwable t) {
			logger.warn("", t);
			newRel = null;
		}

		if (newRel == null) {
			logger.warn("newRel was null, using generic relationship for {}", relationship);
			newRel = (T)StructrApp.getConfiguration().getFactoryDefinition().createGenericRelationship();
		}

		newRel.init(securityContext, relationship, relClass, TransactionCommand.getCurrentTransactionId());

		return newRel;
	}

	@Override
	public T adapt(final Relationship relationship) {
		return instantiate(relationship);
	}

	@Override
	public T instantiate(final Relationship obj, final boolean includeHidden, final boolean publicOnly) throws FrameworkException {

		factoryProfile.setIncludeHidden(includeHidden);
		factoryProfile.setPublicOnly(publicOnly);

		return instantiate(obj);
	}
}
